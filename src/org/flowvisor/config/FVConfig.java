/**
 *
 */
package org.flowvisor.config;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import org.flowvisor.api.APIAuth;
import org.flowvisor.exceptions.DuplicateControllerException;
import org.flowvisor.flows.FlowMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Central collection of all configuration and policy information, e.g., slice
 * permissions, what port to run on, etc.
 *
 * Uses get/set on a hierarchy of nodes like sysctl,snmp, etc.
 * getInt("flowvisor.list_port") --> 6633
 * setString("slice.alice.controller_hostname"
 * ,"alice-controller.controllers.org")
 *
 * All of the set* operations will dynamically create the entry if it does not
 * exist.
 *
 * @author capveg
 *
 */
public class FVConfig {
	public static final String FS = "!";
	
	public static final String SLICE_SALT = "passwd_salt";
	public static final String SLICE_CRYPT = "passwd_crypt";

	public static final String SUPER_USER = "fvadmin";
	
	final static public int OFP_TCP_PORT = 6633;
	public static final long DelayWarning = 10;

	/**
	 * Return the flowmap associated with this node
	 *
	 * @param node
	 * @return
	 * @throws ConfigError
	 */
	static public FlowMap getFlowMap() throws ConfigError{
		FlowSpace proxy = FlowSpaceImpl.getProxy();
		return proxy.getFlowMap();
	}

	static synchronized public FlowMap getFlowSpaceFlowMap() {
		FlowSpace proxy = FlowSpaceImpl.getProxy();
		try {
			return proxy.getFlowMap();
		} catch (ConfigError e) {
			e.printStackTrace();
			throw new RuntimeException("WTF!?!  No FlowSpace defined!?!");
		}
		
	}

	/**
	 * Set the flowmap at this entry, creating it if it does not exist
	 *
	 * @param node
	 * @param val
	 * @throws ConfigError
	 */
	static public void setFlowMap(FlowMap val) throws ConfigError {
		FlowSpace proxy = FlowSpaceImpl.getProxy();
		proxy.setFlowMap(val);
	}


