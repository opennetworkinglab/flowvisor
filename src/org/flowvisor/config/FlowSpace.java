package org.flowvisor.config;

import java.util.HashMap;
import java.util.List;

import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;

/**
 * A proxy interface for the flowspace db table. 
 * It handles all the logistics to get flow rules 
 * in and out of the data repository.
 * 
 * @author ash
 *
 */
public interface FlowSpace extends FVAppConfig {
	
	//COLUMN NAMES
	public static String DPID = "dpid";
	public static String PRIO = "priority";
	public static String INPORT = "in_port";
	public static String VLAN = "dl_vlan";
	public static String VPCP = "dl_vpcp";
	public static String DLSRC = "dl_src";
	public static String DLDST = "dl_dst";
	public static String DLTYPE = "dl_type";
	public static String NWSRC = "nw_src";
	public static String NWDST = "nw_dst";
	public static String NWPROTO = "nw_proto";
	public static String NWTOS = "nw_tos";
	public static String TPSRC = "tp_src";
	public static String TPDST = "tp_dst";
	public static String WILDCARDS = "wildcards";
	public static String ACTION = "slice_action";
	public static String QUEUE = "queue_id";
	public static String FORCED_QUEUE = "forced_queue";
	public static String NAME = "name";
	
	//Table name
	public static String FS = "FlowSpaceRule";
	
	/**
	 * Reads the flowspace from the data repository
	 * and returns it
	 * 
	 * @return a flowmap
	 * @throws ConfigError an error occurred while 
	 * reading the data repository.
	 */
	public FlowMap getFlowMap() throws ConfigError;
	
	
	/**
	 * Writes the flowmap to the data repository.
	 * 
	 * Notifies the flowspace change!
	 * 
	 * @param map the flowmap to be written
	 * 
	 * @throws ConfigError an error occurred 
	 * during the write process.
	 * 
	 */
	public void setFlowMap(FlowMap map) throws ConfigError;
	
	
	/**
	 * Removes a single FlowSpaceRule
	 * 
	 * Does not Notify of the flowspace change!
	 * 
	 * @param id the id of the rule to remove
	 * 
	 * @throws ConfigError if this rule is not found.
	 */
	public void removeRule(int id) throws ConfigError;


	/**
	 * Add the rule to the FlowSpace
	 * 
	 * Does not Notify of the flowspace change!
	 * 
	 * @param entry the rule to add
	 * @return id of the rule
	 */
	public int addRule(FlowEntry fe) throws ConfigError;

	/**
	 * Preserve the flowspace for future usage.
	 * 
	 * @param sliceName - flowspace for sliceName will be saved.
	 * @throws ConfigError
	 */
	public void saveFlowSpace(String sliceName) throws ConfigError;
	
	public void notifyChange(FlowMap map);

	
	public HashMap<String, Object> toJson(HashMap<String, Object> map, String sliceName, Boolean show) throws ConfigError;


	public void removeRuleByName(List<String> names) throws ConfigError;

	
	
}
