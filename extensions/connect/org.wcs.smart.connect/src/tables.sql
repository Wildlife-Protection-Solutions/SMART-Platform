create table smart.connect_server(
uuid char(16) for bit data not null,
ca_uuid char(16) for bit data,
url varchar(2064),
timeout bigint,
PRIMARY KEY (uuid));

alter table smart.connect_server add constraint server_ca_uuid_fk foreign key (ca_uuid) references smart.conservation_area (uuid) on update restrict on delete restrict;


create table smart.connect_account(
employee_uuid char(16) for bit data not null,
connect_uuid char(16) for bit data not null,
connect_user varchar(32),
connect_pass varchar(60),
primary key(employee_uuid, connect_uuid));

alter table smart.connect_account add constraint connect_employee_uuid_fk foreign key (employee_uuid) references smart.employee (uuid) on update restrict on delete restrict;


create table smart.connect_status(
ca_uuid char(16) for bit data not null,
connect_uuid char(16) for bit data not null,
client_id char(16) for bit data not null,
version char(16) for bit data,
revision bigint,
status varchar(6),
uploadurl long varchar,
localfile long varchar,
primary key (ca_uuid));

alter table smart.connect_status add constraint connect_status_ca_uuid_fk foreign key (ca_uuid) references smart.conservation_area (uuid) on update restrict on delete restrict;

alter table smart.connect_status add constraint connect_status_connect_uuid_fk foreign key (connect_uuid) references smart.connect_server (uuid) on update restrict on delete restrict;


create table smart.connect_change_log(
	revision BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	action varchar(15) CONSTRAINT action_check CHECK (action in ('INSERT', 'UPDATE', 'DELETE', 'FS_INSERT', 'FS_DELETE', 'FS_UPDATE')),
	filename varchar(32700),
	tablename varchar(256),
	key1_fieldname varchar(256),
	key1 char(16) for bit data,
	key2_fieldname varchar(256),
	key2_str varchar(256),
	key2_uuid char(16) for bit data,
	ca_uuid char(16) for bit data,
	primary key (revision)
);
alter table smart.connect_change_log add constraint connect_changelog_ca_uuid_fk foreign key (ca_uuid) references smart.conservation_area(uuid) on update restrict on delete restrict;


