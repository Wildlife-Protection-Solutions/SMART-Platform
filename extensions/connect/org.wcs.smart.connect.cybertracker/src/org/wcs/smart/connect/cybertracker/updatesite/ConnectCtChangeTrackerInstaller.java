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
package org.wcs.smart.connect.cybertracker.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.connect.cybertracker.ConnectCtPlugIn;

/**
 * Installs triggers for tracking changes in Connect for CyberTracker plugin.
 * 
 * @author elitvin
 */
public class ConnectCtChangeTrackerInstaller extends AbstractChangeTrackerInstaller{

	private String[][] triggers = new String[][]{
		{"smart.trg_connect_alert_insert", "CREATE TRIGGER smart.trg_connect_alert_insert AFTER INSERT ON smart.connect_alert REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(cm.ca_uuid), smart.uuid(), 'INSERT', 'smart.connect_alert', 'uuid', new.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = new.cm_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_connect_alert_update", "CREATE TRIGGER smart.trg_connect_alert_update AFTER UPDATE ON smart.connect_alert REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(cm.ca_uuid), smart.uuid(), 'UPDATE', 'smart.connect_alert', 'uuid', new.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = new.cm_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_connect_alert_delete", "CREATE TRIGGER smart.trg_connect_alert_delete AFTER DELETE ON smart.connect_alert REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(cm.ca_uuid), smart.uuid(), 'DELETE', 'smart.connect_alert', 'uuid', old.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = old.cm_uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
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
		return ConnectCtPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return ConnectCtPlugIn.DB_VERSION;
	}
}
