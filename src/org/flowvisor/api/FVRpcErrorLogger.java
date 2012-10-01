package org.flowvisor.api;

import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.flowvisor.exceptions.FVException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;


/**
 * A wrapper around FVLog for the XMLRPC webserver
 *
 * @author capveg
 *
 */

public class FVRpcErrorLogger extends XmlRpcErrorLogger {

	/**
	 * Wrapper around FVLog;
	 *
	 * If the throwable is an FVException or SSLException, assume
	 * that it's just an API call that is intentionally propagating an
	 * error back to the caller, so we just log it as DEBUG.
	 * Else log as WARN.
	 * @param msg A string to log
	 * @param throwable an exception to log
	 */

	@Override
	public void log(String msg, Throwable throwable) {
		LogLevel logLevel = LogLevel.WARN;
		Throwable cause = throwable.getCause();
		if (cause instanceof FVException ||
				cause instanceof javax.net.ssl.SSLException)
			logLevel = LogLevel.DEBUG;
		if (cause != null)
			throwable = cause;	// skip down to the inner exception
		StackTraceElement[] stackTrace= throwable.getStackTrace();
		FVLog.log(logLevel, null, msg, "(exception = ",throwable.getClass(),")" );
		for(int i=0; i< stackTrace.length; i++)
			FVLog.log(logLevel, null, "     at ", stackTrace[i]);
	}

	@Override
	public void log(String msg) {
		FVLog.log(LogLevel.INFO, null, msg);
	}
}
