package org.flowvisor.config.convertor;
/**
 * Interface to walk the config directory
 *
 * @author capveg
 *
 */

public interface ConfigIterator {
	/**
	 * Called on each node in the subdirectory
	 *
	 * @param path
	 *            The path to this entry, e.g.., "slices.alice"
	 * @param entry
	 *            The actual entry
	 */
	public void visit(String path, ConfigEntry entry);
}
