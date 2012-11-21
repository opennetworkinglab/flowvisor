package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFQueueConfigReply;


public class FVQueueConfigReply extends OFQueueConfigReply implements
		Classifiable, Slicable  {

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable xid in QueueConfigReply: " + this);
			return;
		}
		if (fvSlicer.portInSlice(this.port))
			fvSlicer.sendMsg(this, fvClassifier);
		else 
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping QueueConfigReply because port is not in slice: " + 
					fvSlicer.getSliceName() + " : " + this);
	}

}