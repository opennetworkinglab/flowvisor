package org.flowvisor.config;

/**
 * Interface for listeners to the data repository.
 * @author ash
 *
 */
public interface ChangedListener {
	
	/**
	 * Key to the listener data structure for 
	 * a flowmap.
	 */
	public static String FLOWMAP = "__flowmap";
	
	/**
	 * Key to the listener data structure for 
	 * a flowvisor.
	 */
	public static String FLOWVISOR = "__flowvisor";
	
	
	/**
	 * Callback method implementing the listener.
	 * 
	 *
	 * @param event contains the name of the method to
	 * call for this callback. The method is called 
	 * using reflection.
	 */
	public void processChange(ConfigurationEvent event);
}
