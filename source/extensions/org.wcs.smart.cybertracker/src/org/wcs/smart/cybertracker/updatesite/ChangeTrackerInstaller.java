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

/**
 * Installs change log tracking triggers for ct plugin.
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {

	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"trg_ct_properties_option_insert", "CREATE TRIGGER trg_ct_properties_option_insert AFTER INSERT ON smart.ct_properties_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_properties_option', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_ct_properties_option_update", "CREATE TRIGGER trg_ct_properties_option_update AFTER UPDATE ON smart.ct_properties_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_properties_option', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_ct_properties_option_delete", "CREATE TRIGGER trg_ct_properties_option_delete AFTER DELETE ON smart.ct_properties_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_properties_option', 'uuid', old.uuid, old.ca_uuid)"},

		{"trg_ct_properties_profile_insert", "CREATE TRIGGER trg_ct_properties_profile_insert AFTER INSERT ON smart.ct_properties_profile REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_properties_profile', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_ct_properties_profile_update", "CREATE TRIGGER trg_ct_properties_profile_update AFTER UPDATE ON smart.ct_properties_profile REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_properties_profile', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_ct_properties_profile_delete", "CREATE TRIGGER trg_ct_properties_profile_delete AFTER DELETE ON smart.ct_properties_profile REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_properties_profile', 'uuid', old.uuid, old.ca_uuid)"},

		{"trg_ct_properties_profile_option_insert", "CREATE TRIGGER trg_ct_properties_profile_option_insert AFTER INSERT ON smart.ct_properties_profile_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_properties_profile_option', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_ct_properties_profile_option_update", "CREATE TRIGGER trg_ct_properties_profile_option_update AFTER UPDATE ON smart.ct_properties_profile_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_properties_profile_option', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_ct_properties_profile_option_delete", "CREATE TRIGGER trg_ct_properties_profile_option_delete AFTER DELETE ON smart.ct_properties_profile_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_properties_profile_option', 'uuid', old.uuid, old.ca_uuid)"},
	};

	@Override
	public String[][] getTriggers() {
		return triggers;
	}

}
