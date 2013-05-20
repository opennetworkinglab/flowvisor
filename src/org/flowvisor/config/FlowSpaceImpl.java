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
import java.util.List;
import java.util.UUID;
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

public class FlowSpaceImpl implements FlowSpace {

	private ConfDBSettings settings = null;
	private static FlowSpaceImpl instance =  null;
	//private FlowMap cachedFlowMap = null;
	
	
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
			"=J.flowspacerule_id AND J.slice_id=S.id AND S." + Slice.ADMINDOWN + "=true";
	
	private static String GALLFLOWMAP = "SELECT FSR.*,S." + Slice.FMTYPE + 
			" FROM FlowSpaceRule AS FSR, Slice AS S, JFSRSlice AS J WHERE FSR.id" +
			"=J.flowspacerule_id AND J.slice_id=S.id";
	
	private static String GSLICEFLOWMAP = "SELECT FSR.*,S." + Slice.FMTYPE + 
			" FROM FlowSpaceRule AS FSR, Slice AS S, JFSRSlice AS J WHERE FSR.id" +
			"=J.flowspacerule_id AND J.slice_id=S.id AND S."+Slice.SLICE+"=?";
	
	private static String GSLICEID = "SELECT id FROM Slice WHERE " + Slice.SLICE + "= ?";
	private static String DFLOWMAP = "DELETE FROM FlowSpaceRule";
	private static String DFLOWRULE = "DELETE FROM FlowSpaceRule WHERE id = ?";
	private static String DFLOWRULEBYNAME = "DELETE FROM FlowSpaceRule WHERE ";
	
	private static String GACTIONS = "SELECT " + ACTION + ",S." + Slice.SLICE + " FROM jFSRSlice as J," +
			"Slice as S  WHERE flowspacerule_id=? and slice_id = S.id";
	private static String GQUEUES = "SELECT " + QUEUE + " FROM FSRQueue AS FQ, FlowSpaceRule AS FSR where " +
			"FSR.id = FQ.fsr_id AND FSR.id = ?";
	
