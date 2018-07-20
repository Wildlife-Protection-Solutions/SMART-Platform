ALTER TABLE smart.waypoint ADD COLUMN last_modified timestamp without timezone;
UPDATE smart.waypoint SET last_modified = datetime;
ALTER TABLE smart.waypoint ALTER COLUMN last_modified SET NOT NULL;
ALTER TABLE smart.waypoint ADD COLUMN last_modified_by uuid;
 
update connect.connect_plugin_version set version = '6.1.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '6.1.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '6.1.0';				