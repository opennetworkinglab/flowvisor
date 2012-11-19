/**
 *
 */
package org.flowvisor.flows;

import java.io.FileNotFoundException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.exceptions.MalformedFlowChange;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.TopologyController;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;

/**
 * @author capveg
 *
 */
public class FlowSpaceUtil {
	/**
	 * Consult the FlowSpace and get a list of all slices that get connections
	 * to this switch, as specified by it's DPID
	 *
	 * This function is somewhat expensive (think DB join), so the results
	 * should be cached, and then updated when the FlowSpace signals a change
	 *
	 * @param flowMap
	 *            A map of flow entries, like that from
	 *            FVConfig.getFlowSpaceFlowMap();
	 * @param dpid
	 *            As returned in OFFeaturesReply
	 * @return A list of names of slices, i.e., "alice", "bob", etc.
	 */
	public static Set<String> getSlicesByDPID(FlowMap flowMap, long dpid) {
		Set<String> ret = new HashSet<String>();
		FVMatch match = new FVMatch();
		match.setWildcards(OFMatch.OFPFW_ALL);
		List<FlowEntry> rules = flowMap.matches(dpid, match);
		for (FlowEntry rule : rules) {
			for (OFAction action : rule.getActionsList()) {
				SliceAction sliceAction = (SliceAction) action; // the flowspace
				// should only
				// contain
				// SliceActions
				ret.add(sliceAction.sliceName);
			}
		}

		if (TopologyController.isConfigured())
			ret.add(TopologyController.TopoUser);
		return ret;
	}

	/**
	 * Return the flowspace controlled by this slice
	 *
	 * Note that this correctly removes the "holes" caused by higher priority
	 * flowspace entries
	 *
	 * @param sliceName
	 * @return
	 */

	public static FlowMap getSliceFlowSpace(String sliceName) throws ConfigError {
		FVMatch match = new FVMatch();
		FlowMap ret = new LinearFlowMap();
		match.setWildcards(FVMatch.OFPFW_ALL);
		FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
		List<FlowIntersect> intersections = flowSpace.intersects(
				FlowEntry.ALL_DPIDS, match);
		for (FlowIntersect inter : intersections) {
			FlowEntry rule = inter.getFlowEntry();
			FlowEntry neoRule = null;
			neoRule = rule.clone();
			neoRule.setRuleMatch(inter.getMatch());
			neoRule.setActionsList(new ArrayList<OFAction>());
			for (OFAction action : rule.getActionsList()) {
				// the flowspace should only contain SliceActions
				SliceAction sliceAction = (SliceAction) action;
				if (sliceAction.getSliceName().equals(sliceName)) {
					neoRule.getActionsList().add(sliceAction.clone());
					ret.addRule(neoRule);
				}
			}
		}
		return ret;
	}

	/**
	 * Consult the flowspace and return the set of ports that this slice is
	 * supposed to use on this switch
	 *
	 * This function is somewhat expensive (think DB join), so the results
	 * should be cached, and then updated when the FlowSpace signals a change
	 *
	 * OFPort.OFPP_ALL (0xfffc) is used to describe that all ports are supposed
	 * to be used. If all ports are valid, then OFPP_ALL will be the only port
	 * returned.
	 *
	 * @param dpid
	 *            the switch identifier (from OFFeaturesReply)
	 * @param slice
	 *            The slices name, e.g., "alice"
	 * @return Set of ports
	 */
	public static Set<Short> getPortsBySlice(long dpid, String slice,
			FlowMap flowmap) {
		boolean allPorts = false;
		Set<Short> ret = new HashSet<Short>();
		FVMatch match = new FVMatch();

		if (TopologyController.isConfigured()
				&& slice.equals(TopologyController.TopoUser)) {
			allPorts = true; // topology controller has access to everything
		} else {
			// SYNCH flowmap HERE

			match.setWildcards(FVMatch.OFPFW_ALL);

			List<FlowEntry> rules = flowmap.matches(dpid, match);
			for (FlowEntry rule : rules) {
				for (OFAction action : rule.getActionsList()) {
					SliceAction sliceAction = (SliceAction) action; // the
					// flowspace
					// should only
					// contain
					// SliceActions
					if (sliceAction.sliceName.equals(slice)) {
						OFMatch ruleMatch = rule.getRuleMatch();
						if ((ruleMatch.getWildcards() & OFMatch.OFPFW_IN_PORT) != 0)
							allPorts = true;
						else
							ret.add(ruleMatch.getInputPort());
					}
				}
			}
		}
		if (allPorts) { // if we got one "match all ports", just replace
			// everything
			ret.clear(); // with OFPP_ALL
			ret.add(OFPort.OFPP_ALL.getValue());
		}
		return ret;
	}

