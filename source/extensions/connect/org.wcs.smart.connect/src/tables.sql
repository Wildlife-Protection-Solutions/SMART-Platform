CREATE TABLE smart.connect_server(
	uuid char(16) for bit data not null,
	ca_uuid char(16) for bit data,
	url varchar(2064),
	options varchar(32600),
	PRIMARY KEY (uuid)
);

ALTER TABLE smart.connect_server ADD CONSTRAINT server_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE restrict ON DELETE restrict;


CREATE TABLE smart.connect_account(
	employee_uuid char(16) for bit data not null,
	connect_uuid char(16) for bit data not null,
	connect_user varchar(32),
	connect_pass varchar(60),
	primary key(employee_uuid, connect_uuid)
);

ALTER TABLE smart.connect_account ADD CONSTRAINT connect_employee_uuid_fk foreign key (employee_uuid) REFERENCES smart.employee (uuid) ON UPDATE restrict ON DELETE restrict;


CREATE TABLE smart.connect_status(
	ca_uuid char(16) for bit data not null,
	connect_uuid char(16) for bit data not null,
	version char(16) for bit data,
	server_revision bigint not null,
	status varchar(6),
	uploadurl long varchar,
	localfile long varchar,
	primary key (ca_uuid)
);
ALTER TABLE smart.connect_status ADD CONSTRAINT connect_status_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE restrict ON DELETE restrict;
ALTER TABLE smart.connect_status ADD CONSTRAINT connect_status_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server (uuid) ON UPDATE restrict ON DELETE restrict;


CREATE TABLE smart.connect_change_log(
	uuid char(16) for bit data NOT NULL,
revision BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	action varchar(15) CONSTRAINT action_check CHECK (action in ('INSERT', 'UPDATE', 'DELETE', 'FS_INSERT', 'FS_DELETE', 'FS_UPDATE')),
	filename varchar(32672),
	tablename varchar(256),
	key1_fieldname varchar(256),
	key1 char(16) for bit data,
	key2_fieldname varchar(256),
	key2_str varchar(256),
	key2_uuid char(16) for bit data,
	ca_uuid char(16) for bit data,
	source varchar(6) default 'LOCAL'
	primary key (revision)
);
ALTER TABLE smart.connect_change_log ADD CONSTRAINT connect_changelog_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade;

CREATE TABLE smart.connect_sync_history(
	uuid char(16) for bit data not null,
	ca_uuid char(16) for bit data not null,
	connect_uuid char(16) for bit data not null,
	datetime timestamp not null,
	sync_type varchar(16) not null CONSTRAINT type_check CHECK ( sync_type in ('UPLOAD', 'DOWNLOAD') ),
	status varchar(16) not null CONSTRAINT status_check CHECK ( status in ('ACTIVE', 'ERROR', 'DONE') ),
	status_url varchar(32672),
	start_revision bigint,
	end_revision bigint,
	primary key(uuid)
);
ALTER TABLE smart.connect_sync_history ADD CONSTRAINT connect_sync_history_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE restrict;
ALTER TABLE smart.connect_sync_history ADD CONSTRAINT connect_sync_history_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server(uuid) ON UPDATE restrict ON DELETE restrict;


drop function smart.uuid;

CREATE FUNCTION smart.uuid() returns char(16) for bit data
LANGUAGE JAVA
NOT deterministic
external name 'org.wcs.smart.util.DerbyUtils.createUuid'
PARAMETER STYLE JAVA
NO SQL
RETURNS NULL ON NULL INPUT;