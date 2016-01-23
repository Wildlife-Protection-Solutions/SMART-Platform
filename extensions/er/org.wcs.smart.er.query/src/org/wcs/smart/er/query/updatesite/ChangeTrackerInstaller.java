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
package org.wcs.smart.er.query.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.er.query.ERQueryPlugIn;

/**
 * Installs triggers for tracking changes in er query plugin.
 * 
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {

	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"trg_survey_gridded_query_insert", "CREATE TRIGGER trg_survey_gridded_query_insert AFTER INSERT ON smart.survey_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.survey_gridded_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_gridded_query_update", "CREATE TRIGGER trg_survey_gridded_query_update AFTER UPDATE ON smart.survey_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.survey_gridded_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_gridded_query_delete", "CREATE TRIGGER trg_survey_gridded_query_delete AFTER DELETE ON smart.survey_gridded_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.survey_gridded_query', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_survey_mission_query_insert", "CREATE TRIGGER trg_survey_mission_query_insert AFTER INSERT ON smart.survey_mission_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.survey_mission_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_mission_query_update", "CREATE TRIGGER trg_survey_mission_query_update AFTER UPDATE ON smart.survey_mission_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.survey_mission_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_mission_query_delete", "CREATE TRIGGER trg_survey_mission_query_delete AFTER DELETE ON smart.survey_mission_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.survey_mission_query', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_survey_mission_track_query_insert", "CREATE TRIGGER trg_survey_mission_track_query_insert AFTER INSERT ON smart.survey_mission_track_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.survey_mission_track_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_mission_track_query_update", "CREATE TRIGGER trg_survey_mission_track_query_update AFTER UPDATE ON smart.survey_mission_track_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.survey_mission_track_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_mission_track_query_delete", "CREATE TRIGGER trg_survey_mission_track_query_delete AFTER DELETE ON smart.survey_mission_track_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.survey_mission_track_query', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_survey_observation_query_insert", "CREATE TRIGGER trg_survey_observation_query_insert AFTER INSERT ON smart.survey_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.survey_observation_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_observation_query_update", "CREATE TRIGGER trg_survey_observation_query_update AFTER UPDATE ON smart.survey_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.survey_observation_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_observation_query_delete", "CREATE TRIGGER trg_survey_observation_query_delete AFTER DELETE ON smart.survey_observation_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.survey_observation_query', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_survey_summary_query_insert", "CREATE TRIGGER trg_survey_summary_query_insert AFTER INSERT ON smart.survey_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.survey_summary_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_summary_query_update", "CREATE TRIGGER trg_survey_summary_query_update AFTER UPDATE ON smart.survey_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.survey_summary_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_summary_query_delete", "CREATE TRIGGER trg_survey_summary_query_delete AFTER DELETE ON smart.survey_summary_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.survey_summary_query', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_survey_waypoint_query_insert", "CREATE TRIGGER trg_survey_waypoint_query_insert AFTER INSERT ON smart.survey_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.survey_waypoint_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_waypoint_query_update", "CREATE TRIGGER trg_survey_waypoint_query_update AFTER UPDATE ON smart.survey_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.survey_waypoint_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_survey_waypoint_query_delete", "CREATE TRIGGER trg_survey_waypoint_query_delete AFTER DELETE ON smart.survey_waypoint_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.survey_waypoint_query', 'uuid', old.uuid, old.ca_uuid)"},

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
		return ERQueryPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return ERQueryPlugIn.DB_VERSION;
	}
}
