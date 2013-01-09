package org.flowvisor.api.handlers;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public interface ApiHandler {

	public <T> JSONRPC2Response process(T params);
	
}
