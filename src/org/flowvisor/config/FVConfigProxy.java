package org.flowvisor.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class FVConfigProxy implements InvocationHandler {

	private Object delegate = null;
	
	public FVConfigProxy(Object delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Object value = null;
		try {
	        value = method.invoke(delegate, args);
	        return value;
	    }
	    catch (InvocationTargetException ex) {
	        throw ex.getCause();
	    }
	}
		
}
