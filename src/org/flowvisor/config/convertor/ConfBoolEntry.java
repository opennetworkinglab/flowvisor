/**
 *
 */
package org.flowvisor.config.convertor;
/**
 * @author capveg
 *
 */
public class ConfBoolEntry extends ConfigEntry {
	private boolean bool;

	public ConfBoolEntry() {
		super(ConfigType.BOOL);
	}

	public ConfBoolEntry(String name) {
		super(name, ConfigType.BOOL);
	}

	/**
	 * @return the bool
	 */
	public boolean getBool() {
		return bool;
	}

	/**
	 * @param bool
	 *            the bool to set
	 */
	public void setBool(boolean bool) {
		this.bool = bool;
	}

	@Override
	public String[] getValue() {
		String[] ret = new String[1];
		ret[0] = Boolean.toString(this.bool);
		return ret;
	}

	@Override
	public void setValue(String s) {
		this.bool = Boolean.valueOf(s);
	}

}
