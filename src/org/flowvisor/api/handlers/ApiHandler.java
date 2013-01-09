package org.flowvisor.api.handlers;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public interface ApiHandler<T> {

	public JSONRPC2Response process(T params);
	
	public JSONRPC2ParamsType getType();
	
}
