package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.util.U16;

public class FVSetConfig extends OFSetConfig implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	/**
	 * Fake variable missSendLength parameters
	 *
	 * Save the missSendLength parameter for this slice and the switch<br>
	 *
	 * The switch should always use the MAX missLen of all the slices.
	 *
	 * Update the switch's missLen if it's larger than previously asked for
	 * Replace the missSendLength param with with one for the switch and send it
	 * on the switch
	 *
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
		short missSendLength = this.getMissSendLength();
		fvSlicer.setMissSendLength(missSendLength);
		// check to see if this is a larger missLen param then previously asked
		// for
		if (U16.f(fvClassifier.getMissSendLength()) < U16.f(missSendLength))
			fvClassifier.setMissSendLength(missSendLength);
		else
			this.setMissSendLength(fvClassifier.getMissSendLength());
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
	}

}
