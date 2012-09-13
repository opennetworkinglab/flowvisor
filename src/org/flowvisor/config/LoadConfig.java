package org.flowvisor.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import org.flowvisor.api.APIAuth;
import org.openflow.protocol.OFMatch;

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
	
	private static int WILDCARDS = OFMatch.OFPFW_ALL & ~(OFMatch.OFPFW_DL_SRC | OFMatch.OFPFW_IN_PORT);
	
	private static String  defaultconfig = "DELETE FROM jFSRSlice;\n" +
			"ALTER TABLE jFSRSlice ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM Slice;\n" +
			"ALTER TABLE Slice ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM FlowSpaceRule;\n" +
			"ALTER TABLE FlowSpaceRule ALTER COLUMN id RESTART WITH 1;\n" +
			"DELETE FROM Flowvisor;\n" +
			"ALTER TABLE Flowvisor ALTER COLUMN id RESTART WITH 1;\n" +
			"INSERT INTO Flowvisor(config_name) VALUES('default');\n" +
			"INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt, passwd_salt, controller_hostname, controller_port, contact_email) VALUES(1, 1, 'fvadmin', 'fvadmin', 'CHANGEME', 'sillysalt', 'none', 0, 'fvadmin@localhost');\n" +
			"INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt, passwd_salt, controller_hostname, controller_port, contact_email) VALUES(1, 1, 'alice', 'fvadmin', 'alicePass', 'sillysalt', 'localhost', 54321, 'alice@foo.com');\n" +
			"INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt, passwd_salt, controller_hostname, controller_port, contact_email) VALUES(1, 1, 'bob', 'fvadmin', 'bobPass', 'sillysalt', 'localhost', 54322, 'bob@foo.com');\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 0, 2, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 2, 2, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 3, 2, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 0, 4294967298, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 2, 4294967298, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 3, 4294967298, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 1, 1, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 3, 1, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 1, 4294967297, "+ WILDCARDS + ");\n" +
			"INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src, wildcards) VALUES(-9223372036854775808, 32000, 3, 4294967297, "+ WILDCARDS + ");\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(1,2,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(2,2,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(3,2,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(4,2,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(5,2,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(6,2,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(7,3,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(8,3,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(9,3,4);\n" +
			"INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(10,3,4);\n";
	

	public static void defaultConfig(String passwd) {
		String config = defaultconfig.replace("CHANGEME", APIAuth.makeCrypt("sillysalt", passwd));
		config = config.replace("alicePass", APIAuth.makeCrypt("sillysalt", "alicePass"));
		config = config.replace("bobPass", APIAuth.makeCrypt("sillysalt", "bobPass"));
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
			importSQL(db.getConnection(), new FileInputStream(filename));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
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

	public static void main(String args[]) throws FileNotFoundException {
		if (args.length > 0) {
			FVConfigurationController.init(new ConfDBHandler());
			FVConfig.readFromFile(args[0]);
			return;
		}
			
		
		System.err.println("Generating default config");
		LoadConfig.defaultConfig("CHANGEME");
		System.err.println("Done.");

		
		 
	}
}
