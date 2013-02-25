package org.flowvisor.api.handlers.monitoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.BracketParse;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowDBEntry;
import org.flowvisor.flows.FlowRewriteDB;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.slicer.FVSlicer;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListDatapathFlowRewriteDB implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		
		try {
			Long dpid = FlowSpaceUtil.parseDPID(
					HandlerUtils.<String>fetchField(FlowSpace.DPID, params, true, null));
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);
			

			
			
			FVSlicer fvSlicer = HandlerUtils.getClassifierByDPID(dpid).getSlicerByName(sliceName);
			Map<String, List<Map<String, Object>>> ret = new HashMap<String, List<Map<String, Object>>>();
			FlowRewriteDB flowRewriteDB = fvSlicer.getFlowRewriteDB();
			synchronized (flowRewriteDB) {
				for (FlowDBEntry original : flowRewriteDB.originals()) {
					Map<String, String> originalMap = original.toBracketMap();
					List<Map<String, Object>> rewrites = new LinkedList<Map<String, Object>>();
					for (FlowDBEntry rewrite : flowRewriteDB.getRewrites(original)) {
						rewrites.add(rewrite.toMap());
					}
					ret.put(BracketParse.encode(originalMap), rewrites);
				}
			}
	
			
			resp = new JSONRPC2Response(ret, 0);
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
