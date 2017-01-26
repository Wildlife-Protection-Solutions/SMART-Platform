ALTER TABLE smart.employee ADD COLUMN usertemp VARCHAR(5000);
UPDATE smart.employee set usertemp = case when smartuserlevel = 0 THEN 'ADMIN' when smartuserlevel = 1 THEN 'MANAGER' WHEN smartuserlevel = 2  THEN 'ANALYST' when smartuserlevel=3 THEN 'DATAENTRY' ELSE null END;
ALTER TABLE smart.employee DROP COLUMN smartuserlevel;
ALTER TABLE smart.employee ADD COLUMN smartuserlevel VARCHAR(5000);
UPDATE smart.employee SET smartuserlevel = usertemp;
ALTER TABLE smart.employee DROP COLUMN usertemp;

alter table smart.CONFIGURABLE_MODEL ADD COLUMN instant_gps BOOLEAN;
alter table smart.CONFIGURABLE_MODEL ADD COLUMN photo_first BOOLEAN;


update connect.connect_plugin_version set version = '5.0.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '5.0.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '5.0.0';


insert into connect.plugin_version (version, plugin_id) values ('1.0', 'org.wcs.smart.i2')