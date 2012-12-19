package org.flowvisor.message;

import java.util.LinkedList;
import java.util.List;

import org.flowvisor.classifier.CookieTranslator;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowIntersect;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionEnqueue;
import org.openflow.protocol.action.OFActionOutput;

public class FVFlowMod extends org.openflow.protocol.OFFlowMod implements
		Classifiable, Slicable, Cloneable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	/**
	 * FlowMod slicing
	 *
	 * 1) make sure all actions are ok
	 *
	 * 2) expand this FlowMod to the intersection of things in the given match
	 * and the slice's flowspace
	 */

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
	
		FVLog.log(LogLevel.DEBUG, fvSlicer, "recv from controller: ", this);
		FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);
		translateCookie(fvClassifier, fvSlicer);
		
		// make sure that this slice can access this bufferID
		if ((this.command != OFFlowMod.OFPFC_DELETE 
				&& this.command != OFFlowMod.OFPFC_DELETE_STRICT) 
				&& !fvSlicer.isBufferIDAllowed(this.getBufferId())) {
			FVLog.log(LogLevel.WARN, fvSlicer,
					"EPERM buffer_id ", this.getBufferId(), " disallowed: "
							, this);
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
						OFBadRequestCode.OFPBRC_BUFFER_UNKNOWN, this), fvSlicer);
			return;
		}
		
		// make sure the list of actions is kosher
		List<OFAction> actionsList = this.getActions();
		try {
			actionsList = FVMessageUtil.approveActions(actionsList, this.match,
					fvClassifier, fvSlicer);
		} catch (ActionDisallowedException e) {
			
			FVLog.log(LogLevel.WARN, fvSlicer, "EPERM bad actions: ", this);
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					e.getError(), this), fvSlicer);
			return;
		}
		// expand this match to everything that intersects the flowspace
		List<FlowIntersect> intersections = fvSlicer.getFlowSpace().intersects(
				fvClassifier.getDPID(), new FVMatch(getMatch()));

		int expansions = 0;
		OFFlowMod original = this.clone(); // keep an unmodified copy
		
		
		int oldALen = FVMessageUtil.countActionsLen(this.getActions());
		this.setActions(actionsList);
		// set new length as a function of old length and old actions length
		this.setLength((short) (getLength() - oldALen + FVMessageUtil
				.countActionsLen(actionsList)));
		
		for (FlowIntersect intersect : intersections) {
				if (intersect.getFlowEntry().hasPermissions(
						fvSlicer.getSliceName(), SliceAction.WRITE)) {
					expansions++;
					FVFlowMod newFlowMod = (FVFlowMod) this.clone();
					/*
					 * Only first expansion gets a bufferid, others will 
					 * be set to none.
					 */
					if (expansions > 1)
						newFlowMod.setBufferId(-1);
					// replace match with the intersection
					newFlowMod.setMatch(intersect.getMatch());
					// update flowDBs
					fvSlicer.getFlowRewriteDB().processFlowMods(original,
							newFlowMod);
					fvClassifier.getFlowDB().processFlowMod(newFlowMod,
							fvClassifier.getDPID(), fvSlicer.getSliceName());
					// actually send msg
					/*if (fvClassifier.isFlowTracking() && !((this.flags & 1) != 0))
						*/
					/*
					 * FIXME: THIS HAS TO BE VIRTUALIZED!!!!!!!
					 */
					newFlowMod.flags = (short) (newFlowMod.flags & 1);
					if(this.command == OFFlowMod.OFPFC_DELETE || this.command == OFFlowMod.OFPFC_DELETE_STRICT){
						fvSlicer.decrementFlowRules();
					}else if(this.command == OFFlowMod.OFPFC_ADD){
						FVLog.log(LogLevel.WARN,fvSlicer,"Verifying Slice is not over its flow rule limit");
						if (!fvSlicer.permitFlowMod()){
							FVLog.log(LogLevel.WARN,fvSlicer,"Slice is already at flow rule limit");
							fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(OFFlowModFailedCode.OFPFMFC_EPERM, this), fvClassifier);
							return;
						}
						//increment the flow rule
						fvSlicer.incrementFlowRules();
					}else if(this.command == OFFlowMod.OFPFC_MODIFY_STRICT || this.command == OFFlowMod.OFPFC_MODIFY_STRICT){
						//do nothing
						//this is modifying existing flows not adding/subtracting
					}
					
					/*
					 * Iterates over the list of actions
					 * if the FV rule forces an enqueue action
					 * apply it. Otherwise change nothing.
					 */
					applyForceEnqueue(newFlowMod, intersect.getFlowEntry());
					
					fvClassifier.sendMsg(newFlowMod, fvSlicer);
				}
		}
		

		if (expansions == 0) {
			FVLog.log(LogLevel.WARN, fvSlicer, "dropping illegal fm: ", this);
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					OFFlowModFailedCode.OFPFMFC_EPERM, this), fvSlicer);
		} else
			FVLog.log(LogLevel.DEBUG, fvSlicer, "expanded fm ", expansions,
					" times: ", this);
	}
	

	private void applyForceEnqueue(FVFlowMod newFlowMod, FlowEntry flowEntry) {
		if (!flowEntry.forcesEnqueue())
			return;
		List<OFAction> neoActions = new LinkedList<OFAction>();
		int length = 0;
		for (OFAction action : newFlowMod.actions) {
			if (action instanceof OFActionOutput) {
				OFActionOutput output = (OFActionOutput) action;
				OFActionEnqueue repl = new OFActionEnqueue();
				repl.setPort(output.getPort());
				repl.setQueueId((int)flowEntry.getForcedQueue());
				neoActions.add(repl);
				length += repl.getLengthU();
			} else {
				neoActions.add(action);
				length += action.getLengthU();
			}
		}
		newFlowMod.setActions(neoActions);
		newFlowMod.setLengthU(FVFlowMod.MINIMUM_LENGTH + length);
	}
	
	
	private void translateCookie(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		CookieTranslator cookieTrans = fvClassifier.getCookieTranslator();
		long newCookie = cookieTrans.translate(this.cookie, fvSlicer);
		this.setCookie(newCookie);
	}
	
	

	public FVFlowMod setMatch(FVMatch match) {
		this.match = match;
		return this;
	}
	
	@Override
	public OFMatch getMatch() {
		return this.match;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + ";actions="
				+ FVMessageUtil.actionsToString(this.getActions());
	}
}
