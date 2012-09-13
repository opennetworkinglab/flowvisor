/**
 *
 */
package org.flowvisor.classifier;


/**
 * @author capveg
 *
 */
public class XidPair {
	int xid;
	String sliceName;

	public XidPair(int xid, String sliceName) {
		this.xid = xid;
		this.sliceName = sliceName;
	}

	public int getXid() {
		return xid;
	}

	public void setXid(int xid) {
		this.xid = xid;
	}

	/**
	 * @return the sliceName
	 */
	public String getSliceName() {
		return sliceName;
	}

	/**
	 * @param sliceName the sliceName to set
	 */
	public void setSliceName(String sliceName) {
		this.sliceName = sliceName;
	}

}
