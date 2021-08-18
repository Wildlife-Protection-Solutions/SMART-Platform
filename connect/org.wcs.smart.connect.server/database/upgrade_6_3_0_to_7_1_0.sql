alter table connect.ct_package add column is_private boolean default false;

update connect.connect_version set version = '7.1.0', last_updated = now();
