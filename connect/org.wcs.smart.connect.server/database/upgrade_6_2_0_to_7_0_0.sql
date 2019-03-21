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

	



update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';

update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';




update connect.connect_version set version = '7.0.0', last_updated = now();		
update connect.connect_version set filestore_version = '7.0.0';