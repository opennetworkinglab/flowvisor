/**
 *
 */
package org.flowvisor.config.convertor;

/**
 * @author capveg
 *
 */
public class ConfRealEntry extends ConfigEntry {
	double val;

	public ConfRealEntry(String name) {
		super(name, ConfigType.REAL);
	}

	public double getDouble() {
		return val;
	}

	public void setDouble(double val) {
		this.val = val;
	}

	@Override
	public String[] getValue() {
		String[] ret = new String[1];
		ret[0] = Double.toString(this.val);
		return ret;
	}

	@Override
	public void setValue(String s) {
		this.val = Double.parseDouble(s);
	}
}
