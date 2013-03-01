package org.flowvisor.api;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowvisor.config.ConfigError;
import org.flowvisor.config.InvalidDropPolicy;
import org.flowvisor.config.InvalidSliceName;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.DuplicateControllerException;
import org.flowvisor.exceptions.InvalidUserInfoKey;
import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNotFound;

public interface FVUserAPI {

	/**
	 * For debugging
	 *
	 * @param arg
	 *            test string
	 * @return response test string
	 */
	public String ping(String arg);

	/**
	 * Create a new slice (without flowspace)
	 *
	 * @param sliceName
	 * @param passwd
	 *            Cleartext! FIXME
	 * @param controller_url
	 *            Reference controller pseudo-url, e.g., tcp:hostname[:port]
	 * @param drop_policy
	 * 			  Currently takes either 'exact' or 'rule', defines the 
	 * 				drop policy for a slice when the controller is off-line.
	 * @param slice_email
	 *            As a contract for the slice
	 * @return success
	 * @throws InvalidSliceName
	 * @throws PermissionDeniedException
	 * @throws DuplicateControllerException
	 */

	public Boolean createSlice(String sliceName, String passwd,
			String controller_url, String drop_policy, String slice_email)
			throws MalformedControllerURL, InvalidSliceName, InvalidDropPolicy,
			PermissionDeniedException, DuplicateControllerException;
	
	public Boolean createSlice(String sliceName, String passwd,
			String controller_url, String slice_email)
			throws MalformedControllerURL, InvalidSliceName, InvalidDropPolicy,
			PermissionDeniedException, DuplicateControllerException;

	/**
	 * Changes a key of a slice to value
	 *
	 * Only callable by a the slice owner or a transitive creator
	 *
	 * @param sliceName
	 * @param key
	 * @param value
	 * @return
	 * @throws MalformedURLException
	 * @throws InvalidSliceName
	 * @throws PermissionDeniedException
	 * @throws DuplicateControllerException
	 */

	public Boolean changeSlice(String sliceName, String key, String value)
			throws MalformedURLException, InvalidSliceName,
			PermissionDeniedException, InvalidUserInfoKey, DuplicateControllerException;

	public Map<String, String> getSliceInfo(String sliceName)
			throws PermissionDeniedException, SliceNotFound;
	
	
	/**
	 * Change the password for this slice
	 *
	 * A slice is allowed to change its own password and the password of any
	 * slice that it has (transitively) created
	 *
	 * @param sliceName
	 * @param newPasswd
	 */
	public Boolean changePasswd(String sliceName, String newPasswd)
			throws PermissionDeniedException;

	// have both names, b/c it makes the OM's life easier
	public Boolean change_password(String sliceName, String newPasswd)
			throws PermissionDeniedException;

	/**
	 * Get the list of device DPIDs (e.g., switches, routers, APs) connected to
	 * the FV
	 *
	 * @return
	 */
	public Collection<String> listDevices();

	/**
	 * Get information about a device
	 *
	 * @param dpidStr
	 *            8 colon separated hex bytes, e..g., "00:00:00:00:00:00:00:01"
	 *
	 * @return a map of key=value pairs where the value may itself be a more
	 *         complex object
	 */
	public Map<String, String> getDeviceInfo(String dpidStr)
			throws DPIDNotFound;

	/**
	 * Get the list of links between the devices in getDevices() Links are
	 * directional, so switch1 --> switch2 does not imply the reverse; they will
	 * be both listed if the link is bidirectional
	 *
	 * @return
	 */

	public Collection<Map<String, String>> getLinks();

	/**
	 * Delete the named slice
	 *
	 * Requestor only has permission to delete its own slice or the slice that
	 * it (transitively) created. Since root has transitively created all
	 * slices, root can delete all slices.
	 *
	 * @param sliceName
	 * @return Success
	 * @throws ConfigError 
	 * @throws {@link SliceNotFound}, {@link PermissionDeniedException}
	 */

	public Boolean deleteSlice(String sliceName) throws SliceNotFound,
			PermissionDeniedException, ConfigError;



