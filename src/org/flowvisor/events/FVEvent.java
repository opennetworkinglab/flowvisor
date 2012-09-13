/**
 *
 */
package org.flowvisor.events;

/**
 * Basic unit of information passed between FV's logical units Derived classes
 * express the specific msg information
 *
 * @author capveg
 *
 */
public class FVEvent {
	private FVEventHandler src, dst;

	public FVEvent(FVEventHandler src, FVEventHandler dst) {
		this.src = src;
		this.dst = dst;
	}

	public FVEvent(FVEvent e) {
		this.src = e.getSrc();
		this.dst = e.getDst();
	}

	/**
	 * Get the sending msg handler (could be null)
	 *
	 * @return
	 */
	public FVEventHandler getSrc() {
		return src;
	}

	/**
	 * Set the sending Event handler
	 *
	 * @param src
	 *            could be null
	 */
	public FVEvent setSrc(FVEventHandler src) {
		this.src = src;
		return this;
	}

	/**
	 * Get the destination of this message
	 *
	 * @return dst dst reference
	 */
	public FVEventHandler getDst() {
		return dst;
	}

	/**
	 * Set the destination Event handler
	 *
	 * @param dst
	 */
	public FVEvent setDst(FVEventHandler dst) {
		this.dst = dst;
		return this;
	}
}
