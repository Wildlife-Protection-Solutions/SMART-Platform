--schema creation was accidentally left out of -> 5.0.0 sql script. Drop it if it already exists and add it.
DROP SCHEMA If exists query_temp CASCADE;
CREATE SCHEMA query_temp;