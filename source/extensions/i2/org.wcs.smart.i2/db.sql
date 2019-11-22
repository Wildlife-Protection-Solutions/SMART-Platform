CREATE TABLE smart.i_profile_config(
  uuid char(16) for bit data not null,
  ca_uuid char(16) for bit data not null,
  keyid varchar(128) not null,
  color int,
  primary key (uuid)
);


CREATE TABLE smart.i_profile_entity_type(
  entity_type_uuid char(16) for bit data not null,
  profile_uuid char(16) for bit data not null,
  primary key (entity_type_uuid, profile_uuid)
);

CREATE TABLE smart.i_profile_record_source(
  record_source_uuid char(16) for bit data not null,
  profile_uuid char(16) for bit data not null,
  primary key (record_source_uuid, profile_uuid)
);


ALTER TABLE smart.i_profile_entity_type ADD CONSTRAINT profileentitytype_entitytypeuuid_fk FOREIGN KEY (entity_type_uuid) REFERENCES smart.i_entity_type (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_profile_entity_type ADD CONSTRAINT profileentitytype_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_profile_record_source ADD CONSTRAINT profilerecordtype_recordsourceuuid_fk FOREIGN KEY (record_source_uuid) REFERENCES smart.I_RECORDSOURCE (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_profile_record_source ADD CONSTRAINT profilerecordtype_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_entity ADD COLUMN profile_uuid char(16) for bit data ;
ALTER TABLE smart.i_entity ALTER COLUMN profile_uuid set not null;
ALTER TABLE smart.i_entity ADD CONSTRAINT i_entity_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;





CREATE TABLE smart.i_permission(
  employee_uuid char(16) for bit data not null,
  profile_uuid char(16) for bit data not null,
  permissions integer not null
);


ALTER TABLE smart.i_permission ADD CONSTRAINT ipermission_empuuid_fk FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_permission ADD CONSTRAINT ipermission_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_record ADD COLUMN profile_uuid char(16) for bit data ;
ALTER TABLE smart.i_record ALTER COLUMN profile_uuid set not null;
ALTER TABLE smart.i_record ADD CONSTRAINT i_record_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;





ALTER TABLE smart.i_relationship_type ADD COLUMN src_profile_uuid char(16) for bit data;
ALTER TABLE smart.i_relationship_type ALTER COLUMN src_profile_uuid SET not null;
ALTER TABLE smart.i_relationship_type ADD COLUMN target_profile_uuid char(16) for bit data;
ALTER TABLE smart.i_relationship_type ALTER COLUMN target_profile_uuid SET not null;


ALTER TABLE smart.i_relationship_type ADD CONSTRAINT i_rtype_srcprofileuuid_fk FOREIGN KEY (src_profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_relationship_type ADD CONSTRAINT i_rtype_trgprofileuuid_fk FOREIGN KEY (target_profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_record_query add column profile_filter varchar(32672);
ALTER TABLE smart.i_entity_summary_query add column profile_filter varchar(32672);
ALTER TABLE smart.i_record_obs_query add column profile_filter varchar(32672);

--must be done last to avoid problems adding
ALTER TABLE smart.i_profile_config ADD CONSTRAINT profile_config_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;  

--entity summary queries
CREATE TABLE smart.i_record_summary_query(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,query_string long varchar,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data, profile_filter varchar(32672), PRIMARY KEY (uuid));

ALTER TABLE smart.i_record_summary_query ADD CONSTRAINT irecordsummquery_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_record_summary_query ADD CONSTRAINT irecordsummquery_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_record_summary_query ADD CONSTRAINT irecordsummquery_modifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE;

--record query			
CREATE TABLE smart.i_record_query(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,query_string long varchar,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data, profile_filter varchar(32672), PRIMARY KEY (uuid));

ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery2_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery2_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery2_modifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE;
		