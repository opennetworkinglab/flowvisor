package org.flowvisor.config;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.SortedSet;


import org.flowvisor.flows.FederatedFlowMap;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.sql.Types;

public class FlowSpaceImpl implements FlowSpace {

	private ConfDBSettings settings = null;
	private static FlowSpaceImpl instance =  null;
	
	
	//Callbacks
	
	/*
	 *  This is set static for now because we need to
	 *  notify a change from outside this class because
	 *  of the LinearFlowmap and slice deletion
	 *  
	 *  In the future only set the new flowrule.
	 * 	This will require a significant change. 
	 *  
	 *   FIXME
	 */
	
	public static String FFLOWMAP = "flowMapChanged";
	
	// STATEMENTS
	private static String GFLOWMAP = "SELECT FSR.*,S." + Slice.FMTYPE + 
			" FROM FlowSpaceRule AS FSR, Slice AS S, JFSRSlice AS J WHERE FSR.id" +
			"=J.flowspacerule_id AND J.slice_id=S.id";
	private static String GSLICEID = "SELECT id FROM Slice WHERE " + Slice.SLICE + "= ?";
	private static String DFLOWMAP = "DELETE FROM FlowSpaceRule";
	private static String DFLOWRULE = "DELETE FROM FlowSpaceRule WHERE id = ?";
	
	private static String GACTIONS = "SELECT " + ACTION + ",S." + Slice.SLICE + " FROM jFSRSlice as J," +
			"Slice as S  WHERE flowspacerule_id=? and slice_id = S.id";
	private static String SFLOWMAP = "INSERT INTO FlowSpaceRule(" + DPID + "," + PRIO + "," +  
			INPORT + "," + VLAN + "," + VPCP + "," + DLSRC + "," + DLDST + "," + DLTYPE + "," +
			NWSRC + "," + NWDST + "," + NWPROTO + "," + NWTOS + "," + TPSRC + "," + TPDST + ","+ WILDCARDS+ ") " +
			" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static String SACTIONS = "INSERT INTO jFSRSlice(flowspacerule_id, slice_id," + ACTION + ")" +
			" VALUES(?,?,?)";
	
	private static String SLICEID = "SELECT id FROM " + Slice.TSLICE + " WHERE " + Slice.SLICE + " = ?";
	
	private static String RESETFLOWTABLE = "ALTER TABLE FlowSpaceRule ALTER COLUMN id RESTART WITH 1";
	
	private FlowSpaceImpl() {}
	
	private static FlowSpaceImpl getInstance() {
		if (instance == null)
			instance = new FlowSpaceImpl();
		return instance;
	}

