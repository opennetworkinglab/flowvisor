package org.flowvisor.api.handlers;

import java.util.List;
import java.util.Map;

import org.flowvisor.api.APIAuth;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.InvalidDropPolicy;
import org.flowvisor.config.SliceImpl;

import org.flowvisor.exceptions.DuplicateControllerException;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.UnknownFieldType;
import org.flowvisor.flows.FlowMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AddSlice implements ApiHandler<Map<String, Object>> {

	
	
	private final static String CTRLURL = "controller-url";
	private final static String SLICENAME = "slice-name";
	private final static String DROP = "drop-policy";
	private final static String LLDP = "recv-lldp";
	private final static String ADMIN = "admin-contact";
	private final static String PASS = "password";
	private final static String MAX = "flowmod-limit";
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		if (!FVConfig.isSupervisor(APIUserCred.getUserName()))
			return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
					"only superusers can create new slices"), 0);
		
		try {
			String ctrlUrl = HandlerUtils.fetchField(CTRLURL, params, String.class, true, null);
			String[] list = ctrlUrl.split(":");
			if (list.length < 2)
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
						"controller url needs to be of the form "
								+ "proto:hostname[:port], e.g., tcp:yourhost.foo.com:6633, not: "
								+ ctrlUrl), 0);
			if (!list[0].equals("tcp"))
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
						"Flowvisor currently only supports 'tcp' proto, not: "
								+ list[0]), 0);
			int ctrlPort = 0;
			if (list.length >= 3)
				ctrlPort = Integer.valueOf(list[2]);
			else
				ctrlPort = FVConfig.OFP_TCP_PORT;
			String dropPolicy = HandlerUtils.fetchField(DROP, params, String.class, false, "exact");
			Boolean lldpOptIn = HandlerUtils.fetchField(LLDP, params, Boolean.class, false, false);
			String sliceName = HandlerUtils.fetchField(SLICENAME, params, String.class, true, null);
			String adminInfo = HandlerUtils.fetchField(ADMIN, params, String.class, true, null);
			String password = HandlerUtils.fetchField(PASS, params, String.class, true, null);
			Number maxFM =  HandlerUtils.fetchField(MAX, params, Number.class, false, -1);
			validateSliceName(sliceName);
			validateDropPolicy(dropPolicy);
			SliceImpl.getProxy().createSlice(sliceName, list[1], ctrlPort, 
					dropPolicy, password, APIAuth.getSalt(), adminInfo, APIUserCred.getUserName(),
					lldpOptIn, maxFM.intValue(), 1, FlowMap.type.FEDERATED.ordinal() );
			resp = new JSONRPC2Response(true, 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					"Unable to fetch slice list : " + e.getMessage()), 0);
		} catch (UnknownFieldType e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"add-slice: " + e.getMessage()), 0);
		} catch (PermissionDeniedException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"add-slice: " + e.getMessage()), 0);
		} catch (InvalidDropPolicy e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					"add-slice: " + e.getMessage()), 0);
		} catch (DuplicateControllerException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
					"add-slice: " + e.getMessage()), 0);
		} 
		return resp;
		
	}


	private void validateDropPolicy(String dropPolicy) throws InvalidDropPolicy {
		if (!dropPolicy.equals("exact") && !dropPolicy.equals("rule"))
			throw new InvalidDropPolicy("Flowvisor currently supports an 'exact'"
						+" or a 'rule' based drop policy");
		
	}


	private void validateSliceName(String sliceName)
		throws ConfigError, PermissionDeniedException {
		List<String> slices = FVConfig.getAllSlices();
		if (slices.contains(sliceName))
			throw new PermissionDeniedException(
					"Cannot create slice with existing name.");
	
	}


	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
