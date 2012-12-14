package org.flowvisor.message.actions;

import java.util.List;
import java.util.Set;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

/**
 * Allow/deny based on slice config if OFPP_ALL or OFPP_FLOOD, expand if
 * necessary
 * 
 * @author capveg
 * 
 */

public class FVActionOutput extends OFActionOutput implements SlicableAction,
		Cloneable {

	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException {
		if ((port >= 0)
				|| // physical port
				(port == OFPort.OFPP_LOCAL.getValue())
				|| (port == OFPort.OFPP_NORMAL.getValue())) {
			if (fvSlicer.portInSlice(port))
				approvedActions.add(this);
			else
				throw new ActionDisallowedException("port not in slice" + port, 
						OFBadActionCode.OFPBAC_BAD_OUT_PORT);
			return;
		}
		if ((port == OFPort.OFPP_ALL.getValue())
				|| (port == OFPort.OFPP_FLOOD.getValue()))
			expandPort(approvedActions, match, fvSlicer, fvClassifier);
		else if ((port == OFPort.OFPP_CONTROLLER.getValue())
				|| (port == OFPort.OFPP_TABLE.getValue()))
			approvedActions.add(this); // always allow CONTROLLER or TABLE
		else if (port == OFPort.OFPP_IN_PORT.getValue()) {
			short in_port = match.getInputPort();
			if (fvSlicer.portInSlice(in_port))
				approvedActions.add(this);
			else
				throw new ActionDisallowedException("in port not in slice"
						+ in_port, OFBadActionCode.OFPBAC_EPERM);
		} else {
			FVLog.log(LogLevel.CRIT, fvSlicer,
					"action slicing unimplemented for type: " + this);
			approvedActions.add(this);
		}
	}

	private void expandPort(List<OFAction> approvedActions, OFMatch match,
			FVSlicer fvSlicer, FVClassifier fvClassifier) {
		// potential short cut; if sending to all and all ports are allowed;
		// just approve
		if ((port == OFPort.OFPP_ALL.getValue())
				&& (fvSlicer.isAllowAllPorts())) {
			approvedActions.add(this);
			return;
		}

		// short cut if we have perms to do a native flood
		if (port == OFPort.OFPP_FLOOD.getValue()) {
			if (fvSlicer.hasFloodPerms()) {
				approvedActions.add(this);
				return;
			} else {
				if (fvClassifier.getFloodPermsSlice().equals(
						fvSlicer.getSliceName())) {
					fvSlicer.setFloodPerms(true);
					turnOffOutOfSliceFloodBits(fvSlicer, fvClassifier);
					approvedActions.add(this);
					return;
				} else {
					FVLog.log(LogLevel.DEBUG, fvClassifier,
							"slice has no flood perms: "
									+ fvSlicer.getSliceName() + "!='"
									+ fvClassifier.getFloodPermsSlice() + "'");
				}
			}
		}

		Set<Short> portList;
		if (port == OFPort.OFPP_ALL.getValue())
			portList = fvSlicer.getPorts();
		else if (port == OFPort.OFPP_FLOOD.getValue())
			portList = fvSlicer.getFloodPorts();
		else
			throw new RuntimeException(
					"called expandPorts with non-FLOOD/ALL port: " + port);
		for (Short fPort : portList) {
			if (fPort.equals(Short.valueOf(match.getInputPort())))
				continue; // don't expand to input port! cause bad loops, things
			// go boom
			if (!fvClassifier.isPortActive(fPort))
				continue; // don't expand to inactive ports
			try {
				if (fvSlicer.getFloodPortStatus(fPort)) {
					FVActionOutput neoOut = this.clone();
					neoOut.setPort(fPort);
					approvedActions.add(neoOut);
				}
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(
						"silly java... I *do* implement cloneable");
			}
		}

	}

	private void turnOffOutOfSliceFloodBits(FVSlicer fvSlicer,
			FVClassifier fvClassifier) {
		FVLog.log(LogLevel.ALERT, fvClassifier,
				"Would be turning off flooding ports for slice "
						+ fvSlicer.getSliceName() + " but its NOT IMPLEMENTED");
		/**
		 * TODO Need to send OFPortMod msgs to turn off the flood bit for all
		 * ports NOT in this slice
		 */
	}

	@Override
	public FVActionOutput clone() throws CloneNotSupportedException {
		return (FVActionOutput) super.clone();
	}

	@Override
	public String toString() {
		return super.toString() + ";port=" + port;
	}
}
