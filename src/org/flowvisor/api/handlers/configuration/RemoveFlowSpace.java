package org.flowvisor.api.handlers.configuration;

import java.util.List;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.flows.FlowMap;


import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class RemoveFlowSpace implements ApiHandler<List<String>> {

	
	
	@Override
	public JSONRPC2Response process(List<String> params) {
		if (params.size() < 1)
			return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": Nothing to remove"), 0);
		JSONRPC2Response resp = null;
		try {
			final FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
			for (String name : params) {
				flowSpace.removeRule(flowSpace.findRuleByName(name).getId());
			}
			FlowSpaceImpl.getProxy().removeRuleByName(params);
			FlowSpaceImpl.getProxy().notifyChange(flowSpace);
			resp = new JSONRPC2Response(true, 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					"remove-flowspace: Unable to get flowspace : " + e.getMessage()), 0);
		} catch (FlowEntryNotFound e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					"remove-flowspace: unable to find flow entry : " + e.getMessage()), 0);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.ARRAY;
	}

	@Override
	public String cmdName() {
		return "remove-flowspace";
	}

}
