package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;

public class FVFlowStatisticsReply extends OFFlowStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + msg);
	}

	/**
	 * Rewrite flowstats replies to include only the stats that are in this
	 * slice's flowspace
	 */

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		// TODO : implement slicing here; remove irrelevant flows
		// TODO: serve this from cache?
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null)
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable msg: " + msg);
		else {
			fvSlicer.sendMsg(msg, fvClassifier);
		}
	}

}