	/*
	 * In theory, we could return a different flowmap per slice.
	 * Currently the assumption is that either each slice maintains
	 * the flowmap relevant to itself or has a reference to the 
	 * entire flowspace. 
	 * 
	 * If we want to support different flowmaps per slice we simply
	 * need to pass the slice name to this method and adapt the 
	 * sql accordingly.
	 * 
	 */
	@Override
	public FlowMap getFlowMap() throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement actions = null;
		ResultSet set = null;
		ResultSet actionSet = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GFLOWMAP);
			set = ps.executeQuery();
			FlowMap map = null;
			LinkedList<OFAction> actionsList = null;
			FlowEntry fe = null;
			SliceAction act = null;
			int wildcards = -1;
			while (set.next()) {
				if (map == null)
					map = FlowSpaceUtil.getNewFlowMap(set.getInt(Slice.FMTYPE));
				FVMatch match = new FVMatch();
				actionsList = new LinkedList<OFAction>();
				wildcards = set.getInt(WILDCARDS);
				match.setInputPort(set.getShort(INPORT));
				if ((wildcards & FVMatch.OFPFW_DL_VLAN) == 0)
					match.setDataLayerVirtualLan(set.getShort(VLAN));
				if ((wildcards & FVMatch.OFPFW_DL_VLAN_PCP) == 0)
					match.setDataLayerVirtualLanPriorityCodePoint(set.getByte(VPCP));
				if ((wildcards & FVMatch.OFPFW_DL_SRC) == 0)
					match.setDataLayerSource(set.getLong(DLSRC));
				if ((wildcards & FVMatch.OFPFW_DL_DST) == 0)
					match.setDataLayerDestination(set.getLong(DLDST));
				if ((wildcards & FVMatch.OFPFW_DL_TYPE) == 0)
					match.setDataLayerType(set.getShort(DLTYPE));
				if ((wildcards & FVMatch.OFPFW_NW_SRC_ALL) == 0)
					match.setNetworkSource(set.getInt(NWSRC));
				if ((wildcards & FVMatch.OFPFW_NW_DST_ALL) == 0)
					match.setNetworkDestination(set.getInt(NWDST));
				if ((wildcards & FVMatch.OFPFW_NW_PROTO) == 0)
					match.setNetworkProtocol(set.getByte(NWPROTO));
				if ((wildcards & FVMatch.OFPFW_NW_TOS) == 0)
					match.setNetworkTypeOfService(set.getByte(NWTOS));
				if ((wildcards & FVMatch.OFPFW_TP_SRC) == 0)
					match.setTransportSource(set.getShort(TPSRC));
				if ((wildcards & FVMatch.OFPFW_TP_DST) == 0)
					match.setTransportDestination(set.getShort(TPDST));
				match.setWildcards(wildcards);
				
				actions = conn.prepareStatement(GACTIONS);
				actions.setInt(1, set.getInt("id"));
				actionSet = actions.executeQuery();
				while (actionSet.next()) {
					act = new SliceAction(actionSet.getString(Slice.SLICE), actionSet.getInt(ACTION));
					actionsList.add(act);
				}
				fe = new FlowEntry(set.getLong(DPID), match, set.getInt("id"),set.getInt(PRIO) , actionsList);
				map.addRule(fe);
			}
			if (map == null)
				map = new FederatedFlowMap();
			return map;
		} catch (SQLException e) {
			throw new ConfigError("Unable to retrieve flowmap from db : " + e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(actionSet);
			close(actions);
			close(conn);	
		}	
	}

	@Override
	public void setFlowMap(FlowMap map) throws ConfigError {
		SortedSet<FlowEntry> rules = map.getRules();
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement slice = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(DFLOWMAP);
			ps.execute();
			
			/*
			 *  Not actually needed but whatever...
			 *  
			 *  DB does this for free.
			 */
			//ps = conn.prepareStatement(DACTIONS);
			//ps.execute();
			ps = conn.prepareStatement(RESETFLOWTABLE);
			ps.execute();
			
			
			int sliceid = -1;
			int ruleid = -1;
			int wildcards = -1;
			for (FlowEntry fe : rules) {
				wildcards = fe.getRuleMatch().getWildcards();
				ps = conn.prepareStatement(SFLOWMAP, Statement.RETURN_GENERATED_KEYS);
				ps.setLong(1, fe.getDpid());
				ps.setInt(2, fe.getPriority());
				ps.setShort(3, fe.getRuleMatch().getInputPort());
				if ((wildcards & FVMatch.OFPFW_DL_VLAN) != 0)
					ps.setNull(4, Types.SMALLINT);
				else
					ps.setShort(4, fe.getRuleMatch().getDataLayerVirtualLan());
				
				if ((wildcards & FVMatch.OFPFW_DL_VLAN_PCP) != 0)
					ps.setNull(5, Types.SMALLINT);
				else
					ps.setShort(5, fe.getRuleMatch().getDataLayerVirtualLanPriorityCodePoint());
				
				if ((wildcards & FVMatch.OFPFW_DL_SRC) != 0)
					ps.setNull(6, Types.BIGINT);
				else
					ps.setLong(6, FlowSpaceUtil.toLong(fe.getRuleMatch().getDataLayerSource()));
			
				if ((wildcards & FVMatch.OFPFW_DL_DST) != 0)
					ps.setNull(7, Types.BIGINT);
				else
					ps.setLong(7, FlowSpaceUtil.toLong(fe.getRuleMatch().getDataLayerDestination()));
				
				if ((wildcards & FVMatch.OFPFW_DL_TYPE) != 0)
					ps.setNull(8, Types.SMALLINT);
				else
					ps.setShort(8, fe.getRuleMatch().getDataLayerType());
				
				if ((wildcards & FVMatch.OFPFW_NW_SRC_ALL) != 0)
					ps.setNull(9, Types.INTEGER);
				else
					ps.setInt(9, fe.getRuleMatch().getNetworkSource());
				
				if ((wildcards & FVMatch.OFPFW_NW_DST_ALL) != 0)
					ps.setNull(10, Types.INTEGER);
				else
					ps.setInt(10, fe.getRuleMatch().getNetworkDestination());
				
				if ((wildcards & FVMatch.OFPFW_NW_PROTO) != 0)
					ps.setNull(11, Types.SMALLINT);
				else
					ps.setShort(11, fe.getRuleMatch().getNetworkProtocol());
				
				if ((wildcards & FVMatch.OFPFW_NW_TOS) != 0)
					ps.setNull(12, Types.SMALLINT);
				else
					ps.setShort(12, fe.getRuleMatch().getNetworkTypeOfService());
				
				if ((wildcards & FVMatch.OFPFW_TP_SRC) != 0)
					ps.setNull(13, Types.SMALLINT);
				else
					ps.setShort(13, fe.getRuleMatch().getTransportSource());
				
				if ((wildcards & FVMatch.OFPFW_TP_DST) != 0)
					ps.setNull(14, Types.SMALLINT);
				else
					ps.setShort(14, fe.getRuleMatch().getTransportDestination());
				
				ps.setInt(15, wildcards);
				ps.setInt(16, fe.getId());
				ps.executeUpdate();
				set = ps.getGeneratedKeys();
				set.next();
				ruleid = set.getInt(1);
				for (OFAction act : fe.getActionsList()) {
					slice = conn.prepareStatement(GSLICEID);
					slice.setString(1, ((SliceAction) act).getSliceName());
					set = slice.executeQuery();
					if (set.next())
						sliceid = set.getInt("id");
					else {
						FVLog.log(LogLevel.WARN, null, "Slice name " + ((SliceAction) act).getSliceName() + " does not exist... skipping.");
						continue;
					}
					ps = conn.prepareStatement(SACTIONS);
					ps.setInt(1, ruleid);
					ps.setInt(2, sliceid);
					ps.setInt(3, ((SliceAction) act).getSlicePerms());
					ps.executeUpdate();
				}
			}
			notify(ChangedListener.FLOWMAP, FFLOWMAP, map);
		} catch (SQLException e) {
			FVLog.log(LogLevel.DEBUG, null, e.getMessage());
			throw new ConfigError("Unable to set the flowmap in db");
		} finally {
			close(set);
			close(ps);
			close(slice);
			close(conn);	
		}	

	}
	
	@Override
	public void notifyChange(FlowMap map) {
		FVLog.log(LogLevel.DEBUG, null, "Notifying flowspace change");
		notify(ChangedListener.FLOWMAP, FFLOWMAP, map);
	}

	
	@Override
	public int addRule(FlowEntry fe) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement slice = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			
			int sliceid = -1;
			int ruleid = -1;
			int wildcards = -1;
			
			wildcards = fe.getRuleMatch().getWildcards();
			ps = conn.prepareStatement(SFLOWMAP, Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, fe.getDpid());
			ps.setInt(2, fe.getPriority());
			ps.setShort(3, fe.getRuleMatch().getInputPort());
			if ((wildcards & FVMatch.OFPFW_DL_VLAN) != 0)
				ps.setNull(4, Types.SMALLINT);
			else
				ps.setShort(4, fe.getRuleMatch().getDataLayerVirtualLan());
			
			if ((wildcards & FVMatch.OFPFW_DL_VLAN_PCP) != 0)
				ps.setNull(5, Types.SMALLINT);
			else
				ps.setShort(5, fe.getRuleMatch().getDataLayerVirtualLanPriorityCodePoint());
			
			if ((wildcards & FVMatch.OFPFW_DL_SRC) != 0)
				ps.setNull(6, Types.BIGINT);
			else
				ps.setLong(6, FlowSpaceUtil.toLong(fe.getRuleMatch().getDataLayerSource()));
			
			if ((wildcards & FVMatch.OFPFW_DL_DST) != 0)
				ps.setNull(7, Types.BIGINT);
			else
				ps.setLong(7, FlowSpaceUtil.toLong(fe.getRuleMatch().getDataLayerDestination()));
			
			if ((wildcards & FVMatch.OFPFW_DL_TYPE) != 0)
				ps.setNull(8, Types.SMALLINT);
			else
				ps.setShort(8, fe.getRuleMatch().getDataLayerType());
			
			if ((wildcards & FVMatch.OFPFW_NW_SRC_ALL) != 0)
				ps.setNull(9, Types.INTEGER);
			else
				ps.setInt(9, fe.getRuleMatch().getNetworkSource());
			
			if ((wildcards & FVMatch.OFPFW_NW_DST_ALL) != 0)
				ps.setNull(10, Types.INTEGER);
			else
				ps.setInt(10, fe.getRuleMatch().getNetworkDestination());
			
			if ((wildcards & FVMatch.OFPFW_NW_PROTO) != 0)
				ps.setNull(11, Types.SMALLINT);
			else
				ps.setShort(11, fe.getRuleMatch().getNetworkProtocol());
			
			if ((wildcards & FVMatch.OFPFW_NW_TOS) != 0)
				ps.setNull(12, Types.SMALLINT);
			else
				ps.setShort(12, fe.getRuleMatch().getNetworkTypeOfService());
			
			if ((wildcards & FVMatch.OFPFW_TP_SRC) != 0)
				ps.setNull(13, Types.SMALLINT);
			else
				ps.setShort(13, fe.getRuleMatch().getTransportSource());
			
			if ((wildcards & FVMatch.OFPFW_TP_DST) != 0)
				ps.setNull(14, Types.SMALLINT);
			else
				ps.setShort(14, fe.getRuleMatch().getTransportDestination());
			
			ps.setInt(15, wildcards);
			ps.executeUpdate();
			set = ps.getGeneratedKeys();
			set.next();
			ruleid = set.getInt(1);
			for (OFAction act : fe.getActionsList()) {
				slice = conn.prepareStatement(GSLICEID);
				slice.setString(1, ((SliceAction) act).getSliceName());
				set = slice.executeQuery();
				if (set.next())
					sliceid = set.getInt("id");
				else {
					FVLog.log(LogLevel.WARN, null, "Slice name " + ((SliceAction) act).getSliceName() + " does not exist... skipping.");
					continue;
				}
				ps = conn.prepareStatement(SACTIONS);
				ps.setInt(1, ruleid);
				ps.setInt(2, sliceid);
				ps.setInt(3, ((SliceAction) act).getSlicePerms());
				ps.executeUpdate();
			}
			return ruleid;
		} catch (SQLException e) {
			FVLog.log(LogLevel.DEBUG, null, e.getMessage());
			throw new ConfigError("Unable to set the flowmap in db");
		} finally {
			close(set);
			close(ps);
			close(slice);
			close(conn);	
		}	
	}
	
	
	@Override
	public void removeRule(int id) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
	
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(DFLOWRULE);
			ps.setInt(1, id);
			int affected = -1;
			if ((affected = ps.executeUpdate()) != 1) {
				FVLog.log(LogLevel.ALERT, null, "Failed to delete rule with id ", id, " : rows affected ", affected);
				throw new ConfigError("Unable to remove rule with id " + id);
			}
		} catch (SQLException e) {
			FVLog.log(LogLevel.DEBUG, null, e.getMessage());
			throw new ConfigError("Unable to remove rule with id " + id);
		} finally {
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
	
	public static FlowSpace getProxy() {
		return (FlowSpace) FVConfigurationController.instance()
		.getProxy(getInstance());
	}
	
	public static void addListener(FlowMapChangedListener l) {
		FVConfigurationController.instance().addChangeListener(ChangedListener.FLOWMAP, l);
	}
	
	public static void removeListener(FlowvisorChangedListener l) {
		FVConfigurationController.instance().removeChangeListener(ChangedListener.FLOWMAP, l);
	}

	@Override
	public void toJson(JsonWriter writer) throws IOException {
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement actions = null;
		ResultSet set = null;
		ResultSet actionSet = null;
		try {
			int wildcards = -1;
			conn = settings.getConnection();
			ps = conn.prepareStatement(GFLOWMAP);
			set = ps.executeQuery();
			writer.name(FS);
			writer.beginArray();
			while (set.next()) {
				writer.beginObject();
				wildcards = set.getInt(WILDCARDS);
				
				setJsonParam(writer, DPID, Long.toString(set.getLong(DPID), 16), set.wasNull());
				setJsonParam(writer, PRIO, set.getInt(PRIO), set.wasNull());
				setJsonParam(writer,INPORT, set.getShort(INPORT), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_DL_VLAN) == 0)
					setJsonParam(writer, VLAN, set.getShort(VLAN), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_DL_VLAN_PCP) == 0)
					setJsonParam(writer, VPCP, set.getShort(VPCP), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_DL_SRC) == 0)
					setJsonParam(writer, DLSRC, HexString.toHexString(FlowSpaceUtil.toByteArray(set.getLong(DLSRC))), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_DL_DST) == 0)
					setJsonParam(writer, DLDST, HexString.toHexString(FlowSpaceUtil.toByteArray(set.getLong(DLDST))), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_DL_TYPE) == 0)
					setJsonParam(writer, DLTYPE, set.getShort(DLTYPE), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_NW_SRC_ALL) == 0)
					setJsonParam(writer, NWSRC, set.getInt(NWSRC), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_NW_DST_ALL) == 0)
					setJsonParam(writer, NWDST, set.getInt(NWDST), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_NW_PROTO) == 0)
					setJsonParam(writer, NWPROTO, set.getShort(NWPROTO), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_NW_TOS) == 0)
					setJsonParam(writer, NWTOS, set.getShort(NWTOS), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_TP_SRC) == 0)
					setJsonParam(writer, TPSRC, set.getShort(TPSRC), set.wasNull());
				
				if ((wildcards & FVMatch.OFPFW_TP_DST) == 0)
					setJsonParam(writer, TPDST, set.getShort(TPDST), set.wasNull());
				
				
				setJsonParam(writer, WILDCARDS, wildcards, set.wasNull());
				actions = conn.prepareStatement(GACTIONS);
				actions.setInt(1, set.getInt("id"));
				actionSet = actions.executeQuery();
				writer.name(ACTION);
				writer.beginArray();
				while (actionSet.next()) {
					writer.beginObject();
					writer.name(actionSet.getString(Slice.SLICE)).value(actionSet.getInt(ACTION));
					writer.endObject();
				}
				writer.endArray();
				writer.endObject();
			}
			writer.endArray();
		} catch (SQLException e) {
			FVLog.log(LogLevel.CRIT, null, "Failed to write flowspace config : " + e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(actionSet);
			close(actions);
			close(conn);	
		}	
		
	}
	
	@Override
	public void fromJson(JsonReader reader) throws IOException {
		HashMap<String, Object> row = new HashMap<String , Object>();
		HashMap<String, Integer> actions = new HashMap<String, Integer>();
		String key = null;
		Object value = null;
		reset();
		while (true) {
			switch (reader.peek()) {
				case BEGIN_ARRAY:
					reader.beginArray();
					readActions(reader, actions);
					row.put(key, actions.clone());
					key = null;
					actions.clear();
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
	
	private <T extends Number> void setJsonParam(JsonWriter writer, String param, T value, Boolean wasNull) throws IOException {
		if (wasNull)
			writer.name(param).nullValue();
		else
			writer.name(param).value(value);
	}
	
	private void setJsonParam(JsonWriter writer, String param, String value, Boolean wasNull) throws IOException {
		if (wasNull)
			writer.name(param).nullValue();
		else
			writer.name(param).value(value);
	}
	

	/*
	 * Super ugly list of ifs... but i can't think of a way to so this 
	 * cleanly right now.
	 */
	
	@SuppressWarnings("unchecked")
	private void insert(HashMap<String, Object> row) throws IOException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		int sliceid = -1;
		int ruleid = -1;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SFLOWMAP, Statement.RETURN_GENERATED_KEYS);
			if (row.get(DPID) == null) 
				ps.setNull(1, Types.BIGINT);
			else
				ps.setLong(1, HexString.toLong((String)row.get(DPID)));
			if (row.get(PRIO) == null) 
				ps.setNull(2, Types.INTEGER);
			else
				ps.setInt(2, ((Long) row.get(PRIO)).intValue());
			if (row.get(INPORT) == null) 
				ps.setNull(3, Types.SMALLINT);
			else
				ps.setShort(3, ((Long) row.get(INPORT)).shortValue());
			if (row.get(VLAN) == null) 
				ps.setNull(4, Types.SMALLINT);
			else
				ps.setShort(4, ((Long) row.get(VLAN)).shortValue());
			if (row.get(VPCP) == null) 
				ps.setNull(5, Types.SMALLINT);
			else
				ps.setShort(5, ((Long) row.get(VPCP)).shortValue());
			if (row.get(DLSRC) == null) 
				ps.setNull(6, Types.BIGINT);
			else
				ps.setLong(6, HexString.toLong((String)row.get(DLSRC)));
			if (row.get(DLDST) == null) 
				ps.setNull(7, Types.BIGINT);
			else
				ps.setLong(7, HexString.toLong((String)row.get(DLDST)));
			if (row.get(DLTYPE) == null) 
				ps.setNull(8, Types.SMALLINT);
			else
				ps.setShort(8, ((Long) row.get(DLTYPE)).shortValue());
			if (row.get(NWSRC) == null) 
				ps.setNull(9, Types.INTEGER);
			else
				ps.setInt(9, ((Long) row.get(NWSRC)).intValue());
			if (row.get(NWDST) == null) 
				ps.setNull(10, Types.INTEGER);
			else
				ps.setInt(10, ((Long) row.get(NWDST)).intValue());
			if (row.get(NWPROTO) == null) 
				ps.setNull(11, Types.SMALLINT);
			else
				ps.setShort(11, ((Long) row.get(NWPROTO)).shortValue());
			if (row.get(NWTOS) == null) 
				ps.setNull(12, Types.SMALLINT);
			else
				ps.setShort(12, ((Long) row.get(NWTOS)).shortValue());
			if (row.get(TPSRC) == null) 
				ps.setNull(13, Types.SMALLINT);
			else
				ps.setShort(13, ((Long) row.get(TPSRC)).shortValue());
			if (row.get(TPDST) == null) 
				ps.setNull(14, Types.SMALLINT);
			else
				ps.setShort(14, ((Long) row.get(TPDST)).shortValue());
			if (row.get(WILDCARDS) == null) 
				ps.setNull(15, Types.INTEGER);
			else
				ps.setInt(15, ((Long) row.get(WILDCARDS)).intValue());
			
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Flow rule insertion failed... siliently.");
			set = ps.getGeneratedKeys();
			set.next();
			ruleid = set.getInt(1);
			
			for (Entry<String, Integer> entry : ((HashMap<String, Integer>)  row.get(ACTION)).entrySet()) {
				ps = conn.prepareStatement(SLICEID);
				ps.setString(1, entry.getKey());
				set = ps.executeQuery();
				if (set.next()) 
					sliceid = set.getInt("id");
				else {
					sliceid = -1;
					System.err.println("Inserting rule with action on unknown slice " + entry.getKey() + 
							"; hope you know what you are doing...");
				}
				ps = conn.prepareStatement(SACTIONS);
				ps.setInt(1, ruleid);
				ps.setInt(2, sliceid);
				ps.setInt(3, entry.getValue());
				if (ps.executeUpdate() == 0)
					FVLog.log(LogLevel.WARN, null, "Action insertion failed... siliently.");
			}
			} catch (SQLException e) {
				e.printStackTrace();
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
		
	}
	
	private void readActions(JsonReader reader, HashMap<String, Integer> actions) throws IOException  {
		String slice = null;
		Integer perm = null;
		while (true) {
			switch (reader.peek()) {
			
			case BEGIN_OBJECT:
				reader.beginObject();
				break;
			case END_OBJECT:
				reader.endObject();
				if (slice != null && perm != null)
					actions.put(slice,perm);
				slice = null;
				perm = null;
				break;
			case END_ARRAY:
				reader.endArray();
				return;
			case NAME:
				slice = reader.nextName();
				break;
			
			case NUMBER:
				perm = reader.nextInt();
				break;
			default:
				reader.skipValue();
			}
		}
	}
	
	private void reset() {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(DFLOWMAP);
			ps.execute();
			ps = conn.prepareStatement(RESETFLOWTABLE);
			ps.execute();
		} catch (SQLException e) {
			System.err.println("Reseting index on flowtable failed : " + e.getMessage());
		} finally {
			close(ps);
			close(conn);
		}
	}
}
