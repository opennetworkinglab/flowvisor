package org.flowvisor.api.handlers.configuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.openflow.protocol.FVMatch;


import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListFlowSpace implements ApiHandler<Map<String, Object>> {

	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, false, null);
			String user = APIUserCred.getUserName();
			HashMap<String, Object> map = new HashMap<String, Object>();
			if (FVConfig.isSupervisor(user)) {
				if (sliceName == null) 
					FlowSpaceImpl.getProxy().toJson(map, null);
				else
					FlowSpaceImpl.getProxy().toJson(map, sliceName);
				
			} else
				FlowSpaceImpl.getProxy().toJson(map, user);
			for (HashMap<String, Object> m : (LinkedList<HashMap<String, Object>>) map.get(FlowSpace.FS))
				rewriteFields(m);
			resp = new JSONRPC2Response(map.get(FlowSpace.FS), 0);
		}  catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": Unable to get flowspace : " + e.getMessage()), 0);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return resp;
		
	}

	/*
	 * THIS REALLY BLOWS!!!
	 * TODO: rework JSON output to be identical in config file and over rpc.
	 */
	@SuppressWarnings("unchecked")
	private void rewriteFields(HashMap<String, Object> map) {
		Object list = map.remove("queue_id");
		map.put(FVMatch.STR_QUEUE, list);
		LinkedList<HashMap<String, Integer>> actions = 
				(LinkedList<HashMap<String, Integer>>) map.remove(FlowSpace.ACTION);
		LinkedList<HashMap<String, Object>> neoacts = new LinkedList<HashMap<String,Object>>();
		HashMap<String, Object> neoact = new HashMap<String, Object>();
		for (HashMap<String, Integer> m : actions) {
			for (Entry<String, Integer> entry : m.entrySet()) {
				neoact.put(SLICENAME, entry.getKey());
				neoact.put(PERM, entry.getValue());
				neoacts.add((HashMap<String, Object>) neoact.clone());
				break;
			}
			neoact.clear();
		}
		map.put(FlowSpace.ACTION, neoacts);
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "list-flowspace";
	}

}
