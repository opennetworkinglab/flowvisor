/**
 *
 */
package org.flowvisor.log;

import org.flowvisor.events.FVEventHandler;

/**
 * Generic interface for logging in FV
 *
 * @author capveg
 *
 */
public interface FVLogInterface {

	/**
	 * Do any sort of logging method initializations
	 *
	 * @return did we change the config?
	 */
	public boolean init();

	/**
	 * Log a message
	 *
	 * @param level
	 *            Priority of message
	 * @param source
	 *            Source of message; might be null
	 * @param msg
	 *            Actual message
	 */
	public void log(LogLevel level, long time, FVEventHandler source, String msg);
}
