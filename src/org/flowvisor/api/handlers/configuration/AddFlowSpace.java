package org.flowvisor.api.handlers.configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FVConfigurationController;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownMatchField;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AddFlowSpace implements ApiHandler<List<Map<String, Object>>> {

	
	
	@Override
	public JSONRPC2Response process(List<Map<String, Object>> params) {
		FVLog.log(LogLevel.DEBUG, null,
				"process AddFlowSpace");
		JSONRPC2Response resp = null;
		try {
			final FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
			final List<FlowEntry> list = processFlows(params, flowSpace);
			
			FutureTask<Object> future = new FutureTask<Object>(
	                new Callable<Object>() {
	                    public Object call() {
	                    	addFlowEntries(list, flowSpace);
							FVLog.log(LogLevel.INFO, null,
									"Signalling FlowSpace Update to all event handlers");
							FlowSpaceImpl.getProxy().notifyChange(flowSpace);
							return null;
	                    }
	                });
	                    
			FVConfigurationController.instance().execute(future);	
			resp = new JSONRPC2Response(true, 0);
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

	private List<FlowEntry> processFlows(List<Map<String, Object>> params, FlowMap flowSpace) 
			throws ClassCastException, MissingRequiredField, ConfigError, UnknownMatchField {
		FVLog.log(LogLevel.DEBUG, null,
				"processFlows of AddFlowSpace");
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
	
	private void addFlowEntries(List<FlowEntry> entries, FlowMap flowSpace) {
		for (FlowEntry fentry : entries) {
			try {
				FlowSpaceImpl.getProxy().addRule(fentry);
				
				flowSpace.addRule(fentry);
				String logMsg = "User " + APIUserCred.getUserName() + 
						flowspaceAddChangeLogMessage(fentry.getDpid(), 
								fentry.getRuleMatch(), fentry.getPriority(),
								fentry.getActionsList(), fentry.getName());
				FVLog.log(LogLevel.INFO, null, logMsg);
			} catch (ConfigError e) {
				FVLog.log(LogLevel.WARN, null, e.getMessage());
			}
		}	
	}
	
	

	private List<OFAction> parseSliceActions(List<Map<String, Object>> sactions) 
			throws ClassCastException, MissingRequiredField {
		FVLog.log(LogLevel.DEBUG, null,
				"Inside parseSliceActions in AddFlowSpace");
		List<OFAction> sa = new LinkedList<OFAction>();
		/*for (Map<String, Object> sact : sactions) {
			SliceAction sliceAction = new SliceAction(
					HandlerUtils.<String>fetchField(SLICENAME, sact, true, null),
					HandlerUtils.<Number>fetchField(PERM, sact, true, null).intValue());
			sa.add(sliceAction);
		}*/
		for (Map<String, Object> sact : sactions) {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, sact, true, null);
			String perm = HandlerUtils.<String>fetchField(PERM, sact, true, null);
			int permission=0;
			if (perm.matches("[rwd]+"))
				permission = permToInt(perm);
			else if(perm.matches("[0-9]+"))
				permission = Integer.parseInt(perm);	
			SliceAction sliceAction = new SliceAction(
					sliceName, permission);
			sa.add(sliceAction);
		}
		return sa;
	}

	private int permToInt(String perm) {
		int len = perm.length();
		int permission=0;
		FVLog.log(LogLevel.DEBUG, null,
				"The len of the string  is: "+len);
		int index=0;
		while(index<len){
			//System.out.println("Char at index "+index + "is: "+perm.charAt(index));
			if(perm.charAt(index)=='d' || perm.charAt(index)=='D')
				permission+=1;
			else if(perm.charAt(index)=='r' || perm.charAt(index)=='R')
				permission+=2;
			else if(perm.charAt(index)=='w' || perm.charAt(index)=='W')
				permission+=4;
			index++;
		}
		FVLog.log(LogLevel.INFO, null,
				"The slice is given the foll. permission: "+permission);
		return permission;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.ARRAY;
	}

	@Override
	public String cmdName() {
		return "add-flowspace";
	}
	
	
	private static String flowspaceAddChangeLogMessage(long dpid, OFMatch match,
			int priority, List<OFAction> actions, String name ){
		return " for dpid=" + FlowSpaceUtil.dpidToString(dpid) + " match=" + match +
		" priority=" + priority + " actions=" + FlowSpaceUtil.toString(actions) + " name=" + name;
	}

}
