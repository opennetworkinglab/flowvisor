/**
 *
 */
package org.flowvisor.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.flowvisor.api.FlowChange.FlowChangeOp;
import org.flowvisor.config.BracketParse;
import org.flowvisor.config.FVConfig;
import org.flowvisor.exceptions.MalformedFlowChange;
import org.flowvisor.exceptions.MapUnparsable;
import org.flowvisor.flows.FlowDBEntry;




/**
 * Client side stand alone command-line tool for invoking the FVUserAPI
 *
 * This is pretty hacky and just for testing; people should write their own
 * clients and/or call the XMLRPC directly
 *
 * @author capveg
 *
 */
public class FVCtl {
	String URL;
	XmlRpcClientConfigImpl config;
	XmlRpcClient client;
	static APICmd[] cmdlist = new APICmd[] {
		new APICmd("listSlices", 0),
		new APICmd("createSlice", 3, "<slicename> <controller_url> <email>"),
		new APICmd("changeSlice", 3, "<slicename> <key> <value>"),
		new APICmd("deleteSlice", 1, "<slicename>"),
		new APICmd("changePasswd", 1, "<slicename>"),
		new APICmd("getSliceInfo", 1, "<slicename>"),
		
		new APICmd("setMaximumFlowMods", 3,"<slice> <dpid> <maximum_flow_mods>"),
		new APICmd("getMaximumFlowMods", 2, "<slice> <dpid>"),
		
		new APICmd("getCurrentFlowMods", 2, "<slice> <dpid>"),
		
		new APICmd("setRateLimit", 2,"<slice> <msgs_per_second>"),
		
		new APICmd("getSliceStats", 1, "<slicename>"),
		new APICmd("getSwitchStats", 1, "<dpid>"),
		new APICmd("getSwitchFlowDB", 1, "<dpid>"),
		new APICmd("getSliceRewriteDB", 2, "<slicename> <dpid>"),

		new APICmd("listFlowSpace", 0),
		new APICmd("removeFlowSpace", 1, "<id>"),
		new APICmd("addFlowSpace", 4, "<dpid> <priority> <match> <actions>"),
		
		new APICmd("changeFlowSpace", 5,
		"<id> <dpid> <priority> <match> <actions>"),
		
				
		new APICmd("dumpConfig", 1, "<filename>"),

		new APICmd("listDevices", 0),
		new APICmd("getDeviceInfo", 1, "<dpid>"),
		new APICmd("getLinks", 0),

		new APICmd("ping", 1, "<msg>"),
		
		new APICmd("setFlowTracking", 1, "<true/false>"),
		new APICmd("setFloodPerm", 2, "<dpid> <slicename>"),
		new APICmd("getFlowTracking", 0),
		new APICmd("getFloodPerm", 1, "<dpid>"),
		
		new APICmd("setDefaultFloodPerm", 1, "<slicename>"),
		new APICmd("getDefaultFloodPerm", 0),

		new APICmd("registerCallback", 3, "<URL> <methodName> <cookie>"),
		new APICmd("registerTopologyEventCallback", 3, "<URL> <methodName> <eventType>"),
		new APICmd("deregisterTopologyEventCallback", 2, "<method> <eventType>"),
		new APICmd("unregisterCallback", 0), };

	static class APICmd {
		String name;
		int argCount;
		String usage;
		static HashMap<String, APICmd> cmdlist = new HashMap<String, APICmd>();

		APICmd(String name, int argCount, String usage) {
			this.name = name;
			this.argCount = argCount;
			this.usage = usage;
			cmdlist.put(name, this);
		}

		APICmd(String name, int argCount) {
			this(name, argCount, "");
		}

		@SuppressWarnings("unchecked")
		// Need to figure out magic java sauce to fix this
		void invoke(FVCtl client, String args[]) throws SecurityException,
		NoSuchMethodException, IllegalArgumentException,
		IllegalAccessException, InvocationTargetException {
			Class<String>[] params = new Class[args.length];
			for (int i = 0; i < args.length; i++)
				params[i] = String.class;
			Method m = FVCtl.class.getMethod("run_" + this.name, params);
			m.invoke(client, (Object[]) args);
		}
	}

	/**
	 *
	 * @param URL
	 *            Server URL
	 */
	public FVCtl(String URL) {
		this.URL = URL;
	}

