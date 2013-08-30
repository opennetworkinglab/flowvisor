/**
 *
 */
package org.flowvisor.flows;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flowvisor.config.BracketParse;
import org.flowvisor.config.Bracketable;
import org.flowvisor.config.FVConfig;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;

/**
 * @author capveg Holds data
 *         "IF packets match this RULE, THEN perform list of ACTIONS on it" In
 *         addition to normal openflow flow entry symantics, this flow entry
 *         also matches on dpid
 */
public class FlowEntry implements Comparable<FlowEntry>, Cloneable,
		Serializable, Bracketable<FlowEntry> {

	/**
	 *
	 */
	enum DefragmentPolicy  { DefragAll, DefragEven, DefragOdd};


	private static final long serialVersionUID = 1L;
	public static final long ALL_DPIDS = Long.MIN_VALUE;
	public static final String ALL_DPIDS_STR = "all_dpids";
	public static final int DefaultPriority = 32000;
	static int UNIQUE_FLOW_ID = -1;
	protected FVMatch ruleMatch;
	protected List<Integer> queue_ids = new LinkedList<Integer>();
	List<OFAction> actionsList;
	long dpid;
	int priority;
	int id;
	String name;
	// swap the policy after each defrag for version-ing
	static DefragmentPolicy CurrentDefragPolicy = DefragmentPolicy.DefragAll;


	/**
	 * IF switch is dpid and packet match's match, then perform action list
	 *
	 * @param dpid
	 *            switch's datapath id (from FeaturesReply) or ALL_DPIDS
	 * @param match
	 *            an openflow match structure
	 * @param actionsList
	 *            list of actions; empty list implies DROP
	 */
	public FlowEntry(String name, long dpid, FVMatch match, int id, int priority,
			List<OFAction> actionsList) {
		this.dpid = dpid;
		this.ruleMatch = match;
		this.id = id;
		this.actionsList = actionsList;
		this.priority = priority;
		this.name = name;
	}
	
	
	public FlowEntry(long dpid, FVMatch match, int id, int priority,
			List<OFAction> actionsList) {
		this(UUID.randomUUID().toString(), dpid, match, id, priority, actionsList);
	}
	
	

	public FlowEntry(long dpid, FVMatch match, int priority,
			List<OFAction> actionsList) {
		this(dpid, match, FlowEntry.getUniqueId(), priority, actionsList);
	}

	public FlowEntry(long dpid, FVMatch match, List<OFAction> actionsList) {
		this(dpid, match, FlowEntry.DefaultPriority, actionsList);
	}

	public FlowEntry(long dpid, FVMatch match, OFAction action) {
		this(dpid, match, (List<OFAction>) null);
		this.actionsList = new ArrayList<OFAction>();
		this.actionsList.add(action);
	}

	public FlowEntry(FVMatch match, List<OFAction> actionsList) {
		this(ALL_DPIDS, match, actionsList);
	}

	public FlowEntry(FVMatch match, OFAction action) {
		this(ALL_DPIDS, match, action);
	}

	public FlowEntry() {
	}

	public FlowEntry(long dpid2, FVMatch match, int priority2,
			List<OFAction> actions, List<Integer> queueId) {
		this(dpid2, match, priority2, actions);
		this.queue_ids = queueId;
	}

	public synchronized static int getUniqueId() {
		// find a unique entry if this is the first call or wrapped
		if (FlowEntry.UNIQUE_FLOW_ID == -1) {
			FlowEntry.UNIQUE_FLOW_ID = defragmentFlowIDS();
			if (FlowEntry.UNIQUE_FLOW_ID < 0) {
					FVLog.log(LogLevel.FATAL, null,
							"STILL unable to find a free flow ID "+
							"- FlowSpace > 2Billion?- dying");
					throw new RuntimeException(
							"failed to find free FlowEntry.iD "+
							" even after defrag");
			}
		}
		if (CurrentDefragPolicy == DefragmentPolicy.DefragAll)
			return FlowEntry.UNIQUE_FLOW_ID++;
		else
			return FlowEntry.UNIQUE_FLOW_ID +=2;
	}

	/**
	 * This function will renumber the unique IDs associated with each flowEntry.
	 *
	 * The 'policy' denotes whether we should renumber using all possible IDs,
	 * or only using the even numbers (0,2,4...) or only using odd numbers (1,3,5..)
	 *
	 * The idea is to use the LSB as a 1-bit "version" field for the IDs,
	 * so that if we have a race condition between renumbering and a delete flow ID=x
	 * operation, we want to make sure that flow ID=x does not accidentally delete
	 * a different, newly renumbered flow
	 *
	 * When called without options, we make a decision based on existing policy
	 *
	 * @param policy
	 * @return returns the highest assigned ID
	 */

	public static int defragmentFlowIDS() {
		switch(CurrentDefragPolicy) {
		case DefragAll:
			// NOOP
			break;
		case DefragEven:
			CurrentDefragPolicy = DefragmentPolicy.DefragOdd;
			break;
		case DefragOdd:
			CurrentDefragPolicy = DefragmentPolicy.DefragEven;
			break;
		}
		return defragmentFlowIDS(CurrentDefragPolicy);

	}

	public synchronized static int defragmentFlowIDS( DefragmentPolicy policy) {
		int neoId;
		int increment;

		FVLog.log(LogLevel.INFO, null, "defragmenting flowentry IDs using policy " + CurrentDefragPolicy);

		switch(policy) {
			case DefragAll:
				neoId = 0;
				increment = 1;
				break;
			case DefragEven:
				neoId = 0;
				increment=2;
				break;
			case DefragOdd:
				neoId = 1;
				increment = 2;
				break;
			default:
				throw new RuntimeException("unknown FlowID defrag policy: " + policy);
		}

		try {
			synchronized(FVConfig.class) {  // stop everyone from accessing flowmap for a second
				FlowMap map = FVConfig.getFlowSpaceFlowMap();
				switch (map.getType()) {
				case LINEAR:
					for (FlowEntry flowEntry : map.getRules()) {
						flowEntry.setId(neoId);
						neoId += increment;
					}
					return neoId;
				case FEDERATED:
					return neoId;
				}
				
			}
		} catch (RuntimeException e) {
			// no flowspace, nothing to conflict with!
			// needed for unittests
		}
		return neoId;
	}

	public long getDPID() {
		return this.dpid;
	}

	public void setDPID(long dpid) {
		this.dpid = dpid;
	}

	public List<OFAction> getActionsList() {
		return this.actionsList;
	}

	public void setActionsList(List<OFAction> actionsList) {
		this.actionsList = actionsList;
	}
	
	public void setQueueId(List<Integer> qids) {
		this.ruleMatch.setQueues(qids);
	}
	
	public List<Integer> getQueueId() {
		if (this.ruleMatch.getQueues() == null) {
			return new LinkedList<Integer>();
		} else 
			return this.ruleMatch.getQueues();
	}
	
	public void setForcedQueue(long qid) {
		this.ruleMatch.setForcedQueue(qid);
	}
	
	public long getForcedQueue() {
		return this.ruleMatch.getForcedQueue();
	}
	
	public boolean forcesEnqueue() {
		return !(this.getForcedQueue() == -1);
	}

	/**
	 * Describe the overlap between the passed (dpid, match) argument with this
	 * rule and return the information in a FlowIntersect structure
	 * <p>
	 * SUPERSET implies that the parameter matches a superset of this rule (
	 * rule < param ) <br>
	 * SUBSET that the parameter matches a subset of this rule ( rule > param )
	 * <br>
	 * EQUAL means they have perfect overlap (rule == param) <br>
	 * NONE means they do not have any overlap ( rule ^ param == 0 ) <br>
	 *
	 * General algorithm: step through each possible element of match (dpid,
	 * src_ip, etc.)
	 *
	 * NOTE: if you want to match a packet against a rule, first convert the
	 * packet to an FVMatch using match.loadFromPacket()
	 *
	 * @param dpid
	 *            switch's DPID
	 * @param match
	 * @return An FlowIntersect structure that describes the match. If
	 *         matchType!=NONE, then the flowIntersect.getMatch() describes the
	 *         intersection else flowIntersect.getMatch() is undefined
	 */
	public FlowIntersect matches(long dpid, FVMatch argMatch) {
		FlowIntersect intersection;
		intersection = new FlowIntersect(this.clone());

		int argWildcards = argMatch.getWildcards();
		int ruleWildcards = this.ruleMatch.getWildcards();

		/**
		 * NOTE: the logic here is protracted and error prone...but I can't
		 * think of a better way to do this... :-(
		 */

		// should maybe try to void this: kinda an inner loop
		// need this to be fresh state or unittests break
		FVMatch interMatch = intersection.getMatch();

		// test DPID: 1<<31 == unused wildcard field -- hack!
		intersection.setDpid(FlowTestOp.testFieldLong(intersection, 1 << 31,
				dpid == ALL_DPIDS ? 1 << 31 : 0,
				this.dpid == ALL_DPIDS ? 1 << 31 : 0, dpid, this.dpid));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test in_port
		interMatch.setInputPort(FlowTestOp.testFieldShort(intersection,
				FVMatch.OFPFW_IN_PORT, argWildcards, ruleWildcards,
				argMatch.getInputPort(), ruleMatch.getInputPort()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test ether_dst
		interMatch.setDataLayerDestination(FlowTestOp.testFieldByteArray(
				intersection, FVMatch.OFPFW_DL_DST, argWildcards,
				ruleWildcards, argMatch.getDataLayerDestination(),
				ruleMatch.getDataLayerDestination()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test ether_src
		interMatch.setDataLayerSource(FlowTestOp.testFieldByteArray(
				intersection, FVMatch.OFPFW_DL_SRC, argWildcards,
				ruleWildcards, argMatch.getDataLayerSource(),
				ruleMatch.getDataLayerSource()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test ether_type
		interMatch.setDataLayerType(FlowTestOp.testFieldShort(intersection,
				FVMatch.OFPFW_DL_TYPE, argWildcards, ruleWildcards,
				argMatch.getDataLayerType(), ruleMatch.getDataLayerType()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test vlan_type
		interMatch.setDataLayerVirtualLan(FlowTestOp.testFieldShort(
				intersection, FVMatch.OFPFW_DL_VLAN, argWildcards,
				ruleWildcards, argMatch.getDataLayerVirtualLan(),
				ruleMatch.getDataLayerVirtualLan()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test vlan_pcp
		interMatch.setDataLayerVirtualLanPriorityCodePoint(FlowTestOp
				.testFieldByte(intersection, FVMatch.OFPFW_DL_VLAN_PCP,
						argWildcards, ruleWildcards,
						argMatch.getDataLayerVirtualLanPriorityCodePoint(),
						ruleMatch.getDataLayerVirtualLanPriorityCodePoint()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test ip_dst
		interMatch.setNetworkDestination(FlowTestOp.testFieldMask(intersection,
				FVMatch.OFPFW_NW_DST_SHIFT,
				argMatch.getNetworkDestinationMaskLen(),
				ruleMatch.getNetworkDestinationMaskLen(),
				argMatch.getNetworkDestination(),
				ruleMatch.getNetworkDestination()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test ip_src
		interMatch.setNetworkSource(FlowTestOp.testFieldMask(intersection,
				FVMatch.OFPFW_NW_SRC_SHIFT, argMatch.getNetworkSourceMaskLen(),
				ruleMatch.getNetworkSourceMaskLen(),
				argMatch.getNetworkSource(), ruleMatch.getNetworkSource()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test ip_proto
		interMatch.setNetworkProtocol(FlowTestOp.testFieldByte(intersection,
				FVMatch.OFPFW_NW_PROTO, argWildcards, ruleWildcards,
				argMatch.getNetworkProtocol(), ruleMatch.getNetworkProtocol()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test ip_tos
		interMatch.setNetworkTypeOfService(FlowTestOp.testFieldByte(
				intersection, FVMatch.OFPFW_NW_TOS, argWildcards,
				ruleWildcards, argMatch.getNetworkTypeOfService(),
				ruleMatch.getNetworkTypeOfService()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test tp_src
		interMatch.setTransportSource(FlowTestOp.testFieldShort(intersection,
				FVMatch.OFPFW_TP_SRC, argWildcards, ruleWildcards,
				argMatch.getTransportSource(), ruleMatch.getTransportSource()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		// test tp_dst
		interMatch.setTransportDestination(FlowTestOp.testFieldShort(
				intersection, FVMatch.OFPFW_TP_DST, argWildcards,
				ruleWildcards, argMatch.getTransportDestination(),
				ruleMatch.getTransportDestination()));
		if (intersection.getMatchType() == MatchType.NONE)
			return intersection; // shortcut back

		/***
		 * DONE matching: if we got this far, there is at least an intersection
		 */

		if (intersection.maybeSubset && intersection.maybeSuperset) {
			intersection.setMatchType(MatchType.EQUAL);
		} else if (intersection.maybeSubset) {
			intersection.setMatchType(MatchType.SUBSET);
		} else if (intersection.maybeSuperset) {
			intersection.setMatchType(MatchType.SUPERSET);
		} else
			intersection.setMatchType(MatchType.INTERSECT);
		// wildcards was being set all of the way
		intersection.setMatch(interMatch);
		return intersection;

	}

	@Override
	public String toString() {
		return BracketParse.encode(this.toBracketMap());
	}

	/**
	 * Parse the output from this.toString() and return a matching FlowEntry
	 *
	 * Minimal error checking
	 *
	 * @param string
	 * @return an initialized flowentry
	 */
	public static FlowEntry fromString(String string) {
		Map<String, String> map = BracketParse.decode(string);
		FlowEntry flowEntry = new FlowEntry();
		return flowEntry.fromBacketMap(map);
	}

	public FVMatch getRuleMatch() {
		return ruleMatch;
	}

	public void setRuleMatch(FVMatch ruleMatch) {
		this.ruleMatch = ruleMatch;
	}
		
	public void setRuleMatch(OFMatch ruleMatch) {
		this.ruleMatch = new FVMatch(ruleMatch);
	}
		 

	public long getDpid() {
		return dpid;
	}

	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * @param priority
	 *            the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * @return the user-defined name
	 */
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((actionsList == null) ? 0 : actionsList.hashCode());
		result = prime * result + (int) (dpid ^ (dpid >>> 32));
		result = prime * result + id;
		result = prime * result + priority;
		result = prime * result
				+ ((ruleMatch == null) ? 0 : ruleMatch.hashCode());
		/*if (this.queue_ids != null)
			result = prime * result + queue_ids.hashCode();*/
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlowEntry other = (FlowEntry) obj;
		if (actionsList == null) {
			if (other.actionsList != null)
				return false;
		} else if (!actionsList.equals(other.actionsList))
			return false;
		if (dpid != other.dpid)
			return false;
		if (id != other.id)
			return false;
		if (priority != other.priority)
			return false;
		/*if (queue_ids.equals(other.queue_ids))
			return false;*/
		if (ruleMatch == null) {
			if (other.ruleMatch != null)
				return false;
		} else if (!ruleMatch.equals(other.ruleMatch))
			return false;
		return true;
	}

	@Override
	public int compareTo(FlowEntry other) {
		// sort on priority, tie break on IDs
		if (this.priority != other.priority)
			return other.priority - this.priority;
		if (this.id != other.id)
			return this.id - other.id;
		return this.hashCode() - other.hashCode();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public FlowEntry clone() {
		FlowEntry ret = new FlowEntry(this.name, this.dpid, this.ruleMatch.clone(),
				this.getId(), this.priority, actionsList); // fixme
		ret.setId(this.id);
		ret.setActionsList(new LinkedList<OFAction>(actionsList));
		/*FVLog.log(LogLevel.DEBUG, null, "cloning " + this.queue_ids + " match has " + this.ruleMatch.getQueues());
		ret.setQueueId(new LinkedList<Integer>(this.ruleMatch.getQueues()));*/
		return ret;
	}

	/**
	 * Does this slice have permissions 'perms' for this flowEntry
	 * Also checks if this action is specific to that slice by checking for sliceName.
	 * @param sliceName
	 * @param perms
	 * @return true == yes, false == no
	 */
	public boolean hasPermissions(String sliceName, int perms) {
		for (OFAction ofaction : this.actionsList) {
			if (ofaction instanceof SliceAction) {
				SliceAction sliceAction = (SliceAction) ofaction;
				if (sliceName.equals(sliceAction.getSliceName())) {
					if ((sliceAction.getSlicePerms() & perms) == perms)
						return true;
					else
						return false;
				}
			}
		}
		return false;
	}
	
	public String getSliceName() {
		String slice = new String();
		for (OFAction ofaction : this.actionsList) {
			if (ofaction instanceof SliceAction) {
				SliceAction sliceAction = (SliceAction) ofaction;
				slice = sliceAction.getSliceName();
			}
		}
		return slice;
	}
	
	public Map<String, Object> toMap() {
		HashMap<String, Object> map = new LinkedHashMap<String, Object>();
		if (dpid == ALL_DPIDS)
			map.put("dpid", ALL_DPIDS_STR);
		else
			map.put("dpid", FlowSpaceUtil.dpidToString(dpid));
		if (this.ruleMatch != null)
			map.put("match", this.ruleMatch.toMap());
		map.put("actionsList", actionsList);
		map.put("priority", String.valueOf(this.priority));
		return map;
	}

	@Override
	public Map<String, String> toBracketMap() {
		HashMap<String, String> map = new LinkedHashMap<String, String>();
		map.put(BracketParse.OBJECTNAME, "FlowEntry");
		if (dpid == ALL_DPIDS)
			map.put("dpid", ALL_DPIDS_STR);
		else
			map.put("dpid", FlowSpaceUtil.dpidToString(dpid));
		if (this.ruleMatch != null)
			map.put("ruleMatch", this.ruleMatch.toString());
		map.put("actionsList", FlowSpaceUtil.toString(actionsList));
		map.put("id", String.valueOf(this.id));
		map.put("priority", String.valueOf(this.priority));
		return map;
	}

	@Override
	public FlowEntry fromBacketMap(Map<String, String> map) {
		List<OFAction> actionsList = new ArrayList<OFAction>();
		long dpid;
		FVMatch rule;
		int id;
		int priority;
		if ((map == null)
				|| (!map.get(BracketParse.OBJECTNAME).equals("FlowEntry")))
			throw new IllegalArgumentException(
					"missing expected FlowEntry, got '" + map + "'");
		if (!map.containsKey("dpid"))
			throw new IllegalArgumentException(
					"missing expected key dpid, got '" + map + "'");
		if (map.containsKey("id"))
			id = Integer.valueOf(map.get("id"));
		else
			throw new IllegalArgumentException("missing expected key id, got '"
					+ map + "'");
		if (map.containsKey("priority"))
			priority = Integer.valueOf(map.get("priority"));
		else
			throw new IllegalArgumentException(
					"missing expected key priority, got '" + map + "'");
		
		
		int i;
		// translate dpid
		if (map.get("dpid").equals(ALL_DPIDS_STR))
			dpid = ALL_DPIDS;
		else
			dpid = HexString.toLong(map.get("dpid"));
		rule = new FVMatch();
		rule.fromString(map.get("ruleMatch"));
		String[] actions = map.get("actionsList").split(",");
		for (i = 0; i < actions.length; i++)
			if (!actions[i].equals(""))
				actionsList.add(SliceAction.fromString(actions[i]));
		this.setActionsList(actionsList);
		this.setDPID(dpid);
		this.setRuleMatch(rule);
		this.setId(id);
		this.setPriority(priority);

		return this;
	}

	
}
