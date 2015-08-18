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




