package org.flowvisor.message.actions;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionEnqueue;

public class FVActionEnqueue extends OFActionEnqueue implements SlicableAction {

	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException {
		
		/*
		 * Match OFMatch, if flowentry has queue id then OK. 
		 */
		FVMatch neoMatch = new FVMatch(match);
		neoMatch.setInputPort(this.port);
		List<FlowEntry> entries = fvSlicer.getFlowSpace().matches(fvClassifier.getDPID(), neoMatch);
		for (FlowEntry fe : entries) {
			for (OFAction act : fe.getActionsList()) {
				SliceAction sa = (SliceAction) act;
				if (sa.getSliceName().equals(fvSlicer.getSliceName()) && 
						fe.getQueueId().contains(this.queueId)) {
					approvedActions.add(this);
					return;
				}
			}
		}
		throw new ActionDisallowedException("Slice " + 
				fvSlicer.getSliceName() + " may not enqueue to queue " + this.queueId
				+ " for port " + this.port,
				OFBadActionCode.OFPBAC_BAD_QUEUE);
		
	}

}
