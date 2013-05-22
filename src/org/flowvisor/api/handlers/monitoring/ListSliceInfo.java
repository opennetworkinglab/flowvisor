package org.flowvisor.api.handlers.monitoring;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.resources.SlicerLimits;
import org.flowvisor.resources.ratelimit.TokenBucket;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListSliceInfo implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		HashMap<String, Object> retvals = new HashMap<String, Object>();
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);
			retvals.put(SLICENAME, sliceName);
			retvals.put(CTRLURL, "tcp:" + 
					SliceImpl.getProxy().getcontroller_hostname(sliceName) + ":" +
					SliceImpl.getProxy().getcontroller_port(sliceName));
			retvals.put(ADMIN, SliceImpl.getProxy().getEmail(sliceName));
			retvals.put(DROP, SliceImpl.getProxy().getdrop_policy(sliceName));
			retvals.put(LLDP, SliceImpl.getProxy().getlldp_spam(sliceName));
			retvals.put(ADMINSTATUS, SliceImpl.getProxy().isSliceUp(sliceName));
			try {
				SlicerLimits sl = HandlerUtils.getSliceLimits();
				TokenBucket tb = sl.getRateLimiter(sliceName);
				if (tb != null)
					retvals.put(CURRRATE, tb.currentRate());
				retvals.put(CURRFMUSE, sl.getSliceFMLimit(sliceName));
			} catch (DPIDNotFound e) {
				FVLog.log(LogLevel.WARN, null, "No switches connected; no runtime stats available");
				retvals.put(MSG, "No switches connected; no runtime stats available");
			}
			
			resp = new JSONRPC2Response(retvals, 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
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
		return "list-slice-info";
	}
	


}
