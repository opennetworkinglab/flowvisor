package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFError.OFBadRequestCode;

public class FVFeaturesRequest extends OFFeaturesRequest implements
		Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		if (fvClassifier.isRateLimited(fvSlicer.getSliceName())) {
			FVLog.log(LogLevel.WARN, fvSlicer,
					"dropping msg because slice", fvSlicer.getSliceName(), " is rate limited: ",
					this);
			FVMessageUtil.makeErrorMsg(OFBadRequestCode.OFPBRC_EPERM, this);
			return;
		}
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
	}
}