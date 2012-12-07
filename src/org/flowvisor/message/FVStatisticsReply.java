package org.flowvisor.message;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.XidPairWithMessage;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.statistics.ClassifiableStatistic;
import org.flowvisor.message.statistics.FVDescriptionStatistics;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFStatisticsMessageBase;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;

public class FVStatisticsReply extends OFStatisticsReply implements
		Classifiable, Slicable, TopologyControllable, SanityCheckable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVLog.log(LogLevel.DEBUG, fvClassifier, "classifying reply : ", this);
		XidPairWithMessage pair = FVMessageUtil
				.untranslateXidMsg(this, fvClassifier);
		FVSlicer fvSlicer = pair.getSlicer();
		OFMessage original = pair.getOFMessage();
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable stats reply: ", this);
			return;
		}
		if (this.getStatistics().size() == 0) {
			FVLog.log(LogLevel.WARN, fvClassifier, "Dropping empty stats reply: ", this);
			return;
		}
		FVLog.log(LogLevel.DEBUG, fvSlicer, "Processing reply : ", this);
		List<OFStatistics> newStatsList = new LinkedList<OFStatistics>();
		Iterator<OFStatistics> it = this.getStatistics().iterator();
		while (it.hasNext()) {
			OFStatistics stat = it.next();
			assert (stat instanceof ClassifiableStatistic);
			try {
				((ClassifiableStatistic) stat).classifyFromSwitch(original, newStatsList, fvClassifier,
						fvSlicer);
				
			} catch (StatDisallowedException e) {
				it.remove();
				this.setLengthU(this.getLengthU() - stat.getLength());
				FVLog.log(LogLevel.WARN, fvSlicer, e.getMessage());
			}
			
		}
		this.setStatistics(newStatsList);
		if (newStatsList.size() == 0) {
			FVLog.log(LogLevel.WARN, fvClassifier, "dropping empty stats reply: "
					+ this);
			return;
		}
		FVLog.log(LogLevel.DEBUG, fvSlicer, "Sending msg : ", this);
		fvSlicer.sendMsg(this, fvClassifier);
		
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
