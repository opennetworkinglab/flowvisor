package org.flowvisor.config;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;

public class FVConfigurationController {

	private ConfDBSettings settings = null;
	private static FVConfigurationController instance = null;
	private HashMap<Object, HashSet<ChangedListener>> listeners = null;

	private FVConfigurationController(ConfDBSettings settings) {
		this.settings  = settings;
		this.listeners = new HashMap<Object, HashSet<ChangedListener>>();
	}
	
	public static FVConfigurationController instance() {
		if (instance == null) 
			throw new RuntimeException("Initialize the DB connection please.");
		return instance;
	}
	
	public static void init(ConfDBSettings settings) {
		instance = new FVConfigurationController(settings);
	}
	
	public void addChangeListener(Object key, ChangedListener listener) {
		HashSet<ChangedListener> list = null;
		if (listeners.containsKey(key))
			list = listeners.get(key);
		else 
			list = new HashSet<ChangedListener>();
		list.add(listener);
		listeners.put(key, list);
	}
	
	public void removeChangeListener(Object key,
			FlowvisorChangedListener l) {
		HashSet<ChangedListener> list = listeners.get(key);
		if (list != null && list.contains(l)) {
			list.remove(l);
			listeners.put(key, list);
		}
	}
	
	public void fireChange(Object key, String method, Object value) {
		if (!listeners.containsKey(key))
			return;
		for (ChangedListener l : listeners.get(key)) 
			l.processChange(new ConfigurationEvent(method, l, value));
		
			
	}
	
	public FVAppConfig getProxy(FVAppConfig instance) {
		instance.setSettings(settings);
		FVAppConfig configProxy = (FVAppConfig) Proxy.newProxyInstance(getClass().getClassLoader(), 
				new Class[] { FVAppConfig.class, instance.getClass().getInterfaces()[0]},
				new FVConfigProxy(instance));
		return configProxy;
	}
	
	public void shutdown() {
		settings.shutdown();
	}

	
}
