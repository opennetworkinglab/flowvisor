package org.flowvisor.ofswitch;

public class DPIDandPort {
	long dpid;
	short port;

	public DPIDandPort(long dpid, short port) {
		super();
		this.dpid = dpid;
		this.port = port;
	}

	/**
	 * @return the dpid
	 */
	public long getDpid() {
		return dpid;
	}

	/**
	 * @param dpid
	 *            the dpid to set
	 */
	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	/**
	 * @return the port
	 */
	public short getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(short port) {
		this.port = port;
	}
}
