/**
 *
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.api.TopologyCallback;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.events.FVTimerEvent;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * A simple OpenFlow controller that runs inside the flowvisor to discover and
 * report the network's topology
 *
 * Can only run one instance of topology controller at a time; use spawn() to
 * create or get running instance
 *
 * @author capveg
 *
 */
public class TopologyController extends OFSwitchAcceptor {

	static TopologyController runningInstance = null;
	public static String TopoUser = FVConfig.SUPER_USER;
	List<TopologyConnection> topologyConnections;
	Map<LinkAdvertisement, Long> latestProbes;
	private boolean doCallback;
	private long updatePeriod;
	private long timeoutPeriod;

	static long defaultUpdatePeriod = 5000; // in milliseconds
	static long defaultTimeoutPeriod = 10000; // in milliseconds

	private final Map<String, TopologyCallback> generalCallBackDB;
	private Map<TopologyCallback.EventType, List<TopologyCallback>> eventCallbacks;

	public static TopologyController getRunningInstance() {
		return TopologyController.runningInstance;
	}

	public static void setRunningInstance(TopologyController tc) {
		TopologyController.runningInstance = tc;
	}

	private TopologyController(FVEventLoop pollLoop, int port, int backlog)
			throws IOException {
		super(pollLoop, port, backlog);
		this.topologyConnections = new LinkedList<TopologyConnection>();
		this.latestProbes = new HashMap<LinkAdvertisement, Long>();
		this.doCallback = false;
		this.generalCallBackDB = new HashMap<String, TopologyCallback>();
		this.eventCallbacks = new HashMap<TopologyCallback.EventType, List<TopologyCallback>>();
		this.eventCallbacks.put(TopologyCallback.EventType.DEVICE_CONNECTED, new ArrayList<TopologyCallback>());
		this.eventCallbacks.put(TopologyCallback.EventType.SLICE_CONNECTED, new ArrayList<TopologyCallback>());
		this.eventCallbacks.put(TopologyCallback.EventType.SLICE_DISCONNECTED, new ArrayList<TopologyCallback>());
		this.setUpdatePeriod(TopologyController.defaultUpdatePeriod);
		this.setTimeoutPeriod(TopologyController.defaultTimeoutPeriod);
		// schedule the update timer
		pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.updatePeriod, this, this, null));
		TopologyController.setRunningInstance(this);
	}

	public static FVEventHandler spawn(FVEventLoop pollLoop) {
		if (runningInstance != null)
			return runningInstance; // return version that's already running
		if (!isConfigured())
			return null; // not configured for it
		TopologyController tc = null;
		try {
			tc = new TopologyController(pollLoop, 0, 16); // 0 == any port
			int port = tc.getListenPort();
			try {
				FVConfig.setSliceHost(TopoUser, "localhost");
				FVConfig.setSlicePort(TopoUser, port);
			} catch (ConfigError e) {
				FVLog.log(LogLevel.CRIT, tc,
						"tried to register topology controller info, but topo user '"
								+ TopoUser + "' not found: " + e);
			}
		} catch (IOException e) {
			FVLog.log(LogLevel.ALERT, null,
					"failed to spawn TopologyController: " + e);
		}
		return tc;
	}

	/**
	 * Add a new URL to the list of things to make an XMLRPC call to if the
	 * topology changes
	 *
	 * @param user
	 *            FVUser (e.g., "alice") who registered this callback
	 * @param URL
	 *            Location of user's xmlrpc server
	 * @param cookie
	 *            Some state locally meaningful to user
	 */
	public synchronized void registerCallBack(String user, String URL,String methodName,
			String cookie, String callbackType) {
		TopologyCallback.EventType eventType = TopologyCallback.EventType.valueOf(callbackType);
		if(eventType == TopologyCallback.EventType.GENERAL)
			this.generalCallBackDB.put(user, new TopologyCallback(URL,methodName, cookie));
		else{
			this.eventCallbacks.get(eventType).add(new TopologyCallback(user, URL, methodName, eventType, cookie));
		}

	}
	
	public synchronized void deregisterCallback(String user, String method, String callbackType, String cookie){

		Iterator<TopologyCallback> it = eventCallbacks.get(TopologyCallback.EventType.valueOf(callbackType)).iterator();
		
		while (it.hasNext()) {
			TopologyCallback callback = it.next();
			if (callback.getMethodName().equals(method) && callback.getCookie().equals(cookie) &&
					callback.getUser().equals(user))
				it.remove();
		}
	}

	/*
	 * TODO: This method blows. It should be remove when the XMLRPC api goes. 
	 */
	public synchronized void deregisterCallback(String method, String callbackType){

		for(TopologyCallback callback : this.eventCallbacks.get(TopologyCallback.EventType.valueOf(callbackType))){
			if (callback.getMethodName().equals(method)){
				eventCallbacks.get(TopologyCallback.EventType.valueOf(callbackType)).remove(callback);
			}
		}
	}

	public synchronized String getTopologyCallback(String user){
		TopologyCallback tempTopologyCallback=(TopologyCallback)this.generalCallBackDB.get(user);
		String tempString;
		String tempMethodName;

		if (tempTopologyCallback!=null){
			tempString=tempTopologyCallback.getURL();
			tempMethodName=tempTopologyCallback.getMethodName();
		}
		else{
			return null;
		}

		if (tempString!=null && tempMethodName!=null){
 			return	tempString+" -- XMLRPC method name="+tempMethodName;
		}
		else{
			return "";
		}
	}

	public synchronized void unregisterCallBack(String user) {
		this.generalCallBackDB.remove(user);
	}

	@Override
	public String getName() {
		return "TopoDiscovery";
	}

	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (e instanceof FVTimerEvent)
			processUpdate();
		else
			super.handleEvent(e);
	}

	/**
	 * On each processUpdate signal, step through list of probes,
	 *
	 * @return
	 */
	private synchronized void processUpdate() {
		FVLog.log(LogLevel.DEBUG, this, "processing updates");
		for (Iterator<LinkAdvertisement> it = this.latestProbes.keySet()
				.iterator(); it.hasNext();) {
			LinkAdvertisement linkAdvertisement = it.next();
			long now = System.currentTimeMillis();
			long thisProbe = latestProbes.get(linkAdvertisement).longValue();
			if ((thisProbe + this.timeoutPeriod) < now) {
				FVLog.log(LogLevel.INFO, this, "timeout: removing link "
						+ linkAdvertisement);
				FVLog.log(LogLevel.DEBUG, this, "timeout: " + thisProbe + "+"
						+ this.timeoutPeriod + " > " + now);
				this.doCallback = true;
				it.remove();
			}
		}
		if (this.doCallback)
			processCallback();
		// Schedule next event
		pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.updatePeriod, this, this, null));

	}

	private synchronized void processCallback() {
		FVLog.log(LogLevel.INFO, this, "topology changed: doing callbacks");
		for (TopologyCallback topologyCallback : this.generalCallBackDB.values())
			topologyCallback.spawn();
		this.doCallback = false;
	}

	@Override
	void handleIOEvent(FVIOEvent event) {
		SocketChannel sock = null;

		try {
			sock = ssc.accept();
			if (sock == null) {
				FVLog.log(LogLevel.CRIT, null,
						"ssc.accept() returned null !?! FIXME!");
				return;
			}
			FVLog.log(LogLevel.INFO, this, "got new connection: "
					+ sock.socket().getRemoteSocketAddress());
			TopologyConnection tc = new TopologyConnection(this, pollLoop, sock);
			tc.init();
			topologyConnections.add(tc);

			this.doCallback = true; // signal that we need a call back when a
			// new switch comes
		} catch (IOException e) // ignore IOExceptions -- is this the right
		// thing to do?
		{
			System.err.println("Got IOException for " + sock != null ? sock
					: "unknown socket");
			System.err.println(e);
		}
	}
	
	public void sliceConnectionJustChanged(Map<String, Object> info, TopologyCallback.EventType ev) {
		for (TopologyCallback cb : eventCallbacks.get(ev)) {
			cb.setParams(info);
			cb.run();
			cb.clearParams();
		}
			
	}

	public void topoConnectionJustConnected(String dpidHex){
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(dpidHex);
		for(TopologyCallback callback : eventCallbacks.get(TopologyCallback.EventType.DEVICE_CONNECTED)){
			callback.setParams(params);
			callback.run();
			callback.clearParams();
		}
	}

	/**
	 * Return a list of datapath ids (encoded as raw longs) of all the
	 * switches/devices connected to this topology controller
	 *
	 * Used by FVUserAPI getDevices() call
	 *
	 * @return never null, may return an empty list if nothing is connected
	 */

	public synchronized List<Long> listDevices() {
		List<Long> dpids = new LinkedList<Long>();
		for (TopologyConnection tc : this.topologyConnections)
			if (tc.isConnected())
				dpids.add(tc.getDataPathID());
		return dpids;
	}

	/**
	 * Return a list of links (encoded as LinkAdvertisements) of all discovered
	 * links of all switches connected to this topology controller.
	 *
	 * Used by FVUserAPI getLinks() call
	 *
	 * @return
	 */

	public synchronized Set<LinkAdvertisement> getLinks() {
		return latestProbes.keySet();
	}

	public synchronized void reportProbe(LinkAdvertisement linkAdvertisement) {
		if (!this.latestProbes.containsKey(linkAdvertisement)) {
			this.doCallback = true;
			FVLog.log(LogLevel.INFO, this, "adding link " + linkAdvertisement);
		}
		this.latestProbes.put(linkAdvertisement, Long.valueOf(System
				.currentTimeMillis()));
	}

	/**
	 * Returns true if the FV is configured to do topology discovery and it's
	 * running
	 *
	 * @return true == yes, the topology discovery process is running
	 */

	public static boolean isConfigured() {
		try {
			return FVConfig.getTopologyServer();
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, null, "Creating config entry for topology server run parameter"
					+ "=false");
			try {
				FVConfig.setTopologyServer(false);
			} catch (ConfigError e1) {
				throw new RuntimeException(e1);
			}
			FlowVisor fv = FlowVisor.getInstance();
			if (fv != null)
				fv.checkPointConfig();
			return false;
		}
	}

	/**
	 * @return the topoUser
	 */
	public static String getTopoUser() {
		return TopoUser;
	}

	/**
	 * @param topoUser
	 *            the topoUser to set
	 */
	public static void setTopoUser(String topoUser) {
		TopoUser = topoUser;
	}

	public void setUpdatePeriod(long updatePeriod) {
		this.updatePeriod = updatePeriod;
	}

	public long getUpdatePeriod() {
		return updatePeriod;
	}

	public void setTimeoutPeriod(long timeoutPeriod) {
		this.timeoutPeriod = timeoutPeriod;
	}

	public long getTimeoutPeriod() {
		return timeoutPeriod;
	}

	/**
	 * This gets called when a topology connection is killed
	 *
	 * @param topologyConnection
	 */
	public void disconnect(TopologyConnection topologyConnection) {
		this.topologyConnections.remove(topologyConnection);
		this.doCallback = true;
	}

	@Override
	public void tearDown() {
		super.tearDown();
		FVLog.log(LogLevel.WARN, this, "shutting down");
		for (Iterator<TopologyConnection> it = this.topologyConnections
				.iterator(); it.hasNext();) {
			TopologyConnection topologyConnection = it.next();
			it.remove();
			topologyConnection.tearDown();
		}
	}
}
