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
		try {
			json = parseJSONRequest(req);
			JSONRPC2Response jsonResp = dispatcher.process(json, null);
			jsonResp.setID(json.getID());
			writeJSONObject(resp, jsonResp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	/**
	 * Parses the json.
	 *
	 * @param request the request
	 * @return the jSON object
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONException the jSON exception
	 */
	private JSONRPC2Request parseJSONRequest(HttpServletRequest request)
	throws IOException{
		BufferedReader reader = null;
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

	}
	
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
		FVLog.log(LogLevel.DEBUG, null, "---------JSON RPC response:" + json);
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
