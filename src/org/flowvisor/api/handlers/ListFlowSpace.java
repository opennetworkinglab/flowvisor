package org.flowvisor.api.handlers;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.MissingRequiredField;


import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListFlowSpace implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, false, null);
			String user = APIUserCred.getUserName();
			HashMap<String, Object> map = new HashMap<String, Object>();
			if (FVConfig.isSupervisor(user)) {
				if (sliceName == null) 
					FlowSpaceImpl.getProxy().toJson(map, null);
				else
					FlowSpaceImpl.getProxy().toJson(map, sliceName);
			} else
				FlowSpaceImpl.getProxy().toJson(map, user);
			resp = new JSONRPC2Response(map.get(FlowSpace.FS), 0);
		}  catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": Unable to get flowspace : " + e.getMessage()), 0);
		}  
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "list-flowspace";
	}

}
