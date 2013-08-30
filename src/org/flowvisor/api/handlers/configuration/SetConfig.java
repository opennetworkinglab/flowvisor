package org.flowvisor.api.handlers.configuration;

import java.util.List;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.config.FlowvisorImpl;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.config.SwitchImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.exceptions.NoParamException;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class SetConfig implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params){
		JSONRPC2Response resp = null;
		
		try {
			//Check for no parameters. Throw out an error if there are no parameters specified in the cmd.
			checkForNoParams(params);
			
			Map<String, Object> floodperm = HandlerUtils.<Map<String, Object>>fetchField(FLOOD, params, false, null);
			if (floodperm != null)
				processFloodPerm(floodperm);
			
			Map<String, Object> fmlimit = HandlerUtils.<Map<String, Object>>fetchField(MAX, params, false, null);
			if (fmlimit != null)
				processFMLimit(fmlimit);
			
			Boolean track = HandlerUtils.<Boolean>fetchField(TRACK, params, false, null);
			if (track != null)
				FlowvisorImpl.getProxy().settrack_flows(track);
			
			Boolean stats = HandlerUtils.<Boolean>fetchField(STATSDESC, params, false, null);
			if (stats != null)
				FlowvisorImpl.getProxy().setstats_desc_hack(stats);
			
			Boolean topo = HandlerUtils.<Boolean>fetchField(TOPOCTRL, params, false, null);
			if (topo != null)
				FlowvisorImpl.getProxy().setTopologyServer(topo);
			
			Number fscache = HandlerUtils.<Number>fetchField(FSCACHE, params, false, null);
			if (fscache != null)
				FlowvisorImpl.getProxy().setFlowStatsCache(fscache.intValue());
			
			
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
		} catch (NoParamException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName()  + e.getMessage()), 0);
		} 
		return resp;
		
	}


    private void checkForNoParams(Map<String, Object> params)
    		throws NoParamException{
		if(params.isEmpty()){
			FVLog.log(LogLevel.DEBUG, null, "No parameters are specified. Please input the parameters and the value you want them to be set");
			throw new NoParamException(" No parameters are specified. Please input the parameters and the value you want them to be set.");
		}		
	}


	private void processFMLimit(Map<String, Object> fmlimit) 
    		throws ClassCastException, MissingRequiredField, ConfigError {
    	String sliceName = HandlerUtils.<String>fetchField(SLICENAME, fmlimit, true, null);
    	Long dpid = FlowSpaceUtil.parseDPID(
    			HandlerUtils.<String>fetchField(FlowSpace.DPID, fmlimit, true, null));
    	Number limit = HandlerUtils.<Number>fetchField(FMLIMIT, fmlimit, true, null);
    	if (dpid == FlowEntry.ALL_DPIDS) 
    		SliceImpl.getProxy().setMaxFlowMods(sliceName, limit.intValue());
    	else
    		SwitchImpl.getProxy().setMaxFlowMods(sliceName, dpid, limit.intValue());
	}


	private void processFloodPerm(Map<String, Object> floodperm) 
			throws ClassCastException, MissingRequiredField, PermissionDeniedException, ConfigError {
		String sliceName = HandlerUtils.<String>fetchField(SLICENAME, floodperm, true, null);
		validateSliceName(sliceName);
		String dpidStr = HandlerUtils.<String>fetchField(FlowSpace.DPID, floodperm, false, null);
		if (dpidStr != null)
			SwitchImpl.getProxy().setFloodPerm(FlowSpaceUtil.parseDPID(dpidStr), sliceName);
		else
			FlowvisorImpl.getProxy().setFloodPerm(sliceName);
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
		return "set-config";
	}

}