	/**
	 * Init connection to XMLRPC Server in URL
	 *
	 * @throws MalformedURLException
	 *
	 * @throws Exception
	 */
	public void init(String user, String passwd) throws MalformedURLException {
		this.installDumbTrust();
		config = new XmlRpcClientConfigImpl();
		config.setBasicUserName(user);
		config.setBasicPassword(passwd);
		config.setServerURL(new URL(this.URL));
		config.setEnabledForExtensions(true);

		client = new XmlRpcClient();
		// client.setTransportFactory(new
		// XmlRpcCommonsTransportFactory(client));
		// client.setTransportFactory(new )
		client.setConfig(config);
	}

	/*@SuppressWarnings("unchecked")
	public void runJetty(String user, String passwd, String methodName, Object[] args){
		try {
			this.installDumbTrust();
			// Jetty Client
			AuthorizedServiceProxy proxy;
			proxy = new AuthorizedServiceProxy(FVUserAPIJSON.class, this.URL, user, passwd);
			FVUserAPIJSON apiService = (FVUserAPIJSON)proxy.create();
			Class<?> [] argTypes = new Class[args.length];
			for (int argNum = 0; argNum < args.length; argNum++){
				argTypes[argNum] = String.class;
			}

			if (methodName.equals("changeFlowSpace") ||
				methodName.equals("removeFlowSpace") ||
				methodName.equals("addFlowSpace")){
				args = convertFlowspaceArgs(methodName, args);
				methodName = "changeFlowSpace";
				argTypes = new Class[1];
				argTypes[0] = List.class;
				}

			Method serviceMethod = FVUserAPIJSON.class.getMethod(methodName, argTypes);
			System.out.println("executing request");
			Object result  = serviceMethod.invoke(apiService, (Object[])args);
			if(methodName.equals("listFlowSpace")){
				handleFlowSpaceResults((List<FlowEntry>)result);
				return;
			}
			System.out.println("Reponse: " + result);
			System.out.println("----------------------------------------");

		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private void handleFlowSpaceResults(List<FlowEntry> flows){
		for(FlowEntry flow : flows){
			System.out.println(flow.toString());
		}
	}

	private Object[] convertFlowspaceArgs(String methodName, Object[] initArgs) throws NumberFormatException, MalformedFlowChange{
		// Args need to be converted to a FlowEntry
		Object[] args = new Object[1];
		List<FlowSpaceChangeRequest> flowChanges = new ArrayList<FlowSpaceChangeRequest>();
		args[0] = flowChanges;

		if(methodName.equals("changeFlowSpace")){
			//<id> <dpid> <priority> <match> <actions>"
			String list[] = ((String)initArgs[4]).split(",");
			List<OFAction> alist = new LinkedList<OFAction>();
			for (int i = 0; i < list.length; i++)
				alist.add(SliceAction.fromString(list[i]));
			String match = (String)initArgs[3];
			if (match.equals("") || match.equals("any") || match.equals("all"))
				match = "OFMatch[]";
			FlowEntry entry = new FlowEntry(FlowSpaceUtil.parseDPID((String)initArgs[1]),
					FlowSpaceUtil.fvMatchFromString(match),
					new Integer((String)initArgs[0]),
					new Integer((String)initArgs[2]),
					alist);
			flowChanges.add(new FlowSpaceChangeRequest(entry, "CHANGE"));
		}else if(methodName.equals("addFlowSpace")){
			//<dpid> <priority> <match> <actions>"
			String list[] = ((String)initArgs[3]).split(",");
			List<OFAction> alist = new LinkedList<OFAction>();
			for (int i = 0; i < list.length; i++)
				alist.add(SliceAction.fromString(list[i]));
			String match = (String)initArgs[2];
			if (match.equals("") || match.equals("any") || match.equals("all"))
				match = "OFMatch[]";
			FlowEntry entry = new FlowEntry(FlowSpaceUtil.parseDPID((String)initArgs[0]),
					FlowSpaceUtil.fvMatchFromString(match),
					new Integer((String)initArgs[1]),
					alist);
			flowChanges.add(new FlowSpaceChangeRequest(entry, "ADD"));
		}else{
			// Remove Flowspace
			FlowEntry flowEntry = new FlowEntry();
			flowEntry.setId(new Integer((String)initArgs[0]));
			flowChanges.add(new FlowSpaceChangeRequest(flowEntry, "REMOVE"));
		}

		return args;
	}*/

