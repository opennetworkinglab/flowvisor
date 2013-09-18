/**
 *
 */
package org.flowvisor.message.lldp;

import java.nio.ByteBuffer;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVPacketIn;
import org.flowvisor.message.FVPacketOut;
import org.openflow.util.StringByteSerializer;

/**
 * @author capveg
 *
 *         append a slice-identifying trailer to an lldp packet
 *
 *         2 bytes: tlv : type, length, value pair : (CHASISID)
 *
 *         1 byte : chasis subtype (LOCAL)
 *
 *         variable length, 0-term asci str: slice name
 *
 *         variable length, 0-term asci str: flowvisor name
 *
 *         1 byte: length of slice name str
 *
 *         1 byte: length of flowvisor name str
 *
 *         4 bytes: magic number to identify trailer
 *
 */
public class LLDPTrailer {
	String sliceName;
	String flowVisorName;
	/*short portNumber;
	byte[] hardwareAddress;
	
	public final static int CHASSIS_HEADER_LEN = 2;
	public final static int CHASSIS_SUBTYPE_LEN = 1;
	public final static int HARDWAREADDRESSLEN_LEN = 1;
	public final static int HARDWAREADDRESS_LEN = 6;
	
	public final static int PORT_HEADER_LEN = 2;
	public final static int PORT_SUBTYPE_LEN = 1;
	public final static int PORT_LEN = 2; //2 is the length of a short - correct?
	
	public final static int TTL_HEADER_LEN = 2;
	public final static int TTL_LEN = 2;
	*/
	public final static int OUI_HEADER_LEN = 2;
	public final static byte OUI_TYPE = 127;
	//Are we assuming that the slicename is within 255 characters?
	public final static int SLICENAMELEN_LEN = 1; 
	public final static int SLICENAMELEN_NULL = 1;
	public final static int FLOWNAMELEN_LEN = 1;
	public final static int FLOWNAMELEN_NULL = 1;
	
	public final static int END_LLDPDU_LEN = 2;
	
	public final static int MIN_LENGTH = 10;


	/*public LLDPTrailer(String sliceName, String flowVisorName, short portNum, byte[] hwAddr) {
		this.sliceName = sliceName;
		this.flowVisorName = flowVisorName;
		this.portNumber = portNum;
		this.hardwareAddress = hwAddr;
	}*/


	public LLDPTrailer(String sliceName, String flowVisorName) {
		this.sliceName = sliceName;
		this.flowVisorName = flowVisorName;
	}

	public String getSliceName() {
		return sliceName;
	}

	public void setSliceName(String sliceName) {
		this.sliceName = sliceName;
	}

	public String getFlowVisorName() {
		return flowVisorName;
	}

	public void setFlowVisorName(String flowVisorName) {
		this.flowVisorName = flowVisorName;
	}

