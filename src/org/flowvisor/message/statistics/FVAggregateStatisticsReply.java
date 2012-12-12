package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;

public class FVAggregateStatisticsReply extends OFAggregateStatisticsReply
		implements SlicableStatistic, ClassifiableStatistic {



	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		// TODO Auto-generated method stub
//		assert(original instanceof FVStatisticsRequest);
//		FVStatisticsRequest orig = (FVStatisticsRequest) original;
//		FVAggregateStatisticsReply reply = (FVAggregateStatisticsReply) orig.getReply();
//		if (reply == null) {
//			reply = this;
//			orig.setReply(this);
//			reply.setFlowCount(fvClassifier.getCurrentFlowModCounter(fvSlicer.getSliceName()));
//		} else {
//			reply.byteCount += this.byteCount;
//			reply.packetCount += this.packetCount;
//		}
//		approvedStats.add(reply);
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
		
	}
}
