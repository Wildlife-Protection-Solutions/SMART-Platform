
--/* Drop Tables */
--DROP TABLE CONSERVATION_AREA;
CREATE SCHEMA smart;

--/* Create Tables */
-- Tracks all conservation areas loaded in the current database.
CREATE TABLE smart.conservation_area
(
	-- char(16) for bit data
	uuid CHAR(16) FOR BIT DATA NOT NULL,
	id VARCHAR(8) NOT NULL,
	name VARCHAR(256),
	designation VARCHAR(1024),
	description VARCHAR(2056),
	plan_point_buffer FLOAT ,
	default_srs VARCHAR(16),
	PRIMARY KEY (UUID)
);
CREATE TABLE smart.employee
(
	uuid CHAR(16) FOR BIT DATA NOT NULL,
	ca_uuid CHAR(16) FOR BIT DATA NOT NULL,
	id VARCHAR(32) NOT NULL,
	givenname VARCHAR(64) NOT NULL,
	familyname VARCHAR(64) NOT NULL,
	startemployementdate DATE NOT NULL,
	endemployementdate DATE,
	datecreated DATE NOT NULL,
	birthdate DATE,
	gender CHAR(1) NOT NULL,
	smartuserid VARCHAR(16),
	smartpassword VARCHAR(16),
	smartuserlevel SMALLINT,
	agency_uuid char(16) for bit data,
	rank_uuid char(16) for bit data,
	PRIMARY KEY (UUID)
);

ALTER TABLE smart.EMPLOYEE
	ADD CONSTRAINT employee_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;




CREATE TABLE smart.language
(
	uuid CHAR(16) for bit data NOT NULL,
	ca_uuid CHAR(16) for bit data NOT NULL,
	code CHAR(5) NOT NULL,
	name VARCHAR(64),
	isdefault BOOLEAN default false not null,
	PRIMARY KEY (UUID)
);
ALTER TABLE smart.language
	ADD CONSTRAINT language_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;



CREATE TABLE smart.i18n_label
(
	language_uuid CHAR(16) for bit data NOT NULL,
	element_uuid CHAR(16) for bit data NOT NULL,
	value VARCHAR(1024) NOT NULL,
	PRIMARY KEY (LANGUAGE_UUID, ELEMENT_UUID)
);

ALTER TABLE smart.i18n_label
	ADD CONSTRAINT languages_ca_uuid_fk FOREIGN KEY (LANGUAGE_UUID)
	REFERENCES smart.language (UUID)
	ON UPDATE RESTRICT
	ON DELETE cascade
;

CREATE TABLE smart.station
(
	uuid CHAR(16) for bit data NOT NULL,
	ca_uuid CHAR(16) for bit data  NOT NULL,
	desc_uuid CHAR(16) for bit data, 
	is_active boolean not null,
	PRIMARY KEY (UUID)
);

ALTER TABLE smart.station
	ADD CONSTRAINT station_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


CREATE TABLE smart.agency
(
	-- for bit data
	uuid CHAR(16) for bit data NOT NULL,
	ca_uuid CHAR(16) FOR BIT DATA NOT NULL,
	PRIMARY KEY (uuid)
);

ALTER TABLE smart.agency
	ADD CONSTRAINT agency_ca_uuid_fk FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;
ALTER TABLE smart.employee
	ADD CONSTRAINT employee_agency_uuid_fk FOREIGN KEY (AGENCY_UUID)
	REFERENCES smart.agency (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

CREATE TABLE smart.rank
(
	uuid CHAR(16) for bit data NOT NULL,
	-- for bit data
	agency_uuid CHAR(16) for bit data NOT NULL,
	PRIMARY KEY (uuid)
);
ALTER TABLE smart.employee
	ADD CONSTRAINT employee_rank_uuid_fk FOREIGN KEY (RANK_UUID)
	REFERENCES smart.rank (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.rank
	ADD CONSTRAINT rank_agency_uuid_fk FOREIGN KEY (AGENCY_UUID)
	REFERENCES smart.agency (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;


CREATE TABLE smart.area_geometries
(
	--required for hatbox
	pid INTEGER NOT NULL generated always as identity,
	uuid CHAR(16) for bit data NOT NULL UNIQUE,
	ca_uuid CHAR(16) for bit data NOT NULL,
	area_type VARCHAR(5) NOT NULL,
	id VARCHAR(256),
	keyid VARCHAR(256),
	geom BLOB NOT NULL,
	PRIMARY KEY (PID)
);

ALTER TABLE smart.area_geometries
	ADD CONSTRAINT area_geometries_ca_uuid_fk FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

CREATE SEQUENCE smart.smart_user_id_seq START WITH 0 MAXVALUE 99999 CYCLE;

