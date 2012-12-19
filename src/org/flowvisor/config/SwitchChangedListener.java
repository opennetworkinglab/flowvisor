package org.flowvisor.config;

import java.util.HashMap;

public interface SwitchChangedListener extends ChangedListener {
	public void setFloodPerm(String in);
	public void setFlowModLimit(HashMap<String, Object> in);
	public void setRateLimit(HashMap<String, Object> in);
	
}
