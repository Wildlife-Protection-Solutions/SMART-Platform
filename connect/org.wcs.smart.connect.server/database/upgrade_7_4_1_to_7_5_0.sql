
CREATE TABLE smart.i_patrol_record_motivation(
  patrol_uuid uuid NOT NULL, 
  i_record_uuid uuid NOT NULL, 
  PRIMARY KEY (i_record_uuid, patrol_uuid)
);

ALTER TABLE smart.i_patrol_record_motivation ADD CONSTRAINT i_patrol_record_motivation_patrol_fk FOREIGN KEY (patrol_uuid) REFERENCES smart.patrol(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE smart.i_patrol_record_motivation ADD CONSTRAINT i_patrol_record_motivation_record_fk FOREIGN KEY (i_record_uuid) REFERENCES smart.i_record(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

CREATE OR REPLACE FUNCTION connect.trg_patrol_record_motivation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
     INSERT INTO connect.change_log
         (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid)
         SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'patrol_uuid', ROW.patrol_uuid, 'i_record_uuid', ROW.i_record_uuid, null, p.CA_UUID
         FROM smart.patrol p WHERE p.uuid = ROW.patrol_uuid; 
RETURN ROW; END$$;

CREATE TRIGGER trg_patrol_record_motivation AFTER INSERT OR DELETE OR UPDATE ON smart.i_patrol_record_motivation FOR EACH ROW EXECUTE PROCEDURE connect.trg_patrol_record_motivation();

insert into connect.connect_plugin_version(plugin_id, version) values ('org.wcs.smart.i2.patrol', '1.0');

insert into connect.ca_plugin_version(ca_uuid, plugin_id, version) 
select ca_uuid, 'org.wcs.smart.i2.patrol', '1.0'
from connect.ca_plugin_version where plugin_id = 'org.wcs.smart.i2';

alter table smart.OBSERVATION_ATTACHMENT add column signature_type_uuid uuid;
alter table smart.observation_attachment ADD CONSTRAINT observation_attachment_sig_fk foreign key (signature_type_uuid)  references smart.signature_type(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;

update connect.connect_version set version = '7.5.0', last_updated = now();
