package org.flowvisor.flows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;

import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.openflow.protocol.FVMatch;


/**
 * Interface for computing intersections in flowspace.
 *
 * FlowSpace rules are described via a strict priority list, e.g.,
 *
 * RULE 0: FlowEntry 0: if , then ACTIONS LIST 0 RULE 1: FlowEntry 1: if RULE 1,
 * then ACTIONS LIST 1 . . . RULE N: FlowEntry N: ...
 *
 * FlowEntry's are allowed to overlap in FlowSpace, but intersections are
 * resolved by priority, and a single point in flowspace only *matches* one
 * rule. For example, if FlowEntry 1 affects all TCP traffic and FlowEntry 2
 * affects all IP traffic in prefix 192.168.0.0/16, then:
 *
 * TCP traffic outside of 192.168.0.0/16 will match Rule 1 (no intersection)
 * Non-TCP IP traffic in 192.168.0.0/16 will match Rule 2 (no intersection) TCP
 * traffic in 192.168.0.0/16 will match Rule 1 (intersection, but resolved by
 * priority)
 *
 * NOTE: this behavior emulates that of a TCAM
 *
 * Thus, the matches on a single packet only returns one FlowEntry.
 *
 * However, matches on a region (i.e., OFMatch with wildcards) can return
 * multiple entries
 *
 *
 * @author capveg
 *
 */

public interface FlowMap {
	
	
	public enum type {
		LINEAR("linear"),
		FEDERATED("federated");
		
		private String text;

		  type(String text) {
		    this.text = text;
		  }

		  public String getText() {
		    return this.text;
		  }

		  public static type fromString(String text) {
		    if (text != null) {
		      for (type b : type.values()) {
		        if (text.equalsIgnoreCase(b.text)) {
		          return b;
		        }
		      }
		    }
		    throw new IllegalArgumentException("Unknown flowmap type " + text);
		  }
	}

	/**
	 * Given a dpid and a match, return a list of all of the FlowEntry's that
	 * overlap this match.
	 *
	 * @param dpid
	 *            Identifier for switch
	 * @param match
	 *            Region of interest, as described by an OFMatch structure
	 * @return List of FlowEntries that overlap this region (resolved by
	 *         priority). Valid but empty list on no match.
	 */
	public List<FlowEntry> matches(long dpid, FVMatch match);

	/**
	 * Return the same information as matches(), except with more explicit
	 * information on the type of match (SUBSET, SUPERSET, etc) of each
	 * FlowEntry and the extent of the overlap, i.e., the actual intersection
	 *
	 * Used for computing logical intersections when rewriting flowmods
	 *
	 * @param dpid
	 *            Identifier for switch
	 * @param matchRegion
	 *            of interest, as described by an OFMatch structure
	 * @return List of overlapping regions (resolved by priority)
	 */
	public List<FlowIntersect> intersects(long dpid, FVMatch match);

	/**
	 * Given a dpid and a packet, return the rule that matches this packet.
	 *
	 * @param dpid
	 *            Identifier for switch
	 * @param inputPort
	 *            The port the packet came in on
	 * @param packetData
	 *            Raw packet data, starting from l2 ethernet headers
	 * @return The FlowEntry that this matches to. Can be null if no match.
	 */
	public FlowEntry matches(long dpid, short inputPort, byte[] packetData);

	/**
	 * Add a new rule into the map. Rules are sorted by their priority: highest
	 * number has highest priority
	 *
	 *
	 * @param rule
	 *            A valid FlowEntry
	 */
	public void addRule(FlowEntry rule);

	/**
	 * Remove the rule listed at position
	 *
	 * @param id
	 *            the unique FlowEntry id identifying this rule
	 */
	public void removeRule(int id) throws FlowEntryNotFound;

	/**
	 * Return the number of rules in the list
	 *
	 * @return zero if empty, number of rules otherwise
	 */
	public int countRules();

	/**
	 * Returns the current FlowMap rules in List<> form
	 *
	 * Add() directly to this list will not necessarily add() to the flowmap.
	 * 
	 * update: for a federated flow map, adding to this list will NOT add to
	 * the flowmap. So don't do it, and use addRule!
	 *
	 * @return list of FlowEntry's
	 */

	public SortedSet<FlowEntry> getRules();
	
	public void setRules(SortedSet<FlowEntry> r);
	
	
	/*
	 * Finds a rule by name.
	 */
	
	public FlowEntry findRuleByName(String name) throws FlowEntryNotFound;

	/**
	 * Clone()
	 *
	 * @return Return a deep copy of the flowmap
	 */
	public FlowMap clone();
	
	/**
	 * Returns an empty instance of this flowmap.
	 * 
	 * @return a valid non-null instance
	 */
	
	public FlowMap instance();
	
	public type getType();
	
	public HashMap<Integer,ArrayList<Integer>> getPriorityRangeMap();
}
