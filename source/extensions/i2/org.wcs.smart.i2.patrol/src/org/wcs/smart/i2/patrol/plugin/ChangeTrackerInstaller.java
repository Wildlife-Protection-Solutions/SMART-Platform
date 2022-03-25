/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.i2.patrol.plugin;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.i2.patrol.PatrolProfilePlugIn;

/**
 * Installs triggers for change tracking the plugin.
 * 
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {
	
	private String[][] triggers = new String[][]{
		{"smart.trg_i_patrol_record_motivation_insert", "CREATE TRIGGER smart.trg_i_patrol_record_motivation_insert AFTER INSERT ON smart.i_patrol_record_motivation REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.i_patrol_record_motivation', 'patrol_uuid', new.patrol_uuid, 'i_record_uuid', new.i_record_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_i_patrol_record_motivation_update", "CREATE TRIGGER smart.trg_i_patrol_record_motivation_update AFTER UPDATE ON smart.i_patrol_record_motivation REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.i_patrol_record_motivation', 'patrol_uuid', new.patrol_uuid, 'i_record_uuid', new.i_record_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_i_patrol_record_motivation_delete", "CREATE TRIGGER smart.trg_i_patrol_record_motivation_delete AFTER DELETE ON smart.i_patrol_record_motivation REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.i_patrol_record_motivation', 'patrol_uuid', old.patrol_uuid, 'i_record_uuid', old.i_record_uuid, p.ca_uuid from smart.patrol p where p.uuid = old.patrol_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
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
		return PatrolProfilePlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return PatrolProfilePlugIn.DB_VERSION;
	}
}

