package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortMod;
import org.openflow.protocol.OFError.OFPortModFailedCode;

public class FVPortMod extends OFPortMod implements Classifiable, Slicable {

	/**
	 * Send to all slices with this port
	 *
	 * FIXME: decide if port_mod's can come *up* from switch?
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVLog.log(LogLevel.DEBUG, fvClassifier, "recv from switch: " + this);
		for (FVSlicer fvSlicer : fvClassifier.getSlicers())
			if (fvSlicer.portInSlice(this.portNumber))
				fvSlicer.sendMsg(this, fvClassifier);
	}

	/**
	 * First, check to see if this port is available in this slice Second, check
	 * to see if they're changing the FLOOD bit FIXME: prevent slices from
	 * administratrively bringing down a port!
	 */
	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// First, check if this port is in the slice
		if (!fvSlicer.portInSlice(this.portNumber)) {
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					OFPortModFailedCode.OFPPMFC_BAD_PORT, this), fvClassifier);
			return;
		}
		// Second, update the port's flood state
//		boolean oldValue = fvSlicer.getFloodPortStatus(this.portNumber);
		fvSlicer.setFloodPortStatus(this.portNumber,
				(this.mask & OFPhysicalPort.OFPortConfig.OFPPC_NO_FLOOD
						.getValue()) == 0);
//		if (oldValue != fvSlicer.getFloodPortStatus(this.portNumber))
//			FVLog.log(LogLevel.CRIT, fvSlicer,
//					"FIXME: need to implement FLOODING port changes");
	}
}
