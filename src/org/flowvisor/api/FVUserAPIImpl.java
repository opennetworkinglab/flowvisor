/**
 *
 */
package org.flowvisor.api;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.BracketParse;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.config.FlowvisorImpl;
import org.flowvisor.config.InvalidDropPolicy;
import org.flowvisor.config.InvalidSliceName;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.config.SwitchImpl;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.DuplicateControllerException;
import org.flowvisor.exceptions.InvalidUserInfoKey;
import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNotFound;
import org.flowvisor.flows.FlowDBEntry;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowRewriteDB;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.ofswitch.TopologyController;
import org.flowvisor.resources.SlicerLimits;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.util.HexString;
import org.openflow.util.U16;

/**
 * This is the actual UserAPI that gets wrapped via XMLRPC In theory
 * ("God willin' and the creek dun rise"), XMLRPC calls will call these function
 * directly
 *
 * @author capveg
 *
 */
public class FVUserAPIImpl /*extends BasicJSONRPCService*/ implements FVUserAPI {
	

	/**
	 * For debugging
	 *
	 * @param arg
	 *            test string
	 * @return response test string
	 */
	public String ping(String arg) {
		String user = APIUserCred.getUserName();
		return "PONG(" + user + "): FV version=" + FlowVisor.FLOWVISOR_VERSION
				+ "::" + arg;
	}


	protected Collection<FlowEntry> getFlowEntries() throws ConfigError {
		String sliceName = APIUserCred.getUserName();
		FVLog.log(LogLevel.DEBUG, null, "API listFlowSpace() by: " + sliceName);
		FlowMap flowMap;
		synchronized (FVConfig.class) {
			if (FVConfig.isSupervisor(sliceName))
				flowMap = FVConfig.getFlowSpaceFlowMap();
			else
				flowMap = FlowSpaceUtil.getSliceFlowSpace(sliceName);
			return flowMap.getRules();
		}
	}
	
	public Boolean createSlice(String sliceName, String passwd,
			String controller_url, String slice_email)
			throws MalformedControllerURL, InvalidSliceName, InvalidDropPolicy,
			PermissionDeniedException, DuplicateControllerException {
		return createSlice(sliceName, passwd, controller_url,"exact" ,slice_email);
	}

	/**
	 * Create a new slice (without flowspace)
	 *
	 * Slices that contain the field separator are rewritten with underscores
	 *
	 * @param sliceName
	 *            Cannot contain FVConfig.FS == '!'
	 * @param passwd
	 *            Cleartext! FIXME
	 * @param controller_url
	 *            Reference controller pseudo-url, e.g., tcp:hostname[:port]
	 * @param slice_email
	 *            As a contract for the slice
	 * @return success
	 * @throws InvalidSliceName
	 * @throws PermissionDeniedException
	 * @throws DuplicateControllerException
	 */
	@Override
	public Boolean createSlice(String sliceName, String passwd,
			String controller_url, String drop_policy, String slice_email)
			throws MalformedControllerURL, InvalidSliceName, InvalidDropPolicy,
			PermissionDeniedException, DuplicateControllerException {
		// FIXME: make sure this user has perms to do this OP
		// for now, all slices can create other slices
		// FIXME: for now, only handle tcp, not ssl controller url
		String[] list = controller_url.split(":");
		if (!FVConfig.isSupervisor(APIUserCred.getUserName()))
			throw new PermissionDeniedException(
					"only superusers can create new slices");
		if (list.length < 2)
			throw new MalformedControllerURL(
					"controller url needs to be of the form "
							+ "proto:hostname[:port], e.g., tcp:yourhost.foo.com:6633, not: "
							+ controller_url);
		if (!list[0].equals("tcp"))
			throw new MalformedControllerURL(
					"Flowvisor currently only supports 'tcp' proto, not: "
							+ list[0]);
		int controller_port;
		if (list.length >= 3)
			controller_port = Integer.valueOf(list[2]);
		else
			controller_port = FVConfig.OFP_TCP_PORT;
		// createSlice is synchronized()
		
		if (drop_policy.equals(""))
			drop_policy = "exact";
		else if (!drop_policy.equals("exact") && !drop_policy.equals("rule"))
			throw new InvalidDropPolicy("Flowvisor currently supports an 'exact'"
						+" or a 'rule' based drop policy");

		// We need to make sure this slice doesn't already exist
		List<String> slices = null;
		synchronized (FVConfig.class) {
			try {
				slices = FVConfig.getAllSlices();
			} catch (ConfigError e) {
				e.printStackTrace();
				throw new RuntimeException("no SLICES subdir found in config");
			}
			for (Iterator<String> sliceIter = slices.iterator(); sliceIter
					.hasNext();) {
				String existingSlice = sliceIter.next();
				if (sliceName.equals(existingSlice)) {
					throw new PermissionDeniedException(
							"Cannot create slice with existing name.");
				}
			}
		}

		FVConfig.createSlice(sliceName, list[1], controller_port, drop_policy, passwd,
				slice_email, APIUserCred.getUserName());
		FlowVisor.getInstance().checkPointConfig();
		return true;
	}

