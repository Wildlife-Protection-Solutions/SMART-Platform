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
package org.wcs.smart.connect.dataqueue.cybertracker.survey.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.connect.dataqueue.cybertracker.survey.PlugIn;

/**
 * Installs triggers for tracking changes in Connect for CyberTracker plugin.
 * 
 * @author egouge
 */
public class DataQueueCtMissionChangeTrackerInstaller extends AbstractChangeTrackerInstaller{

	private String[][] triggers = new String[][]{
		{"smart.trg_ct_mission_link_insert", "CREATE TRIGGER smart.trg_ct_mission_link_insert AFTER INSERT ON smart.ct_mission_link REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(pp.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_mission_link', 'ct_uuid', new.ct_uuid, mm.ca_uuid FROM smart.mission mm where mm.uuid = new.mission_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_ct_mission_link_update", "CREATE TRIGGER smart.trg_ct_mission_link_update AFTER UPDATE ON smart.ct_mission_link REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(pp.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_mission_link', 'ct_uuid', new.ct_uuid, mm.ca_uuid FROM smart.mission mm where mm.uuid = new.mission_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_ct_mission_link_delete", "CREATE TRIGGER smart.trg_ct_mission_link_delete AFTER DELETE ON smart.ct_mission_link REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(pp.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_mission_link', 'ct_uuid', old.ct_uuid, mm.ca_uuid FROM smart.mission mm where mm.uuid = old.mission_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
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
		return PlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return PlugIn.DB_VERSION;
	}
}
