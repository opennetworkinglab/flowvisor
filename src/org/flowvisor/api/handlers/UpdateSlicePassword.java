package org.flowvisor.api.handlers;

import java.util.Map;

import org.flowvisor.api.APIAuth;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.UnknownFieldType;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class UpdateSlicePassword implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			String sliceName = HandlerUtils.fetchField(SLICENAME, params, String.class, true, null);
			String newPasswd = HandlerUtils.fetchField(PASS, params, String.class, true, null);
			String changerSlice = APIUserCred.getUserName();
			if (!APIAuth.transitivelyCreated(changerSlice, sliceName)
					&& !FVConfig.isSupervisor(changerSlice))
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
						"Slice " + changerSlice
						+ " does not have perms to change the passwd of "
						+ sliceName), 0);
			String salt = APIAuth.getSalt();
			String crypt = APIAuth.makeCrypt(salt, newPasswd);
			sliceName = FVConfig.sanitize(sliceName);
			SliceImpl.getProxy().setPasswd(sliceName, salt, crypt);
			resp = new JSONRPC2Response(true, 0);
		}  catch (UnknownFieldType e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"update-slice-password: " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"update-slice-password: " + e.getMessage()), 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					"update-slice-password: Unable to set slice password : " + e.getMessage()), 0);
		}  
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
