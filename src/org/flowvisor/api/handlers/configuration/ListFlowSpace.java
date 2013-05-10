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
		LinkedList<Map<String, Object>> list = new LinkedList<Map<String,Object>>();
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, false, null);
			Boolean show = HandlerUtils.<Boolean>fetchField(SHOW, params, false, false);
			String user = APIUserCred.getUserName();
			HashMap<String, Object> map = new HashMap<String, Object>();
			if (FVConfig.isSupervisor(user)) {
				if (sliceName == null) 
					FlowSpaceImpl.getProxy().toJson(map, null, show);
				else
					FlowSpaceImpl.getProxy().toJson(map, sliceName, true);
				
			} else
				FlowSpaceImpl.getProxy().toJson(map, user, true);
			for (HashMap<String, Object> m : (LinkedList<HashMap<String, Object>>) map.get(FlowSpace.FS))
				list.add(rewriteFields(m));
			resp = new JSONRPC2Response(list, 0);
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
	 * 
	 * Ideally, make the objects that are serialized here, serialize themseleves.
	 */
	@SuppressWarnings("unchecked")
	private HashMap<String, Object> rewriteFields(HashMap<String, Object> map) {
		HashMap<String, Object> ret = new HashMap<String, Object>();
		ret.put(FSNAME, map.remove(FSNAME));
		Object list = map.remove(FlowSpace.QUEUE);
		ret.put(QUEUE, list);
		ret.put(FQUEUE, map.remove(FlowSpace.FORCED_QUEUE));
	    ret.put(FlowSpace.DPID, map.remove(FlowSpace.DPID));
	    ret.put(FlowSpace.PRIO, map.remove(FlowSpace.PRIO));
	    ret.put("id", map.remove("id"));
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
		ret.put(SLICEACTIONS, neoacts);
	    
	 
	    ret.put(MATCH, humanify(map));
	    return ret;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "list-flowspace";
	}
	
	private HashMap<String, Object> humanify(HashMap<String, Object> map) {
		if (map == null)
			return map;
		Integer ip_src = (Integer) map.get(FVMatch.STR_NW_SRC);
		Integer ip_dst = (Integer) map.get(FVMatch.STR_NW_DST);
		Integer wildcards = (Integer) map.get(FlowSpace.WILDCARDS);
		
		if (ip_src != null) {
			int ip_src_mask = Math.max(32 - ((wildcards & FVMatch.OFPFW_NW_SRC_MASK) >> FVMatch.OFPFW_NW_SRC_SHIFT),0);
			map.put(FVMatch.STR_NW_SRC, cidrToString(ip_src, ip_src_mask));
		}
		if (ip_dst != null) {
			int ip_dst_mask = Math.max(32 - ((wildcards & FVMatch.OFPFW_NW_DST_MASK) >> FVMatch.OFPFW_NW_DST_SHIFT),0);	
			map.put(FVMatch.STR_NW_DST, cidrToString(ip_dst, ip_dst_mask));
		}
		
		return map;
	}
	
	
	private String cidrToString(int ip, int prefix) {
        String str;
        if (prefix >= 32) {
            str = FVMatch.ipToString(ip);
        } else {
            // use the negation of mask to fake endian magic
            int mask = ~((1 << (32 - prefix)) - 1);
            str = FVMatch.ipToString(ip & mask) + "/" + prefix;
        }

        return str;
    }
	
	

}
