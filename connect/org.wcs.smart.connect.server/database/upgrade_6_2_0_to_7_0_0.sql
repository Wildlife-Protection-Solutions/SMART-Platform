create table connect.ct_package(
  uuid UUID not null,
  package_uuid UUID not null,
  ca_uuid UUID not null,
  uploaded_date timestamp not null,
  version varchar(256) not null,
  filename varchar(256) not null,
  name varchar(256) not null,
  status varchar(16) not null,
  work_item_uuid uuid,
  UNIQUE(package_uuid),
  primary key(uuid)
);

ALTER TABLE connect.ct_package ADD FOREIGN KEY (ca_uuid) REFERENCES connect.ca_info(ca_uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;


create table smart.ct_patrol_package(
 uuid uuid not null,
 name varchar(512),
 ca_uuid uuid not null,
 cm_uuid uuid,
 ctprofile_uuid uuid,
 has_incident boolean default false,
 incident_uuid uuid,
 basemapdef varchar(32672),
 primary key (uuid)
);

ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (cm_uuid) REFERENCES smart.configurable_model(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (incident_uuid) REFERENCES smart.configurable_model(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_ct_patrol_package AFTER INSERT OR UPDATE OR DELETE ON smart.ct_patrol_package FOR EACH ROW execute procedure connect.trg_changelog_common();

create table smart.ct_survey_package(
 uuid uuid not null,
 name varchar(512),
 ca_uuid uuid not null,
 sd_uuid uuid,
 ctprofile_uuid uuid,
 has_incident boolean default false,
 incident_uuid uuid,
 basemapdef varchar(32672),
 primary key (uuid)
);

ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (sd_uuid) REFERENCES smart.survey_design(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (incident_uuid) REFERENCES smart.configurable_model(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_ct_survey_package AFTER INSERT OR UPDATE OR DELETE ON smart.ct_survey_package FOR EACH ROW execute procedure connect.trg_changelog_common();


alter table connect.work_item drop constraint type_chk;
ALTER TABLE connect.work_item ADD CONSTRAINT type_chk 
	CHECK (type IN ('UP_CA', 'UP_SYNC', 'DOWN_CA', 'DOWN_SYNC', 'UP_DATAQUEUE', 'UP_CTPACKAGE'));

	
CREATE TABLE smart.ct_metadata_value(uuid uuid not null, ca_uuid uuid not null, package_uuid uuid not null, keyid varchar(32) not null, is_visible boolean not null, string_value varchar(8192), boolean_value boolean, uuid_value uuid, primary key (uuid));
CREATE TABLE smart.ct_metadata_value_uuid (uuid uuid NOT NULL, field_uuid uuid NOT NULL, uuid_value uuid NOT NULL, PRIMARY KEY (UUID));

ALTER TABLE smart.ct_metadata_value ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_metadata_value_uuid ADD FOREIGN KEY (field_uuid) REFERENCES smart.ct_metadata_value(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_ct_metadata_value AFTER INSERT OR UPDATE OR DELETE ON smart.ct_metadata_value FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.trg_ct_metadata_value_uuid() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, iset.CA_UUID 
 		FROM smart.ct_metadata_value iset WHERE iset.uuid = ROW.field_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_ct_metadata_value_uuid AFTER INSERT OR UPDATE OR DELETE ON smart.ct_metadata_value_uuid FOR EACH ROW execute procedure connect.trg_ct_metadata_value_uuid();


CREATE TABLE connect.ct_api_key(ca_uuid uuid not null, api_key varchar(64) not null, primary key (ca_uuid), unique(api_key));
ALTER TABLE connect.ct_api_key ADD FOREIGN KEY (ca_uuid) REFERENCES connect.ca_info(ca_uuid) on DELETE CASCADE on UPDATE RESTRICT; 

ALTER TABLE connect.alerts ADD COLUMN source VARCHAR(32) not null default 'USER';
ALTER TABLE connect.alerts ALTER COLUMN creator_uuid DROP not null;

------------ EMPLOYEE TEAMS ----------------
CREATE TABLE smart.employee_team (uuid uuid not null, ca_uuid uuid not null, primary key (uuid));
CREATE TABLE smart.employee_team_member (employee_uuid uuid not null, team_uuid uuid not null, primary key(employee_uuid, team_uuid));
	
ALTER TABLE smart.employee_team ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.employee_team_member ADD FOREIGN KEY (team_uuid) REFERENCES smart.employee_team(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.employee_team_member ADD FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_employee_team AFTER INSERT OR UPDATE OR DELETE ON smart.employee_team FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.trg_employee_team_member() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'team_uuid', ROW.team_uuid, 'employee_uuid', ROW.employee_uuid, null, t.CA_UUID 
 		FROM smart.employee_team t WHERE t.uuid = ROW.team_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_employee_team_member AFTER INSERT OR UPDATE OR DELETE ON smart.employee_team_member FOR EACH ROW execute procedure connect.trg_employee_team_member();

------- PATROL ATTRIBUTES -------------
CREATE TABLE SMART.PATROL_ATTRIBUTE(
  UUID UUID NOT NULL,  
  CA_UUID UUID NOT NULL,  
  KEYID VARCHAR(128) NOT NULL,  
  ATT_TYPE VARCHAR(7) NOT NULL,  
  IS_ACTIVE BOOLEAN NOT NULL,  
  PRIMARY KEY (UUID));
  
CREATE TABLE SMART.PATROL_ATTRIBUTE_LIST(
  UUID UUID NOT NULL,
  PATROL_ATTRIBUTE_UUID UUID NOT NULL,  
  KEYID VARCHAR(128) NOT NULL,  LIST_ORDER SMALLINT NOT NULL,  
  IS_ACTIVE BOOLEAN NOT NULL,  
  PRIMARY KEY (UUID));
  
CREATE TABLE SMART.PATROL_ATTRIBUTE_VALUE (
  PATROL_UUID UUID NOT NULL,
  PATROL_ATTRIBUTE_UUID UUID NOT NULL, 
  STRING_VALUE VARCHAR(1024), 
  NUMBER_VALUE DOUBLE PRECISION, 
  LIST_ITEM_UUID UUID, 
  PRIMARY KEY (PATROL_UUID, PATROL_ATTRIBUTE_UUID));
				
ALTER TABLE SMART.PATROL_ATTRIBUTE_LIST ADD FOREIGN KEY (PATROL_ATTRIBUTE_UUID) REFERENCES SMART.PATROL_ATTRIBUTE (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE ADD FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD FOREIGN KEY (PATROL_ATTRIBUTE_UUID) REFERENCES SMART.PATROL_ATTRIBUTE(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD FOREIGN KEY (LIST_ITEM_UUID) REFERENCES SMART.PATROL_ATTRIBUTE_LIST(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
				
CREATE TRIGGER trg_patrol_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.trg_patrol_attribute_list() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, t.CA_UUID 
 		FROM smart.patrol_attribute t WHERE t.uuid = ROW.patrol_attribute_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_patrol_attribute_list AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute_list FOR EACH ROW execute procedure connect.trg_patrol_attribute_list();

CREATE OR REPLACE FUNCTION connect.trg_patrol_attribute_value() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'patrol_uuid', ROW.patrol_uuid, 'patrol_attribute_uuid', ROW.patrol_attribute_uuid, null, t.CA_UUID 
 		FROM smart.patrol_attribute t WHERE t.uuid = ROW.patrol_attribute_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_patrol_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute_value FOR EACH ROW execute procedure connect.trg_patrol_attribute_value();


------------ PAWS ----------------
CREATE TABLE smart.paws_configuration(uuid uuid NOT NULL, ca_uuid uuid NOT NULL, name varchar(8192) NOT NULL, PRIMARY KEY (uuid));
CREATE TABLE smart.paws_parameter( uuid uuid NOT NULL, config_uuid uuid NOT NULL, keyid varchar(8192) NOT NULL, value varchar(8192), PRIMARY KEY (uuid));
CREATE TABLE smart.paws_query_class(uuid uuid NOT NULL, config_uuid uuid NOT NULL, query_uuid uuid NOT NULL, query_type varchar(32) NOT NULL, date_range varchar(512), classification varchar(512) NOT NULL, PRIMARY KEY (uuid));
CREATE TABLE smart.paws_run(uuid uuid NOT NULL, ca_uuid uuid NOT NULL, config_uuid uuid, id varchar(256) NOT NULL, server_run_id varchar(256), run_date timestamp, package_file varchar(256), result_location varchar(256), status varchar(32) NOT NULL, status_message varchar(8192), data_start_date date, data_end_date date, train_start_year smallint, train_end_year smallint, test_start_year smallint, test_end_year smallint, forecast_start_year smallint, forecast_end_year smallint, paws_task_id varchar(8192), PRIMARY KEY (uuid));
CREATE TABLE smart.paws_service(uuid uuid NOT NULL, ca_uuid uuid NOT NULL UNIQUE, url varchar(8192), api_key varchar(8192), PRIMARY KEY (uuid));
CREATE TABLE smart.paws_simple_class(uuid uuid NOT NULL, config_uuid uuid NOT NULL, classification varchar(512) NOT NULL, date_range varchar(512), category_hkey varchar(32672) NOT NULL, attribute_key varchar(128), list_key varchar(128), tree_hkey varchar(32672), PRIMARY KEY (uuid));
CREATE TABLE smart.paws_workspace(uuid uuid NOT NULL, ca_uuid uuid NOT NULL UNIQUE, url varchar(8192), client_id varchar(8192), blob_url varchar(8192), PRIMARY KEY (uuid));
				
ALTER TABLE smart.paws_configuration ADD FOREIGN KEY(ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_run ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_service ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_workspace ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_parameter ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_query_class ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_simple_class ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_run ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


------------ VERSIONS ------------
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';
update connect.connect_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.cybertracker';
insert into connect.connect_plugin_version (version, plugin_id) values ('1.0', 'org.wcs.smart.paws');
update connect.connect_plugin_version set version = '7.0.0' where plugin_id = 'org.wcs.smart';

update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';
update connect.ca_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.cybertracker';
update connect.ca_plugin_version set version = '7.0.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '7.0.0', last_updated = now();		
update connect.connect_version set filestore_version = '7.0.0';