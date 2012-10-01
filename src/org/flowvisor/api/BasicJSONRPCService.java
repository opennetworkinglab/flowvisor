package org.flowvisor.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowvisor.exceptions.RPCException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.json.JSONDeserializers;
import org.json.JSONParam;
import org.json.JSONRequest;
import org.json.JSONResponse;
import org.json.JSONResult;
import org.json.JSONSerializers;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BasicJSONRPCService {

	/** The methods. */
	private Map<String, Method> methods = new HashMap<String, Method>();

	private static final Gson gson =
		new GsonBuilder().registerTypeAdapter(OFAction.class, new JSONSerializers.OFActionSerializer())
		.registerTypeAdapter(OFAction.class, new JSONDeserializers.OFActionDeserializer())
		.registerTypeAdapter(OFMatch.class, new JSONSerializers.OFActionSerializer())
		.registerTypeAdapter(OFMatch.class, new JSONDeserializers.OFMatchDeserializer())
		.registerTypeAdapter(FlowEntry.class, new JSONSerializers.FlowEntrySerializer())
		.registerTypeAdapter(FlowEntry.class, new JSONDeserializers.FlowEntryDeserializer()).create();

	/**
	 * Gets the method.
	 *
	 * @param methodName the method name
	 * @return the method
	 */
	private Method getMethod(String methodName) {
		if (methods.containsKey(methodName))
			return methods.get(methodName);

		Method[] ms = this.getClass().getMethods();
		for (Method m : ms) {
			if (m.getName().equals(methodName)) {
				methods.put(methodName, m);
				return m;
			}
		}
		return null;
	}

	public void dispatch(HttpServletRequest req, HttpServletResponse resp) {
		int id = 0;
		try {

			try {
				JSONRequest jreq = parseJSONRequest(req);
				String methodName = jreq.getMethod();
				id = jreq.getId();


				Method method = getMethod(methodName);
				if (method == null)
					throw new RPCException(RPCException.METHOD_NOT_FOUND, "Method not found", null);

				Object[] args;

				args = parseArguments(method, jreq);

				FVLog.log(LogLevel.DEBUG, null, "---------invoke:" + jreq.toString());
				Object retObj = method.invoke(this, args);

				JSONResponse jResp = new JSONResponse(new JSONResult(gson.toJson(retObj, method.getGenericReturnType())), id);
				FVLog.log(LogLevel.DEBUG, null, "---------invoke ret:" + jResp.toString());
				writeJSONObject(resp, jResp);
			} catch (IOException e) {
				FVLog.log(LogLevel.WARN, null, e.getMessage(), e);
				throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(),  e);
			} catch (InstantiationException e) {
				FVLog.log(LogLevel.WARN, null, e.getMessage(), e);
				throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
			} catch (IllegalAccessException e) {
				FVLog.log(LogLevel.WARN, null, e.getMessage(), e);
				throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
			} catch (InvocationTargetException e) {
				FVLog.log(LogLevel.WARN, null, e.getMessage(), e);
				throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
			}
		} catch (RPCException ex) {
			try {
				String data = stack2string(ex);
				JSONResponse jResp = new JSONResponse(ex.getErrorCode(), ex.getMessage(), data, id);
				writeJSONObject(resp, jResp);
			} catch (Exception exx) {
				FVLog.log(LogLevel.WARN, null, exx.getMessage(), exx);
			}
		} finally {
		}

	}

	protected static Object[] parseArguments(Method method, JSONRequest jReq) throws InstantiationException,
			IllegalAccessException,InvocationTargetException {
		Iterator<JSONParam> paramItr = jReq.getParams().iterator();
		Type[] argTypes = method.getGenericParameterTypes();
		Object[] args = new Object[argTypes.length];

		for (int i = 0; i < argTypes.length; i++) {
			args[i] = paramItr.next().getValue(gson, argTypes[i]);
		}

		return args;

	}

	/**
	 * Parses the json.
	 *
	 * @param request the request
	 * @return the jSON object
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONException the jSON exception
	 */
	private JSONRequest parseJSONRequest(HttpServletRequest request)
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
			return gson.fromJson(buffer.toString(), JSONRequest.class);
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
	private void writeJSONObject(HttpServletResponse response, JSONResponse jresp)
	throws IOException {
		response.setContentType("text/json; charset=utf-8");
		String json = gson.toJson(jresp, JSONResponse.class);
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
