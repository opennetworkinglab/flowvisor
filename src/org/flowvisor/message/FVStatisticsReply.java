package org.flowvisor.message;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
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

		FVSlicer fvSlicer = FVMessageUtil
				.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable stats reply: " + this);
			return;
		}
		boolean hasBody = false;
		List<OFStatistics> newStatsList = new LinkedList<OFStatistics>();
		Iterator<OFStatistics> it = this.getStatistics().iterator();
		while (it.hasNext()) {
			OFStatistics stat = it.next();
			assert (stat instanceof ClassifiableStatistic);
			try {
				((ClassifiableStatistic) stat).classifyFromSwitch(newStatsList, fvClassifier,
						fvSlicer);
				hasBody = true;
			} catch (StatDisallowedException e) {
				it.remove();
				this.setLengthU(this.getLengthU() - stat.getLength());
				FVLog.log(LogLevel.WARN, fvSlicer, e.getMessage());
			}
			
		}
		if (!hasBody)
			fvSlicer.sendMsg(this, fvClassifier);
		else {
			this.setLengthU(OFStatisticsMessageBase.MINIMUM_LENGTH);
			for (OFStatistics stat : newStatsList) {
				this.setLengthU(this.getLengthU() + stat.getLength());
				this.setStatistics(newStatsList);
			}
			fvSlicer.sendMsg(this, fvClassifier);
		}
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
