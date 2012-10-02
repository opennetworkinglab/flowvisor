package org.flowvisor.resources;

import java.util.HashMap;

public class SlicerLimits {
	
	private HashMap<String, Integer> sliceFMLimits = null;
	
	public SlicerLimits() {
		this.sliceFMLimits = new HashMap<String, Integer>();
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

	
}
