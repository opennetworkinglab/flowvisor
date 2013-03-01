package org.flowvisor.api.handlers.monitoring;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowDBEntry;
import org.flowvisor.flows.FlowSpaceUtil;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListDatapathFlowDB implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		List<Map<String, Object>> retvals = new LinkedList<Map<String, Object>>();
		
		try {
			Long dpid = FlowSpaceUtil.parseDPID(
					HandlerUtils.<String>fetchField(FlowSpace.DPID, params, true, null));
			FVClassifier classifier = HandlerUtils.getClassifierByDPID(dpid);
			for (FlowDBEntry fbe : classifier.getFlowDB()) {
				retvals.add(fbe.toMap());
			}
			
			resp = new JSONRPC2Response(retvals, 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (DPIDNotFound e) {
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
		return "list-datapath-info";
	}
	


}
