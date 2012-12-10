package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVFlowStatisticsReply extends OFFlowStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {



	@Override
	public void classifyFromSwitch(OFMessage original, List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		
		/*
		 * Take original request, intersect it with the flowspace.
		 * for each intersection check if the actual reply contains
		 * that match. If it does check if we already approuved such
		 * a match and if so combine the stats. Otherwise approuve a 
		 * new one. In both cases, replace match with the original
		 * match in the original request.
		 */
		/*assert(original instanceof FVStatisticsRequest);
		List<OFStatistics> origStats = ((FVStatisticsRequest) original).getStatistics();
		for (OFStatistics origStat : origStats) {
			assert(origStat instanceof FVFlowStatisticsRequest);
			FVFlowStatisticsRequest origReq = (FVFlowStatisticsRequest) origStat;
			List<FlowIntersect> intersections = 
					fvSlicer.getFlowSpace().intersects(fvClassifier.getDPID(), 
							new FVMatch(origReq.getMatch()));
			for (FlowIntersect inter : intersections) {
				if (new FVMatch(inter.getMatch()).equals(new FVMatch(this.getMatch()))) {
					FVFlowStatisticsReply alreadyApprouved = searchApprovedStats(approvedStats, origReq.getMatch());
					if (alreadyApprouved != null) {
						alreadyApprouved.byteCount += this.byteCount;
						alreadyApprouved.packetCount += this.packetCount;
						approvedStats.add(alreadyApprouved);
					} else {
						//this.setMatch(origReq.getMatch());
						approvedStats.add(this);
					}
					return;
				}
			}
		}
		throw new StatDisallowedException("Unknown FlowStatsReply: " + this, OFBadRequestCode.OFPBRC_BAD_STAT);*/
		approvedStats.add(this);
	}

/*	private FVFlowStatisticsReply searchApprovedStats(
			List<OFStatistics> approvedStats, OFMatch match) {
		
		Iterator<OFStatistics> it = approvedStats.iterator();
		while (it.hasNext()) {
			OFStatistics s = it.next();
			if (s instanceof FVFlowStatisticsReply) {
				FVFlowStatisticsReply stat = (FVFlowStatisticsReply) s;
				if (stat.getMatch().equals(match)) {
					it.remove();
					return stat;
				}
			} else
				continue;
		}
		return null;
	}
	*/

	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + this);
		
	}

}
