package org.flowvisor.config.convertor;

import java.util.LinkedList;
import java.util.List;

public class ConfigPrinter implements ConfigIterator {
	String prefix;
	List<String> out;

	public ConfigPrinter(String prefix) {
		this.prefix = prefix;
		this.out = new LinkedList<String>();
	}

	@Override
	public void visit(String path, ConfigEntry entry) {
		ConfigType type = entry.getType();
		String[] values = entry.getValue();
		int i;
		for (i = 0; i < values.length; i++)
			this.out.add(prefix + path + "::" + type + " : " + values[i]);
	}

	/**
	 * @return the out
	 */
	public List<String> getOut() {
		return out;
	}

	/**
	 * @param out
	 *            the out to set
	 */
	public void setOut(List<String> out) {
		this.out = out;
	}
}
