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
 * Installs triggers for change tracking the PAWS plugin.
 * 
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {
	
	private String[][] triggers = new String[][]{
		{"smart.trg_paws_configuration_insert", "CREATE TRIGGER smart.trg_paws_configuration_insert AFTER INSERT ON smart.paws_configuration REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.paws_configuration', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_configuration_update", "CREATE TRIGGER smart.trg_paws_configuration_update AFTER UPDATE ON smart.paws_configuration REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.paws_configuration', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_configuration_delete", "CREATE TRIGGER smart.trg_paws_configuration_delete AFTER DELETE ON smart.paws_configuration REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.paws_configuration', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		
		{"smart.trg_paws_run_insert", "CREATE TRIGGER smart.trg_paws_run_insert AFTER INSERT ON smart.paws_run REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.paws_run', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_run_update", "CREATE TRIGGER smart.trg_paws_run_update AFTER UPDATE ON smart.paws_run REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.paws_run', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_run_delete", "CREATE TRIGGER smart.trg_paws_run_delete AFTER DELETE ON smart.paws_run REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.paws_run', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		
		{"smart.trg_paws_service_insert", "CREATE TRIGGER smart.trg_paws_service_insert AFTER INSERT ON smart.paws_service REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.paws_service', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_service_update", "CREATE TRIGGER smart.trg_paws_service_update AFTER UPDATE ON smart.paws_service REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.paws_service', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_service_delete", "CREATE TRIGGER smart.trg_paws_service_delete AFTER DELETE ON smart.paws_service REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.paws_service', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		
		{"smart.trg_paws_workspace_insert", "CREATE TRIGGER smart.trg_paws_workspace_insert AFTER INSERT ON smart.paws_workspace REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.paws_workspace', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_workspace_update", "CREATE TRIGGER smart.trg_paws_workspace_update AFTER UPDATE ON smart.paws_workspace REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.paws_workspace', 'uuid', new.uuid, new.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_workspace_delete", "CREATE TRIGGER smart.trg_paws_workspace_delete AFTER DELETE ON smart.paws_workspace REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.paws_workspace', 'uuid', old.uuid, old.ca_uuid)"}, //$NON-NLS-1$ //$NON-NLS-2$
		
		{"smart.trg_paws_parameter_insert", "CREATE TRIGGER smart.trg_paws_parameter_insert AFTER INSERT ON smart.paws_parameter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.paws_parameter', 'uuid', new.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE new.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_parameter_update", "CREATE TRIGGER smart.trg_paws_parameter_update AFTER UPDATE ON smart.paws_parameter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.paws_parameter', 'uuid', new.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE new.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_parameter_delete", "CREATE TRIGGER smart.trg_paws_parameter_delete AFTER DELETE ON smart.paws_parameter REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.paws_parameter', 'uuid', old.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE old.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
	
		{"smart.trg_paws_simple_class_insert", "CREATE TRIGGER smart.trg_paws_simple_class_insert AFTER INSERT ON smart.paws_simple_class REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.paws_simple_class', 'uuid', new.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE new.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_simple_class_update", "CREATE TRIGGER smart.trg_paws_simple_class_update AFTER UPDATE ON smart.paws_simple_class REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.paws_simple_class', 'uuid', new.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE new.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_simple_class_delete", "CREATE TRIGGER smart.trg_paws_simple_class_delete AFTER DELETE ON smart.paws_simple_class REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.paws_simple_class', 'uuid', old.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE old.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		
		{"smart.trg_paws_query_class_insert", "CREATE TRIGGER smart.trg_paws_query_class_insert AFTER INSERT ON smart.paws_query_class REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.paws_query_class', 'uuid', new.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE new.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_query_class_update", "CREATE TRIGGER smart.trg_paws_query_class_update AFTER UPDATE ON smart.paws_query_class REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.paws_query_class', 'uuid', new.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE new.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"smart.trg_paws_query_class_delete", "CREATE TRIGGER smart.trg_paws_query_class_delete AFTER DELETE ON smart.paws_query_class REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid)  SELECT smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.paws_query_class', 'uuid', old.uuid, p.ca_uuid FROM smart.paws_configuration p WHERE old.config_uuid = p.uuid"}, //$NON-NLS-1$ //$NON-NLS-2$
		
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

