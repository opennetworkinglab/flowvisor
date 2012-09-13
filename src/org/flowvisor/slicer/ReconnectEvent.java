/**
 *
 */
package org.flowvisor.slicer;

import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVTimerEvent;

/**
 * @author capveg
 *
 */
public class ReconnectEvent extends FVTimerEvent {

	/**
	 * Signal that we should reconnect in secondsToNextReconnect
	 *
	 * @param secondsToNextReconnect
	 *            relative time
	 * @param src
	 *            both the source and the dest of the event
	 */
	public ReconnectEvent(int secondsToNextReconnect, FVEventHandler src) {
		super(0, src, src, null);
		this.setExpireTime(System.currentTimeMillis() + 1000
				* secondsToNextReconnect);
	}

}
