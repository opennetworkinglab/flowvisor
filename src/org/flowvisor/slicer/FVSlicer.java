/**
 *
 */
package org.flowvisor.slicer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowvisor.api.TopologyCallback;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.FVSendMsg;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.ConfigurationEvent;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowvisorChangedListener;
import org.flowvisor.config.FlowvisorImpl;
import org.flowvisor.config.SliceChangedListener;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.events.OFKeepAlive;
import org.flowvisor.events.TearDownEvent;
import org.flowvisor.exceptions.BufferFull;
import org.flowvisor.exceptions.MalformedOFMessage;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowRewriteDB;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.LinearFlowRewriteDB;
import org.flowvisor.flows.NoopFlowRewriteDB;
import org.flowvisor.io.FVMessageAsyncStream;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.log.SendRecvDropStats.FVStatsType;
import org.flowvisor.message.FVMessageFactory;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVPacketOut;
import org.flowvisor.message.FVPortStatus;
import org.flowvisor.message.SanityCheckable;
import org.flowvisor.message.Slicable;
import org.flowvisor.ofswitch.TopologyController;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.OFType;
import org.openflow.util.LRULinkedHashMap;


/**
 * @author capveg
 * 
 */
public class FVSlicer implements FVEventHandler, FVSendMsg, FlowvisorChangedListener, SliceChangedListener {

	public static final int MessagesPerRead = 50; // for performance tuning
	String sliceName;
	FVClassifier fvClassifier;
	FVEventLoop loop;
	SocketChannel sock;
	String hostname;
	int reconnectSeconds;
	final int maxReconnectSeconds = 15;
	int port; // the tcp port of our controller
	boolean isConnected;
	int connectCount = 0;
	FVMessageAsyncStream msgStream;
	short missSendLength;
	boolean allowAllPorts;
	FlowMap localFlowSpace;
	boolean isShutdown;
	boolean lldpOptIn = true;
	OFKeepAlive keepAlive;
	SendRecvDropStats stats;
	FlowRewriteDB flowRewriteDB;
	boolean floodPerms;
	Map<Short, Boolean> allowedPorts; // ports in this slice and whether they
	boolean reconnectEventScheduled = false;
	LinkedHashMap<Integer, Integer> allowedBufferIDs; // LRU cached list of
														// buffer-IDs
	// that this slice can address
	final private int MAX_ALLOWED_BUFFER_IDS = 256; // max cache size
	
	private Integer fmlimit = -1;

	protected FVSlicer() {}
	// get OFPP_FLOOD'd

	public FVSlicer(FVEventLoop loop, FVClassifier fvClassifier,
			String sliceName) {
		this.loop = loop;
		this.fvClassifier = fvClassifier;
		this.sliceName = sliceName;
		this.isConnected = false;
		this.msgStream = null;
		this.missSendLength = 128; // openflow default (?) findout... TODO
		this.allowAllPorts = false;
		this.reconnectSeconds = 0;
		this.isShutdown = false;
		this.floodPerms = false;
		this.allowedPorts = new HashMap<Short, Boolean>();
		this.allowedBufferIDs = new LRULinkedHashMap<Integer, Integer>(10,
				MAX_ALLOWED_BUFFER_IDS);
		this.stats = SendRecvDropStats.createSharedStats(sliceName);
		
		
		FlowvisorImpl.addListener(this);//FVConfig.watch(this, FVConfig.FLOW_TRACKING);
		try {
			setFlowTracking(FlowvisorImpl.getProxy().gettrack_flows());
		} catch (ConfigError e) {
			setFlowTracking(false);
		}
		//updateFlowTrackingConfig();
	}

	/*private void updateFlowTrackingConfig() {
		try {
			if (FVConfig.getBoolean(FVConfig.FLOW_TRACKING)) {
				this.flowRewriteDB = new LinearFlowRewriteDB(this,
						this.sliceName, fvClassifier.getDPID());
			} else {
				this.flowRewriteDB = new NoopFlowRewriteDB(this,
						this.sliceName, fvClassifier.getDPID());
			}
		} catch (ConfigError e) {
			// default to flow_tracking == off
			this.flowRewriteDB = new NoopFlowRewriteDB(this, this.sliceName,
					fvClassifier.getDPID());
		}
	}*/

