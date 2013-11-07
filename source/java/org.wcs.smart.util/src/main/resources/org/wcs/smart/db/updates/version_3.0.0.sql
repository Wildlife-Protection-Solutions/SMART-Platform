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