	/**
	 * Mini-frontend for querying FlowSpace
	 *
	 * @param args
	 * @throws FileNotFoundException
	 */

	public static void main(String args[]) throws FileNotFoundException, ConfigError  {
		if ((args.length != 2) && (args.length != 3)) {
			System.err
					.println("Usage: FLowSpaceUtil config.xml <dpid> [slice]");
			System.exit(1);
		}

		FVConfig.readFromFile(args[0]);
		long dpid = FlowSpaceUtil.parseDPID(args[1]);

		switch (args.length) {
		case 2:
			Set<String> slices = FlowSpaceUtil.getSlicesByDPID(
					FVConfig.getFlowSpaceFlowMap(), dpid);
			System.out.println("The following slices have access to dpid="
					+ args[1]);
			for (String slice : slices)
				System.out.println(slice);
			break;
		case 3:
			Set<Short> ports = FlowSpaceUtil.getPortsBySlice(dpid, args[2],
					FVConfig.getFlowSpaceFlowMap());
			System.out.println("Slice " + args[2] + " on switch " + args[1]
					+ " has access to port:");
			if (ports.size() == 1
					&& ports.contains(Short.valueOf(OFPort.OFPP_ALL.getValue())))
				System.out.println("ALL PORTS");
			else
				for (Short port : ports)
					System.out.println("Port: " + port);
		}

	}
	
	
	/**
	 * Get the FlowMap. If the existing flowmap is a Linear flow map
	 * then return a copy, otherwise simply return the reference to 
	 * the instance.
	 * 
	 * @param dpid - the dpid to cut up the flowspace. Unused in the
	 * 	case of the federated flowmap.
	 * 
	 * @return a reference to the current flowmap or the subflowmap.
	 */
	
	public static FlowMap getFlowMap(long dpid)  throws ConfigError {
		FlowMap fm = FVConfig.getFlowSpaceFlowMap();
		switch (fm.getType()) {
		case LINEAR:
			return FlowSpaceUtil.getSubFlowMap(fm, dpid, new FVMatch());
		case FEDERATED: 
			return fm;
		default:
			FVLog.log(LogLevel.ALERT, null, "Unknown FlowMap type");
			throw new RuntimeException("Unknown FlowMap type; time to quit");
		}
	}

	/**
	 * Get the FlowMap that is the intersection of the Master FlowSpace and this
	 * dpid
	 *
	 * @param dpid
	 *            As returned from OFFeatureReply
	 * @return A valid flowmap (never null)
	 */

	public static FlowMap getSubFlowMap(long dpid) throws ConfigError  {
		// assumes that new OFMatch() matches everything
		synchronized (FVConfig.class) {
			return FlowSpaceUtil.getSubFlowMap(FVConfig.getFlowSpaceFlowMap(),
					dpid, new FVMatch());
		}
	}

	/**
	 * Get the FlowMap that is the intersection of this FlowMap and the given
	 * flowSpace that is, any rule in the source flowmap that matches any part
	 * of dpid and match is added to the returned flowmap
	 *
	 * @param flowMap
	 *            Source flow map
	 * @param dpid
	 *            datapathId from OFFeaturesReply
	 * @param match
	 *            a valid OFMatch() struture
	 * @return a valid flowMap (never null)
	 */

	public static FlowMap getSubFlowMap(FlowMap flowMap, long dpid,
			FVMatch match) {
		FlowMap neoFlowMap = flowMap.instance(); 
		List<FlowEntry> flowEntries = flowMap.matches(dpid, match);
		for (FlowEntry flowEntry : flowEntries) {
			FlowEntry neoFlowEntry = flowEntry.clone();
			//neoFlowEntry.setId(FlowEntry.getUniqueId());
			neoFlowEntry.setDpid(dpid);
			neoFlowMap.addRule(neoFlowEntry);
		}

		return neoFlowMap;
	}

	public static String toString(List<OFAction> actionsList) {
		String actions = "";
		if (actionsList == null)
			return actions;
		for (OFAction action : actionsList) {
			if (!actions.equals(""))
				actions += ",";
			actions += action.toString();
		}
		return actions;
	}

	/**
	 * Convert a string to a DPID "*","all","all_dpids" --> ALL_DPIDS constant
	 * if there is a ':", treat as a hex string else assume it's decimal
	 *
	 * @param dpidStr
	 * @return a dpid
	 */
	public static long parseDPID(String dpidStr) {
		if (dpidStr.equals("*") || dpidStr.toLowerCase().equals("any")
				|| dpidStr.toLowerCase().equals("all")
				|| dpidStr.toLowerCase().equals("all_dpids"))
			return FlowEntry.ALL_DPIDS;
		if (dpidStr.indexOf(':') != 0)
			return HexString.toLong(dpidStr);
		else
			// maybe long in decimal?
			return Long.valueOf(dpidStr);
	}

