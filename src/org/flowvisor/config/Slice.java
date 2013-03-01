package org.flowvisor.config;


import java.util.LinkedList;

import org.flowvisor.exceptions.DuplicateControllerException;



public interface Slice extends FVAppConfig {
	
	// COLUMN NAMES
	public static String LLDP = "lldp_spam";
	public static String DROP = "drop_policy";
	public static String HOST = "controller_hostname";
	public static String PORT = "controller_port";
	public static String SLICE = "name";
	public static String CREATOR = "creator";
	public static String EMAIL = "contact_email";
	public static String CRYPT = "passwd_crypt";
	public static String SALT = "passwd_salt";
	public static String FMTYPE = "flowmap_type";
	public static String FLOWVISORID = "flowvisor_id";
	public static String FMLIMIT = "max_flow_rules";
	public static String ADMINDOWN = "admin_status";
	
	// Table name
	public static String TSLICE = "Slice";
	
	public void setlldp_spam(String sliceName, Boolean lldp_spam);
	public void setdrop_policy(String sliceName, String policy);
	public void setcontroller_hostname(String sliceName, String name) throws ConfigError;
	public void setcontroller_port(String sliceName, Integer port) throws ConfigError;
	public void setContactEmail(String sliceName, String email) throws ConfigError;
	public void setPasswd(String sliceName, String salt, String crypt) throws ConfigError;
	public void setMaxFlowMods(String sliceName, int limit) throws ConfigError;
	
	public Boolean getlldp_spam(String sliceName);
	public String getdrop_policy(String sliceName);
	public String getcontroller_hostname(String sliceName) throws ConfigError;
	public Integer getcontroller_port(String sliceName) throws ConfigError;
	public String getPasswdElm(String sliceName, String elm) throws ConfigError;
	public String getCreator(String sliceName) throws ConfigError;
	public String getEmail(String sliceName) throws ConfigError ;
	public LinkedList<String> getAllSliceNames() throws ConfigError;
	public Integer getMaxFlowMods(String sliceName) throws ConfigError;
	
	
	public Boolean checkSliceName(String sliceName);
	
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String slice_email, String creatorSlice, int flowvisor_id, int type) throws InvalidSliceName,
			DuplicateControllerException;
	
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String slice_email, String creatorSlice, int flowvisor_id)
					throws InvalidSliceName, DuplicateControllerException;
	
	public void createSlice(String sliceName,
			String controller_hostname, int controller_port, String drop_policy, String passwd,
			String slice_email, String creatorSlice) 
					throws InvalidSliceName, DuplicateControllerException;
	
	public void createSlice(String sliceName,
			String controller_hostname, int controller_port, String drop_policy, String passwd,
			String salt, String slice_email, String creatorSlice)
					throws InvalidSliceName, DuplicateControllerException;
	
	void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String salt, String slice_email, String creatorSlice,
			int flowvisor_id, int type) 
					throws InvalidSliceName, DuplicateControllerException;
	
	public void deleteSlice(String SliceName) throws InvalidSliceName;
	
	// ADDED FOR JSONRPC 
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String salt, String slice_email, String creatorSlice, boolean lldp_spam, 
			int maxFlowMods, int flowvisor_id, int type)
			throws DuplicateControllerException;
	
	public void deleteSlice(String SliceName, Boolean preserve) throws InvalidSliceName, ConfigError;
	
	public void setAdminStatus(String sliceName, boolean status);
	boolean isSliceUp(String sliceName);
		

	
}
