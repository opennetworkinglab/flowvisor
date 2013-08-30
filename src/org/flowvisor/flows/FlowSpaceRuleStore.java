package org.flowvisor.flows;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.exceptions.NoMatch;
import org.flowvisor.exceptions.UnknownMatchField;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;
import org.openflow.util.U16;


/**
 * This is the FlowSpaceRuleStore, it is the supporting structure for federated
 * flow map. We store rule by breaking them up into their fields and storing
 * each field independently.
 * 
 * During a search (ie. a match or intersect), we represent the remaining valid
 * rules using a bitset. Thus, enabling us to simply intersect bitsets obtained
 * by searching for each field. Also, this allows to quickly reject a potential
 * rule or find all matching rules quickly without having to go through all the
 * flowmap.
 * 
 * At the end of a match, this flowmap returns a set ordered by flowentry
 * priority.
 * 
 * @author ash
 * 
 */

public class FlowSpaceRuleStore {

	/**
	 * All these match structures bitsets representing the slice id each rule
	 * points to.
	 * 
	 * These bitset are then intersected and and prioritized. If the bit set
	 * contains any set bit after this process then we have a match.
	 * 
	 */

	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	/**
	 * ports
	 */
	private HashMap<Short, BitSet> port = new HashMap<Short, BitSet>();

	/**
	 * Vlan contains both vid and pcp keys. An attempt at saving some memory.
	 */
	private HashMap<Integer, BitSet> vlan = new HashMap<Integer, BitSet>();

	/**
	 * ethernet types
	 */
	private HashMap<Short, BitSet> dl_type = new HashMap<Short, BitSet>();

	/**
	 * nw contains both proto and tos keys. An attempt at saving some memory.
	 */
	private HashMap<Short, BitSet> nw = new HashMap<Short, BitSet>();

	/**
	 * tp contains both tp_src and tp_dst keys. An attempt at saving some
	 * memory.
	 */
	private HashMap<Integer, BitSet> tp = new HashMap<Integer, BitSet>();

	private HashMap<Long, BitSet> dpids = new HashMap<Long, BitSet>();
			
	private HashMap<Long, BitSet> dl_src = new HashMap<Long, BitSet>();
			
	private HashMap<Long, BitSet> dl_dst = new HashMap<Long, BitSet>();
			

	/**
	 * We need to store all the rules we have seen so far.
	 * 
	 * This saves us time when we get a wildcarded flowmod.
	 */
	private BitSet allRules = new BitSet();

	private BitSet vlan_ignore = new BitSet();

	/**
	 * This sorted structure maintains bitsets representing the rules for every
	 * priority. Makes it easy to return the rule with the highest priority.
	 */
	private TreeMap<Integer, BitSet> prioSet = new TreeMap<Integer, BitSet>();

	/**
	 * Keep track of the number of rules we have so far.
	 * 
	 * No need to synchronize this operation as we only have one thread working
	 * here.
	 */
	private int ruleCount = 0;

	public static long ANY_DPID = FlowEntry.ALL_DPIDS;
	public static short ANY_IN_PORT = 0;// OFPort.OFPP_NONE.getValue();
	/*
	 * ash: defining these here because I can't find them in OpenFlowj
	 */
	public static short ANY_VLAN_ID = ((short) 0xFFFF);
	public static short SOME_VLAN_ID = ((short) 0xFFFE);

	public static byte ANY_VLAN_PCP = -1;
	public static byte ANY_NW_PROTO_TOS = -1;
	public static long ANY_MAC = -1;
	public static short ANY_ETHER = -1;
	public static short ANY_NW = -1;
	public static short ANY_TP = -1;
	
	
	final static short IPTYPE = 0x800;
	final static short ARPTYPE = 0x806;
	
	final static short ICMP = 1;
	final static short TCP = 6;
	final static short UDP = 17;
	final static short SCTP = 132;

	/**
	 * This is used so that we can delete the rules if need be.
	 * 
	 * Little annoying that we must maintain all the flowrule details, but I
	 * fear we do not have a choice.
	 */
	private HashMap<Integer, FlowEntry> rules = new HashMap<Integer, FlowEntry>();

