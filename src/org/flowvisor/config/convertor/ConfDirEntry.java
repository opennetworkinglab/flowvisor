/**
 *
 */
package org.flowvisor.config.convertor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * A directory in the config hierarchy. Does not support multiple nodes with the
 * same name; they will overwrite
 *
 * @author capveg
 *
 */
public class ConfDirEntry extends ConfigEntry {
	HashMap<String, ConfigEntry> entries;

	/**
	 * A directory entry in the Config Hierarchy
	 *
	 * @param name
	 */
	public ConfDirEntry(String name) {
		super(name, ConfigType.DIR);
		this.entries = new HashMap<String, ConfigEntry>();
	}

	public ConfDirEntry() {
		super(ConfigType.DIR);
	}

	public HashMap<String, ConfigEntry> getEntries() {
		return entries;
	}

	public void setEntries(HashMap<String, ConfigEntry> entries) {
		this.entries = entries;
	}

	/**
	 * Lookup an entry in this directory
	 *
	 * @param name
	 *            entry name
	 * @return
	 */

	public ConfigEntry lookup(String name) {
		return entries.get(name);
	}

	/**
	 * Add an entry to this directory
	 *
	 * @param entry
	 */
	public void add(ConfigEntry entry) {
		entries.put(entry.getName(), entry);
	}

	/**
	 * Remove an entry from this direclty
	 *
	 * @param name
	 *            name of entry
	 */
	public void remove(String name) {
		entries.remove(name);
	}

	/**
	 * Return a list of entries for this node
	 *
	 * @return
	 */
	public List<String> list() {
		return new ArrayList<String>(entries.keySet());
	}

	public Collection<ConfigEntry> listEntries() {
		return entries.values();
	}

	@Override
	public String[] getValue() {
		return (String[]) entries.keySet().toArray(new String[entries.size()]);
	}
}
