package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVPortStatisticsReply extends OFPortStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {


	@Override
	public void classifyFromSwitch(OFMessage original,
			List<OFStatistics> approvedStats, FVClassifier fvClassifier,
			FVSlicer fvSlicer) throws StatDisallowedException {
		
		
		if (!fvSlicer.portInSlice(this.getPortNumber())) 
			throw new StatDisallowedException("Port " + portNumber +  " is not in slice " 
					+ fvSlicer.getSliceName(), OFBadRequestCode.OFPBRC_EPERM);
		approvedStats.add(this);
		
	}

	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + this);
		
	}
}
