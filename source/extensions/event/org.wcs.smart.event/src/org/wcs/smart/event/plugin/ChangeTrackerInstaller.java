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
package org.wcs.smart.event.plugin;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.event.EventPlugIn;

/**
 * Installs triggers for change tracking the plan plugin.
 * 
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {
	
	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"smart.trg_e_event_filter_insert", "CREATE TRIGGER smart.trg_e_event_filter_insert AFTER INSERT ON smart.e_event_filter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_event_filter', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_e_event_filter_update", "CREATE TRIGGER smart.trg_e_event_filter_update AFTER UPDATE ON smart.e_event_filter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_event_filter', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_e_event_filter_delete", "CREATE TRIGGER smart.trg_e_event_filter_delete AFTER DELETE ON smart.e_event_filter REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_event_filter', 'uuid', old.uuid, old.ca_uuid)"},

		{"smart.trg_e_action_insert", "CREATE TRIGGER smart.trg_e_action_insert AFTER INSERT ON smart.e_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_action', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_e_action_update", "CREATE TRIGGER smart.trg_e_action_update AFTER UPDATE ON smart.e_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_action', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_e_action_delete", "CREATE TRIGGER smart.trg_e_action_delete AFTER DELETE ON smart.e_action REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_action', 'uuid', old.uuid, old.ca_uuid)"},

		{"smart.trg_e_action_parameter_value_insert", "CREATE TRIGGER smart.trg_e_action_parameter_value_insert AFTER INSERT ON smart.e_action_parameter_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_action_parameter_value', 'action_uuid', new.action_uuid, 'parameter_key', new.parameter_key, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid"},
		{"smart.trg_e_action_parameter_value_update", "CREATE TRIGGER smart.trg_e_action_parameter_value_update AFTER UPDATE ON smart.e_action_parameter_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_action_parameter_value', 'action_uuid', new.action_uuid, 'parameter_key', new.parameter_key, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid"},
		{"smart.trg_e_action_parameter_value_delete", "CREATE TRIGGER smart.trg_e_action_parameter_value_delete AFTER DELETE ON smart.e_action_parameter_value REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_action_parameter_value', 'action_uuid', old.action_uuid, 'parameter_key', old.parameter_key, r.ca_uuid from smart.e_action r where r.uuid = old.action_uuid"},

		{"smart.trg_e_event_action_insert", "CREATE TRIGGER smart.trg_e_event_action_insert AFTER INSERT ON smart.e_event_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_event_action', 'uuid', new.uuid, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid"},
		{"smart.trg_e_event_action_update", "CREATE TRIGGER smart.trg_e_event_action_update AFTER UPDATE ON smart.e_event_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_event_action', 'uuid', new.uuid, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid"},
		{"smart.trg_e_event_action_delete", "CREATE TRIGGER smart.trg_e_event_action_delete AFTER DELETE ON smart.e_event_action REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_event_action', 'uuid', old.uuid, r.ca_uuid from smart.e_action r where r.uuid = old.action_uuid"},
	};
	
	@Override
	public String[][] getCurrentTriggers() {
		return triggers;
	}

	@Override
	public String[] getAllTriggersToDrop(){
		String[] t = new String[triggers.length];
		for (int i = 0; i < triggers.length; i ++){
			t[i] = triggers[i][0];
		}
		return t;
	}
	
	
	@Override
	public String getPluginId() {
		return EventPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return EventPlugIn.DB_VERSION;
	}
}

