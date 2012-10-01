/**
 *
 */
package org.flowvisor.flows;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

/**
 * implement the FlowDB interface, but do nothing used when FlowTracking is
 * disabled because it's easier and cleaner than having a lot of if/then's in
 * the code
 *
 * @author capveg
 *
 */
public class NoopFlowDB implements FlowDB {

	List<FlowDBEntry> emptyList;

	public NoopFlowDB() {
		this.emptyList = new LinkedList<FlowDBEntry>();
	}

	@Override
	public void processFlowMod(OFFlowMod flowMod, long dpid, String sliceName) {
		// do nothing
	}

	@Override
	public String processFlowRemoved(OFFlowRemoved flowRemoved, long dpid) {
		// do nothing
		return null;
	}

	@Override
	public Iterator<FlowDBEntry> iterator() {
		return emptyList.iterator();
	}

	@Override
	public int size() {
		return 0;
	}

}
