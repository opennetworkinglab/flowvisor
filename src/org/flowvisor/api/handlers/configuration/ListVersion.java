package org.flowvisor.api.handlers.configuration;

import java.util.HashMap;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.handlers.ApiHandler;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListVersion implements ApiHandler<Object> {

	
	
	@Override
	public JSONRPC2Response process(Object params) {
	
		@SuppressWarnings("serial")
		HashMap<String, String> versions = new HashMap<String, String>() {{
			put("flowvisor-version", FlowVisor.FLOWVISOR_VERSION);
			put("db-version", String.valueOf(FlowVisor.FLOWVISOR_DB_VERSION));
		}};			
			
		return new JSONRPC2Response(versions, 0);
		
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.NO_PARAMS;
	}

	@Override
	public String cmdName() {
		return "list-version";
	}

}
