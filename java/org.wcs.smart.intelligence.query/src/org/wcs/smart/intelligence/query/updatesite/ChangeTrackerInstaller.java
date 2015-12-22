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
package org.wcs.smart.intelligence.query.updatesite;

import org.wcs.smart.changetracking.AbstractChangeTrackerInstaller;

/**
 * Installs triggers to support change log tracking for intelligence query plugin.
 * @author Emily
 *
 */
public class ChangeTrackerInstaller extends AbstractChangeTrackerInstaller {

	@SuppressWarnings("nls")
	private String[][] triggers = new String[][]{
		{"trg_intel_record_query_insert", "CREATE TRIGGER trg_intel_record_query_insert AFTER INSERT ON smart.intel_record_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.intel_record_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intel_record_query_update", "CREATE TRIGGER trg_intel_record_query_update AFTER UPDATE ON smart.intel_record_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.intel_record_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intel_record_query_delete", "CREATE TRIGGER trg_intel_record_query_delete AFTER DELETE ON smart.intel_record_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.intel_record_query', 'uuid', old.uuid, old.ca_uuid)"},
		{"trg_intel_summary_query_insert", "CREATE TRIGGER trg_intel_summary_query_insert AFTER INSERT ON smart.intel_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.intel_summary_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intel_summary_query_update", "CREATE TRIGGER trg_intel_summary_query_update AFTER UPDATE ON smart.intel_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.intel_summary_query', 'uuid', new.uuid, new.ca_uuid)"},
		{"trg_intel_summary_query_delete", "CREATE TRIGGER trg_intel_summary_query_delete AFTER DELETE ON smart.intel_summary_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.intel_summary_query', 'uuid', old.uuid, old.ca_uuid)"},
	};
	
	
	@Override
	public String[][] getTriggers() {
		return triggers;
	}

}
