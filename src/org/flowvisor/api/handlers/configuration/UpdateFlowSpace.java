package org.flowvisor.api.handlers.configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfigurationController;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownMatchField;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.action.OFAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class UpdateFlowSpace implements ApiHandler<List<Map<String, Object>>> {

	
	
	@Override
	public JSONRPC2Response process(final List<Map<String, Object>> params) {
		JSONRPC2Response resp = null;
		try {
			
			int index = processFlows(params);	
			//int index = FVConfigurationController.instance().pendFlowSpace(list);
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
		} catch (FlowEntryNotFound e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + "Unable to find flowspace entry :" + e.getMessage()), 0);
			
		} catch (UnknownMatchField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": Unknown field(s) in match struct : " + e.getMessage()), 0);
		}
		return resp;
		
	}

	private int processFlows(List<Map<String, Object>> params) 
			throws ClassCastException, MissingRequiredField, ConfigError, FlowEntryNotFound, UnknownMatchField {
		String name = null;
		Long dpid = null;
		Number priority = null;
		FlowEntry update = null;
		//LinkedList<FlowEntry> list = new LinkedList<FlowEntry>();
		FlowMap flowSpace = FlowSpaceImpl.getProxy().getFlowMap();
		for (Map<String,Object> fe : params) {
			name = HandlerUtils.<String>fetchField(FSNAME, fe, false, null);
			if (name == null)
				throw new MissingRequiredField("Cannot update flowspace entry without a name.");
			update = flowSpace.findRuleByName(name);
		
			
			String dpidStr = HandlerUtils.<String>fetchField(FlowSpace.DPID, fe, false, null);
			if (dpidStr != null) {
				dpid = FlowSpaceUtil.parseDPID(dpidStr);
				update.setDpid(dpid);
			
			}
			
			priority = HandlerUtils.<Number>fetchField(FlowSpace.PRIO, fe, false, null);
			if (priority != null) {
				update.setPriority(priority.intValue());
			
			}
			
			/*
			 * TODO: Once XMLRPC API goes away.
			 * this will be more pretty.
			 */
			FVMatch match = HandlerUtils.matchFromMap(
					HandlerUtils.<Map<String, Object>>fetchField(MATCH, fe, false, null));
			if (match != null) {
				match.setQueues(update.getQueueId());
				match.setForcedQueue(update.getForcedQueue());
				update.setRuleMatch(match);
			
			}
			
			
			List<Map<String,Object>> sacts = 
					HandlerUtils.<List<Map<String, Object>>>fetchField(SLICEACTIONS, fe, false, null);
			if (sacts != null) {
				update.setActionsList(parseSliceActions(sacts));
			
			}
			
			List<Integer> l = new LinkedList<Integer>();
			List<Number> origL = HandlerUtils.<List<Number>>fetchField(QUEUE, fe, false, null);
			if (origL != null) {
				for (Number n : origL)
					l.add(n.intValue());
				update.setQueueId(l);
			
			}
			
			
			
			Number fqueue = HandlerUtils.<Number>fetchField(FQUEUE, fe, false, null);
			if (fqueue != null) {
				update.setForcedQueue(fqueue.longValue());
			
			}
			//list.add(update);
			//updateFlowEntry(flowSpace, update);
		}
		try {
                        flowSpace.removeRule(update.getId());
                        FlowSpaceImpl.getProxy().removeRule(update.getId());
                        FlowSpaceImpl.getProxy().addRule(update);
                        flowSpace.addRule(update);
                } catch (ConfigError e) {
                        FVLog.log(LogLevel.DEBUG, null, e.getMessage());
                }

		return update.getId();
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
		return "update-flowspace";
	}
	
}
