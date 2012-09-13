package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFPortStatus;

/**
 * Send the port status message to each slice that uses this port
 *
 * @author capveg
 *
 */

public class FVPortStatus extends OFPortStatus implements Classifiable,
		Slicable, TopologyControllable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		Short port = Short.valueOf(this.getDesc().getPortNumber());

		byte reason = this.getReason();
		boolean updateSlicers = false;

		if (reason == OFPortReason.OFPPR_ADD.ordinal()) {
			FVLog.log(LogLevel.INFO, fvClassifier, "dynamically adding port "
					+ port);
			fvClassifier.addPort(this.getDesc()); // new port dynamically added
			updateSlicers = true;
		} else if (reason == OFPortReason.OFPPR_DELETE.ordinal()) {
			FVLog.log(LogLevel.INFO, fvClassifier, "dynamically removing port "
					+ port);
			fvClassifier.removePort(this.getDesc());
			updateSlicers = true;
		} else if (reason == OFPortReason.OFPPR_MODIFY.ordinal()) {
			// replace/update the port definition
			FVLog.log(LogLevel.INFO, fvClassifier, "modifying port " + port);
			//fvClassifier.removePort(this.getDesc());
			/*
			 * ash: addPort actually removes the port first.
			 */
			fvClassifier.addPort(this.getDesc());
		} else {
			FVLog.log(LogLevel.CRIT, fvClassifier, "unknown reason " + reason
					+ " in port_status msg: " + this);
		}

		if (updateSlicers) {
			for (FVSlicer fvSlicer : fvClassifier.getSlicers()) {
				/*
				 * Ugly call to update flowspace when using a linear flowspace
				 * this WILL go when the linear flowspace goes.
				 */
				fvSlicer.updateFlowSpace();
			}
		}

		for (FVSlicer fvSlicer : fvClassifier.getSlicers()) {
			if (fvSlicer.portInSlice(port)) {
				fvSlicer.sendMsg(this, fvClassifier);
			}
		}
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	/**
	 * Got a dynamically added/removed port,e.g., from an HP add or remove it
	 * from the list of things we poke for topology
	 */
	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		if (this.reason == OFPortReason.OFPPR_ADD.ordinal())
			topologyConnection.addPort(this.getDesc());
		else if (this.reason == OFPortReason.OFPPR_DELETE.ordinal())
			topologyConnection.removePort(this.getDesc());
	}
}
