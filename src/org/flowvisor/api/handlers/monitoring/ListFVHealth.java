package org.flowvisor.api.handlers.monitoring;

import java.util.HashMap;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.config.FVConfigurationController;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListFVHealth implements ApiHandler<Object> {

	
	
	@Override
	public JSONRPC2Response process(Object params) {
		JSONRPC2Response resp = null;
		HashMap<String, Integer> retvals = new HashMap<String, Integer>();
		retvals.put(AVGDELAY, -1);
		retvals.put(INSTDELAY, -1);
		retvals.put(ACTIVE, FVConfigurationController.instance().getSettings().getNumActive() );
		retvals.put(IDLE, FVConfigurationController.instance().getSettings().getNumIdle());
		
		
		resp = new JSONRPC2Response(retvals, 0);
		 
		return resp;
		
	}
	
	
	

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.NO_PARAMS;
	}

	@Override
	public String cmdName() {
		return "list-fv-health";
	}
	


}
