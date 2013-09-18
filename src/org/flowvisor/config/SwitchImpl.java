package org.flowvisor.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFFlowMod;

public class SwitchImpl implements Switch {

	private static SwitchImpl instance = null;
	
	//Callbacks
	private static String FFLOOD = "setFloodPerm";
	private static String FFMLIMIT = "setFlowModLimit";
	private static String FRATELIMIT = "setRateLimit";
	
	// STATEMENTS
	private static String DSWITCH = "DELETE FROM " + TSWITCH;
	private static String RESETSWITCH = "ALTER TABLE " + TSWITCH + " ALTER COLUMN id RESTART WITH 1";
	
	private static String GFLOODSQL = "SELECT " + FLOOD + " FROM "  + TSWITCH + " WHERE " + DPID + " = ?";
	private static String GALL = "SELECT * FROM " + TSWITCH;
	private static String GSWITCHID = "SELECT id FROM " + TSWITCH + " WHERE " + DPID + " = ?";
	private static String GSLICEID = "SELECT id FROM " + Slice.TSLICE + " WHERE " + Slice.SLICE + " = ?";
	
	private static String GSLICENAME = "SELECT " + Slice.SLICE + " FROM " + Slice.TSLICE + " WHERE id = ?";
	
	private static String GALLLIMITS = "SELECT * FROM " + TLIMIT + " WHERE " + SWITCH_ID + "  = ?";
	
	//private static String GFLOWRULEID = "SELECT id FROM " + FlowSpace.FS + " WHERE " + DPID + " = ?";  
	
	private static String SFLOODSQL = "UPDATE " +  TSWITCH + " SET " + FLOOD + "= ? WHERE " + DPID + " = ?";	
	private static String SFLOWENTRY = "INSERT INTO FlowTableEntry(switch_id,slice_id,cookie_id," + FlowSpace.PRIO + "," +  
			FlowSpace.INPORT + "," + FlowSpace.VLAN + "," + FlowSpace.VPCP + "," + FlowSpace.DLSRC + "," + FlowSpace.DLDST + "," + FlowSpace.DLTYPE + "," +
			FlowSpace.NWSRC + "," + FlowSpace.NWDST + "," + FlowSpace.NWPROTO + "," + FlowSpace.NWTOS + "," + FlowSpace.TPSRC + "," + FlowSpace.TPDST + ","
			+ FlowSpace.WILDCARDS+ ") " + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static String SCOOKIE = "INSERT INTO COOKIE(controller_cookie, fv_cookie) VALUES(?,?)";
	
	private static String CREATESWITCH = "INSERT INTO " + TSWITCH + "(" + DPID + "," + MFRDESC + "," + FLOOD + ","
			+ HWDESC + "," + SWDESC + "," + SERIAL + "," + DPDESC + "," + CAPA + ") VALUES(?,?,?,?,?,?,?,?)"; 
	
	private static String CHECKLIMIT = "SELECT id FROM " + TLIMIT + " WHERE " + SLICE_ID + 
							" = ? AND " + SWITCH_ID + " = ?";
	
	private static String ULIMIT = "UPDATE " + TLIMIT + " SET " + FMLIMIT + " = ? WHERE id = ?";
	private static String SLIMIT = "INSERT INTO " + TLIMIT + "(" + SLICE_ID + "," + SWITCH_ID + 
						"," + FMLIMIT + ") VALUES(?,?,?)";
	
	private static String URATELIMIT = "UPDATE " + TLIMIT + " SET " + RATELIMIT + " = ? WHERE id = ?";
	private static String SRATELIMIT = "INSERT INTO " + TLIMIT + "(" + SLICE_ID + "," + SWITCH_ID + 
						"," + RATELIMIT + ") VALUES(?,?,?)";
	
	
	
	private static String GLIMIT = "SELECT " + FMLIMIT + " FROM " + TLIMIT + " AS L, " + Slice.TSLICE 
							+ " AS S, " + TSWITCH + " AS SW WHERE SW.id = L." + SWITCH_ID + " AND S.id = L." + SLICE_ID 
							+ " AND S." + Slice.SLICE + " = ? AND SW." + DPID + " = ?"; 
	
	private static String GRATELIMIT = "SELECT " + RATELIMIT + " FROM " + TLIMIT + " AS L, " + Slice.TSLICE 
			+ " AS S, " + TSWITCH + " AS SW WHERE SW.id = L." + SWITCH_ID + " AND S.id = L." + SLICE_ID 
			+ " AND S." + Slice.SLICE + " = ? AND SW." + DPID + " = ?"; 
	
