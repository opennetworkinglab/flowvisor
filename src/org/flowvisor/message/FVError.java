/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;

/**
 * @author capveg
 *
 */
public class FVError extends org.openflow.protocol.OFError implements
		Classifiable, Slicable {

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.
	 * classifier.FVClassifier)
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping msg with unknown xid: " + this);
			return;
		}
		if (this.errorType == (short) OFErrorType.OFPET_BAD_ACTION.ordinal() 
				|| this.errorType == (short) OFErrorType.OFPET_FLOW_MOD_FAILED.ordinal()) {
			fvSlicer.decrementFlowRules();
		}
		fvSlicer.sendMsg(this, fvClassifier);
	};

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier
	 * .FVClassifier, org.flowvisor.slicer.FVSlicer)
	 */
	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String ret = /*super.toString() + */  "c=" + this.getErrorCode() + ";t="
				+ getErrorType();
		
		OFMessage offendingMsg = null;
		if (!isErrorIsAscii() && (offendingMsg = getOffendingMsg()) != null)
			ret += ";msg=" + offendingMsg.toString();
		
		if (error != null) {
			if (errorIsAscii)
				ret += ";err=" + new String(error);
			else
				ret += ";err=[" + error.length + "]";
		} else
			ret += ";msg=NONE(!?)";
		return ret;
	}

}
