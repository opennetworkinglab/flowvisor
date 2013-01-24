package org.flowvisor.api;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;

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
		System.out.println("Creds : " + credentials.toString());
		return null;
	}

	@Override
	public void setIdentityService(IdentityService ids) {}

	@Override
	public boolean validate(UserIdentity arg0) {
		return false;
	}

}
