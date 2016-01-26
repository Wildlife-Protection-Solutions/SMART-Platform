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
package org.wcs.smart.plan.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;
import org.wcs.smart.plan.SmartPlanPlugIn;

/**
 * Installs triggers for change tracking the plan plugin.
 * 
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {
	
	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
			{"smart.trg_plan_insert", "CREATE TRIGGER smart.trg_plan_insert AFTER INSERT ON smart.plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.plan', 'uuid', new.uuid, new.ca_uuid)"},
			{"smart.trg_plan_update", "CREATE TRIGGER smart.trg_plan_update AFTER UPDATE ON smart.plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.plan', 'uuid', new.uuid, new.ca_uuid)"},
			{"smart.trg_plan_delete", "CREATE TRIGGER smart.trg_plan_delete AFTER DELETE ON smart.plan REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.plan', 'uuid', old.uuid, old.ca_uuid)"},
			{"smart.trg_plan_target_insert", "CREATE TRIGGER smart.trg_plan_target_insert AFTER INSERT ON smart.plan_target REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.plan_target', 'uuid', new.uuid, p.ca_uuid from smart.plan p where p.uuid = new.plan_uuid"},
			{"smart.trg_plan_target_update", "CREATE TRIGGER smart.trg_plan_target_update AFTER UPDATE ON smart.plan_target REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.plan_target', 'uuid', new.uuid, p.ca_uuid from smart.plan p where p.uuid = new.plan_uuid"},
			{"smart.trg_plan_target_delete", "CREATE TRIGGER smart.trg_plan_target_delete AFTER DELETE ON smart.plan_target REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.plan_target', 'uuid', old.uuid, p.ca_uuid from smart.plan p where p.uuid = old.plan_uuid"},
			{"smart.trg_plan_target_point_insert", "CREATE TRIGGER smart.trg_plan_target_point_insert AFTER INSERT ON smart.plan_target_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.plan_target_point', 'uuid', new.uuid, p.ca_uuid from smart.plan_target pt, smart.plan p where p.uuid = pt.plan_uuid and pt.uuid = new.plan_target_uuid"},
			{"smart.trg_plan_target_point_update", "CREATE TRIGGER smart.trg_plan_target_point_update AFTER UPDATE ON smart.plan_target_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.plan_target_point', 'uuid', new.uuid, p.ca_uuid from smart.plan_target pt, smart.plan p where p.uuid = pt.plan_uuid and pt.uuid = new.plan_target_uuid"},
			{"smart.trg_plan_target_point_delete", "CREATE TRIGGER smart.trg_plan_target_point_delete AFTER DELETE ON smart.plan_target_point REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'DELETE', 'smart.plan_target_point', 'uuid', old.uuid, p.ca_uuid from smart.plan_target pt, smart.plan p where p.uuid = pt.plan_uuid and pt.uuid = old.plan_target_uuid"},
			{"smart.trg_patrol_plan_insert", "CREATE TRIGGER smart.trg_patrol_plan_insert AFTER INSERT ON smart.patrol_plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'INSERT', 'smart.patrol_plan', 'patrol_uuid', new.patrol_uuid, 'plan_uuid', new.plan_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid"},
			{"smart.trg_patrol_plan_update", "CREATE TRIGGER smart.trg_patrol_plan_update AFTER UPDATE ON smart.patrol_plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(p.ca_uuid), smart.uuid(), 'UPDATE', 'smart.patrol_plan', 'patrol_uuid', new.patrol_uuid, 'plan_uuid', new.plan_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid"},
			{"smart.trg_patrol_plan_delete", "CREATE TRIGGER smart.trg_patrol_plan_delete AFTER DELETE ON smart.patrol_plan REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.next_revision_id(p.ca_uuid),  smart.uuid(), 'DELETE', 'smart.patrol_plan', 'patrol_uuid', old.patrol_uuid, 'plan_uuid', old.plan_uuid, p.ca_uuid from smart.patrol p where p.uuid = old.patrol_uuid"},
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
		return SmartPlanPlugIn.PLUGIN_ID;
	}

	@Override
	public String getLastestVersion() {
		return SmartPlanPlugIn.DB_VERSION;
	}
}

