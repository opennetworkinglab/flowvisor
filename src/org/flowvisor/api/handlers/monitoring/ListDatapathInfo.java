package org.flowvisor.api.handlers.monitoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowSpaceUtil;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.util.U16;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListDatapathInfo implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		HashMap<String, Object> retvals = new HashMap<String, Object>();
		List<Integer> portnos = new LinkedList<Integer>();
		List<String> portNames = new LinkedList<String>();
		try {
			Long dpid = FlowSpaceUtil.parseDPID(
					HandlerUtils.<String>fetchField(FlowSpace.DPID, params, true, null));
			FVClassifier classifier = HandlerUtils.getClassifierByDPID(dpid);
			getPortLists(classifier, portnos, portNames);
			retvals.put(FlowSpace.DPID, FlowSpaceUtil.dpidToString(dpid));
			retvals.put(NUMPORTS, classifier.getSwitchInfo().getPorts().size());
			retvals.put(PORTLIST, portnos);
			retvals.put(PORTNAMES, portNames);
			retvals.put(CONNNAME, classifier.getConnectionName());
			retvals.put(CURRFMUSE, getDPIDFMLimits(classifier));
			
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
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		}  
		return resp;
		
	}

	private HashMap<String, Integer> getDPIDFMLimits(FVClassifier classifier) 
			throws ConfigError {
		List<String> slices = HandlerUtils.getAllSlices();
		HashMap<String, Integer> ret = new HashMap<String, Integer>();
		int limit = 0;
		for (String slice : slices) {
			limit = classifier.getCurrentFlowModCounter(slice);
			if (limit != -1)
				ret.put(slice, limit);
		}
		return ret;
	}







	private void getPortLists(FVClassifier classifier, List<Integer> portnos,
			List<String> portNames) {
		for (OFPhysicalPort port : classifier.getSwitchInfo().getPorts()) {
			portnos.add(U16.f(port.getPortNumber()));
			portNames.add(port.getName());
		}
		
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
