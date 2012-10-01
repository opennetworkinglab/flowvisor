/**
 *
 */
package org.flowvisor.message.actions;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

/**
 * @author capveg
 *
 */
public interface SlicableAction {

	/**
	 * See if this action is allowed in the slice definition, given this match
	 * If yes, then write it to the approvedActions list (possibly rewritten) or
	 * expanded
	 *
	 * @param approvedActions
	 *            list of already approved actions
	 * @param match
	 *            the context the action is used in ; the inport is set to the
	 *            inport of the packet_out
	 * @param fvClassifier
	 *            switch definition
	 * @param fvSlicer
	 *            slice definition
	 * @throws if the action is not allowed
	 */

	public void slice(List<OFAction> approvedActions, OFMatch match,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException;
}
