/**
 *
 */
package org.flowvisor.config.convertor;

/**
 * @author capveg
 *
 */
public enum ConfigType {

	DIR(ConfDirEntry.class), // directory
	STR(ConfStrEntry.class), // string
	INT(ConfIntEntry.class), // integer
	REAL(ConfRealEntry.class), // real
	FLOWMAP(ConfFlowMapEntry.class), // flowmap
	BOOL(ConfBoolEntry.class), // boolean
	// FLOWENTRY(ConfigEntry.class)
	; // flow rule/flow entry

	protected Class<? extends ConfigEntry> clazz;

	ConfigType(Class<? extends ConfigEntry> clazz) {
		this.clazz = clazz;
	}

	public Class<? extends ConfigEntry> toClass() {
		return this.clazz;
	}
}
