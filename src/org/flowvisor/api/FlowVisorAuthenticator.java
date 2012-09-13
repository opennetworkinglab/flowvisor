package org.flowvisor.api;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.UserIdentity.Scope;

public class FlowVisorAuthenticator extends BasicAuthenticator {

	@Override
	public Authentication validateRequest(ServletRequest req,
			ServletResponse arg1, boolean arg2) throws ServerAuthException {
		String credentials = ((HttpServletRequest) req).getHeader(HttpHeaders.AUTHORIZATION);
		try {
			byte[] b64 = new Base64().decode(credentials.substring(6).getBytes());
			String decoded = new String(b64);

			String username = decoded.substring(0, decoded.indexOf(":"));
			String password = decoded.substring(decoded.indexOf(":") + 1);
			if ((APIAuth.isAuthorized(username, password, ""))){
				return new FlowVisorAuthenticatedUser(username, password );
			}
			else
				return Authentication.UNAUTHENTICATED;
		} catch (Exception e) {

			return Authentication.UNAUTHENTICATED;
		}
	}

	public class FlowVisorAuthenticatedUser implements Authentication.User {


		public FlowVisorAuthenticatedUser(String username, String password){

		}

		@Override
		public String getAuthMethod() {
			return "JettyFlowVisor";
		}

		@Override
		public UserIdentity getUserIdentity() {
			return new DefaultUserIdentity(new Subject(), null, new String[] {"user"});
		}

		@Override
		public boolean isUserInRole(Scope arg0, String arg1) {
			return true;
		}

		@Override
		public void logout() {
			// TODO Auto-generated method stub

		}

	}
}
