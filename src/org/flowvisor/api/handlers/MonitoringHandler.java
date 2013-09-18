package org.flowvisor.api.handlers;

import java.util.HashMap;

import org.flowvisor.api.handlers.monitoring.ListDatapathFlowDB;
import org.flowvisor.api.handlers.monitoring.ListDatapathFlowRewriteDB;
import org.flowvisor.api.handlers.monitoring.ListDatapathInfo;
import org.flowvisor.api.handlers.monitoring.ListDatapathStats;
import org.flowvisor.api.handlers.monitoring.ListDatapaths;
import org.flowvisor.api.handlers.monitoring.ListFVHealth;
import org.flowvisor.api.handlers.monitoring.ListLinks;
import org.flowvisor.api.handlers.monitoring.ListSliceHealth;
import org.flowvisor.api.handlers.monitoring.ListSliceInfo;
import org.flowvisor.api.handlers.monitoring.ListSliceStats;
import org.flowvisor.api.handlers.monitoring.RegisterEventCallback;
import org.flowvisor.api.handlers.monitoring.UnRegisterEventCallback;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class MonitoringHandler implements RequestHandler {

	@SuppressWarnings( { "serial", "rawtypes" } )
	HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {{
		put("list-slice-info", new ListSliceInfo());
		put("list-datapaths", new ListDatapaths());
		put("list-datapath-info", new ListDatapathInfo());
		put("list-datapath-stats", new ListDatapathStats());
		put("list-fv-health", new ListFVHealth());
		put("list-links", new ListLinks());
		put("list-slice-health", new ListSliceHealth());
		put("list-slice-stats", new ListSliceStats());
		put("list-datapath-flowdb", new ListDatapathFlowDB());
		put("list-datapath-flowrewritedb", new ListDatapathFlowRewriteDB());
		put("register-event-callback", new RegisterEventCallback());
		put("unregister-event-callback", new UnRegisterEventCallback());
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
