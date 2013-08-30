package org.flowvisor.classifier;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.flowvisor.api.FlowTableCallback;
import org.flowvisor.api.TopologyCallback;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.ConfigurationEvent;
import org.flowvisor.config.FVConfig;

import org.flowvisor.config.FlowMapChangedListener;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.config.FlowvisorChangedListener;
import org.flowvisor.config.FlowvisorImpl;
import org.flowvisor.config.Slice;
import org.flowvisor.config.SwitchChangedListener;
import org.flowvisor.config.SwitchImpl;

import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.events.FVRequestTimeoutEvent;
import org.flowvisor.events.FVStatsTimer;
import org.flowvisor.events.OFKeepAlive;
import org.flowvisor.events.TearDownEvent;
import org.flowvisor.exceptions.BufferFull;
import org.flowvisor.exceptions.MalformedOFMessage;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowDB;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.LinearFlowDB;
import org.flowvisor.flows.NoopFlowDB;
import org.flowvisor.io.FVMessageAsyncStream;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.log.SendRecvDropStats.FVStatsType;
import org.flowvisor.message.Classifiable;
import org.flowvisor.message.FVError;
import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.FVMessageFactory;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.message.SanityCheckable;
import org.flowvisor.message.statistics.FVAggregateStatisticsReply;
import org.flowvisor.message.statistics.FVAggregateStatisticsRequest;
import org.flowvisor.message.statistics.FVFlowStatisticsReply;
import org.flowvisor.message.statistics.FVFlowStatisticsRequest;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.resources.SlicerLimits;
import org.flowvisor.resources.ratelimit.FixedIntervalRefillStrategy;
import org.flowvisor.resources.ratelimit.TokenBucket;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsReply.OFStatisticsReplyFlags;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFError.OFHelloFailedCode;
import org.openflow.protocol.action.*;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Map OF messages from the switch to the appropriate slice
 *
 * Also handles all of the switch-specific but slice-general state and
 * rewriting.
 *
 * @author capveg
 *
 */

