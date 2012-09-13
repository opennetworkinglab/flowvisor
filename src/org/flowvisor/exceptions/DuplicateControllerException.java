package org.flowvisor.exceptions;

import org.flowvisor.exceptions.FVException;

public class DuplicateControllerException extends FVException {

	private static final long serialVersionUID = 1L;

	public DuplicateControllerException(String controllerHostname, int controllerPort, String sliceName, String changeType) {
		super("A slice with this controller: " + controllerHostname + ":" + controllerPort +
				" already exists, this slice (" + sliceName + ") will not be " + changeType);
	}

}
