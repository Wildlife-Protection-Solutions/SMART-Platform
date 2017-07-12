package org.wcs.smart.upgrade.v600;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;

public class QaPlugInInstaller {
	
	@SuppressWarnings("nls")
	public static void createTables(Session session, Connection c) throws SQLException{
		String[] sql = new String[]{
				 // Create Tables
				"CREATE TABLE smart.qa_error( uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data not null, qa_routine_uuid char(16) for bit data NOT NULL, data_provider_id varchar(128) not null, status varchar(32) NOT NULL, validate_date timestamp NOT NULL, error_id varchar(1024) NOT NULL, error_description varchar(32600), fix_message varchar(32600), src_identifier char(16) for bit data NOT NULL, geometry blob, PRIMARY KEY (uuid) )",
				"CREATE TABLE smart.qa_routine(uuid char(16) FOR BIT DATA NOT NULL, ca_uuid char(16) FOR BIT DATA NOT NULL, routine_type_id varchar(1024) NOT NULL, description varchar(32600), auto_check boolean DEFAULT false NOT NULL, PRIMARY KEY (uuid))",
				"CREATE TABLE smart.qa_routine_parameter( uuid char(16) FOR BIT DATA NOT NULL, qa_routine_uuid char(16) FOR BIT DATA NOT NULL, id varchar(256) NOT NULL, str_value varchar(32600), byte_value blob, PRIMARY KEY (uuid, qa_routine_uuid) )",

				// Create Foreign Keys
				"ALTER TABLE smart.qa_routine ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.qa_error ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.qa_routine_parameter ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.qa_error ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) DEFERRABLE INITIALLY IMMEDIATE",

				//Permissions
				"GRANT ALL PRIVILEGES ON smart.qa_error TO admin,data_entry,manager,analyst",
				"GRANT ALL PRIVILEGES ON smart.qa_routine TO admin,manager",
				"GRANT ALL PRIVILEGES ON smart.qa_error TO admin,manager",
				"GRANT SELECT ON smart.qa_routine TO data_entry,analyst",
				"GRANT SELECT ON smart.qa_error TO data_entry,analyst",
		};
		
		for (String s : sql){
			SmartPlugIn.log(s, null);
			c.createStatement().executeUpdate(s);
		}

		HibernateManager.setPlugInVersion("org.wcs.smart.qa", "1.0", session);
	}
}
