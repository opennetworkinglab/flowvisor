/**
 *
 */
package org.flowvisor.exceptions;

/**
 * @author alshabib
 *
 */
public class MissingRequiredField extends FVException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public MissingRequiredField(String fieldName) {
		super(fieldName + " is required ");
	}

}
