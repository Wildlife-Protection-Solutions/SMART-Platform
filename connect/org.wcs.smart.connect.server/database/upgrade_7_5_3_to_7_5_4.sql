
ALTER TABLE smart.survey_waypoint ADD CONSTRAINT survey_waypoint_wp_uuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(uuid) ON DELETE CASCADE DEFERRABLE;

--update versions
update connect.connect_plugin_version set version = '5.0' where plugin_id = 'org.wcs.smart.er';
update connect.ca_plugin_version set version = '5.0' where plugin_id = 'org.wcs.smart.er';

update connect.connect_plugin_version set version = '7.5.4' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '7.5.4' where plugin_id = 'org.wcs.smart';

--don't update the version as we are not deploying a new jar file
--update connect.connect_version set version = '7.5.4', last_updated = now();
update connect.connect_version set last_updated = now();