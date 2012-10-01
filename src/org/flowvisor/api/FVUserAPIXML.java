package org.flowvisor.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowvisor.config.ConfigError;
import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.exceptions.MalformedFlowChange;
import org.flowvisor.exceptions.PermissionDeniedException;

public interface FVUserAPIXML extends FVUserAPI {

	Collection<String> listFlowSpace() throws ConfigError;

	/**
	 * Make changes to the flowspace
	 *
	 * Changes are processed in order and only after the last change is applied
	 * to the changes take affect, i.e., this is transactional
	 *
	 * FIXME: make this more codified; is XMLRPC the right thing here?
	 * Protobufs?
	 *
	 * Each Map should contain the an "operation" element; all keys and values
	 * are strings key="operation", value={CHANGE,ADD,REMOVE}
	 *
	 * remove: { "operation" : "REMOVE:", "id":"4235253" }
	 *
	 * add: { "operation" : "ADD", "priority":"100", "dpid" :
	 * "00:00:23:20:10:25:55:af" "match":"in_port=5,dl_src=00:23:20:10:10:10",
	 * "actions":"Slice=alice:4" }
	 *
	 * change: { "operation": "CHANGE" "id":"4353454", "priority":"105", // new
	 * priority "dpid" : "all", // new dpid
	 * "match":"in_port=5,dl_src=00:23:20:10:10:10", // new match
	 * "actions":"Slice=alice:4" // new actions }
	 *
	 *
	 * The changeFlowSpace() call will return a list of strings, where each
	 * element is an ID. If the operation was a REMOVE or a CHANGE, it's the ID
	 * of the removed/changed entry. If it's an ADD, it's the ID of the new
	 * entry.
	 *
	 * key="dpid", value=8 octet hexcoded string, e.g.,
	 * "00:00:23:20:10:25:55:af" the dpid string will be pushed off to
	 * FlowSpaceUtils.parseDPID()
	 *
	 * key="match", value=dpctl-style OFMatch string, see below
	 *
	 * key="actions", value=comma separated string of SliceActions suitable to
	 * call SliceAction.fromString e.g., "SliceAction:alice=4,SliceAction:bob=2
	 *
	 * FIXME: change perms flags to human readable letters, e.g.,
	 * "(r)read,(w)rite,(d)elegate"
	 *
	 * The "match" value string is a comma separated string of the form
	 * "match_field=value", e.g., "in_port=5,dl_src=00:43:af:35:22:11,tp_src=80"
	 * similar to dpctl from the OpenFlow reference switch. Any field not
	 * explicitly listed is assumed to be wildcarded.
	 *
	 * The string will get wrapped with "OFMatch[" + match_value + "]" and
	 * passed off to OFMatch.fromString("OFMatch[" + match_value + "]") and
	 * generally follows the same convention as dpctl
	 *
	 * @param list
	 *            of changes
	 * @throws MalformedFlowChange
	 * @return A list of flow entry IDs in string form
	 * @throws MalformedFlowChange
	 * @throws PermissionDeniedException
	 * @throws ConfigError 
	 */
	public Collection<String> changeFlowSpace(List<Map<String, String>> changes)
			throws MalformedFlowChange, PermissionDeniedException,
			FlowEntryNotFound, ConfigError;
}
