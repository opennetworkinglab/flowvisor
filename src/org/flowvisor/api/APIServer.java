package org.flowvisor.api;

import java.io.File;
import java.io.IOException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.server.XmlRpcStreamServer;
import org.apache.xmlrpc.webserver.WebServer;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * This is stolen pretty directly from the apache-xml example code.
 *
 * FIXME: Come back and make asynchronous FIXME: address all of the issues with
 * the WebServer code that the author's bring up
 *
 * @author capveg
 *
 */

public class APIServer {

	private static final int default_port = -1;

	public static int getDefaultPort() {
		return default_port;
	}



	/**
	 * Spawn a thread to run the XMLRPC FlowVisor UserAPI WebServer
	 *
	 * @return the webServer
	 * @throws XmlRpcException
	 * @throws IOException
	 * @throws Exception
	 */
	public static WebServer spawn() throws XmlRpcException, IOException {

		int port;

		try {
			port = FVConfig.getAPIWSPort();
		} catch (ConfigError e) {
			port = default_port; // not explicitly configured
		}

		if (port == -1) {
			FVLog.log(LogLevel.INFO, null, "XMLRPC service disabled in config ( API Webserver port == -1)");
			return null;
		}

		WebServer webServer = new SSLWebServer(port);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

		// set the server to use flowvisor logging
		if(xmlRpcServer instanceof XmlRpcStreamServer) {
			((XmlRpcStreamServer)xmlRpcServer).setErrorLogger(new FVRpcErrorLogger());
		}

		PropertyHandlerMapping phm = new PropertyHandlerMapping();

		phm.addHandler("api", org.flowvisor.api.FVUserAPIXMLRPCImpl.class);
		phm.setAuthenticationHandler(new APIAuth());
		xmlRpcServer.setHandlerMapping(phm);

		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer
				.getConfig();
		// Unset this for now, for python folks:
		// http://bugs.python.org/issue8792
		// XMLRPC is stupid -- need to replace
		// serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);
		FVLog.log(LogLevel.INFO, null,
				"initializing FlowVisor UserAPI XMLRPC SSL WebServer on port "
						+ port);
		String sslKeyStore = System.getProperty("javax.net.ssl.keyStore");
		if (sslKeyStore == null) {
			throw new RuntimeException(
					"Property javax.net.ssl.keyStore not defined; are you correctly using the flowvisor wrapper script?");
		}
		if (!(new File(sslKeyStore)).exists())
			throw new RuntimeException("SSL Key Store file not found: '"
					+ sslKeyStore
					+ "'\nPlease generate with `fvconfig generateCert`");
		webServer.start();
		return webServer;
	}

}
