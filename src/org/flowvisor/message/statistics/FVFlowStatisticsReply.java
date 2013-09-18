package org.flowvisor.message.statistics;

import java.util.ArrayList;
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

import org.openflow.protocol.OFStatisticsReply.OFStatisticsReplyFlags;

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
		
		//fvClassifier.classifyFlowStats(msg,statsMap);
		fvClassifier.classifyFlowStats(msg);
		XidPairWithMessage pair = FVMessageUtil
				.untranslateXidMsg(msg, fvClassifier);
		if (pair == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable stats reply: ", this);
			return;
		}
		FVStatisticsRequest original = (FVStatisticsRequest) pair.getOFMessage();
		
		if(msg.getFlags() != OFStatisticsReplyFlags.REPLY_MORE.getTypeValue()){
			if (original.getStatisticType() == OFStatisticsType.FLOW)
				fvClassifier.sendFlowStatsResp(pair.getSlicer(), original, msg.getFlags());
				//fvClassifier.sendFlowStatsResp(pair.getSlicer(), original);
			else if (original.getStatisticType() == OFStatisticsType.AGGREGATE)
				fvClassifier.sendAggStatsResp(pair.getSlicer(), original);
		}	
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
	
	public static HashMap<String,Object> toMap(FVStatisticsReply msg, long dpid){
		List<OFStatistics> stats = msg.getStatistics();
		HashMap <String,Object> cache = new HashMap<String,Object>();
		for(OFStatistics stat :stats){
			OFFlowStatisticsReply reply = (OFFlowStatisticsReply) stat;
			
			//Put all the individual action fields into the cache
			//cache.put(FlowSpace.ACTION, reply.getActions().toString());
			List<HashMap <String, Object>> actionList = new ArrayList <HashMap <String, Object>>();
			for (OFAction act : reply.getActions()) {
				//HashMap <String,Object> actionMap = new HashMap<String,Object>();
				//Set<HashMap <String, Object>> actionSet = new HashSet <HashMap <String, Object>>();
				
				switch (act.getType()) {
				case OUTPUT:
					HashMap <String,Object> outMap = new HashMap<String,Object>();
					HashMap <String,Object> outputMap = new HashMap<String,Object>();
					OFActionOutput out = (OFActionOutput) act;
					outputMap.put("type", out.getType().toString());
					outputMap.put("len", U16.f(out.getLength()));
					outputMap.put("port", U16.f(out.getPort()));
					outputMap.put("max_len", U16.f(out.getMaxLength()));
					
					outMap.put("ofp_action_output", outputMap);
					actionList.add(outMap);
					break;
		
				case OPAQUE_ENQUEUE:
					HashMap <String,Object> enqMap = new HashMap<String,Object>();
					HashMap <String,Object> enqueueMap = new HashMap<String,Object>();
					OFActionEnqueue enq = (OFActionEnqueue) act;
					enqueueMap.put("type", enq.getType().toString());
					enqueueMap.put("len", U16.f(enq.getLength()));
					enqueueMap.put("port", U16.f(enq.getPort()));
					enqueueMap.put("queue_id", enq.getQueueId());
					
					enqMap.put("ofp_action_enqueue", enqueueMap);
					actionList.add(enqMap);
					break;
		
				case SET_VLAN_VID:
					HashMap <String,Object> vidMap = new HashMap<String,Object>();
					HashMap <String,Object> vlanVidMap = new HashMap<String,Object>();
					OFActionVirtualLanIdentifier vid = (OFActionVirtualLanIdentifier)act;
					vlanVidMap.put("type", vid.getType().toString());
					vlanVidMap.put("len", U16.f(vid.getLength()));	
					vlanVidMap.put("vlan_vid", U16.f(vid.getVirtualLanIdentifier()));
					
					vidMap.put("ofp_action_vlan_vid", vlanVidMap);
					actionList.add(vidMap);					
					break;
			
				case SET_VLAN_PCP:
					HashMap <String,Object> vpcpMap = new HashMap<String,Object>();
					HashMap <String,Object> vlanPcpMap = new HashMap<String,Object>();
					OFActionVirtualLanPriorityCodePoint vpcp = (OFActionVirtualLanPriorityCodePoint) act;
					vlanPcpMap.put("type", vpcp.getType().toString());
					vlanPcpMap.put("len", U16.f(vpcp.getLength()));
					vlanPcpMap.put("vlan_pcp", U8.f(vpcp.getVirtualLanPriorityCodePoint()));
					
					vpcpMap.put("ofp_action_vlan_pcp", vlanPcpMap);
					actionList.add(vpcpMap);
					break;
		
				case STRIP_VLAN:	
					HashMap <String,Object> svMap = new HashMap<String,Object>();
					HashMap <String,Object> stripVlanMap = new HashMap<String,Object>();
					OFActionStripVirtualLan svlan = (OFActionStripVirtualLan) act;
					stripVlanMap.put("type", svlan.getType().toString());
					
					svMap.put("action_strip_vlan", stripVlanMap);
					actionList.add(svMap);
					break;
		
				case SET_DL_DST:
					HashMap <String,Object> dlDMap = new HashMap<String,Object>();
					HashMap <String,Object> dlDstMap = new HashMap<String,Object>();
					OFActionDataLayerDestination dl_dst = (OFActionDataLayerDestination) act;
					dlDstMap.put("type",dl_dst.getType().toString());
					dlDstMap.put("len",U16.f(dl_dst.getLength()));
					dlDstMap.put("dl_addr", HexString.toHexString(dl_dst.getDataLayerAddress()));
					
					dlDMap.put("ofp_action_dl_addr", dlDstMap);
					actionList.add(dlDMap);
					break;
			
				case SET_DL_SRC:
					HashMap <String,Object> dlSMap = new HashMap<String,Object>();
					HashMap <String,Object> dlSrcMap = new HashMap<String,Object>();
					OFActionDataLayerSource dl_src = (OFActionDataLayerSource) act;
					dlSrcMap.put("type",dl_src.getType().toString());
					dlSrcMap.put("len",U16.f(dl_src.getLength()));
					dlSrcMap.put("dl_addr", HexString.toHexString(dl_src.getDataLayerAddress()));
					
					dlSMap.put("ofp_action_dl_addr", dlSrcMap);
					actionList.add(dlSMap);
					break;
		
				case SET_NW_DST:
					HashMap <String,Object> nwDMap = new HashMap<String,Object>();
					HashMap <String,Object> nwDstMap = new HashMap<String,Object>();
					OFActionNetworkLayerDestination nw_dst = (OFActionNetworkLayerDestination) act;
					nwDstMap.put("type", nw_dst.getType().toString());
					nwDstMap.put("len", U16.f(nw_dst.getLength()));
					nwDstMap.put("nw_addr", FlowSpaceUtil.intToIp(nw_dst.getNetworkAddress()));	
					
					nwDMap.put("ofp_action_nw_addr", nwDstMap);
					actionList.add(nwDMap);
					break;

				case SET_NW_SRC:
					HashMap <String,Object> nwSMap = new HashMap<String,Object>();
					HashMap <String,Object> nwSrcMap = new HashMap<String,Object>();
					OFActionNetworkLayerSource nw_src = (OFActionNetworkLayerSource) act;
					nwSrcMap.put("type", nw_src.getType().toString());
					nwSrcMap.put("len", U16.f(nw_src.getLength()));
					nwSrcMap.put("nw_addr", FlowSpaceUtil.intToIp(nw_src.getNetworkAddress()));
					
					nwSMap.put("ofp_action_nw_addr", nwSrcMap);
					actionList.add(nwSMap);
					break;
			
				case SET_NW_TOS:
					HashMap <String,Object> nwTMap = new HashMap<String,Object>();
					HashMap <String,Object> nwTosMap = new HashMap<String,Object>();
					OFActionNetworkTypeOfService nw_tos = (OFActionNetworkTypeOfService) act;
					nwTosMap.put("type", nw_tos.getType().toString());
					nwTosMap.put("len", U16.f(nw_tos.getLength()));
					nwTosMap.put("nw_tos", U8.f(nw_tos.getNetworkTypeOfService()));
					
					nwTMap.put("ofp_action_nw_tos", nwTosMap);
					actionList.add(nwTMap);
					break;
			
				case SET_TP_DST:
					HashMap <String,Object> tpDMap = new HashMap<String,Object>();
					HashMap <String,Object> tpDstMap = new HashMap<String,Object>();
					OFActionTransportLayerDestination tp_dst = (OFActionTransportLayerDestination) act;
					tpDstMap.put("type", tp_dst.getType().toString());
					tpDstMap.put("len", U16.f(tp_dst.getLength()));		
					tpDstMap.put("tp_port", U16.f(tp_dst.getTransportPort()));
					
					tpDMap.put("ofp_action_tp_port", tpDstMap);
					actionList.add(tpDMap);
					break;
			
				case SET_TP_SRC:
					HashMap <String,Object> tpSMap = new HashMap<String,Object>();
					HashMap <String,Object> tpSrcMap = new HashMap<String,Object>();
					OFActionTransportLayerSource tp_src = (OFActionTransportLayerSource) act;
					tpSrcMap.put("type", tp_src.getType().toString());
					tpSrcMap.put("len", U16.f(tp_src.getLength()));		
					tpSrcMap.put("tp_port", U16.f(tp_src.getTransportPort()));
					
					tpSMap.put("ofp_action_tp_port", tpSrcMap);
					actionList.add(tpSMap);
					break;		
			
				case VENDOR:
					HashMap <String,Object> vMap = new HashMap<String,Object>();
					HashMap <String,Object> vendorMap = new HashMap<String,Object>();
					OFActionVendor ven = (OFActionVendor) act;
					vendorMap.put("type", ven.getType().toString());
					vendorMap.put("len", U16.f(ven.getLength()));
					vendorMap.put("vendor", ven.getVendor());
					
					vMap.put("ofp_action_vendor_header", vendorMap);
					actionList.add(vMap);
					break;
				
				default:
					//Error
					FVLog.log(LogLevel.ALERT, null, "Shouldn't have come here- No default ActionType ");
					break;
				}
			}
			cache.put("ofp_action_header actions",actionList);
			
		
			//Put all the individual match fields into the cache
			//cache.put("OFMatch", reply.getMatch().toString());
			HashMap <String,Object> matchMap = new HashMap<String,Object>();
			int wildcards = reply.getMatch().getWildcards();
			matchMap.put(FlowSpace.WILDCARDS, wildcards);
			if((wildcards & OFMatch.OFPFW_IN_PORT) == 0)
				matchMap.put(FlowSpace.INPORT, U16.f(reply.getMatch().getInputPort()));
			if((wildcards & OFMatch.OFPFW_DL_DST) == 0)
				matchMap.put(FlowSpace.DLDST, HexString.toHexString(reply.getMatch().getDataLayerDestination()));
			if((wildcards & OFMatch.OFPFW_DL_SRC) == 0)
				matchMap.put(FlowSpace.DLSRC, HexString.toHexString(reply.getMatch().getDataLayerSource()));
			if((wildcards & OFMatch.OFPFW_DL_TYPE) == 0)
				matchMap.put(FlowSpace.DLTYPE, Integer.toHexString(U16.f(reply.getMatch().getDataLayerType())));
			if((wildcards & OFMatch.OFPFW_DL_VLAN) == 0)
				matchMap.put(FlowSpace.VLAN, U16.f(reply.getMatch().getDataLayerVirtualLan()));
			if((wildcards & OFMatch.OFPFW_DL_VLAN_PCP) == 0)
				matchMap.put(FlowSpace.VPCP, U8.f(reply.getMatch().getDataLayerVirtualLanPriorityCodePoint()));
			if(reply.getMatch().getNetworkDestinationMaskLen() > 0)
				matchMap.put(FlowSpace.NWDST,  cidrToIp.cidrToString(reply.getMatch().getNetworkDestination(),reply.getMatch().getNetworkDestinationMaskLen()));
			if(reply.getMatch().getNetworkSourceMaskLen() > 0)
				matchMap.put(FlowSpace.NWSRC,  cidrToIp.cidrToString(reply.getMatch().getNetworkSource(),reply.getMatch().getNetworkSourceMaskLen()));
			if((wildcards & OFMatch.OFPFW_NW_PROTO) == 0)
				matchMap.put(FlowSpace.NWPROTO, reply.getMatch().getNetworkProtocol());
			if((wildcards & OFMatch.OFPFW_NW_TOS) == 0)
				matchMap.put(FlowSpace.NWTOS,reply.getMatch().getNetworkTypeOfService());
			if((wildcards & OFMatch.OFPFW_TP_DST) == 0)
				matchMap.put(FlowSpace.TPDST,reply.getMatch().getTransportDestination());
			if((wildcards & OFMatch.OFPFW_TP_SRC) == 0)
				matchMap.put(FlowSpace.TPSRC,reply.getMatch().getTransportSource());
			cache.put("ofp_match match", matchMap);
		
			cache.put(FlowSpace.DPID, dpid);
			cache.put("table_id", HexString.toHexString(reply.getTableId()));
			cache.put("duraction_nsec", reply.getDurationNanoseconds());
			cache.put("duration_sec",reply.getDurationSeconds());
			cache.put("hard_timeout",reply.getHardTimeout());
			cache.put("idle_timeout", reply.getIdleTimeout());
			cache.put("cookie",reply.getCookie());
			cache.put(FlowSpace.PRIO, String.valueOf(reply.getPriority()));
			cache.put("packet_count", reply.getPacketCount());
			cache.put("byte_count",reply.getByteCount());
			cache.put("length", reply.getLength());

		}
		return cache;
	}

}
