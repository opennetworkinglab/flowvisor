package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFQueueConfigRequest;


public class FVQueueConfigRequest extends OFQueueConfigRequest implements
		Classifiable, Slicable  {

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
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
