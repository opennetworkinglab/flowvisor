package org.flowvisor.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEventHandler;
import org.productivity.java.syslog4j.SyslogRuntimeException;
import org.productivity.java.syslog4j.impl.log4j.Syslog4jAppenderSkeleton;

public class AnyLogger extends Syslog4jAppenderSkeleton implements FVLogInterface {
	
	private static final long serialVersionUID = 1L;
	static public String DEFAULT_LOGGING_FACILITY = "LOCAL7";
	static public String DEFAULT_LOGGING_IDENT = "flowvisor";
	
	static Logger logger = Logger.getLogger(AnyLogger.class.getName());
	

	@Override
	public boolean init() {
		String propFile = System.getProperty("fvlog.configuration");
		PropertyConfigurator.configureAndWatch(propFile, 60000);
		
		logger.log(Level.INFO, "started flowvisor logger");
		return false;
	}
	
	
	
	
	@Override
	public void log(LogLevel level, long time, FVEventHandler source, String msg) {
		if (level == LogLevel.MOBUG)
			return;
		String srcString = null;
		if (source != null)
			srcString = source.getName();
		else
			srcString = "none";
		logger.log(level.getPriority(), srcString + " : " + msg);

	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.productivity.java.syslog4j.impl.log4j.Syslog4jAppenderSkeleton#requiresLayout()
	 * 
	 * Stupid Syslog4jAppender doesn't layout stuff to syslog, and also breaks log4j's 
	 * requirement to return true frim this method even if you don't layout.
	 */
	public boolean requiresLayout() {
		return true;
	}

	@Override
	public String initialize() throws SyslogRuntimeException {
		if (this.protocol == null)
			this.protocol = UDP;
		
		try {
			String fac = FVConfig.getLogFacility();
			this.facility = fac;
			if (this.facility == null) {
				this.facility = DEFAULT_LOGGING_FACILITY;
				System.err
						.println("Invalid logging facitily: failing back to default: '"
								+ fac + "'");
			}
		} catch (Exception e) {
			try {
				FVConfig.setLogFacility(DEFAULT_LOGGING_FACILITY);
				this.facility = DEFAULT_LOGGING_FACILITY;
			} catch (ConfigError e1) {
				System.err.println("Failed to set logging facility"
						+ " to '" + this.facility + ": " + e1);
			}

		}
		try {
			this.ident = FVConfig.getLogIdent();
		} catch (Exception e) {
			try {
				FVConfig.setLogIdent(DEFAULT_LOGGING_IDENT);
				this.ident = DEFAULT_LOGGING_IDENT;
			} catch (ConfigError e1) {
				System.err.println("Failed to set logging identifier " 
						+ " to '" + this.ident + ": " + e1);
			}

		}

		return this.protocol;
	}

}
