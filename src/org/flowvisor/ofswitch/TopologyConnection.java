/**
 *
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.FVSendMsg;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.events.FVTimerEvent;
import org.flowvisor.events.TearDownEvent;
import org.flowvisor.exceptions.BufferFull;
import org.flowvisor.exceptions.MalformedOFMessage;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.io.FVMessageAsyncStream;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.log.SendRecvDropStats.FVStatsType;
import org.flowvisor.message.FVFeaturesReply;
import org.flowvisor.message.FVMessageFactory;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.TopologyControllable;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.message.statistics.FVDescriptionStatistics;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.openflow.util.StringByteSerializer;

/**
 * Divide ports on a switch in to "slowPorts" and "fastPorts" send topology
 * discovery probes the fastPorts more often then slowports if we get an lldp
 * message on a port, make it a fast port
 *
 * @author capveg
 *
 */
public class TopologyConnection implements FVEventHandler, FVSendMsg {

	private static final int LLDPLen = 128;
	TopologyController topologyController;
	FVEventLoop pollLoop;
	SocketChannel sock;
	String name;
	FVMessageAsyncStream msgStream;
	FVMessageFactory fvMessageFactory;
	FVFeaturesReply featuresReply;
	FVDescriptionStatistics descriptionStatistics;
	private boolean isShutdown;
	private final long fastProbeRate;
	private final long probesPerPeriod; // this is the safety rate: this many
	private final Set<Short> slowPorts;
	private final Set<Short> fastPorts;
	private Iterator<Short> slowIterator;
	private final Map<Short, OFPhysicalPort> phyMap;
	static final byte lldpSysD[] = { 0x0c, 0x08 }; // Type 6, length 8 
	SendRecvDropStats stats;
	public final static int FLOWNAMELEN_LEN = 1;
	public final static int FLOWNAMELEN_NULL = 1;
	public final static byte OUI_TYPE = 127;
	// probes can be dropped before a link
	// down event

	public TopologyConnection(TopologyController topologyController,
			FVEventLoop pollLoop, SocketChannel sock) {
		this.topologyController = topologyController;
		this.pollLoop = pollLoop;
		this.sock = sock;
		this.name = "topo." + sock.toString();
		this.featuresReply = null;
		this.descriptionStatistics = null;
		this.fvMessageFactory = new FVMessageFactory();
		this.stats = SendRecvDropStats.createSharedStats("topo");
		try {
			this.msgStream = new FVMessageAsyncStream(sock,
					this.fvMessageFactory, this, this.stats);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "IOException in constructor!");
			e.printStackTrace();
		}
		this.probesPerPeriod = 3;
		this.fastProbeRate = this.topologyController.getUpdatePeriod()
				/ this.getProbesPerPeriod();
		this.slowPorts = new HashSet<Short>();
		this.fastPorts = new HashSet<Short>();
		this.phyMap = new HashMap<Short, OFPhysicalPort>();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.events.FVEventHandler#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.events.FVEventHandler#getThreadContext()
	 */
	@Override
	public long getThreadContext() {
		return Thread.currentThread().getId();
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
		if (this.isShutdown)
			return; // ignore events if we've done the teardown
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else if (e instanceof FVTimerEvent)
			handleTimerEvent((FVTimerEvent) e);
		else
			throw new UnhandledEvent(e);
	}

