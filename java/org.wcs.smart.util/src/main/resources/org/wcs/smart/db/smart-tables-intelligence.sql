CREATE TABLE smart.intelligence
(
	UUID CHAR(16) for bit data NOT NULL,
	CA_UUID CHAR(16) for bit data  NOT NULL,
	RECEIVED_DATE DATE NOT NULL,
	SOURCE VARCHAR(9) NOT NULL,
	PATROL_UUID CHAR(16) for bit data,
	FROM_DATE DATE NOT NULL,
	TO_DATE DATE,
	SHORT_NAME VARCHAR(32) NOT NULL,
	DESCRIPTION LONG VARCHAR,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.intelligence_point
(
	UUID CHAR(16) for bit data NOT NULL,
	INTELLIGENCE_UUID CHAR(16) for bit data  NOT NULL,
	X DOUBLE NOT NULL,
	Y DOUBLE NOT NULL,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.intelligence_attachment
(
	UUID CHAR(16)  for bit data  NOT NULL,
	INTELLIGENCE_UUID CHAR(16)  for bit data  NOT NULL,
	FILENAME VARCHAR(1024) NOT NULL,
	PRIMARY KEY (UUID)
);


ALTER TABLE smart.intelligence
	ADD CONSTRAINT intelligence_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area(UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.intelligence
	ADD CONSTRAINT intelligence_patrol_uuid_fk FOREIGN KEY (PATROL_UUID)
	REFERENCES smart.patrol(UUID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

ALTER TABLE smart.intelligence_point
	ADD CONSTRAINT intelligence_point_intelligence_uuid_fk FOREIGN KEY (INTELLIGENCE_UUID)
	REFERENCES smart.intelligence(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.intelligence_attachment
	ADD CONSTRAINT intelligence_attachment_intelligence_uuid_fk FOREIGN KEY (INTELLIGENCE_UUID)
	REFERENCES smart.intelligence (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

