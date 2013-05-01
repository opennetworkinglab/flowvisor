package org.flowvisor.api.handlers.configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfigurationController;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownMatchField;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.action.OFAction;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AddFlowSpace implements ApiHandler<List<Map<String, Object>>> {

	
	
	@Override
	public JSONRPC2Response process(List<Map<String, Object>> params) {
		JSONRPC2Response resp = null;
	
		try {
			final List<FlowEntry> list = processFlows(params/*, flowSpace*/);
			int index = FVConfigurationController.instance().pendFlowSpace(list);
			resp = new JSONRPC2Response(index, 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
			e.printStackTrace();
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": failed to insert flowspace entry" + e.getMessage()), 0);
		} catch (UnknownMatchField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": Unknown field(s) in match struct : " + e.getMessage()), 0);
		}
		return resp;
		
	}

	private List<FlowEntry> processFlows(List<Map<String, Object>> params/*, FlowMap flowSpace*/) 
			throws ClassCastException, MissingRequiredField, ConfigError, UnknownMatchField {
		String name = null;
		Long dpid = null;
		Integer priority = null;
		FlowEntry fentry = null;
		List<FlowEntry> list = new LinkedList<FlowEntry>();
		for (Map<String,Object> fe : params) {
			name = HandlerUtils.<String>fetchField(FSNAME, fe, false, UUID.randomUUID().toString());
			
			dpid = FlowSpaceUtil
					.parseDPID(HandlerUtils.<String>fetchField(FlowSpace.DPID, fe, true, "any"));
			priority = HandlerUtils.<Number>fetchField(FlowSpace.PRIO, fe, true, FlowEntry.DefaultPriority).intValue();
			FVMatch match = HandlerUtils.matchFromMap(
					HandlerUtils.<Map<String, Object>>fetchField(MATCH, fe, true, null));
			
			List<OFAction> sliceActions = parseSliceActions(
					HandlerUtils.<List<Map<String, Object>>>fetchField(SLICEACTIONS, fe, true, null));
			//List<FlowEntry> fes = flowSpace.matches(dpid, match);
			
			List<Integer> l = new LinkedList<Integer>();
			for (Number n : HandlerUtils.<List<Number>>fetchField(QUEUE, fe, false, 
											new LinkedList<Number>()))
				l.add(n.intValue());
			
			
			Number fqueue = HandlerUtils.<Number>fetchField(FQUEUE, fe, false, -1);
			
			
			fentry = new FlowEntry(name, dpid, match, 0, priority, 
					(List<OFAction>) sliceActions);
			fentry.setQueueId(l);
			fentry.setForcedQueue(fqueue.longValue());
			list.add(fentry);
			
		}
		return list;
		
	}
	
	

	private List<OFAction> parseSliceActions(List<Map<String, Object>> sactions) 
			throws ClassCastException, MissingRequiredField {
		List<OFAction> sa = new LinkedList<OFAction>();
		for (Map<String, Object> sact : sactions) {
			SliceAction sliceAction = new SliceAction(
					HandlerUtils.<String>fetchField(SLICENAME, sact, true, null),
					HandlerUtils.<Number>fetchField(PERM, sact, true, null).intValue());
			sa.add(sliceAction);
		}
		return sa;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.ARRAY;
	}

	@Override
	public String cmdName() {
		return "add-flowspace";
	}
	
	
	
	
	
}