	/*
	 * Handle a timer event
	 *
	 * On each timer event:<br>
	 *
	 * <ul> <li> send a probe to each fast Port
	 *
	 * <li> send a probe to the next slow port
	 *
	 * <li> reschedule timer
	 *
	 * </ul>
	 */
	synchronized private void handleTimerEvent(FVTimerEvent e) {
		FVLog.log(LogLevel.DEBUG, this, "sending probes");
		// send a probe per fast port
		for (Iterator<Short> fastIterator = this.fastPorts.iterator(); fastIterator
				.hasNext();) {
			Short port = fastIterator.next();
			FVLog.log(LogLevel.DEBUG, this, "sending fast probe to port "
					+ port);
			sendLLDP(this.phyMap.get(port));
		}
		// send a probe for the next slow port
		if (this.slowPorts.size() > 0) {
			if (!this.slowIterator.hasNext())
				this.slowIterator = this.slowPorts.iterator();
			if (this.slowIterator.hasNext()) {
				short port = this.slowIterator.next();
				sendLLDP(this.phyMap.get(port));
				FVLog.log(LogLevel.DEBUG, this, "sending slow probe to port "
						+ port);
			}

		}
		// reschedule timer
		this.pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.fastProbeRate, this, this, null));
	}

	private void handleIOEvent(FVIOEvent e) {
		int ops = e.getSelectionKey().readyOps();

		try {
			// read stuff, if need be
			if ((ops & SelectionKey.OP_READ) != 0) {
				List<OFMessage> newMsgs = msgStream.read();
				if (newMsgs != null) {
					for (OFMessage m : newMsgs) {
						FVLog.log(LogLevel.DEBUG, this, "read " + m);
						if (m instanceof TopologyControllable)
							((TopologyControllable) m).topologyController(this);
						else
							FVLog.log(LogLevel.WARN, this,
									"ignoring unhandled msg: " + m);
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
					"got IO exception; closing because : " + e1);
			this.tearDown();
			return;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.events.FVEventHandler#needsAccept()
	 */
	@Override
	public boolean needsAccept() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.events.FVEventHandler#needsConnect()
	 */
	@Override
	public boolean needsConnect() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.events.FVEventHandler#needsRead()
	 */
	@Override
	public boolean needsRead() {
		return true; // always interested if there is something to read
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.events.FVEventHandler#needsWrite()
	 */
	@Override
	public boolean needsWrite() {
		if (this.msgStream == null)
			return false;
		else
			return msgStream.needsFlush();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.events.FVEventHandler#tearDown()
	 */
	@Override
	public void tearDown() {
		try {
			sock.close();
			this.isShutdown = true;
			FVLog.log(LogLevel.WARN, this, "shutting down");
			this.topologyController.disconnect(this);
		} catch (IOException e) {
			FVLog.log(LogLevel.ALERT, this, "ignoring error on shutdown: " + e);
		}
	}

	/**
	 * Setup the connection
	 *
	 * Queue up OFHello(), OFFeatureRequest(), and Stats Desc request messages
	 *
	 * @throws IOException
	 */
	public void init() throws IOException {
		msgStream.write(this.fvMessageFactory.getMessage(OFType.HELLO));
		msgStream.write(this.fvMessageFactory
				.getMessage(OFType.FEATURES_REQUEST));

		// build stats desc request : FIXME: make this cleaner
		OFStatisticsRequest request = (OFStatisticsRequest) this.fvMessageFactory
				.getMessage(OFType.STATS_REQUEST);
		request.setStatisticType(OFStatisticsType.DESC);
		/*
		 * List<OFStatistics> statistics = new LinkedList<OFStatistics>();
		 * statistics.add(this.fvMessageFactory.getStatistics(
		 * OFType.STATS_REQUEST, OFStatisticsType.DESC));
		 * request.setStatistics(statistics);
		 */
		msgStream.write(request);

		msgStream.flush();
		int ops = SelectionKey.OP_READ;
		if (msgStream.needsFlush())
			ops |= SelectionKey.OP_WRITE;
		this.pollLoop.register(sock, ops, this);
	}

	/**
	 * Return whether this topology instance is fully connected and initialized.
	 *
	 * Specifically, do we have responses to the initial features request and
	 * stats.desc requests
	 *
	 * @return
	 */

	public boolean isConnected() {
		return this.featuresReply != null && this.descriptionStatistics != null;
	}

	/**
	 * Return the DPID of the switch associated with this TopologyConnection
	 * instance
	 *
	 * @return null if featuresReply not yet received
	 */
	public Long getDataPathID() {
		if (this.featuresReply == null)
			return null;
		else
			return featuresReply.getDatapathId();
	}

	/**
	 * @return the featuresReply
	 */
	public FVFeaturesReply getFeaturesReply() {
		return featuresReply;
	}

	/**
	 * @param featuresReply
	 *            the featuresReply to set
	 */
	public void setFeaturesReply(FVFeaturesReply featuresReply) {
		FVLog.log(LogLevel.DEBUG, this, "got featuresReply: " + featuresReply);
		boolean wasConnected = this.isConnected();
		this.featuresReply = featuresReply;
		if (isConnected() && !wasConnected)
			this.doJustConnected();
	}

	/**
	 * @return the descriptionStatistics
	 */
	public FVDescriptionStatistics getDescriptionStatistics() {
		return descriptionStatistics;
	}

	/**
	 * @param descriptionStatistics
	 *            the descriptionStatistics to set
	 */
	public void setDescriptionStatistics(
			FVDescriptionStatistics descriptionStatistics) {
		boolean wasConnected = this.isConnected();
		FVLog.log(LogLevel.DEBUG, this, "got descStats: "
				+ descriptionStatistics);
		this.descriptionStatistics = descriptionStatistics;
		if (isConnected() && !wasConnected)
			this.doJustConnected();
	}

	private void doJustConnected() {
		this.name = "topoDpid="
				+ HexString.toHexString(this.featuresReply.getDatapathId());
		FVLog.log(LogLevel.INFO, this, "starting topo discover: fasttimer = "
				+ this.fastProbeRate);
		// just one time; the timer event will cause them more often
		List<OFPhysicalPort> ports = featuresReply.getPorts();
		if (ports.size() < 1)
			FVLog.log(LogLevel.WARN, this, "got switch with no ports!?!");

		for (OFPhysicalPort port : ports)
			this.addPort(port);

		// schedule timer
		this.pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.fastProbeRate, this, this, null));

		topologyController.topoConnectionJustConnected(HexString.toHexString(this.featuresReply.getDatapathId()));
	}

	synchronized public void addPort(OFPhysicalPort port) {
		// this function is synchronized so it shouldn't get hosed
		FVLog.log(LogLevel.DEBUG, this, "sending init probe to port "
				+ port.getPortNumber());
		sendLLDP(port);
		this.slowPorts.add(Short.valueOf(port.getPortNumber()));
		this.phyMap.put(Short.valueOf(port.getPortNumber()), port);
		this.slowIterator = this.slowPorts.iterator();

	}

	synchronized public void removePort(OFPhysicalPort port) {
		// this function is synchronized so it shouldn't get hosed
		if (this.slowPorts.contains(port)) {
			this.slowPorts.remove(port);
			this.slowIterator = this.slowPorts.iterator();
		} else if (this.fastPorts.contains(port)) {
			this.fastPorts.remove(port);
			// no iterator to update
		} else
			FVLog.log(LogLevel.WARN, this,
					"tried to dynamically remove non-existant port: "
							+ port.getPortNumber());

	}

	private void sendLLDP(OFPhysicalPort port) {
		OFPacketOut packetOut = (OFPacketOut) this.fvMessageFactory
				.getMessage(OFType.PACKET_OUT);
		packetOut.setBufferId(-1);
		List<OFAction> actionsList = new LinkedList<OFAction>();
		OFActionOutput out = (OFActionOutput) this.fvMessageFactory
				.getAction(OFActionType.OUTPUT);
		out.setPort(port.getPortNumber());
		actionsList.add(out);
		packetOut.setActions(actionsList);
		short alen = FVMessageUtil.countActionsLen(actionsList);
		byte[] lldp = makeLLDP(port.getPortNumber(), port.getHardwareAddress());
		packetOut.setActionsLength(alen);
		packetOut.setPacketData(lldp);
		packetOut
				.setLength((short) (OFPacketOut.MINIMUM_LENGTH + alen + lldp.length));
		try {
			this.msgStream.testAndWrite(packetOut);
		} catch (BufferFull e) {
			FVLog.log(LogLevel.CRIT, this, "failed to write LLDP:", e);
		} catch (MalformedOFMessage e) {
			FVLog.log(LogLevel.CRIT, this, "failed to write LLDP:", e);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "failed to write LLDP:", e);
		}
	}

	private byte[] makeLLDP(short portNumber, byte[] hardwareAddress) {
		/**
		 * TODO: merge this with the code in LLDPUtil.java
		 */
		int size = LLDPLen; // needs to be some minsize to avoid ethernet
		// problems
		byte[] buf = new byte[size];
		ByteBuffer bb = ByteBuffer.wrap(buf);

		// LLDP Framing
		bb.put(LLDPUtil.LLDP_MULTICAST); // dst addr
		bb.put(hardwareAddress); // src addr
		bb.putShort(LLDPUtil.ETHER_LLDP);

		// TLV type is 7 bits, length is 9
		// I precomputed them on byte boundaries to save time and sanity

		// NOX only supports Chassis ID subtype 1 and Port ID subtype 2
		// which we can't jam a full datapath ID into.
		// So we have to supply those here, and then overload the
		// System Decription TLV to dump in the datapath ID

		// Chassis ID TLV
		byte chassis[] = { 0x02, 0x07, // type =1, len=7 (1 + 6)
				0x04 }; // subtype = subtype MAC address
		bb.put(chassis);
		bb.put(hardwareAddress);

		// Port ID TLV
		byte id[] = { 0x04, 0x03, // type 2, length 3
				0x02 }; // Subtype Port
		bb.put(id);
		bb.putShort(portNumber);

		// TTL TLV - Isn't this 112 sec?
		byte ttl[] = { 0x06, 0x02, 0x00, 0x78 };
		bb.put(ttl); // type 3, length 2, 120 seconds

		// SysD TLV
		bb.put(lldpSysD);
		bb.putLong(this.featuresReply.getDatapathId());
		
		//OUI TLV
		String fvName = FlowVisor.getInstance().getInstanceName();

		/*if (fvName.length() < LLDPUtil.MIN_FV_NAME) // pad out to min length size
			fvName = String.format("%1$" + LLDPUtil.MIN_FV_NAME + "s", fvName);*/
		
		int ouiLen = 4 +  fvName.length() +  FLOWNAMELEN_LEN + FLOWNAMELEN_NULL;
																						//4 - length of OUI Id + it's subtype 
		int ouiHeader = (ouiLen & 0x1ff) | ((OUI_TYPE & 0x007f) << 9);
		bb.putShort((short)ouiHeader);
		
		// ON.Labs OUI = a42305 and assigning the subtype to 0x01
		byte oui[] = { (byte)0xa4, (byte)0x23, (byte)0x05};
		//byte oui[] = {0x0a, 0x04, 0x02, 0x03, 0x00, 0x05};
		bb.put(oui);
		byte ouiSubtype[] = { 0x01 };
		bb.put(ouiSubtype);
		StringByteSerializer.writeTo(bb, fvName.length() + 1,
				fvName);
		bb.put((byte) (fvName.length() + 1));
		
		//EndOfLLDPDU TLV
		byte endType[] = { 0x00 };
		bb.put(endType);
		byte endLength[] = {0x00 };
		bb.put(endLength);
		
		while (bb.position() <= (size - 4))
			bb.putInt(0xcafebabe); // fill with well known padding
		return buf;
	}

	/**
	 * @return the topologyController
	 */
	public TopologyController getTopologyController() {
		return topologyController;
	}

	/**
	 * @param topologyController
	 *            the topologyController to set
	 */
	public void setTopologyController(TopologyController topologyController) {
		this.topologyController = topologyController;
	}

	static public DPIDandPort parseLLDP(byte[] packet) {
		/**
		 * TODO: merge this with the code in LLDPUtil.java
		 */
		// LLDP packets sent by FV should have the following byte offsets:
		// 0 - dst addr (MAC)
		// 6 - src addr (MAC)
		// 12 - ether lldp
		// 14 - chassis id tl
		// 16 - subtype
		// 17 - src addr (MAC)
		// 23 - port id tl
		// 25 - subtype
		// 26 - port num
		// 28 - ttl tl
		// 30 - ttl value
		// 32 - sysdesc tl
		// 34 - dpid
		// 42 – oui header
		// 44 – oui id
		// 47 – oui subtype
		// 48 – oui string (2 bytes for fvName; 1 for null; 1 for fvNameLength)
		// 52 – endOfLLDPDU
		// 54 - padding 
		
		int vlan_offset = 0;
		if (packet == null || packet.length != LLDPLen)
			return null; // invalid lldp (to us, anyhow)
		ByteBuffer bb = ByteBuffer.wrap(packet);
		byte[] dst = new byte[6];
		bb.get(dst);
		if (!Arrays.equals(dst, LLDPUtil.LLDP_MULTICAST))
			return null;
		bb.position(12);
		short etherType = bb.getShort();
		while (etherType == LLDPUtil.ETHER_VLAN) {
			vlan_offset += 4;
			etherType = bb.getShort(); // noop to advance two bytes
			etherType = bb.getShort();
		}
		if (etherType != LLDPUtil.ETHER_LLDP)
			return null;
		bb.position(26 + vlan_offset);
		short port = bb.getShort();
		byte possibleSysId[] = new byte[2];
		bb.position(32 + vlan_offset);
		bb.get(possibleSysId);
		if (!Arrays.equals(possibleSysId, TopologyConnection.lldpSysD))
			return null;
		bb.position(34 + vlan_offset);
		long dpid = bb.getLong();
		return new DPIDandPort(dpid, port);
	}

	public long getProbesPerPeriod() {
		return probesPerPeriod;
	}

	synchronized void signalPortTimeout(short port) {
		Short sPort = Short.valueOf(port);
		if (this.fastPorts.contains(sPort)) {
			FVLog
					.log(LogLevel.MOBUG, this, "setting fast port to slow: ",
							port);
			this.fastPorts.remove(sPort);
			this.slowPorts.add(sPort);
		} else if (!this.slowPorts.contains(sPort)) {
			FVLog.log(LogLevel.WARN, this,
					"got signalPortTimeout for non-existant port: ", port);
		}
	}

	public synchronized void signalFastPort(short port) {
		Short sPort = Short.valueOf(port);
		if (this.slowPorts.contains(sPort)) {
			FVLog.log(LogLevel.DEBUG, this, "setting slow port to fast: "
					+ port);
			this.slowPorts.remove(sPort);
			this.slowIterator = this.slowPorts.iterator();
			this.fastPorts.add(sPort);
		} else if (!this.fastPorts.contains(sPort)) {
			FVLog.log(LogLevel.WARN, this,
					"got signalFastPort for non-existant port: ", port);
		}
	}

	@Override
	public void sendMsg(OFMessage msg, FVSendMsg from) {
		if (this.msgStream != null) {
			FVLog.log(LogLevel.DEBUG, this, "send to controller: ", msg);
			try {
				this.msgStream.testAndWrite(msg);
			} catch (BufferFull e) {
				FVLog.log(LogLevel.CRIT, this,
						"framing bug; tearing down: got " + e);
				// don't shut down now; we could get a ConcurrencyException
				// just queue up a shutdown for later
				this.pollLoop.queueEvent(new TearDownEvent(this, this));
			} catch (MalformedOFMessage e) {
				FVLog.log(LogLevel.CRIT, this, "BUG: " + e);
				this.stats.increment(FVStatsType.DROP, from, msg);
			} catch (IOException e) {
				FVLog.log(LogLevel.WARN, this, " killing connection, got: ", e);
				this.tearDown();
			}
		} else {
			FVLog.log(LogLevel.WARN, this,
					"dropping msg: controller not connected: " + msg);
			this.stats.increment(FVStatsType.DROP, from, msg);
		}
	}

	@Override
	public String getConnectionName() {
		return FlowSpaceUtil.connectionToString(sock);
	}

	@Override
	public void dropMsg(OFMessage msg, FVSendMsg from) {
		this.stats.increment(FVStatsType.DROP, from, msg);
	}

	@Override
	public SendRecvDropStats getStats() {
		return stats;
	}

}
