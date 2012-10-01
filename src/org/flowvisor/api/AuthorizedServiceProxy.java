package org.flowvisor.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.http.HttpHeaders;
import org.flowvisor.exceptions.RPCException;
import org.flowvisor.flows.FlowEntry;
import org.json.JSONDeserializers;
import org.json.JSONParam;
import org.json.JSONRequest;
import org.json.JSONResponse;
import org.json.JSONSerializers;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class AuthorizedServiceProxy  implements MethodInterceptor{

	private String user;

	private String passwd;

	private String url;

	private int nextId =0;

	private static final Gson gson =
		new GsonBuilder().registerTypeAdapter(OFAction.class, new JSONSerializers.OFActionSerializer())
		.registerTypeAdapter(OFAction.class, new JSONDeserializers.OFActionDeserializer())
		.registerTypeAdapter(OFMatch.class, new JSONSerializers.OFActionSerializer())
		.registerTypeAdapter(OFMatch.class, new JSONDeserializers.OFMatchDeserializer())
		.registerTypeAdapter(FlowEntry.class, new JSONSerializers.FlowEntrySerializer())
		.registerTypeAdapter(FlowEntry.class, new JSONDeserializers.FlowEntryDeserializer()).create();

	/** The e. */
	Enhancer e = null;

	public AuthorizedServiceProxy(Class<?> serviceInterface, String url, String user, String passwd) {
		this.user = user;
		this.passwd = passwd;
		this.url = url;

		e = new Enhancer();
		e.setSuperclass(serviceInterface);
		e.setCallback(this);
	}


	/**
	 * Creates the.
	 *
	 * @return the object
	 */
	public Object create() {
		return e.create();
	}

	/**
	 * Next id.
	 *
	 * @return the int
	 */
	private synchronized int nextId(){
		nextId += 1;
		return nextId;
	}



	/* (non-Javadoc)
	 * @see net.sf.cglib.proxy.MethodInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], net.sf.cglib.proxy.MethodProxy)
	 */
	@Override
	public Object intercept(Object obj, Method method, Object[] args,
			MethodProxy proxy) throws RPCException {

		try {
			Collection<JSONParam> jargs = encodeArguments(method, args);
			return post(method, jargs);
		} catch (IllegalAccessException e) {
			System.out.println(e.getMessage());
			throw new RPCException(RPCException.METHOD_NOT_FOUND, e.getMessage(), e);
		} catch (InvocationTargetException e) {
			System.out.println(e.getMessage());
			throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
		} catch (InstantiationException e) {
			System.out.println(e.getMessage());
			throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
		}

	}

	/**
	 * Post.
	 *
	 * @param method the method
	 * @param args the args
	 * @return the object
	 * @throws JSONException the jSON exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws RPCException
	 */
	protected Object post(Method method, Collection<JSONParam> args) throws
	IOException, InstantiationException, IllegalAccessException,
	InvocationTargetException, RPCException {

		StringBuffer sb = new StringBuffer();
		HttpURLConnection connection = null;
		OutputStreamWriter writer = null;
		InputStreamReader reader = null;

		int responseCode = 200;

		try {
			JSONRequest jsonReq = new JSONRequest(method.getName(), args, nextId());

			sb = new StringBuffer();
			char[] buff = new char[1024];

			URL u = new URL(url);
			connection = (HttpURLConnection) u.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			connection.setDoOutput(true);

			String encodedAuth;
			try {
				encodedAuth = new String (Base64.encodeBase64(new String(user + ":" + passwd).getBytes()));
			} catch (Exception e) {
				encodedAuth = "";
			}
			connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

			writer = new OutputStreamWriter(connection.getOutputStream(),
			"UTF-8");
			writer.write(gson.toJson(jsonReq));
			writer.flush();

			responseCode = connection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				reader = new InputStreamReader(connection.getInputStream(),
						"UTF-8");
				int i = 0;
				while ((i = reader.read(buff, 0, 1024)) != -1) {
					sb.append(buff, 0, i);
				}

				String val = sb.toString();
				return parseReturnValue(method, val);
			} else
				throw new RPCException(RPCException.INTERNAL_ERROR, "response code is:" + responseCode, null);
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (Exception e) {
				}
				if (reader != null)
					try {
						reader.close();
					} catch (Exception e) {
					}
					if (connection != null) {
						try {
							connection.disconnect();
						} catch (Exception e) {
						}
					}
		}

	}

	/**
	 * Parses the return value.
	 *
	 * @param method the method
	 * @param pkt the pkt
	 * @param factory the factory
	 * @return the object
	 * @throws JSONException the jSON exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws RPCException
	 */
	public static Object parseReturnValue(Method method, String retVal) throws
	InstantiationException, IllegalAccessException,
	InvocationTargetException, RPCException {
		JSONResponse jResp = gson.fromJson(retVal, JSONResponse.class);
		if (jResp.getResult() == null) {
			RPCException e = new RPCException(jResp.getErrorCode(), jResp.getMessage(), null);
			e.setData(jResp.getData());
			throw e;
		}

		return jResp.getResult().getValue(gson, method.getGenericReturnType());
	}

	/**
	 * Encode arguments.
	 *
	 * @param method the method
	 * @param args the args
	 * @return the jSON array
	 * @throws JSONException the jSON exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 */
	public static Collection<JSONParam> encodeArguments(Method method, Object[] args)
	throws IllegalAccessException,
	InvocationTargetException {
		ArrayList<JSONParam> params = new ArrayList<JSONParam>();
		Type[] argTypes = method.getGenericParameterTypes();

		for (int i = 0; i < argTypes.length; i++) {
			params.add(new JSONParam(gson.toJson(args[i], argTypes[i])));
		}
		return params;
	}


}
