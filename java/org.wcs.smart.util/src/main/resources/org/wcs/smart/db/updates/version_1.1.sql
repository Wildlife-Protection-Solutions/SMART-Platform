-- Database Updates For Version 1.1
update smart.db_version set version = '1.1';

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
  
-- DB Function that check if patrol in within a certain plan including sub-plans (used in platrol query filter) --
create function smart.patrolInPlan(patrol_uuid CHAR(16) FOR BIT DATA, uuidStr long varchar)
    returns boolean
    language java
    parameter style java
    reads sql data
    external name 'org.wcs.smart.plan.SmartPlanDbStored.patrolInPlan';
  
-- Translation options support for intelligence (copy current names as default language values, remove names column) -- 
INSERT INTO smart.I18N_LABEL(LANGUAGE_UUID, ELEMENT_UUID, VALUE) SELECT lang.UUID as LANG_UUID, i.UUID as ELEM_UUID, 
i.SHORT_NAME as VALUE FROM smart.LANGUAGE lang INNER JOIN smart.INTELLIGENCE i ON lang.CA_UUID = i.CA_UUID WHERE lang.isdefault;
ALTER TABLE smart.INTELLIGENCE DROP COLUMN SHORT_NAME;

-- DB stored function that returns name in current language for given element UUID -- 
create function smart.elementName(element_uuid CHAR(16) FOR BIT DATA)
    returns varchar(1024)
    language java
    parameter style java
    reads sql data
    external name 'org.wcs.smart.ca.Label.getElementName';
