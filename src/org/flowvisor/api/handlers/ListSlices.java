package org.flowvisor.api.handlers;

import java.util.List;

import org.flowvisor.config.ConfigError;
import org.flowvisor.config.SliceImpl;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListSlices implements ApiHandler {

	@Override
	public JSONRPC2Response process(Object params) {
		JSONRPC2Response resp = null;
		try {
			List<String> slices = SliceImpl.getProxy().getAllSliceNames();
			resp = new JSONRPC2Response(slices, 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					"Unable to fetch slice list : " + e.getMessage()));
		} 
		return resp;
		
	}

}
