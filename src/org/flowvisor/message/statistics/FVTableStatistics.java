package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFTableStatistics;

public class FVTableStatistics extends OFTableStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		// TODO generate separate request/reply messages
		// TODO return the count of flows used by this slice
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		// TODO generate separate request/reply messages
		// TODO return the count of flows used by this slice
		FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
	}

}
