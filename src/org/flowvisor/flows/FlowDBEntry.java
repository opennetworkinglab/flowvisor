package org.flowvisor.flows;

import java.util.List;
import java.util.Map;

import org.flowvisor.config.BracketParse;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.action.OFAction;

public class FlowDBEntry extends FlowEntry {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	String sliceName;
	long cookie;
	private long creationTime;

	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return creationTime;
	}

	public FlowDBEntry(long dpid, FVMatch fvMatch, int flowID, short priority,
			List<OFAction> actionsList, String sliceName, long cookie) {
		super(dpid, fvMatch, flowID, priority, actionsList);
		this.sliceName = sliceName;
		this.cookie = cookie;
		this.creationTime = System.currentTimeMillis();
	}

	/**
	 * Init from a flowMod for simplicity
	 *
	 * @param dpid
	 * @param flowID
	 *            unique id for this flow
	 * @param flowMod
	 *            a complete flowMod
	 * @param sliceName
	 *            e.g., "alice"
	 */
	public FlowDBEntry(long dpid, int flowID, OFFlowMod flowMod,
			String sliceName) {
		this(dpid, new FVMatch( flowMod.getMatch()), flowID, flowMod.getPriority(), flowMod
				.getActions(), sliceName, flowMod.getCookie());
	}

	public FlowDBEntry() {
		// java bean
	}

	public FlowDBEntry(long dpid, int flowID, OFFlowRemoved flowRemoved,
			String sliceName) {
		this(dpid, new FVMatch( flowRemoved.getMatch()), flowID, flowRemoved.getPriority(),
				null, sliceName, flowRemoved.getCookie());
	}

	/**
	 * @return the sliceName
	 */
	public String getSliceName() {
		return sliceName;
	}

	/**
	 * @param sliceName
	 *            the sliceName to set
	 */
	public void setSliceName(String sliceName) {
		this.sliceName = sliceName;
	}

	/**
	 * @return the cookie
	 */
	public long getCookie() {
		return cookie;
	}

	/**
	 * @param cookie
	 *            the cookie to set
	 */
	public void setCookie(long cookie) {
		this.cookie = cookie;
	}

	@Override
	public Map<String, String> toBracketMap() {
		Map<String, String> map = super.toBracketMap();
		super.toBracketMap(); 
		map.put("cookie", String.valueOf(cookie));
		map.put("slice", this.sliceName);
		map.put("duration",
				String.valueOf(System.currentTimeMillis() - this.creationTime));
		return map;
	}

	@Override
	public FlowDBEntry fromBacketMap(Map<String, String> map) {
		super.fromBacketMap(map);
		if (map.containsKey("cookie"))
			this.setCookie(Long.valueOf(map.get("cookie")));
		if (map.containsKey("slice"))
			this.setSliceName(map.get("slice"));
		if (map.containsKey("duration"))
			this.setCreationTime(System.currentTimeMillis()
					- Long.valueOf(map.get("duration")));
		return this;
	}

	private void setCreationTime(long l) {
		this.creationTime = l;
	}

	public static FlowEntry fromString(String string) {
		Map<String, String> map = BracketParse.decode(string);
		FlowDBEntry flowDBEntry = new FlowDBEntry();
		return flowDBEntry.fromBacketMap(map);
	}

	/**
	 * Return a description of how this argument intersects with this
	 *
	 * @param dpid
	 * @param fvMatch
	 * @param cookie
	 * @param priority
	 * @return
	 */

	public FlowIntersect matches(long dpid, FVMatch fvMatch, long cookie,
			short priority) {
		/*if (cookie != this.cookie || priority != this.priority)
			return new FlowIntersect(this).setMatchType(MatchType.NONE);
		else*/
			return super.matches(dpid, fvMatch);
	}
}
