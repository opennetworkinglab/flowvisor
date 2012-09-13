/**
 *
 */
package org.flowvisor.api;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.exceptions.MapUnparsable;
import org.flowvisor.flows.FlowSpaceUtil;

/**
 *
 * @author capveg
 *
 */
public class LinkAdvertisement {
	String srcDPID;
	short srcPort;
	String dstDPID;
	short dstPort;
	// list of key=value pairs, for extensibility
	HashMap<String, String> attributes;

	protected LinkAdvertisement() {
		// do nothing, for a java bean
	}

	public LinkAdvertisement(String srcDPID, short srcPort, String dstDPID,
			short dstPort) {
		super();
		this.srcDPID = srcDPID;
		this.srcPort = srcPort;
		this.dstDPID = dstDPID;
		this.dstPort = dstPort;
		this.attributes = new HashMap<String, String>();
	}

	public LinkAdvertisement(long srcDPID, short srcPort, long dstDPID,
			short dstPort) {
		this(FlowSpaceUtil.dpidToString(srcDPID), srcPort, FlowSpaceUtil
				.dpidToString(dstDPID), dstPort);
	}

	public String getSrcDPID() {
		return srcDPID;
	}

	public void setSrcDPID(String srcDPID) {
		this.srcDPID = srcDPID;
	}

	public short getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(short srcPort) {
		this.srcPort = srcPort;
	}

	public String getDstDPID() {
		return dstDPID;
	}

	public void setDstDPID(String dstDPID) {
		this.dstDPID = dstDPID;
	}

	public short getDstPort() {
		return dstPort;
	}

	public void setDstPort(short dstPort) {
		this.dstPort = dstPort;
	}

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}

	public void setAttribute(String key, String value) {
		if (this.attributes == null)
			this.attributes = new HashMap<String, String>();
		this.attributes.put(key, value);
	}

	/**
	 * I *SWEAR* XMLRPC is supposed to be able to handle this for me... :-(
	 *
	 * @return a key=value paired map of information on this link
	 */

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("srcDPID", srcDPID);
		map.put("srcPort", String.valueOf(srcPort));
		map.put("dstDPID", dstDPID);
		map.put("dstPort", String.valueOf(dstPort));
		String attribs = "";
		for (String attrib : attributes.keySet()) {
			if (!attribs.equals(""))
				attribs += ",";
			attribs += attrib + "=" + attributes.get(attrib);
		}
		if (!attribs.equals("")) // only add an "attributes" if non-empty
			map.put("attributes", attribs);
		return map;
	}

	static public boolean checkKey(String keyname, Map<String, String> map)
			throws MapUnparsable {
		if (!map.containsKey(keyname))
			throw new MapUnparsable("key not found: " + keyname);
		return true;
	}

	static public LinkAdvertisement fromMap(Map<String, String> map)
			throws MapUnparsable {
		LinkAdvertisement ad;

		checkKey("srcDPID", map);
		checkKey("dstDPID", map);
		checkKey("srcPort", map);
		checkKey("dstPort", map);

		ad = new LinkAdvertisement(map.get("srcDPID"), Short.valueOf(map
				.get("srcPort")), map.get("dstDPID"), Short.valueOf(map
				.get("dstPort")));
		if (map.containsKey("attributes")) {
			String[] attribs = map.get("attributes").split(",");
			for (int i = 0; i < attribs.length; i++) {
				String[] keyvalue = attribs[i].split("=");
				ad.setAttribute(keyvalue[0], keyvalue[1]);
			}
		}

		return ad;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (attributes.size() > 0)
			return "Link[srcDPID=" + srcDPID + ",srcPort=" + srcPort
					+ ",dstDPID=" + dstDPID + ",dstPort=" + dstPort
					+ ",attributes=" + attributes + "]";
		else
			return "Link[srcDPID=" + srcDPID + ",srcPort=" + srcPort
					+ ",dstDPID=" + dstDPID + ",dstPort=" + dstPort + "]";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((dstDPID == null) ? 0 : dstDPID.hashCode());
		result = prime * result + dstPort;
		result = prime * result + ((srcDPID == null) ? 0 : srcDPID.hashCode());
		result = prime * result + srcPort;
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LinkAdvertisement other = (LinkAdvertisement) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (dstDPID == null) {
			if (other.dstDPID != null)
				return false;
		} else if (!dstDPID.equals(other.dstDPID))
			return false;
		if (dstPort != other.dstPort)
			return false;
		if (srcDPID == null) {
			if (other.srcDPID != null)
				return false;
		} else if (!srcDPID.equals(other.srcDPID))
			return false;
		if (srcPort != other.srcPort)
			return false;
		return true;
	}

}
