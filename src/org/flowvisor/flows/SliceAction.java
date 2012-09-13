/**
 *
 */
package org.flowvisor.flows;

import java.nio.*;
import org.openflow.protocol.action.*;
import org.flowvisor.*;

/**
 * @author capveg
 *
 */
public class SliceAction extends OFActionVendor implements Cloneable {
	public static final int READ = (1 << 1);
	public static final int WRITE = (1 << 2);
	public static final int DELEGATE = (1 << 3);
	public static final int SLICE_CREATE = (1 << 4);

	int slicePerms;
	String sliceName;

	public SliceAction(String sliceName, int slicePerms) {
		super();
		this.sliceName = sliceName;
		this.slicePerms = slicePerms;
		this.setVendor(FlowVisor.FLOWVISOR_VENDOR_EXTENSION);
	}

	public SliceAction() {
		// java bean constructor
	}

	/**
	 * Guaranteed to never print commas, so commas can be used as delimiters in
	 * lists of slice perms
	 *
	 * @return "Slice:$name=3" for name = READ + WRITE, etc.
	 */
	@Override
	public String toString() {
		return "Slice:" + sliceName + "=" + slicePerms;
	}

	public static OFAction fromString(String str) {
		String[] list = str.split("[:=]");
		if ((list.length != 3) || (!list[0].equals("Slice")))
			return OFAction.fromString(str); // format not recognized; default
												// to OFAction
		return new SliceAction(list[1], Integer.parseInt(list[2]));
	}

	@Override
	public void readFrom(ByteBuffer buf) {
		throw new RuntimeException(
				"SliceActions are not to be sent over the wire");
	}

	/**
	 * Auto-generated functions
	 */
	public int getSlicePerms() {
		return slicePerms;
	}

	public void setSlicePerms(int slicePerms) {
		this.slicePerms = slicePerms;
	}

	public String getSliceName() {
		return sliceName;
	}

	public void setSliceName(String sliceName) {
		this.sliceName = sliceName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((sliceName == null) ? 0 : sliceName.hashCode());
		result = prime * result + slicePerms;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SliceAction other = (SliceAction) obj;
		if (sliceName == null) {
			if (other.sliceName != null)
				return false;
		} else if (!sliceName.equals(other.sliceName))
			return false;
		if (slicePerms != other.slicePerms)
			return false;
		return true;
	}

	public SliceAction clone() {
		try {
			return (SliceAction) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
