--CT Updates to upgrade from CT 1.1.1 to 2.0.0

DROP TABLE smart.cybertracker_properties;

CREATE TABLE smart.ct_properties_option (
	uuid CHAR(16) for bit data NOT NULL,
	ca_uuid CHAR(16) for bit data  NOT NULL,
	OPTION_ID VARCHAR(32) NOT NULL,
	DOUBLE_VALUE DOUBLE,
	INTEGER_VALUE INTEGER,
	STRING_VALUE VARCHAR(1024), 
	PRIMARY KEY (UUID));

ALTER TABLE smart.ct_properties_option 
	ADD CONSTRAINT ct_properties_option_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE;
	
GRANT ALL PRIVILEGES ON smart.ct_properties_option to data_entry;
GRANT ALL PRIVILEGES ON smart.ct_properties_option to manager;
GRANT ALL PRIVILEGES ON smart.ct_properties_option to analyst;
GRANT SELECT ON smart.ct_properties_option to login;