package org.flowvisor.log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;

/**
 * Used to accumulate flowvisor statistics, e.g., how many messages per second
 * were in which direction
 *
 * Counters are LOOSELY thread-safe, meaning if you can handle a little bit of
 * error in your counters, i..e, off-by-one, then you should be able to use
 * these with threads. In theory all of the potentially-fatal concurrency
 * violations have been wrapped with synchronized
 *
 * @author capveg
 *
 */

public class FVStats {
	Map<String, Long> counters;

	/**
	 * TODO : reimplement this as a flat array; our profiler says that we spend
	 * 10% of our time in incrementCOunter doing Long.valueOf(long) which seems
	 * crazy
	 */

	/**
	 * Initialize a set of counters from src to dst
	 *
	 * @param src
	 * @param dst
	 */

	public FVStats() {
		super();
		this.counters = new HashMap<String, Long>();
		zeroCounters();
	}

	/**
	 * Reset all counters to zero
	 *
	 * This is synchonized to avoid a concurrency exception with the accumulate
	 * counters function
	 *
	 */

	public synchronized void zeroCounters() {
		counters.clear();
	}

	/**
	 * Don't lock here, for performance
	 *
	 * Yes, it's a race condition, but they are just counters and we will claim
	 * it is okay if they are out of sync
	 *
	 * @param oftype
	 */

	public void incrementCounter(OFMessage ofm) {
		String oftype = msg2str(ofm);
		if (this.counters.containsKey(oftype))
			this.counters.put(oftype, this.counters.get(oftype) + 1l);
		else
			this.counters.put(oftype, 1l);
	}

	static public String msg2str(OFMessage ofm) {
		OFType ofType = ofm.getType();
		if (ofType == null)
			return "UNPARSABLE-" + ofm.toString();
		String ret = ofType.toString();
		if (ofm instanceof OFStatisticsReply) {
			ret += "." + ((OFStatisticsReply) ofm).getStatisticType();
		} else if (ofm instanceof OFStatisticsRequest) {
			ret += "." + ((OFStatisticsRequest) ofm).getStatisticType();
		}
		return ret;
	}

	public long getCounter(OFMessage ofm) {
		String ofType = msg2str(ofm);
		if (this.counters.containsKey(ofType))
			return this.counters.get(ofType);
		else
			return 0l;
	}

	/**
	 * Returns the sum of:
	 *
	 * foreach OFType: getCounter(OFType)
	 *
	 * @return zero or larger
	 */
	public synchronized long getAccumulatedCounters() {
		long ret = 0;
		for (Long l : this.counters.values()) {
			ret += l;
		}
		return ret;
	}

	@Override
	public synchronized String toString() {
		StringBuffer ret = new StringBuffer();
		for (String ofType : new HashSet<String>(counters.keySet())) {
			if (ret.length() > 0)
				ret.append(",");
			ret.append(ofType.toString());
			ret.append("=");
			ret.append(counters.get(ofType));
		}
		return ret.toString();
	}

	public synchronized HashMap<String, Long> toMap() {
		HashMap<String, Long> ret = new HashMap<String, Long>();
		for (String ofType : new HashSet<String>(counters.keySet())) 
			ret.put(ofType.toString(), counters.get(ofType));
		return ret;
	}
}
