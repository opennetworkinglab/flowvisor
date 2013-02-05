package org.flowvisor.resources.ratelimit;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;


public class TokenBucket {

	private final RefillStrategy strategy;
	private final long capacity;
	private long size;
	
	public TokenBucket(long capacity, RefillStrategy refillStrategy) {
		this.capacity = capacity;
		this.strategy = refillStrategy;
		this.size = capacity;
	}
	
	public TokenBucket() {
		this(200, new NoLimitRefillStrategy(200));
	}

	/**
	 * Attempt to consume a single token from the bucket.  If it was consumed then {@code true} is returned, otherwise
	 * {@code false} is returned.
	 *
	 * @return {@code true} if a token was consumed, {@code false} otherwise.
	 */
	public boolean consume() {
		return consume(1);
	}

	/**
	 * Attempt to consume a specified number of tokens from the bucket.  If the tokens were consumed then {@code true}
	 * is returned, otherwise {@code false} is returned.
	 *
	 * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
	 * @return {@code true} if the tokens were consumed, {@code false} otherwise.
	 */
	public synchronized boolean consume(long numTokens) {

		// Give the refill strategy a chance to add tokens if it needs to
		long newTokens = Math.max(0, strategy.refill());
		this.size = Math.max(0, Math.min(this.size + newTokens, capacity));
		FVLog.log(LogLevel.DEBUG, null, "Refilled " + newTokens + " bucket has " + this.size);
		// Now try to consume some tokens
		if (numTokens <= this.size) {
			this.size -= numTokens;
			return true;
		}

		return false;
	}
	
	public long currentRate() {
		return this.capacity - this.size;
	}



}
