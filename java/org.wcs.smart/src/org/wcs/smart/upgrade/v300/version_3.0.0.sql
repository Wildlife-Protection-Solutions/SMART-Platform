
--  ** UPDATES FOR NEW OBSERVATION TYPES **
CREATE TABLE smart.patrol_waypoint
(
	WP_UUID CHAR(16) for bit data NOT NULL,
	LEG_DAY_UUID CHAR(16) for bit data NOT NULL,
	PRIMARY KEY (WP_UUID, LEG_DAY_UUID)
);

INSERT INTO smart.patrol_waypoint (WP_UUID, LEG_DAY_UUID)
SELECT uuid, leg_day_uuid
FROM smart.waypoint;

CREATE TABLE smart.waypoint_temp(
	UUID CHAR(16)  for bit data  NOT NULL,
	CA_UUID CHAR(16) for bit data NOT NULL,
	SOURCE VARCHAR(16) NOT NULL,
	ID INTEGER NOT NULL,
	X DOUBLE NOT NULL,
	Y DOUBLE NOT NULL,
	DATETIME TIMESTAMP NOT NULL,
	DIRECTION REAL,
	DISTANCE REAL,
	WP_COMMENT VARCHAR(4096),
	PRIMARY KEY (UUID)
);

INSERT INTO smart.waypoint_temp 
SELECT wp.uuid, p.ca_uuid, 'PATROL',wp.id, wp.x, wp.y,
TIMESTAMP( pld.patrol_day, wp.time), wp.direction, wp.distance, wp.wp_comment
FROM
smart.patrol p join smart.patrol_leg pl on p.uuid = pl.patrol_uuid
join smart.patrol_leg_day pld on pl.uuid =pld.patrol_leg_uuid
join smart.waypoint wp on wp.leg_day_uuid = pld.uuid;

ALTER TABLE smart.waypoint DROP CONSTRAINT waypoint_leg_day_uuid_fk;
ALTER TABLE smart.WP_ATTACHMENTS DROP CONSTRAINT wp_attachments_wp_uuid_fk;
alter table smart.WP_OBSERVATION DROP CONSTRAINT observation_wp_uuid_fk;

DROP TABLE smart.waypoint;

CREATE TABLE smart.waypoint(
	UUID CHAR(16)  for bit data  NOT NULL,
	CA_UUID CHAR(16) for bit data NOT NULL,
	SOURCE VARCHAR(16) NOT NULL,
	ID INTEGER NOT NULL,
	X DOUBLE NOT NULL,
	Y DOUBLE NOT NULL,
	DATETIME TIMESTAMP NOT NULL,
	DIRECTION REAL,
	DISTANCE REAL,
	WP_COMMENT VARCHAR(4096),
	PRIMARY KEY (UUID)
);
INSERT INTO smart.waypoint select * from smart.waypoint_temp;
DROP TABLE smart.waypoint_temp;



