package org.flowvisor.message;

import java.util.Arrays;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;

/**
 * Verify that this packet_out operation is allowed by slice definition, in
 * terms of destination port, the flowspace of the embedded packet, the
 * buffer_id, and the actions.
 *
 * Send an error msg back to controller if it's not
 *
 * @author capveg
 *
 */

public class FVPacketOut extends OFPacketOut implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {

		// make sure that this slice can access this bufferID
		if (! fvSlicer.isBufferIDAllowed(this.getBufferId())) {
			FVLog.log(LogLevel.WARN, fvSlicer,
					"EPERM buffer_id ", this.getBufferId(), " disallowed: "
							, this.toVerboseString());
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
						OFBadRequestCode.OFPBRC_BUFFER_UNKNOWN, this), fvSlicer);
			return;
		}

		// if it's LLDP, pass off to the LLDP hack
		if (LLDPUtil.handleLLDPFromController(this, fvClassifier, fvSlicer))
			return;
		FVMatch match = new FVMatch();
		byte[] packet = this.getPacketData();
		if (packet != null && packet.length > 0) {
			try {
				match.loadFromPacket(this.getPacketData(), OFPort.OFPP_ALL
						.getValue());
				// TODO : for efficiency, do this lookup on the slice flowspace,
				// not the switch
				List<FlowEntry> flowEntries = fvClassifier.getSwitchFlowMap()
						.matches(fvClassifier.getSwitchInfo().getDatapathId(),
								match);
				if ((flowEntries == null) // got no response
						|| (flowEntries.size() < 1) // nothing matched
						// has write permissions
						|| (!flowEntries.get(0).hasPermissions(
								fvSlicer.getSliceName(), SliceAction.WRITE))
				// TODO add buffer_id check here
				) {
					FVLog
							.log(LogLevel.WARN, fvSlicer,
									"EPERM packet not in flowspace: "
											+ this.toVerboseString());
					fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
							OFBadActionCode.OFPBAC_EPERM, this), fvSlicer);
					return;
				}
			} catch (java.nio.BufferUnderflowException e) {
				// packet was too short to match entire header; just ignore
				FVLog.log(LogLevel.CRIT, fvSlicer,
						"couldn't parse short packet: "
								+ HexString.toHexString(this.getPacketData())
								+ " :: " + e.getStackTrace());
			}
		}
		List<OFAction> actionsList = this.getActions();
		match.setInputPort(this.getInPort());
		try {
			actionsList = FVMessageUtil.approveActions(actionsList, match,
					fvClassifier, fvSlicer);
		} catch (ActionDisallowedException e) {
			FVLog.log(LogLevel.WARN, fvSlicer, "EPERM bad actions: " + this);

			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					e.getError(), this), fvSlicer);
			return;
		}

		this.setActions(actionsList);
		// really annoying; should be in the base class
		short count = FVMessageUtil.countActionsLen(actionsList);
		this.setActionsLength(count);
		this.setLength((short) (FVPacketOut.MINIMUM_LENGTH + count + this
				.getPacketData().length));
		// if we've gotten this far, everything is kosher
		fvClassifier.sendMsg(this, fvSlicer);
	}

	// convenience function that Derickso doesn't want in main openflow.jar
	@Override
	public FVPacketOut setPacketData(byte[] packetData) {
		if (packetData == null)
			this.length = (short) (MINIMUM_LENGTH + actionsLength);
		else
			this.length = (short) (MINIMUM_LENGTH + actionsLength + packetData.length);
		this.packetData = packetData;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
    public String toString() {
        return "FVPacketOut [actions="
                + FVMessageUtil.actionsToString(this.getActions()) + ", actionsLength=" + actionsLength + ", " +
                " inPort=" + inPort + ", packetData=" +
                 Arrays.toString(packetData) + "]";
    }
	
	
	private String toVerboseString() {
		String pkt;
		if (this.packetData != null && (this.packetData.length > 0))
			pkt = new OFMatch().loadFromPacket(this.packetData, this.inPort)
					.toString();
		else
			pkt = "empty";
		return this.toString() + ";pkt=" + pkt;
	}

}
