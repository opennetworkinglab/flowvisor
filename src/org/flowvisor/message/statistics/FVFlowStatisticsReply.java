package org.flowvisor.message.statistics;

import java.util.HashMap;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.XidPairWithMessage;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.openflow.protocol.FVMatch.cidrToIp;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionEnqueue;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionNetworkTypeOfService;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionTransportLayerDestination;
import org.openflow.protocol.action.OFActionTransportLayerSource;
import org.openflow.protocol.action.OFActionVendor;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
import org.openflow.protocol.action.OFActionVirtualLanPriorityCodePoint;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.openflow.util.U16;
import org.openflow.util.U8;

public class FVFlowStatisticsReply extends OFFlowStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {

	/*
	 * Stupid hack to return the correct number of 
	 * flows for an agg stats reply.
	 * 
	 * There may be a better way to do this... but meh.
	 */
	private long trans_cookie;
	
	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		FVLog.log(LogLevel.DEBUG, null, "Inside classifyFromSwitch in FVFlowStatisticsReply");
		//Make a map structure out of the FVStatisticsReply msg	
		HashMap <String, Object> statsMap = new HashMap<String, Object>();
		statsMap = toMap(msg, fvClassifier);
		
		fvClassifier.classifyFlowStats(msg,statsMap);
		XidPairWithMessage pair = FVMessageUtil
				.untranslateXidMsg(msg, fvClassifier);
		if (pair == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable stats reply: ", this);
			return;
		}
		FVStatisticsRequest original = (FVStatisticsRequest) pair.getOFMessage();
		if (original.getStatisticType() == OFStatisticsType.FLOW)
			fvClassifier.sendFlowStatsResp(pair.getSlicer(), original);
		else if (original.getStatisticType() == OFStatisticsType.AGGREGATE)
			fvClassifier.sendAggStatsResp(pair.getSlicer(), original);
	}



	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + this);
		
	}
	
	public long getTransCookie() {
		return this.trans_cookie;
	}
	
	public void setTransCookie(long cookie) {
		this.trans_cookie = cookie;
	}
	
	public HashMap<String,Object> toMap(FVStatisticsReply msg, FVClassifier fvc){
		List<OFStatistics> stats = msg.getStatistics();
		HashMap <String,Object> cache = new HashMap<String,Object>();
		for (int i=0; i<stats.size(); i++){
			OFFlowStatisticsReply reply = (OFFlowStatisticsReply) stats.get(i);

			cache.put(FlowSpace.PRIO, String.valueOf(reply.getPriority()));
		
			//Put all the individual action fields into the cache
			//cache.put(FlowSpace.ACTION, reply.getActions().toString());
			for (OFAction act : reply.getActions()) {
				switch (act.getType()) {
				case OUTPUT:
					OFActionOutput out = (OFActionOutput) act;
					cache.put("OFPAT_OUTPUT type", out.getType().toString());
					cache.put("OFPAT_OUTPUT len", U16.f(out.getLength()));
					cache.put("OFPAT_OUTPUT port", U16.f(out.getPort()));
					cache.put("OFPAT_OUTPUT max_len", U16.f(out.getMaxLength()));
					break;
		
				case OPAQUE_ENQUEUE:
					OFActionEnqueue enq = (OFActionEnqueue) act;
					cache.put("OFPAT_ENQUEUE type", enq.getType().toString());
					cache.put("OFPAT_ENQUEUE len", U16.f(enq.getLength()));
					cache.put("OFPAT_ENQUEUE port", U16.f(enq.getPort()));
					cache.put("OFPAT_ENQUEUE queue_id", enq.getQueueId());
					break;
		
				case SET_VLAN_VID:
					OFActionVirtualLanIdentifier vid = (OFActionVirtualLanIdentifier)act;
					cache.put("OFPAT_SET_VLAN_VID type", vid.getType().toString());
					cache.put("OFPAT_SET_VLAN_VID len", U16.f(vid.getLength()));	
					cache.put("OFPAT_SET_VLAN_VID vlan_vid", U16.f(vid.getVirtualLanIdentifier()));
					break;
			
				case SET_VLAN_PCP:
					OFActionVirtualLanPriorityCodePoint vpcp = (OFActionVirtualLanPriorityCodePoint) act;
					cache.put("OFPAT_SET_VLAN_PCP type", vpcp.getType().toString());
					cache.put("OFPAT_SET_VLAN_PCP len", U16.f(vpcp.getLength()));
					cache.put("OFPAT_SET_VLAN_PCP vlan_pcp", U8.f(vpcp.getVirtualLanPriorityCodePoint()));
					break;
		
				case STRIP_VLAN:	
					OFActionStripVirtualLan svlan = (OFActionStripVirtualLan) act;
					cache.put("OFPAT_STRIP_VLAN type", svlan.getType().toString());
					break;
		
				case SET_DL_DST:
					OFActionDataLayerDestination dl_dst = (OFActionDataLayerDestination) act;
					cache.put("OFPAT_SET_DL_DST type",dl_dst.getType().toString());
					cache.put("OFPAT_SET_DL_DST len",U16.f(dl_dst.getLength()));
					cache.put("OFPAT_SET_DL_DST dl_addr", HexString.toHexString(dl_dst.getDataLayerAddress()));
					break;
			
				case SET_DL_SRC:
					OFActionDataLayerSource dl_src = (OFActionDataLayerSource) act;
					cache.put("OFPAT_SET_DL_SRC type",dl_src.getType().toString());
					cache.put("OFPAT_SET_DL_SRC len",U16.f(dl_src.getLength()));
					cache.put("OFPAT_SET_DL_SRC dl_addr", HexString.toHexString(dl_src.getDataLayerAddress()));
					break;
		
				case SET_NW_DST:
					OFActionNetworkLayerDestination nw_dst = (OFActionNetworkLayerDestination) act;
					cache.put("OFPAT_SET_NW_DST type", nw_dst.getType().toString());
					cache.put("OFPAT_SET_NW_DST len", U16.f(nw_dst.getLength()));
					cache.put("OFPAT_SET_NW_DST	nw_addr", FlowSpaceUtil.intToIp(nw_dst.getNetworkAddress()));	
					break;

				case SET_NW_SRC:
					OFActionNetworkLayerSource nw_src = (OFActionNetworkLayerSource) act;
					cache.put("OFPAT_SET_NW_SRC type", nw_src.getType().toString());
					cache.put("OFPAT_SET_NW_SRC len", U16.f(nw_src.getLength()));
					cache.put("OFPAT_SET_NW_SRC	nw_addr", FlowSpaceUtil.intToIp(nw_src.getNetworkAddress()));	
					break;
			
				case SET_NW_TOS:
					OFActionNetworkTypeOfService nw_tos = (OFActionNetworkTypeOfService) act;
					cache.put("OFPAT_SET_NW_TOS type", nw_tos.getType().toString());
					cache.put("OFPAT_SET_NW_TOS len", U16.f(nw_tos.getLength()));
					cache.put("OFPAT_SET_NW_TOS nw_tos", U8.f(nw_tos.getNetworkTypeOfService()));
					break;
			
				case SET_TP_DST:
					OFActionTransportLayerDestination tp_dst = (OFActionTransportLayerDestination) act;
					cache.put("OFPAT_SET_TP_DST type", tp_dst.getType().toString());
					cache.put("OFPAT_SET_TP_DST len", U16.f(tp_dst.getLength()));		
					cache.put("OFPAT_SET_TP_DST tp_port", U16.f(tp_dst.getTransportPort()));
					break;
			
				case SET_TP_SRC:
					OFActionTransportLayerSource tp_src = (OFActionTransportLayerSource) act;
					cache.put("OFPAT_SET_TP_SRC type", tp_src.getType().toString());
					cache.put("OFPAT_SET_TP_SRC len", U16.f(tp_src.getLength()));		
					cache.put("OFPAT_SET_TP_SRC tp_port", U16.f(tp_src.getTransportPort()));
					break;		
			
				case VENDOR:
					OFActionVendor ven = (OFActionVendor) act;
					cache.put("OFPAT_VENDOR type", ven.getType().toString());
					cache.put("OFPAT_VENDOR len", U16.f(ven.getLength()));
					cache.put("OFPAT_VENDOR vendor", ven.getVendor());
					break;
				
				default:
					//Error
					FVLog.log(LogLevel.ALERT, null, "Shouldn't have come here- No default ActionType ");
					break;
				}
			}
		
			//Put all the individual match fields into the cache
			//cache.put("OFMatch", reply.getMatch().toString());
			int wildcards = reply.getMatch().getWildcards();
			if((wildcards & OFMatch.OFPFW_IN_PORT) == 0)
				cache.put(FlowSpace.INPORT, U16.f(reply.getMatch().getInputPort()));
			if((wildcards & OFMatch.OFPFW_DL_DST) == 0)
				cache.put(FlowSpace.DLDST, HexString.toHexString(reply.getMatch().getDataLayerDestination()));
			if((wildcards & OFMatch.OFPFW_DL_SRC) == 0)
				cache.put(FlowSpace.DLSRC, HexString.toHexString(reply.getMatch().getDataLayerSource()));
			if((wildcards & OFMatch.OFPFW_DL_TYPE) == 0)
				cache.put(FlowSpace.DLTYPE, Integer.toHexString(U16.f(reply.getMatch().getDataLayerType())));
			if((wildcards & OFMatch.OFPFW_DL_VLAN) == 0)
				cache.put(FlowSpace.VLAN, U16.f(reply.getMatch().getDataLayerVirtualLan()));
			if((wildcards & OFMatch.OFPFW_DL_VLAN_PCP) == 0)
				cache.put(FlowSpace.VPCP, U8.f(reply.getMatch().getDataLayerVirtualLanPriorityCodePoint()));
			if(reply.getMatch().getNetworkDestinationMaskLen() > 0)
				cache.put(FlowSpace.NWDST,  cidrToIp.cidrToString(reply.getMatch().getNetworkDestination(),reply.getMatch().getNetworkDestinationMaskLen()));
			if(reply.getMatch().getNetworkSourceMaskLen() > 0)
				cache.put(FlowSpace.NWSRC,  cidrToIp.cidrToString(reply.getMatch().getNetworkSource(),reply.getMatch().getNetworkSourceMaskLen()));
			if((wildcards & OFMatch.OFPFW_NW_PROTO) == 0)
				cache.put(FlowSpace.NWPROTO, reply.getMatch().getNetworkProtocol());
			if((wildcards & OFMatch.OFPFW_NW_TOS) == 0)
				cache.put(FlowSpace.NWTOS,reply.getMatch().getNetworkTypeOfService());
			if((wildcards & OFMatch.OFPFW_TP_DST) == 0)
				cache.put(FlowSpace.TPDST,reply.getMatch().getTransportDestination());
			if((wildcards & OFMatch.OFPFW_TP_SRC) == 0)
				cache.put(FlowSpace.TPSRC,reply.getMatch().getTransportSource());
		
			cache.put(FlowSpace.DPID, fvc.getDPID());
			cache.put("tableId ", HexString.toHexString(reply.getTableId()));
			cache.put("nanoSecondDuration ", reply.getDurationNanoseconds());
			cache.put("durationInSeconds ",reply.getDurationSeconds());
			cache.put("hardTimeOut ",reply.getHardTimeout());
			cache.put("idleTimeOut ", reply.getIdleTimeout());
			cache.put("cookie ",reply.getCookie());
			cache.put("packetCount ", reply.getPacketCount());
			cache.put("byteCount ",reply.getByteCount());
			cache.put("length ", reply.getLength());

		}
		return cache;
	}

}
