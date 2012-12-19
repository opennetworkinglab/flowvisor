package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFQueueConfigRequest;


public class FVQueueConfigRequest extends OFQueueConfigRequest implements
		Classifiable, Slicable  {

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		if (fvClassifier.isRateLimited(fvSlicer.getSliceName())) {
			FVLog.log(LogLevel.WARN, fvSlicer,
					"dropping msg because slice", fvSlicer.getSliceName(), " is rate limited: ",
					this);
			FVMessageUtil.makeErrorMsg(OFBadRequestCode.OFPBRC_EPERM, this);
			return;
		}
		if (!fvSlicer.portInSlice(this.port)) {
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					OFBadRequestCode.OFPBRC_EPERM, this), fvClassifier);
			return;
		}
		
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
			
	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

}
