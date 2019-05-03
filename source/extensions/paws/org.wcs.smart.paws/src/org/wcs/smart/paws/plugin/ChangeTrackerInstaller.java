/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.plugin;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.paws.PawsPlugIn;

/**
 * Installs triggers for change tracking the plan plugin.
 * 
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {
	
	private String[][] triggers = new String[][]{
		//TODO:
//		{"smart.trg_r_script_insert", "CREATE TRIGGER smart.trg_r_script_insert AFTER INSERT ON smart.r_script REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.r_script', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
//		{"smart.trg_r_script_update", "CREATE TRIGGER smart.trg_r_script_update AFTER UPDATE ON smart.r_script REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.r_script', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
//		{"smart.trg_r_script_delete", "CREATE TRIGGER smart.trg_r_script_delete AFTER DELETE ON smart.r_script REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.r_script', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
//
//		{"smart.trg_r_query_insert", "CREATE TRIGGER smart.trg_r_query_insert AFTER INSERT ON smart.r_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.r_query', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
//		{"smart.trg_r_query_update", "CREATE TRIGGER smart.trg_r_query_update AFTER UPDATE ON smart.r_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.r_query', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
//		{"smart.trg_r_query_delete", "CREATE TRIGGER smart.trg_r_query_delete AFTER DELETE ON smart.r_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.r_query', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
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
		return PawsPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return PawsPlugIn.DB_VERSION;
	}
}

