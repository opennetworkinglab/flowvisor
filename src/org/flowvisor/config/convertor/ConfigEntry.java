/**
 *
 */
package org.flowvisor.config.convertor;
import java.util.HashSet;
import java.util.Set;

import org.flowvisor.events.ConfigUpdateEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * An abstract class entry in the Config
 *
 * @author capveg
 *
 */
public class ConfigEntry {
	String name;
	ConfigType type;
	Set<FVEventHandler> watchList; // never gets saved across sessions
	boolean persistent; // does this config entry get saved across FV sessions?

	public ConfigEntry(ConfigType type) {
		this.type = type;
		this.watchList = new HashSet<FVEventHandler>();
		// leave everything else undefined
	}

	public ConfigEntry() {
		// for java beans
	}

	public ConfigEntry(String name, ConfigType type) {
		this.name = name;
		this.type = type;
		this.persistent = true;
		this.watchList = new HashSet<FVEventHandler>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ConfigType getType() {
		return type;
	}

	public void setType(ConfigType type) {
		this.type = type;
	}

	/**
	 * Add this {@link FVEventHandler} to the list of things that get updated if
	 * this config entry changes
	 *
	 * @param eh
	 */
	public void watch(FVEventHandler eh) {
		watchList.add(eh);
	}

	/**
	 * Remove this {@link FVEventHandler} from the list of things that get
	 * updated on config change
	 *
	 * @param eh
	 */
	public void unwatch(FVEventHandler eh) {
		watchList.remove(eh);
	}

	/**
	 * Convert from string to the given value
	 *
	 * @param val
	 */
	public void setValue(String val) {
		// FIXME: find the java way of doing this
		throw new RuntimeException("need to override this... ");
	}

	/**
	 * Convert the node's value to a string
	 *
	 * @return
	 */
	public String[] getValue() {
		// FIXME: find the compile-time java way of ensuring that this gets
		// superclassed
		throw new RuntimeException("need to override this... ");
	}

	void sendUpdates(String fullPath) {
		for (FVEventHandler eh : watchList) {
			try {
				eh.handleEvent(new ConfigUpdateEvent(eh, fullPath));
			} catch (UnhandledEvent e) {
				FVLog.log(LogLevel.CRIT, eh,
						"Doesn't handle ConfigUpdateEvent but asked for them !?");
			}
		}
	}

	/**
	 * Does this config entry get saved across FV sessions? Default is yes.
	 *
	 * @return
	 */
	public boolean getPersistent() {
		return this.persistent;
	}

	/**
	 * Set whether this config entry gets saved across FV sessions
	 *
	 * @param val
	 */
	public void setPersistent(boolean val) {
		this.persistent = val;
	}
}