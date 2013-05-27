
grant select on smart.db_version to login;

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

-- clean up procedure for temporary data
create procedure smart.cleanUpTempData()
    language java
    parameter style java
    external security definer
    modifies sql data
    external name 'org.wcs.smart.query.SmartDbProcedure.cleanUpTempData';

GRANT EXECUTE ON PROCEDURE smart.cleanUpTempData TO login;
GRANT EXECUTE ON PROCEDURE smart.cleanUpTempData TO manager;
GRANT EXECUTE ON PROCEDURE smart.cleanUpTempData TO analyst;
GRANT EXECUTE ON PROCEDURE smart.cleanUpTempData TO data_entry;

GRANT EXECUTE ON FUNCTION smart.computeTileId to manager;
GRANT EXECUTE ON FUNCTION smart.computeTileId to analyst;
GRANT EXECUTE ON FUNCTION smart.pointInPolygon to manager;
GRANT EXECUTE ON FUNCTION smart.pointInPolygon to analyst;
GRANT EXECUTE ON FUNCTION smart.computeHours to manager;
GRANT EXECUTE ON FUNCTION smart.computeHours to analyst;
GRANT EXECUTE ON FUNCTION smart.intersection to manager;
GRANT EXECUTE ON FUNCTION smart.intersection to analyst;
GRANT EXECUTE ON FUNCTION smart.distanceInMeter to manager;
GRANT EXECUTE ON FUNCTION smart.distanceInMeter to analyst;
GRANT EXECUTE ON FUNCTION smart.intersects to manager;
GRANT EXECUTE ON FUNCTION smart.intersects to analyst;


--manager export CA updates
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_EXPORT_QUERY to manager;

GRANT SELECT ON smart.agency to manager;
GRANT SELECT ON smart.rank to manager;

-- update names
DELETE FROM smart.dm_aggregation_i18n;

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'en', 'sum' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'en', 'minimum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'en', 'maximum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'en', 'average' );

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'fr', 'total' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'fr', 'minimum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'fr', 'maximum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'fr', 'moyenne' );

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'es', 'total' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'es', 'mínimo');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'es', 'máximo');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'es', 'promedio' );

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'in', 'jumlah' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'in', 'minimum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'in', 'maksimum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'in', 'rata-rata' );

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'th', 'ผลรวม' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'th', 'ค่าต่ำสุด');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'th', 'ค่าสูงสุด');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'th', 'ค่าเฉลี่ย' );

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'zh', '合计' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'zh', '最小');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'zh', '最大');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'zh', '平均' );

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'ru', 'итога' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'ru', 'минимальный');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'ru', 'максимальный');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'ru', 'средний' );

GRANT SELECT ON smart.dm_aggregation_i18n TO manager;
GRANT SELECT ON smart.dm_aggregation_i18n TO analyst;

-- Database Updates For Version 1.1.0
update smart.db_version set version = '1.1.0';
