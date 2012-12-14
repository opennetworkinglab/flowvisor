package org.flowvisor.classifier;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;

public class XidPairWithMessage extends XidPair {

	OFMessage msg;
	FVSlicer fvSlicer;

	public XidPairWithMessage(XidPair xidPair, OFMessage ofMessage) {
		super(xidPair.xid, xidPair.sliceName);
		this.msg = ofMessage;
	}
	
	public OFMessage getOFMessage() {
		return msg;
	}

	public void setSlicer(FVSlicer slicer) {
		this.fvSlicer = slicer;
		
	}

	public FVSlicer getSlicer() {
		return fvSlicer;
	}
	
	

}
