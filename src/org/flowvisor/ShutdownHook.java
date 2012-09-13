package org.flowvisor;

import org.flowvisor.config.FVConfigurationController;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * 
 * Attempts to hook into the VM to detect a shutdown 
 * procedure. It is not guaranteed but better than 
 * nothing.
 * 
 * Shuts down the db backend cleanly.
 * 
 * 
 * @author ash
 *
 */
public class ShutdownHook extends Thread {
	public void run() {
		FVLog.log(LogLevel.INFO, null, "Shutting down config database.");
		FVConfigurationController.instance().shutdown();
    }
}
