package org.flowvisor.events;

import org.flowvisor.config.FVConfig;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

public class FVEventUtils {
	
	static long eventCount = 0;
	static long total = 0;
	public static long averageDelay = 0;
	public static long instDelay = 0;
	

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
		long delay = System.currentTimeMillis() - startCounter;
		FVEventUtils.processDelay(delay);
		if (delay > FVConfig.DelayWarning) {
			FVLog.log(LogLevel.ALERT, e.getDst(),
					"STARVING: handling event took "
							+ delay + "ms: " + e);
		}
	}
	
	/*
	 * Lazy-ly hoping that the counter won't wrap...
	 * ha!
	 */
	static void processDelay(long delay) {
		eventCount++;
		total += delay;
		averageDelay = total/eventCount;
		instDelay = delay;
	}
}
