/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadRequestCode;

/**
 * @author capveg
 *
 */
public class FVBarrierRequest extends org.openflow.protocol.OFBarrierRequest
		implements Slicable, Classifiable {

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier
	 * .FVClassifier, org.flowvisor.slicer.FVSlicer)
	 */
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

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.
	 * classifier.FVClassifier)
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

}
