package org.flowvisor.api;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.UserIdentity.Scope;

public class FlowVisorLoginService implements LoginService {

	private IdentityService identityService = new DefaultIdentityService();
	
	@Override
	public IdentityService getIdentityService() {
		return identityService;
	}

	@Override
	public String getName() {
		return JettyServer.REALM_NAME;
	}

	@Override
	public UserIdentity login(String username, Object credentials) {
		if (APIAuth.isAuthorized(username, (String) credentials, "")) {
				FlowVisorAuthenticatedUser user = 
					new FlowVisorAuthenticatedUser(username, (String) credentials);
				return user.getUserIdentity();
		}
		return null;
	}

	@Override
	public void setIdentityService(IdentityService ids) {}

	@Override
	public boolean validate(UserIdentity arg0) {
		return false;
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
