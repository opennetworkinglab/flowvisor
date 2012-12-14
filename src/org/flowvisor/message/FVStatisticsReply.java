package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.statistics.ClassifiableStatistic;
import org.flowvisor.message.statistics.FVDescriptionStatistics;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFStatisticsMessageBase;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;

public class FVStatisticsReply extends OFStatisticsReply implements
		Classifiable, Slicable, TopologyControllable, SanityCheckable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {	
		
		if (this.getStatistics().size() < 1) {
			FVLog.log(LogLevel.WARN, fvClassifier, "Dropping empty stats reply: ", this);
			return;
		}
		
		OFStatistics stat = this.getStatistics().get(0);
        assert (stat instanceof ClassifiableStatistic);
        ((ClassifiableStatistic) stat).classifyFromSwitch(this,
                        fvClassifier);
		
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// should never get stats replies from controller
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	@Override
	public String toString() {
		return super.toString() + ";st=" + this.getStatisticType();
		// ";mfr=" + this.getManufacturerDescription() +
	}

	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		List<OFStatistics> statList = this.getStatistics();
		for (OFStatistics stat : statList) {
			if (stat instanceof OFDescriptionStatistics) {
				FVLog.log(LogLevel.DEBUG, topologyConnection,
						" got descriptions stats: " + stat);
				topologyConnection
						.setDescriptionStatistics((FVDescriptionStatistics) stat);
			} else {
				FVLog.log(LogLevel.DEBUG, topologyConnection,
						"ignoring unrequested stat: " + stat);
			}
		}
	}

	@Override
	public boolean isSane() {
		int msgLen = this.getLengthU();
		int count;
		count = OFStatisticsMessageBase.MINIMUM_LENGTH;
		for (OFStatistics stat : this.getStatistics()) {
			count += stat.getLength();
		}
		if (count == msgLen)
			return true;
		else {
			FVLog.log(LogLevel.WARN, null, "msg failed sanity check: " + this);
			return false;
		}
	}
}