	public static String dpidToString(long dpid) {
		if (dpid == FlowEntry.ALL_DPIDS)
			return FlowEntry.ALL_DPIDS_STR;
		return HexString.toHexString(dpid);
	}
	
	public static String macToString(long mac) {
		if (mac == FlowSpaceRuleStore.ANY_MAC)
			return "any";
		return HexString.toHexString(mac).replaceFirst("00:00:", "");
	}
	
	public static long parseMac(String macStr) {
		if (macStr.equalsIgnoreCase("any") || macStr.equalsIgnoreCase("all"))
			return FlowSpaceRuleStore.ANY_MAC;
		return HexString.toLong(macStr);
	}
	

	public static FVMatch fvMatchFromString(String ofMatchStr) throws MalformedFlowChange{
		FVMatch tmp = new FVMatch();

		// try as is first
		try {
			tmp.fromString(ofMatchStr);
		} catch (IllegalArgumentException e) {
			// if that doesn't work, try wrapping with "OFMatch["
			try {
				tmp.fromString("OFMatch[" + ofMatchStr + "]");
			} catch (IllegalArgumentException e1) {
				throw new MalformedFlowChange("could not parse match: '"
						+ ofMatchStr + "'");
			}
		}

		return tmp;
	}
	/**
	 * Remove all of the flowSpace associated with a slice
	 *
	 * Does NOT send updates to classifiers
	 *
	 * DOES lock the flowSpace for synchronization
	 * 
	 * amended by ash: You cannot call remove on the iterator
	 * as this ignores the flowmaps interface and it assumes 
	 * that what is returned from getRules is the actual
	 * data structure which holds the flow space def, which 
	 * may not actually be the case.
	 *
	 * @param sliceName
	 */
	public static FlowMap deleteFlowSpaceBySlice(String sliceName) throws ConfigError {
		FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
		SliceAction sliceAction = null;
		HashSet<Integer> toRemove = new HashSet<Integer>();
		synchronized (flowSpace) {
			for (Iterator<FlowEntry> flowIter = flowSpace.getRules().iterator(); flowIter
					.hasNext();) {
				FlowEntry flowEntry = flowIter.next();
				for (Iterator<OFAction> actIter = flowEntry.getActionsList()
						.iterator(); actIter.hasNext();) {
					OFAction ofAction = actIter.next();
					if (!(ofAction instanceof SliceAction))
						continue;
					sliceAction = (SliceAction) ofAction;
					if (sliceName.equals(sliceAction.getSliceName()))
						//toRemove.add(flowEntry.getId());
						actIter.remove(); // remove this action from the entry
				}
				if (flowEntry.getActionsList().size() == 0)
					toRemove.add(flowEntry.getId());
					//flowIter.remove(); // remove the entry if no more actions
			}
			for (Integer i : toRemove)
				try {
					flowSpace.removeRule(i);
					FlowSpaceImpl.getProxy().removeRule(i);
				} catch (FlowEntryNotFound e) {
					FVLog.log(LogLevel.WARN, null, "Removed flowspace has already been removed, something bad happened.");
				}
		}
		return flowSpace;
	}

	public static String connectionToString(SocketChannel sock) {
		try {
			Socket ss = sock.socket();
			return ss.getLocalAddress().toString() + ":" + ss.getLocalPort()
					+ "-->" + ss.getRemoteSocketAddress().toString();
		} catch (NullPointerException e) {
			return "NONE";
		}
	}

	public static FlowMap getNewFlowMap(int type) {
		FlowMap.type fmtype = FlowMap.type.values()[type];

		switch (fmtype) {
			case LINEAR:
				return new LinearFlowMap();
			case FEDERATED:
				return new FederatedFlowMap();
			default:
				FVLog.log(LogLevel.ALERT, null, "Unknown FlowMap type");
				throw new RuntimeException("Unknown FlowMap type; time to quit");	
		}
	}
	
	public static long toLong(byte[] mac) {
		long arr = 0;
		for (int i = 0; i < mac.length ; i++) {
			arr <<= 8;
			arr ^= (long) mac[i] & 0xff;
		}
		return arr;
	}
	
	public static byte[] toByteArray(Long mac) {
		byte macArr[] = new byte[] {
				(byte)((mac >> 40) & 0xff),
				(byte)((mac >> 32) & 0xff),
				(byte)((mac >> 24) & 0xff),
				(byte)((mac >> 16) & 0xff),
				(byte)((mac >> 8 ) & 0xff),
				(byte)((mac >> 0) & 0xff),
		};
		return macArr;
	}
	
	public static String intToIp(int i) {
        return ((i >> 24 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ( i        & 0xFF);
    }
	
}
