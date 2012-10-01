package org.flowvisor.config.convertor;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;

import org.flowvisor.config.FlowSpace;
import org.flowvisor.config.Flowvisor;
import org.flowvisor.config.Slice;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.util.HexString;

import com.google.gson.stream.JsonWriter;



public class ConfigJson implements ConfigIterator {

	JsonWriter out = null;
	private HashMap<String, HashMap<String, Object>> lines = new HashMap<String, HashMap<String, Object>>();
	private HashMap<String, HashMap<String, Object>> slice = new HashMap<String, HashMap<String, Object>>();
	private LinkedList<HashMap<String, Object>> frules = new LinkedList<HashMap<String,Object>>();
	private HashMap<String, HashMap<String, Object>> floodperms = new HashMap<String, HashMap<String, Object>>();
	
	public ConfigJson(JsonWriter out) {
		this.out = out;
	}
	
	@Override
	public void visit(String path, ConfigEntry entry)  {
		String[] pathlets = path.split("!");
		ConfigType type = entry.type;
		String[] values = entry.getValue();
		
		
		if (pathlets[1].equalsIgnoreCase(("flowvisor"))) {
			for (int i = 0 ; i < values.length ; i++) { 
				addtoLines(pathlets[1], pathlets[2] , getType(type, values[i]));
			}	
		} else if (pathlets[1].equalsIgnoreCase("slices")) {
			addtoSlice(pathlets[2], Slice.FMTYPE, "federated");
			addtoSlice(pathlets[2], Flowvisor.CONFIG, "default");
			addtoSlice(pathlets[2], Slice.SLICE, pathlets[2]);
			for (int i = 0 ; i < values.length ; i++) { 
				addtoSlice(pathlets[2], pathlets[3], getType(type, values[i]));
			}
		} else if (pathlets[1].equalsIgnoreCase("flowspace")) {
			SortedSet<FlowEntry> rules = ((ConfFlowMapEntry) entry).flowMap.getRules();
			for (FlowEntry rule : rules) {
				HashMap<String, Object> r = new HashMap<String, Object>();
				int wildcards = rule.getRuleMatch().getWildcards();
				r.put(FlowSpace.WILDCARDS, wildcards);
				r.put(FlowSpace.DPID, Long.toString(rule.getDpid(),16));
				r.put(FlowSpace.PRIO, rule.getPriority());
				r.put(FlowSpace.INPORT, rule.getRuleMatch().getInputPort());
				if ((wildcards & FVMatch.OFPFW_DL_VLAN) == 0)
					r.put(FlowSpace.VLAN, rule.getRuleMatch().getDataLayerVirtualLan());
				
				if ((wildcards & FVMatch.OFPFW_DL_VLAN_PCP) == 0)
					r.put(FlowSpace.VPCP, rule.getRuleMatch().getDataLayerVirtualLanPriorityCodePoint());
				
				if ((wildcards & FVMatch.OFPFW_DL_SRC) == 0)
					r.put(FlowSpace.DLSRC, HexString.toHexString(rule.getRuleMatch().getDataLayerSource()));
				
				if ((wildcards & FVMatch.OFPFW_DL_DST) == 0)
					r.put(FlowSpace.DLDST, HexString.toHexString(rule.getRuleMatch().getDataLayerDestination()));
				
				if ((wildcards & FVMatch.OFPFW_DL_TYPE) == 0)
					r.put(FlowSpace.DLTYPE, rule.getRuleMatch().getDataLayerType());
				
				if ((wildcards & FVMatch.OFPFW_NW_SRC_ALL) == 0)
					r.put(FlowSpace.NWSRC, rule.getRuleMatch().getNetworkSource());
				
				if ((wildcards & FVMatch.OFPFW_NW_DST_ALL) == 0)
					r.put(FlowSpace.NWDST, rule.getRuleMatch().getNetworkDestination());
				
				if ((wildcards & FVMatch.OFPFW_NW_PROTO) == 0)
					r.put(FlowSpace.NWPROTO, rule.getRuleMatch().getNetworkProtocol());
				
				if ((wildcards & FVMatch.OFPFW_NW_TOS) == 0)
					r.put(FlowSpace.NWTOS, rule.getRuleMatch().getNetworkTypeOfService());
				
				if ((wildcards & FVMatch.OFPFW_TP_SRC) == 0)
					r.put(FlowSpace.TPSRC, rule.getRuleMatch().getTransportSource());
				
				if ((wildcards & FVMatch.OFPFW_TP_DST) == 0)
					r.put(FlowSpace.TPDST, rule.getRuleMatch().getTransportDestination());
				
				r.put(FlowSpace.ACTION, rule.getActionsList());
				frules.add(r);
			}
		} else if (pathlets[1].equalsIgnoreCase("switches")) {
			for (int i = 0 ; i < values.length ; i++) { 
				addtoPerms(pathlets[1], pathlets[2] , getType(type, values[i]));
			}	
		}
		
	}
	
