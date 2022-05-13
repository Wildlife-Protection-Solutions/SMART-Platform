
ALTER TABLE SMART.ASSET_WAYPOINT_QUERY DROP COLUMN SURVEYDESIGN_KEY;

--breaking change for postgresql 14
--Require custom server parameter names to use only characters that are valid in unquoted SQL identifiers (Tom Lane)

CREATE OR REPLACE FUNCTION connect.dolog(cauuid uuid) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
    canrun boolean;
BEGIN
    --check if we should log this ca
    select current_setting('ca.trigger.t' || replace(cauuid::varchar, '-', '')) into canrun;
    return canrun;
    EXCEPTION WHEN others THEN
        RETURN TRUE;
END$$;


ALTER TABLE smart.ct_metadata_value ADD COLUMN is_required boolean default false not null;


update connect.connect_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker';
update connect.ca_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker';


update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query';
update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query';

update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa';

update connect.connect_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '8.0.0', last_updated = now();
