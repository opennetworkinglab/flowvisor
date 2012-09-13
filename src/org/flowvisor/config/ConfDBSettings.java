package org.flowvisor.config;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * 
 * As FV doesn't mandate that derby be the only repository 
 * type, we define this interface to enforce that no matter
 * what the data repository is the behaviour of FV  will not
 * be modified. In theory, of course.  
 * 
 * @author ash
 *
 */
public interface ConfDBSettings {
	/**
	 * Obtain a connection for the underlying data
	 * repository.
	 * 
	 * 
	 * @return an active connection.
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException;
	
	/**
	 * Obtain a connection for the underlying data
	 * repository given a username and password.
	 * 
	 * @param user the username
	 * @param pass the password
	 * @return an active connection
	 * @throws SQLException
	 */
	public Connection getConnection(String user, String pass) throws SQLException;
	
	/**
	 * Returns the connection to the connection pool, 
	 * if applicable.
	 * 
	 * 
	 * @param conn - the closed connection.
	 */
	public void returnConnection(Connection conn);
	
	/**
	 * Shutdown the data repository cleanly.
	 */
	public void shutdown();
	
	/**
	 * Returns the number of active connections
	 * to the data repository.
	 * 
	 * @return number of active connections.
	 */
	public int getNumActive();
	
	/**
	 * Returns the number of idle connections
	 * to the data repository.
	 * 
	 * @return number of idle connections.
	 */
	public int getNumIdle();
	
	/**
	 * Returns the maximal number of active connections
	 * to the data repository.
	 * 
	 * @return max number of active connections.
	 */
	public int getMaxActive();
	
	/**
	 * Returns the max number of idle connections
	 * to the data repository.
	 * 
	 * @return max number of idle connections.
	 */
	public int getMaxIdle();
	
	/**
	 * Obtain the default commit mode. Default is true.
	 * 
	 * @return autocommit mode.
	 */
	public Boolean getDefaultAutoCommit();
}
