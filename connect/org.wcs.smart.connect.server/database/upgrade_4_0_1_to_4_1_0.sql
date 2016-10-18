
CREATE TABLE connect.shared_links(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	owner_uuid uuid NOT NULL,
	expires_at timestamp NOT NULL,
	expires_after int NOT NULL,
	url varchar(2083) NOT NULL, --IE's supposed URL max, "no one will ever need more than 2083 characters"
	PRIMARY KEY (uuid)
)WITHOUT OIDS;

ALTER TABLE connect.shared_links ADD CONSTRAINT connect_shared_link_owner_uuid_fk foreign key (owner_uuid) REFERENCES connect.users(uuid) ON UPDATE restrict ON DELETE cascade;
ALTER TABLE connect.shared_links
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES connect.ca_info (ca_uuid)
	ON UPDATE RESTRICT
	ON DELETE CASCADE;

alter table smart.conservation_area add column organization varchar(256);
alter table smart.conservation_area add column pointofcontact varchar(256);
alter table smart.conservation_area add column country varchar(256);
alter table smart.conservation_area add column owner varchar(256);
insert into smart.PATROL_TYPE (CA_UUID, PATROL_TYPE, IS_ACTIVE, MAX_SPEED) 
select DISTINCT CA_UUID, 'MIXED', true, 10000 from smart.PATROL_TYPE;

--update plugin versions
update connect.connect_plugin_version set version = '4.1.0' where plugin_id = 'org.wcs.smart';
update connect.ca_plugin_version set version = '4.1.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '4.1.0';
