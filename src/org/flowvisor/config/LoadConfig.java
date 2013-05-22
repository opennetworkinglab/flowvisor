package org.flowvisor.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.APIAuth;

/**
 * List of things to populate FVConfig with on startup Everything here can be
 * overridden from the config file. 
 * 
 * If called with a parameter, it dumps the config to that file.
 *
 * @author ash
 *
 */
@SuppressWarnings("deprecation")
public class LoadConfig {
	
	//private static int WILDCARDS = OFMatch.OFPFW_ALL & ~(OFMatch.OFPFW_DL_SRC | OFMatch.OFPFW_IN_PORT);
	
	private static String CLEAR = "DELETE FROM jFSRSlice;\n" +
			"ALTER TABLE jFSRSlice ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM Slice;\n" +
			"ALTER TABLE Slice ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM FlowSpaceRule;\n" +
			"ALTER TABLE FlowSpaceRule ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM Flowvisor;\n" +
			"ALTER TABLE Flowvisor ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM Switch; \n" +
			"ALTER TABLE Switch ALTER COLUMN id RESTART WITH 1;\n";
			
	private static String  defaultconfig = "DELETE FROM jFSRSlice;\n" +
			"ALTER TABLE jFSRSlice ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM Slice;\n" +
			"ALTER TABLE Slice ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM FlowSpaceRule;\n" +
			"ALTER TABLE FlowSpaceRule ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM Flowvisor;\n" +
			"ALTER TABLE Flowvisor ALTER COLUMN id RESTART WITH 1;\n" +
			"INSERT INTO Flowvisor(config_name,run_topology_server,db_version, version) VALUES('default', false, " 
				+ FlowVisor.FLOWVISOR_DB_VERSION + ",'" + FlowVisor.FLOWVISOR_VERSION + "');\n" +
			"INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt, passwd_salt, " +
				"controller_hostname, controller_port, contact_email) VALUES(1, 1, 'fvadmin', 'fvadmin', " +
				"'CHANGEME', 'CHANGESALT', 'none', 0, 'fvadmin@localhost');\n" ;

	public static void defaultConfig(String passwd) {
		String salt = APIAuth.getSalt();
		String pass = APIAuth.makeCrypt(salt, passwd);
		String config = defaultconfig.replace("CHANGEME", pass);
		config = config.replace("CHANGESALT", salt);
		ConfDBHandler db = new ConfDBHandler();
		try {
			importSQL(db.getConnection(), new StringBufferInputStream(config));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}


	
	public static void loadConfig(String filename) {
		ConfDBHandler db = new ConfDBHandler();
		try {
			clearDB(db);
			importSQL(db.getConnection(), new FileInputStream(filename));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void clearDB(ConfDBHandler db) {
		try {
			importSQL(db.getConnection(), new StringBufferInputStream(CLEAR));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}



	private static void importSQL(Connection conn, InputStream in) throws SQLException
	{
	        Scanner s = new Scanner(in);
	        s.useDelimiter("(;(\r)?\n)|(--\n)");
	        Statement st = null;
	        conn.setAutoCommit(true);
	        
	        try
	        {
	                st = conn.createStatement();
	                while (s.hasNext())
	                {
	                        String line = s.next();
	                        if (line.startsWith("/*!") && line.endsWith("*/"))
	                        {
	                                int i = line.indexOf(' ');
	                                line = line.substring(i + 1, line.length() - " */".length());
	                        }

	                        if (line.trim().length() > 0)
	                        {
	                                st.execute(line);
	                        }
	                }
	        }
	        finally
	        {
	                if (st != null) st.close();
	                conn.close();
	        }
	}
	
	/**
	 * Print default config to stdout
	 *
	 * @param args
	 * @throws FileNotFoundException
	 * @throws SQLException 
	 */

	public static void main(String args[]) throws FileNotFoundException, ConfigError {
		if (args.length > 0) {
			FVConfigurationController.init(new ConfDBHandler());
			FVConfig.readFromFile(args[0]);
			FVConfigurationController.instance().shutdown();
			return;
		}
			
		
		System.err.println("Generating default config");
		LoadConfig.defaultConfig("CHANGEME");
		System.err.println("Done.");

		
		 
	}
}
