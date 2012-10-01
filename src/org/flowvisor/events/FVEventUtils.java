package org.flowvisor.events;

import org.flowvisor.config.FVConfig;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

public class FVEventUtils {

	/**
	 * Test to see if more than FVConfig.DelayWarning ms have passed, and if so,
	 * issue a warning log msg
	 *
	 * @param startCounter
	 * @param handler
	 * @param e
	 */
	static public void starvationTest(long startCounter,
			FVEventHandler handler, FVEvent e) {
		long stopCounter = System.currentTimeMillis();
		if ((stopCounter - startCounter) > FVConfig.DelayWarning) {
			FVLog.log(LogLevel.ALERT, e.getDst(),
					"STARVING: handling event took "
							+ (stopCounter - startCounter) + "ms: " + e);
		}
	}
}
