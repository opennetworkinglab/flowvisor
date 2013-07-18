/**
 *
 */
package org.flowvisor.classifier;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.collections.collection.UnmodifiableCollection;
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
	
	public synchronized CookiePair untranslateAndRemove(Long cookie) {
		return cookieMap.remove(cookie);
	}

	public synchronized CookiePair untranslate(Long cookie) {
		return cookieMap.get(cookie);
	}

	public synchronized long translate(Long cookie, FVSlicer fvSlicer) {
		long ret = this.nextID++;
		if (nextID < MIN_COOKIE)
			nextID = MIN_COOKIE;
		cookieMap.put(ret, new CookiePair(cookie, fvSlicer.getSliceName()));
		return ret;
	}

	public synchronized List<Long> getCookieList(String deleteSlice) {
		Collection<CookiePair> pairs = cookieMap.values();
		List<Long> cookies = new LinkedList<Long>();
		for (CookiePair value : pairs) {
		//for (Entry<Long, CookiePair> entry : cookieMap.entrySet()) {
			if (value.sliceName.equalsIgnoreCase(deleteSlice)) 
				cookies.add(value.getCookie());
		}
		return cookies;
	}
	
}
