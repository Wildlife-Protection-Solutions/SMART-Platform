alter table smart.saved_maps drop column employee_uuid;

insert into smart.ca_projection(uuid, ca_uuid, name, definition, is_default)
values (x'00000000000000000000000000000001',x'00000000000000000000000000000000','WGS 84 [EPSG: 4326]', 'GEOGCS["WGS 84", DATUM["World Geodetic System 1984", SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]], AUTHORITY["EPSG","6326"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4326"]]', true);


-- ADD keys for team, mandate, and transport for cross-ca analysis
alter table smart.team add column keyid varchar(128);
alter table smart.patrol_mandate add column keyid varchar(128);
alter table smart.patrol_transport add column keyid varchar(128);

-- this will not do; keys need to be unique (per conservation area) 
-- only contain certain characters
-- and not start with numbers
--see KeyGenerator - the update script will need to run KeyGenerator for these tables
--update smart.team set keyid = (select lower(a.value) from smart.i18n_label a, smart.LANGUAGE c 
--where smart.team.uuid = a.element_uuid and a.language_uuid = c.uuid and c.isdefault);
--
--update smart.patrol_mandate set keyid = (select lower(a.value) from smart.i18n_label a, smart.LANGUAGE c 
--where smart.patrol_mandate.uuid = a.element_uuid and a.language_uuid = c.uuid and c.isdefault);
--
--update smart.patrol_transport set keyid = (select lower(a.value) from smart.i18n_label a, smart.LANGUAGE c 
--where smart.patrol_transport.uuid = a.element_uuid and a.language_uuid = c.uuid and c.isdefault);





update smart.db_version set version = '2.0.0';