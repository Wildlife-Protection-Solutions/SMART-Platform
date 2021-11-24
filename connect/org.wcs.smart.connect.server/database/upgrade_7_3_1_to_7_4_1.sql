
--disable change tracking 
SET session_replication_role = replica;
update smart.WAYPOINT set last_modified_by = null where last_modified_by not in (select uuid from smart.employee);
ALTER TABLE smart.employee_team_member ADD FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
SET session_replication_role = DEFAULT;

update connect.connect_plugin_version set version = '7.4.1' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '7.4.1' where plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '7.4.1', last_updated = now();
