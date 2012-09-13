/**
 *
 */
package org.flowvisor.config.convertor;

/**
 * @author capveg
 *
 */
public class ConfStrEntry extends ConfigEntry {
	String val;

	public ConfStrEntry(String name) {
		super(name, ConfigType.STR);
	}

	public ConfStrEntry() {
		super(ConfigType.STR);
	}

	@Override
	public String[] getValue() {
		String ret[] = new String[1];
		ret[0] = this.val;
		return ret;
	}

	@Override
	public void setValue(String val) {
		this.val = val;
	}

	public String getString() {
		return val;
	}

	public void setString(String val) {
		this.val = val;
	}
}
