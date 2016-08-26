
/* Drop Tables */

DROP TABLE smart.i_entity_location;
DROP TABLE smart.i_observation_attribute;
DROP TABLE smart.i_datamodel_event;
DROP TABLE smart.i_observation;
DROP TABLE smart.i_location;
DROP TABLE smart.i_entity_search;
DROP TABLE smart.i_entity_attribute_value;
DROP TABLE smart.i_entity_relationship_attribute;
DROP TABLE smart.i_attribute_list_item;
DROP TABLE smart.i_relationship_type_attribute;
DROP TABLE smart.i_entity_type_attribute;
DROP TABLE smart.i_entity_record;
DROP TABLE smart.i_working_set_record;
DROP TABLE smart.i_record_attachment;
DROP TABLE smart.i_record;
DROP TABLE smart.i_entity_relationship;
DROP TABLE smart.i_working_set_query;
DROP TABLE smart.i_working_set_entity;
DROP TABLE smart.i_working_set;
DROP TABLE smart.i_entity_attachment;
DROP TABLE smart.i_entity;
DROP TABLE smart.i_entity_type;
DROP TABLE smart.i_attachment;
DROP TABLE smart.i_relationship_type;
DROP TABLE smart.i_record_query;
DROP TABLE smart.i_attribute;
DROP TABLE smart.i_relationship_group;