	/**
	 * Adds a rule to the flowmap. It does so by exploding the rule into its
	 * fields and storing each field into its independent structure.
	 * 
	 * @param rule
	 *            - the rule which will be added to the flowmap
	 * 
	 * @return
	 */
	public void addRule(FlowEntry rule) {
		ruleCount++;
		allRules.set(rule.getId());
		BitSet flowRuleSet = getFlowRuleSet(rule);

		add(dpids, rule.dpid, flowRuleSet);

		add(port,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_IN_PORT) != 0 ? ANY_IN_PORT
						: rule.getRuleMatch().getInputPort(), flowRuleSet);

		short vid = rule.getRuleMatch().getDataLayerVirtualLan();
		int vpcp = rule.getRuleMatch()
				.getDataLayerVirtualLanPriorityCodePoint();

		/*
		 * Openflowj sets unspecified fields to zero.
		 */
		if (vid != 0 || vpcp != 0) {
			vlan_ignore.set(rule.getId());
		}

		add(vlan,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_DL_VLAN) != 0 ? (int) ANY_VLAN_ID
						: (int) rule.getRuleMatch().getDataLayerVirtualLan(),
				flowRuleSet);

		add(vlan,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_DL_VLAN_PCP) != 0 ? ANY_VLAN_PCP << 16
						: rule.getRuleMatch()
								.getDataLayerVirtualLanPriorityCodePoint() << 16,
				flowRuleSet);

		add(dl_type,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_DL_TYPE) != 0 ? ANY_ETHER
						: rule.getRuleMatch().getDataLayerType(), flowRuleSet);

		add(nw,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_NW_PROTO) != 0 ? ANY_NW_PROTO_TOS
						: (short) rule.getRuleMatch().getNetworkProtocol(),
				flowRuleSet);

		add(nw,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_NW_TOS) != 0 ? (short) (ANY_NW_PROTO_TOS << 8)
						: (short) (rule.getRuleMatch()
								.getNetworkTypeOfService() << 8), flowRuleSet);

		add(tp,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_TP_SRC) != 0 ? ANY_TP
						: (int) rule.getRuleMatch().getTransportSource(),
				flowRuleSet);

		add(tp,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_TP_DST) != 0 ? ANY_TP << 16
						: rule.getRuleMatch().getTransportDestination() << 16,
				flowRuleSet);

		add(dl_src,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_DL_SRC) != 0 ? ANY_MAC
						: FVMatch.toLong(rule.getRuleMatch()
								.getDataLayerSource()), flowRuleSet);
		add(dl_dst,
				(rule.getRuleMatch().getWildcards() & FVMatch.OFPFW_DL_DST) != 0 ? ANY_MAC
						: FVMatch.toLong(rule.getRuleMatch()
								.getDataLayerDestination()), flowRuleSet);

		add(prioSet, rule.getPriority(), flowRuleSet);
		FVLog.log(LogLevel.DEBUG, null, "prioSet:",prioSet);
		rules.put(rule.getId(), rule);
		getPrioSetRange();
	}

	public synchronized HashMap<Integer,ArrayList<Integer>> getPrioSetRange() {
		FVLog.log(LogLevel.DEBUG,null,"prioSet: ",prioSet);
		Set<Integer> prios = prioSet.keySet();
		Integer pSize = prioSet.size();
		
		Integer range = 65536/pSize;

		HashMap<Integer,ArrayList<Integer>> prioRange = new HashMap<Integer,ArrayList<Integer>>();
		Short index = 0;
		for(Integer prio: prios){
			index++;
			Integer rangeEnd = (index * range) - 1;
			Integer rangeStart = rangeEnd - range + 1;
			ArrayList<Integer> pRange = new ArrayList<Integer>();
			pRange.add(rangeStart);
			pRange.add(rangeEnd);
			prioRange.put(prio, pRange);
		}
		FVLog.log(LogLevel.DEBUG,null,"prioRange: ",prioRange);
		return prioRange;
	}

	/**
	 * Converse of addRule. Since we know all the rules ever stored in this
	 * flowmap we simply retrieve the rule using the id provided and remove each
	 * of its fields from the underlying structures.
	 * 
	 * @param id
	 *            - the id of the rule to remove
	 * 
	 * @throws FlowEntryNotFound
	 *             - if this id is unknown. Could indicate that rule has already
	 *             been removed.
	 * 
	 * @return
	 * 
	 */

	public void removeRule(int id) throws FlowEntryNotFound {
		ruleCount--;

		if (!rules.containsKey(id))
			throw new FlowEntryNotFound(id);

		FlowEntry rule = rules.get(id);

		BitSet flowRuleSet = getFlowRuleSet(rule);

		remove(port, rule.getRuleMatch().getInputPort(), flowRuleSet);
		remove(dpids, rule.dpid, flowRuleSet);
		remove(vlan, (int) rule.getRuleMatch().getDataLayerVirtualLan(),
				flowRuleSet);
		remove(vlan, rule.getRuleMatch()
				.getDataLayerVirtualLanPriorityCodePoint() << 16, flowRuleSet);
		remove(dl_type, rule.getRuleMatch().getDataLayerType(), flowRuleSet);

		remove(nw, (short) rule.getRuleMatch().getNetworkProtocol(),
				flowRuleSet);
		remove(nw,
				(short) (rule.getRuleMatch().getNetworkTypeOfService() << 8),
				flowRuleSet);

		remove(tp, (int) rule.getRuleMatch().getTransportSource(), flowRuleSet);
		remove(tp, rule.getRuleMatch().getTransportDestination() << 16,
				flowRuleSet);

		remove(dl_src,
				FVMatch.toLong(rule.getRuleMatch().getDataLayerSource()),
				flowRuleSet);
		remove(dl_dst,
				FVMatch.toLong(rule.getRuleMatch().getDataLayerDestination()),
				flowRuleSet);

		remove(prioSet, rule.getPriority(), flowRuleSet);

		vlan_ignore.clear(rule.getId());
		rules.remove(rule.getId());
		allRules.andNot(flowRuleSet);

	}

	/**
	 * This is called when a controller issues a FlowMod. The goal here is to
	 * rewrite flowmods so that they guarantee traffic isolation between slices.
	 * A flowmod is rewritten following these rules:
	 * 
	 * 1. Its field is wildcarded, then for every potentially matching rule we
	 * rewrite the field with the value in the rule.
	 * 
	 * 2. Its field is specified, then leave it untouched, but use it for
	 * matching purposes.
	 * 
	 * 3. We have a potential partial match: 3.1. The field and the flowrule
	 * field do not overlap, return no match. 3.2. The fields overlap, return
	 * the smallest possible intersection of the two.
	 * 
	 * Currently rule 3 only applies to IP addresses, but this could change in
	 * the future.
	 * 
	 * @param dpid
	 *            - The dpid this flowmod is directed to, could be wildcarded.
	 * @param match
	 *            - The set of field modifications issued by this flowmod.
	 * 
	 * @return a list of intersections (ie. flowmod rewrites) created by the
	 *         flowmod issued from the controller.
	 */

	public List<FlowIntersect> intersect(long dpid, FVMatch match) {
		FVLog.log(LogLevel.DEBUG,null,"dpid: ", dpid, " match: ",match.toString());
		BitSet set = new BitSet();
		normalize(match);
		int wildcards = match.getWildcards();
		TreeSet<FlowIntersect> ret = new TreeSet<FlowIntersect>();
		HashMap<Integer, FlowIntersect> intersections = new HashMap<Integer, FlowIntersect>();
		HashMap<Integer, Pair<Boolean, BitSet>> rewrites = new HashMap<Integer, Pair<Boolean, BitSet>>();
		
		
		set.or(allRules);

		try {

			testEmpty(set, dpids, dpid, FlowEntry.ALL_DPIDS, wildcards,0);
			
			rewrites.put(
					FVMatch.OFPFW_IN_PORT,
					new Pair<Boolean, BitSet>(testEmpty(set, port,
							match.getInputPort(), ANY_IN_PORT, wildcards,
							FVMatch.OFPFW_IN_PORT), set));
			
			rewrites.put(
					FVMatch.OFPFW_DL_SRC,
					new Pair<Boolean, BitSet>(testEmpty(set, dl_src,
							FVMatch.toLong(match.getDataLayerSource()),
							ANY_MAC, wildcards, FVMatch.OFPFW_DL_SRC), set));

			rewrites.put(
					FVMatch.OFPFW_DL_DST,
					new Pair<Boolean, BitSet>(testEmpty(set, dl_dst,
							FVMatch.toLong(match.getDataLayerDestination()),
							ANY_MAC, wildcards, FVMatch.OFPFW_DL_DST), set));

			rewrites.put(
					FVMatch.OFPFW_DL_VLAN,
					new Pair<Boolean, BitSet>(
							testEmpty(set, vlan,
									(int) match.getDataLayerVirtualLan(),
									(int) ANY_VLAN_ID, wildcards,
									FVMatch.OFPFW_DL_VLAN), set));

			rewrites.put(
					FVMatch.OFPFW_DL_VLAN_PCP,
					new Pair<Boolean, BitSet>(
							testEmpty(
									set,
									vlan,
									match.getDataLayerVirtualLanPriorityCodePoint() << 16,
									ANY_VLAN_PCP << 16, wildcards,
									FVMatch.OFPFW_DL_VLAN_PCP), set));
			rewrites.put(
					FVMatch.OFPFW_DL_TYPE,
					new Pair<Boolean, BitSet>(testEmpty(set, dl_type,
							match.getDataLayerType(), ANY_ETHER, wildcards,
							FVMatch.OFPFW_DL_TYPE), set));

			rewrites.put(
					FVMatch.OFPFW_NW_PROTO,
					new Pair<Boolean, BitSet>(testEmpty(set, nw,
							(short) match.getNetworkProtocol(),
							(short) ANY_NW_PROTO_TOS, wildcards,
							FVMatch.OFPFW_NW_PROTO), set));

			rewrites.put(
					FVMatch.OFPFW_NW_TOS,
					new Pair<Boolean, BitSet>(testEmpty(set, nw,
							(short) (match.getNetworkTypeOfService() << 8),
							(short) (ANY_NW_PROTO_TOS << 8), wildcards,
							FVMatch.OFPFW_NW_TOS), set));

			rewrites.put(
					FVMatch.OFPFW_TP_SRC,
					new Pair<Boolean, BitSet>(testEmpty(set, tp,
							(int) match.getTransportSource(), (int) ANY_TP,
							wildcards, FVMatch.OFPFW_TP_SRC), set));

			rewrites.put(
					FVMatch.OFPFW_TP_DST,
					new Pair<Boolean, BitSet>(testEmpty(set, tp,
							match.getTransportDestination() << 16,
							ANY_TP << 16, wildcards, FVMatch.OFPFW_TP_DST), set));

			int field = 0;
			boolean rewrite = false;
			BitSet inters = null;
			FlowIntersect flow = null;
			FlowEntry rule = null;
						
			for (Entry<Integer, Pair<Boolean, BitSet>> entry : rewrites
					.entrySet()) {
				field = entry.getKey();
				rewrite = entry.getValue().getFirst();
				inters = entry.getValue().getSecond();
				FVLog.log(LogLevel.DEBUG,null,"Rule ids which intersect: ",inters.toString());
				for (int i = inters.nextSetBit(0); i >= 0; i = inters
						.nextSetBit(i + 1)) {
					rule = rules.get(i).clone();
					flow = getIntersect(rule, intersections);
					
					if (!rewrite) {
						rule.setRuleMatch(match);
						setField(flow, rule.getRuleMatch(), field);
					} 

					FVMatch ruleMatch = rules.get(i).getRuleMatch();
					FlowIntersect inter = flow;
					FVMatch interMatch = inter.getMatch();
					interMatch.setNetworkDestination(testIP(inter,
							FVMatch.OFPFW_NW_DST_SHIFT,
							match.getNetworkDestinationMaskLen(),
							ruleMatch.getNetworkDestinationMaskLen(),
							match.getNetworkDestination(),
							ruleMatch.getNetworkDestination()));
					if (inter.getMatchType() == MatchType.NONE)
						continue;

					interMatch.setNetworkSource(testIP(inter,
							FVMatch.OFPFW_NW_SRC_SHIFT,
							match.getNetworkSourceMaskLen(),
							ruleMatch.getNetworkSourceMaskLen(),
							match.getNetworkSource(),
							ruleMatch.getNetworkSource()));
					if (inter.getMatchType() == MatchType.NONE)
						continue;
					intersections.put(flow.getFlowEntry().getId(), flow);
				}
			}

			/*
			 * Need to resolve intersection by priority.
			 * 
			 * In the worst case this will be O(n) where n is the number of
			 * rules. But in average we will only have a small subset of the
			 * rules. Right??
			 */
			ret.addAll(intersections.values());
			FVLog.log(LogLevel.DEBUG,null,"Intersections: ",intersections);

		} catch (NoMatch e) {
			FVLog.log(LogLevel.FATAL, null, "Failed to intersect flow mod "
					+ match);
			return new ArrayList<FlowIntersect>(ret);
		} catch (UnknownMatchField umf) {
			FVLog.log(LogLevel.FATAL, null, umf.getMessage());
		}

		return new ArrayList<FlowIntersect>(ret);
	}

	/**
	 *  Protocol-specific fields within ofp_match will be ignored within
   	 *	a single table when the corresponding protocol is not specified in the
     *  match.  The IP header and transport header fields
     *  will be ignored unless the Ethertype is specified as either IPv4 or
     *  ARP. The tp_src and tp_dst fields will be ignored unless the network
     *  protocol specified is as TCP, UDP or SCTP. Fields that are ignored
     *  don't need to be wildcarded and should be set to 0.
	 * 
	 */

	private void normalize(FVMatch match) {
		
		int wildcards = match.getWildcards();
		short etherType = match.getDataLayerType();
		short proto = match.getNetworkProtocol();
		if (etherType != IPTYPE && etherType != ARPTYPE) {
			wildcards |= FVMatch.OFPFW_NW_SRC_ALL;
			wildcards |= FVMatch.OFPFW_NW_DST_ALL;
			wildcards |= FVMatch.OFPFW_NW_PROTO;
			wildcards |= FVMatch.OFPFW_NW_TOS;
			wildcards |= FVMatch.OFPFW_TP_DST;
			wildcards |= FVMatch.OFPFW_TP_SRC;
		}
		if (proto != ICMP && proto != TCP && proto != UDP && proto != SCTP) {
			wildcards |= FVMatch.OFPFW_TP_DST;
			wildcards |= FVMatch.OFPFW_TP_SRC;
		}
		match.setWildcards(wildcards);
		FVLog.log(LogLevel.DEBUG,null,match.toString());
	}

	private FlowIntersect getIntersect(FlowEntry fe,
			HashMap<Integer, FlowIntersect> inters) {
		FlowIntersect inter = inters.get(fe.getId());
		if (inter == null) 
			inter = new FlowIntersect(fe);
		
		return inter;
	}

	/**
	 * A big ugly method to set a field of an intersection.
	 * 
	 * 
	 * @param flowIntersect
	 *            - the intersection to update
	 * @param flowEntry
	 *            - the flowentry from where to get the value from.
	 * @param field
	 *            - the field to update
	 * @throws UnknownMatchField
	 *             - Should never happen, but people can be funny.
	 */

	private void setField(FlowIntersect flowIntersect, FVMatch match,
			int field) throws UnknownMatchField {
		flowIntersect.getMatch().setWildcards(
				flowIntersect.getMatch().getWildcards() & ~field);
		switch (field) {
		case FVMatch.OFPFW_DL_DST:
			flowIntersect.getMatch().setDataLayerDestination(
					match.getDataLayerDestination());
			break;
		case FVMatch.OFPFW_DL_SRC:
			flowIntersect.getMatch().setDataLayerSource(
					match.getDataLayerSource());
			break;
		case FVMatch.OFPFW_DL_TYPE:
			flowIntersect.getMatch().setDataLayerType(
					match.getDataLayerType());
			break;
		case FVMatch.OFPFW_IN_PORT:
			FVLog.log(LogLevel.DEBUG, null, "Setting input port");
			flowIntersect.getMatch().setInputPort(
					match.getInputPort());
			break;
		case FVMatch.OFPFW_DL_VLAN:
			flowIntersect.getMatch().setDataLayerVirtualLan(
					match.getDataLayerVirtualLan());
			break;
		case FVMatch.OFPFW_DL_VLAN_PCP:
			flowIntersect.getMatch().setDataLayerVirtualLanPriorityCodePoint(
					match.getDataLayerVirtualLanPriorityCodePoint());
			break;
		case FVMatch.OFPFW_NW_SRC_ALL:
			flowIntersect.getMatch().setNetworkSource(
					match.getNetworkSource());
			break;
		case FVMatch.OFPFW_NW_DST_ALL:
			flowIntersect.getMatch().setNetworkDestination(
					match.getNetworkDestination());
			break;
		case FVMatch.OFPFW_NW_PROTO:
			flowIntersect.getMatch().setNetworkProtocol(
					match.getNetworkProtocol());
			break;
		case FVMatch.OFPFW_NW_TOS:
			flowIntersect.getMatch().setNetworkTypeOfService(
					match.getNetworkTypeOfService());
			break;
		case FVMatch.OFPFW_TP_SRC:
			flowIntersect.getMatch().setTransportSource(
					match.getTransportSource());
			break;
		case FVMatch.OFPFW_TP_DST:
			flowIntersect.getMatch().setTransportDestination(
					match.getTransportDestination());
			break;
		default:
			throw new UnknownMatchField("Unknown field type!");
		}

	}

	/**
	 * match is called when a switch sends a packet in to the controller. The
	 * goal is to find all flow space rules which match the fields of the packet
	 * in.
	 * 
	 * @param dpid
	 *            - the datapath id of the switch which sent the packet in.
	 * @param match
	 *            - the packet in information
	 * @return a list, sorted by priority, of flow space rules which match.
	 */
	public List<FlowEntry> match(long dpid, FVMatch match) {
		BitSet set = new BitSet();
		LinkedList<FlowEntry> flowrules = new LinkedList<FlowEntry>();
		int wildcards = match.getWildcards();
		FVLog.log(LogLevel.DEBUG, null, "dpid: ",dpid, "match: ",match.toString());
		
		set.or(allRules);
		FVLog.log(LogLevel.DEBUG, null, "allRules: ",set.toString());

		try {
			
			testEmpty(set, dpids, dpid, FlowEntry.ALL_DPIDS, wildcards,0);
			/*
			 * Test every field and intersect the resulting bitset. If the bit
			 * set is empty an exception is thrown to stop the search.
			 */
			testEmpty(set, port, match.getInputPort(), ANY_IN_PORT, wildcards,
					FVMatch.OFPFW_IN_PORT);
			if (match.getDataLayerVirtualLan() != ANY_VLAN_ID) {
				testEmpty(set, vlan, (int) match.getDataLayerVirtualLan(),
						(int) ANY_VLAN_ID, wildcards, FVMatch.OFPFW_DL_VLAN);
				testEmpty(set, vlan,
						match.getDataLayerVirtualLanPriorityCodePoint() << 16,
						ANY_VLAN_PCP << 16, wildcards,
						FVMatch.OFPFW_DL_VLAN_PCP);
			} else {

				set.andNot(vlan_ignore);
			}
		
			testEmpty(set, dl_type, match.getDataLayerType(), ANY_ETHER,
					wildcards, FVMatch.OFPFW_DL_TYPE);
			
		
			
			testEmpty(set, nw, (short) match.getNetworkProtocol(),
					(short) ANY_NW_PROTO_TOS, wildcards, FVMatch.OFPFW_NW_PROTO);
			
		
			
			testEmpty(set, nw, (short) (match.getNetworkTypeOfService() << 8),
					(short) (ANY_NW_PROTO_TOS << 8), wildcards,
					FVMatch.OFPFW_NW_TOS);
			
		
			
			testEmpty(set, tp, (int) match.getTransportSource(), (int) ANY_TP,
					wildcards, FVMatch.OFPFW_TP_SRC);
			
		
			
			testEmpty(set, tp, match.getTransportDestination() << 16,
					ANY_TP << 16, wildcards, FVMatch.OFPFW_TP_DST);
			
			
			testEmpty(set, dl_src, FVMatch.toLong(match.getDataLayerSource()),
					ANY_MAC, wildcards, FVMatch.OFPFW_DL_SRC);
			
		
			
			testEmpty(set, dl_dst,
					FVMatch.toLong(match.getDataLayerDestination()), ANY_MAC,
					wildcards, FVMatch.OFPFW_DL_DST);
			
		

			for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
				FlowEntry fe = rules.get(i);
				FVMatch ruleMatch = fe.getRuleMatch();
				FlowIntersect inter;
				inter = new FlowIntersect(fe.clone());

				FVMatch interMatch = inter.getMatch();
				interMatch.setNetworkDestination(testIP(inter,
						FVMatch.OFPFW_NW_DST_SHIFT,
						match.getNetworkDestinationMaskLen(),
						ruleMatch.getNetworkDestinationMaskLen(),
						match.getNetworkDestination(),
						ruleMatch.getNetworkDestination()));
				if (inter.getMatchType() == MatchType.NONE) {
					set.clear(i);
					continue;
				}

				// test ip_src
				interMatch
						.setNetworkSource(testIP(inter,
								FVMatch.OFPFW_NW_SRC_SHIFT,
								match.getNetworkSourceMaskLen(),
								ruleMatch.getNetworkSourceMaskLen(),
								match.getNetworkSource(),
								ruleMatch.getNetworkSource()));
				if (inter.getMatchType() == MatchType.NONE) {
					set.clear(i);
					continue;
				}

			}

		} catch (NoMatch e) {
			FVLog.log(LogLevel.INFO, null, "No match for: ", match);
			return flowrules;
		}

		/*
		 * If we got here we have a match. Now return a prioritized list of
		 * matches.
		 * 
		 * Higher numbers have higher priorities.
		 */

		TreeSet<FlowEntry> entries = new TreeSet<FlowEntry>();
		for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)){
			entries.add(rules.get(i));
		}
		flowrules.addAll(entries);
		return flowrules;

	}

	/**
	 * Given the current set of potential matching rules, obtain the rules which
	 * match the current field defined either by key or anykey. The rules are
	 * represented by bitsets which are intersected to return the remain
	 * potentially matching rules. If after bitset intersection, the resulting
	 * bitset is empty, an exception is thrown to halt the search.
	 * 
	 * 
	 * @param src
	 *            - the current set of potentially matching rules.
	 * @param map
	 *            - the data structure holding all the possible values defined
	 *            by all flow space rules for given field.
	 * @param key
	 *            - the key (or field value) used to index the data structure.
	 * @param anykey
	 *            - the 'any' value for this field.
	 * @param wildcards
	 *            - the set of wildcards defined by this match
	 * @param wild
	 *            - the specific wildcard value for this field
	 * @return - true if field was wildcarded and false otherwise.
	 * @throws NoMatch
	 *             - if the set of rules (ie. the src bitset) is empty after
	 *             intersection.
	 */
	private <K> boolean testEmpty(BitSet src, Map<K, BitSet> map, K key,
			K anykey, int wildcards, int wild) throws NoMatch {
		if ((wildcards & wild) != 0)
			return true;
		BitSet any = get(map, anykey);
		any.or(get(map, key));
		src.and(any);
		if (src.isEmpty())
			throw new NoMatch("No Match");
		return false;
	}

	/**
	 * Convenience function to obtain a value from a data structure.
	 * 
	 * @param map
	 *            - the data structure.
	 * @param key
	 *            - the key for the said data structure.
	 * @return the bitset contained at map.get(key) or an empty bitset
	 */
	private <K> BitSet get(Map<K, BitSet> map, K key) {
		BitSet set = map.get(key);
		if (set == null)
			return new BitSet();
		return (BitSet) set.clone();
	}

	/**
	 * Remove a given bitset from the a given data structure.
	 * 
	 * @param map
	 *            - the data structure to remove from.
	 * @param key
	 *            - the key for the said data structure.
	 * @param value
	 *            - the bitset to remove.
	 * @return the remaining bitset at map.get(key)
	 */
	private <K> BitSet remove(Map<K, BitSet> map, K key, BitSet value) {
		BitSet set = map.get(key);
		if (set == null)
			return null;
		set.andNot(value);
		return map.put(key, set);
	}

	/**
	 * Add a given bitset to a given data structure.
	 * 
	 * @param map
	 *            - the data structure to add to.
	 * @param key
	 *            - the key for the said data structure.
	 * @param value
	 *            the resulting bitset at map.get(key)
	 * @return
	 */
	private <K> BitSet add(Map<K, BitSet> map, K key, BitSet value) {
		BitSet set = get(map, key);
		set.or(value);
		return map.put(key, set);
	}

	/**
	 * Fetches a sorted by priority set of the flow space rules.
	 * 
	 * @return a sorted set of flow entries.
	 */
	public SortedSet<FlowEntry> getRules() {
		TreeSet<FlowEntry> ts = new TreeSet<FlowEntry>(rules.values());
		return ts;// Collections.unmodifiableSortedSet(ts);
	}

	/**
	 * Get a bitset corresponding to a rule.
	 * 
	 * @param rule
	 *            - the rule.
	 * @return the bitset corresponding to this rule.
	 */
	private BitSet getFlowRuleSet(FlowEntry rule) {
		BitSet flowRuleSet = new BitSet();
		flowRuleSet.set(rule.getId());
		return flowRuleSet;
	}

	/**
	 * Returns the number of rules in this flowspace.
	 * 
	 * @return the rule count
	 */
	public int getRuleCount() {
		return ruleCount;
	}

	/**
	 * This method has to exist otherwise the #!@#@! XML[Encoder/Decoder] throws
	 * up.
	 * 
	 * @param r
	 *            - the set of rules.
	 */
	public void setRules(SortedSet<FlowEntry> r) {
		rules.clear();
		for (FlowEntry rule : r)
			addRule(rule);

	}

	/**
	 * Test to check whether the IPs given overlap or not at all
	 * 
	 * @param flowIntersect
	 *            - the current potential intersection
	 * @param maskShift
	 *            - the shift value (ie. either for a src_ip or a dst_ip).
	 * @param masklenX
	 *            - the netmask for the first ip.
	 * @param masklenY
	 *            - the netmask for the second ip.
	 * @param x
	 *            - the first ip
	 * @param y
	 *            - the second ip.
	 * @return the smallest intersection of this ip space.
	 */
	private int testIP(FlowIntersect flowIntersect, int maskShift,
			int masklenX, int masklenY, int x, int y) {
		int min = Math.min(masklenX, masklenY); // get the less specific address
		int max = Math.max(masklenX, masklenY); // get the more specific address
		int min_encoded = 32 - min; // because OpenFlow does it backwards... grr
		int max_encoded = 32 - max; // because OpenFlow does it backwards... grr
		if (max_encoded >= 32) // set all the bits if this is in fact fully
			max_encoded = 63; // wildcarded; if only for wireshark's sake

		int mask;
		if (min == 0)
			mask = 0; // nasty work around for stupid signed ints
		else
			mask = ~((1 << min_encoded) - 1); // min < 32, so no signed issues
		// int mask = (1 << min) - 1;

		if ((x & mask) != (y & mask)) // if these are not in the same CIDR block
			flowIntersect.setMatchType(MatchType.NONE);
		// else there is some overlap
		OFMatch interMatch = flowIntersect.getMatch();
		int wildCards = interMatch.getWildcards();
		// turn off all bits for this match and then turn on the used ones
		// use MAX not MIN, because we want the most specific intersection
		// split into two ops, so we can see intermediate step in debugger
		// assumes SRC mask == DST mask
		// turn off all bits for this match (making it an exact match)
		wildCards = wildCards
				& ~(((1 << OFMatch.OFPFW_NW_SRC_BITS) - 1) << maskShift);
		// turn on the bits for the intersection
		wildCards = wildCards | max_encoded << maskShift;
		interMatch.setWildcards(wildCards);
		if (masklenX < masklenY) {
			flowIntersect.maybeSubset = false;
			return y;
		} else if (masklenX > masklenY) {
			flowIntersect.maybeSuperset = false;
			return x;
		}

		// note that b/c of how CIDR addressing works, there is no overlap that
		// is not a SUB or SUPERSET
		return x; // x == y; doesn't matter
	}

	private class Pair<A, B> {
		private A first;
		private B second;

		public Pair(A first, B second) {
			super();
			this.first = first;
			this.second = second;
		}

		public int hashCode() {
			int hashFirst = first != null ? first.hashCode() : 0;
			int hashSecond = second != null ? second.hashCode() : 0;

			return (hashFirst + hashSecond) * hashSecond + hashFirst;
		}

		@SuppressWarnings("rawtypes")
		public boolean equals(Object other) {
			if (other instanceof Pair) {
				Pair otherPair = (Pair) other;
				return ((this.first == otherPair.first || (this.first != null
						&& otherPair.first != null && this.first
							.equals(otherPair.first))) && (this.second == otherPair.second || (this.second != null
						&& otherPair.second != null && this.second
							.equals(otherPair.second))));
			}

			return false;
		}

		public String toString() {
			return "(" + first + ", " + second + ")";
		}

		public A getFirst() {
			return first;
		}

		@SuppressWarnings("unused")
		public void setFirst(A first) {
			this.first = first;
		}

		public B getSecond() {
			return second;
		}

		@SuppressWarnings("unused")
		public void setSecond(B second) {
			this.second = second;
		}
	}

}
