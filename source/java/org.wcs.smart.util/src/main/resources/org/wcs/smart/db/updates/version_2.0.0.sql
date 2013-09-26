alter table smart.saved_maps drop column employee_uuid;

insert into smart.ca_projection(uuid, ca_uuid, name, definition, is_default)
values (x'00000000000000000000000000000001',x'00000000000000000000000000000000','WGS 84 [EPSG: 4326]', 'GEOGCS["WGS 84", DATUM["World Geodetic System 1984", SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]], AUTHORITY["EPSG","6326"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4326"]]', true);


-- ADD keys for team, mandate, and transport for cross-ca analysis
alter table smart.team add column keyid varchar(128);
alter table smart.patrol_mandate add column keyid varchar(128);
alter table smart.patrol_transport add column keyid varchar(128);

-- this will not do; keys need to be unique (per conservation area) 
-- only contain certain characters
-- and not start with numbers
--see KeyGenerator - the update script will need to run KeyGenerator for these tables
--update smart.team set keyid = (select lower(a.value) from smart.i18n_label a, smart.LANGUAGE c 
--where smart.team.uuid = a.element_uuid and a.language_uuid = c.uuid and c.isdefault);
--
--update smart.patrol_mandate set keyid = (select lower(a.value) from smart.i18n_label a, smart.LANGUAGE c 
--where smart.patrol_mandate.uuid = a.element_uuid and a.language_uuid = c.uuid and c.isdefault);
--
--update smart.patrol_transport set keyid = (select lower(a.value) from smart.i18n_label a, smart.LANGUAGE c 
--where smart.patrol_transport.uuid = a.element_uuid and a.language_uuid = c.uuid and c.isdefault);



--run smart-tables-dataentry.sql

--create waypoint queries table to observation query table
rename table smart.waypoint_query to observation_query;

--name table constraints; dropping and recreating
ALTER TABLE smart.observation_query drop constraint waypoint_query_creator_uuid_fk;
ALTER TABLE smart.observation_query
	ADD constraint observation_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.observation_query drop constraint waypoint_query_ca_uuid_fk;
ALTER TABLE smart.observation_query
	ADD constraint observation_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.observation_query drop constraint waypoint_query_folder_uuid_fk;
ALTER TABLE smart.observation_query
	ADD constraint observation_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

--create new table for waypoint queries
CREATE TABLE smart.waypoint_query
(
	UUID CHAR(16) FOR BIT DATA NOT NULL,
	CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL,
	QUERY_FILTER VARCHAR(32672),
	CA_FILTER VARCHAR(32672),
	CA_UUID CHAR(16) FOR BIT DATA NOT NULL,
	FOLDER_UUID CHAR(16) FOR BIT DATA,
	COLUMN_FILTER VARCHAR(32672),
	SHARED BOOLEAN DEFAULT false NOT NULL,
	ID VARCHAR(6) NOT NULL,
	PRIMARY KEY (UUID)
);

ALTER TABLE smart.waypoint_query
	ADD constraint waypoint_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.waypoint_query
	ADD constraint waypoint_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.waypoint_query
	ADD constraint waypoint_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

--update premissions on new table
GRANT ALL PRIVILEGES  ON smart.WAYPOINT_QUERY to manager;
GRANT ALL PRIVILEGES  ON smart.WAYPOINT_QUERY to analyst;



--Updates for Plans -addition of creator and comments field
ALTER TABLE smart.plan ADD COLUMN creator_uuid CHAR(16) FOR BIT DATA;
ALTER TABLE smart.plan ADD COLUMN comment LONG VARCHAR;
ALTER TABLE smart.plan 
	ADD CONSTRAINT plan_creator_uuid_fk FOREIGN KEY (creator_uuid)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


update smart.db_version set version = '2.0.0';