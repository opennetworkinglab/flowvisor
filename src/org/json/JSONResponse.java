package org.json;

import com.google.gson.annotations.SerializedName;

public class JSONResponse {

	JSONResult result;

	@SerializedName("error_code")
	long errorCode;
	String message;
	String data;
	String jsonrpc = "2.0";
	int id = -1;

	protected JSONResponse(){
		// For serialization
	}

	public JSONResponse(JSONResult result, int id){
		this.result = result;
		this.id = id;
	}

	public JSONResponse(long errorCode, String message, String data, int id){
		this.errorCode = errorCode;
		this.message = message;
		this.data = data;
		this.id = id;
	}

	public JSONResult getResult() {
		return result;
	}

	public long getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	public String getData() {
		return data;
	}

	@Override
	public String toString() {
		if(result == null){
			return "JSONReponse (" + id + "): ERROR (" +errorCode + ")\n Message: " + message + "\n" + data;
		}
		return "JSONResponse (" + id + "):" + result;

	}
}
