alter table smart.data_link drop constraint "data_link_provider_id_key";

alter table smart.data_link add constraint data_link_provider_id_unq unique(provider_id, data_type);
alter table smart.data_link add constraint data_link_smart_id_unq unique(smart_id);

--update versions
update connect.connect_plugin_version set version = '7.5.3' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '7.5.3' where plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '7.5.3', last_updated = now();