	private TrustManager[] getTrustAllManager(){
		// Create a trust manager that does not validate certificate chains
		// System.err.println("WARN: blindly trusting server cert - FIXME");
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}
		} };
		return trustAllCerts;
	}
	public void installDumbTrust() {

		TrustManager[] trustAllCerts = getTrustAllManager();
		try {
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			// Create empty HostnameVerifier
			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			};

			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
			.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (KeyManagementException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}
	
	public void run_dumpConfig(String filename) throws XmlRpcException {
		String output = (String) this.client.execute("api.dumpConfig",
				new Object[] {});
		FileWriter foutput;
		try {
			foutput = new FileWriter(filename);
			foutput.write(output);
			foutput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

	public void run_listDevices() throws XmlRpcException {
		Object[] reply = (Object[]) this.client.execute("api.listDevices",
				new Object[] {});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		for (int i = 0; i < reply.length; i++) {
			String dpid = (String) reply[i];
			System.out.println("Device " + i + ": " + dpid);
		}
	}

	@SuppressWarnings("unchecked")
	public void run_getDeviceInfo(String dpidStr) throws XmlRpcException {
		Map<String, Object> reply = (Map<String, Object>) this.client.execute(
				"api.getDeviceInfo", new Object[] { dpidStr });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		for (String key : reply.keySet()) {
			System.out.println(key + "=" + reply.get(key));
		}
	}

	public void run_getConfig(String name) throws XmlRpcException {

		Object reply = this.client.execute("api.getConfig",
				new Object[] { name });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		Object objects[] = (Object[]) reply;
		if (objects.length == 1)
			System.out.println(name + " = " + (String) objects[0]);
		else
			for (int i = 0; i < objects.length; i++)
				System.out
				.println(name + " " + i + " = " + (String) objects[i]);
	}

	public void run_setConfig(String name, String value) throws XmlRpcException {
		Object reply = this.client.execute("api.setConfig", new Object[] {
				name, value });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (!(reply instanceof Boolean)) {
			System.err.println("Didn't get boolean reply?; got" + reply);
			System.exit(-1);
		}
		boolean success = ((Boolean) reply).booleanValue();
		if (success) {
			System.out.println("success");
			System.exit(0);
		} else {
			System.out.println("failure");
			System.exit(-1);
		}
	}

	@SuppressWarnings("unchecked")
	public void run_getLinks() throws XmlRpcException, MapUnparsable {
		Object[] reply = (Object[]) this.client.execute("api.getLinks",
				new Object[] {});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		Map<String, String> map;
		for (int i = 0; i < reply.length; i++) {
			if (!(reply[i] instanceof Map<?, ?>)) {
				System.err.println("not a map: Skipping unparsed reply: "
						+ reply[i]);
			} else {
				map = (Map<String, String>) reply[i];
				LinkAdvertisement ad = LinkAdvertisement.fromMap(map);
				System.out.println("Link " + i + ": " + ad);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void run_getSwitchFlowDB(String dpidString) throws XmlRpcException,
	MapUnparsable {
		Object[] reply = (Object[]) this.client.execute("api.getSwitchFlowDB",
				new Object[] { dpidString });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		Map<String, String> map;
		FlowDBEntry flowDBEntry;
		for (int i = 0; i < reply.length; i++) {
			if (!(reply[i] instanceof Map<?, ?>)) {
				System.err.println("not a map: Skipping unparsed reply: "
						+ reply[i]);
			} else {
				map = (Map<String, String>) reply[i];
				flowDBEntry = new FlowDBEntry();
				flowDBEntry.fromBacketMap(map);
				System.out.println("DBEntry " + i + ": " + flowDBEntry);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void run_getSliceRewriteDB(String sliceName, String dpidStr)
	throws XmlRpcException {

		Object ret = this.client.execute("api.getSliceRewriteDB", new Object[] {
				sliceName, dpidStr });
		Map<String, Object[]> flowRewriteDB;
		if (!(ret instanceof Map)) {
			throw new XmlRpcException("unknown reply type "
					+ ret.getClass().toString());
		}
		flowRewriteDB = (Map<String, Object[]>) ret;
		for (String original : flowRewriteDB.keySet()) {
			System.out.println("============ Original");
			System.out.println(original);
			System.out.println("\n=========== Rewritten to:");
			Object[] objs = flowRewriteDB.get(original);
			Map<String, String> rewrite;
			for (int i = 0; i < objs.length; i++) {
				rewrite = (Map<String, String>) objs[i];
				System.out.println("\t\t" + BracketParse.encode(rewrite));
			}

		}
	}

	public void run_changePasswd(String sliceName) throws IOException,
	XmlRpcException {
		String passwd = FVConfig.readPasswd("New password: ");
		Boolean reply = (Boolean) this.client.execute("api.changePasswd",
				new Object[] { sliceName, passwd });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}

	public void run_changeSlice(String sliceName, String key, String value)
	throws IOException, XmlRpcException {
		Boolean reply = (Boolean) this.client.execute("api.changeSlice",
				new Object[] { sliceName, key, value });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}

	@SuppressWarnings("unchecked")
	public void run_getSliceInfo(String sliceName) throws IOException,
	XmlRpcException {

		Object o = this.client.execute("api.getSliceInfo",
				new Object[] { sliceName });
		if (o == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		Map<String, String> reply = null;
		if (o instanceof Map<?, ?>)
			reply = (Map<String, String>) o;

		System.out.println("Got reply:");
		for (String key : reply.keySet())
			System.out.println(key + "=" + reply.get(key));
	}

	public void run_getSliceStats(String sliceName) throws IOException,
	XmlRpcException {

		Object o = this.client.execute("api.getSliceStats",
				new Object[] { sliceName });
		if (o == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		String reply = null;
		if (o instanceof String)
			reply = (String) o;

		System.out.println("Got reply:");
		System.out.println(reply);
	}

	public void run_getSwitchStats(String dpid) throws IOException,
	XmlRpcException {

		Object o = this.client.execute("api.getSwitchStats",
				new Object[] { dpid });
		if (o == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		String reply = null;
		if (o instanceof String)
			reply = (String) o;

		System.out.println("Got reply:");
		System.out.println(reply);
	}
	
	public void run_createSlice(String sliceName, String controller_url,
			String slice_email) throws IOException, XmlRpcException {
		String passwd = FVConfig.readPasswd("New password: ");
		Boolean reply = (Boolean) this.client
		.execute("api.createSlice", new Object[] { sliceName, passwd,
				controller_url, slice_email });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}

	public void run_createSlice(String sliceName, String controller_url,
			String drop_policy, String slice_email) throws IOException, XmlRpcException {
		String passwd = FVConfig.readPasswd("New password: ");
		Boolean reply = (Boolean) this.client
		.execute("api.createSlice", new Object[] { sliceName, passwd,
				controller_url, drop_policy, slice_email });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}

	public void run_ping(String msg) throws XmlRpcException {
		String reply = (String) this.client.execute("api.ping",
				new Object[] { msg });
		if (reply != null) {
			System.out.println("Got reply:");
			System.out.println(reply);
		} else {
			System.err.println("Got 'null' for reply :-(");
		}
	}

	public void run_deleteSlice(String sliceName) throws XmlRpcException {
		Boolean reply = (Boolean) this.client.execute("api.deleteSlice",
				new Object[] { sliceName });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}

	public void run_removeFlowSpace(String indexStr) throws XmlRpcException {
		FlowChange change = new FlowChange(FlowChangeOp.REMOVE,
				Integer.valueOf(indexStr));
		List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();
		mapList.add(change.toMap());

		try {
			Object[] reply = (Object[]) this.client.execute(
					"api.changeFlowSpace", new Object[] { mapList });

			if (reply == null) {
				System.err.println("Got 'null' for reply :-(");
				System.exit(-1);
			}
			if (reply.length > 0)
				System.out.println("success: " + (String) reply[0]);
			else
				System.err.println("failed!");
		} catch (XmlRpcException e) {
			System.err.println("Failed: Flow Entry not found");
			System.exit(-1);
		}

	}


	
	
	public void run_addFlowSpace(String dpid, String priority, String match,
			String actions) throws XmlRpcException, MalformedFlowChange {
		do_flowSpaceChange(FlowChangeOp.ADD, dpid, null, priority, match,
				actions);
	}
	


	public void run_changeFlowSpace(String idStr, String dpid, String priority,
			String match, String actions) throws XmlRpcException,
			MalformedFlowChange {
		do_flowSpaceChange(FlowChangeOp.CHANGE, dpid, idStr, priority, match,
				actions);
	}

	private void do_flowSpaceChange(FlowChangeOp op, String dpid, String idStr,
			String priority, String match, String actions)
	throws XmlRpcException {
		if (match.equals("") || match.equals("any") || match.equals("all"))
			match = "OFMatch[]";
		Map<String, String> map = FlowChange.makeMap(op, dpid, idStr, priority,
				match, actions);

		try {
			FlowChange.fromMap(map);
		} catch (MalformedFlowChange e) {
			System.err.println("Local sanity check failed: " + e);
			return;
		}
		List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();
		mapList.add(map);
		Object[] reply = (Object[]) this.client.execute("api.changeFlowSpace",
				new Object[] { mapList });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply.length > 0)
			System.out.println("success: " + (String) reply[0]);
		else
			System.err.println("failed!");
	}

	public void run_listSlices() throws XmlRpcException {
		Object[] reply = (Object[]) this.client.execute("api.listSlices",
				new Object[] {});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		for (int i = 0; i < reply.length; i++) {
			String slice = (String) reply[i];
			System.out.println("Slice " + i + ": " + slice);
		}

	}

	public void run_listFlowSpace() throws XmlRpcException {
		Object[] result2 = (Object[]) client.execute("api.listFlowSpace",
				new Object[] {});
		if (result2 != null) {
			System.out.println("Got reply:");
			int i;
			for (i = 0; i < result2.length; i++)
				System.out.println("rule " + i + ": " + (String) result2[i]);
		} else {
			System.err.println("Got 'null' for reply :-(");
		}
	}

	public void run_registerCallback(String URL, String methodName,
			String cookie) throws IOException, XmlRpcException,
			MalformedURLException {
		Boolean reply = (Boolean) this.client.execute(
				"api.registerTopologyChangeCallback", new Object[] { URL,
						methodName, cookie });
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}

	public void run_unregisterCallback() throws IOException, XmlRpcException,
	MalformedURLException {
		Boolean reply = (Boolean) this.client.execute(
				"api.unregisterTopologyChangeCallback", new Object[] {});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}
	
	public void run_setFlowTracking(String val) throws IOException, XmlRpcException,
												MalformedURLException {
		Boolean reply = (Boolean) this.client.execute(
				"api.setFlowTracking", new Object[] {val});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failure!");
	}
	
	public void run_getFloodPerm(String dpid) throws IOException, XmlRpcException, 
												MalformedURLException {	
		String reply = (String) this.client.execute(
				"api.getFloodPerm", new Object[] {dpid});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply != null)
			System.out.println("DPID " + dpid + " can be flooded by slice " + reply);
	}
	
	public void run_getDefaultFloodPerm() throws IOException, XmlRpcException, 
												MalformedURLException {	
			String reply = (String) this.client.execute(
					"api.getFloodPerm", new Object[] {});
			if (reply == null) {
				System.err.println("Got 'null' for reply :-(");
				System.exit(-1);
			}
			if (reply != null)
				System.out.println("Default floodperm is enabled for slice " + reply);
			
	}
	
	public void run_getFlowTracking() throws IOException, XmlRpcException,
										MalformedURLException {

		Boolean reply = (Boolean) this.client.execute(
				"api.getFlowTracking", new Object[] {});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("enabled");
		else
			System.out.println("disabled");
	}

	public void run_setFloodPerm(String dpid, String sliceName) throws IOException, XmlRpcException, 
										MalformedURLException {	
		Boolean reply = (Boolean) this.client.execute(
				"api.setFloodPerm", new Object[] {dpid, sliceName});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");
	}

	public void run_setDefaultFloodPerm(String sliceName) throws IOException, XmlRpcException, 
										MalformedURLException {	
		Boolean reply = (Boolean) this.client.execute(
				"api.setFloodPerm", new Object[] { sliceName});
			if (reply == null) {
				System.err.println("Got 'null' for reply :-(");
				System.exit(-1);
			}
			if (reply)
				System.out.println("success!");
			else
				System.err.println("failed!");	
	}
	
	public void run_setMaximumFlowMods(String sliceName, String dpidStr,
						String strMaxFlowMods) throws IOException, XmlRpcException, 
						MalformedURLException {
		Boolean reply = (Boolean) this.client.execute(
				"api.setMaximumFlowMods", new Object[] {sliceName,
						dpidStr, strMaxFlowMods});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");	
	}
	
	public void run_getMaximumFlowMods(String sliceName, String dpidStr) 
			throws IOException, XmlRpcException, 
			MalformedURLException {
		Integer reply = (Integer) this.client.execute(
				"api.getMaximumFlowMods", new Object[] {sliceName,
				dpidStr});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		System.out.println("The maximum limit for slice " + sliceName + " on " 
							+ dpidStr + " : " + reply);
	}
	
	public void run_getCurrentFlowMods(String sliceName, String dpidStr)
			throws IOException, XmlRpcException, 
			MalformedURLException {
		Integer reply = (Integer) this.client.execute(
				"api.getCurrentFlowMods", new Object[] {sliceName,
				dpidStr});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		System.out.println("The current limit for slice " + sliceName + " on " 
							+ dpidStr + " : " + reply);
	}
	
	
	public void run_setRateLimit(String sliceName, 
			String strRateLimit) throws IOException, XmlRpcException, 
			MalformedURLException {
		Boolean reply = (Boolean) this.client.execute(
				"api.setRateLimit", new Object[] {sliceName, strRateLimit});
		if (reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply)
			System.out.println("success!");
		else
			System.err.println("failed!");	
	}
	
	private static void usage(String string) {
		usage(string, true);
	}

	private static void usage(String string, boolean printFull) {
		System.err.println(string);
		if (printFull) {
			System.err
			.println("Usage: FVCtl [--debug=true] [--user=user] [--url=url] "
					+ "[--passwd-file=filename] command [args...] ");
			for (int i = 0; i < FVCtl.cmdlist.length; i++) {
				APICmd cmd = FVCtl.cmdlist[i];
				System.err.println("\t" + cmd.name + " " + cmd.usage);
			}
		}
		System.exit(-1);
	}
	
	

	/**
	 * Front-end cmdline parser for FVCtl
	 *
	 * @param args
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	public static void main(String args[]) {
		// FIXME: make URL a parameter
		// FVCtl client = new
		// FVCtl("https://root:joemama@localhost:8080/xmlrpc");
		String URL = "https://localhost:8080/xmlrpc";
		String JETTY_URL = "https://localhost:8081/flowvisor";
		String user = FVConfig.SUPER_USER;
		String passwd = null;
		boolean debug = false;
		boolean jetty = false;

		int cmdIndex = 0;
		// FIXME: find a decent java cmdline args parsing lib
		while ((args.length > cmdIndex) && (args[cmdIndex].startsWith("--"))) {
			String params[] = args[cmdIndex].split("=");
			if (params.length < 2)
				usage("parameter " + params[0] + " needs an argument");
			if (params[0].equals("--url"))
				URL = params[1];
			else if (params[0].equals("--user"))
				user = params[1];
			else if (params[0].equals("--debug"))
				debug = Boolean.valueOf(params[1]);
			else if (params[0].equals("--passwd-file")) {

				try {
					passwd = new BufferedReader(new FileReader(new File(
							params[1]))).readLine();
				} catch (FileNotFoundException e) {
					die(debug, "file: '" + params[1] + "' :: ", e);
				} catch (IOException e) {
					die(debug, "IO: ", e);
				}
			}
			/*else if (params[0].equals("--jetty")){
				jetty = true;
			}*/else
				usage("unknown parameter: " + params[0]);
			cmdIndex++;
		}
		if (args.length == cmdIndex)
			usage("need to specify a command");

		APICmd cmd = APICmd.cmdlist.get(args[cmdIndex]);
		if (cmd == null)
			usage("command '" + args[cmdIndex] + "' does not exist");
		if ((args.length - 1 - cmdIndex) < cmd.argCount)
			usage("command '" + args[cmdIndex] + "' takes " + cmd.argCount
					+ " args: only " + (args.length - 1 - cmdIndex)
					+ " given\n" + args[cmdIndex] + " " + cmd.usage, false);
		String[] strippedArgs = new String[args.length - 1 - cmdIndex];
		System.arraycopy(args, cmdIndex + 1, strippedArgs, 0,
				strippedArgs.length);
		try {
			if (passwd == null)
				passwd = FVConfig.readPasswd("Enter " + user + "'s passwd: ");
			FVCtl client = new FVCtl(jetty ? JETTY_URL : URL);

			/*if (jetty){
				client.runJetty(user, passwd, cmd.name, strippedArgs);
			}
			else{*/
				client.init(user, passwd);
				cmd.invoke(client, strippedArgs);
			//}
		} catch (Exception e) {
			die(debug, "error: ", e);
		}
	}

	private static void die(boolean debug, String string, Exception e) {
		Throwable cause = e;
		while (cause.getCause() != null) {
			if (debug)
				cause.printStackTrace(System.err);
			cause = cause.getCause();
		}
		if (debug)
			cause.printStackTrace(System.err);
		System.err.println(string + cause);
		System.exit(-1);
	}

}
