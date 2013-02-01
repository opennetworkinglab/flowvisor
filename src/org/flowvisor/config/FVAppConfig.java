package org.flowvisor.config;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;

public interface FVAppConfig {
/*
 * This is an empty interface, and its supposed to be like that!
 * It is used so that we don't have to create multiple proxies 
 * for each datatype.
 * 
 * well it's not exactly empty but whatever, the below methods don't count.
 */
	public void setSettings(ConfDBSettings settings);
	public void close(Object o);
	public void close(Connection conn);
	public void notify(Object key, String method, Object newValue);
	public HashMap<String, Object> toJson(HashMap<String,Object> output);
	public void fromJson(ArrayList<HashMap<String, Object>> input) throws IOException;
	
	public void updateDB(int version);
	
}
