package org.flowvisor.api;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.eclipse.jetty.http.HttpHeaders;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

public class FlowTableCallback implements Runnable {
	String URL;
	Long dpid;
	String methodName;
	String cookie;

	String httpBasicUserName;
	String httpBasicPassword;

	XmlRpcClientConfigImpl config;
	XmlRpcClient client;
	List<Object> params = new ArrayList<Object>();

	private int jsonCallbackId = 0;
	private String user;

	public FlowTableCallback(String uRL, String methodName, Long dpid)
	{
		URL = uRL;
		this.methodName=methodName;
		this.dpid=dpid;
		
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
	
	public FlowTableCallback(String user, String url, String methodName, String cookie, Long dpid){
		this(url, methodName, dpid);
		this.user = user;
		this.cookie = cookie;
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
	
	
	public String getUser() {
		return this.user;
	}
	
	public Long getDpid(){
		return this.dpid;
	}
	
	public String getCookie(){
		return this.cookie;
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		runSpecificCallback();
		//clearParams();
		return;
	}
		

	private void runSpecificCallback(){
		HttpURLConnection connection = null;
		OutputStreamWriter writer = null;
		InputStreamReader reader = null;

		int responseCode = 200;

		try {
			FVLog.log(LogLevel.DEBUG, null, "Params: ",this.params);
			
			JSONRPC2Request jsonReq = new JSONRPC2Request(this.methodName, this.params, nextId());
			
			URL u = new URL(this.URL);
			connection = (HttpURLConnection) u.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
			"application/json");
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
			writer.write(jsonReq.toJSONString());
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

	public void setParams(List<Object> params){
		this.params = params;
		FVLog.log(LogLevel.DEBUG, null, "inside setParams: ",this.params);
	}

	public void setParams(Object o) {
		this.params = new ArrayList<Object>();
		this.params.add(o);
		FVLog.log(LogLevel.DEBUG, null, "inside setParams: ",this.params);
				
	}
	
	public void clearParams(){
		this.params = new ArrayList<Object>();
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
