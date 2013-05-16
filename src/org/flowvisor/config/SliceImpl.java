package org.flowvisor.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.flowvisor.api.APIAuth;
import org.flowvisor.exceptions.DuplicateControllerException;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

public class SliceImpl implements Slice {
	
	
	private static SliceImpl instance = null;
	
	//Callbacks
	private static String FLLDP = "setLLDP";
	private static String FDROP = "setDropPolicy";
	private static String FHOST = "setControllerHost";
	private static String FPORT = "setControllerPort";
	private static String FFMLIMIT = "setFlowModLimit";
	
	// STATEMENTS
	private static String GALL = "SELECT S.*,F." + Flowvisor.CONFIG + " FROM Slice AS S, Flowvisor AS F WHERE S.flowvisor_id = F.id";
	private static String GLLDPSQL = "SELECT " + LLDP + " FROM Slice WHERE " + SLICE + " = ?";
	private static String SLLDPSQL = "UPDATE Slice SET " + LLDP + "= ? WHERE " + SLICE + " = ?";
	private static String GDROPSQL = "SELECT " + DROP + " FROM Slice WHERE " + SLICE + " = ?";
	private static String SDROPSQL = "UPDATE Slice SET " + DROP + "= ? WHERE " + SLICE + " = ?";
	private static String GHOSTSQL = "SELECT " + HOST + " FROM Slice WHERE " + SLICE + " = ?";
	private static String SHOSTSQL = "UPDATE Slice SET " + HOST + "= ? WHERE " + SLICE + " = ?";
	private static String GPORTSQL = "SELECT " + PORT + " FROM Slice WHERE " + SLICE + " = ?";
	private static String SPORTSQL = "UPDATE Slice SET " + PORT + "= ? WHERE " + SLICE + " = ?";
	private static String GPASSELM = "SELECT <REPLACEME> FROM Slice WHERE " + SLICE + " = ?";
	private static String GCREATOR = "SELECT " + CREATOR + " FROM Slice WHERE " + SLICE + " = ?";
	private static String SEMAIL = "UPDATE Slice SET " + EMAIL + " = ? WHERE " + SLICE + " = ?";
	private static String GEMAIL = "SELECT " + EMAIL + " FROM Slice WHERE " + SLICE + " = ?";
	private static String SFMLIMIT = "UPDATE Slice SET " + FMLIMIT + " = ? WHERE " + SLICE + " = ?";
	private static String GFMLIMIT = "SELECT " + FMLIMIT + " FROM Slice WHERE " + SLICE + " = ?" ;
	private static String GALLSLICE = "SELECT " + SLICE + " FROM SLICE ORDER BY id ASC";
	private static String NAMECHECK = "SELECT id FROM Slice WHERE " + SLICE + " = ?";
	private static String CONTCHECK = "SELECT id from Slice WHERE " + HOST + " = ? AND " + PORT + " = ?"; 
	private static String CREATESLICE = "INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt," +
			" passwd_salt, controller_hostname, controller_port, contact_email, drop_policy, lldp_spam, max_flow_rules) " +
			"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
	private static String DELETESLICE = "DELETE FROM Slice WHERE " + SLICE + " = ?";
	
	private static String SADMINSTATUS = "UPDATE SLICE SET " + ADMINDOWN + " = ?" +
			" WHERE " + SLICE + " = ?";
	private static String SLICEDOWN = "SELECT " + ADMINDOWN +" FROM Slice WHERE " + SLICE + " = ?";
	
	
	private static String SCRYPT = "UPDATE Slice SET " + CRYPT + " = ?, " + SALT +
			" = ? WHERE " + SLICE + " = ?";
	
	
	private static String FLOWVISOR = "SELECT id from " + Flowvisor.FLOWVISOR + " WHERE " + Flowvisor.CONFIG + " = ?"; 
	

	private ConfDBSettings settings = null;
	
	
	public static SliceImpl getInstance() {
		if (instance == null)
			instance = new SliceImpl();
		return instance;
	}

