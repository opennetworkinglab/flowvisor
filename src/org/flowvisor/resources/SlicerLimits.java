package org.flowvisor.resources;

import java.util.HashMap;

import org.flowvisor.resources.ratelimit.TokenBucket;

public class SlicerLimits {
	
	private HashMap<String, Integer> sliceFMLimits = null;
	private HashMap<String, TokenBucket> rateLimits = null;
	
	public SlicerLimits() {
		this.sliceFMLimits = new HashMap<String, Integer>();
		this.rateLimits = new HashMap<String, TokenBucket>();
	}

	public synchronized void incrementSliceFMCounter(String sliceName) {
		Integer curr = sliceFMLimits.get(sliceName);
		if (curr == null)
			curr = 0;
		sliceFMLimits.put(sliceName, ++curr);
	}

	public synchronized void decrementSliceFMCounter(String sliceName) {
		Integer curr = sliceFMLimits.get(sliceName);
		if (curr == null || curr <= 0)
			curr = 1;
		sliceFMLimits.put(sliceName, --curr);
	}
	
	public synchronized Integer getSliceFMLimit(String sliceName) {
		Integer curr = sliceFMLimits.get(sliceName);
		if (curr == null) 
			curr = 0;
		return curr;
	}
	
	public TokenBucket getRateLimiter(String slice) {
		return rateLimits.get(slice);
	}
	
	public void setRateLimiter(String slice, TokenBucket bucket) {
		rateLimits.put(slice, bucket);
	}

	
}
