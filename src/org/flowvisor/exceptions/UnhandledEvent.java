package org.flowvisor.exceptions;

import org.flowvisor.events.FVEvent;

public class UnhandledEvent extends FVException {

	/**
	 *
	 */
	private static final long serialVersionUID = 9059842200519560846L;

	public UnhandledEvent(String err) {
		super(err);
		// TODO Auto-generated constructor stub
	}

	public UnhandledEvent(FVEvent e) {
		super("Unhandled event: " + e.toString());
	}

}
