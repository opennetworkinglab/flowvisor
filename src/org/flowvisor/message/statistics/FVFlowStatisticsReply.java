package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.XidPairWithMessage;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatisticsType;

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
		fvClassifier.classifyFlowStats(msg);
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

}
