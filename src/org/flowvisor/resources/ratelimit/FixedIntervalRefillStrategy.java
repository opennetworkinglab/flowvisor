package org.flowvisor.resources.ratelimit;

import java.util.concurrent.TimeUnit;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

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
		FVLog.log(LogLevel.DEBUG, null, "now is ", now, " nextrefilltime is ", nextRefillTime);
		if (now < nextRefillTime) {
			return 0;
		}
		nextRefillTime = now + period;
		return numTokens;

	}

}
