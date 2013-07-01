package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.XidPairWithMessage;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFStatisticsReply.OFStatisticsReplyFlags;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatisticsType;

public final class FVFlowStatisticsRequest extends OFFlowStatisticsRequest
		implements SlicableStatistic, ClassifiableStatistic {



	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
		
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidMsg(msg,fvClassifier, fvSlicer);
		
		if (!fvClassifier.pollFlowTableStats(msg)){
			XidPairWithMessage pair = FVMessageUtil
					.untranslateXidMsg(msg, fvClassifier);
			if (pair == null) {
				FVLog.log(LogLevel.WARN, fvClassifier,
						"dropping unclassifiable stats reply: ", this);
				return;
			}
			FVStatisticsRequest original = (FVStatisticsRequest) pair.getOFMessage();
			
			fvClassifier.sendFlowStatsResp(pair.getSlicer(), original, msg.getFlags());
		}
	}
	
	
	
	
}
