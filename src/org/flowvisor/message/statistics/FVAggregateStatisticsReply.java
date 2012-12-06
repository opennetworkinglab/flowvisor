package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVAggregateStatisticsReply extends OFAggregateStatisticsReply
		implements SlicableStatistic, ClassifiableStatistic {



	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
	}

	@Override
	public void classifyFromSwitch(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		/*
		 * TODO: recompress reports flow count to represent the original
		 * number of flowmods the controller actually sent and not the 
		 * expansions.
		 */
		this.setFlowCount(fvClassifier.getCurrentFlowModCounter(fvSlicer.getSliceName()));
		approvedStats.add(this);
	}
}
