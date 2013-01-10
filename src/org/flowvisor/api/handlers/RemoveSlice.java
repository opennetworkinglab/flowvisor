package org.flowvisor.api.handlers;

import java.util.Map;

import org.flowvisor.config.ConfigError;
import org.flowvisor.config.InvalidSliceName;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownFieldType;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class RemoveSlice implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			String sliceName = HandlerUtils.fetchField(SLICENAME, params, String.class, true, null);
			Boolean preserve = HandlerUtils.fetchField(PRESERVE, params, Boolean.class, false, false);
			SliceImpl.getProxy().deleteSlice(sliceName, preserve);
			resp = new JSONRPC2Response(true, 0);
		} catch (UnknownFieldType e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"remove-slice: " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"remove-slice: " + e.getMessage()), 0);
		} catch (InvalidSliceName e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"remove-slice: " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					"remove-slice: Unable to delete slice : " + e.getMessage()), 0);
		} 
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
