package org.flowvisor.api;

import org.flowvisor.flows.FlowEntry;

public class FlowSpaceChangeRequest {

	FlowEntry entry;
	String changeType;

	protected FlowSpaceChangeRequest(){
		// For Serializaton
	}

	public FlowSpaceChangeRequest(FlowEntry entry, String changeType){
		this.entry = entry;
		this.changeType = changeType;
	}

	public FlowEntry getEntry() {
		return entry;
	}

	public String getChangeType() {
		return changeType;
	}
}
