/*
 * Copyright (C) 2017 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.qa.plugin;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.QaPlugIn;

/**
 * Adds and or upgrades intelligence plugin
 * 
 * @author Emily
 *
 */
public class DatabaseTables {

	@SuppressWarnings("nls")
	public static void createTables(Session session){
		String[] sql = new String[]{
				 // Create Tables
				"CREATE TABLE smart.qa_error( uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data not null, qa_routine_uuid char(16) for bit data NOT NULL, data_provider_id varchar(128) not null, status varchar(32) NOT NULL, validate_date timestamp NOT NULL, error_id varchar(1024) NOT NULL, error_description varchar(32600), src_identifier char(16) for bit data NOT NULL, geometry blob, PRIMARY KEY (uuid) )",
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
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					QaPlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}
			}
			
		});
		HibernateManager.setPlugInVersion(QaPlugIn.PLUGIN_ID, QaPlugIn.DB_VERSION_1, session);
	}
	
	
	@SuppressWarnings("nls")
	public String[][] getChangeLogTriggers(){
		return new String[][]{
			{"smart.trg_qa_routine_insert", "CREATE TRIGGER smart.trg_qa_routine_insert AFTER INSERT ON smart.qa_routine REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.qa_routine', 'uuid', new.uuid, new.ca_uuid)"},
			{"smart.trg_qa_routine_update", "CREATE TRIGGER smart.trg_qa_routine_update AFTER UPDATE ON smart.qa_routine REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.qa_routine', 'uuid', new.uuid, new.ca_uuid)"},
			{"smart.trg_qa_routine_delete", "CREATE TRIGGER smart.trg_qa_routine_delete AFTER DELETE ON smart.qa_routine REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.qa_routine', 'uuid', old.uuid, old.ca_uuid)"},
			{"smart.trg_qa_routine_parameter_insert", "CREATE TRIGGER smart.trg_qa_routine_parameter_insert AFTER INSERT ON smart.qa_routine_parameter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'INSERT', 'smart.qa_routine_parameter', 'uuid', new.uuid, r.ca_uuid from smart.qa_routine r where r.qa_routine_uuid = new.uuid"},
			{"smart.trg_qa_routine_parameter_update", "CREATE TRIGGER smart.trg_qa_routine_parameter_update AFTER UPDATE ON smart.qa_routine_parameter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'UPDATE', 'smart.qa_routine_parameter', 'uuid', new.uuid, r.ca_uuid from smart.qa_routine r where r.qa_routine_uuid = new.uuid"},
			{"smart.trg_qa_routine_parameter_delete", "CREATE TRIGGER smart.trg_qa_routine_parameter_delete AFTER DELETE ON smart.qa_routine_parameter REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'DELETE', 'smart.qa_routine_parameter', 'uuid', old.uuid, r.ca_uuid from smart.qa_routine r where r.qa_routine_uuid = old.uuid"},
			{"smart.trg_qa_error_insert", "CREATE TRIGGER smart.trg_qa_error_insert AFTER INSERT ON smart.qa_error REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.qa_error', 'uuid', new.uuid, new.ca_uuid)"},
			{"smart.trg_qa_error_update", "CREATE TRIGGER smart.trg_qa_error_update AFTER UPDATE ON smart.qa_error REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.qa_error', 'uuid', new.uuid, new.ca_uuid)"},
			{"smart.trg_qa_error_delete", "CREATE TRIGGER smart.trg_qa_error_delete AFTER DELETE ON smart.qa_error REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.qa_error', 'uuid', old.uuid, old.ca_uuid)"},
		};
	}
}
