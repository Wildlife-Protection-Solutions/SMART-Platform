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
package org.wcs.smart.intelligence.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
/**
 * Class for installing change log triggers for intelligence plugin.
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {

	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"trg_informant_insert", "CREATE TRIGGER trg_informant_insert AFTER INSERT ON smart.informant REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.informant', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_informant_update", "CREATE TRIGGER trg_informant_update AFTER UPDATE ON smart.informant REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.informant', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_informant_delete", "CREATE TRIGGER trg_informant_delete AFTER DELETE ON smart.informant REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.informant', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_patrol_intelligence_insert", "CREATE TRIGGER trg_patrol_intelligence_insert AFTER INSERT ON smart.patrol_intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.patrol_intelligence', 'patrol_uuid', new.patrol_uuid, 'intelligence_uuid', new.intelligence_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid"},
		{"trg_patrol_intelligence_update", "CREATE TRIGGER trg_patrol_intelligence_update AFTER UPDATE ON smart.patrol_intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.patrol_intelligence', 'patrol_uuid', new.patrol_uuid, 'intelligence_uuid', new.intelligence_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid"},
		{"trg_patrol_intelligence_delete", "CREATE TRIGGER trg_patrol_intelligence_delete AFTER DELETE ON smart.patrol_intelligence REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.patrol_intelligence', 'patrol_uuid', old.patrol_uuid, 'intelligence_uuid', old.intelligence_uuid, p.ca_uuid from smart.patrol p where p.uuid = old.patrol_uuid"},	
		{"trg_intelligence_insert", "CREATE TRIGGER trg_intelligence_insert AFTER INSERT ON smart.intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.intelligence', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intelligence_update", "CREATE TRIGGER trg_intelligence_update AFTER UPDATE ON smart.intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.intelligence', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intelligence_delete", "CREATE TRIGGER trg_intelligence_delete AFTER DELETE ON smart.intelligence REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.intelligence', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_intelligence_attachment_insert", "CREATE TRIGGER trg_intelligence_attachment_insert AFTER INSERT ON smart.intelligence_attachment REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.intelligence_attachment', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid"},
		{"trg_intelligence_attachment_update", "CREATE TRIGGER trg_intelligence_attachment_update AFTER UPDATE ON smart.intelligence_attachment REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.intelligence_attachment', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid"},
		{"trg_intelligence_attachment_delete", "CREATE TRIGGER trg_intelligence_attachment_delete AFTER DELETE ON smart.intelligence_attachment REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.intelligence_attachment', 'uuid', old.uuid, i.ca_uuid from smart.intelligence i where i.uuid = old.intelligence_uuid"},
		{"trg_intelligence_point_insert", "CREATE TRIGGER trg_intelligence_point_insert AFTER INSERT ON smart.intelligence_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.intelligence_point', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid"},
		{"trg_intelligence_point_update", "CREATE TRIGGER trg_intelligence_point_update AFTER UPDATE ON smart.intelligence_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.intelligence_point', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid"},
		{"trg_intelligence_point_delete", "CREATE TRIGGER trg_intelligence_point_delete AFTER DELETE ON smart.intelligence_point REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.intelligence_point', 'uuid', old.uuid, i.ca_uuid from smart.intelligence i where i.uuid = old.intelligence_uuid"},
		{"trg_intelligence_source_insert", "CREATE TRIGGER trg_intelligence_source_insert AFTER INSERT ON smart.intelligence_source REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.intelligence_source', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intelligence_source_update", "CREATE TRIGGER trg_intelligence_source_update AFTER UPDATE ON smart.intelligence_source REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.intelligence_source', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intelligence_source_delete", "CREATE TRIGGER trg_intelligence_source_delete AFTER DELETE ON smart.intelligence_source REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.intelligence_source', 'uuid', old.uuid, old.ca_uuid)"},
	};
	
	@Override
	public String[][] getTriggers() {
		return triggers;
	}

}
