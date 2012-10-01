package org.flowvisor.config.convertor;
import java.io.PrintStream;

public class ConfigDumper implements ConfigIterator {

	PrintStream out;

	public ConfigDumper(PrintStream out) {
		this.out = out;
	}

	@Override
	public void visit(String path, ConfigEntry entry) {
		ConfigType type = entry.getType();
		String[] values = entry.getValue();
		int i;
		for (i = 0; i < values.length; i++)
			this.out.println(path + "::" + type + " : " + values[i]);
	}
}