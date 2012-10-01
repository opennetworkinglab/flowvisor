package org.flowvisor.message;

public interface SanityCheckable {
	/**
	 * A sanity check on the message to decide if it is safe to send or not
	 *
	 * @return true --> safe to send, false --> should be dropped
	 */
	public boolean isSane();
}
