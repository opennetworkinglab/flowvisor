package org.flowvisor.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;

import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFFlowMod;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class SwitchImpl implements Switch {



	private static SwitchImpl instance = null;
	
	//Callbacks
	private static String FFLOOD = "setFloodPerm";
	
	// STATEMENTS
	private static String GFLOODSQL = "SELECT " + FLOOD + " FROM "  + TSWITCH + " WHERE " + DPID + " = ?";
	private static String GALL = "SELECT * FROM " + TSWITCH;
	private static String GSWITCHID = "SELECT id FROM " + TSWITCH + " WHERE " + DPID + " = ?";
	private static String GSLICEID = "SELECT id FROM " + Slice.TSLICE + " WHERE " + Slice.SLICE + " = ?";
	
	//private static String GFLOWRULEID = "SELECT id FROM " + FlowSpace.FS + " WHERE " + DPID + " = ?";  
	
	private static String SFLOODSQL = "UPDATE " +  TSWITCH + " SET " + FLOOD + "= ? WHERE " + DPID + " = ?";	
	private static String SFLOWENTRY = "INSERT INTO FlowTableEntry(switch_id,slice_id,cookie_id," + FlowSpace.PRIO + "," +  
			FlowSpace.INPORT + "," + FlowSpace.VLAN + "," + FlowSpace.VPCP + "," + FlowSpace.DLSRC + "," + FlowSpace.DLDST + "," + FlowSpace.DLTYPE + "," +
			FlowSpace.NWSRC + "," + FlowSpace.NWDST + "," + FlowSpace.NWPROTO + "," + FlowSpace.NWTOS + "," + FlowSpace.TPSRC + "," + FlowSpace.TPDST + ","
			+ FlowSpace.WILDCARDS+ ") " + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static String SCOOKIE = "INSERT INTO COOKIE(contorller_cookie, fv_cookie) VALUES(?,?)";
	
	private static String CREATESWITCH = "INSERT INTO " + TSWITCH + "(" + DPID + "," + MFRDESC + "," + FLOOD + ","
			+ HWDESC + "," + SWDESC + "," + SERIAL + "," + DPDESC + "," + CAPA + ") VALUES(?,?,?,?,?,?,?,?)"; 
	
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
				throw new ConfigError("Flood permission for dpid " + dpid + " not found");
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
			if (ps.executeUpdate() == 0)
				throw new ConfigError("Unable to set flood permission for dpid " + dpid);
			notify(dpid, FFLOOD, flood_perm);
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
		settings.returnConnection(conn);
		try {
			conn.close();
		} catch (SQLException e) {
			// don't care
		}
	}
	
	
	
	@Override
	public void toJson(JsonWriter writer) throws IOException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GALL);
			set = ps.executeQuery();
			//writer.beginObject();
			writer.name(TSWITCH);
			writer.beginArray();
			while (set.next()) {
				writer.beginObject();
				writer.name(DPID).value(Long.toString(set.getLong(DPID)));
				writer.name(FLOOD).value(set.getString(FLOOD));
				writer.name(MFRDESC).value(set.getString(MFRDESC));
				writer.name(HWDESC).value(set.getString(HWDESC));
				writer.name(SWDESC).value(set.getString(SWDESC));
				writer.name(SERIAL).value(set.getString(SERIAL));
				writer.name(DPDESC).value(set.getString(DPDESC));
				writer.name(CAPA).value(set.getInt(CAPA));
				writer.endObject();
			}
			writer.endArray();
			//writer.endObject();
				
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
			
		}
	}

	@Override
	public void fromJson(JsonReader reader) throws IOException {
		HashMap<String, Object> row = new HashMap<String , Object>();
		String key = null;
		Object value = null;
		while (true) {
			switch (reader.peek()) {
				case BEGIN_ARRAY:
					reader.beginArray();
					break;
				case BEGIN_OBJECT:
					reader.beginObject();
					break;
				case BOOLEAN:
					value = reader.nextBoolean();
					break;
				case END_DOCUMENT:
					throw new IOException("Unexpected EOF while parsing config file.");
				case END_OBJECT:
					reader.endObject();
					insert(row);
					row.clear();
					key = null;
					value = null;
					break;
				case END_ARRAY:
					reader.endArray();
					return;
				case NAME:
					key = reader.nextName();
					break;
				case NULL:
					reader.nextNull();
					if (key != null) {
						row.put(key, value);
						key = null;
					}
					break;
				case NUMBER:
					value = reader.nextLong();
					break;
				case STRING:
					value = reader.nextString();
					break;
				default:
					reader.skipValue();
			}
			if (key != null && value != null) {
				row.put(key, value);
				key = null;
				value = null;
			}
		}
	}
	
	private void insert(HashMap<String, Object> row) throws IOException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		//int switchid = -1;
		//int ruleid = -1;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(CREATESWITCH, Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, (Long) row.get(DPID));
			ps.setString(2, (String) row.get(FLOOD));
			ps.setString(3, (String) row.get(MFRDESC));
			ps.setString(4, (String) row.get(HWDESC));
			ps.setString(5, (String) row.get(SWDESC));
			ps.setString(6, (String) row.get(SERIAL));
			ps.setString(7, (String) row.get(DPDESC));
			ps.setInt(8, ((Long) row.get(CAPA)).intValue());
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Insertion failed... siliently.");
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
		} finally {
			close(set);
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



	

	
}

