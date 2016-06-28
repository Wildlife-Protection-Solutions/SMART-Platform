UPDATE smart.ca_projection set IS_DEFAULT = 'false' WHERE ca_uuid in (SELECT ca_uuid FROM smart.observation_options);
UPDATE smart.ca_projection set IS_DEFAULT = 'true' WHERE uuid IN (SELECT view_projection_uuid FROM smart.observation_options);
ALTER TABLE smart.observation_options DROP column view_projection_uuid;

update connect.connect_plugin_version set version = '4.0.1' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '4.0.1' where plugin_id = 'org.wcs.smart';