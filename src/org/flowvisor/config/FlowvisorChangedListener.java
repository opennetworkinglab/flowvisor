package org.flowvisor.config;

public interface FlowvisorChangedListener extends ChangedListener {
	public void setFlowTracking(Boolean in);
	public void setStatsDescHack(Boolean in);
	public void setFloodPerm(String in);
}
