package org.flowvisor.message.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class FVQueueStatisticsRequest extends OFQueueStatisticsRequest
		implements ClassifiableStatistic, SlicableStatistic, Cloneable {

	
	/*
	 * defined here because it is missing in OFJ.
	 */
	private static int OFPQ_ALL = 0xffffffff;
	
		@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		/*
		 * TODO: Handle OFPP_ALL -> expand to all ports in slice.
		 * 		Handle OFPQ_ALL -> expand to all queues in slice 
		 */
		Set<Short> expandToPorts = new HashSet<Short>();
		if (!fvSlicer.isAllowAllPorts() && 
				this.portNumber == OFPort.OFPP_ALL.ordinal()) {
			expandToPorts.clear();
			expandToPorts.addAll(fvSlicer.getPorts());
		} else if (!fvSlicer.portInSlice(this.portNumber)) {
			throw new StatDisallowedException("Port " + this.portNumber + 
					" is not in slice " + fvSlicer.getSliceName(), OFBadRequestCode.OFPBRC_EPERM);
		} else {
			expandToPorts.add(this.portNumber);
		}
		
		
		boolean allqs = this.queueId == OFPQ_ALL;
			
		/*
		 * Don't bother expanding queues. It could lead to
		 * a message explosion. When the replies come
		 * we can filter out the wrong queue_ids.
		 */
		FVMatch testMatch = new FVMatch();
		for (Short port : expandToPorts) {
			testMatch.setInputPort(port);
			FVQueueStatisticsRequest qstatsrep = this.clone();
			List<FlowIntersect> intersections = 
					fvSlicer.getFlowSpace().intersects(fvClassifier.getDPID(), testMatch);
			for (FlowIntersect inter : intersections) {
				if (allqs || inter.getFlowEntry().getRuleMatch().getQueues().contains(qstatsrep.queueId)) {
					for (OFAction act : inter.getFlowEntry().getActionsList()) {
						assert(act instanceof SliceAction);
						SliceAction sa = (SliceAction) act;
						if (sa.getSliceName().equals(fvSlicer.getSliceName()))
								approvedStats.add(qstatsrep);
					}
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
	
	public FVQueueStatisticsRequest clone() {
		FVQueueStatisticsRequest rep = new FVQueueStatisticsRequest();
		rep.portNumber = this.portNumber;
		rep.queueId = this.queueId;
		return rep;
	}

}
