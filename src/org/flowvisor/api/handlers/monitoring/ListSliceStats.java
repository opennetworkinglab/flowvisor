package org.flowvisor.api.handlers.monitoring;

import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.slicer.FVSlicer;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListSliceStats implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);
			if (!HandlerUtils.sliceExists(sliceName))
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
						cmdName() + ": slice does not exist : " + sliceName), 0);
			FVSlicer slicer = HandlerUtils.getSlicerByName(sliceName);
			if (slicer == null)
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
						cmdName() + ": " + SendRecvDropStats.NO_STATS_AVAILABLE_MSG + " : " + sliceName), 0);
			
			resp = new JSONRPC2Response(slicer.getStats().toMap(), 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": failed to fetch slice stats" + e.getMessage()), 0);
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
