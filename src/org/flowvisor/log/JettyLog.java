package org.flowvisor.log;

import org.eclipse.jetty.util.log.Logger;


public class JettyLog implements Logger{

	@Override
	public void debug(String arg0) {
		FVLog.log(LogLevel.DEBUG,null, arg0);
	}

	@Override
	public void debug(String arg0, Throwable arg1) {
		FVLog.log(LogLevel.DEBUG,null, arg0, arg1);
	}

	@Override
	public void debug(String arg0, Object arg1, Object arg2) {
		FVLog.log(LogLevel.DEBUG,null, arg0, arg1, arg2);
	}


	@Override
	public String getName() {
		return FVLog.logger.getClass().getName();
	}

	@Override
	public void info(String arg0) {
		FVLog.log(LogLevel.INFO, null, arg0);

	}

	@Override
	public void info(String arg0, Object arg1, Object arg2) {
		FVLog.log(LogLevel.INFO, null, arg0, arg1, arg2);
	}

	@Override
	public boolean isDebugEnabled() {
		return FVLog.getThreshold() == LogLevel.DEBUG;
	}

	@Override
	public void setDebugEnabled(boolean arg0) {
		FVLog.setThreshold(LogLevel.DEBUG);
	}

	@Override
	public void warn(String arg0) {
		FVLog.log(LogLevel.WARN, null, arg0);

	}

	@Override
	public void warn(String arg0, Throwable arg1) {
		FVLog.log(LogLevel.WARN, null, arg0, arg1);
	}

	@Override
	public void warn(String arg0, Object arg1, Object arg2) {
		FVLog.log(LogLevel.WARN, null, arg0, arg1, arg2);
	}

	@Override
	public Logger getLogger(String arg0) {
		return this;
	}


}
