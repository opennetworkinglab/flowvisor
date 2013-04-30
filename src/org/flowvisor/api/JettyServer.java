package org.flowvisor.api;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;


public class JettyServer implements Runnable{

	public static final int default_jetty_port = 8081;

	public static String REALM_NAME = "JETTYREALM";
	private Server jettyServer;

	protected JSONRPCService service = new JSONRPCService();

	public JettyServer(int port){
		init(port);
	}

	private void init(int port){

		//System.setProperty("org.eclipse.jetty.util.log.class", JettyLog.class.getCanonicalName());
		//org.eclipse.jetty.util.log.Log.setLog(new JettyLogger());
		
		FVLog.log(LogLevel.INFO, null,
				"initializing FlowVisor UserAPI JSONRPC SSL WebServer on port "
						+ port);
		jettyServer = new Server(port);

		SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();
		sslConnector.setPort(port);
		String sslKeyStore = System.getProperty("javax.net.ssl.keyStore");
		if (sslKeyStore == null) {
			throw new RuntimeException(
			"Property javax.net.ssl.keyStore not defined; are you correctly using the flowvisor wrapper script?");
		}
		if (!(new File(sslKeyStore)).exists())
			throw new RuntimeException("SSL Key Store file not found: '"
					+ sslKeyStore
					+ "'\nPlease generate with `fvconfig generateCert`");
		sslConnector.setKeystore(sslKeyStore);

		String sslKeyStorePW = System.getProperty("javax.net.ssl.keyStorePassword");
		sslConnector.setPassword(sslKeyStorePW);

		jettyServer.addConnector(sslConnector);


		jettyServer.setConnectors(new Connector[]{sslConnector});



		// Set up context
		/*ContextHandler context = new ContextHandler();
		context.setContextPath("/flowvisor");
		context.setResourceBase(".");
		context.setClassLoader(Thread.currentThread().getContextClassLoader());
		context.setAllowNullPathInfo(true);
		context.setServer(jettyServer);*/

		// Set up Security
		ConstraintSecurityHandler authHandler = createAuthenticationHandler(jettyServer);
		authHandler.setHandler(new AuthenticationHandler());
		//context.setHandler(authHandler);
	}

	@Override
	public void run(){

		try {
			jettyServer.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			jettyServer.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}

	public class AuthenticationHandler extends AbstractHandler{

		@Override
		public final void handle(String target,Request baseRequest,
				HttpServletRequest request,HttpServletResponse response)
		throws IOException, ServletException
		{
			if(baseRequest.getAuthentication().equals(Authentication.UNAUTHENTICATED)){
				response.sendError(Response.SC_UNAUTHORIZED, "Permission denied.");
				baseRequest.setHandled(true);
				return;
			}

			service.dispatch(request, response);
			baseRequest.setHandled(true);
		}
	}
	
	private ConstraintSecurityHandler createAuthenticationHandler(Server server){
		ConstraintSecurityHandler security = new ConstraintSecurityHandler();
		security.setRealmName(REALM_NAME);
		server.setHandler(security);

		// Not currently using constraints for rules, but perhaps in the future? Here's an example if so...
		Constraint constraint = new Constraint();
		constraint.setName("auth");
		constraint.setAuthenticate( true );
		constraint.setRoles(new String[]{"user", "admin"});


		ConstraintMapping mapping = new ConstraintMapping();
		mapping.setPathSpec( "/*" );
		mapping.setConstraint( constraint );
		Set<String> knownRoles = new HashSet<String>();
		knownRoles.add("user");
		knownRoles.add("admin");
		security.setConstraintMappings(new ConstraintMapping[] {mapping}, knownRoles);
		security.setAuthenticator(new BasicAuthenticator());
		
		
		LoginService loginService = new FlowVisorLoginService();
		server.addBean(loginService);
		security.setLoginService(loginService);
		security.setStrict(false);

		return security;
	}

	public static void spawnJettyServer(int port){

		if(port == -1){
			try {
				port = FVConfig.getJettyPort();
			} catch (ConfigError e) {
				port = default_jetty_port; // not explicitly configured
			}
		}

		if (port == -1) {
			FVLog.log(LogLevel.INFO, null, "JSON service disabled in config (Jetty webserver port == -1)");
			return;
		}
		
		
		Thread jettyThread = new Thread(new JettyServer(port));
		jettyThread.start();
	}
	
	
		
	

}
