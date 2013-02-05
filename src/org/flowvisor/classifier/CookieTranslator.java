/**
 *
 */
package org.flowvisor.classifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.util.LRULinkedHashMap;

/**
 * @author alshabib
 *
 */
public class CookieTranslator {

	static final long MIN_COOKIE = 256;
	static final int INIT_SIZE = (1 << 12);
	static final int MAX_SIZE = (1 << 16); // must be larger than the max
											// lifetime of an XID * rate of
											// mesgs/sec
	long nextID;
	
	LRULinkedHashMap<Long, CookiePair> cookieMap;

	public CookieTranslator() {
		this.nextID = MIN_COOKIE;
		this.cookieMap = new LRULinkedHashMap<Long, CookiePair>(INIT_SIZE,
				MAX_SIZE);
		
	}
	
	public CookiePair untranslateAndRemove(Long cookie) {
		return cookieMap.remove(cookie);
	}

	public CookiePair untranslate(Long cookie) {
		return cookieMap.get(cookie);
	}

	public long translate(Long cookie, FVSlicer fvSlicer) {
		long ret = this.nextID++;
		if (nextID < MIN_COOKIE)
			nextID = MIN_COOKIE;
		cookieMap.put(ret, new CookiePair(cookie, fvSlicer.getSliceName()));
		return ret;
	}

	public List<Long> getCookieList(String deleteSlice) {
		List<Long> cookies = new LinkedList<Long>();
		for (Entry<Long, CookiePair> entry : cookieMap.entrySet()) {
			if (entry.getValue().sliceName.equalsIgnoreCase(deleteSlice)) 
				cookies.add(entry.getValue().getCookie());
		}
		return cookies;
	}
	
}
