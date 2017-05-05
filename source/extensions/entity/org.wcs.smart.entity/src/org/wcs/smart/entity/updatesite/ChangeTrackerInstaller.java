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
 */package org.wcs.smart.entity.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.entity.EntityPlugIn;
/**
 * Installs triggers for change log tracking of entity plugin tables.
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {

	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"smart.trg_entity_insert", "CREATE TRIGGER smart.trg_entity_insert AFTER INSERT ON smart.entity REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'INSERT', 'smart.entity', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid"},
		{"smart.trg_entity_update", "CREATE TRIGGER smart.trg_entity_update AFTER UPDATE ON smart.entity REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'UPDATE', 'smart.entity', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid"},
		{"smart.trg_entity_delete", "CREATE TRIGGER smart.trg_entity_delete AFTER DELETE ON smart.entity REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'DELETE', 'smart.entity', 'uuid', old.uuid, et.ca_uuid from smart.entity_type et where et.uuid = old.entity_type_uuid"},
		{"smart.trg_entity_attribute_insert", "CREATE TRIGGER smart.trg_entity_attribute_insert AFTER INSERT ON smart.entity_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'INSERT', 'smart.entity_attribute', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid"},
		{"smart.trg_entity_attribute_update", "CREATE TRIGGER smart.trg_entity_attribute_update AFTER UPDATE ON smart.entity_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'UPDATE', 'smart.entity_attribute', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid"},
		{"smart.trg_entity_attribute_delete", "CREATE TRIGGER smart.trg_entity_attribute_delete AFTER DELETE ON smart.entity_attribute REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'DELETE', 'smart.entity_attribute', 'uuid', old.uuid, et.ca_uuid from smart.entity_type et where et.uuid = old.entity_type_uuid"},
		{"smart.trg_entity_attribute_value_insert", "CREATE TRIGGER smart.trg_entity_attribute_value_insert AFTER INSERT ON smart.entity_attribute_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'INSERT', 'smart.entity_attribute_value', 'entity_attribute_uuid', new.entity_attribute_uuid, 'entity_uuid', new.entity_uuid, et.ca_uuid from smart.entity_type et, smart.entity e where e.entity_type_uuid = et.uuid and e.uuid = new.entity_uuid"},
		{"smart.trg_entity_attribute_value_update", "CREATE TRIGGER smart.trg_entity_attribute_value_update AFTER UPDATE ON smart.entity_attribute_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'UPDATE', 'smart.entity_attribute_value', 'entity_attribute_uuid', new.entity_attribute_uuid, 'entity_uuid', new.entity_uuid, et.ca_uuid from smart.entity_type et, smart.entity e where e.entity_type_uuid = et.uuid and e.uuid = new.entity_uuid"},
		{"smart.trg_entity_attribute_value_delete", "CREATE TRIGGER smart.trg_entity_attribute_value_delete AFTER DELETE ON smart.entity_attribute_value REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(et.ca_uuid), smart.uuid(), 'DELETE', 'smart.entity_attribute_value', 'entity_attribute_uuid', old.entity_attribute_uuid, 'entity_uuid', old.entity_uuid, et.ca_uuid from smart.entity_type et, smart.entity e where e.entity_type_uuid = et.uuid and e.uuid = old.entity_uuid"},
		{"smart.trg_entity_type_insert", "CREATE TRIGGER smart.trg_entity_type_insert AFTER INSERT ON smart.entity_type REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.entity_type', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_entity_type_update", "CREATE TRIGGER smart.trg_entity_type_update AFTER UPDATE ON smart.entity_type REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.entity_type', 'uuid', new.uuid, new.ca_uuid)"},
		{"smart.trg_entity_type_delete", "CREATE TRIGGER smart.trg_entity_type_delete AFTER DELETE ON smart.entity_type REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.entity_type', 'uuid', old.uuid, old.ca_uuid)"},
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
		return EntityPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return EntityPlugIn.DB_VERSION;
	}
	
}
