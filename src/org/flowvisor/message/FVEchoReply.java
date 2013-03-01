package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;

public class FVEchoReply extends org.openflow.protocol.OFEchoReply implements
		Slicable, Classifiable {

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		fvSlicer.registerPong();
	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		fvClassifier.registerPong();
	}
	
	
	@Override
	public String toString() {
		return "FVEchoReply [ payload=" + this.getPayload() + "]";
	}
	
}
