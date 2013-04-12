-- Database Updates For Version 1.0.5
update smart.db_version set version = '1.0.5';

-- Add "Distance For Completion"("Success Distance") for Plan Target and set default value for previously added spacial targets to 250 --
ALTER TABLE smart.plan_target ADD success_distance INTEGER;
UPDATE smart.plan_target SET success_distance=250 WHERE category='SPATIAL';

-- Remove un-used columns
alter table smart.conservation_area drop column default_srs;
alter table smart.conservation_area drop column plan_point_buffer;

-- Add a default ca for cross-ca analysis.
insert into smart.conservation_area
  (uuid,id,name,description,designation)
  values (x'00000000000000000000000000000000', 'XXX', 'Cross Conservation Analysis','Internal CA for Cross Conservation Analysis', 'Internal');
  
-- Translation options support for intelligence (copy current names as default language values, remove names column) -- 
INSERT INTO smart.I18N_LABEL(LANGUAGE_UUID, ELEMENT_UUID, VALUE) SELECT lang.UUID as LANG_UUID, i.UUID as ELEM_UUID, 
i.SHORT_NAME as VALUE FROM smart.LANGUAGE lang INNER JOIN smart.INTELLIGENCE i ON lang.CA_UUID = i.CA_UUID WHERE lang.isdefault;
ALTER TABLE smart.INTELLIGENCE DROP COLUMN SHORT_NAME;

-- Translation options support for plans (copy current names as default language values, remove names column) -- 
INSERT INTO smart.I18N_LABEL(LANGUAGE_UUID, ELEMENT_UUID, VALUE) SELECT lang.UUID as LANG_UUID, p.UUID as ELEM_UUID, 
coalesce(p.NAME, '') as VALUE FROM smart.LANGUAGE lang INNER JOIN smart.PLAN p ON lang.CA_UUID = p.CA_UUID WHERE lang.isdefault;
ALTER TABLE smart.PLAN DROP COLUMN NAME;

GRANT ALL PRIVILEGES ON smart.i18n_label TO data_entry;


-- Addition spatial functions to support group by areas in queries
CREATE FUNCTION smart.intersection(wkb1 blob, wkb2 blob) returns blob
LANGUAGE JAVA
deterministic
external name 'org.wcs.smart.util.GeometryUtils.intersection'
PARAMETER STYLE JAVA
NO SQL
RETURNS NULL ON NULL INPUT;

CREATE FUNCTION smart.distanceInMeter(wkb1 blob) returns double
LANGUAGE JAVA
deterministic
external name 'org.wcs.smart.util.GeometryUtils.distanceInMeter'
PARAMETER STYLE JAVA
NO SQL
RETURNS NULL ON NULL INPUT;

CREATE FUNCTION smart.computeHours(wkb1 blob, wkb2 blob) returns double
LANGUAGE JAVA
deterministic
external name 'org.wcs.smart.util.GeometryUtils.computeHours'
PARAMETER STYLE JAVA
NO SQL
RETURNS NULL ON NULL INPUT;

-- Convert code from char(5) to varchar(5) to deal
-- with two character languages without adding extra spacing
-- to code.
ALTER TABLE smart.language ADD COLUMN codeA VARCHAR(5);
UPDATE smart.language SET codeA = trim(code);
ALTER TABLE smart.language DROP COLUMN code;
RENAME COLUMN smart.language.codeA TO code;


-- Remove hatbox functionality as we are not currently using this 
-- anywhere in our code and it takes up extra time/space
drop table smart.area_geometries_hatbox;
drop trigger smart.area_geometries_instrg;
drop trigger smart.area_geometries_updtrg;
drop trigger smart.area_geometries_deltrg;
       
drop procedure HATBOX.SPATIALIZE_PROC;
drop procedure HATBOX.BUILD_INDEX_PROC;
drop procedure HATBOX.DE_SPATIALIZE_PROC;
drop procedure HATBOX.INS_SPATIAL_PROC;
drop procedure HATBOX.UPD_SPATIAL_PROC;
drop procedure HATBOX.DEL_SPATIAL_PROC;
drop procedure HATBOX.SAVE_ENTRY_PROC;

drop function HATBOX.MBR_INTERSECTS_ENV;
drop function HATBOX.INTERSECTS_WKB;
drop function HATBOX.INTERSECTS_WKT;
drop function HATBOX.QUERY_WITH_PREDICATE_WKB;
drop function HATBOX.QUERY_WITH_PREDICATE_WKT;

drop schema HATBOX restrict;
