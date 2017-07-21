ALTER TABLE connect.users add column home_ca_uuid UUID;

ALTER TABLE connect.alert_types ADD COLUMN custom_icon varchar(2);

ALTER TABLE smart.patrol_leg ADD COLUMN mandate_uuid UUID;

UPDATE smart.patrol_leg SET mandate_uuid = (SELECT p.mandate_uuid FROM smart.patrol p WHERE p.uuid = smart.patrol_leg.patrol_uuid);

ALTER TABLE SMART.PATROL_LEG 
ADD CONSTRAINT MANDATE_UUID_FK FOREIGN KEY (MANDATE_UUID) REFERENCES SMART.PATROL_MANDATE(UUID)  
ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.patrol_leg ALTER COLUMN mandate_uuid SET NOT NULL;

ALTER TABLE smart.patrol DROP COLUMN mandate_uuid;

UPDATE connect.connect_plugin_version SET version = '6.0.0' WHERE plugin_id = 'org.wcs.smart';
UPDATE connect.ca_plugin_version SET version = '6.0.0' WHERE plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '6.0.0';


-- QA Plugin

insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.qa', '1.0');
insert into connect.ca_plugin_version (ca_uuid, plugin_id, version) select uuid, 'org.wcs.smart.qa', '1.0' from smart.conservation_area;

CREATE TABLE smart.qa_error( 
  uuid UUID NOT NULL, 
  ca_uuid UUID not null, 
  qa_routine_uuid UUID NOT NULL, 
  data_provider_id varchar(128) not null, 
  status varchar(32) NOT NULL, 
  validate_date timestamp NOT NULL, 
  error_id varchar(1024) NOT NULL, 
  error_description varchar(32600), 
  fix_message varchar(32600), 
  src_identifier UUID NOT NULL, 
  geometry bytea, 
  PRIMARY KEY (uuid)
);

CREATE TABLE smart.qa_routine(
  uuid UUID NOT NULL, 
  ca_uuid UUID NOT NULL, 
  routine_type_id varchar(1024) NOT NULL, 
  description varchar(32600), 
  auto_check boolean DEFAULT false NOT NULL, 
  PRIMARY KEY (uuid)
);

CREATE TABLE smart.qa_routine_parameter( 
  uuid UUID NOT NULL, 
  qa_routine_uuid UUID NOT NULL, 
  id varchar(256) NOT NULL, 
  str_value varchar(32600), 
  byte_value bytea, 
  PRIMARY KEY (uuid, qa_routine_uuid)
);

ALTER TABLE smart.qa_routine ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE smart.qa_error ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE smart.qa_routine_parameter ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE smart.qa_error ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) ON DELETE CASCADE DEFERRABLE;



--NEEDS TO BE FIXED SEE TICKET 2209
delete from smart.CONFIGURABLE_MODEL;

CREATE TABLE smart.cm_attribute_config(uuid UUID not null, cm_uuid UUID not null, dm_attribute_uuid UUID not null, display_mode varchar(10), is_default boolean, primary key (uuid));
ALTER TABLE smart.cm_attribute_config ADD FOREIGN KEY (CM_UUID) REFERENCES SMART.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE smart.cm_attribute_config ADD FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;


alter table smart.cm_attribute add column config_uuid UUID;
ALTER TABLE smart.cm_attribute ADD FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE DEFERRABLE ;

alter table smart.cm_attribute_list add column config_uuid UUID;
ALTER TABLE SMART.CM_ATTRIBUTE_LIST ADD FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE DEFERRABLE ; 

alter table smart.cm_attribute_tree_node add column config_uuid UUID;
ALTER TABLE SMART.CM_ATTRIBUTE_TREE_NODE ADD FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE DEFERRABLE ;

drop table SMART.CM_DM_ATTRIBUTE_SETTINGS;

alter table smart.cm_attribute_list drop column CM_ATTRIBUTE_UUID;
alter table smart.cm_attribute_list drop column DM_ATTRIBUTE_UUID;
alter table smart.cm_attribute_list drop column CM_UUID;
alter table smart.cm_attribute_list alter column config_uuid SET NOT NULL;

alter table smart.cm_attribute_tree_node drop column CM_ATTRIBUTE_UUID;
alter table smart.cm_attribute_tree_node drop column DM_ATTRIBUTE_UUID;
alter table smart.cm_attribute_tree_node drop column CM_UUID;
alter table smart.cm_attribute_tree_node alter column config_uuid SET NOT NULL;

delete from smart.CM_ATTRIBUTE_OPTION where OPTION_ID = 'DISPLAY_MODE' OR OPTION_ID = 'CUSTOM_CONFIG';
---- END OF SECTION