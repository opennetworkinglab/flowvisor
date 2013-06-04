/**
 *
 */
package org.flowvisor.message.lldp;

import java.nio.ByteBuffer;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVPacketIn;
import org.flowvisor.message.FVPacketOut;
import org.flowvisor.slicer.FVSlicer;

/**
 * Set of utilities for handling our LLDP virtualization hacks
 *
 * @author capveg
 *
 */
public class LLDPUtil {
	final public static short ETHER_LLDP = (short) 0x88cc;
	final public static short ETHER_VLAN = (short) 0x8100;
	final public static byte[] LLDP_MULTICAST = { 0x01, 0x23, 0x20, 0x00, 0x00,
			0x01 };
	final public static int MIN_FV_NAME = 20;

	/**
	 * If this msg is lldp, then 1) add a slice identifying trailer 2) send to
	 * switch -- all slices can send lldp, no matter flowspace 3) return true,
	 * we've handled this packet else return false
	 *
	 * @param po
	 *            message
	 * @param fvClassifier
	 *            switch classifier
	 * @param fvSlicer
	 *            slice polcies
	 * @return did we handle the message?
	 */
	static public boolean handleLLDPFromController(FVPacketOut po,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {

		FVLog.log(LogLevel.DEBUG,null,"inside handleLLDPFromController in LLDPUtil" );
		if (!LLDPCheck(po.getPacketData()))
			return false;
		String fvName = FlowVisor.getInstance().getInstanceName();
		FVLog.log(LogLevel.DEBUG,null,"fvName is: ",fvName, "sliceName is: ", fvSlicer.getSliceName() );
		/**
		 * This is a hack to ensure that the resulting lldp packet is larger
		 * than 60: #133
		 */
		/*if (fvName.length() < MIN_FV_NAME) // pad out to min length size
			fvName = String.format("%1$" + LLDPUtil.MIN_FV_NAME + "s", fvName);*/
		
		//Get the ports from the classifier and check if it belongs to that slice
		//and send only those ports relevant to this slice to form a new LLDP Trailer.
		/*List<OFPhysicalPort> ports = fvClassifier.getSwitchInfo().getPorts();
		
		
		//OFPhysicalPort port = (OFPhysicalPort)po.getInPort();
		FVLog.log(LogLevel.DEBUG,null,"inside The pkt is lldp." );
		
		short outport=-1000;
		List<OFAction> actions = po.getActions();
		for (OFAction act : actions) {
			if (act instanceof OFActionOutput) {
				OFActionOutput outact = (OFActionOutput) act;
				outport = outact.getPort();
				FVLog.log(LogLevel.DEBUG,null,"outport: ",outport );
			}
		}
		
		
		for (OFPhysicalPort port: ports){
			FVLog.log(LogLevel.DEBUG,null,"port is: ",port.getPortNumber() );
			//FVLog.log(LogLevel.DEBUG,null,"port is);
			//if (fvSlicer.portInSlice(port.getPortNumber()) && port.getPortNumber() == outport){ 
			if (port.getPortNumber() == outport){ 
				String hw = new String(port.getHardwareAddress());
				FVLog.log(LogLevel.DEBUG,null,"The ports are: ", port.getPortNumber());
				FVLog.log(LogLevel.DEBUG,null,"The hw addr is: ", hw );
				LLDPTrailer trailer = new LLDPTrailer(fvSlicer.getSliceName(), fvName, port.getPortNumber(), port.getHardwareAddress());
				FVLog.log(LogLevel.DEBUG, null, "DPID = ",fvClassifier.getDPID());
				trailer.appendTo(po);
			}
		}*/
		
		LLDPTrailer trailer = new LLDPTrailer(fvSlicer.getSliceName(), fvName);
		trailer.appendTo(po);
		FVLog.log(LogLevel.DEBUG, fvSlicer, "applied lldp hack: " + po
				+ " slice=" + fvSlicer.getSliceName());
		fvClassifier.sendMsg(po, fvSlicer);
		return true;
	}

	/**
	 * Is this an lldp packet?
	 *
	 * @param po
	 * @return
	 */

	static private boolean LLDPCheck(byte[] packetArray) {
		if ((packetArray == null) || (packetArray.length < 14))
			return false; // not lddp if no packet exists or too short
		ByteBuffer packet = ByteBuffer.wrap(packetArray);
		short ether_type = packet.getShort(12);
		FVLog.log(LogLevel.DEBUG,null,"Checking if the pkt is LLDP?", ether_type );
		if (ether_type == ETHER_VLAN)
			ether_type = packet.getShort(16);
		if (ether_type != ETHER_LLDP){
			FVLog.log(LogLevel.DEBUG,null,"The pkt is not LLDP" );
			return false;
		}
		// TODO think about checking for NOX OID
		return true;
	}

	/**
	 * If this msg is lldp, then 1) remove the slice identifying trailer 2) send
	 * to controller -- all slices can send lldp, no matter flowspace 3) return
	 * true, we've handled this packet
	 *
	 * @param po
	 * @param fvClassifier
	 * @return did we handle this message?
	 */
	static public boolean handleLLDPFromSwitch(FVPacketIn pi,
			FVClassifier fvClassifier) {
		FVLog.log(LogLevel.DEBUG,null,"inside handleLLDPFromSwitch in LLDPUtil" );
		if (!LLDPCheck(pi.getPacketData()))
			return false;
		LLDPTrailer trailer = LLDPTrailer.getTrailer(pi);
		if (trailer != null) {
			FVSlicer fvSlicer = fvClassifier.getSlicerByName(trailer
					.getSliceName());
			if (fvSlicer != null) {
				if (fvSlicer.isConnected()) {
					FVLog.log(LogLevel.DEBUG, fvSlicer, "undoing lldp hack: "
							+ pi);
					// TODO decide if we should call:
					// fvSlicer.setBufferIDAllowed(pi.getBufferId());
					// the pro is it allows controllers to do stuff their LLDPs
					// e.g., send them another hop
					// the con is that we would have to track more BUFFER_IDS
					fvSlicer.sendMsg(pi, fvClassifier);
				}
				return true;
			}
		}
		/**
		 * HACK: unknown LLDP packet; send to all slices that have access to
		 * this port
		 */
		if (trailer != null)
			FVLog.log(LogLevel.DEBUG, fvClassifier,
					"broadcasting b.c failed to undo llpd hack for unknown slice '"
							+ trailer.getSliceName() + "': " + pi);
		else
			FVLog.log(LogLevel.DEBUG, fvClassifier,
					"broadcasting b.c no lldp trailer found");
		short inport = pi.getInPort();
		pi.setXid(0xdeaddead); // mark this as broadcasted
		
		
		for (FVSlicer fvSlicer : fvClassifier.getSlicers()) {
				if (fvSlicer.portInSlice(inport) && fvSlicer.isConnected() && fvSlicer.isOptedIn())
					fvSlicer.sendMsg(pi, fvClassifier);
		}
		return true;
	}
}
