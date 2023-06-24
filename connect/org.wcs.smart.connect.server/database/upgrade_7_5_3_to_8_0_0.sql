
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


--remove icon, iconfiles from conservation areas that are not referenced
--leave any custom icons if they are used or not  
DELETE FROM smart.ICONFILE WHERE icon_uuid not in (select distinct icon_uuid from smart.dm_attribute where icon_uuid is not null union  select distinct icon_uuid from smart.DM_ATTRIBUTE_LIST where icon_uuid is not null union select distinct icon_uuid from smart.DM_ATTRIBUTE_TREE where icon_uuid is not null union select distinct icon_uuid from smart.DM_CATEGORY where icon_uuid is not null ) and  filename like 'platform%';
DELETE FROM smart.ICON WHERE uuid not in (SELECT icon_uuid FROM smart.iconfile);


-- patrol metadata icons
ALTER TABLE smart.patrol_mandate ADD COLUMN icon_uuid uuid;
ALTER table smart.patrol_mandate ADD CONSTRAINT pm_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.patrol_transport ADD COLUMN icon_uuid uuid;
ALTER table smart.patrol_transport ADD CONSTRAINT ptransport_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.team ADD COLUMN icon_uuid uuid;
ALTER table smart.team ADD CONSTRAINT team_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.patrol_attribute ADD COLUMN icon_uuid uuid;
ALTER table smart.patrol_attribute ADD CONSTRAINT patrol_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.patrol_attribute_list ADD COLUMN icon_uuid uuid;
ALTER TABLE smart.patrol_attribute_list ADD CONSTRAINT patrol_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.station ADD COLUMN icon_uuid uuid;
ALTER TABLE smart.station ADD CONSTRAINT station_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;

--mission metadata icons

-- 5 to 6 upgrade for er
ALTER TABLE smart.mission_attribute ADD COLUMN icon_uuid uuid;
ALTER table smart.mission_attribute ADD CONSTRAINT mission_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.mission_attribute_list ADD COLUMN icon_uuid uuid;
ALTER table smart.mission_attribute_list ADD CONSTRAINT mission_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.sampling_unit_attribute ADD COLUMN icon_uuid uuid;
ALTER table smart.sampling_unit_attribute ADD CONSTRAINT su_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.sampling_unit_attribute_list ADD COLUMN icon_uuid uuid;
ALTER table smart.sampling_unit_attribute_list ADD CONSTRAINT su_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;

-- 4 to 5 upgrade for er
ALTER TABLE SMART.SURVEY_WAYPOINT ADD CONSTRAINT SURVEY_WAYPOINT_WP_UUID_FK FOREIGN KEY (WP_UUID) REFERENCES SMART.WAYPOINT(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE				
				
				
--working item				
alter table connect.work_item add column percent_complete smallint;
alter table connect.work_item add column data varchar;						
ALTER TABLE "connect".work_item drop CONSTRAINT type_chk; 
ALTER TABLE "connect".work_item ADD CONSTRAINT type_chk CHECK (((type)::text = ANY (ARRAY[('UP_CA'::character varying)::text, ('UP_SYNC'::character varying)::text, ('DOWN_CA'::character varying)::text, ('DOWN_SYNC'::character varying)::text, ('UP_DATAQUEUE'::character varying)::text, ('UP_CTPACKAGE'::character varying)::text, ('UP_NAVIGATION'::character varying)::text, ('RECOVERY_CA'::character varying)::text])));

update connect.connect_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker';
update connect.ca_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker';


update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query';
update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query';

update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa';


update connect.connect_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.er';
update connect.ca_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.er';

update connect.connect_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '8.0.0', last_updated = now();
