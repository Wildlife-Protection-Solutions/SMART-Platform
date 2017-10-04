ALTER TABLE smart.employee ADD COLUMN usertemp VARCHAR(5000);
UPDATE smart.employee set usertemp = case when smartuserlevel = 0 THEN 'ADMIN' when smartuserlevel = 1 THEN 'DATAENTRY' WHEN smartuserlevel = 2  THEN 'ANALYST' when smartuserlevel=3 THEN 'MANAGER' ELSE null END;
ALTER TABLE smart.employee DROP COLUMN smartuserlevel;
ALTER TABLE smart.employee ADD COLUMN smartuserlevel VARCHAR(5000);
UPDATE smart.employee SET smartuserlevel = usertemp;
ALTER TABLE smart.employee DROP COLUMN usertemp;

alter table smart.CONFIGURABLE_MODEL ADD COLUMN instant_gps BOOLEAN;
alter table smart.CONFIGURABLE_MODEL ADD COLUMN photo_first BOOLEAN;

alter table connect.shared_links ADD COLUMN is_user_token BOOLEAN NOT NULL DEFAULT FALSE;
alter table connect.shared_links ADD COLUMN allowed_ip VARCHAR(24);
alter table connect.shared_links ADD COLUMN date_created timestamp NOT Null DEFAULT now();
ALTER TABLE connect.shared_links ALTER COLUMN url DROP NOT null;

CREATE OR REPLACE FUNCTION smart.trackIntersects(geom1 bytea, geom2 bytea) RETURNS BOOLEAN AS $$
DECLARE
  ls geometry;
  pnt geometry;
BEGIN
	ls := st_geomfromwkb(geom1);
	if not st_isvalid(ls) and st_length(ls) = 0 then
		pnt = st_pointn(ls, 1);
		return smart.pointinpolygon(st_x(pnt),st_y(pnt),geom2);
	else
		RETURN ST_INTERSECTS(ls, st_geomfromwkb(geom2));
	end if;

END;
$$LANGUAGE plpgsql;


--Below are all the tables to support new intelligence plugin
CREATE TABLE smart.i_attachment
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	date_created timestamp NOT NULL,
	created_by uuid NOT NULL,
	description varchar(2048),
	filename varchar(1024) NOT NULL,
	PRIMARY KEY (uuid)
);
	
CREATE TABLE smart.i_attribute
(
	uuid uuid NOT NULL,
	keyid varchar(128) NOT NULL,
	type char(8) NOT NULL,
	ca_uuid uuid NOT NULL,
	PRIMARY KEY (uuid)
);
	
CREATE TABLE smart.i_attribute_list_item
(
	uuid uuid NOT NULL,
	attribute_uuid uuid NOT NULL,
	keyid varchar(128) NOT NULL,
	PRIMARY KEY (uuid)
);
	
CREATE TABLE smart.i_entity
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	date_created timestamp NOT NULL,
	date_modified timestamp,
	created_by uuid NOT NULL,
	last_modified_by uuid,
	primary_attachment_uuid uuid,
	entity_type_uuid uuid NOT NULL,
	comment varchar,
	PRIMARY KEY (uuid)
);
	
CREATE TABLE smart.i_entity_attachment
(
	entity_uuid uuid NOT NULL,
	attachment_uuid uuid NOT NULL,
	PRIMARY KEY (entity_uuid, attachment_uuid)
);
 	
CREATE TABLE smart.i_entity_attribute_value
(
	entity_uuid uuid NOT NULL,
	attribute_uuid uuid NOT NULL,
	string_value varchar(1024),
	double_value double precision,
	double_value2 double precision,
	list_item_uuid uuid,
	metaphone varchar(32600),
 	PRIMARY KEY (entity_uuid, attribute_uuid)
);

CREATE TABLE smart.i_entity_location
(
	entity_uuid uuid NOT NULL,
	location_uuid uuid NOT NULL,
	PRIMARY KEY (entity_uuid,location_uuid)
);

CREATE TABLE smart.i_entity_record
(
	entity_uuid uuid NOT NULL,
	record_uuid uuid NOT NULL,
	PRIMARY KEY (entity_uuid,record_uuid)
);

