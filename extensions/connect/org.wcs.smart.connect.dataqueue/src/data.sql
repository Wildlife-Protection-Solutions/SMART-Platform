
-- DATA PROCESSING QUEUE TABLES
CREATE TABLE smart.connect_data_queue(
	uuid char(16) for bit data NOT NULL,
	type VARCHAR(32) NOT NULL,
	ca_uuid char(16) for bit data,
	name VARCHAR(4096),
	status varchar(32) NOT NULL,
	queue_order integer,
	error_message VARCHAR(8192),
	local_file varchar(4096),
	date_processed timestamp,
	server_item_uuid char(16) for bit data,
	PRIMARY KEY (uuid)
);
	
	
ALTER TABLE smart.connect_data_queue ADD CONSTRAINT 
data_queue_ca_uuid_fk foreign key (ca_uuid) 
REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.connect_data_queue ADD CONSTRAINT status_chk 
CHECK (status IN ('DOWNLOADING', 'QUEUED', 'PROCESSING', 'COMPLETE', 'COMPLETE_WARN', 'ERROR'));

		
ALTER TABLE smart.connect_data_queue ADD CONSTRAINT type_chk 
CHECK (type IN ('PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML'));