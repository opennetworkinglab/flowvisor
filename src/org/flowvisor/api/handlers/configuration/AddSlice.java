package org.flowvisor.api.handlers.configuration;

import java.util.List;
import java.util.Map;

import org.flowvisor.api.APIAuth;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.handlers.*;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.InvalidDropPolicy;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.config.SwitchImpl;

import org.flowvisor.exceptions.DuplicateControllerException;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNameDisallowedException;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AddSlice implements ApiHandler<Map<String, Object>> {

	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		if (!FVConfig.isSupervisor(APIUserCred.getUserName()))
			return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
					"only superusers can create new slices"), 0);
		
		try {
			String ctrlUrl = HandlerUtils.<String>fetchField(CTRLURL, params, true, null);
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
			String dropPolicy = HandlerUtils.<String>fetchField(DROP, params, false, "exact");
			Boolean lldpOptIn = HandlerUtils.<Boolean>fetchField(LLDP, params, false, false);
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);			
			String adminInfo = HandlerUtils.<String>fetchField(ADMIN, params, true, null);
			String password = HandlerUtils.<String>fetchField(PASS, params, true, null);
			Number maxFM =  HandlerUtils.<Number>fetchField(MAX, params, false, -1);
			Number rate = HandlerUtils.<Number>fetchField(RATE, params, false, -1);
			Boolean status = HandlerUtils.<Boolean>fetchField(ADMINSTATUS, params, false, true);
			validateSliceName(sliceName);
			validateDropPolicy(dropPolicy);
			SliceImpl.getProxy().createSlice(sliceName, list[1], ctrlPort, 
					dropPolicy, password, APIAuth.getSalt(), adminInfo, APIUserCred.getUserName(),
					lldpOptIn, maxFM.intValue(), 1, FlowMap.type.FEDERATED.ordinal() );
			for (FVClassifier classifier : HandlerUtils.getAllClassifiers())
				SwitchImpl.getProxy().setRateLimit(sliceName, classifier.getDPID(), rate.intValue());
			SliceImpl.getProxy().setAdminStatus(sliceName, status);
			
			resp = new JSONRPC2Response(true, 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": Unable to fetch slice list : " + e.getMessage()), 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (PermissionDeniedException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (InvalidDropPolicy e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (DuplicateControllerException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (SliceNameDisallowedException e){
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
					cmdName() + ": The length of the sliceName: " + e.getMessage() + "  should be less than 45 characters"), 0);
		}
		return resp;
		
	}


	private void validateDropPolicy(String dropPolicy) throws InvalidDropPolicy {
		if (!dropPolicy.equals("exact") && !dropPolicy.equals("rule"))
			throw new InvalidDropPolicy("Flowvisor currently supports an 'exact'"
						+" or a 'rule' based drop policy");
		
	}


	private void validateSliceName(String sliceName)
		throws ConfigError, PermissionDeniedException, SliceNameDisallowedException  {
		List<String> slices = FVConfig.getAllSlices();
		if (slices.contains(sliceName))
			throw new PermissionDeniedException(
					"Cannot create slice with existing name.");
		
		//Check if the sliceName length is within 45 char, if not throw out an error.
		if (sliceName.length()>45){
			FVLog.log(LogLevel.ALERT, null,"The length of the sliceName:",sliceName,
					" should be less than 45 characters");
			throw new SliceNameDisallowedException(sliceName);
		}
	
	}


	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}


	@Override
	public String cmdName() {
		return "add-slice";
	}

}