	/**
	 * Read XML-encoded config from filename
	 *
	 * @param filename
	 *            fully qualified or relative pathname
	 */
	public static synchronized void readFromFile(String filename)
			throws FileNotFoundException, ConfigError {
		Gson gson = new GsonBuilder().create();
		File file = new File(filename);
		String json = new Scanner(file).useDelimiter("\\Z").next();
		HashMap<String, ArrayList<HashMap<String, Object>>> config = gson.fromJson(json, new TypeToken<HashMap<String, Object>>(){}.getType());
		
		
		try {
			
			if (config.containsKey(Flowvisor.FLOWVISOR))
				FlowvisorImpl.getProxy().fromJson(config.get(Flowvisor.FLOWVISOR));
			else
				throw new ConfigError("Missing configuration for flowvisor base parameters");
			
			if (config.containsKey(Slice.TSLICE))
				SliceImpl.getProxy().fromJson(config.get(Slice.TSLICE));
			
			if (config.containsKey(FlowSpace.FS))
				FlowSpaceImpl.getProxy().fromJson(config.get(FlowSpace.FS));
			
			if (config.containsKey(Switch.SWITCH))
				SwitchImpl.getProxy().fromJson(config.get(Switch.SWITCH));
				
					
			
		} catch (IOException e) {
			System.err.println("Error while parsing config file " + e.getMessage());
		} 
		
		
	}

	
	/**
	 * Write XML-encoded config to filename
	 *
	 * @param filename
	 *            fully qualified or relative pathname
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public static synchronized void writeToFile(String filename) throws FileNotFoundException  {
		FileWriter foutput = null;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		HashMap<String, Object> output = new HashMap<String, Object>();
		try {
			FlowvisorImpl.getProxy().toJson(output);
			SliceImpl.getProxy().toJson(output);
			FlowSpaceImpl.getProxy().toJson(output);
			SwitchImpl.getProxy().toJson(output);
			foutput = new FileWriter(filename);
			foutput.write(gson.toJson(output));
		} catch (IOException e) {
			System.err.println("Error whie writing config file " + e.getMessage());
		} finally {
			try {
				foutput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static synchronized String getConfig() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		HashMap<String, Object> output = new HashMap<String, Object>();
		FlowvisorImpl.getProxy().toJson(output);
		SliceImpl.getProxy().toJson(output);
		FlowSpaceImpl.getProxy().toJson(output);
		SwitchImpl.getProxy().toJson(output);
		return gson.toJson(output);
	}
	
	public static void createSlice(String sliceName,
			String controller_hostname, int controller_port, String drop_policy, String passwd,
			String slice_email, String creatorSlice) throws InvalidSliceName,
			DuplicateControllerException {
		FVConfig.createSlice(sliceName, controller_hostname, controller_port, drop_policy,
				passwd, APIAuth.getSalt(), slice_email, creatorSlice);
	}

	public synchronized static void createSlice(String sliceName,
			String controller_hostname, int controller_port, String drop_policy, String passwd,
			String salt, String slice_email, String creatorSlice)
			throws InvalidSliceName, DuplicateControllerException {
		
		sliceName = FVConfig.sanitize(sliceName);
		Slice proxy = SliceImpl.getProxy();
		proxy.createSlice(sliceName, controller_hostname, controller_port,
				drop_policy, passwd, salt, slice_email, creatorSlice);
	}

	public static String readPasswd(String prompt) throws IOException {
		Console cons = System.console();
		if (cons != null) {
			char[] passwd = cons.readPassword(prompt);
			return new String(passwd);
		} else {
			/**
			 * This is a hack to get around the fact that in java,
			 * System.console() will return null if the calling process is not a
			 * tty, e.g., with `fvctl listSlices | less`
			 */
			System.err.print(prompt);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					System.in));
			return reader.readLine();
		}
	}
	
	// Slice config params

	public static synchronized void deleteSlice(String sliceName) throws InvalidSliceName {
		sliceName = FVConfig.sanitize(sliceName);
		Slice proxy = SliceImpl.getProxy();
		proxy.deleteSlice(sliceName);
	}
	
	public static void setSliceContactEmail(String sliceName, String email) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		proxy.setContactEmail(sliceName, email);
		
	}
	
	public static void setSliceHost(String sliceName, String hostname) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		proxy.setcontroller_hostname(sliceName, hostname);
	}
	
	public static void setSlicePort(String sliceName, Integer port) throws ConfigError{
		Slice proxy = SliceImpl.getProxy();
		proxy.setcontroller_port(sliceName, port);
	}
	
	public static void setSliceDropPolicy(String sliceName, String policy) {
		Slice proxy = SliceImpl.getProxy();
		proxy.setdrop_policy(sliceName, policy);
	}

	public static boolean checkSliceName(String sliceName) {
		Slice proxy = SliceImpl.getProxy();
		return proxy.checkSliceName(sliceName);
	}
	
	public static String getSliceEmail(String sliceName) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getEmail(sliceName);
	}
	
	public static String getSlicePolicy(String sliceName) {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getdrop_policy(sliceName);
	}
	
	public static Integer getSlicePort(String sliceName) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getcontroller_port(sliceName);
	}
	
	public static String getSliceHost(String sliceName) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getcontroller_hostname(sliceName);
	}
	
	public static String getPasswdElm(String sliceName, String elm) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getPasswdElm(sliceName, elm);
	}
	
	public static String getSliceCreator(String sliceName) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getCreator(sliceName);
		
	}
	
	public static LinkedList<String> getAllSlices() throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getAllSliceNames();
	}
	
	public static String getDropPolicy(String sliceName) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getdrop_policy(sliceName);
	}
	
	public static Boolean getLLDPSpam(String sliceName) throws ConfigError {
		Slice proxy = SliceImpl.getProxy();
		return proxy.getlldp_spam(sliceName);
	}
	
	
	
	// Flowisor config params.
	public static int getAPIWSPort(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getAPIWSPort(id);
	}
	
	public static int getAPIWSPort() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getAPIWSPort();
	}
	
	public static int getJettyPort(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getJettyPort(id);
	}
	
	public static int getJettyPort() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getJettyPort();
	}
	
	public static int getListenPort(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getListenPort(id);
	}

	
	public static int getListenPort() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getListenPort();
	}
	
	public static boolean getFlowTracking(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.gettrack_flows(id);
	}
	
	public static boolean getFlowTracking() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.gettrack_flows();
	}
	
	public static boolean getCheckPoint(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getCheckPoint(id);
	}
	
	public static boolean getCheckPoint() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getCheckPoint();
	}
	
	public static boolean getStatsDescHack(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getstats_desc_hack(id);
	}
	
	public static boolean getStatsDescHack() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getstats_desc_hack();
	}
	
	public static String getLogging(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getLogging(id);
	}
	
	public static String getLogging() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getLogging();
	}
	
	public static String getLogFacility(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getLogFacility(id);
	}
	
	public static String getLogFacility() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getLogFacility();
	}
	
	public static String getLogIdent(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getLogIdent(id);
	}
	
	public static String getLogIdent() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getLogIdent();
	}
	
	public static String getFloodPerm(Long dpid) throws ConfigError {
		if (dpid == null)
			return FlowvisorImpl.getProxy().getFloodPerm();
		else
			return SwitchImpl.getProxy().getFloodPerm(dpid);
	}
	
	public static Boolean getTopologyServer(int id) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getTopologyServer(id);
	}
	
	public static Boolean getTopologyServer() throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		return proxy.getTopologyServer();
	}
	
	public static void setTopologyServer(int id, Boolean topo) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setTopologyServer(id, topo);
	}
	
	public static void setTopologyServer(Boolean topo) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setTopologyServer(topo);
	}
	
	public static void setLogging(int id, String logging) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setLogging(id, logging);
	}
	
	public static void setLogging(String logging) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setLogging(logging);
	}
	
	public static void setLogFacility(int id, String facility) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setLogFacility(id, facility);
	}
	
	public static void setLogFacility(String facility) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setLogFacility(facility);
	}
	
	public static void setLogIdent(int id, String ident) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setLogFacility(id, ident);
	}
	
	public static void setLogIdent(String ident) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setLogFacility(ident);
	}
	
	public static void setStatsDescHack(int id, boolean statsHack) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setstats_desc_hack(id, statsHack);
	}
	
	public static void setStatsDescHack(boolean statsHack) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setstats_desc_hack(statsHack);
	}
	
	public static void setFlowTracking(int id, boolean track) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.settrack_flows(id, track);
	}
	
	public static void setFlowTracking(boolean track) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.settrack_flows(track);
	}
	
	public static void setListenPort(Integer port) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setListenPort(port);
	}
	
	public static void setListenPort(int id, Integer port) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setListenPort(id, port);
	}
	
	public static void setAPIWSPort(Integer port) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setAPIWSPort(port);
	}
	
	public static void setAPIWSPort(int id, Integer port) throws ConfigError {
		Flowvisor proxy = FlowvisorImpl.getProxy();
		proxy.setAPIWSPort(id, port);
	}
	
	
	
	
	/*public static boolean confirm(String base) {
		return (lookup(base) != null);
	}*/

	/**
	 * Return the name of the super user account
	 *
	 * @return
	 */
	public static boolean isSupervisor(String user) {
		return SUPER_USER.equals(user);
	}

	/**
	 * Replace all non-kosher characters with underscores
	 *
	 * @param str
	 * @return
	 */
	public static String sanitize(String str) {
		return str.replaceAll("[^a-zA-Z0-9,_+=:-]", "-");
	}

	/**
	 * Create a default config db and write it to arg1
	 *
	 * @param args
	 *            filename
	 * @throws FileNotFoundException
	 * @throws ConfigError
	 * @throws NumberFormatException
	 */

	public static void main(String args[]) throws FileNotFoundException,
			IOException, NumberFormatException, ConfigError {
		if (args.length < 1) {
			System.err
					.println("Usage: FVConfig config.xml [fvadmin_passwd] [of_listen_port] [rpc_listen_port]");
			System.exit(1);
		}
		String filename = args[0];
		String passwd;
		
		if (args.length > 1)
			passwd = args[1];
		else
			passwd = FVConfig
					.readPasswd("Enter password for account 'fvadmin' on the flowvisor:");
		
		
		System.err.println("Generating default config in db");
		
		LoadConfig.defaultConfig(passwd);
		
		FVConfigurationController.init(new ConfDBHandler());
		// set the listen port, if requested
		if (args.length > 2)
			FVConfig.setListenPort(Integer.valueOf(args[2]));
			
		if (args.length > 3)
			FVConfig.setAPIWSPort(Integer.valueOf(args[3]));
		
		
		
		System.err.println("Outputing config file " + filename);
		FVConfig.writeToFile(filename);
		
		FVConfigurationController.instance().shutdown();
		
		
	}

	public synchronized static void setPasswd(String sliceName, String salt,
			String crypt) {
		
		Slice proxy = SliceImpl.getProxy();
		try {
			proxy.setPasswd(sliceName, salt, crypt);
		} catch (ConfigError e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
	}

	
	
}
