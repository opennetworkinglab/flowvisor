package org.flowvisor.events;

public class FVStatsTimer extends FVTimerEvent {

	public static final long WAIT_TIME = 30000;

	public FVStatsTimer(FVEventHandler handler) {
		super(0, handler, handler, null);
	}

}
