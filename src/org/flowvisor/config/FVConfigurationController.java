package org.flowvisor.config;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowvisor.flows.FlowEntry;

public class FVConfigurationController {

	private ConfDBSettings settings = null;
	private static FVConfigurationController instance = null;
	private HashMap<Object, Set<ChangedListener>> listeners = null;
	
	private FlowSpaceHandler fsHandler = null;


	private FVConfigurationController(ConfDBSettings settings) {
		this.settings  = settings;
		this.listeners = new HashMap<Object, Set<ChangedListener>>();
		fsHandler = new FlowSpaceHandler();
		Thread fsThread = new Thread(fsHandler);
		fsThread.start();
		
	}
	
	public static FVConfigurationController instance() {
		if (instance == null) 
			throw new RuntimeException("Initialize the DB connection please.");
		return instance;
	}
	
	public static void init(ConfDBSettings settings) {
		instance = new FVConfigurationController(settings);
	}
	
	public Integer pendFlowSpace(List<FlowEntry> fes) {
		return fsHandler.add(fes);
	}
	
	public void addChangeListener(Object key, ChangedListener listener) {
		Set<ChangedListener> list = null;
		if (listeners.containsKey(key))
			list = listeners.get(key);
		else 
			list = Collections.synchronizedSet(new HashSet<ChangedListener>());
		list.add(listener);
		listeners.put(key, list);
	}
	
	public void removeChangeListener(Object key,
			FlowvisorChangedListener l) {
		Set<ChangedListener> list = listeners.get(key);
		if (list != null && list.contains(l)) {
			list.remove(l);
			listeners.put(key, list);
		}
	}
	
	public void fireChange(final Object key, final String method, final Object value) {
		
		/*FutureTask<Object> future = new FutureTask<Object>(
                new Callable<Object>() {
                    public Object call() {
                    	if (!listeners.containsKey(key))
                			return null;
                		for (ChangedListener l : listeners.get(key)) 
                			l.processChange(new ConfigurationEvent(method, l, value));
                		return null;
                    }
                });
        executor.execute(future);*/
		
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
	
	public ConfDBSettings getSettings() {
		return settings;
	}
	
	public void shutdown() {
		settings.shutdown();
		fsHandler.shutdown();
		
	}

	public String flowSpaceStatus(Integer id) {
		return fsHandler.status(id);
	}

	
}
