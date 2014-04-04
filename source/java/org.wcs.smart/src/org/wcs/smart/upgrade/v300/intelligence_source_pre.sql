CREATE TABLE smart.intelligence_source (
	uuid CHAR(16) for bit data NOT NULL, 
	ca_uuid CHAR(16) for bit data  NOT NULL, 
	KEYID varchar(128), 
	IS_ACTIVE BOOLEAN NOT NULL, 
	PRIMARY KEY (UUID));

ALTER TABLE smart.intelligence_source 
	ADD CONSTRAINT intelligence_source_ca_uuid_fk FOREIGN KEY (CA_UUID) 
	REFERENCES smart.conservation_area(UUID) 
	ON UPDATE RESTRICT 
	ON DELETE RESTRICT;

ALTER TABLE smart.intelligence ADD COLUMN source_uuid CHAR(16) FOR BIT DATA;

GRANT SELECT ON smart.intelligence_source to data_entry;
GRANT SELECT ON smart.intelligence_source to manager;
GRANT SELECT ON smart.intelligence_source to analyst;
