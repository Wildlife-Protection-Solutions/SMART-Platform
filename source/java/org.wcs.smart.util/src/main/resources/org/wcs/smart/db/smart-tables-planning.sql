--/* Create Tables */
-- Tracks all plans loaded in the current database.
CREATE TABLE smart.plan
(
	uuid CHAR(16) FOR BIT DATA NOT NULL,
	id VARCHAR(32) NOT NULL,
	start_date DATE NOT NULL,
	end_date DATE,
	type VARCHAR(32),
	name VARCHAR(32),
	description VARCHAR(256),
	ca_uuid CHAR(16) FOR BIT DATA NOT NULL,
	station_uuid CHAR(16) FOR BIT DATA,
	team_uuid CHAR(16) FOR BIT DATA,
	active_employees INTEGER,
	unavailable_employees INTEGER,
	PARENT_UUID CHAR(16) for bit data,
	PRIMARY KEY (UUID)
);

ALTER TABLE smart.PLAN
	ADD CONSTRAINT plan_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;
ALTER TABLE smart.PLAN
	ADD CONSTRAINT plan_station_uuid_fk FOREIGN KEY (STATION_UUID)
	REFERENCES smart.station (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.PLAN
	ADD CONSTRAINT plan_team_uuid_fk FOREIGN KEY (TEAM_UUID)
	REFERENCES smart.team (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.plan 
	ADD CONSTRAINT plan_parent_uuid_fk FOREIGN KEY (PARENT_UUID)
	REFERENCES smart.plan (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

CREATE TABLE smart.plan_target
(
	uuid CHAR(16) FOR BIT DATA,
	name VARCHAR(32),
	description VARCHAR(256),
	value double,
	op VARCHAR(1),
	type VARCHAR(32),
	plan_uuid CHAR(16) FOR BIT DATA NOT NULL,
	category varchar(16),
	PRIMARY KEY (UUID)
);

ALTER TABLE smart.plan_Target
	ADD CONSTRAINT target_plan_uuid_fk FOREIGN KEY (PLAN_UUID)
	REFERENCES smart.Plan (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;
