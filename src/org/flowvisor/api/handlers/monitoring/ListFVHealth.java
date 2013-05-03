package org.flowvisor.api.handlers.monitoring;

import java.util.HashMap;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.events.FVEventUtils;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListFVHealth implements ApiHandler<Object> {

	
	
	@Override
	public JSONRPC2Response process(Object params) {
		JSONRPC2Response resp = null;
		HashMap<String, Object> retvals = new HashMap<String, Object>();
		retvals.put(AVGDELAY, FVEventUtils.averageDelay);
		retvals.put(INSTDELAY, FVEventUtils.instDelay);
		/*retvals.put(ACTIVE, FVConfigurationController.instance().getSettings().getNumActive() );
		retvals.put(IDLE, FVConfigurationController.instance().getSettings().getNumIdle());*/
		
		
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
