package org.flowvisor.config;

import org.flowvisor.flows.FlowMap;

/**
 * A listener for a change to the flowmap. 
 * 
 * @author ash
 *
 */
public interface FlowMapChangedListener extends ChangedListener {
	
	/**
	 * Callback method for the flowmap change.
	 * 
	 * @param in the changed flowmap.
	 */
	public void flowMapChanged (FlowMap in);
}
