package org.flowvisor.events;

/**
 * Signal that the event handler should be torn down
 *
 * @author capveg
 *
 */

public class TearDownEvent extends FVEvent {

	public TearDownEvent(FVEventHandler src, FVEventHandler dst) {
		super(src, dst);
	}

}
