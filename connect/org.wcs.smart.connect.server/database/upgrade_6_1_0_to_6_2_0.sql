--ICON SUPORT
CREATE TABLE smart.iconset (
  uuid UUID not null, 
  keyid varchar(64) not null, 
  ca_uuid UUID not null, 
  is_default boolean default false not null, 
  primary key(uuid)
);

CREATE TABLE smart.icon (
  uuid UUID not null,
  keyid varchar(64) not null,
  ca_uuid UUID not null,
  primary key(uuid)
);

CREATE TABLE smart.iconfile (
  uuid UUID not null, 
  icon_uuid UUID not null, 
  iconset_uuid UUID not null, 
  filename varchar(2064) not null, 
  primary key(uuid)
);
				
ALTER TABLE smart.dm_category add column icon_uuid UUID;
ALTER TABLE smart.dm_attribute add column icon_uuid UUID;
ALTER TABLE smart.dm_attribute_list add column icon_uuid UUID;
ALTER TABLE smart.dm_attribute_tree add column icon_uuid UUID;

ALTER TABLE smart.ct_incident_link ADD FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(UUID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;

ALTER TABLE smart.dm_category ADD FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.dm_attribute ADD FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.dm_attribute_list ADD FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.dm_attribute_tree ADD FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE smart.configurable_model add column iconset_uuid UUID;
ALTER TABLE smart.configurable_model ADD FOREIGN KEY (iconset_uuid) REFERENCES smart.iconset(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE smart.iconset ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.icon ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.iconfile ADD FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.iconfile ADD FOREIGN KEY (iconset_uuid) REFERENCES smart.iconset(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;


CREATE TRIGGER trg_iconset AFTER INSERT OR UPDATE OR DELETE ON smart.iconset FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_icon AFTER INSERT OR UPDATE OR DELETE ON smart.icon FOR EACH ROW execute procedure connect.trg_changelog_common();


CREATE OR REPLACE FUNCTION connect.trg_iconfile() RETURNS trigger AS $$
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
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, iset.CA_UUID 
 		FROM smart.iconset iset WHERE iset.uuid = ROW.iconset_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_iconfile AFTER INSERT OR UPDATE OR DELETE ON smart.iconfile FOR EACH ROW execute procedure connect.trg_iconfile();




update connect.connect_plugin_version set version = '6.2.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '6.2.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '6.2.0', last_updated = now();		
--done via connect java upgrade script; update connect.connect_version set filestore_version = '6.2.0';