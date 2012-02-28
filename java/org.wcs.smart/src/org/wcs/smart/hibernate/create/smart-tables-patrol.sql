


CREATE TABLE smart.patrol_mandate
(
	UUID CHAR(16) for bit data NOT NULL,
	CA_UUID CHAR(16) for bit data NOT NULL,
	IS_ACTIVE BOOLEAN NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.patrol_transport
(
	UUID CHAR(16) for bit data NOT NULL,
	CA_UUID CHAR(16) for bit data NOT NULL,
	IS_ACTIVE BOOLEAN NOT NULL,
	PATROL_TYPE VARCHAR(6) NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.patrol_type
(
	CA_UUID CHAR(16) for bit data NOT NULL,
	PATROL_TYPE VARCHAR(6) NOT NULL,
	IS_ACTIVE BOOLEAN NOT NULL,
	PRIMARY KEY(CA_UUID, PATROL_TYPE)
);



CREATE TABLE smart.team
(
	UUID CHAR(16) for bit data NOT NULL,
	CA_UUID CHAR(16) for bit data NOT NULL,
	IS_ACTIVE BOOLEAN  NOT NULL,
	DESC_UUID CHAR(16) for bit data UNIQUE,
	PATROL_MANDATE_UUID CHAR(16) for bit data,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.patrol_options
(
	CA_UUID CHAR(16) for bit data NOT NULL,
	DISTANCE_DIRECTION BOOLEAN NOT NULL,
	EDIT_TIME SMALLINT,
	PRIMARY KEY (CA_UUID)
);

CREATE TABLE smart.patrol
(
	UUID CHAR(16) for bit data NOT NULL,
	CA_UUID CHAR(16) for bit data  NOT NULL,
	ID VARCHAR(23) NOT NULL,
	STATION_UUID CHAR(16) for bit data,
	TEAM_UUID CHAR(16) for bit data,
	OBJECTIVE_RATING SMALLINT,
	OBJECTIVE VARCHAR(8192),
	MANDATE_UUID CHAR(16) for bit data,
	PATROL_TYPE VARCHAR(6) NOT NULL,
	IS_ARMED BOOLEAN NOT NULL,
	START_DATE DATE NOT NULL,
	END_DATE DATE NOT NULL,
	PRIMARY KEY (UUID)
);



CREATE TABLE smart.patrol_leg
(
	UUID CHAR(16)  for bit data NOT NULL,
	PATROL_UUID CHAR(16)  for bit data NOT NULL,
	START_DATE DATE NOT NULL,
	END_DATE DATE NOT NULL,
	TRANSPORT_UUID CHAR(16) for bit data NOT NULL,
	ID INTEGER NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.patrol_leg_day
(
	UUID CHAR(16)  for bit data  NOT NULL,
	PATROL_LEG_UUID CHAR(16)  for bit data  NOT NULL,
	PATROL_DAY DATE NOT NULL,
	START_TIME TIME,
	REST_MINUTES INTEGER,
	END_TIME TIME,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.patrol_leg_members
(
	PATROL_LEG_UUID CHAR(16)  for bit data  NOT NULL,
	EMPLOYEE_UUID CHAR(16)  for bit data  NOT NULL,
	IS_LEADER BOOLEAN NOT NULL,
	IS_PILOT BOOLEAN NOT NULL,
	PRIMARY KEY (PATROL_LEG_UUID, EMPLOYEE_UUID)
);


CREATE TABLE smart.waypoint
(
	UUID CHAR(16)  for bit data  NOT NULL,
	LEG_DAY_UUID CHAR(16)  for bit data  NOT NULL,
	ID INTEGER NOT NULL,
	X DOUBLE NOT NULL,
	Y DOUBLE NOT NULL,
	TIME TIME NOT NULL,
	DIRECTION REAL,
	DISTANCE REAL,
	WP_COMMENT VARCHAR(4096),
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.wp_attachments
(
	UUID CHAR(16)  for bit data  NOT NULL,
	WP_UUID CHAR(16)  for bit data  NOT NULL,
	FILENAME VARCHAR(1024) NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.wp_observation
(
	UUID CHAR(16)  for bit data  NOT NULL,
	WP_UUID CHAR(16)  for bit data  NOT NULL,
	CATEGORY_UUID CHAR(16)  for bit data  NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.wp_observation_attributes 
(
	OBSERVATION_UUID CHAR(16)  for bit data  NOT NULL,
	ATTRIBUTE_UUID CHAR(16)  for bit data  NOT NULL,
	LIST_ELEMENT_UUID CHAR(16) for bit data  ,
	TREE_NODE_UUID CHAR(16)  for bit data ,
	NUMBER_VALUE DOUBLE,
	STRING_VALUE VARCHAR(1024),
	PRIMARY KEY (OBSERVATION_UUID, ATTRIBUTE_UUID)
);



CREATE TABLE smart.track
(
	UUID CHAR(16) for bit data NOT NULL,
	PATROL_LEG_DAY_UUID CHAR(16) for bit data NOT NULL,
	GEOMETRY BLOB NOT NULL,
	DISTANCE REAL NOT NULL,
	PRIMARY KEY (UUID, PATROL_LEG_DAY_UUID)
);


ALTER TABLE smart.patrol_options
	ADD CONSTRAINT patrol_options_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.patrol_mandate
	ADD CONSTRAINT patrol_mandate_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;



ALTER TABLE smart.patrol
	ADD CONSTRAINT patrol_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area(UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.patrol
	ADD CONSTRAINT patrol_station_uuid_fk FOREIGN KEY (STATION_UUID)
	REFERENCES smart.station(UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.patrol
	ADD CONSTRAINT patrol_team_uuid_fk FOREIGN KEY (TEAM_UUID)
	REFERENCES smart.team(UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.patrol
	ADD CONSTRAINT patrol_mandate_uuid_fk FOREIGN KEY (MANDATE_UUID)
	REFERENCES smart.patrol_mandate(UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.patrol_transport
	ADD CONSTRAINT patrol_transport_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.patrol_type
	ADD CONSTRAINT patrol_type_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.team
	ADD CONSTRAINT team_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;



ALTER TABLE smart.team
	ADD CONSTRAINT team_patrol_mandate_uuid_fk FOREIGN KEY (PATROL_MANDATE_UUID)
	REFERENCES smart.patrol_mandate(UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;





ALTER TABLE smart.patrol_leg
	ADD CONSTRAINT patrol_leg_patrol_uuid_fk FOREIGN KEY (PATROL_UUID)
	REFERENCES smart.patrol (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.patrol_leg_day
   ADD CONSTRAINT patrol_leg_day_leg_uuid_fk FOREIGN KEY (PATROL_LEG_UUID)
	REFERENCES smart.PATROL_LEG (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.patrol_leg_members 
    ADD CONSTRAINT leg_members_patrol_leg_uuid_fk FOREIGN KEY (PATROL_LEG_UUID)
	REFERENCES smart.PATROL_LEG (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;
ALTER TABLE smart.patrol_leg_members 
    ADD CONSTRAINT leg_members_employee_uuid_fk FOREIGN KEY (EMPLOYEE_UUID)
	REFERENCES smart.EMPLOYEE (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.track
	ADD CONSTRAINT track_leg_day_uuid_fk FOREIGN KEY (PATROL_LEG_DAY_UUID)
	REFERENCES smart.PATROL_LEG_DAY (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.waypoint
	ADD CONSTRAINT waypoint_leg_day_uuid_fk FOREIGN KEY (LEG_DAY_UUID)
	REFERENCES smart.PATROL_LEG_DAY (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.wp_attachments
	ADD CONSTRAINT wp_attachments_wp_uuid_fk FOREIGN KEY (WP_UUID)
	REFERENCES smart.waypoint (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.wp_observation 
	ADD CONSTRAINT observation_wp_uuid_fk FOREIGN KEY (WP_UUID)
	REFERENCES smart.waypoint (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;
ALTER TABLE smart.wp_observation 
	ADD CONSTRAINT observation_category_uuid_fk FOREIGN KEY (CATEGORY_UUID)
	REFERENCES smart.dm_category (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.wp_observation_attributes 
	ADD CONSTRAINT obs_attribute_obs_uuid_fk FOREIGN KEY (OBSERVATION_UUID)
	REFERENCES smart.WP_OBSERVATION (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.wp_observation_attributes 
	ADD CONSTRAINT observation_attribute_att_uuid_fk FOREIGN KEY (ATTRIBUTE_UUID)
	REFERENCES smart.dm_attribute (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.wp_observation_attributes 
	ADD CONSTRAINT observation_attribute_att_list_uuid_fk FOREIGN KEY (LIST_ELEMENT_UUID)
	REFERENCES smart.dm_attribute_list (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.wp_observation_attributes 
	ADD CONSTRAINT observation_attribute_att_tree_uuid_fk FOREIGN KEY (TREE_NODE_UUID)
	REFERENCES smart.dm_attribute_tree (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


