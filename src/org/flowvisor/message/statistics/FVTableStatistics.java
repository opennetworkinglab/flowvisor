package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFTableStatistics;

public class FVTableStatistics extends OFTableStatistics implements
		SlicableStatistic, ClassifiableStatistic {



	@Override
	public void classifyFromSwitch(OFMessage original,
			List<OFStatistics> approvedStats, FVClassifier fvClassifier,
			FVSlicer fvSlicer) throws StatDisallowedException {
		FVLog.log(LogLevel.DEBUG, fvSlicer, "Inner stat process : ", this);
		int currentFMs = fvClassifier.getCurrentFlowModCounter(fvSlicer.getSliceName());
		FVLog.log(LogLevel.DEBUG, fvSlicer, "WTF ", this);
		this.setMaximumEntries(currentFMs);
		if (this.activeCount > currentFMs)
			this.activeCount = currentFMs;
		FVLog.log(LogLevel.DEBUG, fvSlicer, "WTF ", this);
		approvedStats.add(this);
		FVLog.log(LogLevel.DEBUG, fvSlicer, "Approving Table stats : ", this);
	}

	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
		
	}

}
