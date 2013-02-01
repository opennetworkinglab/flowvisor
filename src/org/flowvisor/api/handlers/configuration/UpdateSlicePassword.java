package org.flowvisor.api.handlers.configuration;

import java.util.Map;

import org.flowvisor.api.APIAuth;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.exceptions.MissingRequiredField;


import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class UpdateSlicePassword implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);
			String newPasswd = HandlerUtils.<String>fetchField(PASS, params, true, null);
			String changerSlice = APIUserCred.getUserName();
			if (!APIAuth.transitivelyCreated(changerSlice, sliceName)
					&& !FVConfig.isSupervisor(changerSlice))
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
						cmdName() + ": Slice " + changerSlice
						+ " does not have perms to change the passwd of "
						+ sliceName), 0);
			String salt = APIAuth.getSalt();
			String crypt = APIAuth.makeCrypt(salt, newPasswd);
			sliceName = FVConfig.sanitize(sliceName);
			SliceImpl.getProxy().setPasswd(sliceName, salt, crypt);
			resp = new JSONRPC2Response(true, 0);
		}  catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": Unable to set slice password : " + e.getMessage()), 0);
		}  
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "update-slice-password";
	}

}
