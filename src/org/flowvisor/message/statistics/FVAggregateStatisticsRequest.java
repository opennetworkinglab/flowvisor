package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;

public class FVAggregateStatisticsRequest extends
		org.openflow.protocol.statistics.OFAggregateStatisticsRequest implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ msg);
	}

}
