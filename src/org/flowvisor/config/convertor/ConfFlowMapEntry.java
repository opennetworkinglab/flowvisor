/**
 *
 */
package org.flowvisor.config.convertor;
import org.flowvisor.flows.*;

/**
 * @author capveg
 *
 */
public class ConfFlowMapEntry extends ConfigEntry {
	public FlowMap flowMap;

	public ConfFlowMapEntry(String name) {
		super(name, ConfigType.FLOWMAP);
	}

	public ConfFlowMapEntry() {
		super(ConfigType.FLOWMAP);
	}

	public FlowMap getFlowMap() {
		return flowMap;
	}

	public void setFlowMap(FlowMap flowMap) {
		this.flowMap = flowMap;
	}

	@Override
	public String[] getValue() {
		String[] ret = new String[flowMap.countRules()];
		int i = 0;

		for (FlowEntry rule : flowMap.getRules()) {
			ret[i] = "" + i + " " + rule.toString();
			i++;
		}
		return ret;
	}

	/**
	 * This will get called iteratively for each flow entry in this flow map
	 */
	@Override
	public void setValue(String val) {
		String[] tokens = val.split(" ");
		if (tokens.length < 2)
			throw new IllegalArgumentException(
					"Expected <rule num> <flow entry> but got '" + val + "'");
		FlowEntry rule = FlowEntry.fromString(tokens[1]);
		flowMap.addRule(rule);
	}
}
