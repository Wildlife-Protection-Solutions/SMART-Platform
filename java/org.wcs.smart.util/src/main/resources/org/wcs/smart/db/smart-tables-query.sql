--for temporary tables for running queriess
CREATE SCHEMA smart_query;  


CREATE TABLE SMART.query_folder
(
	UUID CHAR(16) FOR BIT DATA NOT NULL,
	EMPLOYEE_UUID CHAR(16) FOR BIT DATA,
	CA_UUID CHAR(16) FOR BIT DATA NOT NULL,
	PARENT_UUID CHAR(16) FOR BIT DATA,
	PRIMARY KEY (UUID)
);

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

CREATE TABLE smart.patrol_query
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


CREATE TABLE smart.summary_query
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

CREATE TABLE smart.gridded_query
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




ALTER TABLE smart.query_folder 
	add constraint query_folder_ca_uuid_fk FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.query_folder
	add constraint query_folder_employee_uuid_fk FOREIGN KEY (employee_uuid)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.query_folder
	ADD constraint query_folder_parent_uuid_fk FOREIGN KEY (PARENT_UUID)
	REFERENCES smart.query_folder(uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;



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

ALTER TABLE smart.patrol_query
	ADD constraint patrol_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.patrol_query
	ADD constraint patrol_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.patrol_query
	ADD constraint patrol_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;




ALTER TABLE smart.summary_query
	ADD constraint summary_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.summary_query
	ADD constraint summary_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.summary_query
	ADD constraint summary_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.gridded_query
	ADD constraint gridded_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)
	REFERENCES smart.employee (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.gridded_query
	ADD constraint gridded_query_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.gridded_query
	ADD constraint gridded_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)
	REFERENCES smart.query_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


create index patrol_leg_day_patrol_day_idx on smart.patrol_leg_day(patrol_day);
