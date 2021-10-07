CREATE OR REPLACE FUNCTION connect.trg_survey_waypoint() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
         SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'wp_uuid', ROW.wp_uuid, null, null, null, wp.CA_UUID
         FROM smart.waypoint wp
         WHERE wp.uuid = ROW.wp_uuid;
     RETURN ROW;
END$$;

update connect.connect_plugin_version set version = '4.0' where plugin_id = 'org.wcs.smart.er';
update connect.ca_plugin_version set version = '4.0' where plugin_id = 'org.wcs.smart.er';
