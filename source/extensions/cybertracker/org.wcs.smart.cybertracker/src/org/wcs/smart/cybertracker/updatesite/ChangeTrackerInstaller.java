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
package org.wcs.smart.cybertracker.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;

/**
 * Installs change log tracking triggers for ct plugin.
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {

	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"smart.trg_ct_properties_option_insert", "CREATE TRIGGER smart.trg_ct_properties_option_insert AFTER INSERT ON smart.ct_properties_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_properties_option', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_properties_option_update", "CREATE TRIGGER smart.trg_ct_properties_option_update AFTER UPDATE ON smart.ct_properties_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_properties_option', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_properties_option_delete", "CREATE TRIGGER smart.trg_ct_properties_option_delete AFTER DELETE ON smart.ct_properties_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_properties_option', 'uuid', old.uuid, old.ca_uuid)"},

		{"smart.trg_ct_properties_profile_insert", "CREATE TRIGGER smart.trg_ct_properties_profile_insert AFTER INSERT ON smart.ct_properties_profile REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_properties_profile', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_properties_profile_update", "CREATE TRIGGER smart.trg_ct_properties_profile_update AFTER UPDATE ON smart.ct_properties_profile REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_properties_profile', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_properties_profile_delete", "CREATE TRIGGER smart.trg_ct_properties_profile_delete AFTER DELETE ON smart.ct_properties_profile REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_properties_profile', 'uuid', old.uuid, old.ca_uuid)"},

		{"smart.trg_ct_properties_profile_option_insert", "CREATE TRIGGER smart.trg_ct_properties_profile_option_insert AFTER INSERT ON smart.ct_properties_profile_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_properties_profile_option', 'uuid', new.uuid, p.ca_uuid FROM smart.ct_properties_profile p WHERE new.profile_uuid = p.uuid"},
		{"smart.trg_ct_properties_profile_option_update", "CREATE TRIGGER smart.trg_ct_properties_profile_option_update AFTER UPDATE ON smart.ct_properties_profile_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_properties_profile_option', 'uuid', new.uuid, p.ca_uuid FROM smart.ct_properties_profile p WHERE new.profile_uuid = p.uuid"},
		{"smart.trg_ct_properties_profile_option_delete", "CREATE TRIGGER smart.trg_ct_properties_profile_option_delete AFTER DELETE ON smart.ct_properties_profile_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_properties_profile_option', 'uuid', old.uuid, p.ca_uuid FROM smart.ct_properties_profile p WHERE old.profile_uuid = p.uuid"},
		
		{"smart.trg_ct_incident_link_insert", "CREATE TRIGGER smart.trg_ct_incident_link_insert AFTER INSERT ON smart.ct_incident_link REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_incident_link', 'uuid', new.uuid, p.ca_uuid FROM smart.waypoint p WHERE new.wp_uuid = p.uuid"},
		{"smart.trg_ct_incident_link_update", "CREATE TRIGGER smart.trg_ct_incident_link_update AFTER UPDATE ON smart.ct_incident_link REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_incident_link', 'uuid', new.uuid, p.ca_uuid FROM smart.waypoint p WHERE new.wp_uuid = p.uuid"},
		{"smart.trg_ct_incident_link_delete", "CREATE TRIGGER smart.trg_ct_incident_link_delete AFTER DELETE ON smart.ct_incident_link REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_incident_link', 'uuid', old.uuid, p.ca_uuid FROM smart.waypoint p WHERE old.wp_uuid = p.uuid"},

		{"smart.trg_ct_metadata_value_insert", "CREATE TRIGGER smart.trg_ct_metadata_value_insert AFTER INSERT ON smart.ct_metadata_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_metadata_value', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_metadata_value_update", "CREATE TRIGGER smart.trg_ct_metadata_value_update AFTER UPDATE ON smart.ct_metadata_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_metadata_value', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_metadata_value_delete", "CREATE TRIGGER smart.trg_ct_metadata_value_delete AFTER DELETE ON smart.ct_metadata_value REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_metadata_value', 'uuid', old.uuid, old.ca_uuid)"},
		
		{"smart.trg_ct_metadata_value_uuid_insert", "CREATE TRIGGER smart.trg_ct_metadata_value_uuid_insert AFTER INSERT ON smart.ct_metadata_value_uuid REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(op.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_metadata_value_uuid', 'uuid', new.uuid, op.ca_uuid from smart.ct_metadata_value op where op.uuid = new.field_uuid"},
		{"smart.trg_ct_metadata_value_uuid_update", "CREATE TRIGGER smart.trg_ct_metadata_value_uuid_update AFTER UPDATE ON smart.ct_metadata_value_uuid REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(op.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_metadata_value_uuid', 'uuid', new.uuid, op.ca_uuid from smart.ct_metadata_value op where op.uuid = new.field_uuid"},
		{"smart.trg_ct_metadata_value_uuid_delete", "CREATE TRIGGER smart.trg_ct_metadata_value_uuid_delete AFTER DELETE ON smart.ct_metadata_value_uuid REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(op.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_metadata_value_uuid', 'uuid', old.uuid, op.ca_uuid from smart.ct_metadata_value op where op.uuid = old.field_uuid"},

		{"smart.trg_ct_navigation_layer_insert", "CREATE TRIGGER smart.trg_ct_navigation_layer_insert AFTER INSERT ON smart.ct_navigation_layer REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_navigation_layer', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_navigation_layer_update", "CREATE TRIGGER smart.trg_ct_navigation_layer_update AFTER UPDATE ON smart.ct_navigation_layer REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_navigation_layer', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_navigation_layer_delete", "CREATE TRIGGER smart.trg_ct_navigation_layer_delete AFTER DELETE ON smart.ct_navigation_layer REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_navigation_layer', 'uuid', old.uuid, old.ca_uuid)"},
		
		{"smart.trg_ct_device_insert", "CREATE TRIGGER smart.trg_ct_device_insert AFTER INSERT ON smart.ct_device REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_device', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_device_update", "CREATE TRIGGER smart.trg_ct_device_update AFTER UPDATE ON smart.ct_device REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_device', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_ct_device_delete", "CREATE TRIGGER smart.trg_ct_device_delete AFTER DELETE ON smart.ct_device REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_device', 'uuid', old.uuid, old.ca_uuid)"},
		
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
		return CyberTrackerPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return CyberTrackerPlugIn.DB_VERSION;
	}
}