	/**
	 * Return a list of slices in the flowvisor: root only!
	 *
	 * @return
	 */
	public Collection<String> listSlices() throws PermissionDeniedException;
	
	/**
	 * Set the flood permission for a switch
	 * 
	 * @param dpidStr 
	 * 			8 colon separated hex bytes, e..g., "00:00:00:00:00:00:00:01"
	 * @param floodPerm
	 * 			slice which is allowed to flood this switch.
	 * @return true on success, false on failure
	 * @throws PermissionDeniedException
	 */
	public Boolean setFloodPerm(String dpidStr, String floodPerm) throws PermissionDeniedException;
	
	/**
	 * Get the flood permission for a switch
	 * 
	 * @param dpidStr 
	 * 			8 colon separated hex bytes, e..g., "00:00:00:00:00:00:00:01"
	 * 
	 * @return the name of the slice with permission
	 * @throws PermissionDeniedException
	 */
	public String getFloodPerm(String dpidStr) throws PermissionDeniedException;
	
	/**
	 * Set the default flood permission for all switches
	 * 
	 * @param floodPerm
	 * 			slice which is allowed to flood this switch.
	 * @return true on success, false on failure
	 * @throws PermissionDeniedException
	 */
	public Boolean setFloodPerm(String floodPerm) throws PermissionDeniedException;
	
	/**
	 * Get the default flood permission. 
	 * 
	 * 
	 * @return the name of the slice with permission
	 * @throws PermissionDeniedException
	 */
	public String getFloodPerm() throws PermissionDeniedException;
	
	
	/**
	 * Set the maximum rate of messages a slice can send to a switch.
	 * 
	 * @param sliceName - slice to apply the limit to.
	 * @param rate - the limit
	 * @return
	 * @throws PermissionDeniedException
	 */
	
	public boolean setRateLimit (String sliceName, String rate) 
			throws PermissionDeniedException;
	
	/**
	 * Set the maximum number of flow mods per slice per dpid.
	 * If dpid is set to any then the global slice limit applies.
	 * If no value is set then the number of slices is limitless.
	 * 
	 * @param sliceName - slice to apply the limit to.
	 * @param dpid - dpid to apply limit to
	 * @param maxFlowMods - the limit
	 * @return
	 * @throws PermissionDeniedException
	 */

	public boolean setMaximumFlowMods (String sliceName, String dpid, String maxFlowMods) 
			throws PermissionDeniedException;
	
	/**
	 * Get the maximum per slice per dpid limit.
	 * 
	 * 
	 * 
	 * @param sliceName
	 * @param dpid
	 * @return the limit or -1 if there is not limit.
	 * @throws PermissionDeniedException
	 */
	public Integer getMaximumFlowMods (String sliceName, String dpid)
			throws PermissionDeniedException;
	
	
	/**
	 * Get the current per slice per dpid limit.
	 * 
	 * 
	 * 
	 * @param sliceName
	 * @param dpid
	 * @return the limit or -1 if there is not limit.
	 * @throws PermissionDeniedException
	 */
	public Integer getCurrentFlowMods(String sliceName, String dpid) 
			throws PermissionDeniedException, SliceNotFound, DPIDNotFound;
	
	
	/**
	 * Enable/Disable flow tracking in FlowVisor
	 * 
	 * @param flowtracking 
	 * @return true on success, false on failure
	 * @throws PermissionDeniedException
	 */
	public Boolean setFlowTracking(String flowtracking) throws PermissionDeniedException;
	
	/**
	 * Get flow tracking status in FlowVisor
	 * 
	 * 
	 * @return true on enabled, false on disabled
	 * @throws PermissionDeniedException
	 */
	public Boolean getFlowTracking() throws PermissionDeniedException;

	/**
	 * Returns a list of strings that represents the requested config element
	 *
	 * @param nodeName
	 *            config element name
	 * @return List of strings
	 * @throws ConfigError
	 */
	/*public Collection<String> getConfig(String nodeName) throws ConfigError,
		PermissionDeniedException;*/

	/**
	 * Sets a config element by name
	 *
	 * @param nodeName
	 *            config element name
	 * @param value
	 *            string representation of value
	 * @return success
	 * @throws ConfigError
	 */
	/*public Boolean setConfig(String nodeName, String value) throws ConfigError,
			PermissionDeniedException;*/