public class FVClassifier implements FVEventHandler, FVSendMsg, FlowMapChangedListener, FlowvisorChangedListener,
											SwitchChangedListener {

	public static final int MessagesPerRead = 50; // for performance tuning
	FVEventLoop loop;
	SocketChannel sock;
	String switchName;
	boolean doneID;
	FVMessageAsyncStream msgStream;
	OFFeaturesReply switchInfo;
	ConcurrentHashMap<String, FVSlicer> slicerMap;
	XidTranslator xidTranslator;
	CookieTranslator cookieTranslator;
	short missSendLength;
	FlowMap switchFlowMap;
	private boolean shutdown;
	Set<Short> activePorts;
	private final FVMessageFactory factory;
	OFKeepAlive keepAlive;
	SendRecvDropStats stats;
	private FlowDB flowDB;
	private boolean wantStatsDescHack;
	String floodPermsSlice; // the slice that has permission to use native
	private Boolean flowTracking = false;
	private Boolean registeredForFlowTable;

	/*private HashMap<String, TopologyCallback> flowTableCallbackDb;
	private HashMap<String, HashMap<Long, FlowTableCallback>> flowTableCallbackDb;
	private HashMap<Long, FlowTableCallback> dpidMap;*/
	private List<FlowTableCallback> flowTableList;

	
	private HashMap<String, Integer> fmlimits = new HashMap<String, Integer>();
	private HashMap<String, Integer> currfmlimits = new HashMap<String, Integer>();
	private SlicerLimits slicerLimits;
	
	//If the window is open, pollFlowTableStats can be called to poll for the statistics from the switch!
	private boolean statsWindowOpen = true;
	private HashMap<String, ArrayList<FVFlowStatisticsReply>> flowStats = 
			new HashMap<String, ArrayList<FVFlowStatisticsReply>>();
	private HashMap<String, ArrayList<FVFlowStatisticsReply>> actualStats = 
			new HashMap<String, ArrayList<FVFlowStatisticsReply>>();
	private ConcurrentLinkedQueue<String> toDeleteSlices = new ConcurrentLinkedQueue<String>();

	// OFPP_FLOOD

	public FVClassifier(FVEventLoop loop, SocketChannel sock) {
		this.loop = loop;
		this.switchName = "unidentified:" + sock.toString();
		this.factory = new FVMessageFactory();
		this.stats = new SendRecvDropStats();
		try {
			this.msgStream = new FVMessageAsyncStream(sock, this.factory, this,
					this.stats);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "IOException in constructor!");
			e.printStackTrace();
		}
		this.sock = sock;
		this.switchInfo = null;
		this.doneID = false;
		this.floodPermsSlice = ""; // disabled, at first
		this.slicerMap = new ConcurrentHashMap<String, FVSlicer>();
		this.xidTranslator = new XidTranslatorWithMessage();
		this.cookieTranslator = new CookieTranslator();
		this.missSendLength = 128;
		this.switchFlowMap = null;
		this.activePorts = new HashSet<Short>();
		this.wantStatsDescHack = true;
		FlowvisorImpl.addListener(this);
	
		//Initializing the below two var for the new feature - obtaining flowTable from the switch
		this.registeredForFlowTable = false;
		this.flowTableList= new ArrayList<FlowTableCallback>();
		//this.flowTableCallbackDb = new HashMap<String, TopologyCallback>();
		/*this.flowTableCallbackDb = new HashMap<String,  HashMap<Long , FlowTableCallback>>();
		this.dpidMap = new HashMap<Long, FlowTableCallback>();*/
		
		// need to initialize values.
		try {
			setFlowTracking(FlowvisorImpl.getProxy().gettrack_flows());
		} catch (ConfigError e) {
			setFlowTracking(false);
		}
	}

	public short getMissSendLength() {
		return missSendLength;
	}

	public void setMissSendLength(short missSendLength) {
		this.missSendLength = missSendLength;
	}

	@Override
	public boolean needsConnect() {
		return false; // never want connect events
	}

	@Override
	public boolean needsRead() {
		return true; // always want read events
	}

	@Override
	public boolean needsWrite() {
		if (this.msgStream == null)
			return false;
		return this.msgStream.needsFlush();
	}

	@Override
	public boolean needsAccept() {
		return false;
	}

	public OFFeaturesReply getSwitchInfo() {
		return switchInfo;
	}

	public void setSwitchInfo(OFFeaturesReply switchInfo) {
		this.switchInfo = switchInfo;
		this.activePorts.clear();
		for (OFPhysicalPort phyPort : switchInfo.getPorts())
			this.activePorts.add(phyPort.getPortNumber());
	}

	public boolean isPortActive(short port) {
		return this.activePorts.contains(port);
	}

	public void addPort(OFPhysicalPort phyPort) {
		for (Iterator<OFPhysicalPort> it = switchInfo.getPorts().iterator(); it
				.hasNext();) {
			// remove stale info, if it exists
			OFPhysicalPort lPort = it.next();
			if (lPort.getPortNumber() == phyPort.getPortNumber()) {
				it.remove();
				break;
			}
		}
		// update new info
		switchInfo.getPorts().add(phyPort);
		this.activePorts.add(phyPort.getPortNumber());
	}

	public void removePort(OFPhysicalPort phyPort) {
		boolean found = false;
		for (Iterator<OFPhysicalPort> it = switchInfo.getPorts().iterator(); it
				.hasNext();) {
			OFPhysicalPort lPort = it.next();
			if (lPort.getPortNumber() == phyPort.getPortNumber()) {
				found = true;
				it.remove();
			}
		}
		if (!found)
			FVLog.log(LogLevel.INFO, this,
					"asked to remove non-existant port: ", phyPort);
		this.activePorts.remove(phyPort.getPortNumber());
	}

	public FVSlicer getSlicerByName(String sliceName) {
		if (this.slicerMap == null)
			return null; // race condition on shutdown
		synchronized (slicerMap) {
			return slicerMap.get(sliceName);
		}
	}

	public XidTranslator getXidTranslator() {
		return xidTranslator;
	}

	public void setXidTranslator(XidTranslator xidTranslator) {
		this.xidTranslator = xidTranslator;
	}
	
	public CookieTranslator getCookieTranslator() {
		return this.cookieTranslator;
	}
	
	public void setCookieTranslator(CookieTranslator cookieTranslator) {
		this.cookieTranslator = cookieTranslator;
	}

	/**
	 * on init, send HELLO, delete all flow entries, and send features request
	 *
	 * @throws IOException
	 */

	public void init() throws IOException {
		// send initial handshake
		sendMsg(new OFHello(), this);
		// delete all entries in the flowtable
		OFMatch match = new OFMatch();
		match.setWildcards(OFMatch.OFPFW_ALL);
		OFFlowMod fm = new OFFlowMod();
		fm.setMatch(match);
		fm.setCommand(OFFlowMod.OFPFC_DELETE);
		fm.setOutPort(OFPort.OFPP_NONE);
		fm.setBufferId(0xffffffff); // buffer to NONE
		sendMsg(fm, this);
		// request the switch's features
		sendMsg(new OFFeaturesRequest(), this);
		msgStream.flush();

		// Schedule an event to timeout on the features request
		FVRequestTimeoutEvent requestTimeoutEvent = new FVRequestTimeoutEvent(this);
		requestTimeoutEvent.setExpireTime(System.currentTimeMillis() + FVRequestTimeoutEvent.WAIT_TIME);
		loop.addTimer(requestTimeoutEvent);
		
		
		
		int ops = SelectionKey.OP_READ;
		if (msgStream.needsFlush())
			ops |= SelectionKey.OP_WRITE;
		// this now calls FlowVisor.addHandler()
		loop.register(sock, ops, this);
		// start up keep alive events
		this.keepAlive = new OFKeepAlive(this, this, loop);
		this.keepAlive.scheduleNextCheck();

		try {
			this.wantStatsDescHack = FVConfig.getStatsDescHack();
			FlowvisorImpl.addListener(this);
		} catch (ConfigError e) {
			try {
				FVLog.log(LogLevel.WARN, this, "config: stats_desc_hack "
						+ " not set; defaulting to off");
				this.wantStatsDescHack = false;
				FVConfig.setStatsDescHack(false);
			} catch (ConfigError e1) {
				throw new RuntimeException("Tried to set default "
						+ "stats_desc_hack=true, but got: " + e1);
			}
		}
		updateFloodPerms();
	}

	/*
	 * Parse the flood_perms out of the config
	 *
	 * Check "switches.$dpid.flood_perms" if we know $dpid, else check
	 * "switches.default.flood_perms" also add it to the watch list
	 */

	

	void updateFloodPerms() {
		Long dpid = null;
		if (this.doneID)
			dpid = this.getDPID();
		try {
			/*String entry = FVConfig.SWITCHES + FVConfig.FS + dpid + FVConfig.FS
				+ FVConfig.FLOOD_PERM;*/
			String perm = FVConfig.getFloodPerm(dpid);
			if (!perm.equals(""))
				this.floodPermsSlice = perm;
			FVLog.log(LogLevel.DEBUG, this, "giving flood perms to slice: "
					+ this.floodPermsSlice);
			// note: watch() is smart and won't double enter this
			if (doneID)
				SwitchImpl.addListener(dpid, this);
			else
				FlowvisorImpl.addListener(this);
			//FVConfig.watch(this, entry);
		} catch (ConfigError e) {
			// do nothing if no entry
		}
	}

	public void registerPong() {
		this.keepAlive.registerPong();
	}

	@Override
	public String getName() {
		return "classifier-" + switchName;
	}

	@Override
	public long getThreadContext() {
		return loop.getThreadContext();
	}

	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (this.shutdown) {
			FVLog.log(LogLevel.WARN, this, "is shutdown: ignoring: " + e);
			return;
		}
		if (Thread.currentThread().getId() != this.getThreadContext()) {
			// this event was sent from a different thread context
			loop.queueEvent(e); // queue event
			return; // and process later
		}
		if (e instanceof FVIOEvent){
			handleIOEvent((FVIOEvent) e);
		}
		else if (e instanceof OFKeepAlive){
			handleKeepAlive(e);
		}
	/*	else if (e instanceof ConfigUpdateEvent)
			updateConfig((ConfigUpdateEvent) e);*/
		else if (e instanceof TearDownEvent){
			this.tearDown();
		}
		else if (e instanceof FVRequestTimeoutEvent){
			handleRequestTimeout();
		}
		else if (e instanceof FVStatsTimer){
			this.statsWindowOpen = true;
			//TopologyController tc = TopologyController.getRunningInstance();
			//this.registeredForFlowTable = tc.getRegisteredForFlowTable();
			if (this.registeredForFlowTable)
				pollFlowTableStats(null);		
		}
		else
			throw new UnhandledEvent(e);
	}

	private void handleRequestTimeout(){
		if (switchName.startsWith("unidentified")){
			// Haven't got reply yet, tear down socket
			tearDown();
		}

	}

	private void handleKeepAlive(FVEvent e) {
		if (!this.keepAlive.isAlive()) {
			FVLog.log(LogLevel.WARN, this, "keepAlive timeout");
			this.tearDown();
			return;
		}
		this.keepAlive.sendPing();
		this.keepAlive.scheduleNextCheck();
	}

	/**
	 * Something in the config has changed; figure out what and re-cache it
	 *
	 * @param e
	 * 
	 * update ash: this method is irrelevant now because the listeners update the 
	 * values as they call the callback methods.
	 */
	/*private void updateConfig(ConfigUpdateEvent e) {
		String config = e.getConfig();
		FVLog.log(LogLevel.DEBUG, this, "got update: ", config);
		if (config.equals(FVConfig.FLOWSPACE)) {
			// update ourselves first
			connectToControllers(); // re-figure out who we should connect to
			// then tell everyone who depends on us (causality important :-)
			for (FVSlicer fvSlicer : slicerMap.values())
				this.loop.queueEvent(new ConfigUpdateEvent(e).setDst(fvSlicer));
		} else if (config.equals(FVConfig.FLOW_TRACKING)) {
			updateFlowTrackingConfig();
		} else if (config.endsWith(FVConfig.FLOOD_PERM)) {
			this.updateFloodPerms();
		} else {
			FVLog.log(LogLevel.WARN, this, "ignoring unknown config update: ",
					e.getConfig());
		}
	}*/

	/*
	 * Set by callback in setFlowTracking below.
	 */
	/*private synchronized void updateFlowTrackingConfig() {
		try {
			if (FVConfig.getBoolean(FVConfig.FLOW_TRACKING))
				this.flowDB = new LinearFlowDB(this);
			else
				this.flowDB = new NoopFlowDB();
		} catch (ConfigError e) {
			// default to flow tracking == off
			this.flowDB = new NoopFlowDB();
		}
	}*/

	void handleIOEvent(FVIOEvent e) {
		int ops = e.getSelectionKey().readyOps();

		try {
			// read stuff, if need be
			if ((ops & SelectionKey.OP_READ) != 0) {
				List<OFMessage> newMsgs = msgStream.read();
						//.read(FVClassifier.MessagesPerRead);
				if (newMsgs != null) {
					for (OFMessage m : newMsgs) {
						if (m == null) {
							FVLog.log(LogLevel.ALERT, this,
									"got an unparsable OF Message ",
									"(msgStream.read() returned a null):",
									"trying to ignore it");
							continue;
						}
						//FVLog.log(LogLevel.DEBUG, this, "THE TYPE " + m.getType());
						//FVLog.log(LogLevel.DEBUG, this, "read ", m);
						if ((m instanceof SanityCheckable)
								&& (!((SanityCheckable) m).isSane())) {
							FVLog.log(LogLevel.WARN, this,
									"msg failed sanity check; dropping: ", m);
							continue;
						}
						if (switchInfo != null) {
							classifyOFMessage(m);
							// mark this channel as still alive
							this.keepAlive.registerPong();
						}
						else
							handleOFMessage_unidenitified(m);

					}
				} else {
					throw new IOException("got EOF from other side");
				}
			}
			// write stuff if need be
			if ((ops & SelectionKey.OP_WRITE) != 0)
				msgStream.flush();
		} catch (IOException e1) {
			// connection to switch died; tear it down
			FVLog.log(LogLevel.INFO, this,
					"got IO exception; closing because : ", e1);
			this.tearDown();
			return;
		}
		// no need to setup for next select; done in eventloop
	}

	/**
	 * Close all slice connections and cleanup
	 */
	@Override
	public void tearDown() {
		FVLog.log(LogLevel.WARN, this, "tearing down");
		this.loop.unregister(this.sock, this);
		this.shutdown = true;
		try {
			this.sock.close();
			// shutdown each of the connections to the controllers
			Map<String, FVSlicer> tmpMap = slicerMap;
			slicerMap = null; // to prevent tearDown(slice) corruption
			for (FVSlicer fvSlicer : tmpMap.values())
				fvSlicer.closeDown(false);
		} catch (IOException e) {
			FVLog.log(LogLevel.WARN, this, "weird error on close:: ", e);
		}
		FlowSpaceImpl.removeListener(this);
		FlowvisorImpl.removeListener(this);
		SwitchImpl.removeListener(this.getDPID(), this);
		/*FVConfig.unwatch(this, FVConfig.FLOWSPACE); // unregister for FS updates
		FVConfig.unwatch(this, FVConfig.FLOW_TRACKING);
		FVConfig.unwatch(this, FVConfig.STATS_DESC_HACK);

		if (this.doneID)
			FVConfig.unwatch(this, FVConfig.SWITCHES + FVConfig.FS
					+ FlowSpaceUtil.dpidToString(getDPID()) + FVConfig.FS
					+ FVConfig.FLOOD_PERM);
		FVConfig
				.unwatch(this, FVConfig.SWITCHES + FVConfig.FS
						+ FVConfig.SWITCHES_DEFAULT + FVConfig.FS
						+ FVConfig.FLOOD_PERM);*/
		this.msgStream = null; // trick GC; prob not needed
	}

	/**
	 * Main function Pass this message on to the appropriate Slicer as defined
	 * by XID, FlowSpace, config, etc.
	 *
	 * @param m
	 */
	private void classifyOFMessage(OFMessage msg) {
		FVLog.log(LogLevel.DEBUG, this, "received from switch: ", msg);
		((Classifiable) msg).classifyFromSwitch(this); // msg specific handling
	}

	/**
	 * State machine for switches before we know which switch it is
	 *
	 * Wait for FEATURES_REPLY; ignore everything else
	 *
	 * @param m
	 *            incoming message
	 */
	private void handleOFMessage_unidenitified(OFMessage m) {
		switch (m.getType()) {
		case HELLO: // aleady sent our hello; just NOOP here
			if (m.getVersion() != OFMessage.OFP_VERSION) {
				FVLog.log(LogLevel.WARN, this,
						"Mismatched version from switch ", sock, " Got: ", m
								.getVersion(), " Wanted: ",
						OFMessage.OFP_VERSION);
				FVError fvError = (FVError) this.factory
						.getMessage(OFType.ERROR);
				fvError.setErrorType(OFErrorType.OFPET_HELLO_FAILED);
				fvError.setErrorCode(OFHelloFailedCode.OFPHFC_INCOMPATIBLE);
				fvError.setVersion(OFMessage.OFP_VERSION);
				String errmsg = "we only support version "
						+ Integer.toHexString(OFMessage.OFP_VERSION)
						+ " and you are not it";
				fvError.setError(errmsg.getBytes());
				fvError.setErrorIsAscii(true);
				fvError.setLength((short) FVError.MINIMUM_LENGTH);
				
				this.sendMsg(fvError, this);
				tearDown();
			}
			break;
		case ECHO_REQUEST:
			OFMessage echo_reply = new OFEchoReply();
			echo_reply.setXid(m.getXid());
			sendMsg(echo_reply, this);
			break;
		case FEATURES_REPLY:
			this.setSwitchInfo((OFFeaturesReply) m);
			/*
			 * OFStatisticsRequest stats = new OFStatisticsRequest();
			 * stats.setStatisticType(OFStatisticsType.DESC);
			 */
			switchName = "dpid=" + FlowSpaceUtil.dpidToString(this.getDPID());
			FVLog.log(LogLevel.INFO, this, "identified switch as " + switchName
					+ " on " + this.sock);
			FlowSpaceImpl.addListener(this); // register for FS updates
			SwitchImpl.addListener(this.getDPID(), this);
			this.connectToControllers(null); // connect to controllers
			// TODO create switch entry in db.
			doneID = true;
			updateFloodPerms();
			break;
		default:
			FVLog.log(LogLevel.WARN, this, "Got unknown message type " + m
					+ " to unidentified switch");
		}
	}

	/**
	 * Figure out which slices have access to the switch and spawn a Slicer
	 * EventHandler for each of them. Also, close the connection to any slice
	 * that is no longer listed
	 *
	 * Also make a connection for the topology discovery daemon here if
	 * configured
	 *
	 * Assumes The switch is already been identified;
	 *
	 */
	private void connectToControllers(FlowMap fm) {
		Set<String> newSlices;
		synchronized (FVConfig.class) {
			try {
				if (fm == null || fm.getType() == FlowMap.type.LINEAR)
					this.switchFlowMap = FlowSpaceUtil.getFlowMap(switchInfo.getDatapathId());
				else
					this.switchFlowMap = fm;
			} catch (ConfigError e) {
				FVLog.log(LogLevel.CRIT, this, "Unable to fetch Flow Space : " + e.getMessage());
				return;
			}
					/*FlowSpaceUtil.getSubFlowMap(this.switchInfo
					.getDatapathId());*/
			// this.switchFlowMap = FVConfig.getFlowSpaceFlowMap();
			newSlices = FlowSpaceUtil.getSlicesByDPID(this.switchFlowMap,
					this.switchInfo.getDatapathId());
		}
		StringBuffer strbuf = new StringBuffer();
		for (String sliceName : newSlices) {
			if (strbuf.length() > 0) // prune the last
				strbuf.append(',');
			strbuf.append(sliceName);
		}

		FVLog.log(LogLevel.DEBUG, this, "slices with access=", strbuf
				.toString());
		// foreach slice, make sure it has access to this switch
		for (String sliceName : newSlices) {
			if (slicerMap == null)
				throw new NullPointerException("slicerMap is null!?");
			if (!slicerMap.containsKey(sliceName)) {
				FVLog.log(LogLevel.INFO, this,
						"making new connection to slice " + sliceName);
				FVSlicer newSlicer = new FVSlicer(this.loop, this, sliceName);
				if (newSlicer.isUp()) {
					slicerMap.put(sliceName, newSlicer); // create new slicer in
					// this same EventLoop
					newSlicer.init(); // and start it up
				}
			} 
		}

		// foreach slice with previous access, make sure it still has access
		List<String> deletelist = new LinkedList<String>();
		for (String sliceName : slicerMap.keySet()) {
			if (!newSlices.contains(sliceName)) {
				// this slice no longer has access to this switch
				FVLog.log(LogLevel.INFO, this,
						"disconnecting: removed from FlowSpace: " + sliceName);
				slicerMap.get(sliceName).closeDown(false);
				deletelist.add(sliceName);
			}
		}
		// delete anything we marked in prev pass
		// should be able to do this in one loop, but can't
		// seem to iterate over a Map's keys and del inline
		boolean poll = false;
		for (String deleteSlice : deletelist) {
			slicerMap.remove(deleteSlice);
			poll |= cleanUpFlowMods(deleteSlice);
		}
		/*
		 * Poll the flowtable once rather than
		 * hammering it for every deleted slice.
		 */
		if (poll)
			pollFlowTableStats(null);
	}
	
	
	/*
	 * Clean up a deleted or downed slice. Take flows from cache 
	 * and build delete requests. If there are still flow mods in
	 * the table, re-poll the flowtable and do this again. The idea
	 * being that we do not want to poll the flowtable unnecessarily.
	 */
	private boolean cleanUpFlowMods(String deleteSlice) {
		List<Long> cookies = this.cookieTranslator.getCookieList(deleteSlice);
		ArrayList<FVFlowStatisticsReply> flows = getFlowStats(deleteSlice);
		for (FVFlowStatisticsReply reply : flows) {
			
			FVFlowMod delete = new FVFlowMod();
			delete.setActions(reply.getActions());
			delete.setBufferId(0);
			delete.setCommand(FVFlowMod.OFPFC_DELETE_STRICT);
			delete.setCookie(reply.getCookie());
			
			delete.setHardTimeout(reply.getHardTimeout());
			delete.setIdleTimeout(reply.getIdleTimeout());
			delete.setMatch(reply.getMatch());
			delete.setOutPort(OFPort.OFPP_NONE);
			delete.setPriority(reply.getPriority());
			
			cookieTranslator.untranslateAndRemove(reply.getTransCookie());
			cookies.remove(reply.getCookie());
			delete.setLengthU(FVFlowMod.MINIMUM_LENGTH);
			for (OFAction act : delete.getActions()) 
				delete.setLengthU(delete.getLengthU() + act.getLengthU());
			sendMsg(delete, this);
		}
		if (cookies.size() > 0) {
			toDeleteSlices.add(deleteSlice);
			return true;
		}
		return false;
	}

	public FlowMap getSwitchFlowMap() {
		return switchFlowMap;
	}

	public void setSwitchFlowMap(FlowMap switchFlowMap) {
		this.switchFlowMap = switchFlowMap;
	}

	/**
	 * Called by FVSlicer to tell us to forget about them
	 *
	 * @param sliceName
	 */
	public void tearDownSlice(String sliceName) {
		if (slicerMap != null) {
			slicerMap.remove(sliceName);
			FVLog.log(LogLevel.DEBUG, this, "tore down slice " + sliceName
					+ " on request");
		}
	}

	public String getSwitchName() {
		return this.switchName;
	}

	public String getConnectionName() {
		return FlowSpaceUtil.connectionToString(sock);
	}

	/**
	 * @return This switch's DPID
	 */
	public long getDPID() {
		if (this.switchInfo == null)
			return -1;
		return this.switchInfo.getDatapathId();
	}

	/**
	 * Send a message to the switch connected to this classifier
	 *
	 * @param msg
	 *            OFMessage
	 */

	public void sendMsg(OFMessage msg, FVSendMsg from) {
		if (this.msgStream != null) {
			FVLog.log(LogLevel.DEBUG, this, "send to switch:", msg);
			try {
				this.msgStream.testAndWrite(msg);
			} catch (BufferFull e) {
				FVLog.log(LogLevel.CRIT, this,
						"framing BUG; tearing down: got ", e);
				this.loop.queueEvent(new TearDownEvent(this, this));
				this.stats.increment(FVStatsType.DROP, from, msg);
			} catch (MalformedOFMessage e) {
				FVLog.log(LogLevel.CRIT, this, "BUG: bad msg: ", e);
				this.stats.increment(FVStatsType.DROP, from, msg);
			} catch (IOException e) {
				FVLog.log(LogLevel.WARN, this,
						"restarting connection, got IO error: ", e);
				this.tearDown();
			}
		} else {
			FVLog
					.log(LogLevel.WARN, this, "dropping msg: no connection: ",
							msg);
			this.stats.increment(FVStatsType.DROP, from, msg);
		}

	}

	public boolean isIdentified() {
		return this.switchInfo != null;
	}

	public Collection<FVSlicer> getSlicers() {
		// TODO: figure out if this is a copy and could have SYNCH issues
		return slicerMap.values();
	}

	@Override
	public void dropMsg(OFMessage msg, FVSendMsg from) {
		this.stats.increment(FVStatsType.DROP, from, msg);
	}

	@Override
	public SendRecvDropStats getStats() {
		return stats;
	}

	public void setFlowDB(FlowDB flowDB) {
		this.flowDB = flowDB;
	}

	public FlowDB getFlowDB() {
		return flowDB;
	}

	public SocketChannel getSocketChannel() {
		return this.sock;
	}

	public boolean wantStatsDescHack() {
		// TODO make this a configurable option
		return wantStatsDescHack;
	}
	
	public boolean isFlowTracking() {
		return flowTracking;
	}

	/**
	 * @return the floodPermsSlice
	 */
	public String getFloodPermsSlice() {
		return floodPermsSlice;
	}

	@Override
	public void processChange(ConfigurationEvent event) {
		event.invoke();
	}

	@Override
	public void flowMapChanged (FlowMap in) {
		
		// update ourselves first
		connectToControllers(in); // re-figure out who we should connect to
		// then tell everyone who depends on us (causality important :-)
		for (FVSlicer fvSlicer : slicerMap.values())
			fvSlicer.updateFlowSpace();
	}

	@Override
	public void setFlowTracking(Boolean in) {
		this.flowTracking  = in;
		if (in)
			this.flowDB = new LinearFlowDB(this);
		else
			this.flowDB = new NoopFlowDB();		
	}

	@Override
	public void setStatsDescHack(Boolean in) {
		this.wantStatsDescHack = in;
		
	}

	@Override
	public void setFloodPerm(String in) {
		this.floodPermsSlice = in;
		
	}

	@Override
	public void setFlowModLimit(HashMap<String, Object> in) {
		FVLog.log(LogLevel.DEBUG, null, "Setting limit to " + in.get("LIMIT"));
		fmlimits.put((String) in.get(Slice.SLICE), (Integer) in.get("LIMIT")); 
	}
	
	/*
	 * Uncomment below, if we want one ratelimiter 
	 * per slicer-classifier pair
	 */
	@Override
	public void setRateLimit(HashMap<String, Object> in) {
		Integer rateLimit = (Integer) in.get("RATELIMIT");
		if (rateLimit == -1) {
			slicerLimits.setRateLimiter(/*FlowSpaceUtil.dpidToString(this.getDPID()) + */(String)in.get(Slice.SLICE), 
					new TokenBucket());
		} else {
			slicerLimits.setRateLimiter(/*FlowSpaceUtil.dpidToString(this.getDPID()) + */(String)in.get(Slice.SLICE), 
					new TokenBucket(rateLimit, new FixedIntervalRefillStrategy(rateLimit, 1, TimeUnit.SECONDS)));
		}
		
	}
	
	
	
	public void incrementFlowMod(String sliceName) {
		Integer curr = currfmlimits.get(sliceName);
		if (curr == null)
			curr = 0;
		currfmlimits.put(sliceName, ++curr);
	}
	
	public void decrementFlowMod(String sliceName) {
		Integer curr = currfmlimits.get(sliceName);
		if (curr == null || curr <= 0)
			curr = 1;
		currfmlimits.put(sliceName, --curr);
	}
	
	public boolean permitFlowMod(String sliceName) {
		Integer limit = fmlimits.get(sliceName);
		Integer curr = currfmlimits.get(sliceName);
		FVLog.log(LogLevel.DEBUG,this, "Overall limit is " + limit + 
				" current value is " + curr);
		if (curr == null)
			curr = 0;
		currfmlimits.put(sliceName, curr);
		if (limit == -1)
			return true;
		return curr < limit;
	}

	public void loadLimit(String sliceName) {
		int limit = -1;
		try {
			limit = SwitchImpl.getProxy().getMaxFlowMods(sliceName, this.getDPID());
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, this, "Disabling dpid limits because I can't load it from the db.");
		}
		fmlimits.put(sliceName, limit);
	}
	
	public void loadRateLimit(String sliceName) {
		int limit = -1;
		try {
			limit = SwitchImpl.getProxy().getRateLimit(sliceName, this.getDPID());
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, this, "Disabling dpid limits because I can't load it from the db.");
		}
		if (limit == -1) {
			slicerLimits.setRateLimiter(/*FlowSpaceUtil.dpidToString(this.getDPID()) + */sliceName, 
					new TokenBucket());
		} else {
			slicerLimits.setRateLimiter(/*FlowSpaceUtil.dpidToString(this.getDPID()) + */sliceName, 
					new TokenBucket(200, new FixedIntervalRefillStrategy(limit, 1, TimeUnit.SECONDS)));
		}
	}

	public Integer getCurrentFlowModCounter(String sliceName) {
		Integer curr = currfmlimits.get(sliceName);
		if (curr == null)
			return 0;
		return curr;
	}
	
	public Integer getMaxAllowedFlowMods(String sliceName) {
		return fmlimits.get(sliceName);
	}

	public void setSlicerLimits(SlicerLimits slicerLimits) {
		this.slicerLimits = slicerLimits;
	}
	
	public SlicerLimits getSlicerLimits() {
		return this.slicerLimits;
	}
	
	public boolean isRateLimited(String sliceName) {
		return slicerLimits.getRateLimiter(/*FlowSpaceUtil.dpidToString(this.getDPID()) + */sliceName).consume();
	}
	
	private synchronized ArrayList<FVFlowStatisticsReply> getFlowStats(String sliceName) {
		ArrayList<FVFlowStatisticsReply> stats = new ArrayList<FVFlowStatisticsReply>();
		if (actualStats.get(sliceName) != null)
			stats.addAll(actualStats.get(sliceName));
		FVLog.log(LogLevel.DEBUG, null, actualStats.toString());
		return stats;
	}
	
	public void sendAggStatsResp(FVSlicer fvSlicer, FVStatisticsRequest original) {
		boolean found = false;
		FVAggregateStatisticsRequest orig = (FVAggregateStatisticsRequest) original.getStatistics().get(0);
	
		ArrayList<FVFlowStatisticsReply> replies = getFlowStats(fvSlicer.getSliceName());
		
		List<OFStatistics> stats = new LinkedList<OFStatistics>();
		FVStatisticsReply statsReply = new FVStatisticsReply();
		statsReply.setLengthU(FVStatisticsReply.MINIMUM_LENGTH);
		HashSet<Long> cookieTracker = new HashSet<Long>();
		FVAggregateStatisticsReply rep = new FVAggregateStatisticsReply();
		for (FVFlowStatisticsReply reply : replies) {
			if (new FVMatch(orig.getMatch()).subsumes(new FVMatch(reply.getMatch()))) {
				if (orig.getOutPort() == OFPort.OFPP_NONE.getValue() || 
						matchContainsPort(reply, orig.getOutPort())) {
					rep.setByteCount(rep.getByteCount() + reply.getByteCount());
					rep.setPacketCount(rep.getPacketCount() + reply.getPacketCount());
					cookieTracker.add(reply.getTransCookie());
					found = true;
				}
			}
		}
		if (!found) {
			FVLog.log(LogLevel.WARN, fvSlicer, "Stats request resulted in an empty set ", original);
			return;
		}
		
		
		
		rep.setFlowCount(cookieTracker.size());
		
		stats.add(rep);
		statsReply.setStatistics(stats);
		statsReply.setXid(original.getXid());
		statsReply.setLengthU(FVStatisticsReply.MINIMUM_LENGTH + rep.computeLength());
		statsReply.setVersion(original.getVersion());
		statsReply.setStatisticType(original.getStatisticType());
	
		if (statsReply.getStatistics().size() == 0) 
			FVLog.log(LogLevel.WARN, fvSlicer, "Stats request resulted in an empty set ", original);
		
		fvSlicer.sendMsg(statsReply, this);
		
	}

	private boolean matchContainsPort(FVFlowStatisticsReply reply, short outPort) {
		for (OFAction act : reply.getActions()) {
			if (act instanceof OFActionOutput) {
				OFActionOutput outact = (OFActionOutput) act;
				if (outact.getPort() == outPort)
					return true;
			}
		}
		return false;
	}

	public void sendFlowStatsResp(FVSlicer fvSlicer, FVStatisticsRequest original, short flag) {
		FVFlowStatisticsRequest orig = (FVFlowStatisticsRequest) original.getStatistics().get(0);
		
	
		// Don't think this is required as we only return whatever THAT slice pushed. Thanks to cookies!
	//	List<FlowIntersect> intersections = this.getSwitchFlowMap().intersects(this.getDPID(), new FVMatch(orig.getMatch()));
	
		ArrayList<FVFlowStatisticsReply> replies = getFlowStats(fvSlicer.getSliceName());
		
		List<OFStatistics> stats = new LinkedList<OFStatistics>();
		FVStatisticsReply statsReply = new FVStatisticsReply();
		statsReply.setLengthU(FVStatisticsReply.MINIMUM_LENGTH);
		for (FVFlowStatisticsReply reply : replies) {
			if (new FVMatch(orig.getMatch()).subsumes(new FVMatch(reply.getMatch()))) {
				if (orig.getOutPort() == OFPort.OFPP_NONE.getValue() ||
						matchContainsPort(reply, orig.getOutPort())) {	 
					FVLog.log(LogLevel.DEBUG, this, "Appending FlowStats reply: ", reply);
					stats.add(reply);
					statsReply.setLengthU(statsReply.getLength() + reply.computeLength());
				}
			}

		}
		statsReply.setStatistics(stats);
		statsReply.setFlags(flag);	
		statsReply.setXid(original.getXid());
		
		statsReply.setVersion(original.getVersion());
		statsReply.setStatisticType(original.getStatisticType());
	
		if (statsReply.getStatistics().size() == 0) 
			FVLog.log(LogLevel.WARN, fvSlicer, "Stats request resulted in an empty set ", original);
		
		fvSlicer.sendMsg(statsReply, this);
	
	}

	
	//public synchronized void classifyFlowStats(FVStatisticsReply fvStatisticsReply, HashMap<String,Object> cache) {
	public synchronized void classifyFlowStats(FVStatisticsReply fvStatisticsReply) {
		actualStats.clear();

		List<OFStatistics> stats = fvStatisticsReply.getStatistics();

		//Adding for registering a FlowTable
		if (this.registeredForFlowTable == true && !this.flowTableList.isEmpty()){
			FVLog.log(LogLevel.DEBUG, this, "Inside registeredForFlowTable ",this.registeredForFlowTable);
			
			HashMap <String,Object> cache = new HashMap<String,Object>();
			cache = FVFlowStatisticsReply.toMap(fvStatisticsReply, this.getDPID());
			
			for (FlowTableCallback fcb : this.flowTableList) {
				fcb.clearParams();
				fcb.setParams(cache);
				fcb.spawn(); // Is this ok to use run, there will be only one thread of fcb per classifier?
				
			}
		}
		
		for (OFStatistics s : stats) {
			FVFlowStatisticsReply stat = (FVFlowStatisticsReply) s;
			CookiePair pair = getCookieTranslator().untranslate(stat.getCookie());
			if (pair == null) {
				FVLog.log(LogLevel.WARN, this, "Unable to classify stats - ignoring - ", stat);
				continue;
			}
			stat.setTransCookie(stat.getCookie());
			stat.setCookie(pair.getCookie());
			addToFlowStats(stat, pair.getSliceName());
		}
		actualStats.putAll(flowStats);
		FVLog.log(LogLevel.DEBUG, this, " actualStats: ",actualStats.toString(), "flowStats: ", flowStats.toString()); 
		if ((fvStatisticsReply.getFlags() != OFStatisticsReplyFlags.REPLY_MORE.getTypeValue()) ){
			flowStats.clear();
		}
		for (String slice : toDeleteSlices) {
			cleanUpFlowMods(slice);
		}
		toDeleteSlices.clear();
		
	}
	
	private void addToFlowStats(FVFlowStatisticsReply stat, String sliceName) {
		ArrayList<FVFlowStatisticsReply> stats = flowStats.get(sliceName);
		if (stats == null) 
			stats = new ArrayList<FVFlowStatisticsReply>();
		stats.add(stat);
		flowStats.put(sliceName, stats);
	}
	
	public boolean pollFlowTableStats(FVStatisticsRequest orig) {
		if (!this.statsWindowOpen && orig != null)
			return this.statsWindowOpen;
		this.statsWindowOpen = false;
		FVStatisticsRequest request = new FVStatisticsRequest();
		request.setStatisticType(OFStatisticsType.FLOW);
		request.setType(OFType.STATS_REQUEST);
		
		
		FVFlowStatisticsRequest statsReq = new FVFlowStatisticsRequest();
		statsReq.setMatch(new FVMatch());
		statsReq.setOutPort(OFPort.OFPP_NONE.getValue());
		statsReq.setTableId((byte) 0xFF);
		List<OFStatistics> stats = new LinkedList<OFStatistics>();
		stats.add(statsReq);
		request.setStatistics(stats);
		request.setLengthU(FVStatisticsRequest.MINIMUM_LENGTH + statsReq.computeLength());
		request.setXid(orig == null ? -1 : orig.getXid());
		this.sendMsg(request, this);
		
		FVStatsTimer statsTimer = new FVStatsTimer(this);
		statsTimer.setExpireTime(System.currentTimeMillis() + FVStatsTimer.WAIT_TIME);
		loop.addTimer(statsTimer);
		return !this.statsWindowOpen;
	}

	public void registerCallBack(String userName, String url, String method, String cookie, TopologyCallback.EventType eventType, Long dpid) {
			this.registeredForFlowTable = true;
			pollFlowTableStats(null);
			this.flowTableList.add(new FlowTableCallback(userName,url,method,cookie,dpid));
	}
	
	public void deRegisterCallBack(String userName, String method,String cookie, TopologyCallback.EventType eventType, Long dpid){
		Iterator<FlowTableCallback> it = flowTableList.iterator();
		
		while (it.hasNext()) {
			FlowTableCallback callback = it.next();
			if (callback.getMethodName().equals(method) && callback.getCookie().equals(cookie) &&
					callback.getUser().equals(userName))
				it.remove();
		}		
	}

}
