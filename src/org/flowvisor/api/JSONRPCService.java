package org.flowvisor.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
//The JSON-RPC 2.0 server framework package

public class JSONRPCService {

	Dispatcher dispatcher = new Dispatcher();
	
	public JSONRPCService() {
		dispatcher.register(new ConfigurationHandler());
	}
	
	
	
	

	public void dispatch(HttpServletRequest req, HttpServletResponse resp) { 
		JSONRPC2Request json = null;
		JSONRPC2Response jsonResp = null;
		try {
			json = parseJSONRequest(req);
			jsonResp = dispatcher.process(json, null);
			jsonResp.setID(json.getID());
		} catch (IOException e) {
			jsonResp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), 
					e.getMessage()), 0);
		} catch (JSONRPC2ParseException e) {
			jsonResp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), 
					e.getMessage()), 0);
		}
		try {
			writeJSONObject(resp, jsonResp);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, null, "Unable to send response: " + e.getMessage());
		}
		
		
	}


	/**
	 * Parses the json.
	 *
	 * @param request the request
	 * @return the jSON object
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPC2ParseException 
	 * @throws JSONException the jSON exception
	 */
	private JSONRPC2Request parseJSONRequest(HttpServletRequest request)
			throws IOException, JSONRPC2ParseException{
		BufferedReader reader = request.getReader();
	    StringBuilder sb = new StringBuilder();
	    String line = reader.readLine();
	    while (line != null) {
	        sb.append(line + "\n");
	        line = reader.readLine();
	    }
	    reader.close();
	    FVLog.log(LogLevel.DEBUG, null, "---------JSON RPC request: ", sb.toString());
	    return JSONRPC2Request.parse(sb.toString());

	}
	/*	BufferedReader reader = null;
		char[] buff = new char[100];
		int sz = 0;
		try {
			reader = request.getReader();
			StringBuffer buffer = new StringBuffer();
			while ((sz = reader.read(buff)) != -1) {
				buffer.append(buff, 0, sz);
			}
			FVLog.log(LogLevel.DEBUG, null, "---------JSON RPC request:" + buffer.toString());
			return JSONRPC2Request.parse(buffer.toString());
		} catch (JSONRPC2ParseException e) {
			return null;
		} finally {
			if (reader != null)
				reader.close();
		}

	}*/
	
	/**
	 * Write json object.
	 *
	 * @param response the response
	 * @param jobj the jobj
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void writeJSONObject(HttpServletResponse response, JSONRPC2Response jresp)
			throws IOException {
		response.setContentType("text/json; charset=utf-8");
		String json = jresp.toJSONString();
		Writer writer = response.getWriter();
		FVLog.log(LogLevel.DEBUG, null, "---------JSON RPC response:", json);
		writer.write(json);
	}


	protected static String stack2string(Exception e) {
		PrintWriter pw = null;
		try {
			StringWriter sw = new StringWriter();
			pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return "------\r\n" + sw.toString() + "------\r\n";
		} catch (Exception e2) {
			return "bad stack2string";
		} finally {
			if (pw != null) {
				try {
					pw.close();
				} catch (Exception ex) {
				}
			}
		}
	}

}
