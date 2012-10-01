package org.flowvisor.events;

public class FVRequestTimeoutEvent extends FVTimerEvent {

	public static final long WAIT_TIME = 5000;

	public FVRequestTimeoutEvent(FVEventHandler handler) {
		super(0, handler, handler, null);
	}

}
