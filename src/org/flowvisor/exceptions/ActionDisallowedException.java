/**
 *
 */
package org.flowvisor.exceptions;


import org.openflow.protocol.OFError.OFBadActionCode;

/**
 * Signal that an action in an actions list was not allowed
 *
 * @author capveg
 *
 */
public class ActionDisallowedException extends FVException {

	private OFBadActionCode error = null;
	
	public ActionDisallowedException(String string, OFBadActionCode err) {
		super(string);
		error = err;
	}

	public OFBadActionCode getError() {
		return error;
	}
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

}