	@Override
	public void setlldp_spam(String sliceName, Boolean lldp_spam) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SLLDPSQL);
			ps.setBoolean(1, lldp_spam);
			ps.setString(2, sliceName);
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "LLDP update had no effect.");
			notify(sliceName, FLLDP, lldp_spam);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}	
	}
	
	

	@Override
	public void setdrop_policy(String sliceName, String policy) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SDROPSQL);
			ps.setString(1, policy);
			ps.setString(2, sliceName);
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Drop policy update had no effect.");
			notify(sliceName, FDROP, policy);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}	

	}

	@Override
	public void setcontroller_hostname(String sliceName, String name) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SHOSTSQL);
			ps.setString(1, name);
			ps.setString(2, sliceName);
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Controller host update had no effect.");
			notify(sliceName, FHOST, name);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
			throw new ConfigError("Unable to update controller hostname for slice " + sliceName);
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}

	}

	@Override
	public void setcontroller_port(String sliceName, Integer port) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SPORTSQL);
			ps.setInt(1, port);
			ps.setString(2, sliceName);
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Controller port update had no effect.");
			notify(sliceName, FPORT, port);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
			throw new ConfigError("Unable to set the controller port for slice " + sliceName);
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}

	}
	
	@Override
	public void setContactEmail(String sliceName, String email) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SEMAIL);
			ps.setString(1, email);
			ps.setString(2, sliceName);
			if (ps.executeUpdate() == 0)
				throw new ConfigError("Email for slice " + sliceName + " was not set to " + email);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
	}
	
	@Override
	public void setPasswd(String sliceName, String salt, String crypt) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SCRYPT);
			ps.setString(1, crypt);
			ps.setString(2, salt);
			ps.setString(3, sliceName);
			if (ps.executeUpdate() == 0)
				throw new ConfigError("Password for slice " + sliceName + " was not updated");
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);	
		}
	}
	
	@Override
	public void setMaxFlowMods(String sliceName, int limit) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SFMLIMIT);
			ps.setInt(1, limit);
			ps.setString(2, sliceName);
			if (ps.executeUpdate() == 0)
				throw new ConfigError("Global limit for slice " + sliceName + " was not set to " + limit);
			notify(sliceName, FFMLIMIT, limit);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
			
		}
	}

	@Override
	public Integer getMaxFlowMods(String sliceName) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GFMLIMIT);
			ps.setString(1, sliceName);
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
		return null;
	}

	@Override
	public Boolean getlldp_spam(String sliceName) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GLLDPSQL);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				return set.getBoolean(LLDP);
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
	public String getdrop_policy(String sliceName) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GDROPSQL);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				return set.getString(DROP);
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
	public String getcontroller_hostname(String sliceName) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GHOSTSQL);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				return set.getString(HOST);
			else
				throw new ConfigError("No such slice " + sliceName);
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
	public Integer getcontroller_port(String sliceName) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GPORTSQL);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				return set.getInt(PORT);
			else
				throw new ConfigError("Controller port for slice " + sliceName + " not found.");
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
	public String getPasswdElm(String sliceName, String elm) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			String stmt = GPASSELM.replaceFirst("<REPLACEME>", elm);
			conn = settings.getConnection();
			ps = conn.prepareStatement(stmt);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				return set.getString(elm);
			else
				throw new ConfigError("No " + elm + " found for " + sliceName);
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
	public String getCreator(String sliceName) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GCREATOR);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				return set.getString(CREATOR);
			else
				throw new ConfigError("Unknown slice " + sliceName);
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
	public String getEmail(String sliceName) throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GEMAIL);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next())
				return set.getString(EMAIL);
			else
				throw new ConfigError("Unknown slice " + sliceName);
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
	public LinkedList<String> getAllSliceNames() throws ConfigError {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		LinkedList<String> list = new LinkedList<String>();
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GALLSLICE);
			set = ps.executeQuery();
			while (set.next())
				list.add(set.getString(SLICE));
			if (list.isEmpty())
				throw new ConfigError("No slices defined");
			return list;
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
	public Boolean checkSliceName(String sliceName) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(NAMECHECK);
			ps.setString(1, sliceName);
			return ps.execute();
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
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String slice_email, String creatorSlice, int flowvisor_id, int type) throws InvalidSliceName,
			DuplicateControllerException {
		createSlice(sliceName, controller_hostname,
				controller_port, drop_policy, passwd,
				APIAuth.getSalt(), slice_email, creatorSlice, flowvisor_id, type);
	}
	
	@Override
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String slice_email, String creatorSlice, int flowvisor_id) throws InvalidSliceName,
			DuplicateControllerException {
		createSlice(sliceName, controller_hostname,
				controller_port, drop_policy, passwd,
				APIAuth.getSalt(), slice_email, creatorSlice, flowvisor_id,
				FlowMap.type.FEDERATED.ordinal());

	}

	@Override
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String slice_email, String creatorSlice) throws InvalidSliceName,
			DuplicateControllerException {
		createSlice(sliceName, controller_hostname,
				controller_port, drop_policy, passwd,
				APIAuth.getSalt(), slice_email, creatorSlice, 1,
				FlowMap.type.FEDERATED.ordinal());

	}
	
	@Override
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String salt, String slice_email, String creatorSlice)
			throws InvalidSliceName, DuplicateControllerException {
		createSlice(sliceName, controller_hostname,
				controller_port, drop_policy, passwd,
				salt, slice_email, creatorSlice, 1, FlowMap.type.FEDERATED.ordinal());
		
	}

	@Override
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String salt, String slice_email, String creatorSlice, int flowvisor_id, int type)
			throws DuplicateControllerException {
		String crypt = APIAuth.makeCrypt(salt, passwd);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(CONTCHECK);
			ps.setString(1, controller_hostname);
			ps.setInt(2, controller_port);
			set = ps.executeQuery();
			if (set.next())
				throw new DuplicateControllerException(controller_hostname, controller_port, sliceName, null);
            close(conn);
			conn = settings.getConnection();
			ps = conn.prepareStatement(CREATESLICE);
			ps.setInt(1, flowvisor_id);
			ps.setInt(2, type);
			ps.setString(3, sliceName);
			ps.setString(4, creatorSlice);
			ps.setString(5, crypt);
			ps.setString(6, salt);
			ps.setString(7, controller_hostname);
			ps.setInt(8, controller_port);
			ps.setString(9, slice_email);
			ps.setString(10, drop_policy);
			ps.setBoolean(11, true);
			ps.setInt(12, -1);
			if (ps.executeUpdate() == 0)
				FVLog.log(LogLevel.WARN, null, "Slice " + sliceName + " creation had no effect.");
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
			
		}	
	}
	
	
	@Override
	public void createSlice(String sliceName, String controller_hostname,
			int controller_port, String drop_policy, String passwd,
			String salt, String slice_email, String creatorSlice, boolean lldp_spam, 
			int maxFlowMods, int flowvisor_id, int type)
			throws DuplicateControllerException {
		String crypt = APIAuth.makeCrypt(salt, passwd);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(CONTCHECK);
			ps.setString(1, controller_hostname);
			ps.setInt(2, controller_port);
			set = ps.executeQuery();
			if (set.next())
				throw new DuplicateControllerException(controller_hostname, controller_port, sliceName, null);
            close(conn);
			conn = settings.getConnection();
			ps = conn.prepareStatement(CREATESLICE);
			ps.setInt(1, flowvisor_id);
			ps.setInt(2, type);
			ps.setString(3, sliceName);
			ps.setString(4, creatorSlice);
			ps.setString(5, crypt);
			ps.setString(6, salt);
			ps.setString(7, controller_hostname);
			ps.setInt(8, controller_port);
			ps.setString(9, slice_email);
			ps.setString(10, drop_policy);
			ps.setBoolean(11, lldp_spam);
			ps.setInt(12, maxFlowMods);
			if (ps.executeUpdate() == 0) {
				FVLog.log(LogLevel.WARN, null, "Slice " + sliceName + " creation had no effect.");
				return;
			}
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
			
		}	
	}

	
	@Override
	public void deleteSlice(String sliceName, Boolean preserve) 
			throws InvalidSliceName, ConfigError {
		if (preserve) 
			FlowSpaceImpl.getProxy().saveFlowSpace(sliceName);
		deleteSlice(sliceName);

	}
	
	@Override
	public void deleteSlice(String sliceName) throws InvalidSliceName {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(DELETESLICE);
			ps.setString(1, sliceName);
			if (ps.executeUpdate() == 0)
				throw new InvalidSliceName("Unknown slice name : " + sliceName);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
		}

	}
	
	@Override
	public void setAdminStatus(String sliceName, boolean status) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SADMINSTATUS);
			ps.setBoolean(1, status);
			ps.setString(2, sliceName);
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
	public boolean isSliceUp(String sliceName) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(SLICEDOWN);
			ps.setString(1, sliceName);
			set = ps.executeQuery();
			if (set.next()) 
				return set.getBoolean(ADMINDOWN);
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, e.getMessage());
		} finally {
			close(set);
			close(ps);
			close(conn);
		}
		return false;
	}
	
	@Override
	public void close(Connection conn) {
		//settings.returnConnection(conn);
		try {
			conn.close();
		} catch (Exception e) {
			// don't care
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

	public static Slice getProxy() {
		return (Slice) FVConfigurationController.instance()
		.getProxy(getInstance());
	}
	
	public static void addListener(String sliceName, SliceChangedListener l) {
		FVConfigurationController.instance().addChangeListener(sliceName, l);
	}
	
	public static void removeListener(String sliceName, FlowvisorChangedListener l) {
		FVConfigurationController.instance().removeChangeListener(sliceName, l);
	}

	@Override
	public HashMap<String, Object> toJson(HashMap<String, Object> output) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet set = null;	
		HashMap<String, Object> slice = new HashMap<String, Object>();
		LinkedList<Object> list = new LinkedList<Object>();
				
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(GALL);
			set = ps.executeQuery();
			while (set.next()) {
				slice.put(Flowvisor.CONFIG, set.getString(Flowvisor.CONFIG));
				slice.put(FMTYPE, FlowMap.type.values()[set.getInt(FMTYPE)].getText());
				slice.put(SLICE, set.getString(SLICE));
				slice.put(CREATOR, set.getString(CREATOR));
				slice.put(CRYPT, set.getString(CRYPT));
				slice.put(SALT, set.getString(SALT));
				slice.put(HOST, set.getString(HOST));
				slice.put(PORT, set.getInt(PORT));
				slice.put(EMAIL, set.getString(EMAIL));
				slice.put(DROP, set.getString(DROP));
				slice.put(LLDP, set.getBoolean(LLDP));
				slice.put(FMLIMIT, set.getInt(FMLIMIT));
				slice.put(ADMINDOWN, set.getBoolean(ADMINDOWN));
				
				list.add(slice.clone());
				slice.clear();
				
			}
			output.put(TSLICE, list);	
		} catch (SQLException e) {
			FVLog.log(LogLevel.WARN, null, "Failed to write slice information "  + e.getMessage());
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
		int flowvisorid = -1;
		try {
			conn = settings.getConnection();
			ps = conn.prepareStatement(FLOWVISOR);
			ps.setString(1, (String) row.get(Flowvisor.CONFIG));
			set = ps.executeQuery();
			if (set.next())
				flowvisorid = set.getInt("id");
			else
				throw new IOException("Unknown config name " + row.get(Flowvisor.CONFIG));
			ps = conn.prepareStatement(CREATESLICE);
			ps.setInt(1, flowvisorid);
			ps.setInt(2, FlowMap.type.fromString((String) row.get(FMTYPE)).ordinal());
			ps.setString(3, (String) row.get(SLICE));
			ps.setString(4, (String) row.get(CREATOR));
			ps.setString(5, (String) row.get(CRYPT));
			ps.setString(6, (String) row.get(SALT));
			ps.setString(7, (String) row.get(HOST));
			ps.setInt(8, ((Double) row.get(PORT)).intValue());
			ps.setString(9, (String) row.get(EMAIL));
			if (row.get(DROP) == null)
				row.put(DROP, "exact");
			ps.setString(10, (String) row.get(DROP));
			if (row.get(LLDP) == null)
				row.put(LLDP, true);
			ps.setBoolean(11, (Boolean) row.get(LLDP));
			if (row.get(FMLIMIT) != null)
				ps.setInt(12, ((Double) row.get(FMLIMIT)).intValue());
			else
				ps.setInt(12, -1);
			if (ps.executeUpdate() == 0) {
				FVLog.log(LogLevel.WARN, null, "Insertion failed... siliently.");
				return;
			}
			if (row.get(ADMINDOWN) != null)
				setAdminStatus((String) row.get(SLICE), (Boolean) row.get(ADMINDOWN));
			else 
				setAdminStatus((String) row.get(SLICE), true);
			} catch (SQLException e) {
				e.printStackTrace();
		} finally {
			close(set);
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
		FVLog.log(LogLevel.INFO, null, "Updating Slice database table.");
		if (version == 0) {
			processAlter("ALTER TABLE Slice ADD COLUMN " + FMLIMIT + " INT NOT NULL DEFAULT -1");
			version++;
		}
		if (version == 1) {
			processAlter("ALTER TABLE Slice ADD COLUMN " + ADMINDOWN + " BOOLEAN NOT NULL DEFAULT TRUE");
			version++;
		}
		
		
	}

	
	

}
