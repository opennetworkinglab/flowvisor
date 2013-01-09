package org.flowvisor.api.handlers;

import java.util.Map;

import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownFieldType;

public class HandlerUtils {
	
	public static <T> T fetchField(String fieldName, Map<String, Object> map, 
			Class<T> type, boolean required, T def) 
		throws UnknownFieldType, MissingRequiredField {
		Object field = map.get(fieldName);
		if (field == null)
			if(required) 
				throw new MissingRequiredField(fieldName);
			else
				return def;
		if (field.getClass().isAssignableFrom(type)) 
			return type.cast(field);
		throw new UnknownFieldType(fieldName, type.getName());
		
	}

}