	private static String SQUEUES = "INSERT INTO FSRQueue(fsr_id," + QUEUE + ") VALUES(?,?)";
	
	
	private static String SFLOWMAP = "INSERT INTO FlowSpaceRule(" + DPID + "," + PRIO + "," +  
			INPORT + "," + VLAN + "," + VPCP + "," + DLSRC + "," + DLDST + "," + DLTYPE + "," +
			NWSRC + "," + NWDST + "," + NWPROTO + "," + NWTOS + "," + TPSRC + "," + TPDST + "," +
			FORCED_QUEUE + "," + WILDCARDS+ "," + NAME +") " + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	

	
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
		/*if (cachedFlowMap != null)
			return cachedFlowMap;*/
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement actions = null;
		PreparedStatement queues = null;
		ResultSet set = null;
		ResultSet actionSet = null;
		ResultSet queueSet = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GFLOWMAP);
			set = ps.executeQuery();
			FlowMap map = null;
			LinkedList<OFAction> actionsList = null;
			LinkedList<Integer> queueList = null;
			FlowEntry fe = null;
			SliceAction act = null;
			int wildcards = -1;
			int fsr_id = -1;
			while (set.next()) {
				if (map == null)
					map = FlowSpaceUtil.getNewFlowMap(set.getInt(Slice.FMTYPE));
				FVMatch match = new FVMatch();
				actionsList = new LinkedList<OFAction>();
				queueList = new LinkedList<Integer>();
				wildcards = set.getInt(WILDCARDS);
				fsr_id = set.getInt("id");
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
				actions.setInt(1, fsr_id);
				actionSet = actions.executeQuery();
				while (actionSet.next()) {
					act = new SliceAction(actionSet.getString(Slice.SLICE), actionSet.getInt(ACTION));
					actionsList.add(act);
				}
				queues = conn.prepareStatement(GQUEUES);
				queues.setInt(1, fsr_id);
				queueSet = queues.executeQuery();
				while (queueSet.next()) {
					queueList.add(queueSet.getInt(QUEUE));
				}
				fe = new FlowEntry(set.getString(NAME), set.getLong(DPID), match, set.getInt("id"),set.getInt(PRIO) , actionsList);
				fe.setQueueId(queueList);
				fe.setForcedQueue(set.getInt(FORCED_QUEUE));
				map.addRule(fe);
			}
			if (map == null)
				map = new FederatedFlowMap();
			//cachedFlowMap = map;
			return map;
		} catch (SQLException e) {
			throw new ConfigError("Unable to retrieve flowmap from db : " + e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(actionSet);
			close(actions);
			close(queueSet);
			close(queues);
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
				
				ps.setInt(15, (int) fe.getForcedQueue());
				ps.setInt(16, wildcards);
				ps.setInt(17, fe.getId());
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
				if (fe.getQueueId() == null)
					return;
				ps = conn.prepareStatement(SQUEUES);
				ps.setInt(1, ruleid);
				for (Integer queue_id : (LinkedList<Integer>) fe.getQueueId()) {
					ps.setInt(2, queue_id);
					if (ps.executeUpdate() == 0)
						FVLog.log(LogLevel.WARN, null, "Queue insertion failed... siliently.");
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
		PreparedStatement queues = null;
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
			
			ps.setInt(15, (int) fe.getForcedQueue());
			ps.setInt(16, wildcards);
			ps.setString(17, fe.getName());
			ps.executeUpdate();
			set = ps.getGeneratedKeys();
			set.next();
			ruleid = set.getInt(1);
			slice = conn.prepareStatement(GSLICEID);
			ps = conn.prepareStatement(SACTIONS);
			for (OFAction act : fe.getActionsList()) {
				slice.setString(1, ((SliceAction) act).getSliceName());
				set = slice.executeQuery();
				if (set.next())
					sliceid = set.getInt("id");
				else {
					FVLog.log(LogLevel.WARN, null, "Slice name " + ((SliceAction) act).getSliceName() + " does not exist... skipping.");
					continue;
				}
				
				ps.setInt(1, ruleid);
				ps.setInt(2, sliceid);
				ps.setInt(3, ((SliceAction) act).getSlicePerms());
				//ps.setInt(4, fe.getQueueId());
				ps.executeUpdate();
			}
			
			queues = conn.prepareStatement(SQUEUES);
			queues.setInt(1, ruleid);
			for (Integer queue_id : fe.getQueueId()) {
				queues.setInt(2, queue_id);
				queues.executeUpdate();
			}
			fe.setId(ruleid);
			return ruleid;
		} catch (SQLException e) {
			FVLog.log(LogLevel.DEBUG, null, e.getMessage());
			
			throw new ConfigError("Unable to set the flowmap in db");
		} finally {
			close(set);
			close(ps);
			close(slice);
			close(queues);
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
	public void removeRuleByName(List<String> names) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
	
		String stmt = DFLOWRULEBYNAME;
		for (int i = 0 ; i < names.size() ; i++) {
			if (i == 0)
				stmt += " name = ? ";
			else
				stmt += " OR name = ? ";	
		}
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(stmt);
			for (int i = 0 ; i < names.size() ; i++)
				ps.setString(i+1, names.get(i));
			int affected = -1;
			if ((affected = ps.executeUpdate()) < 1) {
				FVLog.log(LogLevel.ALERT, null, "Failed to delete rules by name  : rows affected ", affected);
				throw new ConfigError("Unable to remove rule by names");
			}
		} catch (SQLException e) {
			FVLog.log(LogLevel.DEBUG, null, e.getMessage());
			throw new ConfigError("Unable to remove rule by names" + e.getMessage());
		} finally {
			close(ps);
			close(conn);
		}
 		
		
	}
	
	@Override
	public void saveFlowSpace(String sliceName) throws ConfigError {
		FVLog.log(LogLevel.WARN, null, "FlowSpace preservation not yet implemented");
		/*FlowMap fm = getFlowMap();
		for (FlowEntry fe : fm.getRules()) {
			for (OFAction act : fe.getActionsList()) {
				assert(act instanceof SliceAction);
				SliceAction sact = (SliceAction) act;
				if (sact.getSliceName().equals(sliceName))
					preserveFlowSpace(fe, sliceName, sact.getSlicePerms());
			}
		}*/
		
	}
	
	
	
		
	/*private void preserveFlowSpace(FlowEntry fe, String sliceName, int perm) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			
			int wildcards = -1;
			
			wildcards = fe.getRuleMatch().getWildcards();
			ps = conn.prepareStatement(SAVESLICEFLOWMAP);
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
			
			ps.setInt(15, (int) fe.getForcedQueue());
			ps.setInt(16, wildcards);
			ps.setString(17, sliceName);
			ps.setInt(18, perm);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			FVLog.log(LogLevel.DEBUG, null, e.getMessage());
			throw new ConfigError("Unable to preserve the flowspace in db");
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
		
	}*/

	@Override
	public void close(Connection conn) {
		//settings.returnConnection(conn);
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
 	public HashMap<String, Object> toJson(HashMap<String, Object> output) {
  		try {
			return toJson(output, null, true);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, null, "Failed to convert config to JSON: "
							+ e.getMessage());
		}
  		return null;
	}
	
	@Override
	public void fromJson(ArrayList<HashMap<String, Object>> list) throws IOException {
		reset();
		for (HashMap<String, Object> row : list)
			insert(row);
	}
	
	
	@Override
	public HashMap<String, Object> toJson(HashMap<String, Object> map, 
						String sliceName, Boolean show)
			throws ConfigError {
  		Connection conn = null;
  		PreparedStatement ps = null;
  		PreparedStatement actions = null;
  		PreparedStatement queues = null;
  		ResultSet set = null;
  		ResultSet actionSet = null;
  		ResultSet queueSet = null;
 		HashMap<String, Object> fs = new HashMap<String, Object>();
 		HashMap<String, Object> action = new HashMap<String, Object>();
 		LinkedList<Object> list = new LinkedList<Object>();
 		LinkedList<Object> actionList = new LinkedList<Object>();
 		LinkedList<Integer> queueList = new LinkedList<Integer>();
  		try {
  			int wildcards = -1;
  			conn = settings.getConnection();
  			if (sliceName != null) {
  				ps = conn.prepareStatement(GSLICEFLOWMAP);
  				ps.setString(1, sliceName);
  			} else {
  				if (show)
  					ps = conn.prepareStatement(GALLFLOWMAP);
  				else
  					ps = conn.prepareStatement(GFLOWMAP);
  			}
  			set = ps.executeQuery();
 			//writer.name(FS);
 			//writer.beginArray();
  			while (set.next()) {
 				//writer.beginObject();
  				wildcards = set.getInt(WILDCARDS);
 				fs.put(DPID, FlowSpaceUtil.dpidToString(set.getLong(DPID)));
 				fs.put(PRIO, set.getInt(PRIO));
 				fs.put("id", set.getInt("id"));
 				if ((wildcards & FVMatch.OFPFW_IN_PORT) == 0)
 					fs.put(INPORT, set.getInt(INPORT));
  				
  				if ((wildcards & FVMatch.OFPFW_DL_VLAN) == 0)
 					fs.put(VLAN, set.getShort(VLAN));
  				
  				if ((wildcards & FVMatch.OFPFW_DL_VLAN_PCP) == 0)
 					fs.put(VPCP, set.getShort(VPCP));
  				
  				if ((wildcards & FVMatch.OFPFW_DL_SRC) == 0)
 					fs.put(DLSRC, FlowSpaceUtil.macToString(set.getLong(DLSRC)));
  				
  				if ((wildcards & FVMatch.OFPFW_DL_DST) == 0)
 					fs.put(DLDST, FlowSpaceUtil.macToString(set.getLong(DLDST)));
  				
  				if ((wildcards & FVMatch.OFPFW_DL_TYPE) == 0)
 					fs.put(DLTYPE, set.getShort(DLTYPE));
  				
  				if ((wildcards & FVMatch.OFPFW_NW_SRC_ALL) == 0)
 					fs.put(NWSRC, set.getInt(NWSRC));
  				
  				if ((wildcards & FVMatch.OFPFW_NW_DST_ALL) == 0)
 					fs.put(NWDST, set.getInt(NWDST));
  				
  				if ((wildcards & FVMatch.OFPFW_NW_PROTO) == 0)
 					fs.put(NWPROTO, set.getShort(NWPROTO));
  				
  				if ((wildcards & FVMatch.OFPFW_NW_TOS) == 0)
 					fs.put(NWTOS, set.getShort(NWTOS));
 				
 				if ((wildcards & FVMatch.OFPFW_TP_DST) == 0)
 					fs.put(TPDST, set.getShort(TPDST));
  				
  				if ((wildcards & FVMatch.OFPFW_TP_SRC) == 0)
 					fs.put(TPSRC, set.getShort(TPSRC));
  				
  				fs.put(FORCED_QUEUE, set.getInt(FORCED_QUEUE));
 				fs.put(WILDCARDS, wildcards);
 				fs.put(NAME, set.getString(NAME));
  				//fs.put(QUEUE, set.getInt(QUEUE));
  				actions = conn.prepareStatement(GACTIONS);
  				actions.setInt(1, set.getInt("id"));
  				actionSet = actions.executeQuery();
 				//writer.name(ACTION);
 				//writer.beginArray();
  				while (actionSet.next()) {
 					action.put(actionSet.getString(Slice.SLICE), actionSet.getInt(ACTION));
 					actionList.add(action.clone());
 					action.clear();
  				}
  				fs.put(ACTION, actionList.clone());
  				actionList.clear();
  				
  				queues = conn.prepareStatement(GQUEUES);
  				queues.setInt(1, set.getInt("id"));
  				queueSet = queues.executeQuery();
 				while (queueSet.next()) {
 						queueList.add(queueSet.getInt(QUEUE));
 				}
 				
 				fs.put(QUEUE, queueList.clone());
 				queueList.clear();
 				list.add(fs.clone());
 				fs.clear();
  			}
 			map.put(FS, list);
  		} catch (SQLException e) {
  			FVLog.log(LogLevel.CRIT, null, "Failed to write flowspace config : " + e.getMessage());
  		} finally {

			close(set);
			close(ps);
			close(actionSet);
			close(actions);
			close(queueSet);
			close(queues);
			close(conn);	
		}	
		return map;
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
				ps.setLong(1, FlowSpaceUtil.parseDPID(((String) row.get(DPID))));
			if (row.get(PRIO) == null) 
				ps.setNull(2, Types.INTEGER);
			else
				ps.setInt(2, ((Double) row.get(PRIO)).intValue());
			if (row.get(INPORT) == null) 
				ps.setNull(3, Types.SMALLINT);
			else
				ps.setShort(3, ((Double) row.get(INPORT)).shortValue());
			if (row.get(VLAN) == null) 
				ps.setNull(4, Types.SMALLINT);
			else
				ps.setShort(4, ((Double) row.get(VLAN)).shortValue());
			if (row.get(VPCP) == null) 
				ps.setNull(5, Types.SMALLINT);
			else
				ps.setShort(5, ((Double) row.get(VPCP)).shortValue());
			if (row.get(DLSRC) == null) 
				ps.setNull(6, Types.BIGINT);
			else
				ps.setLong(6, FlowSpaceUtil.parseMac(((String)row.get(DLSRC))));
			if (row.get(DLDST) == null) 
				ps.setNull(7, Types.BIGINT);
			else
				ps.setLong(7, FlowSpaceUtil.parseMac(((String)row.get(DLDST))));
			if (row.get(DLTYPE) == null) 
				ps.setNull(8, Types.SMALLINT);
			else
				ps.setShort(8, ((Double) row.get(DLTYPE)).shortValue());
			if (row.get(NWSRC) == null) 
				ps.setNull(9, Types.INTEGER);
			else
				ps.setInt(9, ((Double) row.get(NWSRC)).intValue());
			if (row.get(NWDST) == null) 
				ps.setNull(10, Types.INTEGER);
			else
				ps.setInt(10, ((Long) row.get(NWDST)).intValue());
			if (row.get(NWPROTO) == null) 
				ps.setNull(11, Types.SMALLINT);
			else
				ps.setShort(11, ((Double) row.get(NWPROTO)).shortValue());
			if (row.get(NWTOS) == null) 
				ps.setNull(12, Types.SMALLINT);
			else
				ps.setShort(12, ((Double) row.get(NWTOS)).shortValue());
			if (row.get(TPSRC) == null) 
				ps.setNull(13, Types.SMALLINT);
			else
				ps.setShort(13, ((Double) row.get(TPSRC)).shortValue());
			if (row.get(TPDST) == null) 
				ps.setNull(14, Types.SMALLINT);
			else
				ps.setShort(14, ((Double) row.get(TPDST)).shortValue());
			if (row.get(FORCED_QUEUE) == null) 
				ps.setInt(15, -1);
			else
				ps.setInt(15, ((Double) row.get(FORCED_QUEUE)).intValue());
			if (row.get(WILDCARDS) == null) 
				ps.setNull(16, Types.INTEGER);
			else
				ps.setInt(16, ((Double) row.get(WILDCARDS)).intValue());
			
			if (row.get(NAME) == null) 
				ps.setString(17, UUID.randomUUID().toString());
			else
				ps.setString(17, (String) row.get(NAME));
			
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Flow rule insertion failed... siliently.");
			set = ps.getGeneratedKeys();
			set.next();
			ruleid = set.getInt(1);
			
			for (HashMap<String, Double> item : ((ArrayList<HashMap<String, Double>>)  row.get(ACTION))) {
				for (Entry<String, Double> entry : item.entrySet()) {
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
					ps.setInt(3, ((Double) entry.getValue()).intValue());
					
					if (ps.executeUpdate() == 0)
						FVLog.log(LogLevel.WARN, null, "Action insertion failed... siliently.");
				}
			}
			if (row.get(QUEUE) == null)
				return;
			ps = conn.prepareStatement(SQUEUES);
			ps.setInt(1, ruleid);
			for (Double queue_id : (ArrayList<Double>) row.get(QUEUE)) {
				ps.setInt(2, queue_id.intValue());
				if (ps.executeUpdate() == 0)
					FVLog.log(LogLevel.WARN, null, "Queue insertion failed... siliently.");
			}
		} catch (SQLException e) {
				e.printStackTrace();
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
		FVLog.log(LogLevel.INFO, null, "Updating FlowSpace database table.");
		if (version == 0) {
			processAlter("ALTER TABLE FlowSpaceRule ADD COLUMN " + FORCED_QUEUE + " INT DEFAULT -1");
			processAlter("CREATE TABLE FSRQueue( " +
					"id  INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
					"fsr_id INT NOT NULL, " +
					"queue_id INT DEFAULT -1, " +
					"PRIMARY KEY (id))");
			processAlter("CREATE INDEX fsrqueue_index on FSRQueue (fsr_id ASC)");
			processAlter("ALTER TABLE FSRQueue " +
					"ADD CONSTRAINT FlowSpaceRule_to_queue_fk FOREIGN KEY (fsr_id) " +
					"REFERENCES FlowSpaceRule (id) ON DELETE CASCADE");
			version++;
		}
		if (version == 1) {
			processAlter("ALTER TABLE FlowSpaceRule ADD COLUMN " + NAME + " VARCHAR(64)");
			version++;
		}
		
		
	}

}