	public void init() {
		FVLog.log(LogLevel.DEBUG, this, "initializing new FVSlicer");
		
		// snag controller info from config
		try {
			hostname = FVConfig.getSliceHost(sliceName);
			port = FVConfig.getSlicePort(sliceName); 
			lldpOptIn = FVConfig.getLLDPSpam(sliceName);
			SliceImpl.addListener(sliceName, this);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.CRIT, this, "ignoring slice ", sliceName,
					" malformed slice definition: ", e);
			this.tearDown();
			return;
		}
		this.updatePortList();
		this.reconnect();
		this.keepAlive = new OFKeepAlive(this, this, loop);
		this.keepAlive.scheduleNextCheck();
		fvClassifier.loadLimit(sliceName);
		fvClassifier.loadRateLimit(sliceName);
		try {
			this.fmlimit = SliceImpl.getProxy().getMaxFlowMods(sliceName);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, this, "Global slice flow mod limit unreadable; disabling.");
			this.fmlimit = -1;
		}
	}

	private FlowMap getLocalFlowSpace() {
		FlowMap fm = this.fvClassifier.getSwitchFlowMap();
		switch (fm.getType()) {
		case LINEAR:
			return fm.clone();
		case FEDERATED:
			return fm;
		}
		FVLog.log(LogLevel.FATAL, null,
				"Something is fatally wrong with the flowmap. Cannot determine its type.");
		return null;
	}

	private void updatePortList() {
		ArrayList<Short> addedPorts = new ArrayList<Short>();
		ArrayList<Short> removedPorts = new ArrayList<Short>();
		synchronized (FVConfig.class) {
			// update our local copy
			this.localFlowSpace = getLocalFlowSpace();
		}
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(this.fvClassifier
				.getSwitchInfo().getDatapathId(), this.sliceName,
				this.localFlowSpace);
		if (ports.contains(OFPort.OFPP_ALL.getValue())) {
			// this switch has access to ALL PORTS; feed them in from the
			// features request
			ports.clear(); // remove the OFPP_ALL virtual port
			this.allowAllPorts = true;
			for (OFPhysicalPort phyPort : this.fvClassifier.getSwitchInfo()
					.getPorts())
				ports.add(phyPort.getPortNumber());
		}
		for (Short port : ports) {
			if (!allowedPorts.keySet().contains(port)) {
				FVLog.log(LogLevel.DEBUG, this, "adding access to port ", port);
				allowedPorts.put(port, Boolean.TRUE);
				addedPorts.add(port);
			}
		}
		for (Iterator<Short> it = allowedPorts.keySet().iterator(); it
				.hasNext();) {
			Short port = it.next();
			if (!ports.contains(port)) {
				FVLog.log(LogLevel.DEBUG, this, "removing access to port ",
						port);
				it.remove();
				removedPorts.add(port);
			}
		}
		updatePortStatus(addedPorts, removedPorts);
	}

	private void updatePortStatus(ArrayList<Short> addedPorts,
			ArrayList<Short> removedPorts) {
		for (Short port : addedPorts) {
			OFPhysicalPort phyPort = findPhyPort(port);
			if (phyPort != null)
				sendPortStatusUpdate(phyPort, true);
		}
		for (Short port : removedPorts) {
			OFPhysicalPort phyPort = findPhyPort(port);
			if (phyPort != null)
				sendPortStatusUpdate(phyPort, false);
		}
	}

	private void sendPortStatusUpdate(OFPhysicalPort phyPort, boolean added) {
		FVPortStatus portStatus = new FVPortStatus();
		portStatus.setDesc(phyPort);
		portStatus.setReason(added ? (byte) OFPortReason.OFPPR_ADD.ordinal() : 
			(byte) OFPortReason.OFPPR_DELETE.ordinal());
		FVLog.log(LogLevel.INFO, this, (added ? "added " : "removed ") + "port " + phyPort.getPortNumber());
		sendMsg(portStatus, this);
	}

	private OFPhysicalPort findPhyPort(Short port) {
		for (OFPhysicalPort phyPort : this.fvClassifier.getSwitchInfo()
				.getPorts()) {
			if (phyPort.getPortNumber() == port)
				return phyPort;
		}
		return null;
	}
	/**
	 * Return the list of ports in this slice on this switch
	 * 
	 * @return
	 */
	public Set<Short> getPorts() {
		return this.allowedPorts.keySet();
	}

	/**
	 * Return the list of ports that have flooding enabled for OFPP_FLOOD
	 * 
	 * @return
	 */
	public Set<Short> getFloodPorts() {
		Set<Short> floodPorts = new LinkedHashSet<Short>();
		for (Short port : this.allowedPorts.keySet())
			if (this.allowedPorts.get(port))
				floodPorts.add(port);
		return floodPorts;
	}

	public boolean isAllowAllPorts() {
		return allowAllPorts;
	}

	/**
	 * @return the missSendLength
	 */
	public short getMissSendLength() {
		return missSendLength;
	}

	/**
	 * @param missSendLength
	 *            the missSendLength to set
	 */
	public void setMissSendLength(short missSendLength) {
		this.missSendLength = missSendLength;
	}

	/**
	 * Set the OFPP_FLOOD flag for this port silently fail if this port is not
	 * in the slice
	 * 
	 * @param port
	 * @param status
	 */

	public void setFloodPortStatus(Short port, Boolean status) {
		if (this.allowedPorts.containsKey(port))
			this.allowedPorts.put(port, status);
	}

	/**
	 * Is this port in this slice on this switch?
	 * 
	 * @param port
	 * @return true is yes, false is no.. durh
	 */
	public boolean portInSlice(Short port) {
		return (this.allowAllPorts || this.allowedPorts.containsKey(port));
	}

	@Override
	public void sendMsg(OFMessage msg, FVSendMsg from) {
		if (this.msgStream != null) {
			FVLog.log(LogLevel.DEBUG, this, "send to controller: ", msg);
			try {
				this.msgStream.testAndWrite(msg);
			} catch (BufferFull e) {
				FVLog.log(LogLevel.CRIT, this,
						"buffer full: tearing down: got ", e,
						": resetting connection");
				this.reconnectLater();
			} catch (MalformedOFMessage e) {
				this.stats.increment(FVStatsType.DROP, from, msg);
				FVLog.log(LogLevel.CRIT, this, "BUG: ", e);
			} catch (IOException e) {
				FVLog.log(LogLevel.WARN, this, "reconnection; got IO error: ",
						e);
				this.reconnectLater();
			}
		} else {
			this.stats.increment(FVStatsType.DROP, from, msg);
			FVLog.log(LogLevel.WARN, this,
					"dropping msg: controller not connected: ", msg);
		}
	}

	@Override
	public void dropMsg(OFMessage msg, FVSendMsg from) {
		this.stats.increment(FVStatsType.DROP, from, msg);
	}

	@Override
	public boolean needsConnect() {
		return !this.isConnected; // want connect events if we're not connected
	}

	@Override
	public boolean needsRead() {
		return this.isConnected; // want read events if we are connected
	}

	@Override
	public boolean needsWrite() {
		if (this.msgStream == null) // want write events if msgStream wants them
			return false;
		return this.msgStream.needsFlush();
	}

	@Override
	public boolean needsAccept() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#getName()
	 */
	@Override
	public String getName() {
		return new StringBuilder("slicer_").append(this.sliceName).append("_")
				.append(fvClassifier.getSwitchName()).toString();
	}
	


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#getThreadContext()
	 */
	@Override
	public long getThreadContext() {
		return loop.getThreadContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#tearDown()
	 */
	@Override
	public void tearDown() {
		closeDown(true);
	}
	
	public int getConnectCount() {
		return connectCount;
	}

	public void closeDown(boolean unregisterClassifier) {
		FVLog.log(LogLevel.DEBUG, this, "tearing down");
		this.isShutdown = true;
		this.loop.unregister(this.sock, this);
		if (this.sock != null) {
			try {
				this.sock.close(); // FIXME will this also cancel() the key in
				// the event loop?
			} catch (IOException e) {
				// ignore if error... we're shutting down already
			}
		}
		// tell the classifier to forget about us
		if (unregisterClassifier)
			fvClassifier.tearDownSlice(this.sliceName);

		this.msgStream = null; // force this to GC, in case we have a memleak on
		// "this"

		HashMap<String, Object> info = this.getStatusInfo();
		TopologyController tc = TopologyController.getRunningInstance();
		if (tc != null)
			tc.sliceConnectionJustChanged(info, TopologyCallback.EventType.SLICE_DISCONNECTED);
		SliceImpl.removeListener(sliceName, this);
		FlowvisorImpl.removeListener(this);
	}
	
	private HashMap<String, Object> getStatusInfo() {
		HashMap<String, Object> info = new HashMap<String, Object>();
		info.put("connection-status", this.isConnected());
		info.put("slice-name", this.sliceName);
		info.put("controller-host", this.hostname);
		info.put("controller-port", this.port);
		info.put("shutdown-status", this.isShutdown);
		info.put("floodperms", this.floodPerms);
		FVLog.log(LogLevel.DEBUG, this, "FVSlicer StatusInfo: " + info);
		return info;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.flowvisor.events.FVEventHandler#handleEvent(org.flowvisor.events.
	 * FVEvent)
	 */
	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (isShutdown) {
			FVLog.log(LogLevel.WARN, this, "is shutdown; ignoring: " + e);
			return; // don't process any events after shutdown
		}
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else if (e instanceof OFKeepAlive)
			handleKeepAlive(e);
		else if (e instanceof ReconnectEvent) {
			this.reconnectEventScheduled = false;
			this.reconnect();
		} else if (e instanceof TearDownEvent)
			this.tearDown();
		else
			throw new UnhandledEvent(e);
	}

	private void handleKeepAlive(FVEvent e) {
		if (!this.keepAlive.isAlive()) {
			FVLog.log(LogLevel.WARN, this,
					"keepAlive timeout; trying to reconnnect later");
			try {
				if (this.sock != null)
					this.sock.close();
			} catch (IOException e1) {
				FVLog.log(LogLevel.WARN, this,
						"ignoring error while closing socket: ", e1);
			}
			this.reconnectLater();
			return;
		}
		this.keepAlive.sendPing();
		this.keepAlive.scheduleNextCheck();
	}

	public void registerPong() {
		this.keepAlive.registerPong();
	}

	private void reconnect() {
		FVLog.log(LogLevel.INFO, this, "trying to reconnect to ",
				this.hostname, ":", this.port);
		// reset our state to unconnected (might be a NOOP)
		this.isConnected = false;
		this.msgStream = null;
		// try to connect socket to controller
		try {
			if (this.sock != null)
				// note that this automatically unregisters from selector
				this.sock.close();
			this.sock = SocketChannel.open();
			sock.configureBlocking(false); // set to non-blocking
			InetSocketAddress addr = new InetSocketAddress(hostname, port);
			if (addr.isUnresolved()) {
				FVLog.log(LogLevel.INFO, this,
						"retrying: failed to resolve hostname: ", hostname);
				this.reconnectLater();
				return;
			}
			this.isConnected = this.sock.connect(addr); // try to connect
			// register into event loop
			this.loop.register(this.sock, SelectionKey.OP_CONNECT, this);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this,
					"Trying to reconnect; trying later; got : ", e);
			this.reconnectLater();
		}

	}

	private void handleIOEvent(FVIOEvent e) {
		if (!this.isConnected) {
			try {
				if (!this.sock.finishConnect())
					return; // not done yet

			} catch (IOException e1) {
				FVLog.log(LogLevel.DEBUG, this, "retrying connection in ",
						this.reconnectSeconds, " seconds; got: ", e1);
				this.reconnectLater();
				return;
			}
			FVLog.log(LogLevel.DEBUG, this, "connected");
			this.isConnected = true;
			this.connectCount++;
			HashMap<String, Object> info = this.getStatusInfo();
			TopologyController tc = TopologyController.getRunningInstance();
			if (tc != null)
				tc.sliceConnectionJustChanged(info, TopologyCallback.EventType.SLICE_CONNECTED);
			this.reconnectSeconds = 0;
			try {
				msgStream = new FVMessageAsyncStream(this.sock,
						new FVMessageFactory(), this, this.stats);
			} catch (IOException e1) {
				FVLog.log(
						LogLevel.WARN,
						this,
						"Trying again later; while creating OFMessageAsyncStream, got: ",
						e1);
				this.reconnectLater();
				return;
			}
			sendMsg(new OFHello(), this); // send initial handshake
		}
		try {
			if (msgStream.needsFlush()) // flush any pending messages
				msgStream.flush();
			List<OFMessage> msgs = this.msgStream.read();
			// .read(FVSlicer.MessagesPerRead); // read any new
			// messages
			if (msgs == null)
				throw new IOException("got null from read()");
			for (OFMessage msg : msgs) {
				FVLog.log(LogLevel.DEBUG, this, "recv from controller: ", msg);
				this.stats.increment(FVStatsType.SEND, this, msg);
				if ((msg instanceof SanityCheckable)
						&& (!((SanityCheckable) msg).isSane())) {
					FVLog.log(LogLevel.CRIT, this,
							"msg failed sanity check; dropping: " + msg);
					continue;
				}
				if (msg instanceof Slicable ) {
					// mark this channel as still alive
					this.keepAlive.registerPong();
					if (msg.getType() != OFType.HELLO && !fvClassifier.isRateLimited(this.getSliceName())) {
						FVLog.log(LogLevel.WARN, this,
								"dropping msg because slice", this.getSliceName(), " is rate limited: ",
								msg);
						this.sendMsg(FVMessageUtil.makeErrorMsg(OFBadRequestCode.OFPBRC_EPERM, msg), this);
						
						continue;
					}
					((Slicable) msg).sliceFromController(fvClassifier, this);
					
				} else
					FVLog.log(LogLevel.CRIT, this,
							"dropping msg that doesn't implement classify: ",
							msg);
			}
		} catch (IOException e1) {
			FVLog.log(LogLevel.WARN, this,
					"got i/o error; tearing down and reconnecting: ", e1);
			reconnect();
		} catch (Exception e2) {
			e2.printStackTrace();
			FVLog.log(LogLevel.ALERT, this,
					"got unknown error; tearing down and reconnecting: " , e2);
			
			reconnect();
		}
		// no need to setup for next select; done in eventloop
	}

	private void reconnectLater() {
		if (this.sock != null)
			try {
				this.sock.close();
				this.sock = null;
				this.isConnected = false;
				HashMap<String, Object> info = this.getStatusInfo();
				TopologyController tc = TopologyController.getRunningInstance();
				if (tc != null)
					tc.sliceConnectionJustChanged(info, TopologyCallback.EventType.SLICE_DISCONNECTED);
				/*
				 * Place slice connection callback here
				 */
			} catch (IOException e) {
				FVLog.log(LogLevel.WARN, this,
						"ignoring error closing socket: ", e);
			}
		if (this.reconnectEventScheduled) {
			// Don't schedule another reconnect, one's already in there
			return;
		}

		// exponential back off
		this.reconnectSeconds = Math.min(2 * this.reconnectSeconds + 1,
				this.maxReconnectSeconds);
		this.loop.addTimer(new ReconnectEvent(this.reconnectSeconds, this));
		this.reconnectEventScheduled = true;
	}

	public String getSliceName() {
		return this.sliceName;
	}

	public boolean getFloodPortStatus(short port) {
		return this.allowedPorts.get(port);
	}

	public FlowMap getFlowSpace() {
		return this.localFlowSpace;
	}

	@Override
	public String getConnectionName() {
		if (isConnected)
			return FlowSpaceUtil.connectionToString(sock);
		else
			return "NONE (retry in " + this.reconnectSeconds
					+ " seconds: max + " + this.maxReconnectSeconds + ")";
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	@Override
	public SendRecvDropStats getStats() {
		return stats;
	}

	/**
	 * @return the flowRewriteDB
	 */
	public FlowRewriteDB getFlowRewriteDB() {
		return flowRewriteDB;
	}

	/**
	 * @param flowRewriteDB
	 *            the flowRewriteDB to set
	 */
	public void setFlowRewriteDB(FlowRewriteDB flowRewriteDB) {
		this.flowRewriteDB = flowRewriteDB;
	}

	/**
	 * @return the floodPerms
	 */
	public boolean hasFloodPerms() {
		return floodPerms;
	}
	
	/**
	 * @return whether the slice is opted in for lldp messages that aren't 
	 * from it's slice
	 */
	public boolean isOptedIn() {
		return lldpOptIn;
	}

	/**
	 * @param floodPerms
	 *            the floodPerms to set
	 */
	public void setFloodPerms(boolean floodPerms) {
		this.floodPerms = floodPerms;
	}

	/**
	 * Check to see if this buffer_id is in the list of IDs that this slice is
	 * allowed to address
	 * 
	 * if bufferID==NONE, then always allow
	 * 
	 * if yes, for fun, increment a counter to see how often the buffer is
	 * referenced
	 * 
	 * @param bufferID
	 *            from, e.g., FVPacketOut->bufferID
	 * @return
	 */

	public boolean isBufferIDAllowed(int bufferID) {
		if (bufferID == FVPacketOut.BUFFER_ID_NONE)
			return true; // always allowed
		Integer count = this.allowedBufferIDs.get(bufferID);
		if (count == null)
			return false; // not allowed
		this.allowedBufferIDs.put(bufferID, count + 1);
		return true;
	}

	/**
	 * Mark this bufferID as allowed and init its access count to zero
	 * 
	 * @param bufferID
	 *            from, e.g., FVPacketIn->bufferID
	 */

	public void setBufferIDAllowed(int bufferID) {
		FVLog.log(LogLevel.DEBUG, this, "allowing bufferID: ", bufferID);
		this.allowedBufferIDs.put(bufferID, 0);
	}


	
	/*
	 * Aweful temporary fix to support linear flowmap copying....
	 * this sux, can't stress it enough. 
	 * 
	 * Hopefully one day we will remove the need to copy 
	 * the linear flow space all over the place!
	 * 
	 * Called by FVClassifier when he updates his flowspace.
	 */
	
	public void updateFlowSpace() {
		updatePortList();
		/*
		 * FIXME: need to flush old flow entries
		 */
	}

	@Override
	public void processChange(ConfigurationEvent event) {
		event.invoke();
	}

	@Override
	public void setFlowTracking(Boolean in) {
		
		if (in) {
			this.flowRewriteDB = new LinearFlowRewriteDB(this,
					this.sliceName, fvClassifier.getDPID());
		} else {
			this.flowRewriteDB = new NoopFlowRewriteDB(this,
					this.sliceName, fvClassifier.getDPID());
		}
		
		
	}
	
	@Override
	public void setControllerHost(String in) {
		this.hostname = in;
	}

	@Override
	public void setControllerPort(Integer in) {
		this.port = in;
	}

	@Override
	public void setStatsDescHack(Boolean in) {}

	@Override
	public void setFloodPerm(String in) {}

	@Override
	public void setLLDP(Boolean in) {
		lldpOptIn = in;
	}

	@Override
	public void setDropPolicy(String in) {}

	@Override                                      
	public void setFlowModLimit(Integer in) {
		this.fmlimit = in;
		
	}

	public void incrementFlowRules(){
		fvClassifier.incrementFlowMod(sliceName);
		fvClassifier.getSlicerLimits().incrementSliceFMCounter(sliceName);
	}
		
	public void decrementFlowRules(){
		fvClassifier.decrementFlowMod(sliceName);
		fvClassifier.getSlicerLimits().decrementSliceFMCounter(sliceName);
	}
	
	public boolean permitFlowMod() {
		if (fmlimit == -1)
			return true && fvClassifier.permitFlowMod(sliceName);
		int currlimit = fvClassifier.getSlicerLimits().getSliceFMLimit(sliceName);
		return ((currlimit < fmlimit) && (fvClassifier.permitFlowMod(sliceName)));
	}

	public boolean isUp() {
		return SliceImpl.getProxy().isSliceUp(this.sliceName);
	}



}