	/**
	 * Reload last checkpointed config from disk
	 *
	 * Only available to root
	 *
	 * TODO: implement!
	 *
	 * @return success
	 */
	public Boolean revertToLastCheckpoint();

	/**
	 * Register an XMLRPC URL to be called when the topology changes.
	 *
	 * When the topology changes, FV will make a XMLRPC call to URL with
	 * parameter "cookie"
	 *
	 * @param URL
	 *            XMLRPC Address/proceedure
	 * @param cookie
	 *            opaque string with some meaningful state from the caller
	 * @return success on registering the callback
	 */
	public Boolean registerTopologyChangeCallback(String URL, String methodName, String cookie)
			throws MalformedURLException;

	/**
	 *
	 */
	public String getTopologyCallback();

	/**
	 * Unregister a previously registered callback
	 *
	 *
	 * @return true if successful, false otherwise
	 */
	public Boolean unregisterTopologyChangeCallback();

	/**
	 * Return a multiline string of the slice's stats
	 *
	 * The string is of the form:
	 *
	 * ---SENT--- $switch1 :: $type1=$count1[,$type2=$count2[...]] $switch2 ::
	 * $type1=$count1[,$type2=$count2[...]] Total ::
	 * $type1=$count1[,$type2=$count2[...]] ---DROP--- $switch1 ::
	 * $type1=$count1[,$type2=$count2[...]] $switch2 ::
	 * $type1=$count1[,$type2=$count2[...]] Total ::
	 * $type1=$count1[,$type2=$count2[...]]
	 *
	 * @param sliceName
	 *            which slice do you wants stats for
	 * @return A string of the above form
	 * @throws SliceNotFound
	 * @throws PermissionDeniedException
	 */
	public String getSliceStats(String sliceName) throws SliceNotFound,
			PermissionDeniedException;

	/**
	 * Return a multiline string of the switch's stats
	 *
	 * The string is of the form:
	 *
	 * ---SENT--- $slice1 :: $type1=$count1[,$type2=$count2[...]] $slice2 ::
	 * $type1=$count1[,$type2=$count2[...]] Total ::
	 * $type1=$count1[,$type2=$count2[...]] ---DROP--- $slice1 ::
	 * $type1=$count1[,$type2=$count2[...]] $slice2 ::
	 * $type1=$count1[,$type2=$count2[...]] Total ::
	 * $type1=$count1[,$type2=$count2[...]]
	 *
	 * @param dpid
	 *            of the switchyou wants stats for
	 * @return A string of the above form
	 * @throws DPIDNotFound
	 * @throws PermissionDeniedException
	 */

	public String getSwitchStats(String dpidStr) throws DPIDNotFound,
			PermissionDeniedException;

	/**
	 * Get a List of FlowDBEnty's converted by toBracketMap()
	 *
	 * @param dpid
	 *            a specific switch or "all" for all
	 * @return
	 */
	public Collection<Map<String, String>> getSwitchFlowDB(String dpidstr)
			throws DPIDNotFound;

	/**
	 * Return a map of the flow entries the slice requested to what the
	 * flowvisor produced
	 *
	 * @note KILL ME; this map crap is horrible, but we seemingly can't rely on
	 *       the remote side to support the extensions that serializable needs
	 *       so I have to do this by hand... need to rewrite everything here and
	 *       maybe move to SOAP or ProtoBufs
	 *
	 * @param sliceName
	 * @param dpidstr
	 * @return
	 * @throws DPIDNotFound
	 * @throws SliceNotFound
	 */
	public Map<String, List<Map<String, String>>> getSliceRewriteDB(
			String sliceName, String dpidstr) throws DPIDNotFound,
			SliceNotFound, PermissionDeniedException;
	
	/**
	 * Dumps a copy of the config deployed in a db to the
	 * file specified.
	 * 
	 * @param filename the file to write to.
	 * @throws PermissionDeniedException
	 */
	public String dumpConfig() 
			throws PermissionDeniedException, FileNotFoundException; 

	

	
}
