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
package org.wcs.smart.smartcollect.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;

/**
 * Installs triggers for tracking changes in Connect for CyberTracker plugin.
 * 
 * @author elitvin
 */
public class SmartCollectChangeTrackerInstaller extends AbstractChangeTrackerInstaller{

	private String[][] triggers = new String[][]{
		{"smart.trg_smartcollect_package_insert", "CREATE TRIGGER smart.trg_smartcollect_package_insert AFTER INSERT ON smart.smartcollect_package REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values(smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.smartcollect_package', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_smartcollect_package_update", "CREATE TRIGGER smart.trg_smartcollect_package_update AFTER UPDATE ON smart.smartcollect_package REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values(smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.smartcollect_package', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_smartcollect_package_delete", "CREATE TRIGGER smart.trg_smartcollect_package_delete AFTER DELETE ON smart.smartcollect_package REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values(smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.smartcollect_package', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		
		{"smart.trg_smartcollect_waypoint_insert", "CREATE TRIGGER smart.trg_smartcollect_waypoint_insert AFTER INSERT ON smart.smartcollect_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(w.ca_uuid), smart.uuid(), 'INSERT', 'smart.smartcollect_waypoint', 'wp_uuid', new.wp_uuid, w.ca_uuid FROM smart.smartcollect_waypoint l, smart.waypoint w where l.wp_uuid = w.uuid and l.wp_uuid = new.wp_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_smartcollect_waypoint_update", "CREATE TRIGGER smart.trg_smartcollect_waypoint_update AFTER UPDATE ON smart.smartcollect_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(w.ca_uuid), smart.uuid(), 'UPDATE', 'smart.smartcollect_waypoint', 'wp_uuid', new.wp_uuid, w.ca_uuid FROM smart.smartcollect_waypoint l, smart.waypoint w where l.wp_uuid = w.uuid and l.wp_uuid = new.wp_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_smartcollect_waypoint_delete", "CREATE TRIGGER smart.trg_smartcollect_waypoint_delete AFTER DELETE ON smart.smartcollect_waypoint REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(w.ca_uuid), smart.uuid(), 'DELETE', 'smart.smartcollect_waypoint', 'wp_uuid', old.wp_uuid, w.ca_uuid FROM smart.smartcollect_waypoint l, smart.waypoint w where l.wp_uuid = w.uuid and l.wp_uuid = old.wp_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
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
		return SmartCollectPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return SmartCollectPlugIn.DB_VERSION;
	}
}
