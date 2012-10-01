/**
 *
 */
package org.flowvisor.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.flowvisor.events.FVEventHandler;

/**
 * @author capveg
 *
 */
public class StderrLogger implements FVLogInterface {

	DateFormat df;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.log.FVLogInterface#init()
	 */
	@Override
	public boolean init() {
		// this.df =
		// DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
		this.df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.log.FVLogInterface#log(org.flowvisor.log.FVLogLevel,
	 * org.flowvisor.events.FVEventHandler, java.lang.String)
	 */
	@Override
	public void log(LogLevel level, long time, FVEventHandler source, String msg) {
		String srcString = null;
		if (source != null)
			srcString = source.getName();
		else
			srcString = "none";
		System.err.println(String.format("%5s", level.toString()) + ":"
				+ df.format(time) + ":" + srcString + ":: " + msg);
	}
}
