package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesRequest;

public class FVFeaturesRequest extends OFFeaturesRequest implements
		Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
	}
	
	@Override
	public String toString() {
		return "FVFeaturesRequest []"; 
	}

	
}