ALTER TABLE smart.patrol_leg ADD COLUMN mandate_uuid UUID;

UPDATE smart.patrol_leg SET mandate_uuid = (SELECT p.mandate_uuid FROM smart.patrol p WHERE p.uuid = smart.patrol_leg.patrol_uuid);

ALTER TABLE SMART.PATROL_LEG 
ADD CONSTRAINT MANDATE_UUID_FK FOREIGN KEY (MANDATE_UUID) REFERENCES SMART.PATROL_MANDATE(UUID)  
ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE smart.patrol_leg ALTER COLUMN mandate_uuid SET NOT NULL;

ALTER TABLE smart.patrol DROP COLUMN mandate_uuid;

UPDATE connect.connect_plugin_version SET version = '6.0.0' WHERE plugin_id = 'org.wcs.smart';
UPDATE connect.ca_plugin_version SET version = '6.0.0' WHERE plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '6.0.0';