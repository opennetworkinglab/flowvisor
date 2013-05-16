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
	 * Shutdown the data repository cleanly.
	 */
	public void shutdown();
	
}
