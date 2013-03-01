/**
 *
 */
package org.flowvisor.exceptions;


import org.openflow.protocol.OFError.OFBadRequestCode;

/**
 * Signal that an action in an actions list was not allowed
 *
 * @author ash
 *
 */
public class StatDisallowedException extends FVException {

	private OFBadRequestCode error = null;
	
	public StatDisallowedException(String string, OFBadRequestCode err) {
		super(string);
		error = err;
	}

	public OFBadRequestCode getError() {
		return error;
	}
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

}
