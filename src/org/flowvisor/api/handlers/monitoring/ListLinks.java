package org.flowvisor.api.handlers.monitoring;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.TopologyController;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ListLinks implements ApiHandler<Object> {

	
	
	@Override
	public JSONRPC2Response process(Object params) {
		JSONRPC2Response resp = null;
		
		resp = new JSONRPC2Response(getLinks(), 0);
		 
		return resp;
		
	}
	
	public Collection<Map<String, String>> getLinks() {
		FVLog.log(LogLevel.DEBUG, null,
				"API getLinks() by: " + APIUserCred.getUserName());
		TopologyController topologyController = TopologyController
				.getRunningInstance();
		if (topologyController == null)
			return getFakeLinks();
		List<Map<String, String>> list = new LinkedList<Map<String, String>>();
		for (Iterator<LinkAdvertisement> it = topologyController.getLinks()
				.iterator(); it.hasNext();) {
			LinkAdvertisement linkAdvertisement = it.next();
			list.add(linkAdvertisement.toMap());
		}
		return list;
	}
	
	protected List<Map<String, String>> getFakeLinks() {
		FVLog.log(LogLevel.ALERT, null,
				"API: topology server not running: faking getLinks()");
		List<String> devices = HandlerUtils.getAllDevices();
		List<Map<String, String>> list = new LinkedList<Map<String, String>>();
		for (int i = 0; i < devices.size(); i++) {
			// forward direction
			LinkAdvertisement link = new LinkAdvertisement(devices.get(i), (short) 0, 
					devices.get((i + 1) % devices.size()), (short) 1 );
			link.setAttribute("fakeLink", "true");
			list.add(link.toMap());
		}
		return list;
	}

	

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.NO_PARAMS;
	}

	@Override
	public String cmdName() {
		return "list-links";
	}
	


}
