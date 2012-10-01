package org.flowvisor.message.statistics;

import java.util.Iterator;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVPortStatisticsReply extends OFPortStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + msg);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable port stats reply: " + this);
			return;
		}
		boolean changed = false;
		OFStatisticsReply statsReply = (OFStatisticsReply) msg;
		for (Iterator<OFStatistics> it = statsReply.getStatistics().iterator(); it
				.hasNext();) {
			OFStatistics stat = it.next();
			if (stat instanceof OFPortStatisticsReply) {
				OFPortStatisticsReply portStat = (OFPortStatisticsReply) stat;
				if (!fvSlicer.portInSlice(portStat.getPortNumber())) {
					it.remove();
					changed = true;
				}
			}
		}
		if (changed) { // removed a stat; rebuild packet
			int statsLen = 0;
			for (OFStatistics stat : statsReply.getStatistics()) {
				statsLen += stat.getLength();
			}
			statsReply.setLengthU(statsLen + OFStatisticsReply.MINIMUM_LENGTH);
		}
		fvSlicer.sendMsg(msg, fvClassifier);
	}
}
