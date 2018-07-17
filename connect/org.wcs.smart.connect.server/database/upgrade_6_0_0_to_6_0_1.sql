CREATE TABLE smart.r_script(
   uuid UUID NOT NULL, 
   ca_uuid UUID NOT NULL,
   filename varchar(2048) NOT NULL, 
   creator_uuid UUID NOT NULL, 
   default_parameters varchar(32672), 
   PRIMARY KEY (uuid)
);
   
CREATE TABLE smart.r_query(
  uuid UUID NOT NULL, 
  script_uuid UUID NOT NULL, 
  ca_uuid UUID not null, 
  config varchar(32672), 
  PRIMARY KEY (uuid)
);

ALTER TABLE smart.r_script ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;
ALTER TABLE smart.r_script ADD FOREIGN KEY (creator_uuid) REFERENCES smart.employee(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;
ALTER TABLE smart.r_query ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;
ALTER TABLE smart.r_query ADD FOREIGN KEY (script_uuid) REFERENCES smart.r_script(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;

CREATE TRIGGER trg_qa_routine AFTER INSERT OR UPDATE OR DELETE ON smart.r_script FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_qa_routine AFTER INSERT OR UPDATE OR DELETE ON smart.r_query FOR EACH ROW execute procedure connect.trg_changelog_common();


alter table connect.shared_links add column permissionuser_uuid uuid ;
ALTER TABLE connect.shared_links ADD FOREIGN KEY (permissionuser_uuid) REFERENCES connect.users(uuid) ON DELETE CASCADE ;

insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.r', '1.0');
 
update connect.connect_plugin_version set version = '6.0.1' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '6.0.1' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '6.0.1';				