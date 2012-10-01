package org.flowvisor.api;

import java.util.HashMap;
import java.util.Map;

public class DeviceAdvertisement {
	long dpid;
	// from ofp_stats_desc()
	String mfr_desc;
	String hw_desc;
	String sw_desc;
	String serial_num;
	String dp_desc;

	// list of key=value pairs, for extensibility
	HashMap<String, String> attributes;

	protected DeviceAdvertisement() {
		// do nothing, for java beans
	}

	public DeviceAdvertisement(long dpid, String mfrDesc, String hwDesc,
			String swDesc, String serialNum, String dpDesc) {
		super();
		this.dpid = dpid;
		mfr_desc = mfrDesc;
		hw_desc = hwDesc;
		sw_desc = swDesc;
		serial_num = serialNum;
		dp_desc = dpDesc;
	}

	/**
	 * This is a fugly hack to turn these structs into something that our XMLRPC
	 * encoder can handler
	 *
	 * FIXME: figure out if a better XMLRPC encoder can solve this for us
	 *
	 * @return
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("dpid", this.dpid);
		map.put("mfr_desc", this.mfr_desc);
		map.put("hw_desc", this.hw_desc);
		map.put("sw_desc", this.sw_desc);
		map.put("serial_num", this.serial_num);
		map.put("dp_desc", this.dp_desc);
		return map;
	}

	public long getDpid() {
		return dpid;
	}

	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	public String getMfr_desc() {
		return mfr_desc;
	}

	public void setMfr_desc(String mfrDesc) {
		mfr_desc = mfrDesc;
	}

	public String getHw_desc() {
		return hw_desc;
	}

	public void setHw_desc(String hwDesc) {
		hw_desc = hwDesc;
	}

	public String getSw_desc() {
		return sw_desc;
	}

	public void setSw_desc(String swDesc) {
		sw_desc = swDesc;
	}

	public String getSerial_num() {
		return serial_num;
	}

	public void setSerial_num(String serialNum) {
		serial_num = serialNum;
	}

	public String getDp_desc() {
		return dp_desc;
	}

	public void setDp_desc(String dpDesc) {
		dp_desc = dpDesc;
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

	public String getAttribute(String key) {
		if (this.attributes == null)
			return null;
		return this.attributes.get(key);
	}

}
