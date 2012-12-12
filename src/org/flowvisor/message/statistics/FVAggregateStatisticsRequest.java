package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;

public class FVAggregateStatisticsRequest extends
		org.openflow.protocol.statistics.OFAggregateStatisticsRequest implements
		SlicableStatistic, ClassifiableStatistic {

	


	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
//		if (this.getOutPort() != OFPort.OFPP_NONE.ordinal() && 
//				!fvSlicer.portInSlice(this.getOutPort())) {
//			throw new StatDisallowedException(
//					"Dropping stat " + this + 
//					" because outport " + this.getOutPort() + 
//					" is not in the slice", 
//					OFBadRequestCode.OFPBRC_EPERM);
//		}
//			
//		List<FlowIntersect> intersections = fvSlicer.getFlowSpace().intersects(
//				fvClassifier.getDPID(), new FVMatch(getMatch()));
//		int expansions = 0;
//		for (FlowIntersect intersect : intersections) {
//			if (intersect.getFlowEntry().hasPermissions(
//					fvSlicer.getSliceName(), SliceAction.WRITE) || 
//					intersect.getFlowEntry().hasPermissions(
//							fvSlicer.getSliceName(), SliceAction.READ)) {
//				expansions++;
//				FVAggregateStatisticsRequest newAggStatsReq = (FVAggregateStatisticsRequest) this.clone();
//			
//				newAggStatsReq.setMatch(intersect.getMatch());
//				newAggStatsReq.setOutPort(outPort);
//				newAggStatsReq.setTableId(tableId);
//				approvedStats.add(newAggStatsReq);
//			}
//		}
//		if (expansions == 0) {
//			throw new StatDisallowedException(
//					"dropping illegal AggregateStatsRequest: " + this, 
//					OFBadRequestCode.OFPBRC_EPERM);
//		} else
//			FVLog.log(LogLevel.DEBUG, fvSlicer, "expanded AggregateStatsRequest ", expansions,
//					" times: ", this);
		
	}




}
