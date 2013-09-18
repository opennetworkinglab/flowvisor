package org.flowvisor.api.handlers;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public interface ApiHandler<T> {
	
	final static String CTRLURL = "controller-url";
    final static String CTRLHOST = "controller-host";
	final static String CTRLPORT = "controller-port";
	final static String SLICENAME = "slice-name";
	final static String DROP = "drop-policy";
	final static String LLDP = "recv-lldp";
	final static String ADMIN = "admin-contact";
	final static String MAX = "flowmod-limit";
	final static String RATE = "rate-limit";
	final static String ADMINSTATUS = "admin-status";
	final static String PRESERVE = "preserve-flowspace";
	final static String PASS = "password";
	final static String FSNAME = "name";
	final static String MATCH = "match";
	final static String SLICEACTIONS = "slice-action";
	final static String PERM = "permission";
	final static String FLOOD = "flood-perm";
	final static String TRACK = "track-flows";
	final static String STATSDESC = "stats-desc";
	final static String TOPOCTRL = "enable-topo-ctrl";
	final static String FMLIMIT = "limit";
	final static String FSCACHE = "flow-stats-cache";
	final static String FQUEUE = "force-enqueue";
	final static String QUEUE = "queues";
	final static String SHOW = "show-disabled";
	final static String FSID = "fs-id";
	//Additions to get config
	public static String APIPORT = "api_webserver_port";
	public static String CONFIG = "config_name";
	public static String JETTYPORT = "api_jetty_webserver_port";
	public static String LOGFACILITY = "log_facility";
	public static String LOGGING = "logging";
	public static String CHECKPOINT = "checkpointing";
	public static String LOGIDENT = "log_ident";
	public static String VERSION = "version";
	public static String HOST = "host";
	public static String DB_VERSION = "db_version";
	
	final static String MSG = "msg";
	final static String CURRRATE = "current-rate";
	final static String CURRFMUSE = "current-flowmod-usage";
	final static String NUMPORTS = "num-ports";
	final static String PORTLIST = "port-list";
	final static String PORTNAMES = "port-names";
	final static String CONNNAME = "connection";
	final static String AVGDELAY = "average-delay";
	final static String INSTDELAY = "instant-delay";
	final static String ACTIVE = "active-db-sessions";
	final static String IDLE = "idle-db-sessions";
	final static String CONNECTED = "is-connected";
	final static String CONNCOUNT = "connect-drop-count";
	final static String FSENTRIES = "fs-entries";
	final static String CONNDPIDS = "connected-dpids";
	final static String URL = "url";
	final static String METHOD = "method";
	final static String EVENT = "event-type";
	final static String COOKIE = "cookie";

	public JSONRPC2Response process(T params);
	
	public JSONRPC2ParamsType getType();
	
	public String cmdName();
	
}
