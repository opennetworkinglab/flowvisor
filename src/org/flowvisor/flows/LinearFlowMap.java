/**
 *
 */
package org.flowvisor.flows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.openflow.protocol.FVMatch;

/**
 * @author capveg Implements FlowMap, but in a slow and linear fashion.
 *
 *         (Hopefully Peyman will implement something faster :-)
 *
 */
public class LinearFlowMap implements FlowMap, Cloneable {
	
	SortedSet<FlowEntry> rules;

	public LinearFlowMap() {
		this.rules = new TreeSet<FlowEntry>();
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
		this.rules.add(rule);
	}

	@Override
	public int countRules() {
		return this.rules.size();
	}

	/**
	 * Lame: linear search to delete something in a sorted set ... but it's
	 * sorted by priority, not ID
	 */
	@Override
	public void removeRule(int id) throws FlowEntryNotFound {
		for (FlowEntry flowEntry : this.getRules())
			if (flowEntry.getId() == id) {
				this.rules.remove(flowEntry);
				return;
			}
		throw new FlowEntryNotFound(id);
	}

	/**
	 * Strip down the flow intersections to just the matching rules
	 */
	@Override
	public List<FlowEntry> matches(long dpid, FVMatch match) {
		List<FlowEntry> results = new LinkedList<FlowEntry>();
		for (FlowEntry rule : this.rules) {
			if (rule.matches(dpid, match).getMatchType() != MatchType.NONE)
				results.add(rule);
		}
		return results;
	}

	/**
	 * Step through each FlowEntry in order and match on it. If we get EQUALS or
	 * SUBSET, then stop.
	 *
	 * IF we get SUPERSET or INTERSECT, then keep going and merge the results.
	 */

	@Override
	public List<FlowIntersect> intersects(long dpid, FVMatch match) {

		List<FlowIntersect> results = new ArrayList<FlowIntersect>();
		FlowIntersect intersect;
		MatchType matchType;
		boolean needMerge = false;

		for (Iterator<FlowEntry> it = rules.iterator(); it.hasNext();) {
			FlowEntry rule = it.next();
			intersect = rule.matches(dpid, match);
			matchType = intersect.getMatchType();

			if (matchType == MatchType.NONE)
				continue;

			results.add(intersect);
			if ((matchType == MatchType.EQUAL)
					|| (matchType == MatchType.SUBSET))
				break;
			if ((matchType == MatchType.INTERSECT)
					|| (matchType == MatchType.SUPERSET))
				needMerge = true;
			else
				// else, wtf?
				throw new RuntimeException("Unknown MatchType = "
						+ intersect.getMatchType());
		}

		if (needMerge && (results.size() > 1))
			// BROKEN: needs to virtualize priorities
			// return priorityMerge(results); // expensive, avoid if possible
			return results;
		else
			return results;

	}

	/**
	 * Step through all of the partially computed results, compute the
	 * intersections and remove the intersections by priority.
	 *
	 * Could be O(n^2) in worst case, but we expect that intersections are rare
	 * (?)
	 *
	 *
	 * Uses the fact that the order of the list is also the priority order
	 *
	 * FIXME :: come back and make this faster
	 *
	 * @param mergeList
	 *            List of all FlowEntry's from matches(), including overlaps.
	 * @return A pruned list of just the non-completely-overlapping matches
	 */

	List<FlowIntersect> priorityMerge(List<FlowIntersect> mergeList) {
		List<FlowIntersect> results = new ArrayList<FlowIntersect>();
		boolean eclipsed;
		MatchType matchType;
		results.add(mergeList.get(0));
		mergeList.remove(0);

		for (FlowIntersect merge : mergeList) {
			eclipsed = false;
			for (FlowIntersect result : results) {
				/*
				 * is this new match eclipsed by previous entries?
				 *
				 * with each successive matches() call, the part that over laps
				 * result is removed, so that if a merge rule is not fully
				 * eclipsed by any one result, but is fully eclipsed by a sum of
				 * results, we will catch that to
				 */
				FlowIntersect tmpIntersect = merge.getFlowEntry().matches(
						result.getDpid(), result.getMatch());
				matchType = tmpIntersect.getMatchType();

				if ((matchType == MatchType.EQUAL)
						|| (matchType == MatchType.SUPERSET)) {
					eclipsed = true;
					break;
				} else if (matchType == MatchType.SUBSET) {
					merge = tmpIntersect; // then update with the intersection
				}
				// note: if matchtype == NONE, then tmpIntersect.getMatch() is
				// undefined
			}
			if (!eclipsed) // add this match to the list iff it's
				results.add(merge); // not complete eclipsed by something before
			// it
		}
		return results;
	}

	@Override
	public SortedSet<FlowEntry> getRules() {
		return this.rules;
	}

	/**
	 * @param rules
	 *            the rules to set
	 *
	 *            DO NOT REMOVE! This breaks XML encoding/decoding
	 */
	public void setRules(SortedSet<FlowEntry> rules) {
		this.rules = rules;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public FlowMap clone() {
		LinearFlowMap flowMap = new LinearFlowMap();
		for (FlowEntry flowEntry : this.rules)
			flowMap.addRule(flowEntry.clone());
		return flowMap;
	}

	@Override
	public FlowMap instance() {
		return new LinearFlowMap();
	}
	
	public type getType() {
		return type.LINEAR;
	}

	@Override
	public FlowEntry findRuleByName(String name) throws FlowEntryNotFound {
		for (FlowEntry fe : rules) 
			if (fe.getName().equals(name))
				return fe;
		throw new FlowEntryNotFound(name);
		
	}

	@Override
	public HashMap<Integer,ArrayList<Integer>> getPriorityRangeMap() {
		return null;
		
	}

}
