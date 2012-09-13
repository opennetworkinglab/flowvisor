package org.flowvisor.config;

import java.lang.reflect.Method;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

public class ConfigurationEvent {
	private String toCall = null;
	private Object newValue = null;
	private ChangedListener base = null;

	/**
	 * Constructs a new configuration event. 
	 * 
	 * @param toCall the method to call.
	 * @param base the base class
	 * @param newValue parameter to the method
	 */
	public ConfigurationEvent(String toCall, ChangedListener base, Object newValue) {
		this.toCall = toCall;
		this.newValue = newValue;
		this.base = base;
	}
	
	/**
	 * Finds and invokes the method that implements the callback
	 * in the listener class.
	 */
	public void invoke() {
		Method m = null;
		try {
			try {
				m = base.getClass().getMethod(toCall, newValue.getClass());
			} catch (Throwable e) {
				/*
				 * Stupid work around to perform a flowspace callback.
				 * One day these will go and we will only do differential
				 * updates.... 
				 */
				
				m = base.getClass().getMethod(toCall, newValue.getClass().getInterfaces()[0]);
			}
			Object arglist[] = new Object[1];
			arglist[0] = newValue;
			m.invoke(base, arglist);
		} catch (Throwable e) {
			e.printStackTrace();
			FVLog.log(LogLevel.CRIT, null, e.getMessage() + " " + m + " " + base);
		}
		
	}
}
