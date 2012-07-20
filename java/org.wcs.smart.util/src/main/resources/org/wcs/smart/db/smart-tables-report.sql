
/* Create Tables */

CREATE TABLE smart.REPORT
(
	UUID CHAR(16) FOR BIT DATA NOT NULL,
	CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL,
	NAME VARCHAR(1024) NOT NULL,
	ID VARCHAR(6) NOT NULL,
	FILENAME VARCHAR(2048) NOT NULL,
	CA_UUID CHAR(16) FOR BIT DATA NOT NULL,
	SHARED BOOLEAN NOT NULL,
	FOLDER_UUID CHAR(16) FOR BIT DATA,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.REPORT_FOLDER
(
	UUID CHAR(16) FOR BIT DATA NOT NULL,
	EMPLOYEE_UUID CHAR(16) FOR BIT DATA ,
	CA_UUID CHAR(16) FOR BIT DATA NOT NULL,
	PARENT_UUID CHAR(16) FOR BIT DATA ,
	PRIMARY KEY (UUID)
);



/* Create Foreign Keys */

ALTER TABLE smart.report
	ADD CONSTRAINT report_folder_uuid_fk FOREIGN KEY (folder_uuid)
	REFERENCES smart.report_folder (uuid)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.report
	ADD CONSTRAINT report_ca_uuid_fk FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.report
	ADD CONSTRAINT report_creator_uuid_fk FOREIGN KEY (creator_uuid)
	REFERENCES smart.employee (uuid)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.report_folder
	ADD CONSTRAINT report_folder_parent_uuid_fk FOREIGN KEY (parent_uuid)
	REFERENCES smart.report_folder(uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.report_folder
	ADD CONSTRAINT report_folder_ca_uuid_fk FOREIGN KEY (ca_uuid)
	REFERENCES smart.conservation_area(uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.report_folder
	ADD CONSTRAINT report_employee_uuid_fk FOREIGN KEY (employee_uuid)
	REFERENCES smart.employee(uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;