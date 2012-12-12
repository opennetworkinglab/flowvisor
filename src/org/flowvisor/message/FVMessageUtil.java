/**
 *
 */
package org.flowvisor.message;

import java.util.ArrayList;
import java.util.List;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.XidPair;
import org.flowvisor.classifier.XidPairWithMessage;
import org.flowvisor.classifier.XidTranslator;
import org.flowvisor.classifier.XidTranslatorWithMessage;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.actions.SlicableAction;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFError.OFPortModFailedCode;
import org.openflow.protocol.action.OFAction;

/**
 * @author capveg
 *
 */
public class FVMessageUtil {

	/**
	 * Translate the XID of a message from controller-unique to switch unique
	 * Also, record the <oldXid,FVSlicer> mapping so we can reverse this later
	 *
	 * @param msg
	 * @param fvClassifier
	 * @param fvSlicer
	 */
	static public void translateXid(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		XidTranslator xidTranslator = fvClassifier.getXidTranslator();
		int newXid = xidTranslator.translate(msg.getXid(), fvSlicer);
		msg.setXid(newXid);
	}
	
	
	
	public static void translateXidMsg(FVStatisticsRequest msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		XidTranslatorWithMessage xidTranslator = (XidTranslatorWithMessage) fvClassifier.getXidTranslator();
		int newXid = xidTranslator.translate(msg.clone(), msg.getXid(), fvSlicer);
		msg.setXid(newXid);
	}

	/**
	 * Undo the effect of translateXID, and return the FVSlicer this came from
	 *
	 * @param msg
	 * @param fvClassifier
	 * @return the fvSlicer that was input in the translate step or null if not
	 *         found
	 */
	 static public FVSlicer untranslateXid(OFMessage msg,
			FVClassifier fvClassifier) {
		XidTranslator xidTranslator = fvClassifier.getXidTranslator();
		XidPair pair = xidTranslator.untranslate(msg.getXid());
		if (pair == null)
			return null;
		msg.setXid(pair.getXid());
		String sliceName = pair.getSliceName();
		return fvClassifier.getSlicerByName(sliceName);
	}
	 
	 
	 static public XidPairWithMessage untranslateXidMsg(OFMessage msg, 
			 FVClassifier fvClassifier) {
		 XidTranslatorWithMessage xidTranslator = (XidTranslatorWithMessage) fvClassifier.getXidTranslator();
		 XidPairWithMessage pair = xidTranslator.untranslate(msg.getXid());
		 if (pair == null)
			 return null;
		 pair.setSlicer( fvClassifier.getSlicerByName(pair.getSliceName()));
		 return pair;
	 }

	/**
	 * Is this slice allowed to use this list of actions with this ofmatch
	 * structure?
	 *
	 * Return a (potentially edited) list of actions or throw an exception if
	 * not allowed
	 *
	 * @param actionList
	 * @param match
	 *            inPort is encapsulated in the match
	 * @param fvClassifier
	 * @param fvSlicer
	 * @return A list of actions the slice is actually allowed to send
	 * @throws ActionDisallowedException
	 */
	static public List<OFAction> approveActions(List<OFAction> actionList,
			OFMatch match, FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException {
		List<OFAction> approvedList = new ArrayList<OFAction>();

		if (actionList == null)
			return null;
		for (OFAction action : actionList)
			((SlicableAction) action).slice(approvedList, match, fvClassifier,
					fvSlicer);
		return approvedList;
	}

	public static short countActionsLen(List<OFAction> actionsList) {
		short count = 0;
		for (OFAction act : actionsList)
			count += act.getLength();
		return count;
	}

	public static void translateXidAndSend(OFMessage msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.translateXid(msg, fvClassifier, fvSlicer);
		fvClassifier.sendMsg(msg, fvSlicer);
	}
	
	public static void translateXidMsgAndSend(
			FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidMsg(msg, fvClassifier, fvSlicer);
		fvClassifier.sendMsg(msg, fvSlicer);
	}



	public static void dropUnexpectedMesg(OFMessage msg, FVEventHandler handler) {
		FVLog.log(LogLevel.WARN, handler, "dropping unexpected msg: " + msg);
	}

	public static void untranslateXidAndSend(OFMessage msg,
			FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping msg with unknown xid: " + msg);
			return;
		}
		FVLog.log(LogLevel.DEBUG, fvSlicer, "sending to controller: " + msg);
		fvSlicer.sendMsg(msg, fvClassifier);
	}

	public static OFMessage makeErrorMsg(OFFlowModFailedCode code, OFMessage msg) {
		OFError err = (OFError) FlowVisor.getInstance().getFactory()
				.getMessage(OFType.ERROR);
		err.setErrorType(OFErrorType.OFPET_FLOW_MOD_FAILED);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);
		return err;
	}

	public static OFMessage makeErrorMsg(OFPortModFailedCode code, OFMessage msg) {
		OFError err = (OFError) FlowVisor.getInstance().getFactory()
				.getMessage(OFType.ERROR);
		err.setErrorType(OFErrorType.OFPET_PORT_MOD_FAILED);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);
		return err;
	}

	public static OFMessage makeErrorMsg(OFBadRequestCode code, OFMessage msg) {
		OFError err = (OFError) FlowVisor.getInstance().getFactory()
				.getMessage(OFType.ERROR);
		err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);
		return err;
	}

	public static OFMessage makeErrorMsg(OFBadActionCode code, OFMessage msg) {

		OFError err = (OFError) FlowVisor.getInstance().getFactory()
				.getMessage(OFType.ERROR);
		err.setErrorType(OFErrorType.OFPET_BAD_ACTION);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);
		return err;
	}

	public static String actionsToString(List<OFAction> actions) {
		if ((actions == null) || (actions.size() == 0))
			return "DROP";
		String ret = "";
		for (OFAction action : actions) {
			if (!ret.equals(""))
				ret += ",";
			ret += action.toString();
		}
		return ret;
	}

	
}
