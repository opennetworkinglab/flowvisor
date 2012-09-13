/**
 *
 */
package org.flowvisor.message.lldp;

import java.nio.ByteBuffer;

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
	public final static int MAGIC = 0xdeadcafe;
	public final static byte LLDP_CHASSIS_ID = 1;
	public final static byte LLDP_CHASSIS_ID_LOCAL = 7;
	public final static int MIN_LENGTH = 10;
	public final static int MAGIC_LEN = 4;
	public final static int SLICENAMELEN_LEN = 1;
	public final static int SLICENAMELEN_NULL = 1;

	public final static int FLOWNAMELEN_LEN = 1;
	public final static int FLOWNAMELEN_NULL = 1;
	public final static int TLV_LEN = 2;
	public final static int CHASSIS_ID_LEN = 1;
	public final static int TRAILER_HEADER_LEN = MAGIC_LEN + SLICENAMELEN_LEN
			+ SLICENAMELEN_NULL + FLOWNAMELEN_LEN + FLOWNAMELEN_NULL + TLV_LEN
			+ CHASSIS_ID_LEN;
	String sliceName;
	String flowVisorName; // for cross-aggregate federated GENI identification

	public LLDPTrailer(String sliceName) {
		this.sliceName = sliceName;
		this.flowVisorName = "";
	}

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

		int len = this.length();
		byte[] embedded = po.getPacketData();

		ByteBuffer newPacket = ByteBuffer.allocate(embedded.length + len);
		newPacket.put(embedded);

		int tlv = (len & 0x1ff) | ((LLDP_CHASSIS_ID & 0x007f) << 9);
		newPacket.putShort((short) tlv);

		newPacket.put(LLDP_CHASSIS_ID_LOCAL);

		StringByteSerializer.writeTo(newPacket, sliceName.length() + 1,
				sliceName);
		StringByteSerializer.writeTo(newPacket, flowVisorName.length() + 1,
				flowVisorName);

		newPacket.put((byte) (sliceName.length() + 1));
		newPacket.put((byte) (flowVisorName.length() + 1));
		newPacket.putInt(MAGIC);

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
		ByteBuffer packet = ByteBuffer.wrap(pi.getPacketData());
		if (packet.capacity() < MIN_LENGTH)
			return null;
		// work backwards through the trailer
		int offset = packet.capacity() - MAGIC_LEN;
		if (packet.getInt(offset) != MAGIC)
			return null; // didn't find MAGIC trailer
		offset -= FLOWNAMELEN_LEN;
		byte flowLen = packet.get(offset);
		offset -= SLICENAMELEN_LEN;
		byte sliceLen = packet.get(offset);
		offset -= flowLen + sliceLen; // this includes the NULL
		packet.position(offset);
		LLDPTrailer trailer = new LLDPTrailer(StringByteSerializer.readFrom(
				packet, sliceLen), StringByteSerializer.readFrom(packet,
				flowLen));
		byte[] newPacket = new byte[packet.capacity() - trailer.length()];
		packet.position(0);
		packet.get(newPacket);
		pi.setPacketData(newPacket);
		pi.setTotalLength((short) newPacket.length);
		return trailer;
	}

	public int length() {
		return TRAILER_HEADER_LEN + this.sliceName.length()
				+ this.flowVisorName.length();
	}
}
