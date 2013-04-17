--spelling fix
RENAME COLUMN smart.employee.startemployementdate to startemploymentdate;
RENAME COLUMN smart.employee.endemployementdate to endemploymentdate;
 
-- create a "shared" cross ca employee for storing shared queries
insert into smart.employee (uuid, ca_uuid, id, givenname, familyname, startemploymentdate, endemploymentdate, datecreated, birthdate, gender, smartuserid, smartpassword,smartuserlevel, agency_uuid, rank_uuid)
values(x'00000000000000000000000000000000', -- uuid
x'00000000000000000000000000000000',  -- ca uuid
'SMART', --id
'',  --given name
'',  --family name
'1950-01-01', -- start employment date
null,  -- end employment date
'1950-01-01', --date created
null,  -- birthdate
'M',   -- gender
null,  -- smart user id
null,  --smart password
null,  --smart user level
null,  --agency
null); --rank

update smart.query_folder set employee_uuid = x'00000000000000000000000000000000' where ca_uuid = x'00000000000000000000000000000000';
update smart.waypoint_query set creator_uuid = x'00000000000000000000000000000000' where ca_uuid = x'00000000000000000000000000000000';
update smart.gridded_query set creator_uuid = x'00000000000000000000000000000000' where ca_uuid = x'00000000000000000000000000000000';
update smart.summary_query set creator_uuid = x'00000000000000000000000000000000' where ca_uuid = x'00000000000000000000000000000000';
update smart.patrol_query set creator_uuid = x'00000000000000000000000000000000' where ca_uuid = x'00000000000000000000000000000000';