	private Object getType(ConfigType type, String string) {
		if (type == ConfigType.BOOL)
			return new Boolean(string);
		else if (type == ConfigType.INT)
			return new Integer(string);
		else if (type == ConfigType.REAL)
			return new Long(string);
		else if (type == ConfigType.STR)
			return string;
		else
			return null;
	}

	private void addtoSlice(String sliceName, String field, Object value) {
		HashMap<String, Object> entry = slice.get(sliceName);
		if (entry == null) {
			entry = new HashMap<String, Object>();
		}
		entry.put(field, value);
		slice.put(sliceName, entry);
		
	}
	
	private void addtoLines(String table, String field, Object value) {
		HashMap<String, Object> entry = lines.get(table);
		if (entry == null) {
			entry = new HashMap<String, Object>();
		}
		entry.put(field, value);
		lines.put(table, entry);
		
	}
	
	private void addtoPerms(String table, String field, Object value) {
		HashMap<String, Object> entry = floodperms.get(table);
		if (entry == null) {
			entry = new HashMap<String, Object>();
		}
		entry.put(field, value);
		floodperms.put(table, entry);
		
	}

	@SuppressWarnings("unchecked")
	public void write() {
		try {
			
			for (Entry<String, HashMap<String, Object>> entry : lines.entrySet()) {
				out.name(entry.getKey());
				out.beginArray();
				out.beginObject();
				for (Entry<String, Object> val : entry.getValue().entrySet()) {
					Object tmp = val.getValue();
					if (tmp instanceof Boolean)
						out.name(val.getKey()).value((Boolean) tmp);
					else if (tmp instanceof Number)
						out.name(val.getKey()).value((Number) tmp);
					else if (tmp instanceof String)
						out.name(val.getKey()).value(tmp.toString());
				}
				out.endObject();
				out.endArray();
			}
			out.name("Slice");
			out.beginArray();
			for (Entry<String, HashMap<String, Object>> entry : slice.entrySet()) {
				out.beginObject();
				for (Entry<String, Object> val : entry.getValue().entrySet()) {
					Object tmp = val.getValue();
					if (tmp instanceof Boolean)
						out.name(val.getKey()).value((Boolean) tmp);
					else if (tmp instanceof Number)
						out.name(val.getKey()).value((Number) tmp);
					else if (tmp instanceof String) {
						if (val.getKey().equals(Slice.SLICE) && tmp.equals("root"))
							out.name(val.getKey()).value("fvadmin");
						else
							out.name(val.getKey()).value(tmp.toString());
					}
				}
				out.endObject();
			}
			out.endArray();
			
			for (Entry<String, HashMap<String, Object>> entry : floodperms.entrySet()) {
				out.name(entry.getKey());
				out.beginArray();
				out.beginObject();
				for (Entry<String, Object> val : entry.getValue().entrySet()) {
					Object tmp = val.getValue();
					if (tmp instanceof Boolean)
						out.name(val.getKey()).value((Boolean) tmp);
					else if (tmp instanceof Number)
						out.name(val.getKey()).value((Number) tmp);
					else if (tmp instanceof String)
						out.name(val.getKey()).value(tmp.toString());
				}
				out.endObject();
				out.endArray();
			}
			
			out.name("FlowSpaceRule");
			out.beginArray();
			for (HashMap<String, Object> rules : frules) {
				out.beginObject();
				for (Entry<String, Object> r : rules.entrySet()) {
					if (r.getKey().equals(FlowSpace.ACTION)) {
						List<SliceAction> actions = (List<SliceAction>) r.getValue();
						out.name(FlowSpace.ACTION);
						out.beginArray();
						for (SliceAction action : actions) {
							out.beginObject();
							out.name(action.getSliceName()).value(action.getSlicePerms());
							out.endObject();
						}
						out.endArray();
					} else {
						Object tmp = r.getValue();
						if (tmp instanceof Number)
							out.name(r.getKey()).value((Number) tmp);
						else
							out.name(r.getKey()).value(tmp.toString());
					}
				}
				out.endObject();
			}
			out.endArray();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	

}
