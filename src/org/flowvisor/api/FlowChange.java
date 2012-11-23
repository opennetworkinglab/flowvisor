/**
 *
 */
package org.flowvisor.api;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.flowvisor.exceptions.MalformedFlowChange;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.openflow.protocol.FVMatch;
//import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.*;
import org.openflow.util.HexString;


/**
 * Object that holds a change to the flowspace
 *
 * @author capveg
 *
 */
public class FlowChange {
	final static public String OP_KEY = "operation";
	final static public String ID_KEY = "id";
	final static public String PRIORITY_KEY = "priority";
	final static public String DPID_KEY = "dpid";
	final static public String ACTIONS_KEY = "actions";
	final static public String MATCH_KEY = "match";

	public enum FlowChangeOp {
		ADD, REMOVE, CHANGE;
	}

	private FlowChangeOp operation;
	private int id;
	private int priority;
	private long dpid;
	private FVMatch match;
	private List<OFAction> actions;

	/**
	 * Convert this Map to a FlowChange
	 *
	 * @return a key=value map representing this flowchange
	 */
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put(OP_KEY, operation.toString());
		if (operation != FlowChangeOp.ADD)
			map.put(ID_KEY, String.valueOf(id));
		if (operation != FlowChangeOp.REMOVE) {
			map.put(DPID_KEY, HexString.toHexString(dpid));
			map.put(PRIORITY_KEY, String.valueOf(priority));
			map.put(MATCH_KEY, match.toString());
			map.put(ACTIONS_KEY, FlowSpaceUtil.toString(actions));
		}
		return map;
	}

	/**
	 * Convert a map to a FlowChange that is, reverse the actions of toMap()
	 *
	 * @param map
	 * @return
	 * @throws MalformedFlowChange
	 */

	public static FlowChange fromMap(Map<String, String> map)
			throws MalformedFlowChange {
		FlowChange flowChange = new FlowChange();
		String op = map.get(OP_KEY);
		if (op == null)
			throw new MalformedFlowChange("missing key '" + OP_KEY + "' from "
					+ map.toString());
		flowChange.setOperation(FlowChangeOp.valueOf(op.toUpperCase()));
		if (flowChange.getOperation() != FlowChangeOp.ADD) {
			String ind = map.get(ID_KEY);
			if (ind == null)
				throw new MalformedFlowChange("missing key '" + ID_KEY
						+ "' from " + map.toString());
			flowChange.setId(Integer.valueOf(ind));
		}
		if (flowChange.getOperation() != FlowChangeOp.REMOVE) {
			// parse dpid
			String dpidStr = map.get(DPID_KEY);
			if (dpidStr == null)
				throw new MalformedFlowChange("operation "
						+ flowChange.getOperation() + "requires key '"
						+ DPID_KEY + "' from " + map.toString());
			flowChange.setDpid(FlowSpaceUtil.parseDPID(dpidStr));

			// parse priority
			String ind = map.get(PRIORITY_KEY);
			if (ind == null)
				throw new MalformedFlowChange("missing key '" + PRIORITY_KEY
						+ "' from " + map.toString());
			flowChange.setPriority(Integer.valueOf(ind));

			// parse match
			String matchStr = map.get(MATCH_KEY);
			if (matchStr == null)
				throw new MalformedFlowChange("operation "
						+ flowChange.getOperation() + "requires key '"
						+ MATCH_KEY + "' from " + map.toString());
			FVMatch tmp = new FVMatch();

			// try as is first
			try {
				tmp.fromString(matchStr);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				// if that doesn't work, try wrapping with "OFMatch["
				try {
					tmp.fromString("OFMatch[" + matchStr + "]");
				} catch (IllegalArgumentException e1) {
					throw new MalformedFlowChange("could not parse match: '"
							+ matchStr + "'");
				}
			}
			flowChange.setMatch(tmp);

			// parse actions
			String astr = map.get(ACTIONS_KEY);
			if (astr == null)
				throw new MalformedFlowChange("operation "
						+ flowChange.getOperation() + "requires key '"
						+ ACTIONS_KEY + "' from " + map.toString());

			String list[] = astr.split(",");
			List<OFAction> alist = new LinkedList<OFAction>();
			for (int i = 0; i < list.length; i++)
				alist.add(SliceAction.fromString(list[i]));
			flowChange.setActions(alist);
			
		}
		return flowChange;
	}


	/**
	 * Create a map from the parameters
	 *
	 * @param add
	 * @param idStr
	 * @param priorityStr
	 * @param dpid2
	 * @param match2
	 * @param actions2
	 * @return
	 */
	public static Map<String, String> makeMap(FlowChangeOp op, String dpid2,
			String idStr, String priorityString, String match2, String actions2) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(OP_KEY, op.toString());
		if (idStr != null)
			map.put(ID_KEY, idStr);
		map.put(PRIORITY_KEY, priorityString);
		map.put(DPID_KEY, dpid2);
		map.put(MATCH_KEY, match2);
		map.put(ACTIONS_KEY, actions2);
		return map;
	}

	// ----------------------------- auto
	// generated-----------------------------------
	public FlowChange() {
		// java beans constructor
	}

	/**
	 * Constructor
	 *
	 * @param operation
	 * @param index
	 * @param dpid
	 * @param match
	 * @param actionsList
	 */
	public FlowChange(FlowChangeOp operation, int id, int priority, long dpid,
			FVMatch match, List<OFAction> actions) {
		super();
		this.operation = operation;
		this.id = id;
		this.priority = priority;
		this.dpid = dpid;
		this.match = match;
		this.actions = actions;
	}

	public FlowChange(FlowChangeOp remove, Integer id) {
		this(remove, id, -1, -1, null, null);
	}

	/**
	 * @return the operation
	 */
	public FlowChangeOp getOperation() {
		return operation;
	}

	/**
	 * @param operation
	 *            the operation to set
	 */
	public void setOperation(FlowChangeOp operation) {
		this.operation = operation;
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
	 * @param priority
	 *            the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	
	/**
	 * @return the dpid
	 */
	public long getDpid() {
		return dpid;
	}

	/**
	 * @param dpid
	 *            the dpid to set
	 */
	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	/**
	 * @return the match
	 */
	public FVMatch getMatch() {
		return match;
	}

	/**
	 * @param match
	 *            the match to set
	 */
	public void setMatch(FVMatch match) {
		this.match = match;
	}

	/**
	 * @return the actionsList
	 */
	public List<OFAction> getActions() {
		return actions;
	}

	/**
	 * @param actionsList
	 *            the actionsList to set
	 */
	public void setActions(List<OFAction> actions) {
		this.actions = actions;
	}

	public int getPriority() {
		return this.priority;
	}
}
