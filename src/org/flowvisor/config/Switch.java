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
	
	//Table name
	public static String TSWITCH = "Switch";
	
	public static String SWITCH = "switches";
	
	public String getFloodPerm(Long dpid) throws ConfigError;
	
	public void setFloodPerm(Long dpid, String flood_perm) throws ConfigError;	
	
	public int pushFlowMod(OFFlowMod flowMod, String sliceName, long dpid);

	public void pullFlowMod(int id);
}
