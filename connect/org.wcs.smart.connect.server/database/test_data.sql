insert into connect.connect_version (version) values ('4.0');

insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart', '4.0.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.intelligence', '4.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.intelligence.query', '2.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.plan', '4.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.cybertracker', '4.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.entity.query', '3.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.entity', '2.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.er', '2.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.er.query', '3.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.connect', '1.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.connect.dataqueue', '1.0');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.connect.cybertracker', '1.0');

-- the user should configure all of these; running these statements for anything other than
--testing may cause issues with uuids in the future
--insert into connect.users (uuid, username, password, email) values ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a36', 'smart', '$2a$12$85fjO64uLvgwaS1WtavLZ.J4OToU8fFo1pQFUlh6EIPVLbFgDffcS', 'smart@smart.com');
--insert into connect.user_actions(uuid, username, action) values ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a37', 'smart', 'admin');
--insert into connect.alert_types values('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50', 'emergency', 'Emergency','#80AEFF','#000000','.8','exclamation','orage','f');
--insert into connect.alert_types values('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51', 'observation', 'Observation','#a0AEFF','#000000','.8','fire','green','f');
--insert into connect.alert_types values('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52', 'intelligence', 'Intelligence','#20AE4F','#000000','.8','home','blue','f');
--insert into connect.alert_types values('e0eebc99-9c0b-4ef8-bb6d-91b9bd380a53', 'patrolposition', 'Patrol Position','#305E5F','#000000','.8','eye','red','f');
--insert into connect.alerts values('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21','mynewalert',to_date('05 Dec 2000', 'DD Mon YYYY'),'test description, some stuffsdfsdr drsdf','c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51',2,'a0eedf99-9c0c-4ef8-bb6d-6bb9bd340a36','ACTIVE',12,23,'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a36');
--insert into connect.ca_info values('a0eedf99-9c0c-4ef8-bb6d-6bb9bd340a36','b0efdf99-9c0c-4ee4-bb6d-6bb9bd340a36','test ca 1','NODATA');
--insert into connect.map_layers values('c5aabc99-9c0b-4ef8-bf6d-6bb9bd350a80',1,true, 'pk.eyJ1IjoiamVmZmxvdW4iLCJhIjoiOTYyMGFkZDk5ZWM2ZDQ5NDc5Njc2Y2ZlOGM4YjQ1YWIifQ.R715pq8aRAM9hRdGcy10Xg', 'jeffloun.mp3jogfm','','streets',0);
--insert into connect.map_layers values('d5aabc99-9c0b-4ef8-bf6d-6bb9bd350a80',2,true, 'bdd66dd4ade33e6b69aed41b64b2b294', '','1084716:canada_major_lakes,1138164:bcschool','Schools and Lakes',1);