	/**
	 * Append this trailer to the packet out; update the length and everything
	 *
	 * @param po
	 */
	public void appendTo(FVPacketOut po) {
		FVLog.log(LogLevel.DEBUG,null,"inside appendTo of LLDPTrailer" );
		int len = this.length();
		byte[] embedded = po.getPacketData();

		ByteBuffer newPacket = ByteBuffer.allocate(embedded.length + len);
		newPacket.put(embedded);

		//byte[] buf = new byte[len];
		//ByteBuffer bb = ByteBuffer.wrap(buf);

		// TLV type is 7 bits, length is 9
		// I precomputed them on byte boundaries to save time and sanity

		// NOX only supports Chassis ID subtype 1 and Port ID subtype 2
		// which we can't jam a full datapath ID into.
		// So we have to supply those here, and then overload the
		// System Decription TLV to dump in the datapath ID

		// Chassis ID TLV
		/*byte chassis[] = { 0x02, 0x07, // type =1, len=7 (1 + 6)
				0x04 }; // subtype = subtype MAC address
		newPacket.put(chassis);
		newPacket.put(hardwareAddress);
		//newPacket.putInt(hardwareAddress.length); //Should I add into the trailer or just assume=6?

		// Port ID TLV
		byte id[] = { 0x04, 0x03, // type 2, length 3
				0x02 }; // Subtype Port
		newPacket.put(id);
		newPacket.putShort(portNumber);

		// TTL TLV 
		byte ttl[] = { 0x06, 0x02, 0x00, 0x78 };
		newPacket.put(ttl); // type 3, length 2, 120 seconds*/

		//OUI TLV
		int ouiLen = 4 + sliceName.length()+ SLICENAMELEN_NULL + flowVisorName.length() + FLOWNAMELEN_NULL + SLICENAMELEN_LEN + FLOWNAMELEN_LEN;
																											//4 - length of OUI Id + it's subtype 
		int ouiHeader = (ouiLen & 0x1ff) | ((OUI_TYPE & 0x007f) << 9);
		newPacket.putShort((short)ouiHeader);
		
		//Change when ON.Lab gets a OUI, current assumption of OUI os 010101
		//with subtype as 01 too!
		
		//ByteBuffer ouiBB = ByteBuffer.allocate(3);
		//ouiBB.putInt(0xa42305);
		
		//byte oui[] = ouiBB.array(); 
		
		// ON.Labs OUI = a42305 and assigning the subtype to 0x01
		byte oui[] = { (byte)0xa4, (byte)0x23, (byte)0x05};
		newPacket.put(oui);
		byte ouiSubtype[] = { 0x01 };
		newPacket.put(ouiSubtype);
		
		//Append the OUI Information String = sliceName + flowVisorName 
		// + sliceNameLength + flowVisorNameLength to bytebuffer
		StringByteSerializer.writeTo(newPacket, sliceName.length() + 1,
				sliceName);
		StringByteSerializer.writeTo(newPacket, flowVisorName.length() + 1,
				flowVisorName);
		newPacket.put((byte) (sliceName.length() + 1));
		newPacket.put((byte) (flowVisorName.length() + 1));
		
		//EndOfLLDPDU TLV
		byte endType[] = { 0x00 };
		newPacket.put(endType);
		byte endLength[] = {0x00 };
		newPacket.put(endLength);
		
		//FVLog.log(LogLevel.DEBUG, null, "The trailer has a length equal to: ", bb.capacity());
		
		/*if (bb.capacity()<=len)
			FVLog.log(LogLevel.CRIT, null, "The trailer has a length less than expected.");*/
		
		//newPacket.put(bb);
		if(newPacket.capacity() < (embedded.length + len))
			FVLog.log(LogLevel.CRIT, null, "The length of the new packet is not as expected- "+newPacket.capacity());
		
		po.setPacketData(newPacket.array());
	}

	/**
	 * Checks if the LLDP trailer exists and if so, parses it and removes it
	 * from the packet
	 *
	 * @param po
	 * @return
	 */