/* Create Tables */
CREATE TABLE smart.i_attachment
(
	uuid char(16) for bit data NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	date_created timestamp NOT NULL,
	created_by char(16) for bit data NOT NULL,
	description varchar(2048),
	file varchar(1024) NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_attribute
(
	uuid char(16) for bit data NOT NULL,
	keyid varchar(128) NOT NULL,
	type char(8) NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_attribute_list_item
(
	uuid char(16) for bit data NOT NULL,
	attribute_uuid char(16) for bit data NOT NULL,
	keyid varchar(128) NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_datamodel_event
(
	uuid char(16) for bit data NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	category_uuid  char(16) for bit data NOT NULL,
	list_item_uuid char(16) for bit data,
	tree_node_uuid char(16) for bit data,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_entity
(
	uuid char(16) for bit data NOT NULL,
	date_created timestamp NOT NULL,
	date_modified timestamp,
	created_by char(16) for bit data NOT NULL,
	last_modified_by char(16) for bit data,
	primary_attachment_uuid char(16) for bit data,
	entity_type_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_entity_attachment
(
	entity_uuid char(16) for bit data NOT NULL,
	attachment_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (entity_uuid, attachment_uuid)
);


CREATE TABLE smart.i_entity_attribute_value
(
	entity_uuid char(16) for bit data NOT NULL,
	attribute_uuid char(16) for bit data NOT NULL,
	string_value varchar(1024),
	double_value double,
	list_item_uuid char(16) for bit data,
	PRIMARY KEY (entity_uuid, attribute_uuid)
);


CREATE TABLE smart.i_entity_location
(
	entity_uuid char(16) for bit data NOT NULL,
	location_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (entity_uuid, location_uuid)
);


CREATE TABLE smart.i_entity_record
(
	entity_uuid char(16) for bit data NOT NULL,
	record_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (entity_uuid, record_uuid)
);


CREATE TABLE smart.i_entity_relationship
(
	uuid char(16) for bit data NOT NULL,
	src_entity_uuid char(16) for bit data NOT NULL,
	relationship_type_uuid char(16) for bit data NOT NULL,
	target_entity_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_entity_relationship_attribute
(
	entity_relationship_uuid char(16) for bit data NOT NULL,
	attribute_uuid char(16)for bit data NOT NULL,
	string_value varchar(1024),
	double_value double,
	list_item_uuid char(16) for bit data,
	PRIMARY KEY (entity_relationship_uuid, attribute_uuid)
);


CREATE TABLE smart.i_entity_search
(
	uuid char(16) for bit data NOT NULL,
	search_string long varchar,
	ca_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_entity_type
(
	uuid char(16) for bit data NOT NULL,
	keyid varchar(128) NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	id_attribute_uuid char(16) for bit data NOT NULL,
	icon blob,
	birt_template varchar(4096),
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_entity_type_attribute
(
	entity_type_uuid char(16)for bit data  NOT NULL,
	attribute_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (entity_type_uuid, attribute_uuid)
);


CREATE TABLE smart.i_location
(
	uuid char(16) for bit data NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	geometry blob NOT NULL,
	datetime timestamp,
	comment varchar(4096),
	id varchar(1028),
	record_uuid char(16) for bit data,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_observation
(
	uuid char(16) for bit data NOT NULL,
	location_uuid char(16) for bit data NOT NULL,
	category_uuid char(16)for bit data ,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_observation_attribute
(
	observation_uuid char(16) for bit data NOT NULL,
	attribute_uuid char(16) for bit data NOT NULL,
	list_element_uuid char(16) for bit data,
	tree_node_uuid char(16) for bit data,
	string_value varchar(1024),
	double_value double,
	PRIMARY KEY (observation_uuid, attribute_uuid)
);


CREATE TABLE smart.i_record
(
	uuid char(16) for bit data NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	title varchar(1024) NOT NULL,
	date_created timestamp NOT NULL,
	last_modified_date timestamp,
	created_by char(16) for bit data NOT NULL,
	last_modified_by char(16) for bit data,
	status varchar(16) NOT NULL,
	description long varchar,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_record_attachment
(
	record_uuid char(16) for bit data NOT NULL,
	attachment_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (record_uuid, attachment_uuid)
);


CREATE TABLE smart.i_record_query
(
	uuid char(16) for bit data NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	style long varchar,
	query_string long varchar,
	column_filter long varchar,
	date_created timestamp NOT NULL,
	last_modified_date timestamp,
	created_by char(16) for bit data NOT NULL,
	last_modified_by char(16) for bit data,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_relationship_type_attribute
(
	relationship_type_uuid char(16) for bit data NOT NULL,
	attribute_uuid char(16) for bit data NOT NULL,
	PRIMARY KEY (relationship_type_uuid, attribute_uuid)
);


CREATE TABLE smart.i_relationship_group
(
	uuid char(16) for bit data NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	keyid varchar(128) NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_relationship_type
(
	uuid char(16) for bit data NOT NULL,
	keyid varchar(128) NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	icon blob,
	relationship_group_uuid char(16) for bit data,
	src_entity_type char(16) for bit data NOT NULL,
	target_entity_type char(16) for bit data NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_working_set
(
	uuid char(16) for bit data NOT NULL,
	ca_uuid char(16) for bit data NOT NULL,
	date_created timestamp NOT NULL,
	last_modified_date timestamp,
	created_by char(16) for bit data NOT NULL,
	last_modified_by char(16) for bit data,
	entity_date_filter varchar(1024),
	PRIMARY KEY (uuid)
);


CREATE TABLE smart.i_working_set_entity
(
	working_set_uuid char(16) for bit data NOT NULL,
	entity_uuid char(16) for bit data,
	map_style long varchar,
	PRIMARY KEY (working_set_uuid)
);


CREATE TABLE smart.i_working_set_query
(
	working_set_uuid char(16) for bit data NOT NULL,
	query_uuid char(16) for bit data NOT NULL,
	date_filter varchar(1024),
	map_style long varchar,
	PRIMARY KEY (working_set_uuid, query_uuid)
);


CREATE TABLE smart.i_working_set_record
(
	working_set_uuid char(16) for bit data NOT NULL,
	record_uuid char(16) for bit data NOT NULL,
	map_style long varchar,
	PRIMARY KEY (working_set_uuid, record_uuid)
);



/* Create Foreign Keys */

ALTER TABLE smart.i_location ADD CONSTRAINT ilocation_cauuid_fk
FOREIGN KEY (ca_uuid)
REFERENCES smart.conservation_area (uuid)
ON UPDATE RESTRICT
ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_search ADD CONSTRAINT ientitysearch_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_attribute ADD CONSTRAINT iattribute_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_datamodel_event ADD CONSTRAINT idatamodelevent_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_record ADD CONSTRAINT irecord_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_record ADD CONSTRAINT irecord_createdby_fk
	FOREIGN KEY (created_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_record ADD CONSTRAINT irecord_modifiedby_fk
	FOREIGN KEY (lasT_modified_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_entity_type ADD CONSTRAINT ientitytype_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_entity_type ADD CONSTRAINT ientitytype_idattributeuuid_fk
	FOREIGN KEY (id_attribute_uuid)
	REFERENCES smart.i_attribute (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_attachment ADD CONSTRAINT iattachment_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_attachment ADD CONSTRAINT iattachment_createdby_fk
	FOREIGN KEY (created_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_relationship_type ADD CONSTRAINT irelationshiptype_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_working_set ADD CONSTRAINT iworkingset_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_working_set ADD CONSTRAINT iworkingset_createdby_fk
	FOREIGN KEY (created_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_working_set ADD CONSTRAINT iworkingset_lastmodifiedby_fk
	FOREIGN KEY (last_modified_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery_createdby_fk
	FOREIGN KEY (created_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery_modifiedby_fk
	FOREIGN KEY (last_modified_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_attributeuuid_fk
	FOREIGN KEY (attribute_uuid)
	REFERENCES smart.DM_ATTRIBUTE (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_list_fk
	FOREIGN KEY (list_element_uuid)
	REFERENCES smart.DM_ATTRIBUTE_LIST (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_datamodel_event ADD CONSTRAINT idatamodelevent_list_fk
	FOREIGN KEY (list_item_uuid)
	REFERENCES smart.DM_ATTRIBUTE_LIST (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_tree_fk
	FOREIGN KEY (tree_node_uuid)
	REFERENCES smart.DM_ATTRIBUTE_TREE (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_datamodel_event ADD CONSTRAINT idatamodeleven_tree_fk
	FOREIGN KEY (tree_node_uuid)
	REFERENCES smart.DM_ATTRIBUTE_TREE (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_datamodel_event ADD CONSTRAINT idatamodelevent_category_fk
	FOREIGN KEY (category_uuid)
	REFERENCES smart.DM_CATEGORY (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_record_attachment ADD CONSTRAINT irecordattachment_attchment_fk
	FOREIGN KEY (attachment_uuid)
	REFERENCES smart.i_attachment (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_entity_attachment ADD CONSTRAINT ientityattachment_attchment_fk
	FOREIGN KEY (attachment_uuid)
	REFERENCES smart.i_attachment (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_attribute_value ADD CONSTRAINT ientityattribute_attribute_fk
	FOREIGN KEY (attribute_uuid)
	REFERENCES smart.i_attribute (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_attribute_list_item ADD CONSTRAINT iattributelist_attribute_fk
	FOREIGN KEY (attribute_uuid)
	REFERENCES smart.i_attribute (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_relationship_type_attribute ADD CONSTRAINT irelationshipattribute_attribute_fk
	FOREIGN KEY (attribute_uuid)
	REFERENCES smart.i_attribute (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_type_attribute ADD CONSTRAINT ientitytypeattribute_attribute_fk
	FOREIGN KEY (attribute_uuid)
	REFERENCES smart.i_attribute (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_relationship_attribute ADD CONSTRAINT ientityrelationshipattribute_attribute_fk
	FOREIGN KEY (attribute_uuid)
	REFERENCES smart.i_attribute (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_relationship_attribute ADD CONSTRAINT ientityrelationshipattribute_list_fk
	FOREIGN KEY (list_item_uuid)
	REFERENCES smart.i_attribute_list_item (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_attribute_value ADD CONSTRAINT ientityattributevalue_list_fk
	FOREIGN KEY (list_item_uuid)
	REFERENCES smart.i_attribute_list_item (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_relationship ADD CONSTRAINT ientityrelationship_srcentity_fk
	FOREIGN KEY (src_entity_uuid)
	REFERENCES smart.i_entity (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_relationship ADD CONSTRAINT ientityrelationship_targetentity_fk
	FOREIGN KEY (target_entity_uuid)
	REFERENCES smart.i_entity (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_record ADD CONSTRAINT ientityrecord_entity_fk
	FOREIGN KEY (entity_uuid)
	REFERENCES smart.i_entity (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_working_set_entity ADD CONSTRAINT iworkingsetentity_entity_fk
	FOREIGN KEY (entity_uuid)
	REFERENCES smart.i_entity (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_attribute_value ADD CONSTRAINT ientityattributevalue_entity_fk
	FOREIGN KEY (entity_uuid)
	REFERENCES smart.i_entity (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_attachment ADD CONSTRAINT ientityattachment_entity_fk
	FOREIGN KEY (entity_uuid)
	REFERENCES smart.i_entity (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_location ADD CONSTRAINT ientitylocation_entity_fk
	FOREIGN KEY (entity_uuid)
	REFERENCES smart.i_entity (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_relationship_attribute ADD CONSTRAINT ientityrelationshipattribute_entityrelationship_fk
	FOREIGN KEY (entity_relationship_uuid)
	REFERENCES smart.i_entity_relationship (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_type_attribute ADD CONSTRAINT ientitytypeattribute_entitytype_fk
	FOREIGN KEY (entity_type_uuid)
	REFERENCES smart.i_entity_type (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;



ALTER TABLE smart.i_entity ADD CONSTRAINT ientity_entitytype_fk
	FOREIGN KEY (entity_type_uuid)
	REFERENCES smart.i_entity_type (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity ADD CONSTRAINT ientity_createdby_fk
	FOREIGN KEY (created_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity ADD CONSTRAINT ientity_lastmodifiedby_fk
	FOREIGN KEY (last_modified_by)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;



ALTER TABLE smart.i_entity_location ADD CONSTRAINT ientitylocation_location_fk
	FOREIGN KEY (location_uuid)
	REFERENCES smart.i_location (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_observation ADD CONSTRAINT iobservation_location_fk
	FOREIGN KEY (location_uuid)
	REFERENCES smart.i_location (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_observation ADD CONSTRAINT iobservation_category_fk
	FOREIGN KEY (category_uuid)
	REFERENCES smart.dm_category (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_observation_fk
	FOREIGN KEY (observation_uuid)
	REFERENCES smart.i_observation (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_record ADD CONSTRAINT ientityrecord_record_fk
	FOREIGN KEY (record_uuid)
	REFERENCES smart.i_record (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_working_set_record ADD CONSTRAINT iworkingsetrecord_record_fk
	FOREIGN KEY (record_uuid)
	REFERENCES smart.i_record (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_record_attachment ADD CONSTRAINT irecordattachment_record_fk
	FOREIGN KEY (record_uuid)
	REFERENCES smart.i_record (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_working_set_query ADD CONSTRAINT iworkingsetquery_query_fk
	FOREIGN KEY (query_uuid)
	REFERENCES smart.i_record_query (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_relationship_type ADD CONSTRAINT irelationshiptype_group_fk
	FOREIGN KEY (relationship_group_uuid)
	REFERENCES smart.i_relationship_group (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_relationship_type_attribute ADD CONSTRAINT irelationshipattribute_type_fk
	FOREIGN KEY (relationship_type_uuid)
	REFERENCES smart.i_relationship_type (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_entity_relationship ADD CONSTRAINT ientityrelationship_type_fk
	FOREIGN KEY (relationship_type_uuid)
	REFERENCES smart.i_relationship_type (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;



ALTER TABLE smart.i_working_set_query ADD CONSTRAINT iworkingsetquery_workingset_fk
	FOREIGN KEY (working_set_uuid)
	REFERENCES smart.i_working_set (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_working_set_record ADD CONSTRAINT iworkingsetrecord_workingset_fk
	FOREIGN KEY (working_set_uuid)
	REFERENCES smart.i_working_set (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_working_set_entity ADD CONSTRAINT iworkginsetentity_workingset_fk
	FOREIGN KEY (working_set_uuid)
	REFERENCES smart.i_working_set (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


ALTER TABLE smart.i_relationship_group ADD CONSTRAINT relationshipgroup_cauuid_fk
	FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
DEFERRABLE INITIALLY IMMEDIATE;


