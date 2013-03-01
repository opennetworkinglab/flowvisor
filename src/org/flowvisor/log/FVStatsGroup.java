package org.flowvisor.log;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.classifier.FVSendMsg;
import org.openflow.protocol.OFMessage;

/**
 * A collection of FVStats, organized by their senders
 *
 * @author capveg
 *
 */

public class FVStatsGroup {
	Map<FVSendMsg, FVStats> group;
	FVStats total;

	static Map<String, FVStatsGroup> sharedStats = new HashMap<String, FVStatsGroup>();

	public FVStatsGroup() {
		this.group = new HashMap<FVSendMsg, FVStats>();
		this.total = new FVStats();
	}

	public void increment(FVSendMsg from, OFMessage ofm) {
		FVStats stats = group.get(from);
		if (stats == null) {
			stats = new FVStats();
			group.put(from, stats);
		}
		stats.incrementCounter(ofm);
		total.incrementCounter(ofm);
	}

	public long get(FVSendMsg from, OFMessage ofm) {
		if (!group.containsKey(from))
			return 0;
		else
			return group.get(from).getCounter(ofm);
	}

	public FVStats getTotal() {
		return this.total;
	}

	public long getTotal(OFMessage ofm) {
		return this.total.getCounter(ofm);
	}

	public synchronized void zeroCounters() {
		group.clear();
		total.zeroCounters();
	}

	@Override
	public synchronized String toString() {
		StringBuffer ret = new StringBuffer();
		for (FVSendMsg fvSendMsg : group.keySet()) {
			ret.append(fvSendMsg.getName());
			ret.append(" :: ");
			ret.append(group.get(fvSendMsg).toString());
			ret.append("\n");
		}
		ret.append("Total :: ");
		ret.append(total.toString());
		ret.append("\n");

		return ret.toString();
	}
	
	public synchronized HashMap<String, Object> toMap() {
		HashMap<String, Object> ret = new HashMap<String,Object>();
		for (FVSendMsg fvSendMsg : group.keySet())
			ret.put(fvSendMsg.getName(), group.get(fvSendMsg).toMap());
		
		ret.put("Total", total.toMap());
		
		return ret;
	}

	/**
	 * Creates a shared reference to a stats group; this is used by all slicer
	 * instances in a slice, i.e., one per switch
	 *
	 * @param owner
	 *            the name of the slice
	 * @return An already instantiated FVStatsGroup
	 */

	public static synchronized FVStatsGroup createSharedStats(String owner) {
		if (!sharedStats.containsKey(owner)) {
			sharedStats.put(owner, new FVStatsGroup());
		}
		return sharedStats.get(owner);
	}



}