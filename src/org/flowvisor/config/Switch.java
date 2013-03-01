package org.flowvisor.config;

import org.openflow.protocol.OFFlowMod;

public interface Switch extends FVAppConfig {
	
	// COLUMN NAMES
	public static String FLOOD = "flood_perm";
	public static String DPID = "dpid";
	public static final String MFRDESC = "mfr_desc";
	public static final String HWDESC = "hw_desc";
	public static final String SWDESC = "sw_desc";
	public static final String SERIAL = "serial_num";
	public static final String DPDESC = "dp_desc";
	public static final String CAPA = "capabilities";
	
	public static final String SWITCH_ID = "switch_id";
	public static final String SLICE_ID = "slice_id";
	public static final String FMLIMIT = "maximum_flow_mods";
	public static final String RATELIMIT = "rate_limit";
	
	public static final String LIMITS = "limits";
	
	//Table name
	public static String TSWITCH = "Switch";
	public static String TLIMIT = "jSliceSwitchLimits";
	
	public static String SWITCH = "switches";
	
	public String getFloodPerm(Long dpid) throws ConfigError;
	public Integer getMaxFlowMods(String sliceName, Long dp) throws ConfigError;
	public Integer getRateLimit(String sliceName, Long dp) throws ConfigError;
	
	public void setFloodPerm(Long dpid, String flood_perm) throws ConfigError;	
	public void setMaxFlowMods(String sliceName, Long dp, int limit) throws ConfigError;
	public void setRateLimit(String sliceName, Long dp, int rate) throws ConfigError;
	
	public int pushFlowMod(OFFlowMod flowMod, String sliceName, long dpid);

	public void pullFlowMod(int id);
	
}
