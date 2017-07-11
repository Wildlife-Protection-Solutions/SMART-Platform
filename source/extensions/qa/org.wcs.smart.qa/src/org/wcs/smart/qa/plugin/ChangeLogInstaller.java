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

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.qa.QaPlugIn;

/**
 * Change log trigger installer for QA tables
 * 
 * @author Emily
 *
 */
public class ChangeLogInstaller extends AbstractChangeTrackerInstaller{

	@SuppressWarnings("nls")
	public String[][] triggers =  new String[][]{
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
	
	
	@Override
	public String[][] getCurrentTriggers() {
		return triggers;
	}

	@Override
	public String[] getAllTriggersToDrop() {
		String[] t = new String[triggers.length];
		for (int i = 0; i < triggers.length; i++) {
			t[i] = triggers[i][0];
		}
		return t;
	}

	@Override
	public String getPluginId() {
		return QaPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return QaPlugIn.DB_VERSION;
	}

}
