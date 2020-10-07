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
package org.wcs.smart.cybertracker.incident.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.cybertracker.incident.CtIncidentPlugIn;

/**
 * Installs triggers for tracking changes
 * 
 * 
 */
public class CtIncidentChangeTrackerInstaller extends AbstractChangeTrackerInstaller{

	private String[][] triggers = new String[][]{
		{"smart.trg_ct_incident_package_insert", "CREATE TRIGGER smart.trg_ct_incident_package_insert AFTER INSERT ON smart.ct_incident_package REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values(smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.ct_incident_package', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_ct_incident_package_update", "CREATE TRIGGER smart.trg_ct_incident_package_update AFTER UPDATE ON smart.ct_incident_package REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values(smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.ct_incident_package', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_ct_incident_package_delete", "CREATE TRIGGER smart.trg_ct_incident_package_delete AFTER DELETE ON smart.ct_incident_package REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values(smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.ct_incident_package', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		
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
		return CtIncidentPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return CtIncidentPlugIn.DB_VERSION;
	}
}
