package org.flowvisor.classifier;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.util.LRULinkedHashMap;

public class XidTranslatorWithMessage extends XidTranslator {

	
	LRULinkedHashMap<Integer, OFMessage> msgMap;
	
	public XidTranslatorWithMessage() {
		super();
		this.msgMap = new LRULinkedHashMap<Integer, OFMessage>(INIT_SIZE,
				MAX_SIZE);
	}
	
	public int translate(OFMessage original, int xid, FVSlicer fvSlicer) {
		int ret = translate(xid, fvSlicer);
		msgMap.put(Integer.valueOf(ret), original);
		return ret;
	}
	
	public XidPairWithMessage untranslate(int xid) {
		XidPair xidPair = super.untranslate(xid);
		if (xidPair == null)
			return null;
		return new XidPairWithMessage(xidPair, msgMap.get(xid));
	}
}
