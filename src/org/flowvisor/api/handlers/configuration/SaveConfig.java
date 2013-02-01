package org.flowvisor.api.handlers.configuration;


import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.config.FVConfig;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class SaveConfig implements ApiHandler<Object> {

	
	
	@Override
	public JSONRPC2Response process(Object params) {
	
			
		return new JSONRPC2Response(FVConfig.getConfig(), 0);
		
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.NO_PARAMS;
	}

	@Override
	public String cmdName() {
		return "save-config";
	}

}
