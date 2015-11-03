/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;

/**
 * Installs triggers for tracking changes in connect plugin.
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller{

	private String[][] triggers = new String[][]{
		{"trg_connect_account_insert", "CREATE TRIGGER trg_connect_account_insert AFTER INSERT ON smart.connect_account REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.connect_account', 'employee_uuid', new.employee_uuid, server.ca_uuid from smart.connect_server server where server.uuid = new.connect_uuid"},
		{"trg_connect_account_update", "CREATE TRIGGER trg_connect_account_update AFTER UPDATE ON smart.connect_account REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.connect_account', 'employee_uuid', new.employee_uuid, server.ca_uuid from smart.connect_server server where server.uuid = new.connect_uuid"},
		{"trg_connect_account_delete", "CREATE TRIGGER trg_connect_account_delete AFTER DELETE ON smart.connect_account REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.connect_account', 'employee_uuid', old.employee_uuid, server.ca_uuid from smart.connect_server server where server.uuid = old.connect_uuid"},
		{"trg_connect_server_insert", "CREATE TRIGGER trg_connect_server_insert AFTER INSERT ON smart.connect_server REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.connect_server', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_connect_server_update", "CREATE TRIGGER trg_connect_server_update AFTER UPDATE ON smart.connect_server REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.connect_server', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_connect_server_delete", "CREATE TRIGGER trg_connect_server_delete AFTER DELETE ON smart.connect_server REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.connect_server', 'uuid', old.uuid, old.ca_uuid)"},	
		{"trg_connect_server_option_insert", "CREATE TRIGGER trg_connect_server_option_insert AFTER INSERT ON smart.connect_server_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.uuid(), 'INSERT', 'smart.connect_server_option', 'server_uuid', new.server_uuid, 'option_key', new.option_key, server.ca_uuid from smart.connect_server server where server.uuid = new.server_uuid"},
		{"trg_connect_server_option_update", "CREATE TRIGGER trg_connect_server_option_update AFTER UPDATE ON smart.connect_server_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.connect_server_option', 'server_uuid', new.server_uuid, 'option_key', new.option_key, server.ca_uuid from smart.connect_server server where server.uuid = new.server_uuid"},
		{"trg_connect_server_option_delete", "CREATE TRIGGER trg_connect_server_option_delete AFTER DELETE ON smart.connect_server_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.uuid(), 'DELETE', 'smart.connect_server_option', 'server_uuid', old.server_uuid, 'option_key', old.option_key, server.ca_uuid from smart.connect_server server where server.uuid = old.server_uuid"},
	};
	
	@Override
	public String[][] getTriggers() {
		return triggers;
	}

}
