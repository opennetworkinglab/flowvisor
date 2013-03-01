package org.flowvisor.api.handlers.monitoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.slicer.FVSlicer;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListSliceHealth implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		HashMap<String, Object> retvals = new HashMap<String, Object>();
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);
			FVSlicer slicer = HandlerUtils.getSlicerByName(sliceName);
			retvals.put(CONNECTED, slicer.isConnected());
			retvals.put(CONNCOUNT, slicer.getConnectCount());
			retvals.put(FSENTRIES, HandlerUtils.getSliceLimits().getSliceFMLimit(sliceName)); 
			retvals.put(CONNDPIDS, getConnectedDpid(sliceName));
			resp = new JSONRPC2Response(retvals, 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (DPIDNotFound e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		}
		
		
		
		 
		return resp;
		
	}

	private List<String> getConnectedDpid(String sliceName) {
		List<String> list = new LinkedList<String>();
		for (FVClassifier classifier : HandlerUtils.getAllClassifiers())
			if (classifier.getSlicerByName(sliceName) != null && classifier.isIdentified())
				list.add(FlowSpaceUtil.dpidToString(classifier.getDPID()));
		
		return list;
	}


	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "list-slice-health";
	}
	


}
