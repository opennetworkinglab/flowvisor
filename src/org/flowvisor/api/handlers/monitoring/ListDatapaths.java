package org.flowvisor.api.handlers.monitoring;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListDatapaths implements ApiHandler<Object> {

	
	
	@Override
	public JSONRPC2Response process(Object params) {
		JSONRPC2Response resp = null;
		
		
		resp = new JSONRPC2Response(HandlerUtils.getAllDevices(), 0);
		 
		return resp;
		
	}

	

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.NO_PARAMS;
	}

	@Override
	public String cmdName() {
		return "list-datapaths";
	}
	


}
