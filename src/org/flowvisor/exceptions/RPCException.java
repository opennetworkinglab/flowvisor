package org.flowvisor.exceptions;

public class RPCException extends Exception {


	private static final long serialVersionUID = 1L;

	public static final int PARSE_ERROR = -3;
	public static final int METHOD_NOT_FOUND = -4;
	public static final int INTERNAL_ERROR = -5;

	private long errorCode;

	private String message;

	private Exception exception;

	private String data;

	public RPCException(long code, String msg, Exception e){
		this.errorCode = code;
		this.message = msg;
		this.exception = e;
	}

	public long getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	public Exception getException() {
		return exception;
	}

	public String getData() {
		return data;
	}

	public void setData(String data){
		this.data = data;
	}


}
