package org.flowvisor.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.FlowChange.FlowChangeOp;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.TopologyController;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;


public class FVUserAPIJSONImpl extends FVUserAPIImpl implements FVUserAPIJSON {

	@Override
	public Collection<FlowEntry> listFlowSpace() throws ConfigError {
		return getFlowEntries();
	}

	@Override
	public Collection<Integer> changeFlowSpace(List<FlowSpaceChangeRequest> changes)
		throws PermissionDeniedException, FlowEntryNotFound, ConfigError {

		String user = APIUserCred.getUserName();
		List<Integer> changeIDs = new LinkedList<Integer>();

		// TODO: implement the "delegate" bit; for now only root can change FS
		if (!FVConfig.isSupervisor(APIUserCred.getUserName()))
			throw new PermissionDeniedException(
			"only superusers can add/remove/change the flowspace");

		synchronized (FVConfig.class) {

			FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
			String logMsg;
			for (FlowSpaceChangeRequest change : changes){

				FlowEntry entry = change.getEntry();
				FlowChangeOp changeOp = FlowChangeOp.valueOf(change.getChangeType().toUpperCase());
				logMsg = "user " + user + " " + changeOp;
				if(changeOp != FlowChangeOp.ADD){
					logMsg += " id=" + entry.getId();
					flowSpace.removeRule(entry.getId());
					FlowSpaceImpl.getProxy().removeRule(entry.getId());
				}
				if (changeOp != FlowChangeOp.REMOVE) {
					// astruble: set the ID BEFORE adding the rule. Rules are kept in a sorted set based on id
					// and if the ID is changed after adding it wont be in the proper place in the sorted
					// set which will cause problems later.
					if (changeOp == FlowChangeOp.ADD){
						entry.setId(FlowSpaceImpl.getProxy().addRule(entry));
					}
					flowSpace.addRule(entry);
					logMsg += flowspaceAddChangeLogMessage(entry.getDpid(), entry.getRuleMatch(), entry.getPriority(),
							entry.getActionsList());
				}

				changeIDs.add(entry.getId());
				FVLog.log(LogLevel.INFO, null, logMsg);
			}
			// update the indexes at the end, not with each rule
			FlowVisor.getInstance().checkPointConfig();
			FVLog.log(LogLevel.INFO, null,
					"Signalling FlowSpace Update to all event handlers");
			FlowSpaceImpl.getProxy().notifyChange(flowSpace); // signals that FS has changed
			
		}
		return changeIDs;
	}

	private static String flowspaceAddChangeLogMessage(long dpid, OFMatch match, int priority, List<OFAction> actions ){
		return " for dpid=" + FlowSpaceUtil.dpidToString(dpid) + " match=" + match +
		" priority=" + priority + " actions=" + FlowSpaceUtil.toString(actions);
	}

	@Override
	public Boolean registerTopologyEventCallback(String URL, String method, String eventType)
	throws MalformedURLException {
		// this will throw MalformedURL back to the client if the URL is bad
		new URL(URL);
		TopologyController tc = TopologyController.getRunningInstance();
		if (tc != null) {
			tc.registerCallBack("", URL, method, "", eventType);
			return true;
		} else
			return false; // topology server not running
	}

	@Override
	public boolean deregisterTopologyEventCallback(String method,
			String eventType) {
		TopologyController tc = TopologyController.getRunningInstance();
		if (tc != null) {
			tc.deregisterCallback(method, eventType);
			return true;
		} else
			return false; // topology server not running
	}
}
