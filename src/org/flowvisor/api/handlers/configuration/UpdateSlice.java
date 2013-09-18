package org.flowvisor.api.handlers.configuration;

import java.util.List;
import java.util.Map;

import org.flowvisor.api.APIAuth;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.config.InvalidDropPolicy;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.config.SwitchImpl;
import org.flowvisor.exceptions.DuplicateControllerException;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.PermissionDeniedException;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class UpdateSlice implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, true, null);
			String changerSlice = APIUserCred.getUserName();
			if (!APIAuth.transitivelyCreated(changerSlice, sliceName)
					&& !FVConfig.isSupervisor(changerSlice))
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_REQUEST.getCode(), 
						"Slice " + changerSlice
						+ " does not have perms to change " + sliceName), 0);
			
			String ctrlHost = HandlerUtils.<String>fetchField(CTRLHOST, params, false, null);
			Number ctrlPort = HandlerUtils.<Number>fetchField(CTRLPORT, params, false, null);
			String adminInfo = HandlerUtils.<String>fetchField(ADMIN, params, false, null);
			Number maxFM =  HandlerUtils.<Number>fetchField(MAX, params, false, null);
			String dropPolicy = HandlerUtils.<String>fetchField(DROP, params, false, null);
			Boolean lldpOptIn = HandlerUtils.<Boolean>fetchField(LLDP, params, false, null);
			Number rate = HandlerUtils.<Number>fetchField(RATE, params, false, null);
			Boolean status = HandlerUtils.<Boolean>fetchField(ADMINSTATUS, params, false, null);
			
			validateSliceName(sliceName);
			updateCtrl(sliceName, ctrlHost, ctrlPort);
			updateDropPolicy(sliceName, dropPolicy);
			updateAdminInfo(sliceName, adminInfo);	
			updateMaxFM(sliceName, maxFM);
			updateLLDP(sliceName, lldpOptIn);
			updateRate(sliceName, rate);
			updateStatus(sliceName, status);
			
			resp = new JSONRPC2Response(true, 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": Unable to fetch/set config : " + e.getMessage()), 0);
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
		} 
		return resp;
		
	}

	/*
	 * TODO: put notifyChange in FutureTask
	 */
	private void updateStatus(String sliceName, Boolean status) {
		if (status == null)
			return;
		SliceImpl.getProxy().setAdminStatus(sliceName, status);
		FlowSpaceImpl.getProxy().notifyChange(FVConfig.getFlowSpaceFlowMap());
	}


	private void updateRate(String sliceName, Number rate) throws ConfigError {
		if (rate == null)
			return;
		for (FVClassifier classifier : HandlerUtils.getAllClassifiers())
			SwitchImpl.getProxy().setRateLimit(sliceName, classifier.getDPID(), rate.intValue());
		
	}


	private void updateLLDP(String sliceName, Boolean lldpOptIn) {
		if (lldpOptIn == null)
			return;
		SliceImpl.getProxy().setlldp_spam(sliceName, lldpOptIn);
		
	}


	private void updateMaxFM(String sliceName, Number max) throws ConfigError {
		if (max == null)
			return;
		SliceImpl.getProxy().setMaxFlowMods(sliceName, max.intValue());
		
	}


	private void updateAdminInfo(String sliceName, String adminInfo) throws ConfigError {
		if (adminInfo == null)
			return;
		SliceImpl.getProxy().setContactEmail(sliceName, adminInfo);
	}


	private void updateCtrl(String sliceName, String ctrlHost,
			Number ctrlPort) throws ConfigError, DuplicateControllerException {
		if (ctrlHost == null && ctrlPort == null) 
			return;
		if (ctrlPort == null)
			ctrlPort = SliceImpl.getProxy().getcontroller_port(sliceName);
		if (ctrlHost == null)
			ctrlHost = SliceImpl.getProxy().getcontroller_hostname(sliceName);
		checkDupCtrl(sliceName, ctrlHost, ctrlPort.intValue());
		SliceImpl.getProxy().setcontroller_hostname(sliceName, ctrlHost);
		SliceImpl.getProxy().setcontroller_port(sliceName, ctrlPort.intValue());
	}


	private void checkDupCtrl(String sliceName, String ctrlHost,
			Integer ctrlPort) throws ConfigError, DuplicateControllerException {
		String host = null;
		Integer port = null;
		List<String> slices = FVConfig.getAllSlices();
		for (String slice : slices) {
			if (slice.equals(sliceName))
				continue;
			host = SliceImpl.getProxy().getcontroller_hostname(slice);
			port = SliceImpl.getProxy().getcontroller_port(slice);
			if (port == ctrlPort && host.equalsIgnoreCase(ctrlHost))
				throw new DuplicateControllerException(ctrlHost, ctrlPort, sliceName, "update");
		}
		
	}


	private void updateDropPolicy(String sliceName, String dropPolicy) 
			throws InvalidDropPolicy {
		if (dropPolicy == null)
			return;
		validateDropPolicy(dropPolicy);
		SliceImpl.getProxy().setdrop_policy(sliceName, dropPolicy);
	}


	private void validateDropPolicy(String dropPolicy) throws InvalidDropPolicy {
		if (!dropPolicy.equals("exact") && !dropPolicy.equals("rule"))
			throw new InvalidDropPolicy("Flowvisor currently supports an 'exact'"
						+" or a 'rule' based drop policy");
		
	}


	private void validateSliceName(String sliceName)
		throws ConfigError, PermissionDeniedException {
		List<String> slices = FVConfig.getAllSlices();
		if (!slices.contains(sliceName))
			throw new PermissionDeniedException(
					"Slice " + sliceName + " does not exist");
	
	}


	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}


	@Override
	public String cmdName() {
		return "update-slice";
	}

}
