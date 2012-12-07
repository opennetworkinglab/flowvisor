package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.flows.FlowIntersect;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class FVQueueStatisticsRequest extends OFQueueStatisticsRequest
		implements ClassifiableStatistic, SlicableStatistic {

		@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		if (!fvSlicer.portInSlice(this.portNumber)) {
			throw new StatDisallowedException("Port " + this.portNumber + 
					" is not in slice " + fvSlicer.getSliceName(), OFBadRequestCode.OFPBRC_EPERM);
		}
			
		FVMatch testMatch = new FVMatch();
		testMatch.setInputPort(this.portNumber);
		List<FlowIntersect> intersections = 
				fvSlicer.getFlowSpace().intersects(fvClassifier.getDPID(), testMatch);
		for (FlowIntersect inter : intersections) {
			if (inter.getFlowEntry().getRuleMatch().getQueues().contains(this.queueId)) {
				for (OFAction act : inter.getFlowEntry().getActionsList()) {
					assert(act instanceof SliceAction);
					SliceAction sa = (SliceAction) act;
					if (sa.getSliceName().equals(fvSlicer.getSliceName()))
							approvedStats.add(this);
				}
			}
		}
		throw new StatDisallowedException("QueueId " + this.queueId + 
				" is not in slice " + fvSlicer.getSliceName(), OFBadRequestCode.OFPBRC_EPERM);
	}

	@Override
	public void classifyFromSwitch(OFMessage original,
			List<OFStatistics> approvedStats, FVClassifier fvClassifier,
			FVSlicer fvSlicer) throws StatDisallowedException {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
		
	}

}
