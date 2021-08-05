alter table connect.ct_package add column password varchar;

update connect.connect_version set version = '7.0.5', last_updated = now();		