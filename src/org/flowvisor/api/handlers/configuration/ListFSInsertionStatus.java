package org.flowvisor.api.handlers.configuration;

import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.FVConfigurationController;
import org.flowvisor.exceptions.MissingRequiredField;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListFSInsertionStatus implements ApiHandler<Map<String, Object>> {

	
	

	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		
		try {
			Integer id = HandlerUtils.<Number>fetchField(FSID, params, true, null).intValue();
			
			String status = FVConfigurationController.instance().flowSpaceStatus(id);
			resp = new JSONRPC2Response(status, 0);
		}  catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return resp;
		
	}

	
	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "list-fs-status";
	}

}