ALTER TABLE smart.patrol_waypoint
	ADD CONSTRAINT patrol_waypoint_leg_day_uuid_fk FOREIGN KEY (LEG_DAY_UUID)
	REFERENCES smart.PATROL_LEG_DAY (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;
ALTER TABLE smart.patrol_waypoint
	ADD CONSTRAINT patrol_waypoint_wp_uuid_fk FOREIGN KEY (WP_UUID)
	REFERENCES smart.waypoint (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;


ALTER TABLE smart.wp_attachments
	ADD CONSTRAINT wp_attachments_wp_uuid_fk FOREIGN KEY (WP_UUID)
	REFERENCES smart.waypoint (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.wp_observation 
	ADD CONSTRAINT observation_wp_uuid_fk FOREIGN KEY (WP_UUID)
	REFERENCES smart.waypoint (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;
 
 ALTER TABLE smart.waypoint
	ADD CONSTRAINT waypoint_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

create index waypoint_datetime_idx on smart.waypoint(datetime);
 
CREATE TABLE smart.observation_attachment
(
	UUID CHAR(16)  for bit data  NOT NULL,
	OBS_UUID CHAR(16)  for bit data  NOT NULL,
	FILENAME VARCHAR(1024) NOT NULL,
	PRIMARY KEY (UUID)
);

ALTER TABLE smart.observation_attachment
	ADD CONSTRAINT observation_attachment_obs_uuid_fk FOREIGN KEY (OBS_UUID)
	REFERENCES smart.wp_observation (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;


GRANT ALL PRIVILEGES  ON  smart.WAYPOINT TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.WAYPOINT TO manager;
GRANT SELECT ON  smart.WAYPOINT TO analyst;

GRANT ALL PRIVILEGES  ON  smart.PATROL_WAYPOINT TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.PATROL_WAYPOINT TO manager;
GRANT SELECT ON  smart.PATROL_WAYPOINT TO analyst;

GRANT ALL PRIVILEGES  ON  smart.observation_attachment TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.observation_attachment TO manager;
GRANT SELECT ON  smart.observation_attachment TO analyst;



--  ** UPDATES To Permissions To Allow data-entry users add/delete ENTITIES
GRANT DELETE ON smart.cm_attribute_option to data_entry;
GRANT DELETE ON smart.cm_attribute_list to data_entry;
GRANT DELETE ON smart.cm_attribute_tree_node to data_entry;
GRANT DELETE ON smart.cm_attribute_option to manager;
GRANT DELETE ON smart.cm_attribute_list to manager;
GRANT DELETE ON smart.cm_attribute_tree_node to manager;


-- ** GENERALIZING PATROL OPTIONS table
RENAME table smart.patrol_options to observation_options;


-- ** ADD the query tables necessary for new QUERY TYPES
CREATE TABLE smart.obs_waypoint_query
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


CREATE TABLE smart.obs_summary_query
(
	UUID CHAR(16) for bit data NOT NULL,
	CREATOR_UUID CHAR(16) for bit data NOT NULL,
	CA_FILTER VARCHAR(32672),
	QUERY_DEF VARCHAR(32672),
	FOLDER_UUID CHAR(16) for bit data,
	SHARED BOOLEAN NOT NULL,
	CA_UUID CHAR(16) for bit data NOT NULL,
	ID VARCHAR(6) NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.obs_gridded_query
(
	UUID CHAR(16) for bit data NOT NULL,
	CREATOR_UUID CHAR(16) for bit data NOT NULL,
	QUERY_FILTER VARCHAR(32672),
	CA_FILTER VARCHAR(32672),
	QUERY_DEF VARCHAR(32672),
	FOLDER_UUID CHAR(16) for bit data,
	SHARED BOOLEAN NOT NULL,
	CA_UUID CHAR(16) for bit data NOT NULL,
	ID VARCHAR(6) NOT NULL,
	CRS_DEFINITION VARCHAR(32672) NOT NULL,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.obs_observation_query
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


ALTER TABLE smart.obs_waypoint_query
	ADD constraint obswaypoint_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;
		
ALTER TABLE smart.obs_waypoint_query
	ADD constraint obs_waypoint_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;
		
ALTER TABLE smart.obs_waypoint_query
	ADD constraint obs_waypoint_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;
		
ALTER TABLE smart.obs_observation_query
	ADD constraint obsobservation_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;
		
ALTER TABLE smart.obs_observation_query
	ADD constraint obs_observation_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;
		
ALTER TABLE smart.obs_observation_query
	ADD constraint obs_observation_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;

ALTER TABLE smart.obs_summary_query
	ADD constraint obs_summary_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;
		
ALTER TABLE smart.obs_summary_query
	ADD constraint obs_summary_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;

ALTER TABLE smart.obs_summary_query
	ADD constraint obs_summary_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;

ALTER TABLE smart.obs_gridded_query
	ADD constraint obs_gridded_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;
			
ALTER TABLE smart.obs_gridded_query
	ADD constraint obs_gridded_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;

ALTER TABLE smart.obs_gridded_query
	ADD constraint obs_gridded_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT;

GRANT ALL PRIVILEGES ON smart.obs_observation_query to manager;
GRANT ALL PRIVILEGES ON smart.obs_waypoint_query to manager;
GRANT ALL PRIVILEGES ON smart.obs_summary_query to manager;
GRANT ALL PRIVILEGES ON smart.obs_gridded_query to manager;
GRANT ALL PRIVILEGES ON smart.obs_observation_query to analyst;
GRANT ALL PRIVILEGES ON smart.obs_waypoint_query to analyst;
GRANT ALL PRIVILEGES ON smart.obs_summary_query to analyst;
GRANT ALL PRIVILEGES ON smart.obs_gridded_query to analyst;

-- ** MODIFY VERSION TABLE To SUPPORT PLUGINS
ALTER TABLE smart.db_version ADD COLUMN plugin_id VARCHAR(512);
UPDATE smart.db_version SET plugin_id = 'org.wcs.smart';
ALTER TABLE smart.db_version ALTER COLUMN plugin_id  NOT NULL;
ALTER TABLE smart.db_version ADD PRIMARY KEY (plugin_id);

GRANT SELECT ON smart.db_version TO data_entry;
GRANT SELECT ON smart.db_version TO manager;
GRANT SELECT ON smart.db_version TO analyst;

-- ** "photo required" option support **
ALTER TABLE smart.CM_NODE ADD COLUMN photo_required BOOLEAN;

-- ** Projection Selection for Data Entry **
ALTER TABLE smart.observation_options ADD COLUMN view_projection_uuid CHAR(16) for bit data;
CREATE TRIGGER smart.view_projection_cleanup
  AFTER DELETE ON smart.ca_projection
  REFERENCING OLD AS OLD
  FOR EACH ROW MODE DB2SQL
  update smart.observation_options SET VIEW_PROJECTION_UUID = NULL where CA_UUID = OLD.CA_UUID AND VIEW_PROJECTION_UUID = OLD.UUID
;

--  ** INTELLIGENCE PLUGIN UPDATES **
ALTER TABLE smart.intelligence ADD COLUMN creator_uuid CHAR(16) FOR BIT DATA;
ALTER TABLE smart.intelligence 
	ADD CONSTRAINT intelligence_creator_uuid_fk FOREIGN KEY (creator_uuid)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;
insert into smart.db_version (plugin_id, version) values ('org.wcs.smart.intelligence', '3.0');
-- additional updates to intelligence plugin source fields are done in the SmartUpdaters30 java script
-- as uuids need to be generated


-- ** PLAN PLUGIN UPDATES **
insert into smart.db_version (plugin_id, version) values ('org.wcs.smart.plan', '3.0');


-- ** CYBERTRACKER UPDATES **
-- this works from upgrading from CT2.0.X to 3.0.0; does not work for upgrading from CT1.1.1 
insert into smart.db_version(version,plugin_id)
select '3.0', 'org.wcs.smart.cybertracker'
FROM 
(select a.tablename, b.schemaname from sys.SYSTABLES a join sys.SYSSCHEMAS b on a.schemaid = b.schemaid
WHERE a.tablename='CT_PROPERTIES_OPTION' and b.schemaname='SMART') foo;

-- ** VERSION UDATE **
update smart.db_version set version = '3.0.0' where plugin_id = 'org.wcs.smart';

