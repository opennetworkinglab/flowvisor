package org.flowvisor.api.handlers.monitoring;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.ofswitch.TopologyController;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class RegisterEventCallback implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			
			
			String url = HandlerUtils.<String>fetchField(URL, params, true, null);
			String method = HandlerUtils.<String>fetchField(METHOD, params, true, null);
			String eventType = HandlerUtils.<String>fetchField(EVENT, params, true, null);
			String cookie = HandlerUtils.<String>fetchField(COOKIE, params, true, null);
			
			new URL(url);
			TopologyController tc = TopologyController.getRunningInstance();
			if (tc != null) {
				tc.registerCallBack(APIUserCred.getUserName(), url, method, cookie, eventType);
				resp = new JSONRPC2Response(true, 0);
			} else
				resp = new JSONRPC2Response(false, 0);
			
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MalformedURLException e) {
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
		return "list-slice-stats";
	}
	


}
