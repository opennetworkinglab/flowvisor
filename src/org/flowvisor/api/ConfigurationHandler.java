package org.flowvisor.api;

import java.util.HashMap;

import org.flowvisor.api.handlers.AddSlice;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.ListSlices;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class ConfigurationHandler implements RequestHandler {

	@SuppressWarnings( { "serial", "rawtypes" } )
	HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {{
		put("list-slices", new ListSlices());
		put("add-slice", new AddSlice());
	}};
	
	
	@Override
	public String[] handledRequests() {
		return handlers.keySet().toArray(new String[]{});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctxt) {
		ApiHandler m = handlers.get(req.getMethod());
		try {
			if (m != null) {
				
				switch (m.getType()) {
				case NO_PARAMS:
					return m.process(null);
				case ARRAY:
					return m.process(req.getPositionalParams());
				case OBJECT:
					return m.process(req.getNamedParams());
				}
			}
		} catch (ClassCastException e) {
			/*FVLog.log(LogLevel.WARN, null, req.getMethod(), "requires a ",
					m.getType().toString(), " and not a ", 
					req.getParamsType());*/
			System.out.println(m.getType());
			return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					req.getMethod() + " requires a " + m.getType() + 
					" and not a " + req.getParamsType()),
					req.getID());
		}
		return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
	}

}
