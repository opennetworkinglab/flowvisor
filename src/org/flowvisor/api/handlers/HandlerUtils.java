package org.flowvisor.api.handlers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownMatchField;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.resources.SlicerLimits;
import org.flowvisor.slicer.FVSlicer;
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
			throws ClassCastException, MissingRequiredField, UnknownMatchField {
		FVMatch match = new FVMatch();
		if (map == null)
			return match;
		int wildcards = FVMatch.OFPFW_ALL;
		Number inport = HandlerUtils.<Number>fetchField(FVMatch.STR_IN_PORT, map, false, null);
		if (inport != null) {
			match.setInputPort(U16.t(inport.intValue()));
			wildcards &= ~FVMatch.OFPFW_IN_PORT;
			map.remove(FVMatch.STR_IN_PORT);
		}
		
		String dl_dst = HandlerUtils.<String>fetchField(FVMatch.STR_DL_DST, map, false, null);
		if (dl_dst != null) {
			match.setDataLayerDestination(FlowSpaceUtil.parseMac(dl_dst));
			wildcards &= ~FVMatch.OFPFW_DL_DST; 
			map.remove(FVMatch.STR_DL_DST);
		}
		
		String dl_src = HandlerUtils.<String>fetchField(FVMatch.STR_DL_SRC, map, false, null);
		if (dl_src != null) {
			match.setDataLayerSource(FlowSpaceUtil.parseMac(dl_src));
			wildcards &= ~FVMatch.OFPFW_DL_SRC; 
			map.remove(FVMatch.STR_DL_SRC);
		}
		
		Number dl_type = HandlerUtils.<Number>fetchField(FVMatch.STR_DL_TYPE, map, false, null);
		if (dl_type != null) {
			match.setDataLayerType(U16.t(dl_type.intValue()));
			wildcards &= ~FVMatch.OFPFW_DL_TYPE;
			map.remove(FVMatch.STR_DL_TYPE);
		}
		
		Number dl_vlan = HandlerUtils.<Number>fetchField(FVMatch.STR_DL_VLAN, map, false, null);
		if (dl_vlan != null) {
			match.setDataLayerVirtualLan(U16.t(dl_vlan.intValue()));
			wildcards &= ~FVMatch.OFPFW_DL_VLAN;
			map.remove(FVMatch.STR_DL_VLAN);
		}
		
		Number dl_vlan_pcp = HandlerUtils.<Number>fetchField(FVMatch.STR_DL_VLAN_PCP, map, false, null);
		if (dl_vlan_pcp != null) {
			match.setDataLayerVirtualLanPriorityCodePoint(U8.t(dl_vlan_pcp.shortValue()));
			wildcards &= ~FVMatch.OFPFW_DL_VLAN_PCP; 
			map.remove(FVMatch.STR_DL_VLAN_PCP);
		}
		
		String nw_dst = HandlerUtils.<String>fetchField(FVMatch.STR_NW_DST, map, false, null);
		if (nw_dst != null) {
			match.setWildcards(wildcards);
			match.setFromCIDR(nw_dst, FVMatch.STR_NW_DST);
			wildcards = match.getWildcards();
			map.remove(FVMatch.STR_NW_DST);
		}
		
		String nw_src = HandlerUtils.<String>fetchField(FVMatch.STR_NW_SRC, map, false, null);
		if (nw_src != null) {
			match.setWildcards(wildcards);
			match.setFromCIDR(nw_src, FVMatch.STR_NW_SRC);
			wildcards = match.getWildcards();
			map.remove(FVMatch.STR_NW_SRC);
		}
		
		Number nw_proto = HandlerUtils.<Number>fetchField(FVMatch.STR_NW_PROTO, map, false, null);
		if (nw_proto != null) {
			match.setNetworkProtocol(U8.t(nw_proto.shortValue()));
			wildcards &= ~FVMatch.OFPFW_NW_PROTO;
			map.remove(FVMatch.STR_NW_PROTO);
		}
		
		Number nw_tos = HandlerUtils.<Number>fetchField(FVMatch.STR_NW_TOS, map, false, null);
		if (nw_tos != null) {
			match.setNetworkTypeOfService(U8.t(nw_tos.shortValue()));
			wildcards &= ~FVMatch.OFPFW_NW_TOS;
			map.remove(FVMatch.STR_NW_TOS);
		}
		
		Number tp_src = HandlerUtils.<Number>fetchField(FVMatch.STR_TP_SRC, map, false, null);
		if (tp_src != null) {
			match.setTransportSource(U16.t(tp_src.intValue()));
			wildcards &= ~FVMatch.OFPFW_TP_SRC;
			map.remove(FVMatch.STR_TP_SRC);
		}
		
		Number tp_dst = HandlerUtils.<Number>fetchField(FVMatch.STR_TP_DST, map, false, null);
		if (tp_dst != null) {
			match.setTransportDestination(U16.t(tp_dst.intValue()));
			wildcards &= ~FVMatch.OFPFW_TP_DST;
			map.remove(FVMatch.STR_TP_DST);
		}
		
		match.setWildcards(wildcards);
		
		if (!map.isEmpty()) {
			String unknowns = map.keySet().toString();
			throw new UnknownMatchField(unknowns);
		}
		
		return match;
		
	}
	
	public static List<FVClassifier> getAllClassifiers() {
		List<FVClassifier> list = new LinkedList<FVClassifier>();
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				list.add(classifier);
			}
		}
		return list;
	}
	
	public static FVClassifier getClassifierByDPID(Long dpid) throws DPIDNotFound {
		for (FVClassifier classifier : getAllClassifiers()) {
			if (!classifier.isIdentified())
				continue;
			if (dpid == classifier.getDPID())
				return classifier;
		}
		throw new DPIDNotFound(FlowSpaceUtil.dpidToString(dpid));
	}
	
	
	public static FVSlicer getSlicerByName(String sliceName) {
		FVSlicer fvSlicer = null;
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (!classifier.isIdentified()) 
					continue;
				fvSlicer = classifier.getSlicerByName(sliceName);
				if (fvSlicer != null) {
					break;
				}
			}
		}
		return fvSlicer;
	}
	
	public static SlicerLimits getSliceLimits() throws DPIDNotFound{
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				return classifier.getSlicerLimits();
			}
		}
		throw new DPIDNotFound("No switches connected, therefore no limits available");
	}
	
	
	public static List<String> getAllDevices() {
		List<String> dpids = new LinkedList<String>();
		for (FVClassifier classifier : HandlerUtils.getAllClassifiers())
			dpids.add(FlowSpaceUtil.dpidToString(classifier.getDPID()));
		return dpids;
	}
	
	public static List<String> getAllSlices() throws ConfigError {
		return SliceImpl.getProxy().getAllSliceNames();
	}
	
	public static boolean sliceExists(String sliceName) 
			throws ConfigError {
		return getAllSlices().contains(sliceName);
	}
	
	
}
