package org.flowvisor.log;

import org.flowvisor.events.FVEventHandler;

public class DevNullLogger implements FVLogInterface {

	@Override
	public boolean init() {
		return false;
	}

	@Override
	public void log(LogLevel level, long time, FVEventHandler source, String msg) {
		// NOOP
	}

}