	public static LLDPTrailer getTrailer(FVPacketIn pi) {
		FVLog.log(LogLevel.DEBUG,null,"inside getTrailer in LLDPTrailer" );
		String sliceName = new String();
		String fvName = new String();
		ByteBuffer packet = ByteBuffer.wrap(pi.getPacketData());
		if (packet.capacity() < MIN_LENGTH)
			return null;
		// work backwards through the trailer
		/*int offset = packet.capacity() - MAGIC_LEN;
		if (packet.getInt(offset) != MAGIC)
			return null; // didn't find MAGIC trailer*/
		
		try{
		FVLog.log(LogLevel.DEBUG, null, " packet capacity: ", packet.capacity());
		int offset = packet.capacity() - END_LLDPDU_LEN ;
		//FVLog.log(LogLevel.DEBUG, null, "offset0: ",offset);
		if (packet.get(offset) != 0){
			FVLog.log(LogLevel.WARN, null, "End of LLDPDU is missing");
			return null;//didn't find the End Trailer, so does not contain the rest too!
		}
		offset -= FLOWNAMELEN_LEN;
		//FVLog.log(LogLevel.DEBUG, null, "offset1: ",offset);
		byte flowLen = packet.get(offset);
		FVLog.log(LogLevel.DEBUG, null, "flowLen: ",flowLen);
		offset -= SLICENAMELEN_LEN;
		//FVLog.log(LogLevel.DEBUG, null, "offset2: ",offset);
		byte sliceLen = packet.get(offset);
		FVLog.log(LogLevel.DEBUG, null, "sliceLen: ",sliceLen);
		offset = offset - (flowLen + sliceLen); // this includes the NULL
		//FVLog.log(LogLevel.DEBUG, null, "offset3: ",offset);
		packet.position(offset);
		FVLog.log(LogLevel.MOBUG, null, "packet: ",packet.toString());
		sliceName = StringByteSerializer.readFrom(packet, sliceLen);
		fvName = StringByteSerializer.readFrom(packet, flowLen);
 
		//Check for the OUI Id and its subtype from backwards -
		offset -= 1;
		FVLog.log(LogLevel.DEBUG, null, " OUI Subtype: ",packet.get(offset));
		if(packet.get(offset) != 1) //0x01
			FVLog.log(LogLevel.ALERT, null, "OUI Subtype is wrong");
		offset -=1;
		FVLog.log(LogLevel.DEBUG, null, " OUI: ",packet.get(offset));
		if(packet.get(offset) != 5) //0x05
			FVLog.log(LogLevel.ALERT, null, "Wrong OUI0");
		offset -=1;
		FVLog.log(LogLevel.DEBUG, null, " OUI: ",packet.get(offset));
		if(packet.get(offset) != 35) //0x23
			FVLog.log(LogLevel.ALERT, null, "Wrong OUI1");
		offset -=1;
		FVLog.log(LogLevel.DEBUG, null, " OUI: ",packet.get(offset));
		if(packet.get(offset) != -92) //0xa4 = -92 
			FVLog.log(LogLevel.ALERT, null, "Wrong OUI2");
		}
		catch(IndexOutOfBoundsException ioe){
			FVLog.log(LogLevel.CRIT, null, "Yikes! The LLDP packet-in is not well formed - " +
					"		IndexOutOfBound while getting the trailer ",ioe);
		}
		catch(IllegalArgumentException iae){
			FVLog.log(LogLevel.CRIT, null, "Yikes! Got an illegal argument-something wrong " +
					"		with the positioning of the offset in the ByteBuffer! ",iae);
		}
		
		/*offset = offset - (OUI_HEADER_LEN + TTL_LEN + TTL_HEADER_LEN + PORT_LEN);
		short port = packet.getShort(offset);
		
		offset = offset - (PORT_SUBTYPE_LEN + PORT_HEADER_LEN + HARDWAREADDRESS_LEN);
		int hwAddrLen = packet.getInt(offset);
		
		offset -= hwAddrLen;
		byte[] hwAddress = new byte[HARDWAREADDRESS_LEN];
		packet.get(hwAddress,0,HARDWAREADDRESS_LEN);*/

		LLDPTrailer trailer = new LLDPTrailer(sliceName,fvName);
		//offset = offset - (CHASSIS_SUBTYPE_LEN + CHASSIS_HEADER_LEN);
		FVLog.log(LogLevel.DEBUG, null, "newPkt Len = ",packet.capacity() - trailer.length());
		byte[] newPacket = new byte[packet.capacity() - trailer.length()];
		packet.position(0);
		packet.get(newPacket);
		pi.setPacketData(newPacket);
		pi.setTotalLength((short) newPacket.length);
		FVLog.log(LogLevel.DEBUG, null, "newPacket.length = ",newPacket.length);
		return trailer;

		
	}

	public int length() {
		FVLog.log(LogLevel.MOBUG, null, "Slice Name = ", sliceName,
				"flowVisor Name = ", flowVisorName,
				"SliceLength = ", sliceName.length(),
				"FlowVisor Length = ", flowVisorName.length(),
				"SLICENAMELEN_NULL = ", SLICENAMELEN_NULL,
				"FLOWNAMELEN_NULL = ", FLOWNAMELEN_NULL,
				"SLICENAMELEN_LEN = ", SLICENAMELEN_LEN,
				"FLOWNAMELEN_LEN = ", FLOWNAMELEN_LEN
				);
		
		
		
		int ouiLen = 4 + sliceName.length()+ SLICENAMELEN_NULL + flowVisorName.length() +
				FLOWNAMELEN_NULL + SLICENAMELEN_LEN + FLOWNAMELEN_LEN; 
												//4 is the length of OUI ID + it's subtype.
		FVLog.log(LogLevel.DEBUG, null, "OUI has a length equal to: ", ouiLen);
		
		int trailerLen = OUI_HEADER_LEN + ouiLen + END_LLDPDU_LEN;
		
		
		/*int trailerLen = CHASSIS_HEADER_LEN + CHASSIS_SUBTYPE_LEN  + this.hardwareAddress.length 
				+ PORT_HEADER_LEN + PORT_SUBTYPE_LEN + PORT_LEN + TTL_HEADER_LEN 
				+ TTL_LEN + OUI_HEADER_LEN + ouiLen + END_LLDPDU_LEN;
		String hw = new String(this.hardwareAddress);*/
		FVLog.log(LogLevel.MOBUG, null, 
				"OUI_HEADER_LEN = ", OUI_HEADER_LEN,
				"END_LLDPDU_LEN = ", END_LLDPDU_LEN
				);
		
		
		
		FVLog.log(LogLevel.DEBUG, null, "The trailer has a length equal to: ", trailerLen);
		
		return trailerLen;
	}
}
