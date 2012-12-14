package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFTableStatistics;

public class FVTableStatistics extends OFTableStatistics implements
		SlicableStatistic, ClassifiableStatistic {


	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer ==  null) {
			FVLog.log(LogLevel.WARN, fvClassifier, "Dropping unclassifiable message: ", msg);
			return;
		}
		int currentMax = fvClassifier.getMaxAllowedFlowMods(fvSlicer.getSliceName());
		int currentFMs = fvClassifier.getCurrentFlowModCounter(fvSlicer.getSliceName());
		if (currentMax != -1)
			this.setMaximumEntries(currentMax);
		this.setActiveCount(currentFMs);
		fvSlicer.sendMsg(msg, fvClassifier);
		
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ msg);
		
	}

}
