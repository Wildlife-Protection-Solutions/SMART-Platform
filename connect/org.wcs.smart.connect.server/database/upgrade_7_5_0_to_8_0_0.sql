
ALTER TABLE SMART.ASSET_WAYPOINT_QUERY DROP COLUMN SURVEYDESIGN_KEY;

update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query';
update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query';

update connect.connect_version set version = '8.0.0', last_updated = now();
