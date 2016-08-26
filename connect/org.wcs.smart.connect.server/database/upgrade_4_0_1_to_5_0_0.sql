
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


