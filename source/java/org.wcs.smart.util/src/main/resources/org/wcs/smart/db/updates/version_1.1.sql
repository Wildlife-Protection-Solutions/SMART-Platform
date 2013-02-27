-- Database Updates For Version 1.1
update smart.db_version set version = '1.1';

-- Add "Distance For Completion"("Success Distance") for Plan Target and set default value for previously added spacial targets to 250 --
ALTER TABLE smart.plan_target ADD success_distance INTEGER;
UPDATE smart.plan_target SET success_distance=250 WHERE category='SPATIAL';