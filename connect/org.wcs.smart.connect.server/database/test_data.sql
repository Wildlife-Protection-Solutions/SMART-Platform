insert into connect.users (uuid, username, password, email) values ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a36', 'smart', '$2a$12$85fjO64uLvgwaS1WtavLZ.J4OToU8fFo1pQFUlh6EIPVLbFgDffcS', 'smart@smart.com');

insert into connect.user_actions(uuid, username, action) values ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a37', 'smart', 'admin');

insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart', '3.2.1');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.intelligence', '3.2');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.intelligence.query', '1.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.plan', '3.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.cybertracker', '3.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.entity.query', '2.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.entity', '1.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.er', '1.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.er.query', '2.0');

insert into alert_types values('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50', 'emergency', 'Emergency');
insert into alert_types values('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51', 'observation', 'Observation');
insert into alert_types values('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52', 'intelligence', 'Intelligence');
insert into alert_types values('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a53', 'patrolposition', 'Patrol Position');

insert into alerts values('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21','mynewalert',to_date('05 Dec 2000', 'DD Mon YYYY'),'test description, some stuffsdfsdr drsdf','c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51',2,'a0eedf99-9c0c-4ef8-bb6d-6bb9bd340a36','ACTIVE',12,23,'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a36');