/**
 *
 */
package org.flowvisor.flows;

import java.util.Iterator;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

/**
 * An FV-local copy of the flow table state on the switch
 *
 * track flow_mods and flow_expire messages
 *
 * @author capveg
 *
 */
public interface FlowDB extends Iterable<FlowDBEntry> {
	/**
	 * Update the contents of the FlowDB with
	 *
	 * @param flowMod
	 * @param Slicename
	 */
	public void processFlowMod(OFFlowMod flowMod, long dpid, String sliceName);

	/**
	 * Update the database with a flow_removed message
	 *
	 * @param flowRemoved
	 *            the new information
	 * @param dpid
	 *            the switch it came from
	 * @return the name of the slice that inserted the flow or null if none
	 */
	public String processFlowRemoved(OFFlowRemoved flowRemoved, long dpid);

	public Iterator<FlowDBEntry> iterator();

	public int size();
}
