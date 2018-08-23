-- last modified date and last modified by for waypoint
ALTER TABLE smart.waypoint ADD COLUMN last_modified timestamp;
UPDATE smart.waypoint SET last_modified = datetime;
ALTER TABLE smart.waypoint ALTER COLUMN last_modified SET NOT NULL;
ALTER TABLE smart.waypoint ADD COLUMN last_modified_by uuid;
 
-- incident to group id link for cybertracker
CREATE TABLE smart.ct_incident_link (
  uuid uuid  not null, 
  ct_group_id uuid not null, 
  wp_uuid uuid not null, 
  last_cnt integer not null, 
  primary key (uuid)
);		

ALTER TABLE smart.ct_incident_link ADD FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;
				 
CREATE OR REPLACE FUNCTION connect.trg_ct_incident_link() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, wp.CA_UUID 
 		FROM smart.waypoint wp WHERE wp.uuid = ROW.wp_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_ct_incident_link AFTER INSERT OR UPDATE OR DELETE ON smart.ct_incident_link FOR EACH ROW execute procedure connect.trg_ct_incident_link();


--support for svg images
ALTER TABLE smart.cm_node add column imagetype varchar(32);
ALTER TABLE smart.cm_attribute_list add column imagetype varchar(32);
ALTER TABLE smart.cm_attribute_tree_node add column imagetype varchar(32);

--smart source for records
alter table smart.i_record ADD COLUMN smart_source varchar(2048);

update connect.connect_plugin_version set version = '4.0' where plugin_id = 'org.wcs.smart.i2';
update connect.ca_plugin_version set version = '4.0' where plugin_id = 'org.wcs.smart.i2';

update connect.connect_plugin_version set version = '5.0' where plugin_id = 'org.wcs.smart.cybertracker';
update connect.ca_plugin_version set version = '5.0' where plugin_id = 'org.wcs.smart.cybertracker';

update connect.connect_plugin_version set version = '6.1.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '6.1.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '6.1.0';				