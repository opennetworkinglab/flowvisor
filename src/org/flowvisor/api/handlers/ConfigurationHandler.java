package org.flowvisor.api.handlers;

import java.util.HashMap;

import org.flowvisor.api.handlers.configuration.AddFlowSpace;
import org.flowvisor.api.handlers.configuration.AddSlice;
import org.flowvisor.api.handlers.configuration.GetConfig;
import org.flowvisor.api.handlers.configuration.ListFSInsertionStatus;
import org.flowvisor.api.handlers.configuration.ListFlowSpace;
import org.flowvisor.api.handlers.configuration.ListSlices;
import org.flowvisor.api.handlers.configuration.ListVersion;
import org.flowvisor.api.handlers.configuration.RemoveFlowSpace;
import org.flowvisor.api.handlers.configuration.RemoveSlice;
import org.flowvisor.api.handlers.configuration.SaveConfig;
import org.flowvisor.api.handlers.configuration.SetConfig;
import org.flowvisor.api.handlers.configuration.UpdateAdminPassword;
import org.flowvisor.api.handlers.configuration.UpdateFlowSpace;
import org.flowvisor.api.handlers.configuration.UpdateSlice;
import org.flowvisor.api.handlers.configuration.UpdateSlicePassword;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class ConfigurationHandler implements RequestHandler {

	@SuppressWarnings( { "serial", "rawtypes" } )
	HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {{
		put("list-slices", new ListSlices());
		put("add-slice", new AddSlice());
		put("update-slice", new UpdateSlice());
		put("remove-slice", new RemoveSlice());
		put("update-slice-password", new UpdateSlicePassword());
		put("update-admin-password", new UpdateAdminPassword());
		put("list-flowspace", new ListFlowSpace());
		put("add-flowspace", new AddFlowSpace());
		put("update-flowspace", new UpdateFlowSpace());
		put("remove-flowspace", new RemoveFlowSpace());
		put("list-version", new ListVersion());
		put("save-config", new SaveConfig());
		put("get-config", new GetConfig());
		put("set-config", new SetConfig());
		put("list-fs-status", new ListFSInsertionStatus());
	}};
	
	
	@Override
	public String[] handledRequests() {
		return handlers.keySet().toArray(new String[]{});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctxt) {
		ApiHandler m = handlers.get(req.getMethod());
		if (m != null) {
			
			if (m.getType() != JSONRPC2ParamsType.NO_PARAMS && m.getType() != req.getParamsType())
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
						req.getMethod() + " requires: " + m.getType() + 
						"; got: " + req.getParamsType()),
						req.getID());
			
			switch (m.getType()) {
			case NO_PARAMS:
				return m.process(null);
			case ARRAY:
				return m.process(req.getPositionalParams());
			case OBJECT:
				return m.process(req.getNamedParams());
			}
		}
		
		return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
	}

}
