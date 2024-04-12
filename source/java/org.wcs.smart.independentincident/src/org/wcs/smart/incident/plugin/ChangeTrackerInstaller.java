/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.incident.plugin;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.incident.IncidentPlugIn;

public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller{

	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"smart.trg_incident_waypoint_insert", "CREATE TRIGGER smart.trg_incident_waypoint_insert AFTER INSERT ON smart.incident_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(wp.ca_uuid), smart.uuid(), 'INSERT', 'smart.incident_waypoint', 'wp_uuid', new.wp_uuid, 'patrol_uuid', new.patrol_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid"},
		{"smart.trg_incident_waypoint_update", "CREATE TRIGGER smart.trg_incident_waypoint_update AFTER UPDATE ON smart.incident_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(wp.ca_uuid), smart.uuid(), 'UPDATE', 'smart.incident_waypoint', 'wp_uuid', new.wp_uuid, 'patrol_uuid', new.patrol_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid"},
		{"smart.trg_incident_waypoint_delete", "CREATE TRIGGER smart.trg_incident_waypoint_delete AFTER DELETE ON smart.incident_waypoint REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(wp.ca_uuid), smart.uuid(), 'DELETE', 'smart.incident_waypoint', 'wp_uuid', old.wp_uuid, 'patrol_uuid', old.patrol_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = old.wp_uuid"},
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
		return IncidentPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return IncidentPlugIn.DB_VERSION;
	}

}
