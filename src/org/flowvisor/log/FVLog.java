/**
 *
 */
package org.flowvisor.log;

import org.flowvisor.FlowVisor;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.FVMessageFactory;
import org.openflow.protocol.OFType;

/**
 * Static method
 *
 * @author capveg
 *
 */
public class FVLog {
	
	static boolean needsInit = true;
	static FVLogInterface logger = new AnyLogger();
	static LogLevel threshold = null;

	/**
	 * Wrapper around the default logger
	 *
	 * @param level
	 *            Log priority
	 * @param source
	 *            who sent the log message (can be null)
	 * @param msgs
	 *            Log message
	 */
	public static synchronized void log(LogLevel level, FVEventHandler source,
			Object... msgs) {
		if (needsInit)
			doInit();
		if (level.ordinal() <= threshold.ordinal() && (msgs.length > 0)) {
			StringBuilder stringBuilder = new StringBuilder(msgs[0].toString());
			for (int i = 1; i < msgs.length; i++){
				if (msgs[i] != null)
					stringBuilder.append(msgs[i].toString());
			}

			logger.log(level, System.currentTimeMillis(), source,
					stringBuilder.toString());
		}
	}

	private static void doInit() {
		needsInit = false;
		// hack around setup if we don't want any logging
		if (FVLog.logger instanceof DevNullLogger) {
			threshold = LogLevel.FATAL;
			return;
		}
		boolean needConfigFlush = false;
		try {
			if (threshold == null)
				threshold = LogLevel.valueOf(FVConfig
						.getLogging());
		} catch (ConfigError e) {
			System.err.println("--- Logging threshold"  
					+ "' not set in config; defaulting to loglevel 'NOTE'");
			try {
				FVConfig.setLogging(LogLevel.NOTE.toString());
				needConfigFlush = true;

			} catch (ConfigError e1) {
				throw new RuntimeException(e1);
			}
			threshold = LogLevel.DEBUG;
		}
		try {
			logger.init();
		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Unable to load default logger; failing over to stderr: "
							+ e);
			logger = new StderrLogger();
			logger.init();
		}
		System.err.println("--- Setting logging level to " + threshold);
		if (needConfigFlush) {
			FlowVisor fv = FlowVisor.getInstance();
			if (fv != null)
				fv.checkPointConfig();
		}
		for (LogLevel level : LogLevel.class.getEnumConstants()) {
			if (level != LogLevel.FATAL) // fatal gets broadcasted to console
				FVLog.log(level, null, "log level enabled: " + level);
		}
	}

	/**
	 * Change the default logger
	 *
	 * @param logger
	 *            New logger
	 */
	public static synchronized void setDefaultLogger(FVLogInterface logger) {
		FVLog.logger = logger;
		needsInit = true;
	}

	/**
	 * Get the logging threshold
	 *
	 */
	public static LogLevel getThreshold() {
		return FVLog.threshold;
	}

	/**
	 * Set the logging threshold All logs equal to or greater than this level
	 * are logged
	 */
	public static void setThreshold(LogLevel l) {
		FVLog.threshold = l;
	}

	/**
	 * Benchmarking tool for the logging systems
	 *
	 * @param args
	 */
	public static void main(String args[]) {
		long iterations = 10000000;
		FVLog.logger = new DevNullLogger();
		FVLog.log(LogLevel.ALERT, null, "Setting up logging facility");
		long start1 = System.currentTimeMillis();
		FVMessageFactory factory = new FVMessageFactory();
		FVFlowMod fm = (FVFlowMod) factory.getMessage(OFType.FLOW_MOD);
		for (long it = 0; it < iterations; it++)
			FVLog.log(LogLevel.MOBUG, null, "LogLevel.INFO test" + " one "
					+ " two" + " three " + fm);
		long stop1 = System.currentTimeMillis();
		for (long it = 0; it < iterations; it++)
			FVLog.log(LogLevel.MOBUG, null, "LogLevel.MOBUG test", " one ",
					" two", " three ", fm);
		long stop2 = System.currentTimeMillis();
		double slow = iterations * 1.0 / (stop1 - start1);
		double fast = iterations * 1.0 / (stop2 - stop1);
		System.out.println("Slow run logs/second: " + slow);
		System.out.println("Fast run logs/second: " + fast);
		System.out.println("Difference: " + fast / slow);
	}
}
