package org.flowvisor.config;

import java.io.IOException;
import java.sql.Connection;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public interface FVAppConfig {
/*
 * This is an empty interface, and its supposed to be like that!
 * It is used so that we don't have to create multiple proxies 
 * for each datatype.
 * 
 * well it's not exactly empty but whatever, the below method doesn't count.
 */
	public void setSettings(ConfDBSettings settings);
	public void close(Object o);
	public void close(Connection conn);
	public void notify(Object key, String method, Object newValue);
	public void toJson(JsonWriter writer) throws IOException;
	public void fromJson(JsonReader reader) throws IOException;
	
}
