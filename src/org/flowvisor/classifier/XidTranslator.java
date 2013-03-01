/**
 *
 */
package org.flowvisor.classifier;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.util.LRULinkedHashMap;

/**
 * @author capveg
 *
 */
public class XidTranslator {

	static final int MIN_XID = 256;
	static final int INIT_SIZE = (1 << 12);
	static final int MAX_SIZE = (1 << 14); // must be larger than the max
											// lifetime of an XID * rate of
											// mesgs/sec
	int nextID;
	LRULinkedHashMap<Integer, XidPair> xidMap;

	public XidTranslator() {
		this.nextID = MIN_XID;
		this.xidMap = new LRULinkedHashMap<Integer, XidPair>(INIT_SIZE,
				MAX_SIZE);
		
	}

	public XidPair untranslate(int xid) {
		return xidMap.get(Integer.valueOf(xid));
	}

	public int translate(int xid, FVSlicer fvSlicer) {
		int ret = this.nextID++;
		if (nextID < MIN_XID)
			nextID = MIN_XID;
		xidMap.put(Integer.valueOf(ret), new XidPair(xid, fvSlicer.getSliceName()));
		return ret;
	}
	
}
