package org.flowvisor.resources.ratelimit;

public interface RefillStrategy {

	/*
	 * Defines the refill strategy for a given
	 * token bucket
	 */
	long refill();
	
	
	
}
