package org.flowvisor.api.handlers.monitoring;


import java.util.List;
import java.util.Map;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.TopologyCallback;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.ofswitch.TopologyController;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class UnRegisterEventCallback implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			
			
			String method = HandlerUtils.<String>fetchField(METHOD, params, true, null);
			String eventType = HandlerUtils.<String>fetchField(EVENT, params, true, null);
			String cookie = HandlerUtils.<String>fetchField(COOKIE, params, true, null);
			String dpidStr = HandlerUtils.<String>fetchField(FlowSpace.DPID, params, false, null);
			
			TopologyCallback.EventType callbackType = TopologyCallback.EventType.valueOf(eventType);
			if(callbackType == TopologyCallback.EventType.FLOWTABLE_CALLBACK)
			{
				if(dpidStr != null){
					Long dpid = FlowSpaceUtil.parseDPID(dpidStr);
					FVClassifier classifier = HandlerUtils.getClassifierByDPID(dpid);
					if (classifier!= null){
					classifier.deRegisterCallBack(APIUserCred.getUserName(), method, cookie, TopologyCallback.EventType.FLOWTABLE_CALLBACK, dpid);
					resp = new JSONRPC2Response(true, 0);
					}
					else
						resp = new JSONRPC2Response(false,0);
				}
				else if(dpidStr == null){
					List<FVClassifier> cList = HandlerUtils.getAllClassifiers();
					for (FVClassifier c: cList){
						if(c!=null){
							c.deRegisterCallBack(APIUserCred.getUserName(), method, cookie, TopologyCallback.EventType.FLOWTABLE_CALLBACK, c.getDPID());
							resp = new JSONRPC2Response(true, 0);
						}
						else
							resp = new JSONRPC2Response(false,0);
					}
				}
			}
			else
			{	
				TopologyController tc = TopologyController.getRunningInstance();
				if (tc != null) {
					tc.deregisterCallback(APIUserCred.getUserName(), method, cookie, eventType);
					resp = new JSONRPC2Response(true, 0);
				} else
					resp = new JSONRPC2Response(false, 0);
			}
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (DPIDNotFound e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} 
		return resp;
		
	}

	

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	@Override
	public String cmdName() {
		return "unregister-event-callback";
	}
	


}
