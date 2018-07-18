ALTER TABLE connect.users add column home_ca_uuid UUID;

ALTER TABLE connect.alert_types ADD COLUMN custom_icon varchar(2);

ALTER TABLE smart.patrol_leg ADD COLUMN mandate_uuid UUID;

UPDATE smart.patrol_leg SET mandate_uuid = (SELECT p.mandate_uuid FROM smart.patrol p WHERE p.uuid = smart.patrol_leg.patrol_uuid);

ALTER TABLE SMART.PATROL_LEG 
ADD CONSTRAINT MANDATE_UUID_FK FOREIGN KEY (MANDATE_UUID) REFERENCES SMART.PATROL_MANDATE(UUID)  
ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE smart.patrol DROP COLUMN mandate_uuid;

ALTER TABLE connect.data_queue DROP CONSTRAINT type_chk;


CREATE OR REPLACE FUNCTION smart.hkeylength(hkey varchar) RETURNS integer AS $$
BEGIN
	RETURN length(hkey) - length(replace(hkey, '.', '')) - 1;

END;
$$LANGUAGE plpgsql;

ALTER TABLE connect.users ALTER COLUMN username TYPE varchar(256);

--changes to map layers table and removing all non-wms map layers
delete from connect.map_layers where layer_type != 3;
alter table connect.map_layers add column layer_type_tmp varchar(16);
update connect.map_layers set layer_type_tmp = 'WMS';
alter table connect.map_layers alter column layer_type_tmp set not null;
alter table connect.map_layers drop column layer_type;
alter table connect.map_layers rename column layer_type_tmp to layer_type;
alter table connect.map_layers add constraint type_chk check (layer_type in ('WMS'));
alter table connect.map_layers add primary key (uuid);
alter table connect.map_layers drop column mapboxid;

--unique user id constraint
ALTER TABLE smart.employee ADD CONSTRAINT smartuseridunq UNIQUE(ca_uuid, smartuserid);
 
--agency key ids
ALTER table smart.agency add column keyid varchar(128);
 
UPDATE smart.agency SET keyId = lower(regexp_replace(a.value, '[^a-zA-Z0-9]', '', 'g')) 
from smart.i18n_label a, smart.language b 
where b.uuid = a.language_uuid and a.element_uuid = smart.agency.uuid and b.isdefault;

UPDATE smart.agency SET keyId = cast(uuid as varchar) where keyId is null or trim(keyId) = '';
 
--ensure unique keys by using uuids
 update smart.agency
 set keyId = keyId || replace(cast(uuid as varchar), '-', '')  WHERE uuid IN (
 select uuid from smart.agency a, 
 (select ca_uuid, keyid from smart.agency group by ca_uuid, keyid having count(*) > 1) b
 WHERE a.ca_uuid = b.ca_uuid and a.keyid = b.keyid
 );

ALTER TABLE smart.agency ADD CONSTRAINT keyunq UNIQUE (keyid, ca_uuid);
 

-- Update to intelligence/profiles plugin
CREATE TABLE smart.i_entity_summary_query(
  uuid uuid NOT NULL,
  ca_uuid uuid NOT NULL,
  query_string varchar,
  date_created timestamp NOT NULL
  ,last_modified_date timestamp,
  created_by uuid NOT NULL,
  last_modified_by uuid,
  PRIMARY KEY (uuid));
  
  
ALTER TABLE smart.i_entity_summary_query ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_entity_summary_query ADD FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_entity_summary_query ADD FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE smart.i_entity_record_query(
  uuid uuid NOT NULL,
  ca_uuid uuid NOT NULL,
  query_string varchar,
  date_created timestamp NOT NULL
  ,last_modified_date timestamp,
  created_by uuid NOT NULL,
  last_modified_by uuid,
  PRIMARY KEY (uuid));
  
  
ALTER TABLE smart.i_entity_record_query ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_entity_record_query ADD FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_entity_record_query ADD FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

alter table smart.i_working_set_query drop constraint iworkingsetquery_query_fk;
ALTER TABLE smart.I_WORKING_SET_QUERY add column query_type varchar(32);
UPDATE smart.i_working_set_query set query_type = 'I2_OBS_QUERY';
ALTER TABLE smart.i_working_set_query alter column query_type set not null;


CREATE TABLE smart.i_diagram_style (
  uuid UUID NOT NULL, 
  ca_uuid UUID NOT NULL, 
  IS_DEFAULT BOOLEAN, 
  OPTIONS VARCHAR(2048), 
  PRIMARY KEY (UUID)
);

ALTER TABLE smart.i_diagram_style ADD FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
		

