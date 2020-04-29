ALTER TABLE smart.icon ADD CONSTRAINT keyidcaunq UNIQUE (keyid, ca_uuid);

update connect.connect_plugin_version set version = '6.3.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '6.3.0' where plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '6.3.0', last_updated = now();		

--done via connect java upgrade script; update connect.connect_version set filestore_version = '6.3.0';