	/**
	 * Change the password for this slice
	 *
	 * A slice is allowed to change its own password and the password of any
	 * slice that it has (transitively) created
	 *
	 * @param sliceName
	 * @param newPasswd
	 */
	@Override
	public Boolean changePasswd(String sliceName, String newPasswd)
			throws PermissionDeniedException {
		String changerSlice = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(changerSlice, sliceName)
				&& !FVConfig.isSupervisor(changerSlice))
			throw new PermissionDeniedException("Slice " + changerSlice
					+ " does not have perms to change the passwd of "
					+ sliceName);
		String salt = APIAuth.getSalt();
		String crypt = APIAuth.makeCrypt(salt, newPasswd);
		sliceName = FVConfig.sanitize(sliceName);
		// set passwd is synchronized
		FVConfig.setPasswd(sliceName, salt, crypt);
		FlowVisor.getInstance().checkPointConfig();
		return true;
	}

	@Override
	public Boolean changeSlice(String sliceName, String key, String value)
			throws MalformedURLException, InvalidSliceName,
			PermissionDeniedException, InvalidUserInfoKey, DuplicateControllerException {
		String changerSlice = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(changerSlice, sliceName)
				&& !FVConfig.isSupervisor(changerSlice))
			throw new PermissionDeniedException("Slice " + changerSlice
					+ " does not have perms to change the passwd of "
					+ sliceName);
		/**
		 * this is the list of things a user is allowed to change about
		 * themselves. Critically, it should not include "creator" string as
		 * this would allow security issues.
		 */
		
		try {
			if (key.equals("contact_email"))
				FVConfig.setSliceContactEmail(sliceName, value);
			else if (key.equals("controller_hostname")){
				// make sure there isn't already a slice with this hostname and port
				// that this slice uses
				if (isSecondSliceSharingController(sliceName, value, FVConfig.getSlicePort(sliceName))){
					throw new DuplicateControllerException(value, FVConfig.getSlicePort(sliceName), sliceName, "changed");
				}
				FVConfig.setSliceHost(sliceName, value);
			}
			else if (key.equals("controller_port")){
				// Make sure that there isn't already a slice on this port that uses
				// the same hostname that this slice uses
				if (isSecondSliceSharingController(sliceName, FVConfig.getSliceHost(sliceName), Integer.parseInt(value))){
					throw new DuplicateControllerException(FVConfig.getSliceHost(sliceName),
							Integer.parseInt(value), sliceName, "changed");
				}
				
				FVConfig.setSlicePort(sliceName, Integer.valueOf(value));
			} else if (key.equals("drop_policy")) {
				//Set the drop policy when the controller is done, 
				//either to an exact match of the packet in or to the
				//flow entry.
				FVConfig.setSliceDropPolicy(sliceName, value);
			} 
			else
				throw new InvalidUserInfoKey("invalid key: " + key
						+ "-- only contact_email, drop_policy and "
						+ "controller_{hostname,port} can be changed");
			FlowVisor.getInstance().checkPointConfig();
		} catch (ConfigError e) {
			// this should probably never happen b/c of above checks
			throw new InvalidUserInfoKey(e.toString());
		}

		return true;
	}

	private Boolean isSecondSliceSharingController(String thisSlice, String hostname, int port){
		Collection<String> sliceList;
		try {
			sliceList = listSlices();
		} catch (PermissionDeniedException e1) {
			return false;
		}
		for(String otherSlice : sliceList){
			if(otherSlice.equals(thisSlice)){
				// This is actually the same slice, ignore
				continue;
			}
			try {
				if(FVConfig.getSliceHost(otherSlice).equalsIgnoreCase(hostname)){
					if(FVConfig.getSlicePort(otherSlice) == port){
						return true;
					}
				}
			} catch (ConfigError e) {
				// Guess it wasn't a match. just ignore
			}
		}

		return false;
	}

	@Override
	public Boolean change_password(String sliceName, String newPasswd)
			throws PermissionDeniedException {
		return changePasswd(sliceName, newPasswd);
		// just call changePasswd(); keeping the two names made things easier
		// for Jad /shrug
	}

	/**
	 * For now, create a circular, bidirectional loop between existing switches
	 * FIXME need to actually infer and calc real topology
	 */

	@Override
	public Collection<Map<String, String>> getLinks() {
		FVLog.log(LogLevel.DEBUG, null,
				"API getLinks() by: " + APIUserCred.getUserName());
		TopologyController topologyController = TopologyController
				.getRunningInstance();
		if (topologyController == null)
			return getFakeLinks();
		List<Map<String, String>> list = new LinkedList<Map<String, String>>();
		for (Iterator<LinkAdvertisement> it = topologyController.getLinks()
				.iterator(); it.hasNext();) {
			LinkAdvertisement linkAdvertisement = it.next();
			list.add(linkAdvertisement.toMap());
		}
		return list;
	}

	protected List<Map<String, String>> getFakeLinks() {
		FVLog.log(LogLevel.ALERT, null,
				"API: topology server not running: faking getLinks()");
		List<String> devices = listDevices();
		List<Map<String, String>> list = new LinkedList<Map<String, String>>();
		for (int i = 0; i < devices.size(); i++) {
			// forward direction
			LinkAdvertisement link = new LinkAdvertisement();
			link.srcDPID = devices.get(i);
			link.dstDPID = devices.get((i + 1) % devices.size());
			link.srcPort = 0;
			link.dstPort = 1;
			link.setAttribute("fakeLink", "true");
			list.add(link.toMap());
		}
		return list;
	}

	@Override
	public List<String> listDevices() {
		FVLog.log(LogLevel.DEBUG, null,
				"API listDevices() by: " + APIUserCred.getUserName());
		FlowVisor fv = FlowVisor.getInstance();
		// get list from main flowvisor instance
		List<String> dpids = new ArrayList<String>();
		String dpidStr;

		/*
		 * if (TopologyController.isConfigured()) { for (Long dpid :
		 * TopologyController.getRunningInstance() .listDevices()) { dpidStr =
		 * HexString.toHexString(dpid); if (!dpids.contains(dpidStr))
		 * dpids.add(dpidStr); else FVLog.log(LogLevel.WARN, TopologyController
		 * .getRunningInstance(), "duplicate dpid detected: " + dpidStr); } }
		 * else {
		 */
		// only list a device is we have a features reply for it
		for (FVEventHandler handler : fv.getHandlersCopy()) {
			if (handler instanceof FVClassifier) {
				OFFeaturesReply featuresReply = ((FVClassifier) handler)
						.getSwitchInfo();
				if (featuresReply != null) {
					dpidStr = FlowSpaceUtil.dpidToString(featuresReply
							.getDatapathId());
					if (!dpids.contains(dpidStr))
						dpids.add(dpidStr);
					else
						FVLog.log(LogLevel.WARN, handler,
								"duplicate dpid detected: " + dpidStr);
				}
			}
		}
		// }
		return dpids;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.api.FVUserAPI#getDeviceInfo()
	 */
	@Override
	public Map<String, String> getDeviceInfo(String dpidStr)
			throws DPIDNotFound {
		Map<String, String> map = new HashMap<String, String>();
		long dpid = HexString.toLong(dpidStr);
		FVClassifier fvClassifier = null;
		for (FVEventHandler handler : FlowVisor.getInstance().getHandlersCopy()) {
			if (handler instanceof FVClassifier) {
				OFFeaturesReply featuresReply = ((FVClassifier) handler)
						.getSwitchInfo();
				if (featuresReply != null
						&& featuresReply.getDatapathId() == dpid) {
					fvClassifier = (FVClassifier) handler;
					break;
				}
			}
		}
		if (fvClassifier == null)
			throw new DPIDNotFound("dpid does not exist: " + dpidStr + " ::"
					+ String.valueOf(dpid));
		OFFeaturesReply config = fvClassifier.getSwitchInfo();
		map.put("dpid", FlowSpaceUtil.dpidToString(dpid));
		if (config != null) {
			map.put("nPorts", String.valueOf(config.getPorts().size()));
			String portList = "";
			String portNames = "";
			int p;
			for (Iterator<OFPhysicalPort> it = config.getPorts().iterator(); it
					.hasNext();) {
				OFPhysicalPort port = it.next();
				p = U16.f(port.getPortNumber());
				portList += p;
				portNames += port.getName() + "(" + p + ")";
				if (it.hasNext()) {
					portList += ",";
					portNames += ",";
				}
			}
			map.put("portList", portList);
			map.put("portNames", portNames);
		} else {
			FVLog.log(LogLevel.WARN, null, "null config for: " + dpidStr);
		}
		map.put("remote", String.valueOf(fvClassifier.getConnectionName()));
		return map;
	}

	@Override
	public Boolean deleteSlice(String sliceName) throws SliceNotFound,
			PermissionDeniedException, ConfigError {
		String changerSlice = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(changerSlice, sliceName)) {
			FVLog.log(LogLevel.WARN, null, "API deletSlice(" + sliceName
					+ ") failed by: " + APIUserCred.getUserName());
			throw new PermissionDeniedException("Slice " + changerSlice
					+ " does not have perms to change the passwd of "
					+ sliceName);
		}
		synchronized (FVConfig.class) {
			FVLog.log(LogLevel.DEBUG, null, "API removeSlice(" + sliceName
					+ ") by: " + APIUserCred.getUserName());
			FlowMap flowSpace = FlowSpaceUtil.deleteFlowSpaceBySlice(sliceName);
			try {
				// this is also synchronized against FVConfig.class
				FVConfig.deleteSlice(sliceName);
			} catch (Exception e) {
				throw new SliceNotFound("slice does not exist: " + sliceName);
			}
			
			/*
			 * We need to do this because of the linear flowmap, because
			 * it wants the slicers and classifiers to update their 
			 * view of the flowspace.
			 * Once linearflowmap is dropped, this should be dropped as
			 * well.
			 * 
			 * FIXME
			 */
			FlowSpaceImpl.getProxy().notifyChange(flowSpace);
			//FVConfig.sendUpdates(FVConfig.FLOWSPACE);
			// signal that FS has changed
			FlowVisor.getInstance().checkPointConfig();
		}
		return true;
	}



	@Override
	public Collection<String> listSlices() throws PermissionDeniedException {
		/*
		 * relaxed security; anyone can get a list of slices if
		 * (!FVConfig.isSupervisor(APIUserCred.getUserName())) throw new
		 * PermissionDeniedException( "listSlices only available to root");
		 */
		List<String> slices = null;
		synchronized (FVConfig.class) {
			try {
				// this is synchronized
				List<String> entries = FVConfig.getAllSlices();
				slices = new LinkedList<String>(entries);
			} catch (ConfigError e) {
				e.printStackTrace();
				throw new RuntimeException(
						"wtf!?: no SLICES subdir found in config");
			}
		}
		return slices;
	}

	@Override
	public Map<String, String> getSliceInfo(String sliceName)
			throws PermissionDeniedException, SliceNotFound {

		/*
		 * relaxed security -- anyone can read slice info for now String user =
		 * APIUserCred.getUserName(); if (!FVConfig.isSupervisor(user) &&
		 * !APIAuth.transitivelyCreated(user, sliceName)) throw new
		 * PermissionDeniedException(
		 * "not superuser or transitive slice creator");
		 */
		if (!(doesSliceExist(sliceName))){
			throw new SliceNotFound("Slice does not exist: " + sliceName);
		}

		HashMap<String, String> map = new HashMap<String, String>();

		synchronized (FVConfig.class) {
			try {
				map.put("contact_email",
						FVConfig.getSliceEmail(sliceName));
				map.put("controller_hostname",
						FVConfig.getSliceHost(sliceName));
				map.put("controller_port", String.valueOf(FVConfig.getSlicePort(sliceName)));
				map.put("creator", FVConfig.getSliceCreator(sliceName));
				map.put("drop_policy", FVConfig.getSlicePolicy(sliceName));
			} catch (ConfigError e) {
				FVLog.log(LogLevel.CRIT, null, "malformed slice: " + e);
				e.printStackTrace();
			}
		}
		long dpid;
		int connection = 1;

		// TODO: come back an architect this so we can walk the list of slicers,
		// not the list of classifiers, and then slicers
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (!classifier.isIdentified()) // only print switches have have
					// been identified
					continue;
				dpid = classifier.getDPID();
				FVSlicer fvSlicer = classifier.getSlicerByName(sliceName);
				if (fvSlicer != null) {
					map.put("connection_" + connection++,
							FlowSpaceUtil.dpidToString(dpid) + "-->"
									+ fvSlicer.getConnectionName());
				}

			}
		}

		return map;
	}

	/*
	 * @return true if slice exists, otherwise false
	 * @param sliceName name of slice to check for existance
	 */
	public static boolean doesSliceExist(String sliceName){
		List<String> slices = new ArrayList<String>();
		try {
			slices = FVConfig.getAllSlices();
		} catch (ConfigError e) {
			e.printStackTrace();
		}
		return slices.contains(sliceName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.api.FVUserAPI#revertToLastCheckpoint()
	 */
	@Override
	public Boolean revertToLastCheckpoint() {
		// TODO: implement!
		return false;
	}


	public Boolean registerTopologyChangeCallback(String URL, String methodName, String cookie)
			throws MalformedURLException {
		// this will throw MalformedURL back to the client if the URL is bad
		new URL(URL);
		TopologyController tc = TopologyController.getRunningInstance();
		if (tc != null) {
			tc.registerCallBack(APIUserCred.getUserName(), URL, methodName, cookie, "GENERAL");
			return true;
		} else
			return false; // topology server not running
	}

	public String getTopologyCallback(){

		TopologyController tc=TopologyController.getRunningInstance();
		String URL="";//="No callback defined yet"
		if (tc!=null){
			URL=tc.getTopologyCallback(APIUserCred.getUserName());
		}

		if (URL==null || URL.equals("")){
			return "No callback defined yet";
		}
		else{
			return URL;
		}

	}

	@Override
	public Boolean unregisterTopologyChangeCallback() {
		TopologyController tc = TopologyController.getRunningInstance();
		if (tc != null) {
			tc.unregisterCallBack(APIUserCred.getUserName());
			return true;
		} else
			return false; // topology server not running
	}

	@Override
	public String getSliceStats(String sliceName) throws SliceNotFound,
			PermissionDeniedException {

		if (!(doesSliceExist(sliceName))){
			throw new SliceNotFound("Slice does not exist: " + sliceName);
		}

		FVSlicer fvSlicer = null;
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (!classifier.isIdentified()) // only print switches have have
					// been identified
					continue;
				fvSlicer = classifier.getSlicerByName(sliceName);
				if (fvSlicer != null) {
					break;
				}
			}
		}

		if (fvSlicer == null)
			return SendRecvDropStats.NO_STATS_AVAILABLE_MSG;

		return fvSlicer.getStats().combinedString();
	}

	@Override
	public String getSwitchStats(String dpidStr) throws DPIDNotFound,
			PermissionDeniedException {
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (classifier.getDPID() == dpid)
					return classifier.getStats().combinedString();
			}
		}
		throw new DPIDNotFound("dpid not found: " + dpidStr);
	}

	@Override
	public Collection<Map<String, String>> getSwitchFlowDB(String dpidStr)
			throws DPIDNotFound {
		boolean found = false;
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		List<Map<String, String>> ret = new LinkedList<Map<String, String>>();
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (dpid == classifier.getDPID() || dpid == FlowEntry.ALL_DPIDS) {
					synchronized (classifier) {
						for (Iterator<FlowDBEntry> it2 = classifier.getFlowDB()
								.iterator(); it2.hasNext();) {
							ret.add(it2.next().toBracketMap());
						}
					}
					found = true;
				}
			}
		}
		if (!found)
			throw new DPIDNotFound("dpid not found: " + dpidStr);
		return ret;
	}

	@Override
	public Map<String, List<Map<String, String>>> getSliceRewriteDB(
			String sliceName, String dpidStr) throws DPIDNotFound,
			SliceNotFound, PermissionDeniedException {
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		FVSlicer fvSlicer = lookupSlicer(sliceName, dpid);
		Map<String, List<Map<String, String>>> ret = new HashMap<String, List<Map<String, String>>>();
		FlowRewriteDB flowRewriteDB = fvSlicer.getFlowRewriteDB();
		synchronized (flowRewriteDB) {
			for (FlowDBEntry original : flowRewriteDB.originals()) {
				Map<String, String> originalMap = original.toBracketMap();
				List<Map<String, String>> rewrites = new LinkedList<Map<String, String>>();
				for (FlowDBEntry rewrite : flowRewriteDB.getRewrites(original)) {
					rewrites.add(rewrite.toBracketMap());
				}
				ret.put(BracketParse.encode(originalMap), rewrites);
			}
		}
		return ret;
	}
	


	/**
	 *
	 * @param sliceName
	 * @param dpid
	 * @return a valid fvSlicer (never null)
	 * @throws DPIDNotFound
	 * @throws SliceNotFound
	 */

	private FVSlicer lookupSlicer(String sliceName, long dpid)
			throws DPIDNotFound, SliceNotFound {

		FVClassifier fvClassifier = lookupClassifier(dpid); // throws dpid not
															// found
		synchronized (fvClassifier) {
			FVSlicer fvSlicer = fvClassifier.getSlicerByName(sliceName);
			if (fvSlicer == null)
				throw new SliceNotFound(sliceName);
			return fvSlicer;
		}
	}

	/**
	 * Returns a valid fvClassifier
	 *
	 * @param dpid
	 * @return never null
	 * @throws DPIDNotFound
	 */
	private FVClassifier lookupClassifier(long dpid) throws DPIDNotFound {
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (dpid == classifier.getDPID())
					return classifier;
			}
		}
		throw new DPIDNotFound("No such switch: " + dpid);
	}
	
	private List<FVClassifier> getAllClassifiers() {
		List<FVClassifier> list = new LinkedList<FVClassifier>();
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				list.add(classifier);
			}
		}
		return list;
	}
	
	
	private SlicerLimits getSliceLimits() throws DPIDNotFound{
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				return classifier.getSlicerLimits();
			}
		}
		throw new DPIDNotFound("No classifier found, therefore no limits accessible");
	}


	@Override
	public Boolean setFloodPerm(String dpidStr, String floodPerm)
			throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to change the flood perms of "
					+ dpidStr + "  to " + floodPerm);
		FVLog.log(LogLevel.DEBUG, null, "Setting flood perm for : ", dpidStr);
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		try {
			SwitchImpl.getProxy().setFloodPerm(dpid, floodPerm);
			return true;
		} catch (ConfigError e) {
			FVLog.log(LogLevel.ALERT, null, "Unable to set floodperm", e.getMessage());
		}
		return false;
	}

	
	@Override
	public String getFloodPerm(String dpidStr) throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to obtain the flood perms of "
					+ dpidStr);
		FVLog.log(LogLevel.DEBUG, null, "Setting flood perm for : ", dpidStr);
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		try {
			return SwitchImpl.getProxy().getFloodPerm(dpid);
			
		} catch (ConfigError e) {
			FVLog.log(LogLevel.ALERT, null, "Unable to set floodperm", e.getMessage());
		}
		return null;
	}

	@Override
	public Boolean setFloodPerm(String floodPerm)
			throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to change the default flood perms to " 
					+ floodPerm);
		FVLog.log(LogLevel.DEBUG, null, "Setting default flood perm to " + floodPerm);
		
		FlowvisorImpl.getProxy().setFloodPerm(floodPerm);
		return true;
		
	}
	
	@Override
	public String getFloodPerm() throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to obtain the default flood perms");
		FVLog.log(LogLevel.DEBUG, null, "Getting default flood perm");
		
		try {
			return FlowvisorImpl.getProxy().getFloodPerm();
		} catch (ConfigError e) {
			FVLog.log(LogLevel.ALERT, null, "Unable to get floodperm", e.getMessage());
		}
		return null;
	}
	
	
	@Override
	public boolean setMaximumFlowMods(String sliceName, String dpid,
			String maxFlowMods) throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(user, sliceName)
				&& !FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to set the flow mod limit for slice " + sliceName);
		Long dp = FlowSpaceUtil.parseDPID(dpid);
		int limit = Integer.parseInt(maxFlowMods);
		FVLog.log(LogLevel.DEBUG, null, "Setting flowmod limit for slice " + sliceName + 
					" for dpid " + dpid + " to " + maxFlowMods);
		try {
			if (dp == FlowEntry.ALL_DPIDS)
				SliceImpl.getProxy().setMaxFlowMods(sliceName, limit);
			else
				SwitchImpl.getProxy().setMaxFlowMods(sliceName, dp, limit);
		} catch (ConfigError e) {
			return false;
		}
		return true;
	}


	@Override
	public Integer getMaximumFlowMods(String sliceName, String dpid)
			throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(user, sliceName)
				&& !FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to get the flow mod limit for slice " + sliceName);
		Long dp = FlowSpaceUtil.parseDPID(dpid);
		try {
			if (dp == FlowEntry.ALL_DPIDS)
				return SliceImpl.getProxy().getMaxFlowMods(sliceName);
			else
				return SwitchImpl.getProxy().getMaxFlowMods(sliceName, dp);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.DEBUG, null, "Unable to get flow mod limit; " + e.getMessage());
			return null;
		}
	}
	
	public Integer getCurrentFlowMods(String sliceName, String dpid) 
			throws PermissionDeniedException, SliceNotFound, DPIDNotFound {
		String user = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(user, sliceName)
				&& !FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to get the current flow mod value for slice " + sliceName);
		Long dp = FlowSpaceUtil.parseDPID(dpid);
		if (dp == FlowEntry.ALL_DPIDS)
			return getSliceLimits().getSliceFMLimit(sliceName);
		else
			return lookupClassifier(dp).getCurrentFlowModCounter(sliceName);
	}

	@Override
	public boolean setRateLimit(String sliceName,
			String rate) throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(user, sliceName)
				&& !FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to set the flow mod limit for slice " + sliceName);
		int limit = Integer.parseInt(rate);
		for (FVClassifier classifier : getAllClassifiers()) {
			try {
				SwitchImpl.getProxy().setRateLimit(sliceName, classifier.getDPID(), limit);
			} catch (ConfigError e) {
				FVLog.log(LogLevel.DEBUG, null, "Unable to set rate limit; " + e.getMessage());
				return false;
			}
			FVLog.log(LogLevel.DEBUG, null, "Setting rate limit for slice " + sliceName + 
					 " to " + limit);
		}
		
		return true;
	}

	


	@Override
	public Boolean setFlowTracking(String flowtracking)
			throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to enable or disable flow tracking");
		boolean track = Boolean.parseBoolean(flowtracking);
		FVLog.log(LogLevel.DEBUG, null, "Setting flow tracking to " + (track ? "enabled." : "disabled."));
		FlowvisorImpl.getProxy().settrack_flows(track);
		return true;
	}


	

	@Override
	public Boolean getFlowTracking() throws PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " does not have perms to obtain flow tracking status");
		FVLog.log(LogLevel.DEBUG, null, "Getting flow tracking status");
		try {
			return FlowvisorImpl.getProxy().gettrack_flows();
		} catch (ConfigError e) {
			FVLog.log(LogLevel.ALERT, null, "Unable to get flow tracking status ", e.getMessage());
		}
		return null;
	}


	@Override
	public String dumpConfig() throws PermissionDeniedException, FileNotFoundException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user))
			throw new PermissionDeniedException("User " + user
					+ " cannot dump the configuration to file ");
		return FVConfig.getConfig();
	}

}
