CREATE TABLE smart.e_event_filter(
 uuid char(16) for bit data not null,
 ca_uuid char(16) for bit data not null,
 id varchar(128) not null,
 filter_string varchar(32000) not null,
 PRIMARY KEY(uuid)
);

ALTER TABLE smart.e_event_filter ADD CONSTRAINT eeventfilter_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE;

GRANT SELECT ON smart.e_event_filter TO ANALYST;
GRANT SELECT ON smart.e_event_filter TO DATA_ENTRY;
GRANT ALL PRIVILEGES ON smart.e_event_filter TO MANAGER;

CREATE TRIGGER smart.trg_e_event_filter_insert AFTER INSERT ON smart.e_event_filter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_event_filter', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER smart.trg_e_event_filter_update AFTER UPDATE ON smart.e_event_filter REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_event_filter', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER smart.trg_e_event_filter_delete AFTER DELETE ON smart.e_event_filter REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_event_filter', 'uuid', old.uuid, old.ca_uuid);


CREATE TABLE smart.e_action(
 uuid char(16) for bit data not null,
 ca_uuid char(16) for bit data not null,
 id varchar(128) not null,
 type_key varchar(128) not null,
 PRIMARY KEY (uuid)
);

ALTER TABLE smart.e_action ADD CONSTRAINT eaction_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE;

GRANT SELECT ON smart.e_action TO ANALYST;
GRANT SELECT ON smart.e_action TO DATA_ENTRY;
GRANT ALL PRIVILEGES ON smart.e_action TO MANAGER;

CREATE TRIGGER smart.trg_e_action_insert AFTER INSERT ON smart.e_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_action', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER smart.trg_e_action_update AFTER UPDATE ON smart.e_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(new.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_action', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER smart.trg_e_action_delete AFTER DELETE ON smart.e_action REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.next_revision_id(old.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_action', 'uuid', old.uuid, old.ca_uuid);


CREATE TABLE smart.e_action_parameter_value(
 action_uuid char(16) for bit data not null,
 parameter_key varchar(128)  not null,
 parameter_value varchar(4096) not null,
 PRIMARY KEY (action_uuid, parameter_key)
);

ALTER TABLE smart.e_action_parameter_value ADD CONSTRAINT eactionparametervalue_actionuuid_fk FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) DEFERRABLE INITIALLY IMMEDIATE;

GRANT SELECT ON smart.e_action_parameter_value TO ANALYST;
GRANT SELECT ON smart.e_action_parameter_value TO DATA_ENTRY;
GRANT ALL PRIVILEGES ON smart.e_action_parameter_value TO MANAGER;

CREATE TRIGGER smart.trg_e_action_parameter_value_insert AFTER INSERT ON smart.e_action_parameter_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_action_parameter_value', 'action_uuid', new.action_uuid, 'parameter_key', new.parameter_key, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid;
CREATE TRIGGER smart.trg_e_action_parameter_value_update AFTER UPDATE ON smart.e_action_parameter_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_action_parameter_value', 'action_uuid', new.action_uuid, 'parameter_key', new.parameter_key, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid;
CREATE TRIGGER smart.trg_e_action_parameter_value_delete AFTER DELETE ON smart.e_action_parameter_value REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_action_parameter_value', 'action_uuid', old.action_uuid, 'parameter_key', old.parameter_key, r.ca_uuid from smart.e_action r where r.uuid = old.action_uuid;

CREATE TABLE smart.e_event_action(
 uuid char(16) for bit data not null,
 filter_uuid char(16) for bit data not null,
 action_uuid char(16) for bit data not null,
 PRIMARY KEY (uuid)
);

ALTER TABLE smart.e_event_action ADD CONSTRAINT eeventaction_actionuuid_fk FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.e_event_action ADD CONSTRAINT eeventaction_filteruuid_fk FOREIGN KEY (filter_uuid) REFERENCES smart.e_event_filter(uuid) DEFERRABLE INITIALLY IMMEDIATE;

GRANT SELECT ON smart.e_event_action TO ANALYST;
GRANT SELECT ON smart.e_event_action TO DATA_ENTRY;
GRANT ALL PRIVILEGES ON smart.e_event_action TO MANAGER;

CREATE TRIGGER smart.trg_e_event_action_insert AFTER INSERT ON smart.e_event_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'INSERT', 'smart.e_event_action', 'uuid', new.uuid, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid;
CREATE TRIGGER smart.trg_e_event_action_update AFTER UPDATE ON smart.e_event_action REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'UPDATE', 'smart.e_event_action', 'uuid', new.uuid, r.ca_uuid from smart.e_action r where r.uuid = new.action_uuid;
CREATE TRIGGER smart.trg_e_event_action_delete AFTER DELETE ON smart.e_event_action REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (revision, uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.next_revision_id(r.ca_uuid), smart.uuid(), 'DELETE', 'smart.e_event_action', 'uuid', old.uuid, r.ca_uuid from smart.e_action r where r.uuid = old.action_uuid;