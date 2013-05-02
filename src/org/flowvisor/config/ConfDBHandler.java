package org.flowvisor.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
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
	
	private String dbName = null;

	
	private EmbeddedConnectionPoolDataSource pds = null;
			
	
	public ConfDBHandler(String dbName) {
		this.dbName = System.getProperty("derby.system.home") + "/" + dbName;
	}
	
	public ConfDBHandler() {
		this("FlowVisorDB");
	}
	
	private DataSource getDataSource() {
		if (pds != null) 
			return pds;
		
	    
		
		pds = new EmbeddedConnectionPoolDataSource();
		pds.setDatabaseName(this.dbName);
		
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
	public void shutdown() {
		try {
			//gop.close();
			((EmbeddedDataSource) getDataSource()).setShutdownDatabase("shutdown");
		} catch (ClassCastException cce) {
			//Isn't this a derby db?
		} catch (Exception e) {
			FVLog.log(LogLevel.WARN, null, "Error on closing connection pool to derby");
		}
	}

}
