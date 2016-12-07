UPDATE smart.ca_projection set IS_DEFAULT = 'false' WHERE ca_uuid in (SELECT ca_uuid FROM smart.observation_options);
UPDATE smart.ca_projection set IS_DEFAULT = 'true' WHERE uuid IN (SELECT view_projection_uuid FROM smart.observation_options);
ALTER TABLE smart.observation_options DROP column view_projection_uuid;


--compound query tables
CREATE TABLE smart.compound_query(
	uuid UUID not null, 
	creator_uuid UUID not null, 
	ca_uuid UUID not null, 
	ca_filter varchar(32672), 
	folder_uuid UUID, 
	shared boolean, 
	id varchar(6), 
	primary key (uuid));

CREATE TABLE smart.compound_query_layer(
	uuid UUID not null, 
	compound_query_uuid UUID not null, 
	query_uuid UUID not null, 
	query_type varchar(32), 
	style varchar, 
	layer_order integer not null, 
	date_filter varchar(256), 
	primary key (uuid));
		
ALTER TABLE SMART.COMPOUND_QUERY 
ADD CONSTRAINT COMPOUNDQUERY_CA_UUID_FK 
FOREIGN KEY (CA_UUID) 
REFERENCES SMART.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE SMART.COMPOUND_QUERY 
ADD CONSTRAINT COMPOUNDQUERY_FOLDER_UUID_FK 
FOREIGN KEY (FOLDER_UUID) 
REFERENCES SMART.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE SMART.COMPOUND_QUERY 
ADD CONSTRAINT COMPOUNDQUERY_CREATOR_UUID_FK 
FOREIGN KEY (CREATOR_UUID) 
REFERENCES SMART.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE SMART.COMPOUND_QUERY_LAYER 
ADD CONSTRAINT COMPOUNDQUERYLAYER_PARENT_UUID_FK 
FOREIGN KEY (COMPOUND_QUERY_UUID) 
REFERENCES SMART.COMPOUND_QUERY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.connect_ct_properties add column data_frequency INTEGER;
ALTER TABLE smart.connect_ct_properties add column ping_type UUID;

ALTER TABLE connect.data_queue DROP CONSTRAINT type_chk;
ALTER TABLE connect.data_queue ADD CONSTRAINT type_chk CHECK (type IN (
'PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML', 'JSON_CT', 'JSON_ZLIB_CT')); 

ALTER TABLE CONNECT.ALERTS ADD CONSTRAINT valid_level CHECK (level > 0 AND level < 6);		

-- Cybertracker patrol data queue plugin
CREATE TABLE smart.ct_patrol_link ( 
	CT_UUID UUID NOT NULL, 
	PATROL_LEG_UUID UUID NOT NULL,
	CT_DEVICE_ID VARCHAR(36) NOT NULL,
	LAST_OBSERVATION_CNT integer,
	GROUP_START_TIME timestamp,
	PRIMARY KEY (CT_UUID)
);

ALTER TABLE smart.ct_patrol_link 
ADD CONSTRAINT patrol_key_uuid_fk 
FOREIGN KEY (patrol_leg_uuid) 
REFERENCES smart.patrol_leg ON DELETE cascade DEFERRABLE;


CREATE TABLE smart.ct_mission_link ( 
	CT_UUID uuid NOT NULL, 
	MISSION_UUID uuid NOT NULL, 
	ct_device_id varchar(36) not null, 
	last_observation_cnt integer, 
	group_start_time timestamp, 
	su_uuid uuid,
PRIMARY KEY (CT_UUID));

ALTER TABLE smart.ct_mission_link 
ADD CONSTRAINT mission_uuid_fk 
FOREIGN KEY (mission_uuid) 
REFERENCES smart.mission ON DELETE cascade DEFERRABLE;
	
ALTER TABLE smart.ct_mission_link 
ADD CONSTRAINT mission_link_su_uuid_fk 
FOREIGN KEY (su_uuid) 
REFERENCES smart.sampling_unit ON DELETE cascade DEFERRABLE;

insert into connect.connect_plugin_version (version, plugin_id) values('1.0', 'org.wcs.smart.connect.dataqueue.cybertracker.patrol');
insert into connect.connect_plugin_version (version, plugin_id) values('1.0', 'org.wcs.smart.connect.dataqueue.cybertracker.survey');

update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.connect.cybertracker';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.connect.cybertracker';

update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.connect.dataqueue';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.connect.dataqueue';
		
update connect.connect_plugin_version set version = '4.0.1' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '4.0.1' where plugin_id = 'org.wcs.smart';


update connect.connect_version set version = '4.0.1';