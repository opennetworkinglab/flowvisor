package org.flowvisor.message.actions;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionNetworkTypeOfService;

public class FVActionNetworkTypeOfService extends OFActionNetworkTypeOfService
		implements SlicableAction {

	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException {
		// TODO Auto-generated method stub
		FVLog.log(LogLevel.CRIT, fvSlicer,
				"action slicing unimplemented for type: " + this);
		approvedActions.add(this);
	}

}
