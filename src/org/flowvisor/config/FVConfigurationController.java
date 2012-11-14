package org.flowvisor.config;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class FVConfigurationController {

	private ConfDBSettings settings = null;
	private static FVConfigurationController instance = null;
	private HashMap<Object, Set<ChangedListener>> listeners = null;

	private FVConfigurationController(ConfDBSettings settings) {
		this.settings  = settings;
		this.listeners = new HashMap<Object, Set<ChangedListener>>();
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
		Set<ChangedListener> list = null;
		if (listeners.containsKey(key))
			list = listeners.get(key);
		else 
			list = Collections.synchronizedSet(new HashSet<ChangedListener>());
		synchronized (list) {
			list.add(listener);	
		}
		listeners.put(key, list);
	}
	
	public void removeChangeListener(Object key,
			FlowvisorChangedListener l) {
		Set<ChangedListener> list = listeners.get(key);
			if (list != null && list.contains(l)) {
				synchronized (list) {
					list.remove(l);
					listeners.put(key, list);
				}
			}
	}
	
	public void fireChange(Object key, String method, Object value) {
		if (!listeners.containsKey(key))
			return;
		Set<ChangedListener> set = listeners.get(key);
		synchronized (set) {
			for (ChangedListener l : set) 
				l.processChange(new ConfigurationEvent(method, l, value));
		}
			
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
