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
  values (x'00000000000000000000000000000000', 'XXX', 'Cross Conservation Analysis','Internal CA for Cross Conservation Analysis', 'Internal');=======


