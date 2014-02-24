ALTER TABLE smart.intelligence 
	ADD CONSTRAINT intelligence_source_uuid_fk FOREIGN KEY (source_uuid) 
	REFERENCES smart.intelligence_source (uuid) 
	ON UPDATE RESTRICT 
	ON DELETE RESTRICT;

alter table smart.intelligence drop column SOURCE;
