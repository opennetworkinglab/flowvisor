package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
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
	public void classifyFromSwitch(OFMessage original, List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		/*
		 * TODO: recompress reports flow count to represent the original
		 * number of flowmods the controller actually sent and not the 
		 * expansions.
		 */
		assert(original instanceof FVStatisticsRequest);
		FVStatisticsRequest orig = (FVStatisticsRequest) original;
		FVAggregateStatisticsReply reply = (FVAggregateStatisticsReply) orig.getReply();
		if (reply == null) {
			reply = this;
			orig.setReply(this);
			reply.setFlowCount(fvClassifier.getCurrentFlowModCounter(fvSlicer.getSliceName()));
		} else {
			reply.byteCount += this.byteCount;
			reply.packetCount += this.packetCount;
		}
		approvedStats.add(reply);
		
		
	}
}
