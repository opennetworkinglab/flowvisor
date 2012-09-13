package org.flowvisor.config;

/**
 * A proxy interface for flowvisor specific 
 * information
 * 
 * 
 * @author ash
 *
 */
public interface Flowvisor extends FVAppConfig {
	
	// COLUMN NAMES
	public static String TRACK = "track_flows";
	public static String STATS = "stats_desc_hack";
	public static String LISTEN = "listen_port";
	public static String APIPORT = "api_webserver_port";
	public static String CHECKPOINT = "checkpointing";
	public static String JETTYPORT = "api_jetty_webserver_port";
	public static String FLOODPERM = "default_flood_perm";
	public static String LOGIDENT = "log_ident";
	public static String LOGGING = "logging";
	public static String LOGFACILITY = "log_facility";
	public static String TOPO = "run_topology_server";
	public static String VERSION = "version";
	public static String HOST = "host";
	public static String CONFIG = "config_name";
	
	// Table name
	public static String FLOWVISOR = "flowvisor";
	
	public Boolean gettrack_flows(Integer id) throws ConfigError;
	public Boolean gettrack_flows() throws ConfigError;
	public Boolean getstats_desc_hack(Integer id);
	public Boolean getstats_desc_hack();
	public Integer getAPIWSPort(Integer id) throws ConfigError;
	public Integer getAPIWSPort() throws ConfigError;
	public Integer getJettyPort(Integer id) throws ConfigError;
	public Integer getJettyPort() throws ConfigError;
	public Integer getListenPort(Integer id) throws ConfigError;
	public Integer getListenPort() throws ConfigError;
	public Boolean getCheckPoint(Integer id) throws ConfigError;
	public Boolean getCheckPoint() throws ConfigError;
	public String getFloodPerm(Integer id) throws ConfigError;
	public String getFloodPerm() throws ConfigError;
	public String getLogIdent(Integer id) throws ConfigError;
	public String getLogIdent() throws ConfigError;
	public String getLogging(Integer id) throws ConfigError;
	public String getLogging() throws ConfigError;
	public String getLogFacility(Integer id) throws ConfigError;
	public String getLogFacility() throws ConfigError;
	public Boolean getTopologyServer(int id) throws ConfigError;
	public Boolean getTopologyServer() throws ConfigError;
	
	
	public void settrack_flows(Integer id, Boolean track_flows);
	public void settrack_flows(Boolean track_flows);
	public void setstats_desc_hack(Integer id, Boolean stats_desc_hack);
	public void setstats_desc_hack(Boolean stats_desc_hack);
	public void setFloodPerm(Integer id, String floodPerm);
	public void setFloodPerm(String floodPerm);
	public void setLogging(Integer id, String logging);
	public void setLogging(String logging);
	public void setLogFacility(Integer id, String logging);
	public void setLogFacility(String logging);
	public void setLogIdent(Integer id, String logging);
	public void setLogIdent(String logging);
	public void setTopologyServer(Integer id, Boolean topo) throws ConfigError;
	public void setTopologyServer(Boolean topo) throws ConfigError;
	public void setListenPort(Integer id, Integer port) throws ConfigError;
	public void setListenPort(Integer port) throws ConfigError;
	public void setAPIWSPort(Integer id, Integer port) throws ConfigError;
	public void setAPIWSPort(Integer port) throws ConfigError;
	public void setJettyPort(Integer id, Integer port) throws ConfigError;
	public void setJettyPort(Integer port) throws ConfigError;
	
	
}
