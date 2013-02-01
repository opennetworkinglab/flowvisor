package org.flowvisor.resources.ratelimit;

import java.util.concurrent.TimeUnit;


public class FixedIntervalRefillStrategy implements RefillStrategy {

	private final long numTokens;
	private final long period;
	private long nextRefillTime;
	
	
	public FixedIntervalRefillStrategy(long numTokens, long period, TimeUnit unit) {
		this.numTokens = numTokens;
		this.period = unit.toNanos(period);
		this.nextRefillTime = -1;
	}

	public synchronized long refill() {
		
		long now = System.nanoTime();
		if (now < nextRefillTime) {
			return 0;
		}
		nextRefillTime = now + period;
		return numTokens;

	}

}
