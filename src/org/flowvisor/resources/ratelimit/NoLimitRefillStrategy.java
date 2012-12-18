package org.flowvisor.resources.ratelimit;


public class NoLimitRefillStrategy implements RefillStrategy {

	private final long numTokens;	
	
	public NoLimitRefillStrategy(long numTokens) {
		this.numTokens = numTokens;
	}

	public long refill() {
		
		return numTokens;

	}

}
