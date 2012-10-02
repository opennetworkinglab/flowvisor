package org.flowvisor.config;


public interface SliceChangedListener extends ChangedListener {
	public void setLLDP(Boolean in);
	public void setDropPolicy(String in);
	public void setControllerHost(String in);
	public void setControllerPort(Integer in);
	public void setFlowModLimit(Integer in);
}
