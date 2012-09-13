package org.flowvisor.api;

import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.xmlrpc.webserver.WebServer;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

public class SSLWebServer extends WebServer {

	public SSLWebServer(int pPort) {
		super(pPort);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected ServerSocket createServerSocket(int pPort, int backlog,
			java.net.InetAddress addr) {
		try {
			//ServerSocketFactory sslFactory = (ServerSocketFactory) ServerSocketFactory
			SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			String[] ciphers = sslFactory.getDefaultCipherSuites();
			if (ciphers.length == 0)
				throw new RuntimeException(
						"Need to configure SSL: no ciphers found");
			else {
				FVLog.log(LogLevel.DEBUG, null, "SSL Supports "
						+ ciphers.length + " Ciphers:: ");
				for (int i = 0; i < ciphers.length; i++)
					FVLog.log(LogLevel.DEBUG, null, "		" + ciphers[i]);
			}
			return sslFactory.createServerSocket(pPort, backlog, addr);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
