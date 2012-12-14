/**
 *
 */
package org.flowvisor.classifier;


/**
 * @author capveg
 *
 */
public class CookiePair {
	long cookie;
	String sliceName;

	public CookiePair(long xid, String sliceName) {
		this.cookie = xid;
		this.sliceName = sliceName;
	}

	public long getCookie() {
		return cookie;
	}

	public void setXid(Long cookie) {
		this.cookie = cookie;
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
