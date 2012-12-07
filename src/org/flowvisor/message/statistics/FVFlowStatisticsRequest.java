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
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public final class FVFlowStatisticsRequest extends OFFlowStatisticsRequest
		implements SlicableStatistic, ClassifiableStatistic {



	@Override
	public void classifyFromSwitch(OFMessage original, List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
	}

	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		if (this.getOutPort() != OFPort.OFPP_NONE.ordinal() && 
				!fvSlicer.portInSlice(this.getOutPort())) {
			throw new StatDisallowedException(
					"Dropping stat " + this + 
					" because outport " + this.getOutPort() + 
					" is not in the slice", 
					OFBadRequestCode.OFPBRC_EPERM);
		}
			
		List<FlowIntersect> intersections = fvSlicer.getFlowSpace().intersects(
				fvClassifier.getDPID(), new FVMatch(getMatch()));
		int expansions = 0;
		for (FlowIntersect intersect : intersections) {
			if (intersect.getFlowEntry().hasPermissions(
					fvSlicer.getSliceName(), SliceAction.WRITE) || 
					intersect.getFlowEntry().hasPermissions(
							fvSlicer.getSliceName(), SliceAction.READ)) {
				expansions++;
				FVFlowStatisticsRequest newFlowStatsReq = (FVFlowStatisticsRequest) this.clone();
			
				newFlowStatsReq.setMatch(intersect.getMatch());
				newFlowStatsReq.setOutPort(outPort);
				newFlowStatsReq.setTableId(tableId);
				approvedStats.add(newFlowStatsReq);
			}
		}
		if (expansions == 0) {
			throw new StatDisallowedException(
					"dropping illegal AggregateStatsRequest: " + this, 
					OFBadRequestCode.OFPBRC_EPERM);
		} else
			FVLog.log(LogLevel.DEBUG, fvSlicer, "expanded AggregateStatsRequest ", expansions,
					" times: ", this);
		
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public FVFlowStatisticsRequest clone() {
        
    	OFMatch neoMatch = match.clone();
    	FVFlowStatisticsRequest newFlowReq = new FVFlowStatisticsRequest();
    	newFlowReq.setMatch(neoMatch);
    	newFlowReq.tableId = this.tableId;
    	newFlowReq.outPort = this.outPort;
            
    	return newFlowReq;
       
    }

}
