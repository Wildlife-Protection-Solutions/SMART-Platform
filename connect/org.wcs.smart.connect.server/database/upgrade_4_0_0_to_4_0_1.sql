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
		
ALTER TABLE smart.PLAN
ADD CONSTRAINT PLAN_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

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

ALTER TABLE CONNECT.ALERTS ADD CONSTRAINT valid_level CHECK (level > 0 AND level < 6);
			
update connect.connect_plugin_version set version = '4.0.1' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '4.0.1' where plugin_id = 'org.wcs.smart';