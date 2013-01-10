package org.flowvisor.api.handlers;

import java.util.Map;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownFieldType;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListFlowSpace implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			String sliceName = HandlerUtils.fetchField(SLICENAME, params, String.class, false, null);
			String user = APIUserCred.getUserName();
			FlowMap fm = null;
			if (FVConfig.isSupervisor(user)) {
				if (sliceName == null)
					fm = FlowSpaceImpl.getProxy().getFlowMap();
				else
					fm = FlowSpaceUtil.getSliceFlowSpace(sliceName);
			} else
				fm = FlowSpaceUtil.getSliceFlowSpace(user);
			resp = new JSONRPC2Response(fm.getRules(), 0);
		}  catch (UnknownFieldType e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"list-flowspace: " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"list-flowspace: " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					"list-flowspace: Unable to set slice password : " + e.getMessage()), 0);
		}  
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
