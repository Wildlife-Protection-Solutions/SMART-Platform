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
 		FROM smart.ct_metadata_value iset WHERE iset.uuid = ROW.metadata_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_ct_metadata_value_uuid AFTER INSERT OR UPDATE OR DELETE ON smart.ct_metadata_value_uuid FOR EACH ROW execute procedure connect.trg_ct_metadata_value_uuid();


update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';
update connect.connect_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.cybertracker';

update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';
update connect.ca_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.cybertracker';

update connect.connect_version set version = '7.0.0', last_updated = now();		
update connect.connect_version set filestore_version = '7.0.0';