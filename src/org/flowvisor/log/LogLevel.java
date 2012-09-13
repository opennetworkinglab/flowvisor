/**
 *
 */
package org.flowvisor.log;

import org.apache.log4j.Level;

/**
 * Logging Priority Levels (very similar to syslog) Sorted in descending order
 * of importance.
 *
 * @author capveg
 *
 */
public enum LogLevel {
	FATAL(Level.FATAL), // The world is on fire
	CRIT(Level.ERROR), // Will always want to know
	ALERT(Level.ERROR), // Will typically want to know
	WARN(Level.WARN), // Might want to know cuz it's possibly
	// bad
	INFO(Level.INFO), // Maybe worth knowing, maybe not -- not bad
	NOTE(Level.INFO),

	DEBUG(Level.DEBUG), /* Debug */
	MOBUG(Level.TRACE); // more debugging; rarely worth knowing

	@Override
	public String toString() {
		return this.name();
	}

	Level priority;

	LogLevel(Level priority) {
		this.priority = priority;
	}

	LogLevel() {
		this.priority = null;
	}

	public Level getPriority() {
		return this.priority;
	}
}
