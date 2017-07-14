--schema creation was accidentally left out of -> 5.0.0 sql script. Drop it if it already exists and add it.
DROP SCHEMA If exists query_temp CASCADE;
CREATE SCHEMA query_temp;

alter table connect.shared_links drop column expires_after;
alter table connect.shared_links ALTER COLUMN ca_uuid DROP NOT null;

update connect.connect_version set version = '5.0.1';