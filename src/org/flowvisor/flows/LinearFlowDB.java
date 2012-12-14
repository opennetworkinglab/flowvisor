package org.flowvisor.flows;

import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.flowvisor.events.FVEventHandler;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

/**
 * Internal DB for tracking the switch's state
 *
 * NOT internally thread-safe
 *
 * @author capveg
 *
 */

public class LinearFlowDB implements FlowDB, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	SortedSet<FlowDBEntry> db;
	long dpid;
	transient FVEventHandler fvEventHandler;
	transient int flowID;

	public LinearFlowDB(FVEventHandler fvEventHandler) {
		this.db = new TreeSet<FlowDBEntry>();
		this.fvEventHandler = fvEventHandler;
		this.flowID = 0;
	}

	@Override
	public void processFlowMod(OFFlowMod flowMod, long dpid, String sliceName) {
		String op = "unknown";
		switch (flowMod.getCommand()) {
		case OFFlowMod.OFPFC_ADD:
			op = "ADD";
			processFlowModAdd(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_MODIFY:
		case OFFlowMod.OFPFC_MODIFY_STRICT:
			op = "MOD";
			processFlowModModify(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_DELETE_STRICT:
			op = "DEL";
			processFlowModDeleteStrict(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_DELETE:
			op = "DEL";
			processFlowModDelete(flowMod, sliceName, dpid);
			break;
		default:
			FVLog.log(LogLevel.WARN, fvEventHandler,
					"flowDB: ignore fm with unknown flow_mod command:: ",
					flowMod.getCommand());
		}
		FVLog.log(LogLevel.DEBUG, null, "flowdb: ", op, ": new size ", size());
	}

	private void processFlowModDeleteStrict(OFFlowMod flowMod,
			String sliceName, long dpid) {
		boolean found = false;
		for (Iterator<FlowDBEntry> it = this.db.iterator(); it.hasNext();) {
			FlowDBEntry flowDBEntry = it.next();
			if (flowDBEntry.matches(dpid, new FVMatch(flowMod.getMatch()),
					flowMod.getCookie(), flowMod.getPriority()).getMatchType() == MatchType.EQUAL) {
				FVLog.log(LogLevel.DEBUG, fvEventHandler,
						"flowDB: del by strict: ", flowDBEntry);
				it.remove();
				found = true;
			}
		}
		if (!found)
			FVLog.log(LogLevel.DEBUG, fvEventHandler,
					"flowDB: delete strict - no match found");
	}

	/**
	 * Remove one or more flowdb entries
	 *
	 * fail silently if there is nothing deleted
	 *
	 * @param flowMod
	 * @param sliceName
	 */

	private void processFlowModDelete(OFFlowMod flowMod, String sliceName,
			long dpid) {
		boolean found = false;
		for (Iterator<FlowDBEntry> it = this.db.iterator(); it.hasNext();) {
			FlowDBEntry flowDBEntry = it.next();
			MatchType matchType = flowDBEntry.matches(dpid, new FVMatch(flowMod.getMatch()),
					flowMod.getCookie(), flowMod.getPriority()).getMatchType();
			FVLog.log(LogLevel.DEBUG, null, "flowdb " + flowDBEntry.getCookie() + " == " + flowMod.getCookie());
			if (matchType == MatchType.EQUAL || matchType == MatchType.SUPERSET) {
				FVLog.log(LogLevel.DEBUG, fvEventHandler,
						"flowDB: del by non-strict: ", flowDBEntry);
				it.remove();
				found = true;
			}
		}
		if (!found)
			FVLog.log(LogLevel.DEBUG, fvEventHandler,
					"flowDB: delete - no match found");
	}

	/**
	 * Change one or more flowdb entries
	 *
	 * fail silently if nothing matches
	 *
	 * @param flowMod
	 * @param sliceName
	 */
	private void processFlowModModify(OFFlowMod flowMod, String sliceName,
			long dpid) {
		FVLog.log(LogLevel.WARN, fvEventHandler,
				"flowdb: ignoring unimplemented flowMod.modify");
	}

	/**
	 * Add a new flowdb entry
	 *
	 * @param flowMod
	 * @param sliceName
	 * @param dpid
	 */
	private void processFlowModAdd(OFFlowMod flowMod, String sliceName,
			long dpid) {
		FlowDBEntry flowDBEntry = new FlowDBEntry(dpid, new FVMatch(flowMod.getMatch()),
				this.flowID++, flowMod.getPriority(), flowMod.getActions(),
				sliceName, flowMod.getCookie());
		FVLog.log(LogLevel.DEBUG, this.fvEventHandler,
				"flowDB: adding new entry:", flowDBEntry, flowMod);
		this.db.add(flowDBEntry);
	}

	@Override
	public String processFlowRemoved(OFFlowRemoved flowRemoved, long dpid) {
		String sliceName = null;
		for (Iterator<FlowDBEntry> it = this.db.iterator(); it.hasNext();) {
			FlowDBEntry flowDBEntry = it.next();
			FVLog.log(LogLevel.DEBUG, null, flowDBEntry.toString());
			FVLog.log(LogLevel.DEBUG, null, "FV " + flowDBEntry.getCookie() + " == " + flowRemoved.getCookie());
			
			if (flowDBEntry.getRuleMatch().equals(flowRemoved.getMatch())
					&& flowDBEntry.getPriority() == flowRemoved.getPriority()
					&& flowDBEntry.getCookie() == flowRemoved.getCookie()
					&& flowDBEntry.getDpid() == dpid) {
				it.remove();
				
				sliceName = flowDBEntry.getSliceName();
				FVLog.log(LogLevel.DEBUG, this.fvEventHandler,
						"flowDB: removing flow '", flowDBEntry,
						"'matching flowRemoved: ", flowRemoved);
				break;
			}
		}
		if (sliceName == null)
			FVLog.log(LogLevel.INFO, this.fvEventHandler,
					"flowDB: ignoring unmatched flowRemoved: ", flowRemoved);
		return sliceName;
	}

	@Override
	public Iterator<FlowDBEntry> iterator() {
		return this.db.iterator();
	}

	@Override
	public int size() {
		return this.db.size();
	}

}
