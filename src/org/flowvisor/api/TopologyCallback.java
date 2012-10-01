package org.flowvisor.api;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.eclipse.jetty.http.HttpHeaders;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.TopologyController;
import org.json.JSONDeserializers;
import org.json.JSONParam;
import org.json.JSONRequest;
import org.json.JSONSerializers;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TopologyCallback implements Runnable {

	public enum EventType{
		GENERAL,
		DEVICE_CONNECTED,
		SLICE_CONNECTED,
		SLICE_DISCONNECTED,
		//DEVICE_DISCONNECTED,
	//	PORT_ADDED,
	//	PORT_REMOVED
	}

	String URL;
	String cookie;
	String methodName;

	String httpBasicUserName;
	String httpBasicPassword;

	XmlRpcClientConfigImpl config;
	XmlRpcClient client;
	Collection<JSONParam> params = new ArrayList<JSONParam>();

	private EventType eventType = EventType.GENERAL;

	private int jsonCallbackId = 0;

	private static final Gson gson =
		new GsonBuilder().registerTypeAdapter(OFAction.class, new JSONSerializers.OFActionSerializer())
		.registerTypeAdapter(OFAction.class, new JSONDeserializers.OFActionDeserializer())
		.registerTypeAdapter(OFMatch.class, new JSONSerializers.OFActionSerializer())
		.registerTypeAdapter(OFMatch.class, new JSONDeserializers.OFMatchDeserializer())
		.registerTypeAdapter(FlowEntry.class, new JSONSerializers.FlowEntrySerializer())
		.registerTypeAdapter(FlowEntry.class, new JSONDeserializers.FlowEntryDeserializer()).create();

	public TopologyCallback(String uRL, String methodName,String cookie) {
		super();
		URL = uRL;
		this.methodName=methodName;
		this.cookie = cookie;

		int indexAt;

		indexAt=uRL.indexOf("@");
		if (indexAt>3){//means there is a username/password encoded in the URL
			String newString;
			newString=uRL.substring(uRL.indexOf("://")+3,indexAt-uRL.indexOf("://")+5);
			this.httpBasicUserName=newString.substring(0,newString.indexOf(":"));
			this.httpBasicPassword=newString.substring(newString.indexOf(":")+1);
		}
		else{
			this.httpBasicUserName="";
			this.httpBasicPassword="";
		}

	}

	public TopologyCallback(String url, String methodName, EventType eventType){
		this(url, methodName, "");
		this.eventType = eventType;
	}

	public void spawn() {
		new Thread(this).start();
	}

	public String getURL() {
		return this.URL;
	}

	public String getMethodName(){
		return this.methodName;
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if (eventType != EventType.GENERAL){
			runSpecificCallback();
			return;
		}

		this.installDumbTrust();
		config = new XmlRpcClientConfigImpl();
		URL urlType;
		try {
			urlType = new URL(this.URL);
			config.setServerURL(urlType);
		} catch (MalformedURLException e) {
			// should never happen; we test this on input
			throw new RuntimeException(e);
		}
		config.setEnabledForExtensions(true);

		if (httpBasicUserName!=null && httpBasicUserName!="" && httpBasicPassword!="" && httpBasicPassword!=null)
		{
			config.setBasicUserName(httpBasicUserName);
			config.setBasicPassword(httpBasicPassword);
		}

		client = new XmlRpcClient();
		// client.setTransportFactory(new
		// XmlRpcCommonsTransportFactory(client));
		// client.setTransportFactory(new )
		client.setConfig(config);
		try {
			String call = urlType.getPath();
			if (call.startsWith("/"))
				call = call.substring(1);
			//this.client.execute(this.methodName, new Object[] { cookie });
			this.client.execute(this.methodName,new Object[]{ null});
		} catch (XmlRpcException e) {
			FVLog.log(LogLevel.WARN, TopologyController.getRunningInstance(),
					"topoCallback to URL=" + URL + " failed: " + e);
		}

	}

	private void runSpecificCallback(){
		HttpURLConnection connection = null;
		OutputStreamWriter writer = null;
		InputStreamReader reader = null;

		int responseCode = 200;

		try {
			JSONRequest jsonReq = new JSONRequest(this.methodName, params, nextId());

			URL u = new URL(this.URL);
			connection = (HttpURLConnection) u.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
			"application/x-www-form-urlencoded");
			connection.setDoOutput(true);

			if (httpBasicUserName!=null && httpBasicUserName!="" && httpBasicPassword!="" && httpBasicPassword!=null){
				String encodedAuth;
				try {
					encodedAuth = new String (Base64.encodeBase64(new String(this.httpBasicUserName + ":" + this.httpBasicPassword).getBytes()));
				} catch (Exception e) {
					encodedAuth = "";
				}
				connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
			}

			writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
			writer.write(gson.toJson(jsonReq));
			writer.flush();
			responseCode = connection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				FVLog.log(LogLevel.INFO, null, "HTTP topology callback '" + this.methodName + " to " + this.URL + "'successful.");
			} else
				FVLog.log(LogLevel.WARN, null, "HTTP topology callback '" + this.methodName + " to " + this.URL + "' failed on server.");
		} catch (Exception e) {
			FVLog.log(LogLevel.WARN, null, "HTTP topology callback '" + this.methodName + " to " + this.URL + "'failed due to " + e.getLocalizedMessage());
		} finally {
			if (writer != null){
				try {
					writer.close();
				} catch (Exception e) {
				}
				if (reader != null){
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
		}
	}

	/**
	 * Next id.
	 *
	 * @return the int
	 */
	private synchronized int nextId(){
		jsonCallbackId += 1;
		return jsonCallbackId;
	}

	public void setParams(Collection<JSONParam> params){
		this.params = params;
	}

	public void setParams(Object o) {
		this.params = new ArrayList<JSONParam>();
		this.params.add(new JSONParam(gson.toJson(o)));
				
	}
	
	public void clearParams(){
		this.params = new ArrayList<JSONParam>();
	}

	public void installDumbTrust() {

		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}
		} };
		try {
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			// Create empty HostnameVerifier
			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			};

			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
			.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (KeyManagementException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
