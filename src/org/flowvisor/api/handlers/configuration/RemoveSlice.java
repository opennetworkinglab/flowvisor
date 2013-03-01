package org.flowvisor.api.handlers.configuration;

import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.config.InvalidSliceName;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class RemoveSlice implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			/*
			 * TODO: put notifyChange in futureTask
			 */
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);
			//Boolean preserve = HandlerUtils.<Boolean>fetchField(PRESERVE, params, false, false);
			SliceImpl.getProxy().deleteSlice(sliceName);
			FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
			FlowSpaceImpl.getProxy().notifyChange(flowSpace);
			resp = new JSONRPC2Response(true, 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (InvalidSliceName e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} 
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "remove-slice";
	}

}
