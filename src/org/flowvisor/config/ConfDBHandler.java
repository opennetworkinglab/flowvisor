package org.flowvisor.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;


/**
 * Defines a connection pool to derby. 
 * Guarantees that the status of a returned connection 
 * is valid.
 * 
 * 
 * @author ash
 *
 */
public class ConfDBHandler implements ConfDBSettings {
	
	private String protocol = null;
	private String dbName = null;
	private String username = null;
	private String password = null;
	
	private ConnectionFactory cf = null;
	
	@SuppressWarnings("unused")
	private PoolableConnectionFactory pcf = null;
	private GenericObjectPool gop = null;
	private PoolingDataSource pds = null;
	
	private Boolean autoCommit = false;
			
	
	public ConfDBHandler(String protocol, String dbName, String username, String password, Boolean autoCommit) {
		this.protocol = protocol;
		this.dbName = System.getProperty("derby.system.home") + "/" + dbName;
		this.username = username;
		this.password = password;
		this.autoCommit = autoCommit;
	}
	
	public ConfDBHandler() {
		this("jdbc:derby:", "FlowVisorDB", "", "", true);
	}
	
	private DataSource getDataSource() {
		if (pds != null) 
			return pds;
		
		gop = new GenericObjectPool(null);
		cf = new DriverManagerConnectionFactory(this.protocol + this.dbName, this.username, this.password);
		pcf = new PoolableConnectionFactory(cf, gop, null, null,false, autoCommit);
		pds = new PoolingDataSource(gop);
		
		return pds;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getDataSource().getConnection();		
	}

	@Override
	public Connection getConnection(String user, String pass)
			throws SQLException {
		return getDataSource().getConnection(user, pass);
	}
	
	@Override
	public void returnConnection(Connection conn) {
		try {
			gop.returnObject(conn);
		} catch (Exception e) {
			FVLog.log(LogLevel.CRIT, null, "Unable to return connection");
		}
	}

	@Override
	public int getNumActive() {
		return gop.getNumActive();
	}

	@Override
	public int getNumIdle() {
		return gop.getNumIdle();
	}

	@Override
	public int getMaxActive() {
		return gop.getMaxActive();
	}

	@Override
	public int getMaxIdle() {
		return gop.getMaxIdle();
	}

	@Override
	public Boolean getDefaultAutoCommit() {
		return autoCommit;
	}

	@Override
	public void shutdown() {
		try {
			gop.close();
			((EmbeddedDataSource) getDataSource()).setShutdownDatabase("shutdown");
		} catch (ClassCastException cce) {
			//Isn't this a derby db?
		} catch (Exception e) {
			FVLog.log(LogLevel.WARN, null, "Error on closing connection pool to derby");
		}
	}

}
