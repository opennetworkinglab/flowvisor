package org.flowvisor.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.FlowChange.FlowChangeOp;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.exceptions.MalformedFlowChange;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

public class FVUserAPIXMLRPCImpl extends FVUserAPIImpl implements FVUserAPIXML{

	
	/**
	 * Lists all the flowspace
	 *
	 * @return
	 */
	@Override
	public Collection<String> listFlowSpace() throws ConfigError {
		String[] fs;
		synchronized (FVConfig.class) {
			
			Collection<FlowEntry> flowEntries = getFlowEntries();
			fs = new String[flowEntries.size()];
			int i = 0;
			for (FlowEntry flowEntry : flowEntries)
				fs[i++] = flowEntry.toString();
		}
		return Arrays.asList(fs);
	}

	/**
	 * Implements {@link org.flowvisor.api.FVUserAPI#changeFlowSpace}
	 *
	 * Allow this change if it affects the flowspace delagated to this slice.
	 *
	 * @throws PermissionDeniedException
	 *
	 */

	@Override
	public Collection<String> changeFlowSpace(List<Map<String, String>> changes)
			throws MalformedFlowChange, PermissionDeniedException, ConfigError,
			FlowEntryNotFound {
		String user = APIUserCred.getUserName();
		List<String> returnIDs = new LinkedList<String>();

		// TODO: implement the "delegate" bit; for now only root can change FS
		if (!FVConfig.isSupervisor(APIUserCred.getUserName()))
			throw new PermissionDeniedException(
					"only superusers can add/remove/change the flowspace");

		synchronized (FVConfig.class) { // prevent multiple API clients from
			// stomping
			// on each other
			FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
				
			String logMsg;
			for (int i = 0; i < changes.size(); i++) {
				FlowChange change = FlowChange.fromMap(changes.get(i));
				FlowChangeOp operation = change.getOperation();
				logMsg = "user " + user + " " + operation;
				if (operation != FlowChangeOp.ADD) {
					logMsg += " id=" + change.getId();
					flowSpace.removeRule(change.getId());
					FlowSpaceImpl.getProxy().removeRule(change.getId());
					returnIDs.add(String.valueOf(change.getId()));
				}
				if (operation != FlowChangeOp.REMOVE) {
					logMsg += " for dpid="
							+ FlowSpaceUtil.dpidToString(change.getDpid())
							+ " match=" + change.getMatch() + " priority="
							+ change.getPriority() + " actions="
							+ FlowSpaceUtil.toString(change.getActions());
				
					FlowEntry flowEntry = new FlowEntry(change.getDpid(),
							change.getMatch(), change.getPriority(),
							change.getActions());
					flowEntry.setId(FlowSpaceImpl.getProxy().addRule(flowEntry));

					if (operation == FlowChangeOp.ADD)
						returnIDs.add(String.valueOf(flowEntry.getId()));
					else
						flowEntry.setId(change.getId()); // keep id on change
					flowSpace.addRule(flowEntry);

				}
				FVLog.log(LogLevel.INFO, null, logMsg);
			}
			// update the indexes at the end, not with each rule
			
			FVLog.log(LogLevel.INFO, null,
					"Signalling FlowSpace Update to all event handlers");
			FlowSpaceImpl.getProxy().notifyChange(flowSpace); // signal that FS has
			// changed
			FlowVisor.getInstance().checkPointConfig();
		}
		return returnIDs;
	}
}
