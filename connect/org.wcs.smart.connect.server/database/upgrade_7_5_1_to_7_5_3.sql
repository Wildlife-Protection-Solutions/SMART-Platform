alter table smart.data_link drop constraint "data_link_provider_id_key";

alter table smart.data_link add constraint data_link_provider_id_unq unique(provider_id, data_type);
alter table smart.data_link add constraint data_link_smart_id_unq unique(smart_id);



CREATE OR REPLACE FUNCTION connect.trg_changelog_after() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
BEGIN
    PERFORM pg_advisory_unlock_shared(a.lock_key) FROM connect.ca_info a WHERE a.ca_uuid = NEW.ca_uuid;
RETURN NEW; END$$;


CREATE OR REPLACE FUNCTION connect.trg_changelog_before() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  canlock boolean;
BEGIN
    --check if we should log this ca
    IF (NOT connect.dolog(NEW.ca_uuid)) THEN RETURN NULL; END IF;
    SELECT pg_try_advisory_lock_shared(a.lock_key) into canlock FROM connect.ca_info a WHERE a.ca_uuid = NEW.ca_uuid;
    IF (canlock) THEN return NEW; ELSE RAISE EXCEPTION 'Database Locked to Editing'; END IF;
END$$;



--update versions
update connect.connect_plugin_version set version = '7.5.3' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '7.5.3' where plugin_id = 'org.wcs.smart';
update connect.connect_version set version = '7.5.3', last_updated = now();