package org.flowvisor.flows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.openflow.protocol.FVMatch;


/**
 * @author ash Implements FlowMap, in a federated fashion.
 * 
 * Each field of the flow entry is indexed in its own data
 * structure. This data structure is either a trie or a hashmap
 * depending on the type of the field.
 * 
 * @see FlowSpaceRuleStore for more detail.
 *
 */
public class FederatedFlowMap implements FlowMap, Cloneable {
	
	private FlowSpaceRuleStore fsrs = new FlowSpaceRuleStore();
	private Map<String, FlowEntry> namedFlowEntries = new HashMap<String, FlowEntry>();
	
	@Override
	public List<FlowEntry> matches(long dpid, FVMatch match) {
		return fsrs.match(dpid, match);
	}

	@Override
	public List<FlowIntersect> intersects(long dpid, FVMatch match) {
		return fsrs.intersect(dpid, match);
	}

	@Override
	public FlowEntry matches(long dpid, short inputPort, byte[] packetData) {
		FVMatch m = new FVMatch();
		m.loadFromPacket(packetData, inputPort);
		List<FlowEntry> list = matches(dpid, m);
		// if we match more than one thing, return the highest priority
		if (list.size() > 0)
			return list.get(0);
		return null;
	}

	@Override
	public void addRule(FlowEntry rule) {
		namedFlowEntries.put(rule.getName(), rule);
		fsrs.addRule(rule);
	}

	@Override
	public void removeRule(int id) throws FlowEntryNotFound {
		fsrs.removeRule(id);

	}

	@Override
	public int countRules() {
		return fsrs.getRuleCount();
	}

	@Override
	public SortedSet<FlowEntry> getRules() {
		return fsrs.getRules();
	}
	
	/**
	 * @param rules
	 *            the rules to set
	 *
	 *            DO NOT REMOVE! This breaks XML encoding/decoding
	 */
	public void setRules(SortedSet<FlowEntry> rules) {
		fsrs.setRules(rules);
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public FlowMap clone() {
		FederatedFlowMap flowMap = new FederatedFlowMap();
		for (FlowEntry flowEntry : this.getRules())
			flowMap.addRule(flowEntry.clone());
		return flowMap;
	}

	@Override
	public FlowMap instance() {
		return new FederatedFlowMap();
	}
	
	public type getType() {
		return type.FEDERATED;
	}

	@Override
	public FlowEntry findRuleByName(String name) throws FlowEntryNotFound {
		FlowEntry fe = namedFlowEntries.get(name);
		if (fe != null)
			return fe;
		throw new FlowEntryNotFound(name);
	}

	@Override
	public HashMap<Integer,ArrayList<Integer>> getPriorityRangeMap() {
		return (fsrs.getPrioSetRange());
		
	}

}
