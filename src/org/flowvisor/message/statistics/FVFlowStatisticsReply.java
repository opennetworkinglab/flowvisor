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

public class FVFlowStatisticsReply extends OFFlowStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {

	
	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		XidPairWithMessage pair = FVMessageUtil
				.untranslateXidMsg(msg, fvClassifier);
		if (pair == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable stats reply: ", this);
			return;
		}
		fvClassifier.classifyFlowStats(msg);
		fvClassifier.sendFlowStatsResp(pair.getSlicer(), (FVStatisticsRequest) pair.getOFMessage());
	}



	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + this);
		
	}

}
