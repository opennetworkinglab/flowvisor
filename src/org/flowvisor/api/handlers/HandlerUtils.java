package org.flowvisor.api.handlers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.util.U16;
import org.openflow.util.U8;

public class HandlerUtils {
	
	@SuppressWarnings("unchecked")
	public static <T> T fetchField(String fieldName, Map<String, Object> map, 
			/*Class<T> type,*/ boolean required, T def) 
		throws ClassCastException, MissingRequiredField {
		Object field = map.get(fieldName);
		if (field == null)
			if(required) 
				throw new MissingRequiredField(fieldName);
			else
				return def;
		/*if (field.getClass().isAssignableFrom()) 
			return type.cast(field);*/
		return (T) field;
		//throw new UnknownFieldType(fieldName, type.getName());
		
	}
	
	public static FVMatch matchFromMap(Map<String, Object> map) 
			throws ClassCastException, MissingRequiredField {
		FVMatch match = new FVMatch();
		if (map == null)
			return match;
		int wildcards = FVMatch.OFPFW_ALL;
		Number inport = HandlerUtils.<Number>fetchField(INPORT, map, false, null);
		if (inport != null) {
			match.setInputPort(U16.t(inport.intValue()));
			wildcards &= ~FVMatch.OFPFW_IN_PORT; 
		}
		
		String dl_dst = HandlerUtils.<String>fetchField(DLDST, map, false, null);
		if (dl_dst != null) {
			match.setDataLayerDestination(FlowSpaceUtil.parseMac(dl_dst));
			wildcards &= ~FVMatch.OFPFW_DL_DST; 
		}
		
		String dl_src = HandlerUtils.<String>fetchField(DLSRC, map, false, null);
		if (dl_src != null) {
			match.setDataLayerSource(FlowSpaceUtil.parseMac(dl_src));
			wildcards &= ~FVMatch.OFPFW_DL_SRC; 
		}
		
		String dl_type = HandlerUtils.<String>fetchField(DLTYPE, map, false, null);
		if (dl_type != null) {
			if (dl_type.startsWith("0x"))
				match.setDataLayerType(U16.t(Short
                    .valueOf(dl_type.replaceFirst("0x", ""), 16)));
			else
				match.setDataLayerType(U16.t(Short
                        .valueOf(dl_type)));
			wildcards &= ~FVMatch.OFPFW_DL_TYPE; 
		}
		
		String dl_vlan = HandlerUtils.<String>fetchField(DLVLAN, map, false, null);
		if (dl_vlan != null) {
			if (dl_vlan.startsWith("0x"))
				match.setDataLayerVirtualLan(U16.t(Short
                    .valueOf(dl_vlan.replaceFirst("0x", ""), 16)));
			else
				match.setDataLayerType(U16.t(Short
                        .valueOf(dl_vlan)));
			wildcards &= ~FVMatch.OFPFW_DL_VLAN; 
		}
		
		String dl_vlan_pcp = HandlerUtils.<String>fetchField(DLVLANPCP, map, false, null);
		if (dl_vlan_pcp != null) {
			if (dl_vlan.startsWith("0x"))
				match.setDataLayerVirtualLanPriorityCodePoint(U8.t(Short
                    .valueOf(dl_vlan_pcp.replaceFirst("0x", ""), 16)));
			else
				match.setDataLayerType(U8.t(Short
                        .valueOf(dl_vlan_pcp)));
			wildcards &= ~FVMatch.OFPFW_DL_VLAN_PCP; 
		}
		
		String nw_dst = HandlerUtils.<String>fetchField(NWDST, map, false, null);
		if (nw_dst != null)
			match.setFromCIDR(nw_dst, FVMatch.STR_NW_DST);
		
		String nw_src = HandlerUtils.<String>fetchField(NWSRC, map, false, null);
		if (nw_src != null)
			match.setFromCIDR(nw_src, FVMatch.STR_NW_SRC);
		
		Number nw_proto = HandlerUtils.<Number>fetchField(NWPROTO, map, false, null);
		if (nw_proto != null) {
			match.setNetworkProtocol(U8.t(nw_proto.shortValue()));
			wildcards &= ~FVMatch.OFPFW_NW_PROTO; 
		}
		
		Number nw_tos = HandlerUtils.<Number>fetchField(NWTOS, map, false, null);
		if (nw_tos != null) {
			match.setNetworkTypeOfService(U8.t(nw_tos.shortValue()));
			wildcards &= ~FVMatch.OFPFW_NW_TOS; 
		}
		
		Number tp_src = HandlerUtils.<Number>fetchField(TPSRC, map, false, null);
		if (tp_src != null) {
			match.setTransportSource(U16.t(tp_src.intValue()));
			wildcards &= ~FVMatch.OFPFW_TP_SRC; 
		}
		
		Number tp_dst = HandlerUtils.<Number>fetchField(TPDST, map, false, null);
		if (tp_dst != null) {
			match.setTransportSource(U16.t(tp_dst.intValue()));
			wildcards &= ~FVMatch.OFPFW_TP_DST; 
		}
		
		match.setQueues(HandlerUtils.<List<Integer>>fetchField(QUEUES, map, false, 
				new LinkedList<Integer>()));
		
		Number fqueue = HandlerUtils.<Number>fetchField(FQUEUE, map, false, null);
		if (fqueue != null) 
			match.setForcedQueue(fqueue.intValue()); 
		
		match.setWildcards(wildcards);
		
		return match;
		
	}
	
	private static final String INPORT = "in-port";
	private static final String DLDST = "dl-dst";
	private static final String DLSRC = "dl-src";
	private static final String DLTYPE = "dl-type";
	private static final String DLVLAN = "dl-vlan";
	private static final String DLVLANPCP = "dl-vlan-pcp";
	private static final String NWDST = "nw-dst";
	private static final String NWSRC = "nw-src";
	private static final String NWPROTO = "nw-proto";
	private static final String NWTOS = "nw-tos";
	private static final String TPSRC = "tp-src";
	private static final String TPDST = "tp-dst";
	private static final String QUEUES = "queues";
	private static final String FQUEUE = "force-enqueue";
	
		
	
	
	
}
