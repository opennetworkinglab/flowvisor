/**
 *
 */
package org.flowvisor.exceptions;

/**
 * @author alshabib
 *
 */
public class UnknownFieldType extends FVException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public UnknownFieldType(String fieldName, String type) {
		super(fieldName + " is not of type " + type);
	}

}
