/**
 *
 */
package org.flowvisor.flows;

import java.util.Set;
import java.util.TreeSet;

import org.flowvisor.events.FVEventHandler;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

/**
 * This is a stub class that's loaded when flow tracking is disabled
 *
 * @author capveg
 *
 */
public class NoopFlowRewriteDB implements FlowRewriteDB {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	FVEventHandler fvEventHandler;
	Set<FlowDBEntry> set;

	public NoopFlowRewriteDB(FVEventHandler fvEventHandler, String sliceName,
			long dpid) {
		this.fvEventHandler = fvEventHandler;
		this.set = new TreeSet<FlowDBEntry>();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.flows.FlowRewriteDB#processFlowMods(org.flowvisor.message
	 * .FVFlowMod, org.flowvisor.message.FVFlowMod)
	 */
	@Override
	public void processFlowMods(OFFlowMod original, OFFlowMod rewrite) {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.flows.FlowRewriteDB#processFlowRemoved(org.flowvisor.message
	 * .FVFlowRemoved)
	 */
	@Override
	public void processFlowRemoved(OFFlowRemoved flowRemoved) {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.flows.FlowRewriteDB#originals()
	 */
	@Override
	public Set<FlowDBEntry> originals() {
		return this.set; // return set with zero entries
	}

	/*
	 * (non-Javadoc) Always returns null
	 *
	 * @see
	 * org.flowvisor.flows.FlowRewriteDB#getRewrites(org.flowvisor.flows.FlowDBEntry
	 * )
	 */
	@Override
	public FlowDB getRewrites(FlowDBEntry original) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.flows.FlowRewriteDB#size()
	 */
	@Override
	public int size() {
		return 0;
	}

}
