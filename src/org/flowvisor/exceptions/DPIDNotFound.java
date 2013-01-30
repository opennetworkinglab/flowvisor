/**
 *
 */
package org.flowvisor.exceptions;

/**
 * @author capveg
 *
 */
public class DPIDNotFound extends FVException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public DPIDNotFound(String arg0) {
		super("dpid does not exist or has not yet connected: " + arg0);
	}

}