CREATE TABLE smart.i_entity_relationship
(
	uuid uuid NOT NULL,
	src_entity_uuid uuid NOT NULL,
	relationship_type_uuid uuid NOT NULL,
	target_entity_uuid uuid NOT NULL,
	source varchar(16) not null,
	source_uuid uuid,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_entity_relationship_attribute_value
(
	entity_relationship_uuid uuid NOT NULL,
	attribute_uuid uuid NOT NULL,
	string_value varchar(1024),
	double_value double precision,
	double_value2 double precision,
	list_item_uuid uuid,
	PRIMARY KEY (entity_relationship_uuid,attribute_uuid)
);

CREATE TABLE smart.i_entity_search(
	uuid uuid NOT NULL,
	search_string varchar,
	ca_uuid uuid NOT NULL,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_entity_type(
	uuid uuid NOT NULL,
	keyid varchar(128) NOT NULL,
	ca_uuid uuid NOT NULL,
	id_attribute_uuid uuid NOT NULL,
	icon bytea,
	birt_template varchar(4096),
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_entity_type_attribute
(
	entity_type_uuid uuid NOT NULL,
	attribute_uuid uuid NOT NULL,
	attribute_group_uuid uuid,
	seq_order integer not null,
	PRIMARY KEY (entity_type_uuid, attribute_uuid)
);

CREATE TABLE smart.i_entity_type_attribute_group
(
	uuid uuid NOT NULL,
	entity_type_uuid uuid not null,
	seq_order integer not null,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_location
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	geometry bytea NOT NULL,
	datetime timestamp,
	comment varchar(4096),
	id varchar(1028),
	record_uuid uuid,
	PRIMARY KEY (uuid)
);
CREATE TABLE smart.i_observation
(
	uuid uuid NOT NULL,
	location_uuid uuid NOT NULL,
	category_uuid uuid ,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_observation_attribute
(
	observation_uuid uuid NOT NULL,
	attribute_uuid uuid NOT NULL,
	list_element_uuid uuid,
	tree_node_uuid uuid,
	string_value varchar(1024),
	double_value double precision,
	PRIMARY KEY (observation_uuid, attribute_uuid)
);

CREATE TABLE smart.i_record
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	source_uuid uuid,
	title varchar(1024) NOT NULL,
	date_created timestamp NOT NULL,
	last_modified_date timestamp,
	created_by uuid NOT NULL,
	last_modified_by uuid,
	date_exported timestamp,
	status varchar(16) NOT NULL,
	description varchar,
	comment varchar,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_record_attachment
(
	record_uuid uuid NOT NULL,
	attachment_uuid uuid NOT NULL,
	PRIMARY KEY (record_uuid, attachment_uuid)
);

CREATE TABLE smart.i_record_obs_query
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	style varchar,
	query_string varchar,
	column_filter varchar,
	date_created timestamp NOT NULL,
	last_modified_date timestamp,
	created_by uuid NOT NULL,
	last_modified_by uuid,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_relationship_type_attribute
(
	relationship_type_uuid uuid NOT NULL,
	attribute_uuid uuid NOT NULL,
	seq_order integer not null,
	PRIMARY KEY (relationship_type_uuid, attribute_uuid)
);

CREATE TABLE smart.i_relationship_group
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	keyid varchar(128) NOT NULL,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_relationship_type
(
	uuid uuid NOT NULL,
	keyid varchar(128) NOT NULL,
	ca_uuid uuid NOT NULL,
	icon bytea,
	relationship_group_uuid uuid,
	src_entity_type uuid,
	target_entity_type uuid,
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_working_set
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	date_created timestamp NOT NULL,
	last_modified_date timestamp,
	created_by uuid NOT NULL,
	last_modified_by uuid,
	entity_date_filter varchar(1024),
	PRIMARY KEY (uuid)
);

CREATE TABLE smart.i_working_set_entity
(
	working_set_uuid uuid NOT NULL,
	entity_uuid uuid NOT NULL,
	map_style varchar,
	is_visible boolean not null default true,
	PRIMARY KEY (working_set_uuid, entity_uuid)
);

CREATE TABLE smart.i_working_set_query
(
	working_set_uuid uuid NOT NULL,
	query_uuid uuid NOT NULL,
	date_filter varchar(1024),
	map_style varchar,
	is_visible boolean not null default true,
	PRIMARY KEY (working_set_uuid, query_uuid)
);

CREATE TABLE smart.i_working_set_record
(
	working_set_uuid uuid NOT NULL,
	record_uuid uuid NOT NULL,
	map_style varchar,
	is_visible boolean not null default true,
	PRIMARY KEY (working_set_uuid, record_uuid)
);

CREATE TABLE smart.i_record_attribute_value
(
	uuid uuid NOT NULL,
	record_uuid uuid NOT NULL,
	attribute_uuid uuid NOT NULL,
	string_value varchar(1024),
	double_value double precision,
	double_value2 double precision,
	PRIMARY KEY (uuid),
	UNIQUE(record_uuid, attribute_uuid)
);
 
CREATE TABLE smart.i_record_attribute_value_list
(
	value_uuid uuid not null,
	element_uuid uuid not null,
	primary key (value_uuid, element_uuid)
);

CREATE TABLE smart.i_recordsource_attribute
(
	uuid uuid,
	source_uuid uuid NOT NULL,
	attribute_uuid uuid,
	entity_type_uuid uuid,
	seq_order integer,
	is_multi boolean,
	PRIMARY KEY(uuid),
	UNIQUE (source_uuid, attribute_uuid, entity_type_uuid)
);

CREATE TABLE smart.i_recordsource 
(
	uuid uuid not null,
	ca_uuid uuid not null,
	keyid varchar(128) not null,
	icon bytea,
	PRIMARY KEY (uuid)
);


--FOREIGN KEYs

ALTER TABLE smart.i_location 
ADD CONSTRAINT ilocation_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_location 
ADD CONSTRAINT location_recorduuid_fk 
FOREIGN KEY (record_uuid) 
REFERENCES smart.i_record (uuid) ON DELETE CASCADE DEFERRABLE ;

ALTER TABLE smart.i_entity_search 
ADD CONSTRAINT ientitysearch_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE ;

ALTER TABLE smart.i_attribute 
ADD CONSTRAINT iattribute_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE; 

ALTER TABLE smart.i_record 
ADD CONSTRAINT irecord_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record 
ADD CONSTRAINT irecord_createdby_fk 
FOREIGN KEY (created_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record 
ADD CONSTRAINT irecord_modifiedby_fk 
FOREIGN KEY (lasT_modified_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_type 
ADD CONSTRAINT ientitytype_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_type 
ADD CONSTRAINT ientitytype_idattributeuuid_fk 
FOREIGN KEY (id_attribute_uuid) 
REFERENCES smart.i_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_attachment 
ADD CONSTRAINT iattachment_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_attachment 
ADD CONSTRAINT iattachment_createdby_fk 
FOREIGN KEY (created_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_relationship_type 
ADD CONSTRAINT irelationshiptype_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set 
ADD CONSTRAINT iworkingset_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set 
ADD CONSTRAINT iworkingset_createdby_fk 
FOREIGN KEY (created_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set 
ADD CONSTRAINT iworkingset_lastmodifiedby_fk 
FOREIGN KEY (last_modified_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity 
ADD CONSTRAINT ientity_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_obs_query 
ADD CONSTRAINT irecordquery_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_obs_query 
ADD CONSTRAINT irecordquery_createdby_fk 
FOREIGN KEY (created_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_obs_query 
ADD CONSTRAINT irecordquery_modifiedby_fk 
FOREIGN KEY (last_modified_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_observation_attribute 
ADD CONSTRAINT iobservationattribute_attributeuuid_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.DM_ATTRIBUTE (UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_observation_attribute 
ADD CONSTRAINT iobservationattribute_list_fk 
FOREIGN KEY (list_element_uuid) 
REFERENCES smart.DM_ATTRIBUTE_LIST (UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_observation_attribute 
ADD CONSTRAINT iobservationattribute_tree_fk 
FOREIGN KEY (tree_node_uuid) 
REFERENCES smart.DM_ATTRIBUTE_TREE (UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_attachment 
ADD CONSTRAINT irecordattachment_attchment_fk 
FOREIGN KEY (attachment_uuid) 
REFERENCES smart.i_attachment (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_attachment 
ADD CONSTRAINT ientityattachment_attchment_fk 
FOREIGN KEY (attachment_uuid) 
REFERENCES smart.i_attachment (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_attribute_value 
ADD CONSTRAINT ientityattribute_attribute_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.i_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_attribute_list_item 
ADD CONSTRAINT iattributelist_attribute_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.i_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_relationship_type_attribute 
ADD CONSTRAINT irelationshipattribute_attribute_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.i_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_type_attribute 
ADD CONSTRAINT ientitytypeattribute_attribute_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.i_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_type_attribute 
ADD CONSTRAINT iattributegroupuuid_fk 
FOREIGN KEY (attribute_group_uuid) 
REFERENCES smart.i_entity_type_attribute_group (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_type_attribute_group 
ADD CONSTRAINT ientitytypeattributegroupentitytypeuuid_fk 
FOREIGN KEY (entity_type_uuid) 
REFERENCES smart.i_entity_type (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_relationship_attribute_value 
ADD CONSTRAINT ientityrelationshipattribute_attribute_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.i_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_relationship_attribute_value 
ADD CONSTRAINT ientityrelationshipattribute_list_fk 
FOREIGN KEY (list_item_uuid) 
REFERENCES smart.i_attribute_list_item (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_attribute_value 
ADD CONSTRAINT ientityattributevalue_list_fk 
FOREIGN KEY (list_item_uuid) 
REFERENCES smart.i_attribute_list_item (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_relationship 
ADD CONSTRAINT ientityrelationship_srcentity_fk 
FOREIGN KEY (src_entity_uuid) 
REFERENCES smart.i_entity (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_relationship 
ADD CONSTRAINT ientityrelationship_targetentity_fk 
FOREIGN KEY (target_entity_uuid) 
REFERENCES smart.i_entity (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_record 
ADD CONSTRAINT ientityrecord_entity_fk 
FOREIGN KEY (entity_uuid) 
REFERENCES smart.i_entity (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set_entity 
ADD CONSTRAINT iworkingsetentity_entity_fk 
FOREIGN KEY (entity_uuid) 
REFERENCES smart.i_entity (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_attribute_value 
ADD CONSTRAINT ientityattributevalue_entity_fk 
FOREIGN KEY (entity_uuid) 
REFERENCES smart.i_entity (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_attachment 
ADD CONSTRAINT ientityattachment_entity_fk 
FOREIGN KEY (entity_uuid) 
REFERENCES smart.i_entity (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_location 
ADD CONSTRAINT ientitylocation_entity_fk 
FOREIGN KEY (entity_uuid) 
REFERENCES smart.i_entity (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_relationship_attribute_value 
ADD CONSTRAINT ientityrelationshipattribute_entityrelationship_fk 
FOREIGN KEY (entity_relationship_uuid) 
REFERENCES smart.i_entity_relationship (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_type_attribute 
ADD CONSTRAINT ientitytypeattribute_entitytype_fk 
FOREIGN KEY (entity_type_uuid) 
REFERENCES smart.i_entity_type (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity 
ADD CONSTRAINT ientity_entitytype_fk 
FOREIGN KEY (entity_type_uuid) 
REFERENCES smart.i_entity_type (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity 
ADD CONSTRAINT ientity_createdby_fk 
FOREIGN KEY (created_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity 
ADD CONSTRAINT ientity_lastmodifiedby_fk 
FOREIGN KEY (last_modified_by) 
REFERENCES smart.employee (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_location 
ADD CONSTRAINT ientitylocation_location_fk 
FOREIGN KEY (location_uuid) 
REFERENCES smart.i_location (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_observation 
ADD CONSTRAINT iobservation_location_fk 
FOREIGN KEY (location_uuid) 
REFERENCES smart.i_location (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_observation 
ADD CONSTRAINT iobservation_category_fk 
FOREIGN KEY (category_uuid) 
REFERENCES smart.dm_category (uuid) ON DELETE CASCADE DEFERRABLE; 

ALTER TABLE smart.i_observation_attribute 
ADD CONSTRAINT iobservationattribute_observation_fk 
FOREIGN KEY (observation_uuid) 
REFERENCES smart.i_observation (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_record 
ADD CONSTRAINT ientityrecord_record_fk 
FOREIGN KEY (record_uuid) 
REFERENCES smart.i_record (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set_record 
ADD CONSTRAINT iworkingsetrecord_record_fk 
FOREIGN KEY (record_uuid) 
REFERENCES smart.i_record (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_attachment 
ADD CONSTRAINT irecordattachment_record_fk 
FOREIGN KEY (record_uuid) 
REFERENCES smart.i_record (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set_query 
ADD CONSTRAINT iworkingsetquery_query_fk 
FOREIGN KEY (query_uuid) 
REFERENCES smart.i_record_obs_query (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_relationship_type 
ADD CONSTRAINT irelationshiptype_group_fk 
FOREIGN KEY (relationship_group_uuid) 
REFERENCES smart.i_relationship_group (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_relationship_type_attribute 
ADD CONSTRAINT irelationshipattribute_type_fk 
FOREIGN KEY (relationship_type_uuid) 
REFERENCES smart.i_relationship_type (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.I_RELATIONSHIP_TYPE 
ADD CONSTRAINT I_RELATIONSHIP_TYPE_SRC_TYPE_FK  
FOREIGN KEY (src_entity_type) 
REFERENCES smart.I_ENTITY_TYPE(uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.I_RELATIONSHIP_TYPE 
ADD CONSTRAINT I_RELATIONSHIP_TYPE_TRG_TYPE_FK  
FOREIGN KEY (target_entity_type) 
REFERENCES smart.I_ENTITY_TYPE(uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_entity_relationship 
ADD CONSTRAINT ientityrelationship_type_fk 
FOREIGN KEY (relationship_type_uuid) 
REFERENCES smart.i_relationship_type (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set_query 
ADD CONSTRAINT iworkingsetquery_workingset_fk 
FOREIGN KEY (working_set_uuid) 
REFERENCES smart.i_working_set (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set_record 
ADD CONSTRAINT iworkingsetrecord_workingset_fk 
FOREIGN KEY (working_set_uuid) 
REFERENCES smart.i_working_set (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_working_set_entity 
ADD CONSTRAINT iworkginsetentity_workingset_fk 
FOREIGN KEY (working_set_uuid) 
REFERENCES smart.i_working_set (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_relationship_group 
ADD CONSTRAINT relationshipgroup_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_recordsource 
ADD CONSTRAINT irecordsource_cauuid_fk 
FOREIGN KEY (ca_uuid) 
REFERENCES smart.conservation_area (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_recordsource_attribute 
ADD CONSTRAINT irecordsourceattribute_sourceuuid_fk 
FOREIGN KEY (source_uuid) 
REFERENCES smart.i_recordsource (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_recordsource_attribute 
ADD CONSTRAINT irecordsourceattribute_attributeuuid_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.i_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_recordsource_attribute 
ADD CONSTRAINT irecordsourceattribute_entitytypeuuid_fk 
FOREIGN KEY (entity_type_uuid) 
REFERENCES smart.i_entity_type (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_attribute_value 
ADD CONSTRAINT irecordattvalue_sourceuuid_fk 
FOREIGN KEY (record_uuid) 
REFERENCES smart.i_record (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_attribute_value 
ADD CONSTRAINT irecordattvalue_attributeuuid_fk 
FOREIGN KEY (attribute_uuid) 
REFERENCES smart.i_recordsource_attribute (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record 
ADD CONSTRAINT irecord_sourceuuid_fk 
FOREIGN KEY (source_uuid) 
REFERENCES smart.i_recordsource (uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.i_record_attribute_value_list 
ADD CONSTRAINT i_recordattributelist_valueuuid_fk 
FOREIGN KEY (value_uuid) 
REFERENCES smart.i_record_attribute_value (uuid) ON DELETE CASCADE DEFERRABLE;


--Update Sharedlinks to allow for longer URLS
Alter table connect.shared_links alter column url type varchar(262143);


--Tables to Support Quicklinks and DashBoards 

CREATE TABLE connect.dashboards
(
	uuid uuid not null,
	label varchar(256),
	report_uuid_1 uuid,
	report_uuid_2 uuid,
	date_range1 int not null,
	date_range2 int not null,
	custom_date1_from text,
	custom_date1_to text,
	custom_date2_from text,
	custom_date2_to text,
	report_parameterlist_1 text,
	report_parameterlist_2 text,
	PRIMARY KEY (uuid)
);

CREATE TABLE connect.users_default_dashboard 
(
	user_uuid uuid not null,
	dashboard_uuid uuid not null,
	date_range1 int not null,
	date_range2 int not null,
	custom_date1_from text,
	custom_date1_to text,
	custom_date2_from text,
	custom_date2_to text,
	PRIMARY KEY (user_uuid)
);

ALTER TABLE connect.users_default_dashboard
ADD CONSTRAINT default_dashboard_user_fk
FOREIGN KEY (user_uuid) 
REFERENCES connect.users(uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE connect.users_default_dashboard
ADD CONSTRAINT default_dashboard_dashboard_fk
FOREIGN KEY (dashboard_uuid) 
REFERENCES connect.dashboards(uuid) ON DELETE CASCADE DEFERRABLE;


CREATE TABLE connect.quicklinks
(
	uuid uuid not null,
	url text not null,
	label varchar(256),
	created_on timestamp not null,
	created_by_user_uuid uuid not null,
	is_admin_created boolean not null,
	PRIMARY KEY (uuid)
);

ALTER TABLE connect.quicklinks
ADD CONSTRAINT quicklink_user_fk
FOREIGN KEY (created_by_user_uuid) 
REFERENCES connect.users(uuid) ON DELETE CASCADE DEFERRABLE;


CREATE TABLE connect.user_quicklinks 
(
	uuid uuid not null,
	user_uuid uuid not null,
	quicklink_uuid uuid not null,
	label_override varchar(256),
	link_order int,
	PRIMARY KEY (uuid)
);

ALTER TABLE connect.user_quicklinks
ADD CONSTRAINT quicklink_user_fk
FOREIGN KEY (user_uuid) 
REFERENCES connect.users(uuid) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE connect.user_quicklinks
ADD CONSTRAINT userquicklink_quicklink_fk
FOREIGN KEY (quicklink_uuid) 
REFERENCES connect.quicklinks(uuid) ON DELETE CASCADE DEFERRABLE;

-- UPDATES TO SUPPORT DISABLING QUERY COLUMNS
ALTER TABLE smart.observation_query ADD COLUMN show_data_columns_only BOOLEAN;
ALTER TABLE smart.obs_observation_query ADD COLUMN show_data_columns_only BOOLEAN;
ALTER TABLE smart.survey_observation_query ADD COLUMN show_data_columns_only BOOLEAN;
ALTER TABLE smart.entity_observation_query ADD COLUMN show_data_columns_only BOOLEAN;


-- UPDATES TO VERSIONS

INSERT INTO connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.i2', '1.0');

UPDATE connect.connect_plugin_version SET version = '4.0' WHERE plugin_id = 'org.wcs.smart.entity.query';
UPDATE connect.connect_plugin_version SET version = '4.0' WHERE plugin_id = 'org.wcs.smart.er.query';
UPDATE connect.ca_plugin_version SET version = '4.0' WHERE plugin_id = 'org.wcs.smart.entity.query';
UPDATE connect.ca_plugin_version SET version = '4.0' WHERE plugin_id = 'org.wcs.smart.er.query';

update connect.connect_plugin_version set version = '5.0.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '5.0.0' where plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '5.0.0';