CREATE TABLE smart.i_diagram_entity_type_style (
  uuid UUID NOT NULL, 
  style_uuid UUID NOT NULL, 
  entity_type_uuid UUID NOT NULL, 
  OPTIONS VARCHAR(1024), 
  PRIMARY KEY (UUID)
);
ALTER TABLE smart.i_diagram_entity_type_style ADD FOREIGN KEY (STYLE_UUID) REFERENCES SMART.I_DIAGRAM_STYLE(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_diagram_entity_type_style ADD FOREIGN KEY (ENTITY_TYPE_UUID) REFERENCES SMART.I_ENTITY_TYPE(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
			
CREATE TABLE smart.i_diagram_relationship_type_style (
  uuid UUID NOT NULL, 
  style_uuid UUID NOT NULL, 
  relationship_type_uuid UUID NOT NULL, 
  OPTIONS VARCHAR(1024), 
  PRIMARY KEY (UUID)
);
			
ALTER TABLE smart.i_diagram_relationship_type_style ADD FOREIGN KEY (STYLE_UUID) REFERENCES SMART.I_DIAGRAM_STYLE(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_diagram_relationship_type_style ADD FOREIGN KEY (RELATIONSHIP_TYPE_UUID) REFERENCES SMART.I_RELATIONSHIP_TYPE(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


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

ALTER TABLE smart.qa_routine ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.qa_error ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.qa_routine_parameter ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.qa_error ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;



-- UPGRADE CONFIGURABLE MODEL CONFIGURATIONS
-- SEE UpgradeServlet.java for full Configurable Model update code
--create new tables
CREATE TABLE smart.cm_attribute_config(uuid UUID not null, cm_uuid UUID not null, dm_attribute_uuid UUID not null, display_mode varchar(10), is_default boolean, primary key (uuid));
ALTER TABLE smart.cm_attribute_config ADD FOREIGN KEY (CM_UUID) REFERENCES SMART.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.cm_attribute_config ADD FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

alter table smart.cm_attribute add column config_uuid UUID;
ALTER TABLE smart.cm_attribute ADD FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;
alter table smart.cm_attribute_list add column config_uuid UUID;
ALTER TABLE SMART.CM_ATTRIBUTE_LIST ADD FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ; 
alter table smart.cm_attribute_tree_node add column config_uuid UUID;
ALTER TABLE SMART.CM_ATTRIBUTE_TREE_NODE ADD FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;

-- the following run in the upgradeservlet code
--drop table SMART.CM_DM_ATTRIBUTE_SETTINGS;
--alter table smart.cm_attribute_list drop column CM_ATTRIBUTE_UUID;
--alter table smart.cm_attribute_list drop column DM_ATTRIBUTE_UUID;
--alter table smart.cm_attribute_list drop column CM_UUID;
--alter table smart.cm_attribute_list alter column config_uuid SET NOT NULL;

--alter table smart.cm_attribute_tree_node drop column CM_ATTRIBUTE_UUID;
--alter table smart.cm_attribute_tree_node drop column DM_ATTRIBUTE_UUID;
--alter table smart.cm_attribute_tree_node drop column CM_UUID;
--alter table smart.cm_attribute_tree_node alter column config_uuid SET NOT NULL;

--delete from smart.CM_ATTRIBUTE_OPTION where OPTION_ID = 'DISPLAY_MODE' OR OPTION_ID = 'CUSTOM_CONFIG';
---- END OF SECTION


--i2 UPDATES
alter table smart.i_record ADD COLUMN primary_date timestamp;
update smart.i_record set primary_date = (select a.maxdatetime from (select record_uuid, max(datetime) as maxdatetime from smart.I_LOCATION group by record_uuid) a where a.record_uuid = smart.i_record.uuid );
update smart.i_record set primary_date = date_created where primary_date is null;
alter table smart.i_record ALTER COLUMN primary_date SET NOT NULL;

UPDATE connect.connect_plugin_version SET version = '2.0' WHERE plugin_id = 'org.wcs.smart.i2';
UPDATE connect.ca_plugin_version SET version = '2.0' WHERE plugin_id = 'org.wcs.smart.i2';


CREATE OR REPLACE FUNCTION smart.metaphoneContains(metaphone varchar(4), searchstring varchar) RETURNS boolean AS $$
DECLARE
	part varchar;
BEGIN
	IF (metaphone IS NULL OR searchstring IS NULL) THEN RETURN false; END IF;
	FOREACH PART IN ARRAY string_to_array(searchstring, ' ')
	LOOP
    		IF (metaphone = part) THEN RETURN TRUE; END IF;
	END LOOP;
	RETURN FALSE;
END;
$$LANGUAGE plpgsql;

create table smart.i_config_option (
  uuid uuid, 
  ca_uuid uuid not null,
  keyid varchar(32000) not null, 
  value varchar(32000), 
  unique(ca_uuid, keyid),
  primary key (uuid));

ALTER TABLE SMART.i_config_option ADD FOREIGN KEY (ca_uuid) REFERENCES SMART.conservation_area(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;

 ALTER TABLE smart.i_attribute_list_item add column list_order integer not null default 0;
 
-- EVENTS
CREATE TABLE smart.e_event_filter(
  uuid UUID not null, 
  ca_uuid UUID not null, 
  id varchar(128) not null, 
  filter_string varchar(32000) not null, 
PRIMARY KEY(uuid));

ALTER TABLE smart.e_event_filter ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;

CREATE TABLE smart.e_action( 
  uuid UUID not null, 
  ca_uuid UUID not null, 
  id varchar(128) not null, 
  type_key varchar(128) not null, 
  PRIMARY KEY (uuid));

ALTER TABLE smart.e_action ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;

CREATE TABLE smart.e_action_parameter_value( 
  action_uuid UUID not null, 
  parameter_key varchar(128)  not null, 
  parameter_value varchar(4096) not null, 
  PRIMARY KEY (action_uuid, parameter_key));
  
ALTER TABLE smart.e_action_parameter_value ADD FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;


CREATE TABLE smart.e_event_action(
  uuid UUID not null, 
  filter_uuid UUID not null, 
  action_uuid UUID not null, 
  is_enabled boolean not null default true, 
  PRIMARY KEY (uuid) );
  
ALTER TABLE smart.e_event_action ADD FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;
ALTER TABLE smart.e_event_action ADD FOREIGN KEY (filter_uuid) REFERENCES smart.e_event_filter(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;

INSERT INTO connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.event', '1.0');



 -- TRIGGERS FOR CHANGELOG

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TRIGGER IF EXISTS trg_query_folder ON smart.query_folder;                                                                                  
DROP TRIGGER IF EXISTS trg_report ON smart.report;                                                                                              
DROP TRIGGER IF EXISTS trg_report_folder ON smart.report_folder;                                                                                
DROP TRIGGER IF EXISTS trg_saved_maps ON smart.saved_maps;                                                                                      
DROP TRIGGER IF EXISTS trg_station ON smart.station;                                                                                            
DROP TRIGGER IF EXISTS trg_summary_query ON smart.summary_query;                                                                                
DROP TRIGGER IF EXISTS trg_team ON smart.team;                                                                                                  
DROP TRIGGER IF EXISTS trg_waypoint ON smart.waypoint;                                                                                          
DROP TRIGGER IF EXISTS trg_waypoint_query ON smart.waypoint_query;                                                                              
DROP TRIGGER IF EXISTS trg_configurable_model ON smart.configurable_model;                                                                      
DROP TRIGGER IF EXISTS trg_screen_option ON smart.screen_option;                                                                                
DROP TRIGGER IF EXISTS trg_compound_query ON smart.compound_query;                                                                              
DROP TRIGGER IF EXISTS trg_agency ON smart.agency;                                                                                              
DROP TRIGGER IF EXISTS trg_area_geometries ON smart.area_geometries;                                                                            
DROP TRIGGER IF EXISTS trg_ca_projection ON smart.ca_projection;                                                                                
DROP TRIGGER IF EXISTS trg_conservation_area ON smart.conservation_area;                                                                        
DROP TRIGGER IF EXISTS trg_dm_attribute ON smart.dm_attribute;                                                                                  
DROP TRIGGER IF EXISTS trg_dm_category ON smart.dm_category;                                                                                    
DROP TRIGGER IF EXISTS trg_employee ON smart.employee;                                                                                          
DROP TRIGGER IF EXISTS trg_gridded_query ON smart.gridded_query;                                                                                
DROP TRIGGER IF EXISTS trg_language ON smart.language;                                                                                          
DROP TRIGGER IF EXISTS trg_map_styles ON smart.map_styles;                                                                                      
DROP TRIGGER IF EXISTS trg_observation_options ON smart.observation_options;                                                                    
DROP TRIGGER IF EXISTS trg_observation_query ON smart.observation_query;                                                                        
DROP TRIGGER IF EXISTS trg_obs_gridded_query ON smart.obs_gridded_query;                                                                            
DROP TRIGGER IF EXISTS trg_obs_observation_query ON smart.obs_observation_query;                                                                
DROP TRIGGER IF EXISTS trg_obs_summary_query ON smart.obs_summary_query;                                                                        
DROP TRIGGER IF EXISTS trg_obs_waypoint_query ON smart.obs_waypoint_query;                                                                      
DROP TRIGGER IF EXISTS trg_patrol ON smart.patrol;                                                                                              
DROP TRIGGER IF EXISTS trg_patrol_mandate ON smart.patrol_mandate;                                                                              
DROP TRIGGER IF EXISTS trg_patrol_query ON smart.patrol_query;                                                                                  
DROP TRIGGER IF EXISTS trg_patrol_transport ON smart.patrol_transport;                                                                          
DROP TRIGGER IF EXISTS trg_patrol_type ON smart.patrol_type;                                                                                    
DROP TRIGGER IF EXISTS trg_dm_attribute_list ON smart.dm_attribute_list;                                                                        
DROP TRIGGER IF EXISTS trg_dm_attribute_tree ON smart.dm_attribute_tree;                                                                        
DROP TRIGGER IF EXISTS trg_dm_att_agg_map ON smart.dm_att_agg_map;                                                                              
DROP TRIGGER IF EXISTS trg_dm_cat_att_map ON smart.dm_cat_att_map;                                                                              
DROP TRIGGER IF EXISTS trg_i18n_label ON smart.i18n_label;                                                                                      
DROP TRIGGER IF EXISTS trg_patrol_leg ON smart.patrol_leg;                                                                                      
DROP TRIGGER IF EXISTS trg_patrol_leg_day ON smart.patrol_leg_day;                                                                              
DROP TRIGGER IF EXISTS trg_patrol_leg_members ON smart.patrol_leg_members;                                                                      
DROP TRIGGER IF EXISTS trg_patrol_waypoint ON smart.patrol_waypoint;                                                                            
DROP TRIGGER IF EXISTS trg_rank ON smart.rank;                                                                                                  
DROP TRIGGER IF EXISTS trg_report_query ON smart.report_query;                                                                                  
DROP TRIGGER IF EXISTS trg_track ON smart.track;                                                                                                
DROP TRIGGER IF EXISTS trg_wp_attachments ON smart.wp_attachments;                                                                              
DROP TRIGGER IF EXISTS trg_wp_observation ON smart.wp_observation;                                                                              
DROP TRIGGER IF EXISTS trg_wp_observation_attributes ON smart.wp_observation_attributes;                                                        
DROP TRIGGER IF EXISTS trg_cm_attribute ON smart.cm_attribute;                                                                                  
DROP TRIGGER IF EXISTS trg_cm_attribute_list ON smart.cm_attribute_list;                                                                        
DROP TRIGGER IF EXISTS trg_cm_attribute_option ON smart.cm_attribute_option;                                                                    
DROP TRIGGER IF EXISTS trg_cm_attribute_tree_node ON smart.cm_attribute_tree_node;                                                              
DROP TRIGGER IF EXISTS trg_cm_node ON smart.cm_node;                                                                                            
DROP TRIGGER IF EXISTS trg_screen_option_uuid ON smart.screen_option_uuid;                                                                      
DROP TRIGGER IF EXISTS trg_cm_attribute_config ON smart.cm_attribute_config;                                                                    
DROP TRIGGER IF EXISTS trg_compound_query_layer ON smart.compound_query_layer;                                                                  
DROP TRIGGER IF EXISTS trg_connect_ct_properties ON smart.connect_ct_properties;                                                                
DROP TRIGGER IF EXISTS trg_connect_alert ON smart.connect_alert;                                                                                
DROP TRIGGER IF EXISTS trg_plan ON smart.plan;                                                                                                  
DROP TRIGGER IF EXISTS trg_plan_target ON smart.plan_target;                                                                                    
DROP TRIGGER IF EXISTS trg_plan_target_point ON smart.plan_target_point;                                                                        
DROP TRIGGER IF EXISTS trg_patrol_plan ON smart.patrol_plan;                                                                                    
DROP TRIGGER IF EXISTS trg_ct_patrol_link ON smart.ct_patrol_link;                                                                              
DROP TRIGGER IF EXISTS trg_ct_mission_link ON smart.ct_mission_link;                                                                            
DROP TRIGGER IF EXISTS trg_informant ON smart.informant;                                                                                        
DROP TRIGGER IF EXISTS trg_intelligence ON smart.intelligence;                                                                                  
DROP TRIGGER IF EXISTS trg_intelligence_source ON smart.intelligence_source;                                                                    
DROP TRIGGER IF EXISTS trg_patrol_intelligence ON smart.patrol_intelligence;                                                                    
DROP TRIGGER IF EXISTS trg_intelligence_attachment ON smart.intelligence_attachment;                                                            
DROP TRIGGER IF EXISTS trg_intelligence_point ON smart.intelligence_point;                                                                      
DROP TRIGGER IF EXISTS trg_intel_record_query ON smart.intel_record_query;                                                                      
DROP TRIGGER IF EXISTS trg_intel_summary_query ON smart.intel_summary_query;                                                                    
DROP TRIGGER IF EXISTS trg_i_attachment ON smart.i_attachment;                                                                                  
DROP TRIGGER IF EXISTS trg_i_attribute ON smart.i_attribute;                                                                                    
DROP TRIGGER IF EXISTS trg_i_entity ON smart.i_entity;                                                                                          
DROP TRIGGER IF EXISTS trg_i_entity_search ON smart.i_entity_search;                                                                            
DROP TRIGGER IF EXISTS trg_i_entity_type ON smart.i_entity_type;                                                                                
DROP TRIGGER IF EXISTS trg_i_location ON smart.i_location;                                                                                      
DROP TRIGGER IF EXISTS trg_i_record ON smart.i_record;                                                                                          
DROP TRIGGER IF EXISTS trg_i_record_obs_query ON smart.i_record_obs_query;
DROP TRIGGER IF EXISTS trg_i_entity_summary_query ON smart.i_entity_summary_query;
DROP TRIGGER IF EXISTS trg_i_entity_record_query ON smart.i_entity_record_query;
DROP TRIGGER IF EXISTS trg_i_relationship_group ON smart.i_relationship_group;                                                                  
DROP TRIGGER IF EXISTS trg_i_relationship_type ON smart.i_relationship_type;                                                                    
DROP TRIGGER IF EXISTS trg_i_working_set ON smart.i_working_set;                                                                                
DROP TRIGGER IF EXISTS trg_i_recordsource ON smart.i_recordsource;                                                                              
DROP TRIGGER IF EXISTS trg_i_attribute_list_item ON smart.i_attribute_list_item;                                                                
DROP TRIGGER IF EXISTS trg_i_entity_attachment ON smart.i_entity_attachment;                                                                    
DROP TRIGGER IF EXISTS trg_i_entity_attribute_value ON smart.i_entity_attribute_value;                                                          
DROP TRIGGER IF EXISTS trg_i_entity_location ON smart.i_entity_location;                                                                        
DROP TRIGGER IF EXISTS trg_i_entity_record ON smart.i_entity_record;                                                                            
DROP TRIGGER IF EXISTS trg_i_entity_relationship ON smart.i_entity_relationship;                                                                
DROP TRIGGER IF EXISTS trg_i_entity_relationship_attribute_value ON smart.i_entity_relationship_attribute_value;                                
DROP TRIGGER IF EXISTS trg_i_entity_type_attribute ON smart.i_entity_type_attribute;                                                            
DROP TRIGGER IF EXISTS trg_i_entity_type_attribute_group ON smart.i_entity_type_attribute_group;                                                
DROP TRIGGER IF EXISTS trg_i_observation ON smart.i_observation;                                                                                
DROP TRIGGER IF EXISTS trg_i_observation_attribute ON smart.i_observation_attribute;                                                            
DROP TRIGGER IF EXISTS trg_i_record_attachment ON smart.i_record_attachment;                                                                    
DROP TRIGGER IF EXISTS trg_i_relationship_type_attribute ON smart.i_relationship_type_attribute;                                                
DROP TRIGGER IF EXISTS trg_i_working_set_entity ON smart.i_working_set_entity;                                                                  
DROP TRIGGER IF EXISTS trg_i_working_set_query ON smart.i_working_set_query;                                                                    
DROP TRIGGER IF EXISTS trg_i_working_set_record ON smart.i_working_set_record;                                                                  
DROP TRIGGER IF EXISTS trg_i_record_attribute_value ON smart.i_record_attribute_value;                                                          
DROP TRIGGER IF EXISTS trg_i_record_attribute_value_list ON smart.i_record_attribute_value_list;                                                
DROP TRIGGER IF EXISTS trg_i_recordsource_attribute ON smart.i_recordsource_attribute;                                                          
DROP TRIGGER IF EXISTS trg_mission_attribute ON smart.mission_attribute;                                                                        
DROP TRIGGER IF EXISTS trg_sampling_unit_attribute ON smart.sampling_unit_attribute;                                                            
DROP TRIGGER IF EXISTS trg_survey_design ON smart.survey_design;                                                                                
DROP TRIGGER IF EXISTS trg_mission ON smart.mission;                                                                                            
DROP TRIGGER IF EXISTS trg_mission_attribute_list ON smart.mission_attribute_list;                                                              
DROP TRIGGER IF EXISTS trg_mission_day ON smart.mission_day;                                                                                    
DROP TRIGGER IF EXISTS trg_mission_member ON smart.mission_member;                                                                              
DROP TRIGGER IF EXISTS trg_mission_property ON smart.mission_property;                                                                          
DROP TRIGGER IF EXISTS trg_mission_property_value ON smart.mission_property_value;                                                              
DROP TRIGGER IF EXISTS trg_mission_track ON smart.mission_track;                                                                                
DROP TRIGGER IF EXISTS trg_sampling_unit ON smart.sampling_unit;                                                                                
DROP TRIGGER IF EXISTS trg_sampling_unit_attribute_list ON smart.sampling_unit_attribute_list;                                                  
DROP TRIGGER IF EXISTS trg_sampling_unit_attribute_value ON smart.sampling_unit_attribute_value;                                                
DROP TRIGGER IF EXISTS trg_survey ON smart.survey;                                                                                              
DROP TRIGGER IF EXISTS trg_survey_waypoint ON smart.survey_waypoint;                                                                            
DROP TRIGGER IF EXISTS trg_survey_design_property ON smart.survey_design_property;                                                              
DROP TRIGGER IF EXISTS trg_survey_design_sampling_unit ON smart.survey_design_sampling_unit;                                                    
DROP TRIGGER IF EXISTS trg_survey_gridded_query ON smart.survey_gridded_query;                                                                  
DROP TRIGGER IF EXISTS trg_survey_mission_query ON smart.survey_mission_query;                                                                  
DROP TRIGGER IF EXISTS trg_survey_mission_track_query ON smart.survey_mission_track_query;                                                      
DROP TRIGGER IF EXISTS trg_survey_observation_query ON smart.survey_observation_query;                                                          
DROP TRIGGER IF EXISTS trg_survey_summary_query ON smart.survey_summary_query;                                                                  
DROP TRIGGER IF EXISTS trg_survey_waypoint_query ON smart.survey_waypoint_query;                                                                
DROP TRIGGER IF EXISTS trg_entity_type ON smart.entity_type;                                                                                    
DROP TRIGGER IF EXISTS trg_entity ON smart.entity;                                                                                              
DROP TRIGGER IF EXISTS trg_entity_attribute ON smart.entity_attribute;                                                                          
DROP TRIGGER IF EXISTS trg_entity_attribute_value ON smart.entity_attribute_value;                                                              
DROP TRIGGER IF EXISTS trg_entity_gridded_query ON smart.entity_gridded_query;                                                                  
DROP TRIGGER IF EXISTS trg_entity_observation_query ON smart.entity_observation_query;                                                          
DROP TRIGGER IF EXISTS trg_entity_summary_query ON smart.entity_summary_query;                                                                  
DROP TRIGGER IF EXISTS trg_entity_waypoint_query ON smart.entity_waypoint_query;                                                                
DROP TRIGGER IF EXISTS trg_ct_properties_option ON smart.ct_properties_option;                                                                  
DROP TRIGGER IF EXISTS trg_ct_properties_profile ON smart.ct_properties_profile;                                                                
DROP TRIGGER IF EXISTS trg_ct_properties_profile_option ON smart.ct_properties_profile_option;                                                  
DROP TRIGGER IF EXISTS trg_cm_ct_properties_profile ON smart.cm_ct_properties_profile;                                                          
DROP TRIGGER IF EXISTS trg_connect_account ON smart.connect_account;                                                                            
DROP TRIGGER IF EXISTS trg_qa_routine ON smart.qa_routine;                                                                                      
DROP TRIGGER IF EXISTS trg_qa_error ON smart.qa_error;                                                                                          
DROP TRIGGER IF EXISTS trg_qa_routine_parameter ON smart.qa_routine_parameter;                                                                  
DROP TRIGGER IF EXISTS trg_observation_attachment on smart.observation_attachment;
DROP TRIGGER IF EXISTS trg_e_event_filter on smart.e_event_filter;
DROP TRIGGER IF EXISTS trg_e_action on smart.e_action;
DROP TRIGGER IF EXISTS trg_e_action_parameter_value on smart.e_action_parameter_value;
DROP TRIGGER IF EXISTS trg_e_event_action on smart.e_event_action;

DROP FUNCTION IF EXISTS connect.trg_changelog_common();
DROP FUNCTION IF EXISTS connect.trg_cm_attribute();
DROP FUNCTION IF EXISTS connect.trg_cm_attribute_config();
DROP FUNCTION IF EXISTS connect.trg_cm_attribute_list();
DROP FUNCTION IF EXISTS connect.trg_cm_attribute_option();
DROP FUNCTION IF EXISTS connect.trg_cm_attribute_tree_node();
DROP FUNCTION IF EXISTS connect.trg_cm_ct_properties_profile();
DROP FUNCTION IF EXISTS connect.trg_cm_node();
DROP FUNCTION IF EXISTS connect.trg_compound_query_layer();
DROP FUNCTION IF EXISTS connect.trg_connect_account();
DROP FUNCTION IF EXISTS connect.trg_connect_alert();
DROP FUNCTION IF EXISTS connect.trg_connect_ct_properties();
DROP FUNCTION IF EXISTS connect.trg_ct_mission_link();
DROP FUNCTION IF EXISTS connect.trg_ct_patrol_link();
DROP FUNCTION IF EXISTS connect.trg_ct_properties_profile_option();
DROP FUNCTION IF EXISTS connect.trg_dm_att_agg_map();
DROP FUNCTION IF EXISTS connect.trg_dm_attribute_list();
DROP FUNCTION IF EXISTS connect.trg_dm_attribute_tree();
DROP FUNCTION IF EXISTS connect.trg_dm_cat_att_map();
DROP FUNCTION IF EXISTS connect.trg_entity();
DROP FUNCTION IF EXISTS connect.trg_entity_attribute();
DROP FUNCTION IF EXISTS connect.trg_entity_attribute_value();
DROP FUNCTION IF EXISTS connect.trg_i18n_label();
DROP FUNCTION IF EXISTS connect.trg_i_attribute_list_item();
DROP FUNCTION IF EXISTS connect.trg_i_entity_attachment();
DROP FUNCTION IF EXISTS connect.trg_i_entity_attribute_value();
DROP FUNCTION IF EXISTS connect.trg_i_entity_location();
DROP FUNCTION IF EXISTS connect.trg_i_entity_record();
DROP FUNCTION IF EXISTS connect.trg_i_entity_relationship();
DROP FUNCTION IF EXISTS connect.trg_i_entity_relationship_attribute_value();
DROP FUNCTION IF EXISTS connect.trg_i_entity_type_attribute();
DROP FUNCTION IF EXISTS connect.trg_i_entity_type_attribute_group();
DROP FUNCTION IF EXISTS connect.trg_i_observation();
DROP FUNCTION IF EXISTS connect.trg_i_observation_attribute();
DROP FUNCTION IF EXISTS connect.trg_i_record_attachment();
DROP FUNCTION IF EXISTS connect.trg_i_record_attribute_value();
DROP FUNCTION IF EXISTS connect.trg_i_record_attribute_value_list();
DROP FUNCTION IF EXISTS connect.trg_i_recordsource_attribute();
DROP FUNCTION IF EXISTS connect.trg_i_relationship_type_attribute();
DROP FUNCTION IF EXISTS connect.trg_i_working_set_entity();
DROP FUNCTION IF EXISTS connect.trg_i_working_set_query();
DROP FUNCTION IF EXISTS connect.trg_i_working_set_record();
DROP FUNCTION IF EXISTS connect.trg_intelligence_attachment();
DROP FUNCTION IF EXISTS connect.trg_intelligence_point();
DROP FUNCTION IF EXISTS connect.trg_mission();
DROP FUNCTION IF EXISTS connect.trg_mission_attribute_list();
DROP FUNCTION IF EXISTS connect.trg_mission_day();
DROP FUNCTION IF EXISTS connect.trg_mission_member();
DROP FUNCTION IF EXISTS connect.trg_mission_property();
DROP FUNCTION IF EXISTS connect.trg_mission_property_value();
DROP FUNCTION IF EXISTS connect.trg_mission_track();
DROP FUNCTION IF EXISTS connect.trg_observation_attachment();
DROP FUNCTION IF EXISTS connect.trg_patrol_intelligence();
DROP FUNCTION IF EXISTS connect.trg_patrol_leg();
DROP FUNCTION IF EXISTS connect.trg_patrol_leg_day();
DROP FUNCTION IF EXISTS connect.trg_patrol_leg_members();
DROP FUNCTION IF EXISTS connect.trg_patrol_plan();
DROP FUNCTION IF EXISTS connect.trg_patrol_type();
DROP FUNCTION IF EXISTS connect.trg_patrol_waypoint();
DROP FUNCTION IF EXISTS connect.trg_plan_target();
DROP FUNCTION IF EXISTS connect.trg_plan_target_point();
DROP FUNCTION IF EXISTS connect.trg_qa_routine_parameter();
DROP FUNCTION IF EXISTS connect.trg_rank();
DROP FUNCTION IF EXISTS connect.trg_report_query();
DROP FUNCTION IF EXISTS connect.trg_sampling_unit();
DROP FUNCTION IF EXISTS connect.trg_sampling_unit_attribute_list();
DROP FUNCTION IF EXISTS connect.trg_sampling_unit_attribute_value();
DROP FUNCTION IF EXISTS connect.trg_screen_option_uuid();
DROP FUNCTION IF EXISTS connect.trg_survey();
DROP FUNCTION IF EXISTS connect.trg_survey_design_property();
DROP FUNCTION IF EXISTS connect.trg_survey_design_sampling_unit();
DROP FUNCTION IF EXISTS connect.trg_survey_waypoint();
DROP FUNCTION IF EXISTS connect.trg_track();
DROP FUNCTION IF EXISTS connect.trg_wp_attachments();
DROP FUNCTION IF EXISTS connect.trg_wp_observation();
DROP FUNCTION IF EXISTS connect.trg_wp_observation_attributes();
DROP FUNCTION IF EXISTS connect.trg_conservation_area();
DROP FUNCTION IF EXISTS connect.trg_observation_options();
DROP FUNCTION IF EXISTS connect.trg_e_action_parameter_value();
DROP FUNCTION IF EXISTS connect.trg_e_event_action();

CREATE OR REPLACE FUNCTION connect.trg_changelog_common() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str,  ca_uuid) 
 		VALUES
 		(uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, ROW.CA_UUID);
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';



--- QA MODULE TRIGGERS --- 
CREATE OR REPLACE FUNCTION connect.trg_qa_routine_parameter() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, r.CA_UUID FROM smart.qa_routine r WHERE r.uuid = ROW.qa_routine_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_qa_routine AFTER INSERT OR UPDATE OR DELETE ON smart.qa_routine FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_qa_error AFTER INSERT OR UPDATE OR DELETE ON smart.qa_error FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_qa_routine_parameter AFTER INSERT OR UPDATE OR DELETE ON smart.qa_routine_parameter FOR EACH ROW execute procedure connect.trg_qa_routine_parameter();





-- CONNECT ACCOUNT -- 
CREATE OR REPLACE FUNCTION connect.trg_connect_account() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'employee_uuid', ROW.EMPLOYEE_UUID, null, null, null, server.CA_UUID FROM smart.connect_server server WHERE server.uuid = ROW.connect_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_connect_account AFTER INSERT OR UPDATE OR DELETE ON smart.connect_account FOR EACH ROW execute procedure connect.trg_connect_account();



-- CT PROPERTIES -- 

CREATE OR REPLACE FUNCTION connect.trg_ct_properties_profile_option() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, p.CA_UUID FROM smart.ct_properties_profile p WHERE p.uuid = ROW.profile_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_cm_ct_properties_profile() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'cm_uuid', ROW.CM_UUID, null, null, null, cm.CA_UUID FROM smart.configurable_model cm WHERE cm.uuid = ROW.cm_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_ct_properties_option AFTER INSERT OR UPDATE OR DELETE ON smart.ct_properties_option FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_ct_properties_profile AFTER INSERT OR UPDATE OR DELETE ON smart.ct_properties_profile FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE TRIGGER trg_ct_properties_profile_option AFTER INSERT OR UPDATE OR DELETE ON smart.ct_properties_profile_option FOR EACH ROW execute procedure connect.trg_ct_properties_profile_option();
CREATE TRIGGER trg_cm_ct_properties_profile AFTER INSERT OR UPDATE OR DELETE ON smart.cm_ct_properties_profile FOR EACH ROW execute procedure connect.trg_cm_ct_properties_profile();



--ENTITY QUERIES

CREATE TRIGGER trg_entity_gridded_query AFTER INSERT OR UPDATE OR DELETE ON smart.entity_gridded_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_entity_observation_query AFTER INSERT OR UPDATE OR DELETE ON smart.entity_observation_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_entity_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.entity_summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_entity_waypoint_query AFTER INSERT OR UPDATE OR DELETE ON smart.entity_waypoint_query FOR EACH ROW execute procedure connect.trg_changelog_common();

--ENTITIES

CREATE OR REPLACE FUNCTION connect.trg_entity() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, et.CA_UUID FROM smart.entity_type et WHERE et.uuid = ROW.entity_type_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';



CREATE OR REPLACE FUNCTION connect.trg_entity_attribute() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, et.CA_UUID FROM smart.entity_type et WHERE et.uuid = ROW.entity_type_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_entity_attribute_value() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_attribute_uuid', ROW.entity_attribute_uuid, 'entity_uuid', ROW.entity_uuid, null, et.CA_UUID FROM smart.entity_type et, smart.entity e WHERE e.entity_type_uuid = et.uuid and e.uuid = ROW.entity_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_entity_type AFTER INSERT OR UPDATE OR DELETE ON smart.entity_type FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_entity AFTER INSERT OR UPDATE OR DELETE ON smart.entity FOR EACH ROW execute procedure connect.trg_entity();
CREATE TRIGGER trg_entity_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.entity_attribute FOR EACH ROW execute procedure connect.trg_entity_attribute();
CREATE TRIGGER trg_entity_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.entity_attribute_value FOR EACH ROW execute procedure connect.trg_entity_attribute_value();


-- ER QUERIES
CREATE TRIGGER trg_survey_gridded_query AFTER INSERT OR UPDATE OR DELETE ON smart.survey_gridded_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_survey_mission_query AFTER INSERT OR UPDATE OR DELETE ON smart.survey_mission_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_survey_mission_track_query AFTER INSERT OR UPDATE OR DELETE ON smart.survey_mission_track_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_survey_observation_query AFTER INSERT OR UPDATE OR DELETE ON smart.survey_observation_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_survey_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.survey_summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_survey_waypoint_query AFTER INSERT OR UPDATE OR DELETE ON smart.survey_waypoint_query FOR EACH ROW execute procedure connect.trg_changelog_common();

-- ER CORE
CREATE OR REPLACE FUNCTION connect.trg_mission() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sd.CA_UUID FROM smart.survey s, smart.survey_design sd WHERE s.survey_design_uuid = sd.uuid and s.uuid = ROW.survey_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';



CREATE OR REPLACE FUNCTION connect.trg_mission_attribute_list() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, ma.CA_UUID FROM smart.mission_attribute ma WHERE ma.uuid = ROW.mission_attribute_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_mission_day() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sd.CA_UUID FROM smart.mission m, smart.survey s, smart.survey_design sd 
 		WHERE s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = ROW.mission_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_mission_member() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'mission_uuid', ROW.mission_uuid, 'employee_uuid', ROW.employee_uuid, null, e.CA_UUID FROM smart.employee e
 		WHERE e.uuid = ROW.employee_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_mission_property() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'survey_design_uuid', ROW.survey_design_uuid, 'mission_attribute_uuid', ROW.mission_attribute_uuid, null, sd.CA_UUID FROM smart.survey_design sd
 		WHERE sd.uuid = ROW.survey_design_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_mission_property_value() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'mission_uuid', ROW.mission_uuid, 'mission_attribute_uuid', ROW.mission_attribute_uuid, null, ma.CA_UUID 
 		FROM smart.mission_attribute ma
 		WHERE ma.uuid = ROW.mission_attribute_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_mission_track() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sd.CA_UUID 
 		FROM smart.mission_day md, smart.mission m, smart.survey s, smart.survey_design sd 
 		WHERE s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = md.mission_uuid and md.uuid = ROW.mission_day_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

 
 
CREATE OR REPLACE FUNCTION connect.trg_sampling_unit() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sd.CA_UUID 
 		FROM smart.survey_design sd 
 		WHERE sd.uuid = ROW.survey_design_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_sampling_unit_attribute_list() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sa.CA_UUID 
 		FROM smart.sampling_unit_attribute sa
 		WHERE sa.uuid = ROW.sampling_unit_attribute_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_sampling_unit_attribute_value() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'su_attribute_uuid', ROW.su_attribute_uuid, 'su_uuid', ROW.su_uuid, null, sa.CA_UUID 
 		FROM smart.sampling_unit_attribute sa
 		WHERE sa.uuid = ROW.su_attribute_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

 
CREATE OR REPLACE FUNCTION connect.trg_survey() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sd.CA_UUID 
 		FROM smart.survey_design sd
 		WHERE sd.uuid = ROW.survey_design_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';



CREATE OR REPLACE FUNCTION connect.trg_survey_design_property() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sd.CA_UUID 
 		FROM smart.survey_design sd
 		WHERE sd.uuid = ROW.survey_design_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_survey_design_sampling_unit() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'survey_design_uuid', ROW.survey_design_uuid, 'su_attribute_uuid', ROW.su_attribute_uuid, null, sd.CA_UUID 
 		FROM smart.survey_design sd
 		WHERE sd.uuid = ROW.survey_design_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_survey_waypoint() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'wp_uuid', ROW.wp_uuid, 'mission_day_uuid', ROW.mission_day_uuid, null, wp.CA_UUID 
 		FROM smart.waypoint wp
 		WHERE wp.uuid = ROW.wp_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';
 
CREATE TRIGGER trg_mission_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.mission_attribute FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_sampling_unit_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.sampling_unit_attribute FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_survey_design AFTER INSERT OR UPDATE OR DELETE ON smart.survey_design FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_mission AFTER INSERT OR UPDATE OR DELETE ON smart.mission FOR EACH ROW execute procedure connect.trg_mission();
CREATE TRIGGER trg_mission_attribute_list AFTER INSERT OR UPDATE OR DELETE ON smart.mission_attribute_list FOR EACH ROW execute procedure connect.trg_mission_attribute_list();
CREATE TRIGGER trg_mission_day AFTER INSERT OR UPDATE OR DELETE ON smart.mission_day FOR EACH ROW execute procedure connect.trg_mission_day();
CREATE TRIGGER trg_mission_member AFTER INSERT OR UPDATE OR DELETE ON smart.mission_member FOR EACH ROW execute procedure connect.trg_mission_member();
CREATE TRIGGER trg_mission_property AFTER INSERT OR UPDATE OR DELETE ON smart.mission_property FOR EACH ROW execute procedure connect.trg_mission_property();
CREATE TRIGGER trg_mission_property_value AFTER INSERT OR UPDATE OR DELETE ON smart.mission_property_value FOR EACH ROW execute procedure connect.trg_mission_property_value();
CREATE TRIGGER trg_mission_track AFTER INSERT OR UPDATE OR DELETE ON smart.mission_track FOR EACH ROW execute procedure connect.trg_mission_track();
CREATE TRIGGER trg_sampling_unit AFTER INSERT OR UPDATE OR DELETE ON smart.sampling_unit FOR EACH ROW execute procedure connect.trg_sampling_unit();
CREATE TRIGGER trg_sampling_unit_attribute_list AFTER INSERT OR UPDATE OR DELETE ON smart.sampling_unit_attribute_list FOR EACH ROW execute procedure connect.trg_sampling_unit_attribute_list();
CREATE TRIGGER trg_sampling_unit_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.sampling_unit_attribute_value FOR EACH ROW execute procedure connect.trg_sampling_unit_attribute_value();
CREATE TRIGGER trg_survey AFTER INSERT OR UPDATE OR DELETE ON smart.survey FOR EACH ROW execute procedure connect.trg_survey();
CREATE TRIGGER trg_survey_waypoint AFTER INSERT OR UPDATE OR DELETE ON smart.survey_waypoint FOR EACH ROW execute procedure connect.trg_survey_waypoint(); 
CREATE TRIGGER trg_survey_design_property AFTER INSERT OR UPDATE OR DELETE ON smart.survey_design_property FOR EACH ROW execute procedure connect.trg_survey_design_property(); 
CREATE TRIGGER trg_survey_design_sampling_unit AFTER INSERT OR UPDATE OR DELETE ON smart.survey_design_sampling_unit FOR EACH ROW execute procedure connect.trg_survey_design_sampling_unit(); 

-- ADVANCED INTELLIGENCE --

CREATE OR REPLACE FUNCTION connect.trg_i_attribute_list_item() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		FROM smart.i_attribute i
 		WHERE i.uuid = ROW.attribute_uuid;
 RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_entity_attribute_value() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_uuid', ROW.entity_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.i_entity i where i.uuid = ROW.entity_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';
 
CREATE OR REPLACE FUNCTION connect.trg_i_entity_attachment() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_uuid', ROW.entity_uuid, 'attachment_uuid', ROW.attachment_uuid, null, i.CA_UUID 
 		from smart.i_entity i where i.uuid = ROW.entity_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';
 
CREATE OR REPLACE FUNCTION connect.trg_i_entity_location() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_uuid', ROW.entity_uuid, 'location_uuid', ROW.location_uuid, null, i.CA_UUID 
 		from smart.i_entity i where i.uuid = ROW.entity_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_entity_record() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_uuid', ROW.entity_uuid, 'record_uuid', ROW.record_uuid, null, i.CA_UUID 
 		from smart.i_entity i where i.uuid = ROW.entity_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION connect.trg_i_entity_relationship() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.i_relationship_type i where i.uuid = ROW.relationship_type_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_entity_relationship_attribute_value() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_relationship_uuid', ROW.entity_relationship_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.i_attribute i where i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_entity_type_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_type_uuid', ROW.entity_type_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.i_entity_type i where i.uuid = ROW.entity_type_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_entity_type_attribute_group() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.i_entity_type i where i.uuid = ROW.entity_type_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_observation() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.i_location i where i.uuid = ROW.location_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_observation_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'observation_uuid', ROW.observation_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.dm_attribute i where i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_record_attachment() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'record_uuid', ROW.record_uuid, 'attachment_uuid', ROW.attachment_uuid, null, i.CA_UUID 
 		from smart.i_record i where i.uuid = ROW.record_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_relationship_type_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'relationship_type_uuid', ROW.relationship_type_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.i_attribute i where i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_working_set_entity() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'working_set_uuid', ROW.working_set_uuid, 'entity_uuid', ROW.entity_uuid, null, i.CA_UUID 
 		from smart.i_working_set i where i.uuid = ROW.working_set_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_working_set_query() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'working_set_uuid', ROW.working_set_uuid, 'query_uuid', ROW.query_uuid, null, i.CA_UUID 
 		from smart.i_working_set i where i.uuid = ROW.working_set_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_working_set_record() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'working_set_uuid', ROW.working_set_uuid, 'record_uuid', ROW.record_uuid, null, i.CA_UUID 
 		from smart.i_working_set i where i.uuid = ROW.working_set_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_record_attribute_value() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'record_uuid', ROW.record_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.i_record i where i.uuid = ROW.record_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_record_attribute_value_list() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'value_uuid', ROW.value_uuid, 'element_uuid', ROW.element_uuid, null, i.CA_UUID 
 		from smart.i_record_attribute_value v, smart.i_record i where v.uuid = ROW.value_uuid and i.uuid = v.record_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_i_recordsource_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.i_recordsource i WHERE i.uuid = ROW.source_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';


CREATE TRIGGER trg_i_attachment AFTER INSERT OR UPDATE OR DELETE ON smart.i_attachment FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.i_attribute FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_entity AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_entity_search AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_search FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_entity_type AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_type FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_location AFTER INSERT OR UPDATE OR DELETE ON smart.i_location FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_record AFTER INSERT OR UPDATE OR DELETE ON smart.i_record FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_record_obs_query AFTER INSERT OR UPDATE OR DELETE ON smart.i_record_obs_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_entity_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_entity_record_query AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_record_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_relationship_group AFTER INSERT OR UPDATE OR DELETE ON smart.i_relationship_group FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_relationship_type AFTER INSERT OR UPDATE OR DELETE ON smart.i_relationship_type FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_working_set AFTER INSERT OR UPDATE OR DELETE ON smart.i_working_set FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_recordsource AFTER INSERT OR UPDATE OR DELETE ON smart.i_recordsource FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_attribute_list_item AFTER INSERT OR UPDATE OR DELETE ON smart.i_attribute_list_item FOR EACH ROW execute procedure connect.trg_i_attribute_list_item();
CREATE TRIGGER trg_i_entity_attachment AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_attachment FOR EACH ROW execute procedure connect.trg_i_entity_attachment();
CREATE TRIGGER trg_i_entity_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_attribute_value FOR EACH ROW execute procedure connect.trg_i_entity_attribute_value();
CREATE TRIGGER trg_i_entity_location AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_location FOR EACH ROW execute procedure connect.trg_i_entity_location();
CREATE TRIGGER trg_i_entity_record AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_record FOR EACH ROW execute procedure connect.trg_i_entity_record();
CREATE TRIGGER trg_i_entity_relationship AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_relationship FOR EACH ROW execute procedure connect.trg_i_entity_relationship();
CREATE TRIGGER trg_i_entity_relationship_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_relationship_attribute_value FOR EACH ROW execute procedure connect.trg_i_entity_relationship_attribute_value();
CREATE TRIGGER trg_i_entity_type_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_type_attribute FOR EACH ROW execute procedure connect.trg_i_entity_type_attribute();
CREATE TRIGGER trg_i_entity_type_attribute_group AFTER INSERT OR UPDATE OR DELETE ON smart.i_entity_type_attribute_group FOR EACH ROW execute procedure connect.trg_i_entity_type_attribute_group();
CREATE TRIGGER trg_i_observation AFTER INSERT OR UPDATE OR DELETE ON smart.i_observation FOR EACH ROW execute procedure connect.trg_i_observation();
CREATE TRIGGER trg_i_observation_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.i_observation_attribute FOR EACH ROW execute procedure connect.trg_i_observation_attribute();
CREATE TRIGGER trg_i_record_attachment AFTER INSERT OR UPDATE OR DELETE ON smart.i_record_attachment FOR EACH ROW execute procedure connect.trg_i_record_attachment();
CREATE TRIGGER trg_i_relationship_type_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.i_relationship_type_attribute FOR EACH ROW execute procedure connect.trg_i_relationship_type_attribute();
CREATE TRIGGER trg_i_working_set_entity AFTER INSERT OR UPDATE OR DELETE ON smart.i_working_set_entity FOR EACH ROW execute procedure connect.trg_i_working_set_entity();
CREATE TRIGGER trg_i_working_set_query AFTER INSERT OR UPDATE OR DELETE ON smart.i_working_set_query FOR EACH ROW execute procedure connect.trg_i_working_set_query();
CREATE TRIGGER trg_i_working_set_record AFTER INSERT OR UPDATE OR DELETE ON smart.i_working_set_record FOR EACH ROW execute procedure connect.trg_i_working_set_record();
CREATE TRIGGER trg_i_record_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.i_record_attribute_value FOR EACH ROW execute procedure connect.trg_i_record_attribute_value();
CREATE TRIGGER trg_i_record_attribute_value_list AFTER INSERT OR UPDATE OR DELETE ON smart.i_record_attribute_value_list FOR EACH ROW execute procedure connect.trg_i_record_attribute_value_list();
CREATE TRIGGER trg_i_recordsource_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.i_recordsource_attribute FOR EACH ROW execute procedure connect.trg_i_recordsource_attribute();
CREATE TRIGGER trg_i_config_option AFTER INSERT OR UPDATE OR DELETE ON smart.i_config_option FOR EACH ROW execute procedure connect.trg_changelog_common();


-- INTELLIGENCE QUERIES --
CREATE TRIGGER trg_intel_record_query AFTER INSERT OR UPDATE OR DELETE ON smart.intel_record_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_intel_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.intel_summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();



--INTELLIGENCE

CREATE OR REPLACE FUNCTION connect.trg_patrol_intelligence() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'patrol_uuid', ROW.patrol_uuid, 'intelligence_uuid', ROW.intelligence_uuid, null, p.CA_UUID 
 		from smart.patrol p where p.uuid = ROW.patrol_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_intelligence_attachment() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.intelligence i where i.uuid = ROW.intelligence_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_intelligence_point() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.intelligence i where i.uuid = ROW.intelligence_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_informant AFTER INSERT OR UPDATE OR DELETE ON smart.informant FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_intelligence AFTER INSERT OR UPDATE OR DELETE ON smart.intelligence FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_intelligence_source AFTER INSERT OR UPDATE OR DELETE ON smart.intelligence_source FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_patrol_intelligence AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_intelligence FOR EACH ROW execute procedure connect.trg_patrol_intelligence();
CREATE TRIGGER trg_intelligence_attachment AFTER INSERT OR UPDATE OR DELETE ON smart.intelligence_attachment FOR EACH ROW execute procedure connect.trg_intelligence_attachment();
CREATE TRIGGER trg_intelligence_point AFTER INSERT OR UPDATE OR DELETE ON smart.intelligence_point FOR EACH ROW execute procedure connect.trg_intelligence_point();



--PLANNING

CREATE OR REPLACE FUNCTION connect.trg_plan_target() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, p.CA_UUID 
 		from smart.plan p where p.uuid = ROW.plan_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_plan_target_point() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, p.CA_UUID 
 		FROM smart.plan_target pt, smart.plan p WHERE p.uuid = pt.plan_uuid and pt.uuid = ROW.plan_target_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_patrol_plan() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'patrol_uuid', ROW.patrol_uuid, 'plan_uuid', ROW.plan_uuid, null, p.CA_UUID 
 		FROM smart.patrol p where p.uuid = ROW.patrol_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';


CREATE TRIGGER trg_plan AFTER INSERT OR UPDATE OR DELETE ON smart.plan FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_plan_target AFTER INSERT OR UPDATE OR DELETE ON smart.plan_target FOR EACH ROW execute procedure connect.trg_plan_target();
CREATE TRIGGER trg_plan_target_point AFTER INSERT OR UPDATE OR DELETE ON smart.plan_target_point FOR EACH ROW execute procedure connect.trg_plan_target_point();
CREATE TRIGGER trg_patrol_plan AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_plan FOR EACH ROW execute procedure connect.trg_patrol_plan();

-- CYBERTRACKER --

CREATE OR REPLACE FUNCTION connect.trg_connect_ct_properties() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, cm.CA_UUID 
 		FROM smart.configurable_model cm WHERE cm.uuid = ROW.cm_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_connect_alert() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, cm.CA_UUID 
 		FROM smart.configurable_model cm WHERE cm.uuid = ROW.cm_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_connect_ct_properties AFTER INSERT OR UPDATE OR DELETE ON smart.connect_ct_properties FOR EACH ROW execute procedure connect.trg_connect_ct_properties();
CREATE TRIGGER trg_connect_alert AFTER INSERT OR UPDATE OR DELETE ON smart.connect_alert FOR EACH ROW execute procedure connect.trg_connect_alert();


CREATE OR REPLACE FUNCTION connect.trg_ct_mission_link() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'ct_uuid', ROW.ct_uuid, null, null, null, sd.CA_UUID 
 		FROM smart.mission mm, smart.survey s, smart.survey_design sd WHERE mm.survey_uuid = s.uuid and s.survey_design_uuid = sd.uuid and mm.uuid = ROW.mission_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_ct_mission_link AFTER INSERT OR UPDATE OR DELETE ON smart.ct_mission_link FOR EACH ROW execute procedure connect.trg_ct_mission_link();


CREATE OR REPLACE FUNCTION connect.trg_ct_patrol_link() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'ct_uuid', ROW.ct_uuid, null, null, null, pp.CA_UUID 
 		FROM smart.patrol pp, smart.patrol_leg pl WHERE pl.patrol_uuid = pp.uuid and pl.uuid = ROW.patrol_leg_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_ct_patrol_link AFTER INSERT OR UPDATE OR DELETE ON smart.ct_patrol_link FOR EACH ROW execute procedure connect.trg_ct_patrol_link();


--SMART CORE
CREATE OR REPLACE FUNCTION connect.trg_patrol_type() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		VALUES (uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'ca_uuid', ROW.ca_uuid, 'patrol_type', null, ROW.patrol_type,  ROW.CA_UUID);
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_dm_attribute_list() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, da.CA_UUID 
 		FROM smart.dm_attribute da WHERE da.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_dm_attribute_tree() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, da.CA_UUID 
 		FROM smart.dm_attribute da WHERE da.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_dm_att_agg_map() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'attribute_uuid', ROW.attribute_uuid, 'agg_name', null, ROW.agg_name, a.CA_UUID 
 		FROM smart.dm_attribute a WHERE a.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_dm_cat_att_map() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'attribute_uuid', ROW.attribute_uuid, 'category_uuid', ROW.category_uuid, null, a.CA_UUID 
 		FROM smart.dm_attribute a WHERE a.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_i18n_label() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'element_uuid', ROW.element_uuid, 'language_uuid', ROW.language_uuid, null, l.CA_UUID 
 		FROM smart.language l WHERE l.uuid = ROW.language_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_observation_attachment() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, wp.CA_UUID 
 		FROM smart.wp_observation ob, smart.waypoint wp where ob.wp_uuid = wp.uuid and ob.uuid = ROW.obs_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_patrol_leg() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, p.CA_UUID 
 		FROM smart.patrol p WHERE p.uuid = ROW.patrol_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_patrol_leg_day() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, p.CA_UUID 
 		FROM smart.patrol p, smart.patrol_leg pl where pl.patrol_uuid = p.uuid and pl.uuid = ROW.patrol_leg_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_patrol_leg_members() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'patrol_leg_uuid', ROW.patrol_leg_uuid, 'employee_uuid', ROW.employee_uuid, null, e.CA_UUID 
 		FROM smart.employee e WHERE e.uuid = ROW.employee_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_patrol_waypoint() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'leg_day_uuid', ROW.leg_day_uuid, 'wp_uuid', ROW.wp_uuid, null, wp.CA_UUID 
 		FROM smart.waypoint wp WHERE wp.uuid = ROW.wp_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_rank() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, a.CA_UUID 
 		FROM smart.agency a WHERE a.uuid = ROW.agency_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_report_query() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'report_uuid', ROW.report_uuid, 'query_uuid', ROW.query_uuid, null, r.CA_UUID 
 		from smart.report r where r.uuid = ROW.report_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_track() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, p.CA_UUID 
 		FROM smart.patrol p, smart.patrol_leg pl, smart.patrol_leg_day pld WHERE p.uuid = pl.patrol_uuid and pl.uuid = pld.patrol_leg_uuid and pld.uuid = ROW.patrol_leg_day_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_wp_attachments() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, wp.CA_UUID 
 		FROM smart.waypoint wp WHERE wp.uuid = ROW.wp_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_wp_observation() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, wp.CA_UUID 
 		FROM smart.waypoint wp WHERE wp.uuid = ROW.wp_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_wp_observation_attributes() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'attribute_uuid', ROW.attribute_uuid, null, null, null, a.CA_UUID 
 		FROM smart.dm_attribute a WHERE a.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_cm_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, a.CA_UUID 
 		FROM smart.dm_attribute a WHERE a.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_cm_attribute_list() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, cm.CA_UUID 
 		FROM smart.configurable_model cm, smart.cm_attribute_config cf where cm.uuid = cf.cm_uuid and cf.uuid = ROW.config_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_cm_attribute_option() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, dm.CA_UUID 
 		FROM smart.cm_attribute cm, smart.dm_attribute dm where cm.attribute_uuid = dm.uuid and cm.uuid = ROW.cm_attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_cm_attribute_tree_node() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, cm.CA_UUID 
 		FROM smart.configurable_model cm, smart.cm_attribute_config cf where cm.uuid = cf.cm_uuid and cf.uuid = ROW.config_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_cm_node() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, cm.CA_UUID 
 		FROM smart.configurable_model cm where cm.uuid = ROW.cm_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_screen_option_uuid() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, op.CA_UUID 
 		FROM smart.screen_option op where op.uuid = ROW.option_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 


CREATE OR REPLACE FUNCTION connect.trg_cm_attribute_config() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, cm.CA_UUID 
 		FROM smart.configurable_model cm where cm.uuid = ROW.cm_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE OR REPLACE FUNCTION connect.trg_compound_query_layer() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, cq.CA_UUID 
 		FROM smart.compound_query cq where cq.uuid = ROW.compound_query_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 


CREATE OR REPLACE FUNCTION connect.trg_conservation_area() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		VALUES (uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, ROW.UUID); 
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_observation_options() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		VALUES (uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'ca_uuid', ROW.ca_uuid, null, null, null, ROW.ca_UUID); 
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_conservation_area AFTER INSERT OR UPDATE OR DELETE ON smart.conservation_area FOR EACH ROW execute procedure connect.trg_conservation_area();

CREATE TRIGGER trg_query_folder AFTER INSERT OR UPDATE OR DELETE ON smart.query_folder FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_report AFTER INSERT OR UPDATE OR DELETE ON smart.report FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_report_folder AFTER INSERT OR UPDATE OR DELETE ON smart.report_folder FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_saved_maps AFTER INSERT OR UPDATE OR DELETE ON smart.saved_maps FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_station AFTER INSERT OR UPDATE OR DELETE ON smart.station FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_team AFTER INSERT OR UPDATE OR DELETE ON smart.team FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_waypoint AFTER INSERT OR UPDATE OR DELETE ON smart.waypoint FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_waypoint_query AFTER INSERT OR UPDATE OR DELETE ON smart.waypoint_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_configurable_model AFTER INSERT OR UPDATE OR DELETE ON smart.configurable_model FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_screen_option AFTER INSERT OR UPDATE OR DELETE ON smart.screen_option FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_compound_query AFTER INSERT OR UPDATE OR DELETE ON smart.compound_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_agency AFTER INSERT OR UPDATE OR DELETE ON smart.agency FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_area_geometries AFTER INSERT OR UPDATE OR DELETE ON smart.area_geometries FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_ca_projection AFTER INSERT OR UPDATE OR DELETE ON smart.ca_projection FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_dm_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.dm_attribute FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_dm_category AFTER INSERT OR UPDATE OR DELETE ON smart.dm_category FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_employee AFTER INSERT OR UPDATE OR DELETE ON smart.employee FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_gridded_query AFTER INSERT OR UPDATE OR DELETE ON smart.gridded_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_language AFTER INSERT OR UPDATE OR DELETE ON smart.language FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_map_styles AFTER INSERT OR UPDATE OR DELETE ON smart.map_styles FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_observation_options AFTER INSERT OR UPDATE OR DELETE ON smart.observation_options FOR EACH ROW execute procedure connect.trg_observation_options();
CREATE TRIGGER trg_observation_query AFTER INSERT OR UPDATE OR DELETE ON smart.observation_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_obs_gridded_query AFTER INSERT OR UPDATE OR DELETE ON smart.obs_gridded_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_obs_observation_query AFTER INSERT OR UPDATE OR DELETE ON smart.obs_observation_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_obs_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.obs_summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_obs_waypoint_query AFTER INSERT OR UPDATE OR DELETE ON smart.obs_waypoint_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_patrol AFTER INSERT OR UPDATE OR DELETE ON smart.patrol FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_patrol_mandate AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_mandate FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_patrol_query AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_patrol_transport AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_transport FOR EACH ROW execute procedure connect.trg_changelog_common();  


CREATE TRIGGER trg_patrol_type AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_type FOR EACH ROW execute procedure connect.trg_patrol_type();
CREATE TRIGGER trg_dm_attribute_list AFTER INSERT OR UPDATE OR DELETE ON smart.dm_attribute_list FOR EACH ROW execute procedure connect.trg_dm_attribute_list();
CREATE TRIGGER trg_dm_attribute_tree AFTER INSERT OR UPDATE OR DELETE ON smart.dm_attribute_tree FOR EACH ROW execute procedure connect.trg_dm_attribute_tree();
CREATE TRIGGER trg_dm_att_agg_map AFTER INSERT OR UPDATE OR DELETE ON smart.dm_att_agg_map FOR EACH ROW execute procedure connect.trg_dm_att_agg_map();
CREATE TRIGGER trg_dm_cat_att_map AFTER INSERT OR UPDATE OR DELETE ON smart.dm_cat_att_map FOR EACH ROW execute procedure connect.trg_dm_cat_att_map();
CREATE TRIGGER trg_i18n_label AFTER INSERT OR UPDATE OR DELETE ON smart.i18n_label FOR EACH ROW execute procedure connect.trg_i18n_label();
CREATE TRIGGER trg_patrol_leg AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_leg FOR EACH ROW execute procedure connect.trg_patrol_leg();
CREATE TRIGGER trg_patrol_leg_day AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_leg_day FOR EACH ROW execute procedure connect.trg_patrol_leg_day();
CREATE TRIGGER trg_patrol_leg_members AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_leg_members FOR EACH ROW execute procedure connect.trg_patrol_leg_members();
CREATE TRIGGER trg_patrol_waypoint AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_waypoint FOR EACH ROW execute procedure connect.trg_patrol_waypoint();
CREATE TRIGGER trg_rank AFTER INSERT OR UPDATE OR DELETE ON smart.rank FOR EACH ROW execute procedure connect.trg_rank();
CREATE TRIGGER trg_report_query AFTER INSERT OR UPDATE OR DELETE ON smart.report_query FOR EACH ROW execute procedure connect.trg_report_query();
CREATE TRIGGER trg_track AFTER INSERT OR UPDATE OR DELETE ON smart.track FOR EACH ROW execute procedure connect.trg_track();
CREATE TRIGGER trg_wp_attachments AFTER INSERT OR UPDATE OR DELETE ON smart.wp_attachments FOR EACH ROW execute procedure connect.trg_wp_attachments();
CREATE TRIGGER trg_wp_observation AFTER INSERT OR UPDATE OR DELETE ON smart.wp_observation FOR EACH ROW execute procedure connect.trg_wp_observation();
CREATE TRIGGER trg_wp_observation_attributes AFTER INSERT OR UPDATE OR DELETE ON smart.wp_observation_attributes FOR EACH ROW execute procedure connect.trg_wp_observation_attributes();
CREATE TRIGGER trg_observation_attachment AFTER INSERT OR UPDATE OR DELETE ON smart.observation_attachment FOR EACH ROW execute procedure connect.trg_observation_attachment();
CREATE TRIGGER trg_cm_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.cm_attribute FOR EACH ROW execute procedure connect.trg_cm_attribute();
CREATE TRIGGER trg_cm_attribute_list AFTER INSERT OR UPDATE OR DELETE ON smart.cm_attribute_list FOR EACH ROW execute procedure connect.trg_cm_attribute_list();
CREATE TRIGGER trg_cm_attribute_option AFTER INSERT OR UPDATE OR DELETE ON smart.cm_attribute_option FOR EACH ROW execute procedure connect.trg_cm_attribute_option();
CREATE TRIGGER trg_cm_attribute_tree_node AFTER INSERT OR UPDATE OR DELETE ON smart.cm_attribute_tree_node FOR EACH ROW execute procedure connect.trg_cm_attribute_tree_node();
CREATE TRIGGER trg_cm_node AFTER INSERT OR UPDATE OR DELETE ON smart.cm_node FOR EACH ROW execute procedure connect.trg_cm_node();
CREATE TRIGGER trg_screen_option_uuid AFTER INSERT OR UPDATE OR DELETE ON smart.screen_option_uuid FOR EACH ROW execute procedure connect.trg_screen_option_uuid();
CREATE TRIGGER trg_cm_attribute_config AFTER INSERT OR UPDATE OR DELETE ON smart.cm_attribute_config FOR EACH ROW execute procedure connect.trg_cm_attribute_config();
CREATE TRIGGER trg_compound_query_layer AFTER INSERT OR UPDATE OR DELETE ON smart.compound_query_layer FOR EACH ROW execute procedure connect.trg_compound_query_layer();



CREATE TRIGGER trg_i_diagram_style AFTER INSERT OR UPDATE OR DELETE ON smart.i_diagram_style FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.trg_i_diagram_entity_type_style() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, a.CA_UUID 
 		FROM smart.i_diagram_style a
 		WHERE a.uuid = ROW.style_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_i_diagram_entity_type_style AFTER INSERT OR UPDATE OR DELETE ON smart.i_diagram_entity_type_style FOR EACH ROW execute procedure connect.trg_i_diagram_entity_type_style();

CREATE OR REPLACE FUNCTION connect.trg_i_diagram_relationship_type_style() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, a.CA_UUID 
 		FROM smart.i_diagram_style a
 		WHERE a.uuid = ROW.style_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_i_diagram_relationship_type_style AFTER INSERT OR UPDATE OR DELETE ON smart.i_diagram_relationship_type_style FOR EACH ROW execute procedure connect.trg_i_diagram_relationship_type_style();


-- EVENTS TRIGGERS
CREATE TRIGGER trg_e_event_filter AFTER INSERT OR UPDATE OR DELETE ON smart.e_event_filter FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_e_action AFTER INSERT OR UPDATE OR DELETE ON smart.e_action FOR EACH ROW execute procedure connect.trg_changelog_common();


CREATE OR REPLACE FUNCTION connect.trg_e_action_parameter_value() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'action_uuid', ROW.action_uuid, 'parameter_key', null, ROW.parameter_key, a.CA_UUID 
 		FROM smart.e_action a
 		WHERE a.uuid = ROW.action_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_e_action_parameter_value AFTER INSERT OR UPDATE OR DELETE ON smart.e_action_parameter_value FOR EACH ROW execute procedure connect.trg_e_action_parameter_value();


CREATE OR REPLACE FUNCTION connect.trg_e_event_action() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, a.CA_UUID 
 		FROM smart.e_action a
 		WHERE a.uuid = ROW.action_uuid;
 	RETURN ROW;
END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_e_event_action AFTER INSERT OR UPDATE OR DELETE ON smart.e_event_action FOR EACH ROW execute procedure connect.trg_e_event_action();

-- Lock the change log table so cannot apply chnages at the same time as sync or packaging conservation area
DROP TRIGGER IF EXISTS trg_connect_account_before ON connect.change_log;
DROP TRIGGER IF EXISTS trg_connect_account_after ON connect.change_log; 
DROP FUNCTION IF EXISTS connect.trg_changelog_before();
DROP FUNCTION IF EXISTS connect.trg_changelog_after();


--If we upgrade to Postgresql 9.6 this function can be removed
--and changed to current_setting('ca.trigger.t' || NEW.ca_uuid, true)
CREATE OR REPLACE FUNCTION connect.dolog(cauuid UUID) RETURNS boolean AS $$
DECLARE
	canrun boolean;
BEGIN
	--check if we should log this ca
	select current_setting('ca.trigger.t' || cauuid) into canrun;
	return canrun;
	EXCEPTION WHEN others THEN
		RETURN TRUE;
END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_changelog_before() RETURNS trigger AS $$
DECLARE
  canlock boolean;
BEGIN
	--check if we should log this ca
	IF (NOT connect.dolog(NEW.ca_uuid)) THEN RETURN NULL; END IF;
	SELECT pg_try_advisory_lock(a.lock_key) into canlock FROM connect.ca_info a WHERE a.ca_uuid = NEW.ca_uuid;
	IF (canlock) THEN return NEW; ELSE RAISE EXCEPTION 'Database Locked to Editing'; END IF;
END$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION connect.trg_changelog_after() RETURNS trigger AS $$
DECLARE
BEGIN
	PERFORM pg_advisory_unlock(a.lock_key) FROM connect.ca_info a WHERE a.ca_uuid = NEW.ca_uuid;
RETURN NEW; END$$ LANGUAGE 'plpgsql';

CREATE  TRIGGER trg_connect_account_before BEFORE INSERT ON connect.change_log  FOR EACH ROW execute procedure connect.trg_changelog_before();
CREATE  TRIGGER trg_connect_account_after AFTER INSERT ON connect.change_log  FOR EACH ROW execute procedure connect.trg_changelog_after();




ALTER TABLE smart.connect_data_queue DROP CONSTRAINT type_chk;
ALTER TABLE connect.data_queue drop constraint type_chk;
update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.connect.dataqueue';
update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.connect.dataqueue';


ALTER TABLE smart.i_entity_attribute_value add column employee_uuid uuid;
ALTER TABLE smart.i_entity_relationship_attribute_value add column employee_uuid uuid;
			
ALTER TABLE smart.i_entity_attribute_value ADD  FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_entity_relationship_attribute_value ADD FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
			
UPDATE smart.employee SET smartuserlevel =  replace(smartuserlevel, 'INTEL_DATA_ENTRY', 'INTEL_RECORD_CREATE,INTEL_RECORD_VIEW,INTEL_RECORD_EDIT,INTEL_ENTITY_VIEW,INTEL_QUERY_ALL') where smartuserlevel is not null and smartuserlevel like '%INTEL_DATA_ENTRY%';
UPDATE connect.connect_plugin_version SET version = '3.0' WHERE plugin_id = 'org.wcs.smart.i2';
UPDATE connect.ca_plugin_version SET version = '3.0' WHERE plugin_id = 'org.wcs.smart.i2';


-- ASSET PLUGIN --qqq
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.asset', '1.0');
insert into connect.ca_plugin_version (ca_uuid, plugin_id, version) select ca_uuid, 'org.wcs.smart.asset', '1.0' from connect.ca_info;

CREATE TABLE smart.asset( 
 uuid uuid NOT NULL, 
 asset_type_uuid uuid NOT NULL, 
 ca_uuid uuid NOT NULL, 
 id varchar(128) NOT NULL, 
 is_retired boolean DEFAULT false NOT NULL, PRIMARY KEY (uuid)
);
ALTER TABLE smart.asset add constraint id_ca_uuid_unq UNIQUE(id, ca_uuid);

CREATE TABLE smart.asset_attribute ( 
 uuid uuid NOT NULL, 
 keyId varchar(128) NOT NULL, 
 type char(8) NOT NULL, 
 ca_uuid uuid NOT NULL, PRIMARY KEY (uuid)
);
ALTER TABLE smart.asset_attribute add constraint keyid_ca_uuid_unq UNIQUE(keyId, ca_uuid);
 
 CREATE TABLE smart.asset_attribute_list_item ( 
 uuid uuid NOT NULL, 
 attribute_uuid uuid NOT NULL, 
 keyid varchar(128) NOT NULL, 
 PRIMARY KEY (uuid) 
);
ALTER TABLE smart.asset_attribute_list_item add constraint asset_li_keyid_attribute_uuid_unq UNIQUE(keyId, attribute_uuid);


CREATE TABLE smart.asset_attribute_value ( 
 asset_uuid uuid NOT NULL, 
 attribute_uuid uuid NOT NULL, 
 string_value varchar(1024), 
 list_item_uuid uuid, 
 double_value1 double precision, 
 double_value2 double precision, 
 PRIMARY KEY (asset_uuid, attribute_uuid)
);

CREATE TABLE smart.asset_deployment ( 
 uuid uuid NOT NULL, 
 asset_uuid uuid NOT NULL, 
 station_location_uuid uuid NOT NULL, 
 start_date timestamp NOT NULL, 
 end_date timestamp, 
 track bytea, 
 PRIMARY KEY (uuid)
);

CREATE TABLE smart.asset_deployment_attribute_value ( 
 asset_deployment_uuid uuid NOT NULL, 
 attribute_uuid uuid NOT NULL, 
 string_value varchar(1024), 
 list_item_uuid uuid, 
 double_value1 double precision,
 double_value2 double precision, 
 PRIMARY KEY (asset_deployment_uuid, attribute_uuid) 
);
  
CREATE TABLE smart.asset_history_record ( 
 uuid uuid NOT NULL, 
 asset_uuid uuid NOT NULL, 
 date timestamp NOT NULL, 
 comment VARCHAR(32672), 
 PRIMARY KEY (uuid)
);

CREATE TABLE smart.asset_module_settings ( 
 uuid uuid NOT NULL, 
 ca_uuid uuid NOT NULL, 
 keyid varchar(128), 
 value varchar(32000), 
 PRIMARY KEY (uuid)
);
ALTER TABLE smart.asset_module_settings add constraint asset_module_key_ca_unq UNIQUE(keyid, ca_uuid);


CREATE TABLE smart.asset_station (
 uuid uuid NOT NULL,
 ca_uuid uuid NOT NULL,
 id varchar(128) NOT NULL, 
 x double precision NOT NULL, 
 y double precision NOT NULL, 
 PRIMARY KEY (uuid)
);
ALTER TABLE smart.asset_station add constraint asset_sn_id_ca_unq UNIQUE(id, ca_uuid);

CREATE TABLE smart.asset_station_attribute ( 
 attribute_uuid uuid NOT NULL, 
 seq_order integer NOT NULL, 
 PRIMARY KEY (attribute_uuid)
);

CREATE TABLE smart.asset_station_attribute_value ( 
 station_uuid uuid NOT NULL, 
 attribute_uuid uuid NOT NULL, 
 string_value varchar(1024), 
 list_item_uuid uuid, 
 double_value1 double precision, 
 double_value2 double precision, 
 PRIMARY KEY (station_uuid, attribute_uuid)
);

CREATE TABLE smart.asset_type ( 
 uuid uuid NOT NULL, 
 ca_uuid uuid NOT NULL, 
 keyid varchar(128), 
 icon bytea, 
 incident_cutoff integer, 
 PRIMARY KEY (uuid)
);
ALTER TABLE smart.asset_type add constraint asset_type_ca_keyid_unq unique(keyid, ca_uuid);


CREATE TABLE smart.asset_type_attribute ( 
 asset_type_uuid uuid NOT NULL, 
 attribute_uuid uuid NOT NULL, 
 seq_order integer NOT NULL, 
 PRIMARY KEY (asset_type_uuid, attribute_uuid)
);

CREATE TABLE smart.asset_type_deployment_attribute ( 
 asset_type_uuid uuid NOT NULL, 
 attribute_uuid uuid NOT NULL, 
 seq_order integer NOT NULL, 
 PRIMARY KEY (asset_type_uuid, attribute_uuid)
);

CREATE TABLE smart.asset_waypoint ( 
 uuid uuid not null, 
 wp_uuid uuid NOT NULL, 
 asset_deployment_uuid uuid NOT NULL, 
 state smallint not null, 
 incident_length integer not null,
 PRIMARY KEY (uuid), 
 UNIQUE(wp_uuid, asset_deployment_uuid)
);

CREATE TABLE smart.asset_waypoint_attachment ( 
 wp_attachment_uuid uuid NOT NULL, 
 asset_waypoint_uuid uuid NOT NULL, 
 PRIMARY KEY (wp_attachment_uuid, asset_waypoint_uuid)
);

CREATE TABLE smart.asset_metadata_mapping (
 uuid uuid not null, 
 ca_uuid uuid not null,
 metadata_type varchar(16) not null, 
 metadata_key varchar(32672) not null, 
 search_order integer not null, 
 asset_field varchar(32), 
 category_uuid uuid,
 attribute_uuid uuid, 
 attribute_list_item_uuid uuid, 
 attribute_tree_node_uuid uuid,  
 PRIMARY KEY (uuid)
);

CREATE TABLE smart.asset_station_location_history (
 uuid uuid NOT NULL, 
 station_location_uuid uuid NOT NULL, 
 date timestamp NOT NULL, 
 comment VARCHAR(32672),
 PRIMARY KEY (uuid)
);

CREATE TABLE smart.asset_station_location ( 
 uuid uuid NOT NULL, 
 station_uuid uuid NOT NULL, 
 id varchar(128) NOT NULL, 
 x double precision NOT NULL, 
 y double precision NOT NULL, 
 PRIMARY KEY (uuid)
);
ALTER TABLE smart.asset_station_location add constraint asset_snlc_id_ca_unq UNIQUE(id, station_uuid);

CREATE TABLE smart.asset_station_location_attribute ( 
 attribute_uuid uuid NOT NULL, 
 seq_order integer NOT NULL, 
 PRIMARY KEY (attribute_uuid)
);

CREATE TABLE smart.asset_station_location_attribute_value ( 
 station_location_uuid uuid NOT NULL, 
 attribute_uuid uuid NOT NULL, 
 string_value varchar(1024), 
 list_item_uuid uuid, 
 double_value1 double precision, 
 double_value2 double precision, 
 PRIMARY KEY (station_location_uuid, attribute_uuid)
);

CREATE TABLE smart.asset_map_style ( 
 uuid uuid NOT NULL, 
 ca_uuid uuid NOT NULL, 
 name varchar(1024), 
 style_string varchar(32672), 
 PRIMARY KEY (uuid)
);

ALTER TABLE smart.asset_station ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_location_history ADD FOREIGN KEY (station_location_uuid) REFERENCES smart.asset_station_location(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_location ADD FOREIGN KEY (station_uuid) REFERENCES smart.asset_station(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;			
ALTER TABLE smart.asset_station_location_attribute_value ADD FOREIGN KEY (station_location_uuid) REFERENCES smart.asset_station_location(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_location_attribute_value ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_attribute_value ADD FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_attribute_value ADD FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_deployment ADD FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_history_record ADD FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_attribute_list_item ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_attribute_value ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_deployment_attribute_value ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_attribute ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_location_attribute ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_attribute_value ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_type_attribute ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_type_deployment_attribute ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_attribute_value ADD FOREIGN KEY (list_item_uuid) REFERENCES smart.asset_attribute_list_item (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_deployment_attribute_value ADD FOREIGN KEY (list_item_uuid) REFERENCES smart.asset_attribute_list_item (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_attribute_value ADD FOREIGN KEY (list_item_uuid) REFERENCES smart.asset_attribute_list_item (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_deployment_attribute_value ADD FOREIGN KEY (asset_deployment_uuid) REFERENCES smart.asset_deployment (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_waypoint ADD FOREIGN KEY (asset_deployment_uuid) REFERENCES smart.asset_deployment (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_deployment ADD FOREIGN KEY (station_location_uuid) REFERENCES smart.asset_station_location (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_station_attribute_value ADD FOREIGN KEY (station_uuid) REFERENCES smart.asset_station (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset ADD FOREIGN KEY (asset_type_uuid) REFERENCES smart.asset_type (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_type_attribute ADD FOREIGN KEY (asset_type_uuid) REFERENCES smart.asset_type (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_type_deployment_attribute ADD FOREIGN KEY  (asset_type_uuid) REFERENCES smart.asset_type (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_attribute ADD FOREIGN KEY  (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_module_settings ADD FOREIGN KEY(ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_type ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_waypoint ADD FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_deployment ADD FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_metadata_mapping ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_metadata_mapping ADD FOREIGN KEY (category_uuid) REFERENCES smart.dm_category (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_metadata_mapping ADD FOREIGN KEY (attribute_uuid) REFERENCES smart.dm_attribute (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_metadata_mapping ADD FOREIGN KEY (attribute_list_item_uuid) REFERENCES smart.dm_attribute_list (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_metadata_mapping ADD FOREIGN KEY (attribute_tree_node_uuid) REFERENCES smart.dm_attribute_tree (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_waypoint_attachment ADD FOREIGN KEY (asset_waypoint_uuid) REFERENCES smart.asset_waypoint (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_waypoint_attachment ADD FOREIGN KEY (wp_attachment_uuid) REFERENCES smart.wp_attachments (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.asset_map_style ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_asset AFTER INSERT OR UPDATE OR DELETE ON smart.asset FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.asset_attribute FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_module_settings AFTER INSERT OR UPDATE OR DELETE ON smart.asset_module_settings FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_station AFTER INSERT OR UPDATE OR DELETE ON smart.asset_station FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_type AFTER INSERT OR UPDATE OR DELETE ON smart.asset_type FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_metadata_mapping AFTER INSERT OR UPDATE OR DELETE ON smart.asset_metadata_mapping FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_map_style AFTER INSERT OR UPDATE OR DELETE ON smart.asset_map_style FOR EACH ROW execute procedure connect.trg_changelog_common();


CREATE OR REPLACE FUNCTION connect.trg_asset_attribute_list_item() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_attribute_list_item AFTER INSERT OR UPDATE OR DELETE ON smart.asset_attribute_list_item FOR EACH ROW execute procedure connect.trg_asset_attribute_list_item();


CREATE OR REPLACE FUNCTION connect.trg_asset_attribute_value() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'asset_uuid', ROW.asset_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.asset_attribute_value FOR EACH ROW execute procedure connect.trg_asset_attribute_value();


CREATE OR REPLACE FUNCTION connect.trg_asset_deployment() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.asset  i WHERE i.uuid = ROW.asset_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_deployment AFTER INSERT OR UPDATE OR DELETE ON smart.asset_deployment FOR EACH ROW execute procedure connect.trg_asset_deployment();


CREATE OR REPLACE FUNCTION connect.trg_asset_deployment_attribute_value() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'asset_deployment_uuid', ROW.asset_deployment_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_deployment_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.asset_deployment_attribute_value FOR EACH ROW execute procedure connect.trg_asset_deployment_attribute_value();




CREATE OR REPLACE FUNCTION connect.trg_asset_history_record() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.asset i WHERE i.uuid = ROW.asset_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_history_record AFTER INSERT OR UPDATE OR DELETE ON smart.asset_history_record FOR EACH ROW execute procedure connect.trg_asset_history_record();



CREATE OR REPLACE FUNCTION connect.trg_asset_station_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'attribute_uuid', ROW.attribute_uuid, null, null, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_station_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.asset_station_attribute FOR EACH ROW execute procedure connect.trg_asset_station_attribute();


CREATE OR REPLACE FUNCTION connect.trg_asset_station_attribute_value() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'station_uuid', ROW.station_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_station_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.asset_station_attribute_value FOR EACH ROW execute procedure connect.trg_asset_station_attribute_value();


CREATE OR REPLACE FUNCTION connect.trg_asset_type_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'asset_type_uuid', ROW.asset_type_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_type_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.asset_type_attribute FOR EACH ROW execute procedure connect.trg_asset_type_attribute();
CREATE TRIGGER trg_asset_type_deployment_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.asset_type_deployment_attribute FOR EACH ROW execute procedure connect.trg_asset_type_attribute();


CREATE OR REPLACE FUNCTION connect.trg_asset_waypoint() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.waypoint i WHERE i.uuid = ROW.wp_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_waypoint AFTER INSERT OR UPDATE OR DELETE ON smart.asset_waypoint FOR EACH ROW execute procedure connect.trg_asset_waypoint();


CREATE OR REPLACE FUNCTION connect.trg_asset_waypoint_attachment() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'wp_attachment_uuid', ROW.wp_attachment_uuid, 'asset_waypoint_uuid', ROW.asset_waypoint_uuid, null, i.CA_UUID 
 		from smart.asset_waypoint wp, smart.waypoint i WHERE i.uuid = wp.wp_uuid and wp.uuid = ROW.asset_waypoint_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_waypoint_attachment AFTER INSERT OR UPDATE OR DELETE ON smart.asset_waypoint_attachment FOR EACH ROW execute procedure connect.trg_asset_waypoint_attachment();

CREATE OR REPLACE FUNCTION connect.trg_asset_station_location_history() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.asset_station_location loc, smart.asset_station i WHERE i.uuid = loc.station_uuid and loc.uuid = ROW.station_location_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_station_location_history AFTER INSERT OR UPDATE OR DELETE ON smart.asset_station_location_history FOR EACH ROW execute procedure connect.trg_asset_station_location_history();



CREATE OR REPLACE FUNCTION connect.trg_asset_station_location() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, i.CA_UUID 
 		from smart.asset_station i WHERE i.uuid = ROW.station_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_station_location AFTER INSERT OR UPDATE OR DELETE ON smart.asset_station_location FOR EACH ROW execute procedure connect.trg_asset_station_location();


CREATE OR REPLACE FUNCTION connect.trg_asset_station_location_attribute() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'attribute_uuid', ROW.attribute_uuid, null, null, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_station_location_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.asset_station_location_attribute FOR EACH ROW execute procedure connect.trg_asset_station_location_attribute();



CREATE OR REPLACE FUNCTION connect.trg_asset_station_location_attribute_value() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'station_location_uuid', ROW.station_location_uuid, 'attribute_uuid', ROW.attribute_uuid, null, i.CA_UUID 
 		from smart.asset_attribute i WHERE i.uuid = ROW.attribute_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_station_location_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.asset_station_location_attribute_value FOR EACH ROW execute procedure connect.trg_asset_station_location_attribute_value();


-- Asset Queries 

insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.asset.query', '1.0');
insert into connect.ca_plugin_version (ca_uuid, plugin_id, version) select ca_uuid, 'org.wcs.smart.asset.query', '1.0' from connect.ca_info;

CREATE TABLE SMART.ASSET_OBSERVATION_QUERY(
 UUID UUID NOT NULL,
 ID VARCHAR(6) NOT NULL,
 CREATOR_UUID UUID NOT NULL,
 QUERY_FILTER VARCHAR(32672),
 CA_FILTER VARCHAR(32672),
 CA_UUID UUID NOT NULL,
 FOLDER_UUID UUID,
 COLUMN_FILTER VARCHAR(32672),
 STYLE VARCHAR,
 SHARED BOOLEAN NOT NULL,
 SHOW_DATA_COLUMNS_ONLY BOOLEAN,
 PRIMARY KEY (UUID)
);
 
CREATE TABLE SMART.ASSET_WAYPOINT_QUERY(
 UUID UUID NOT NULL,
 ID VARCHAR(6) NOT NULL,
 CREATOR_UUID UUID NOT NULL,
 QUERY_FILTER VARCHAR(32672),
 CA_FILTER VARCHAR(32672),
 CA_UUID UUID NOT NULL,
 FOLDER_UUID UUID,
 COLUMN_FILTER VARCHAR(32672),
 SURVEYDESIGN_KEY VARCHAR(128),
 SHARED BOOLEAN NOT NULL,
 STYLE  VARCHAR,
 PRIMARY KEY (UUID)
);


CREATE TABLE SMART.ASSET_SUMMARY_QUERY(
 UUID UUID NOT NULL,
 ID VARCHAR(6) NOT NULL, 
 CREATOR_UUID UUID NOT NULL,
 QUERY_DEF VARCHAR(32672),
 CA_FILTER VARCHAR(32672),
 CA_UUID UUID NOT NULL,
 FOLDER_UUID UUID, 
 SHARED BOOLEAN NOT NULL, 
 STYLE  VARCHAR,
 PRIMARY KEY (UUID)
);


ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;  				
ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;   				
ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;				

CREATE TRIGGER trg_asset_observation_query AFTER INSERT OR UPDATE OR DELETE ON smart.asset_observation_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_waypoint_query AFTER INSERT OR UPDATE OR DELETE ON smart.asset_waypoint_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_asset_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.asset_summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();

-- updates for patrol plug in
CREATE TABLE smart.patrol_folder (
	uuid uuid not null, 
	ca_uuid uuid not null, 
	parent_uuid uuid, 
	folder_order smallint, 
	primary key (uuid)
);
ALTER TABLE smart.patrol_folder ADD FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.patrol_folder ADD FOREIGN KEY (PARENT_UUID) REFERENCES SMART.PATROL_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.patrol ADD COLUMN folder_uuid UUID;
ALTER TABLE smart.patrol ADD FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.PATROL_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_patrol_folder AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_folder FOR EACH ROW execute procedure connect.trg_changelog_common();

--UPDATE FUNCTION TO Work with linestring or multilinestring Tracks
--linestring might be a linestring or multi line string
CREATE OR REPLACE FUNCTION smart.computeHoursPoly(polygon bytea, linestring bytea) RETURNS double precision AS $$
DECLARE
  ls geometry;
  p geometry;
  value double precision;
  ctime double precision;
  clength double precision;
  i integer;
  pnttemp geometry;
  pnttemp2 geometry;
  lstemp geometry;
BEGIN
	ls := st_geomfromwkb(linestring);
	p := st_geomfromwkb(polygon);
	
	IF (UPPER(st_geometrytype(ls)) = 'ST_MULTILINESTRING' ) THEN
		ctime = 0;
		FOR i in 1..ST_NumGeometries(ls) LOOP
			ctime := ctime + smart.computeHoursPoly(polygon, st_geometryn(ls, i));
		END LOOP;
		RETURN ctime;
	END IF;
	
	--wholly contained use entire time
	IF not st_isvalid(ls) and st_length(ls) = 0 THEN
		pnttemp = st_pointn(ls, 1);
		IF (smart.pointinpolygon(st_x(pnttemp),st_y(pnttemp), p)) THEN
			RETURN (st_z(st_endpoint(ls)) - st_z(st_startpoint(ls))) / 3600000.0;
		END IF;
		RETURN 0;
	END IF;
	
	IF (st_contains(p, ls)) THEN
		return (st_z(st_endpoint(ls)) - st_z(st_startpoint(ls))) / 3600000.0;
	END IF;
	
	value := 0;
	FOR i in 1..ST_NumPoints(ls)-1 LOOP
		pnttemp := st_pointn(ls, i);
		pnttemp2 := st_pointn(ls, i+1);
		lstemp := st_makeline(pnttemp, pnttemp2);	
		IF (NOT st_intersects(st_envelope(ls), st_envelope(lstemp))) THEN
			--do nothing; outside envelope
		ELSE
			IF (ST_COVERS(p, lstemp)) THEN
				value := value + st_z(pnttemp2) - st_z(pnttemp);
			ELSIF (ST_INTERSECTS(p, lstemp)) THEN
				ctime := st_z(pnttemp2) - st_z(pnttemp);
				clength := st_distance(pnttemp, pnttemp2);
				IF (clength = 0) THEN
					--points are the same and intersect so include the entire time
					value := value + ctime;
				ELSE
					--part in part out so linearly interpolate
					value := value + (ctime * (st_length(st_intersection(p, lstemp)) / clength));
				END IF;
			END IF;
		END IF;
	END LOOP;
	RETURN value / 3600000.0;
END;
$$LANGUAGE plpgsql;

-- also update to support track multilinestrings
CREATE OR REPLACE FUNCTION smart.trackIntersects(geom1 bytea, geom2 bytea) RETURNS BOOLEAN AS $$
DECLARE
  ls geometry;
  pnt geometry;
BEGIN
	ls := st_geomfromwkb(geom1);
	
	IF (UPPER(st_geometrytype(ls)) = 'ST_MULTILINESTRING' ) THEN
		FOR i in 1..ST_NumGeometries(ls) LOOP
			IF (smart.trackIntersects(st_geometryn(ls, i), geom2)) THEN
				RETURN true;
			END IF;
		END LOOP;
	END IF;
	if not st_isvalid(ls) and st_length(ls) = 0 then
		pnt = st_pointn(ls, 1);
		return smart.pointinpolygon(st_x(pnt),st_y(pnt),geom2);
	else
		RETURN ST_INTERSECTS(ls, st_geomfromwkb(geom2));
	end if;

END;
$$LANGUAGE plpgsql;


--GFW
CREATE TABLE connect.gfw(
	uuid uuid not null,
	alert_uuid uuid not null,
	last_data timestamp,
	creator_uuid uuid not null,
	level smallint not null,
	primary key (uuid)
);
ALTER TABLE connect.gfw ADD FOREIGN KEY (alert_uuid) REFERENCES connect.alert_types(uuid);
ALTER TABLE connect.gfw ADD FOREIGN KEY (creator_uuid) REFERENCES connect.users(uuid); 

-- DROP not null constraint from Conervation Area of alerts for global forest watch alerts
--which will not have a conservation area
alter table connect.alerts alter column ca_uuid drop not null;
--add not null constraint to alerts
-- at a minimum this should be set to [[x,y]]
alter table connect.alerts alter column track set not null;


ALTER TABLE connect.ca_info DROP CONSTRAINT status_chk;

-- UPDATE VERSION
ALTER TABLE connect.connect_version ADD COLUMN filestore_version varchar(5) default '-1';

UPDATE connect.connect_plugin_version SET version = '6.0.0' WHERE plugin_id = 'org.wcs.smart';
UPDATE connect.ca_plugin_version SET version = '6.0.0' WHERE plugin_id = 'org.wcs.smart';

update connect.ca_plugin_version set plugin_id = 'org.wcs.smart.cybertracker.survey' where plugin_id = 'org.wcs.smart.connect.dataqueue.cybertracker.survey';
update connect.ca_plugin_version set plugin_id = 'org.wcs.smart.cybertracker.patrol' where plugin_id = 'org.wcs.smart.connect.dataqueue.cybertracker.patrol';
update connect.connect_plugin_version set plugin_id = 'org.wcs.smart.cybertracker.survey' where plugin_id = 'org.wcs.smart.connect.dataqueue.cybertracker.survey';
update connect.connect_plugin_version set plugin_id = 'org.wcs.smart.cybertracker.patrol' where plugin_id = 'org.wcs.smart.connect.dataqueue.cybertracker.patrol';

--database version
update connect.connect_version set version = '6.0.0';
--flag the filestore as not upgraded; this will require administrator to upgrade before you can login
update connect.connect_version set filestore_version = '5.0.0';
