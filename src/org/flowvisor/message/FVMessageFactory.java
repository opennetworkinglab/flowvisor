/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.message.actions.FVActionDataLayerDestination;
import org.flowvisor.message.actions.FVActionDataLayerSource;
import org.flowvisor.message.actions.FVActionEnqueue;
import org.flowvisor.message.actions.FVActionNetworkLayerDestination;
import org.flowvisor.message.actions.FVActionNetworkLayerSource;
import org.flowvisor.message.actions.FVActionNetworkTypeOfService;
import org.flowvisor.message.actions.FVActionOutput;
import org.flowvisor.message.actions.FVActionStripVirtualLan;
import org.flowvisor.message.actions.FVActionTransportLayerDestination;
import org.flowvisor.message.actions.FVActionTransportLayerSource;
import org.flowvisor.message.actions.FVActionVendor;
import org.flowvisor.message.actions.FVActionVirtualLanIdentifier;
import org.flowvisor.message.actions.FVActionVirtualLanPriorityCodePoint;
import org.flowvisor.message.statistics.FVAggregateStatisticsReply;
import org.flowvisor.message.statistics.FVAggregateStatisticsRequest;
import org.flowvisor.message.statistics.FVDescriptionStatistics;
import org.flowvisor.message.statistics.FVFlowStatisticsReply;
import org.flowvisor.message.statistics.FVFlowStatisticsRequest;
import org.flowvisor.message.statistics.FVPortStatisticsReply;
import org.flowvisor.message.statistics.FVPortStatisticsRequest;
import org.flowvisor.message.statistics.FVQueueStatisticsReply;
import org.flowvisor.message.statistics.FVQueueStatisticsRequest;
import org.flowvisor.message.statistics.FVTableStatistics;
import org.flowvisor.message.statistics.FVVendorStatistics;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.OFActionFactoryAware;
import org.openflow.protocol.factory.OFMessageFactoryAware;
import org.openflow.protocol.factory.OFStatisticsFactoryAware;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * @author capveg
 *
 */
public class FVMessageFactory extends BasicFactory {

	// not sure how to deal with this...
	// HACK to convert OFMessage* to FVMessage*
	@SuppressWarnings("rawtypes")
	static final Class convertMap[] = {FVHello.class, FVError.class,
			FVEchoRequest.class, FVEchoReply.class, FVVendor.class,
			FVFeaturesRequest.class, FVFeaturesReply.class,
			FVGetConfigRequest.class, FVGetConfigReply.class,
			FVSetConfig.class, FVPacketIn.class, FVFlowRemoved.class,
			FVPortStatus.class, FVPacketOut.class, FVFlowMod.class,
			FVPortMod.class, FVStatisticsRequest.class,
			FVStatisticsReply.class, FVBarrierRequest.class,
			FVBarrierReply.class, FVQueueConfigRequest.class, FVQueueConfigReply.class };

	@SuppressWarnings({ "rawtypes" })
	static final Class convertActionsMap[] = { FVActionOutput.class,
			FVActionVirtualLanIdentifier.class,
			FVActionVirtualLanPriorityCodePoint.class,
			FVActionStripVirtualLan.class, FVActionDataLayerSource.class,
			FVActionDataLayerDestination.class,
			FVActionNetworkLayerSource.class,
			FVActionNetworkLayerDestination.class,
			FVActionNetworkTypeOfService.class,
			FVActionTransportLayerSource.class,
			FVActionTransportLayerDestination.class, FVActionEnqueue.class,
			FVActionVendor.class };

	@SuppressWarnings("rawtypes")
	static final Class convertStatsRequestMap[] = {
			FVDescriptionStatistics.class, FVFlowStatisticsRequest.class,
			FVAggregateStatisticsRequest.class, FVTableStatistics.class,
			FVPortStatisticsRequest.class, FVQueueStatisticsRequest.class,
			FVVendorStatistics.class };

	@SuppressWarnings("rawtypes")
	static final Class convertStatsReplyMap[] = {
			FVDescriptionStatistics.class, FVFlowStatisticsReply.class,
			FVAggregateStatisticsReply.class, FVTableStatistics.class,
			FVPortStatisticsReply.class, FVQueueStatisticsReply.class,
			FVVendorStatistics.class };

	@SuppressWarnings("unchecked")
	@Override
	public OFMessage getMessage(OFType t) {
		if (t == null)
			return new FVUnknownMessage();
		byte mtype = t.getTypeValue();
		if (mtype >= convertMap.length)
			throw new IllegalArgumentException("OFMessage type " + mtype
					+ " unknown to FV");
		Class<? extends OFMessage> c = convertMap[mtype];
		try {
			OFMessage m = c.getConstructor(new Class[] {}).newInstance();
			if (m instanceof OFMessageFactoryAware)
				((OFMessageFactoryAware) m).setMessageFactory(this);
			if (m instanceof OFActionFactoryAware) {
				((OFActionFactoryAware) m).setActionFactory(this);
			}
			if (m instanceof OFStatisticsFactoryAware) {
				((OFStatisticsFactoryAware) m).setStatisticsFactory(this);
			}
			return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public OFAction getAction(OFActionType t) {
		Class<? extends OFAction> c = convertActionsMap[t.getTypeValue()];
		try {
			return c.getConstructor(new Class[] {}).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	// big hack; need to fix
	@Override
	public OFStatistics getStatistics(OFType t, OFStatisticsType st) {
		Class<? extends OFStatistics> c;
		if (t == OFType.STATS_REPLY)
			if (st.getTypeValue() == -1)
				c = FVVendorStatistics.class;
			else
				c = convertStatsReplyMap[st.getTypeValue()];
		else if (t == OFType.STATS_REQUEST)
			if (st.getTypeValue() == -1)
				c = FVVendorStatistics.class;
			else
				c = convertStatsRequestMap[st.getTypeValue()];
		else
			throw new RuntimeException("non-stats type in stats factory: " + t);
		try {
			return c.getConstructor(new Class[] {}).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
