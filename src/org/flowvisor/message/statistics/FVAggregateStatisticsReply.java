package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVAggregateStatisticsReply extends OFAggregateStatisticsReply
		implements SlicableStatistic, ClassifiableStatistic {

	

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
	}

	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
	}
}