	private static String DFLOWENTRY = "DELETE FROM FlowTableEntry WHERE id = ?";

	private ConfDBSettings settings = null;
		
	private SwitchImpl() {}
	
	private static SwitchImpl getInstance() {
		if (instance == null)
			instance = new SwitchImpl();
		return instance;
	}
	
	@Override
	public String getFloodPerm(Long dpid) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GFLOODSQL);
			ps.setLong(1, dpid);
			set = ps.executeQuery();
			if (set.next())
				return set.getString(FLOOD);
			else 
				return "";
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
			
		}
		return null;
	}

	@Override
	public void setFloodPerm(Long dpid, String flood_perm) throws ConfigError{
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SFLOODSQL);
			ps.setString(1, flood_perm);
			ps.setLong(2, dpid);
			if (ps.executeUpdate() == 0) {
				createSwitch(dpid);
				ps.executeUpdate();
			}
			notify(dpid, FFLOOD, flood_perm);
		} catch (SQLException e) {
			throw new ConfigError("Unable to set flood permission for dpid " + dpid);
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}	

	}
	
	@Override
	public Integer getMaxFlowMods(String sliceName, Long dp) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GLIMIT);
			ps.setString(1, sliceName);
			ps.setLong(2, dp);
			set = ps.executeQuery();
			if (set.next())
				return set.getInt(FMLIMIT);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
		return -1;
	}

	@Override
	public void setMaxFlowMods(String sliceName, Long dpid, int limit)
			throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		int sliceid, switchid = -1;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GSLICEID);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				sliceid = set.getInt("id");
			else 
				throw new ConfigError("Unknown slice " + sliceName);
			ps = conn.prepareStatement(GSWITCHID);
			ps.setLong(1, dpid);
			set = ps.executeQuery();
			if (set.next()) 
				switchid = set.getInt("id");
			else {
				ps = conn.prepareStatement(CREATESWITCH, Statement.RETURN_GENERATED_KEYS);
				ps.setLong(1, dpid);
				ps.setString(2, "");
				ps.setString(3, "");
				ps.setString(4, "");
				ps.setString(5, "");
				ps.setString(6, "");
				ps.setString(7, "");
				ps.setInt(8, -1);
				ps.executeUpdate();
				set = ps.getGeneratedKeys();
				set.next();
				switchid = set.getInt(1);
			}
			ps = conn.prepareStatement(CHECKLIMIT);
			ps.setInt(1, sliceid);
			ps.setInt(2, switchid);
			set = ps.executeQuery();
			if (set.next()) {
				int id = set.getInt("id");
				ps = conn.prepareStatement(ULIMIT);
				ps.setInt(1, limit);
				ps.setInt(2, id);
				if (ps.executeUpdate() == 0)
					throw new ConfigError("Unable to update slice flow mod limit for dpid " + dpid);
			} else {
				ps = conn.prepareStatement(SLIMIT);
				ps.setInt(1, sliceid);
				ps.setInt(2, switchid);
				ps.setInt(3, limit);
				if (ps.executeUpdate() == 0)
					throw new ConfigError("Unable to insert slice flow mod limit for dpid " + dpid);
			}
			HashMap<String, Object> values = new HashMap<String, Object>();
			values.put(Slice.SLICE, sliceName);
			values.put("LIMIT", limit);
			notify(dpid, FFMLIMIT, values);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
		
	}
	
	@Override
	public Integer getRateLimit(String sliceName, Long dp) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GRATELIMIT);
			ps.setString(1, sliceName);
			ps.setLong(2, dp);
			set = ps.executeQuery();
			if (set.next())
				return set.getInt(RATELIMIT);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
		return -1;
	}
	
	public void setRateLimit(String sliceName, Long dpid, int rate) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		int sliceid, switchid = -1;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GSLICEID);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				sliceid = set.getInt("id");
			else 
				throw new ConfigError("Unknown slice " + sliceName);
			ps = conn.prepareStatement(GSWITCHID);
			ps.setLong(1, dpid);
			set = ps.executeQuery();
			if (set.next()) 
				switchid = set.getInt("id");
			else {
				ps = conn.prepareStatement(CREATESWITCH, Statement.RETURN_GENERATED_KEYS);
				ps.setLong(1, dpid);
				ps.setString(2, "");
				ps.setString(3, "");
				ps.setString(4, "");
				ps.setString(5, "");
				ps.setString(6, "");
				ps.setString(7, "");
				ps.setInt(8, -1);
				ps.executeUpdate();
				set = ps.getGeneratedKeys();
				set.next();
				switchid = set.getInt(1);
			}
			ps = conn.prepareStatement(CHECKLIMIT);
			ps.setInt(1, sliceid);
			ps.setInt(2, switchid);
			set = ps.executeQuery();
			if (set.next()) {
				int id = set.getInt("id");
				ps = conn.prepareStatement(URATELIMIT);
				ps.setInt(1, rate);
				ps.setInt(2, id);
				if (ps.executeUpdate() == 0)
					throw new ConfigError("Unable to update slice rate limit for dpid " + dpid);
			} else {
				ps = conn.prepareStatement(SRATELIMIT);
				ps.setInt(1, sliceid);
				ps.setInt(2, switchid);
				ps.setInt(3, rate);
				if (ps.executeUpdate() == 0)
					throw new ConfigError("Unable to insert slice rate limit for dpid " + dpid);
			}
			HashMap<String, Object> values = new HashMap<String, Object>();
			values.put(Slice.SLICE, sliceName);
			values.put("RATELIMIT", rate);
			notify(dpid, FRATELIMIT, values);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
	}
	
	public int pushFlowMod(OFFlowMod flowMod, String sliceName,
			long dpid) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		int switchid = -1;
		int sliceid = -1;
		int cookieid = -1;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GSLICEID);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				sliceid = set.getInt("id");
			else {
				FVLog.log(LogLevel.WARN, null, "Unknown slice "+ sliceName + " when pushing flow mod to db. Returning...");
				return 0;
			}
			ps = conn.prepareStatement(GSWITCHID);
			ps.setLong(1, dpid);
			set = ps.executeQuery();
			if (set.next()) 
				switchid = set.getInt("id");
			else {
				ps = conn.prepareStatement(CREATESWITCH, Statement.RETURN_GENERATED_KEYS);
				ps.setLong(1, dpid);
				ps.setString(2, "");
				ps.setString(3, "");
				ps.setString(4, "");
				ps.setString(5, "");
				ps.setString(6, "");
				ps.setString(7, "");
				ps.setInt(8, -1);
				ps.executeUpdate();
				set = ps.getGeneratedKeys();
				set.next();
				switchid = set.getInt(1);
			}
			ps = conn.prepareStatement(SCOOKIE);
			ps.setLong(1, flowMod.getCookie());
			// TODO virtualize cookies
			ps.setLong(2, flowMod.getCookie());
			ps.executeUpdate();
			set = ps.getGeneratedKeys();
			set.next();
			cookieid = set.getInt(1);
			int wildcards = flowMod.getMatch().getWildcards();
			ps = conn.prepareStatement(SFLOWENTRY, Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, switchid);
			ps.setInt(2, sliceid);
			ps.setInt(3, cookieid);
			ps.setInt(4, flowMod.getPriority());
			ps.setShort(5, flowMod.getMatch().getInputPort());
			if ((wildcards & FVMatch.OFPFW_DL_VLAN) != 0)
				ps.setNull(6, Types.SMALLINT);
			else
				ps.setShort(6, flowMod.getMatch().getDataLayerVirtualLan());
			
			if ((wildcards & FVMatch.OFPFW_DL_VLAN_PCP) != 0)
				ps.setNull(7, Types.SMALLINT);
			else
				ps.setShort(7, flowMod.getMatch().getDataLayerVirtualLanPriorityCodePoint());
			
			if ((wildcards & FVMatch.OFPFW_DL_SRC) != 0)
				ps.setNull(8, Types.BIGINT);
			else
				ps.setLong(8, FlowSpaceUtil.toLong(flowMod.getMatch().getDataLayerSource()));
		
			if ((wildcards & FVMatch.OFPFW_DL_DST) != 0)
				ps.setNull(9, Types.BIGINT);
			else
				ps.setLong(9, FlowSpaceUtil.toLong(flowMod.getMatch().getDataLayerDestination()));
			
			if ((wildcards & FVMatch.OFPFW_DL_TYPE) != 0)
				ps.setNull(10, Types.SMALLINT);
			else
				ps.setShort(10, flowMod.getMatch().getDataLayerType());
			
			if ((wildcards & FVMatch.OFPFW_NW_SRC_ALL) != 0)
				ps.setNull(11, Types.INTEGER);
			else
				ps.setInt(11, flowMod.getMatch().getNetworkSource());
			
			if ((wildcards & FVMatch.OFPFW_NW_DST_ALL) != 0)
				ps.setNull(12, Types.INTEGER);
			else
				ps.setInt(12, flowMod.getMatch().getNetworkDestination());
			
			if ((wildcards & FVMatch.OFPFW_NW_PROTO) != 0)
				ps.setNull(13, Types.SMALLINT);
			else
				ps.setShort(13, flowMod.getMatch().getNetworkProtocol());
			
			if ((wildcards & FVMatch.OFPFW_NW_TOS) != 0)
				ps.setNull(14, Types.SMALLINT);
			else
				ps.setShort(14, flowMod.getMatch().getNetworkTypeOfService());
			
			if ((wildcards & FVMatch.OFPFW_TP_SRC) != 0)
				ps.setNull(15, Types.SMALLINT);
			else
				ps.setShort(15, flowMod.getMatch().getTransportSource());
			
			if ((wildcards & FVMatch.OFPFW_TP_DST) != 0)
				ps.setNull(16, Types.SMALLINT);
			else
				ps.setShort(16, flowMod.getMatch().getTransportDestination());
			
			ps.setInt(17, wildcards);
			ps.executeUpdate();
			set = ps.getGeneratedKeys();
			set.next();
			return set.getInt(1);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
		return 0;
	}
	
	@Override
	public void pullFlowMod(int id) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(DFLOWENTRY);
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}	
		
	}
	
	@Override
	public void close(Connection conn) {
		//settings.returnConnection(conn);
		try {
			conn.close();
		} catch (SQLException e) {
			// don't care
		}
	}
	
	
	
	@Override
	public HashMap<String, Object> toJson(HashMap<String, Object> output) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		HashMap<String, Object> sws = new HashMap<String, Object>();
		HashMap<String, HashMap<String, Integer>> limit = new HashMap<String, HashMap<String, Integer>>();
		LinkedList<Object> list = new LinkedList<Object>();
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GALL);
			set = ps.executeQuery();
			//writer.name(SWITCH);
			//writer.beginArray();
			while (set.next()) {
				//writer.beginObject();
				sws.put(DPID, FlowSpaceUtil.dpidToString(set.getLong(DPID)));
				sws.put(FLOOD, set.getString(FLOOD));
				sws.put(MFRDESC, set.getString(MFRDESC));
				sws.put(HWDESC, set.getString(HWDESC));
				sws.put(SWDESC, set.getString(SWDESC));
				sws.put(SERIAL, set.getString(SERIAL));
				sws.put(DPDESC, set.getString(DPDESC));
				sws.put(CAPA, set.getInt(CAPA));
				
				PreparedStatement limits = conn.prepareStatement(GALLLIMITS);
				limits.setInt(1, set.getInt("id"));
				ResultSet limitSet = limits.executeQuery();
				int columns = limitSet.getMetaData().getColumnCount();
				//boolean found = false;
				while (limitSet.next()) {
					
					ps = conn.prepareStatement(GSLICENAME);
					ps.setInt(1, limitSet.getInt(SLICE_ID));
					ResultSet nameset = ps.executeQuery();
					String sliceName = null;
					if (nameset.next())
						sliceName = nameset.getString(1);
					else
						sliceName = "unkown slice";
					limit.put(sliceName, new HashMap<String, Integer>());
					for (int i = 1; i <= columns ; i++) {
						if (!limitSet.getMetaData().getColumnName(i).contains("ID"))
							limit.get(sliceName).put(limitSet.getMetaData().getColumnName(i).toLowerCase(), 
									limitSet.getInt(limitSet.getMetaData().getColumnName(i)));
						
					}
					
				}
				sws.put(LIMITS, limit.clone());
				limit.clear();
				list.add(sws.clone());
				sws.clear();
				
			}
			output.put(SWITCH, list);
			
		} catch (SQLException e) {
			e.printStackTrace();
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
			
		}
		return output;
	}

	@Override
	public void fromJson(ArrayList<HashMap<String, Object>> list) throws IOException {
		for (HashMap<String, Object> row : list)
			insert(row);
	}
	
	private void insert(HashMap<String, Object> row) throws IOException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
	
		reset();
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(CREATESWITCH, Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, FlowSpaceUtil.parseDPID(((String) row.get(DPID))));
			ps.setString(2, (String) row.get(FLOOD));
			ps.setString(3, (String) row.get(MFRDESC));
			ps.setString(4, (String) row.get(HWDESC));
			ps.setString(5, (String) row.get(SWDESC));
			ps.setString(6, (String) row.get(SERIAL));
			ps.setString(7, (String) row.get(DPDESC));
			ps.setInt(8, ((Double) row.get(CAPA)).intValue());
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Insertion failed... siliently.");
			@SuppressWarnings("unchecked")
			HashMap<String, HashMap<String, Object>> limits = (HashMap<String, HashMap<String, Object>>) row.get(LIMITS);
			for (Entry<String, HashMap<String, Object>> entry : limits.entrySet()) {
				String sliceName = entry.getKey();
				HashMap<String, Object> lims = entry.getValue();
				setMaxFlowMods(sliceName, FlowSpaceUtil.parseDPID(((String) row.get(DPID))), ((Double)lims.get(FMLIMIT)).intValue());
				setRateLimit(sliceName, FlowSpaceUtil.parseDPID(((String) row.get(DPID))), ((Double)lims.get(RATELIMIT)).intValue());
			}
			
			/*
			 * FIXME Selecting the flow rule id doesn't make sense. 
			 * 
			 * 1. Because a flow rule may not have a dpid.
			 * 2. A flow space rule may map to multiple switchs.
			 * 3. The relation between flow space rule and switch should be 
			 * 		established by the flowtracker and should not be persistent. 
			 */
			/*set = ps.getGeneratedKeys();
			set.next();
			switchid = set.getInt(1);
			ps = conn.prepareStatement(GFLOWRULEID);
			ps.setLong(1, (Long) row.get(DPID));
			set = ps.executeQuery();
			set.next();
			ruleid = set.getInt(1);*/
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ConfigError ce) {
				ce.printStackTrace();
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
		
	}
	
	private int createSwitch(long dpid) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(CREATESWITCH, Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, dpid);
			ps.setString(2, "");
			ps.setString(3, "");
			ps.setString(4, "");
			ps.setString(5, "");
			ps.setString(6, "");
			ps.setString(7, "");
			ps.setInt(8, -1);
			ps.executeUpdate();
			set = ps.getGeneratedKeys();
			set.next();
			return set.getInt(1);
		} catch (SQLException e) {
			throw new ConfigError("Switch element creation failed : " + e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
		}
	}
	
	
	private void reset() {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(DSWITCH);
			ps.execute();
			ps = conn.prepareStatement(RESETSWITCH);
			ps.execute();
		} catch (SQLException e) {
			System.err.println("Reseting index on switch failed : " + e.getMessage());
		} finally {
			close(ps);
			close(conn);
		}
	}
	
	@Override
	public void close(Object o) {
		try {
			o.getClass().getMethod("close", (Class<?>) null).invoke(null,(Object[]) null);
		} catch (Exception e) {
			// Don't care, haha!
		}
	}

	@Override
	public void notify(Object key, String method, Object newValue) {
		FVConfigurationController.instance().fireChange(key, method, newValue);
		
	}
	
	@Override
	public void setSettings(ConfDBSettings settings) {
		this.settings = settings;
	}

	public static Switch getProxy() {
		return (Switch) FVConfigurationController.instance()
		.getProxy(getInstance());
	}
	
	public static void addListener(long dpid, SwitchChangedListener l) {
		FVConfigurationController.instance().addChangeListener(dpid, l);
	}
	
	public static void removeListener(long dpid, FlowvisorChangedListener l) {
		FVConfigurationController.instance().removeChangeListener(dpid, l);
	}

	private void processAlter(String alter) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(alter);
			ps.execute();
		} catch (SQLException e) {
			System.err.println("WARN: " + e.getMessage());
		} finally {
			close(ps);
			close(conn);
		}
	}

	@Override
	public void updateDB(int version) {
		FVLog.log(LogLevel.INFO, null, "Updating Switch database table.");
		if (version == 0) {
			processAlter("CREATE TABLE jSliceSwitchLimits ( " +
					"id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
					"slice_id INT NOT NULL, " +
					"switch_id INT NOT NULL, " +
					"maximum_flow_mods INT NOT NULL DEFAULT -1, " +
					"PRIMARY KEY (id))");
			processAlter("CREATE INDEX slice_limit_index ON jSliceSwitchLimits (slice_id ASC)");
			processAlter("CREATE INDEX switch_limit_index ON jSliceSwitchLimits (switch_id ASC)");
			processAlter("ALTER TABLE JSliceSwitchLimits " +
					"ADD CONSTRAINT limit_to_switch_fk FOREIGN KEY (switch_id) " +
					"REFERENCES Switch (id) ON DELETE CASCADE");
			processAlter("ALTER TABLE jSliceSwitchLimits " +
					"ADD CONSTRAINT limit_to_slice_fk FOREIGN KEY (slice_id) " +
					"REFERENCES Slice (id) ON DELETE CASCADE");
			version++;
		}
		if (version == 1) {
			processAlter("ALTER TABLE jSliceSwitchLimits ADD COLUMN " + RATELIMIT + " INT NOT NULL DEFAULT -1");
		}
		
	}
	
}

