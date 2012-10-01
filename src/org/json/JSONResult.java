package org.json;

import java.lang.reflect.Type;
import com.google.gson.Gson;

public class JSONResult {


	String valueAsJson;

	protected JSONResult(){
		// For Serialization
	}

	public JSONResult(String value){
		this.valueAsJson = value;
	}

	public <T>T getValue(Gson gson, Type type){
		// forcing <T> cast to work around
		// broken 1.6.0._2{1,2,3,4} openjdk javac
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954
		return gson.<T>fromJson(valueAsJson, type);
	}

}
