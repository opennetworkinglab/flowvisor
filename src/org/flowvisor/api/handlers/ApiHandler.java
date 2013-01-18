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
	final static String PRESERVE = "preserve-flowspace";
	final static String PASS = "password";
	final static String FSNAME = "name";
	final static String MATCH = "match";
	final static String SLICEACTIONS = "slice-action";
	final static String PERM = "permission";
	

	public JSONRPC2Response process(T params);
	
	public JSONRPC2ParamsType getType();
	
	public String cmdName();
	
}
