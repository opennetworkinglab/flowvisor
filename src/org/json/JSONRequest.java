package org.json;

import java.util.Collection;
import java.util.Iterator;


public class JSONRequest {

	String jsonrpc = "2.0";
	String method = "";
	Collection<JSONParam> params;
	int id = -1;


	protected JSONRequest(){//For serialization

	}

	public JSONRequest(String methodName, Collection<JSONParam> params, int id){
		this.method = methodName;
		this.params = params;
		this.id = id;
	}

	public String getMethod() {
		return method;
	}

	public Collection<JSONParam> getParams() {
		return params;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		String retVal = "JSONRequest (" + id + "):" + method + "\nParams:   ";
		for(Iterator<JSONParam> itr = params.iterator(); itr.hasNext();){
			retVal = retVal + itr.next().toString();
		}
		return retVal;
	}
}
