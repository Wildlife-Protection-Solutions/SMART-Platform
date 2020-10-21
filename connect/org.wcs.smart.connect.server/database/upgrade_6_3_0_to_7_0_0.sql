alter table smart.LANGUAGE alter column code set data type varchar(8);
alter table smart.cm_attribute_option alter COLUMN string_value set data type varchar(32672);

-- DISTANCE & BEARING(DIRECTION) SUPPORT
CREATE OR REPLACE FUNCTION smart.projectPoint(x double precision, y double precision, distance float, direction float) RETURNS GEOMETRY AS $$
DECLARE
 a double precision;
 dR double precision;
 rx double precision;
 ry double precision;
 prjy1 double precision;
 prjx1 double precision;
 prjy double precision;
 prjx double precision;
BEGIN
  a := radians(direction);
  dR := distance / 6378100;		
  ry := radians(y);
  rx := radians(x);
  prjy1 := asin( sin(ry) * cos(dR) + cos(ry) * sin(dR) * cos(a) );
  prjx1 := rx + atan2(sin(a) * sin(dR) * cos(ry), cos(dR) - sin(ry) * sin(prjy1));
  prjx := degrees(prjx1);
  prjy := degrees(prjy1);
  RETURN ST_MAKEPOINT(prjx, prjy);
END;
$$LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION smart.pointinpolygon(x double precision, y double precision, distance float, direction float, geom bytea) RETURNS BOOLEAN AS $$
BEGIN
	IF (distance IS NOT NULL AND direction IS NOT NULL) THEN
		RETURN ST_INTERSECTS(smart.projectPoint(x, y, distance, direction), st_geomfromwkb(geom));
	END IF;
	RETURN ST_INTERSECTS(ST_MAKEPOINT(x, y), st_geomfromwkb(geom));

END;
$$LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION smart.trackIntersects(geom1 bytea, geom2 bytea) RETURNS BOOLEAN AS $$
DECLARE
  ls geometry;
  pnt geometry;
BEGIN
	ls := st_geomfromwkb(geom1);
	if not st_isvalid(ls) and st_length(ls) = 0 then
		pnt = st_pointn(ls, 1);
		return smart.pointinpolygon(st_x(pnt),st_y(pnt), null, null, geom2);
	else
		RETURN ST_INTERSECTS(ls, st_geomfromwkb(geom2));
	end if;

END;
$$LANGUAGE plpgsql;

-- also update to support track multilinestrings
CREATE OR REPLACE FUNCTION smart.trackIntersects(geom1 bytea, geom2 bytea) RETURNS BOOLEAN AS $$
DECLARE
  ls geometry;
  pnt geometry;
BEGIN
	ls := st_geomfromwkb(geom1);
	
	IF (UPPER(st_geometrytype(ls)) = 'ST_MULTILINESTRING' ) THEN
		FOR i in 1..ST_NumGeometries(ls) LOOP
			IF (smart.trackIntersects(st_geometryn(ls, i), geom2)) THEN
				RETURN true;
			END IF;
		END LOOP;
	END IF;
	if not st_isvalid(ls) and st_length(ls) = 0 then
		pnt = st_pointn(ls, 1);
		return smart.pointinpolygon(st_x(pnt),st_y(pnt), null, null, geom2);
	else
		RETURN ST_INTERSECTS(ls, st_geomfromwkb(geom2));
	end if;

END;
$$LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION smart.computeHoursPoly(polygon bytea, linestring bytea) RETURNS double precision AS $$
DECLARE
  ls geometry;
  p geometry;
  value double precision;
  ctime double precision;
  clength double precision;
  i integer;
  pnttemp geometry;
  pnttemp2 geometry;
  lstemp geometry;
BEGIN
	ls := st_geomfromwkb(linestring);
	p := st_geomfromwkb(polygon);
	
	IF (UPPER(st_geometrytype(ls)) = 'ST_MULTILINESTRING' ) THEN
		ctime = 0;
		FOR i in 1..ST_NumGeometries(ls) LOOP
			ctime := ctime + smart.computeHoursPoly(polygon, st_geometryn(ls, i));
		END LOOP;
		RETURN ctime;
	END IF;
	
	--wholly contained use entire time
	IF not st_isvalid(ls) and st_length(ls) = 0 THEN
		pnttemp = st_pointn(ls, 1);
		IF (smart.pointinpolygon(st_x(pnttemp),st_y(pnttemp), null, null, p)) THEN
			RETURN (st_z(st_endpoint(ls)) - st_z(st_startpoint(ls))) / 3600000.0;
		END IF;
		RETURN 0;
	END IF;
	
	IF (st_contains(p, ls)) THEN
		return (st_z(st_endpoint(ls)) - st_z(st_startpoint(ls))) / 3600000.0;
	END IF;
	
	value := 0;
	FOR i in 1..ST_NumPoints(ls)-1 LOOP
		pnttemp := st_pointn(ls, i);
		pnttemp2 := st_pointn(ls, i+1);
		lstemp := st_makeline(pnttemp, pnttemp2);	
		IF (NOT st_intersects(st_envelope(ls), st_envelope(lstemp))) THEN
			--do nothing; outside envelope
		ELSE
			IF (ST_COVERS(p, lstemp)) THEN
				value := value + st_z(pnttemp2) - st_z(pnttemp);
			ELSIF (ST_INTERSECTS(p, lstemp)) THEN
				ctime := st_z(pnttemp2) - st_z(pnttemp);
				clength := st_distance(pnttemp, pnttemp2);
				IF (clength = 0) THEN
					--points are the same and intersect so include the entire time
					value := value + ctime;
				ELSE
					--part in part out so linearly interpolate
					value := value + (ctime * (st_length(st_intersection(p, lstemp)) / clength));
				END IF;
			END IF;
		END IF;
	END LOOP;
	RETURN value / 3600000.0;
END;
$$LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION smart.computeTileId(x double precision, y double precision, distance float, direction float, srid integer, originX double precision, originY double precision, gridSize double precision) RETURNS VARCHAR AS $$
DECLARE 
  pnt geometry;
  tx integer;
  ty integer;
BEGIN
	IF (distance is not null and direction is not null) THEN
		pnt := st_transform(st_setsrid(smart.projectPoint(x,y,distance,direction), 4326), srid);
	ELSE
		pnt := st_transform(st_setsrid(st_makepoint(x,y), 4326), srid);
	END IF;
	tx := floor ( (st_x(pnt) - originX ) / gridSize) + 1;
	ty := floor ( (st_y(pnt) - originY ) / gridSize) + 1;
	RETURN tx || '_' || ty;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION smart.pointinpolygon(double precision, double precision, bytea);
DROP FUNCTION smart.computetileid (double precision, double precision, integer, double precision, double precision, double precision);

------- CT PACKAGES

create table connect.ct_package(
  uuid UUID not null,
  package_uuid UUID not null,
  ca_uuid UUID not null,
  uploaded_date timestamp not null,
  version varchar(256) not null,
  filename varchar(256) not null,
  package_type varchar(256) not null;
  name varchar(256) not null,
  status varchar(16) not null,
  work_item_uuid uuid,
  UNIQUE(package_uuid),
  primary key(uuid)
);

ALTER TABLE connect.ct_package ADD FOREIGN KEY (ca_uuid) REFERENCES connect.ca_info(ca_uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;


create table smart.ct_patrol_package(
 uuid uuid not null,
 name varchar(512),
 ca_uuid uuid not null,
 cm_uuid uuid,
 ctprofile_uuid uuid,
 has_incident boolean default false,
 incident_uuid uuid,
 basemapdef varchar(32672),
 primary key (uuid)
);

ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (cm_uuid) REFERENCES smart.configurable_model(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (incident_uuid) REFERENCES smart.configurable_model(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_patrol_package ADD FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_ct_patrol_package AFTER INSERT OR UPDATE OR DELETE ON smart.ct_patrol_package FOR EACH ROW execute procedure connect.trg_changelog_common();

create table smart.ct_survey_package(
 uuid uuid not null,
 name varchar(512),
 ca_uuid uuid not null,
 sd_uuid uuid,
 ctprofile_uuid uuid,
 has_incident boolean default false,
 incident_uuid uuid,
 basemapdef varchar(32672),
 primary key (uuid)
);

ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (sd_uuid) REFERENCES smart.survey_design(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (incident_uuid) REFERENCES smart.configurable_model(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_survey_package ADD FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_ct_survey_package AFTER INSERT OR UPDATE OR DELETE ON smart.ct_survey_package FOR EACH ROW execute procedure connect.trg_changelog_common();


alter table connect.work_item drop constraint type_chk;
ALTER TABLE connect.work_item ADD CONSTRAINT type_chk 
	CHECK (type IN ('UP_CA', 'UP_SYNC', 'DOWN_CA', 'DOWN_SYNC', 'UP_DATAQUEUE', 'UP_CTPACKAGE', 'UP_NAVIGATION'));

	
CREATE TABLE smart.ct_metadata_value(uuid uuid not null, ca_uuid uuid not null, package_uuid uuid not null, keyid varchar(32) not null, is_visible boolean not null, string_value varchar(8192), boolean_value boolean, uuid_value uuid, primary key (uuid));
CREATE TABLE smart.ct_metadata_value_uuid (uuid uuid NOT NULL, field_uuid uuid NOT NULL, uuid_value uuid NOT NULL, PRIMARY KEY (UUID));

ALTER TABLE smart.ct_metadata_value ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_metadata_value_uuid ADD FOREIGN KEY (field_uuid) REFERENCES smart.ct_metadata_value(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_ct_metadata_value AFTER INSERT OR UPDATE OR DELETE ON smart.ct_metadata_value FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.trg_ct_metadata_value_uuid() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.UUID, null, null, null, iset.CA_UUID 
 		FROM smart.ct_metadata_value iset WHERE iset.uuid = ROW.field_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_ct_metadata_value_uuid AFTER INSERT OR UPDATE OR DELETE ON smart.ct_metadata_value_uuid FOR EACH ROW execute procedure connect.trg_ct_metadata_value_uuid();


CREATE TABLE connect.ct_api_key(ca_uuid uuid not null, key_type varchar(32) not null, api_key varchar(64) not null, primary key (ca_uuid,key_type), unique(api_key));
ALTER TABLE connect.ct_api_key ADD FOREIGN KEY (ca_uuid) REFERENCES connect.ca_info(ca_uuid) on DELETE CASCADE on UPDATE RESTRICT; 

ALTER TABLE connect.alerts ADD COLUMN source VARCHAR(32) not null default 'USER';
ALTER TABLE connect.alerts ALTER COLUMN creator_uuid DROP not null;


CREATE TABLE smart.ct_navigation_layer(
  uuid uuid not null, 
  ca_uuid uuid not null, 
  name varchar(512), 
  targets bytea, 
  created_date date not null, 
  last_modified_date date, 
  last_modified_by uuid,  
primary key (uuid));

ALTER TABLE smart.ct_navigation_layer ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.ct_navigation_layer ADD FOREIGN KEY (last_modified_by) REFERENCES smart.employee(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
CREATE TRIGGER trg_ct_navigation_layer AFTER INSERT OR UPDATE OR DELETE ON smart.ct_navigation_layer FOR EACH ROW execute procedure connect.trg_changelog_common();



create table connect.ct_navigation_layer(
  uuid UUID not null,
  ca_uuid UUID not null,
  uploaded_date timestamp not null,
  filename varchar(256) not null,
  name varchar(256) not null,
  status varchar(16) not null,
  work_item_uuid uuid,
  primary key(uuid)
);
ALTER TABLE connect.ct_navigation_layer ADD FOREIGN KEY (ca_uuid) REFERENCES connect.ca_info(ca_uuid) on DELETE CASCADE on UPDATE RESTRICT;



------------ EMPLOYEE TEAMS ----------------
CREATE TABLE smart.employee_team (uuid uuid not null, ca_uuid uuid not null, primary key (uuid));
CREATE TABLE smart.employee_team_member (employee_uuid uuid not null, team_uuid uuid not null, primary key(employee_uuid, team_uuid));
	
ALTER TABLE smart.employee_team ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.employee_team_member ADD FOREIGN KEY (team_uuid) REFERENCES smart.employee_team(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.employee_team_member ADD FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_employee_team AFTER INSERT OR UPDATE OR DELETE ON smart.employee_team FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.trg_employee_team_member() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'team_uuid', ROW.team_uuid, 'employee_uuid', ROW.employee_uuid, null, t.CA_UUID 
 		FROM smart.employee_team t WHERE t.uuid = ROW.team_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_employee_team_member AFTER INSERT OR UPDATE OR DELETE ON smart.employee_team_member FOR EACH ROW execute procedure connect.trg_employee_team_member();

------- PATROL ATTRIBUTES -------------
CREATE TABLE SMART.PATROL_ATTRIBUTE(
  UUID UUID NOT NULL,  
  CA_UUID UUID NOT NULL,  
  KEYID VARCHAR(128) NOT NULL,  
  ATT_TYPE VARCHAR(7) NOT NULL,  
  IS_ACTIVE BOOLEAN NOT NULL,  
  PRIMARY KEY (UUID));
  
CREATE TABLE SMART.PATROL_ATTRIBUTE_LIST(
  UUID UUID NOT NULL,
  PATROL_ATTRIBUTE_UUID UUID NOT NULL,  
  KEYID VARCHAR(128) NOT NULL,  LIST_ORDER SMALLINT NOT NULL,  
  IS_ACTIVE BOOLEAN NOT NULL,  
  PRIMARY KEY (UUID));
  
CREATE TABLE SMART.PATROL_ATTRIBUTE_VALUE (
  PATROL_UUID UUID NOT NULL,
  PATROL_ATTRIBUTE_UUID UUID NOT NULL, 
  STRING_VALUE VARCHAR(1024), 
  NUMBER_VALUE DOUBLE PRECISION, 
  LIST_ITEM_UUID UUID, 
  PRIMARY KEY (PATROL_UUID, PATROL_ATTRIBUTE_UUID));
				
ALTER TABLE SMART.PATROL_ATTRIBUTE_LIST ADD FOREIGN KEY (PATROL_ATTRIBUTE_UUID) REFERENCES SMART.PATROL_ATTRIBUTE (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE ADD FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD FOREIGN KEY (PATROL_ATTRIBUTE_UUID) REFERENCES SMART.PATROL_ATTRIBUTE(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD FOREIGN KEY (LIST_ITEM_UUID) REFERENCES SMART.PATROL_ATTRIBUTE_LIST(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
				
CREATE TRIGGER trg_patrol_attribute AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.trg_patrol_attribute_list() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, t.CA_UUID 
 		FROM smart.patrol_attribute t WHERE t.uuid = ROW.patrol_attribute_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_patrol_attribute_list AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute_list FOR EACH ROW execute procedure connect.trg_patrol_attribute_list();

CREATE OR REPLACE FUNCTION connect.trg_patrol_attribute_value() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'patrol_uuid', ROW.patrol_uuid, 'patrol_attribute_uuid', ROW.patrol_attribute_uuid, null, t.CA_UUID 
 		FROM smart.patrol_attribute t WHERE t.uuid = ROW.patrol_attribute_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_patrol_attribute_value AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute_value FOR EACH ROW execute procedure connect.trg_patrol_attribute_value();


------------ PAWS ----------------
CREATE TABLE smart.paws_configuration(uuid uuid NOT NULL, ca_uuid uuid NOT NULL, name varchar(8192) NOT NULL, PRIMARY KEY (uuid));
CREATE TABLE smart.paws_parameter( uuid uuid NOT NULL, config_uuid uuid NOT NULL, keyid varchar(8192) NOT NULL, value varchar(8192), PRIMARY KEY (uuid));
CREATE TABLE smart.paws_query_class(uuid uuid NOT NULL, config_uuid uuid NOT NULL, query_uuid uuid NOT NULL, query_type varchar(32) NOT NULL, classification varchar(512) NOT NULL, PRIMARY KEY (uuid));
CREATE TABLE smart.paws_run(uuid uuid NOT NULL, ca_uuid uuid NOT NULL, config_uuid uuid, id varchar(256) NOT NULL, server_run_id varchar(256), run_date timestamp, package_file varchar(256), result_location varchar(256), status varchar(32) NOT NULL, status_message varchar, server_status_json varchar, train_start_year smallint, train_end_year smallint, forecast_start_year smallint, forecast_end_year smallint, container varchar(8192), paws_task_id varchar(8192), PRIMARY KEY (uuid));
CREATE TABLE smart.paws_service(uuid uuid NOT NULL, ca_uuid uuid NOT NULL UNIQUE, paws_api varchar(8192), task_api varchar(8192), paws_api_key varchar(8192), oauth_url varchar(8192), client_id varchar(8192), storage_account_url varchar(8192), PRIMARY KEY (uuid));
CREATE TABLE smart.paws_simple_class(uuid uuid NOT NULL, config_uuid uuid NOT NULL, classification varchar(512) NOT NULL, date_range varchar(512), category_hkey varchar(32672) NOT NULL, attribute_key varchar(128), list_key varchar(128), tree_hkey varchar(32672), PRIMARY KEY (uuid));

ALTER TABLE smart.paws_configuration ADD FOREIGN KEY(ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_run ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_service ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_parameter ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_run ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_query_class ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.paws_simple_class ADD FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--change log

CREATE TRIGGER trg_paws_configuration AFTER INSERT OR UPDATE OR DELETE ON smart.paws_configuration FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_paws_run AFTER INSERT OR UPDATE OR DELETE ON smart.paws_run FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_paws_service AFTER INSERT OR UPDATE OR DELETE ON smart.paws_service FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE OR REPLACE FUNCTION connect.trg_paws_config_join() RETURNS trigger AS $$
	DECLARE
	ROW RECORD;
BEGIN
	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN	
 	ROW = NEW;
 	ELSIF (TG_OP = 'DELETE') THEN
 		ROW = OLD;
 	END IF;
 
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, c.CA_UUID 
 		FROM smart.paws_configuration c WHERE c.uuid = ROW.config_uuid;
 RETURN ROW;
END$$ LANGUAGE 'plpgsql';
CREATE TRIGGER trg_paws_simple_class AFTER INSERT OR UPDATE OR DELETE ON smart.paws_simple_class FOR EACH ROW execute procedure connect.trg_paws_config_join();
CREATE TRIGGER trg_paws_query_class AFTER INSERT OR UPDATE OR DELETE ON smart.paws_query_class FOR EACH ROW execute procedure connect.trg_paws_config_join();
CREATE TRIGGER trg_paws_parameter AFTER INSERT OR UPDATE OR DELETE ON smart.paws_parameter FOR EACH ROW execute procedure connect.trg_paws_config_join();


---- Observation Groups -----
CREATE TABLE smart.wp_observation_group (uuid uuid not null, wp_uuid uuid not null, primary key (uuid));
INSERT INTO smart.wp_observation_group (uuid, wp_uuid) SELECT uuid_generate_v4(), uuid FROM smart.waypoint WHERE uuid in (SELECT o.wp_uuid FROM smart.wp_observation o);

CREATE TABLE smart.wp_observation_temp (uuid uuid not null, wp_group_uuid uuid not null, primary key (uuid))
INSERT INTO smart.wp_observation_temp (uuid, wp_group_uuid) SELECT a.uuid, b.uuid FROM smart.wp_observation a JOIN smart.wp_observation_group b on b.wp_uuid = a.wp_uuid
ALTER TABLE smart.wp_observation ADD COLUMN wp_group_uuid uuid;
UPDATE smart.wp_observation set wp_group_uuid = a.wp_group_uuid FROM smart.wp_observation_temp a WHERE a.uuid = smart.wp_observation.uuid;
DROP table smart.wp_observation_temp;

ALTER table smart.wp_observation drop column wp_uuid;
ALTER table smart.wp_observation_group ADD FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER table smart.wp_observation ADD FOREIGN KEY (wp_group_uuid) REFERENCES smart.wp_observation_group (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

DROP TRIGGER IF EXISTS trg_wp_observation ON smart.wp_observation;                                                                              
DROP FUNCTION IF EXISTS connect.trg_wp_observation();
DROP TRIGGER IF EXISTS trg_observation_attachment on smart.observation_attachment;
DROP FUNCTION IF EXISTS connect.trg_observation_attachment();

CREATE OR REPLACE FUNCTION connect.trg_observation_attachment() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, wp.CA_UUID 
 		FROM smart.wp_observation ob, smart.waypoint wp, smart.wp_observation_group g where ob.wp_group_uuid = g.uuid and g.wp_uuid = wp.uuid and ob.uuid = ROW.obs_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 

CREATE TRIGGER trg_observation_attachment AFTER INSERT OR UPDATE OR DELETE ON smart.observation_attachment FOR EACH ROW execute procedure connect.trg_observation_attachment();

CREATE OR REPLACE FUNCTION connect.trg_wp_observation() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, wp.CA_UUID 
 		FROM smart.waypoint wp, smart.wp_observation_group g WHERE wp.uuid = g.wp_uuid and g.uuid = ROW.wp_group_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 
CREATE TRIGGER trg_wp_observation AFTER INSERT OR UPDATE OR DELETE ON smart.wp_observation FOR EACH ROW execute procedure connect.trg_wp_observation();

CREATE OR REPLACE FUNCTION connect.trg_wp_group_observation() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, wp.CA_UUID 
 		FROM smart.waypoint wp WHERE wp.uuid = ROW.wp_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql'; 
CREATE TRIGGER trg_wp_group_observation AFTER INSERT OR UPDATE OR DELETE ON smart.wp_observation_group FOR EACH ROW execute procedure connect.trg_wp_group_observation();


-- ct observation groups

CREATE TABLE smart.ct_patrol_wplink(uuid uuid not null, ct_patrol_link_uuid uuid, ct_root_id uuid, ct_group_id uuid,  wp_uuid uuid, obs_group_uuid uuid, primary key (uuid));
ALTER TABLE SMART.ct_patrol_wplink ADD FOREIGN KEY (ct_patrol_link_uuid) REFERENCES smart.ct_patrol_link(CT_UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE smart.ct_mission_wplink(uuid uuid not null, ct_mission_link_uuid uuid, ct_root_id uuid, ct_group_id uuid,  wp_uuid uuid, obs_group_uuid uuid, primary key (uuid));
ALTER TABLE SMART.ct_mission_wplink ADD FOREIGN KEY (ct_mission_link_uuid) REFERENCES smart.ct_mission_link(CT_UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY DEFERRED;		
	
CREATE OR REPLACE FUNCTION connect.trg_ct_patrol_wplink() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, pp.CA_UUID 
		FROM smart.patrol pp, smart.patrol_leg pl, smart.ct_patrol_link l 
		WHERE pl.patrol_uuid = pp.uuid and pl.uuid = l.patrol_leg_uuid and l.ct_uuid = row.ct_patrol_link_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_ct_patrol_wplink AFTER INSERT OR UPDATE OR DELETE ON smart.ct_patrol_wplink FOR EACH ROW execute procedure connect.trg_ct_patrol_wplink();
	
			

CREATE OR REPLACE FUNCTION connect.trg_ct_mission_wplink() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, sd.CA_UUID 
 		FROM smart.mission mm, smart.survey s, smart.survey_design sd, smart.ct_mission_wplink l WHERE mm.survey_uuid = s.uuid and s.survey_design_uuid = sd.uuid and mm.uuid = l.mission_uuid and l.ct_uuid = row.ct_mission_link.uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_ct_mission_wplink AFTER INSERT OR UPDATE OR DELETE ON smart.ct_mission_wplink FOR EACH ROW execute procedure connect.trg_ct_mission_wplink();

ALTER TABLE smart.ct_incident_link drop column last_cnt;
ALTER TABLE smart.ct_incident_link add column ct_root_id uuid;
ALTER TABLE smart.ct_incident_link add column obs_group_uuid uuid;
ALTER TABLE smart.ct_incident_link alter column ct_group_id drop not null;


-------- MULTI PROFILES ------------
CREATE TABLE smart.i_profile_config(uuid uuid not null, ca_uuid uuid not null, keyid varchar(128) not null, color int, primary key (uuid));
CREATE TABLE smart.i_profile_entity_type(entity_type_uuid uuid not null, profile_uuid uuid not null, primary key (entity_type_uuid, profile_uuid));
CREATE TABLE smart.i_profile_record_source(record_source_uuid uuid not null, profile_uuid uuid not null, primary key (record_source_uuid, profile_uuid));


ALTER TABLE smart.i_profile_entity_type ADD FOREIGN KEY (entity_type_uuid) REFERENCES smart.i_entity_type (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_profile_entity_type ADD FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_profile_record_source ADD FOREIGN KEY (record_source_uuid) REFERENCES smart.I_RECORDSOURCE (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_profile_record_source ADD FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE smart.i_entity_type ADD COLUMN dm_attribute_uuid uuid;
ALTER TABLE smart.i_entity_type ADD COLUMN dm_active_filter varchar;
ALTER TABLE smart.i_entity_type ADD FOREIGN KEY (dm_attribute_uuid) REFERENCES smart.dm_attribute(uuid) ON UPDATE RESTRICT ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED;


ALTER TABLE smart.i_entity ADD COLUMN profile_uuid uuid ;
ALTER TABLE smart.i_entity ADD FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_entity ADD COLUMN dm_list_item_uuid uuid;
ALTER TABLE smart.i_entity ADD FOREIGN KEY (dm_list_item_uuid) REFERENCES smart.dm_attribute_list(uuid) ON UPDATE RESTRICT ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED;
				
ALTER TABLE smart.i_record ADD COLUMN profile_uuid uuid ;
ALTER TABLE smart.i_record ADD FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE smart.i_relationship_type ADD COLUMN src_profile_uuid uuid;
ALTER TABLE smart.i_relationship_type ADD COLUMN target_profile_uuid uuid;
ALTER TABLE smart.i_relationship_type ADD FOREIGN KEY (src_profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_relationship_type ADD FOREIGN KEY (target_profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;


CREATE TABLE smart.i_permission(employee_uuid uuid not null, profile_uuid uuid not null, permissions integer not null, primary key (employee_uuid, profile_uuid));
ALTER TABLE smart.i_permission ADD FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_permission ADD FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


ALTER TABLE smart.i_entity_record_query add column profile_filter varchar(32672);
ALTER TABLE smart.i_entity_summary_query add column profile_filter varchar(32672);
ALTER TABLE smart.i_record_obs_query add column profile_filter varchar(32672);

--must be done last to avoid problems adding
ALTER TABLE smart.i_profile_config ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;  

--entity summary queries
CREATE TABLE smart.i_record_summary_query(uuid uuid NOT NULL,ca_uuid uuid NOT NULL,query_string varchar(32700),date_created timestamp NOT NULL,last_modified_date timestamp,created_by uuid NOT NULL,last_modified_by uuid, profile_filter varchar(32672), PRIMARY KEY (uuid));

ALTER TABLE smart.i_record_summary_query ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_record_summary_query ADD FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_record_summary_query ADD FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;

--record query			
CREATE TABLE smart.i_record_query(uuid uuid NOT NULL,ca_uuid uuid NOT NULL,query_string varchar(32700),date_created timestamp NOT NULL,last_modified_date timestamp,created_by uuid NOT NULL,last_modified_by uuid, profile_filter varchar(32672), PRIMARY KEY (uuid));

ALTER TABLE smart.i_record_query ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_record_query ADD FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.i_record_query ADD FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;
		
ALTER TABLE smart.i_recordsource_attribute ADD COLUMN keyid varchar(128);
--remaining updates are done via in the code via the UpgradeServlet

--triggers
CREATE TRIGGER trg_i_profile_config AFTER INSERT OR UPDATE OR DELETE ON smart.i_profile_config FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_record_query AFTER INSERT OR UPDATE OR DELETE ON smart.i_record_query FOR EACH ROW execute procedure connect.trg_changelog_common();
CREATE TRIGGER trg_i_record_summary_query AFTER INSERT OR UPDATE OR DELETE ON smart.i_record_summary_query FOR EACH ROW execute procedure connect.trg_changelog_common();

		
CREATE OR REPLACE FUNCTION connect.trg_i_permission() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'employee_uuid', ROW.employee_uuid, 'profile_uuid', ROW.profile_uuid, null, i.CA_UUID 
 		FROM smart.i_profile_config i WHERE i.uuid = row.profile_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_i_permission AFTER INSERT OR UPDATE OR DELETE ON smart.i_permission FOR EACH ROW execute procedure connect.trg_i_permission();



CREATE OR REPLACE FUNCTION connect.i_profile_record_source() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'record_source_uuid', ROW.record_source_uuid, 'profile_uuid', ROW.profile_uuid, null, i.CA_UUID 
 		FROM smart.i_profile_config i WHERE i.uuid = row.profile_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_i_profile_record_source AFTER INSERT OR UPDATE OR DELETE ON smart.i_profile_record_source FOR EACH ROW execute procedure connect.i_profile_record_source();

CREATE OR REPLACE FUNCTION connect.i_profile_entity_type() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'entity_type_uuid', ROW.entity_type_uuid, 'profile_uuid', ROW.profile_uuid, null, i.CA_UUID 
 		FROM smart.i_profile_config i WHERE i.uuid = row.profile_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_i_profile_entity_type AFTER INSERT OR UPDATE OR DELETE ON smart.i_profile_entity_type FOR EACH ROW execute procedure connect.i_profile_entity_type();


--TODO: MAKE THESE UNIQUE and not null
update smart.I_RECORDSOURCE_ATTRIBUTE set keyid = (select a.keyid from smart.i_attribute a where a.uuid = smart.i_recordsource_attribute.attribute_uuid) where attribute_uuid is not null;
update smart.I_RECORDSOURCE_ATTRIBUTE set keyid = (select a.keyid from smart.i_entity_type a where a.uuid = smart.i_recordsource_attribute.entity_type_uuid) where entity_type_uuid is not null;

alter table smart.i_recordsource_attribute alter column keyid set not null;


update connect.connect_plugin_version set version = '5.0' where plugin_id = 'org.wcs.smart.i2';
update connect.ca_plugin_version set version = '5.0' where plugin_id = 'org.wcs.smart.i2';



-- functions for utm zone area
 CREATE OR REPLACE FUNCTION connect.toutm(lat double precision, long double precision) RETURNS integer AS $$
 DECLARE 
 	zone integer;
 	issouth boolean;
 	sql varchar;
 	srid integer;
 	rec record;
 BEGIN
	IF (lat < -80 OR lat > 84) THEN 
        	RETURN NULL;
        END IF;
 
 	zone := floor((long+180) / 6 ) + 1;
 	
 	IF (lat >= 0) THEN
 		issouth := false;
 	ELSE 
 		issouth := true;
 	END IF;
 	 
        IF ( lat >= 56.0 AND lat < 64.0 AND long >= 3.0 AND long < 12.0 ) THEN
        	zone := 32;
        END IF;        
         
        	IF ( lat >= 72.0 AND lat < 84.0 ) THEN
        	IF (long >= 0 AND long < 9.0) THEN
        		zone := 31;
        	ELSIF (long >= 9.0 and long < 21.0 ) THEN
        		zone := 33;
        	ELSIF (long >= 21.0 and long < 33.0 ) THEN
        		zone := 35;
        	ELSIF (long >= 33.0 and long < 42.0 ) THEN
        		zone := 37;
        	END IF;
	END IF;
      
	sql := 'SELECT srid FROM spatial_ref_sys WHERE proj4text like ''%proj=utm %'' AND proj4text like ''%zone=' || zone || ' %'' AND proj4text like ''%datum=WGS84 %''';
	IF (issouth = true) THEN
		sql := sql || ' AND proj4text like ''%south %''';
	ELSE
		sql := sql || ' AND proj4text not like ''%south %''';
	END IF;
      
	srid := null;
	FOR rec IN EXECUTE sql LOOP
		IF (srid is not null) THEN 
			RETURN NULL;
		END IF;
		srid := rec.srid;
	END LOOP;
      
	RETURN srid;
END;

$$LANGUAGE plpgsql;                     


CREATE OR REPLACE FUNCTION connect.utmarea(geom geometry) RETURNS double precision AS $$
DECLARE
	srid integer;
	centroid geometry;
BEGIN
	centroid := st_centroid(geom);
	srid := connect.toutm(st_y(centroid), st_x(centroid));
	IF (srid is null) THEN
		return st_area(geography(geom));
	END IF;
	RETURN st_area(st_transform( st_setsrid(geom, 4326), srid));
END;
$$LANGUAGE plpgsql;  



ALTER TABLE smart.i_entity_type_attribute add column is_duplicate_check boolean;
UPDATE smart.I_ENTITY_TYPE_ATTRIBUTE set IS_DUPLICATE_CHECK = (select true from smart.I_ENTITY_TYPE t where smart.I_ENTITY_TYPE_ATTRIBUTE.entity_type_uuid = t.uuid and t.id_attribute_uuid = smart.I_ENTITY_TYPE_ATTRIBUTE.attribute_uuid);
UPDATE smart.I_ENTITY_TYPE_ATTRIBUTE set IS_DUPLICATE_CHECK = false where IS_DUPLICATE_CHECK is null;
alter table smart.i_entity_type_attribute alter column is_duplicate_check set not null;

ALTER TABLE smart.i_recordsource_attribute add column is_duplicate_check boolean;
UPDATE smart.i_recordsource_attribute set IS_DUPLICATE_CHECK = false;
alter table smart.i_recordsource_attribute alter column is_duplicate_check set not null;

-- profile for event parameters
insert into smart.e_action_parameter_value(action_uuid, parameter_key, parameter_value)
select uuid, 'org.wcs.smart.profile.common.profile', 'profile1'
from smart.E_ACTION where type_key in ('org.wcs.smart.profile.newrecord', 'org.wcs.smart.profile.i2.newentity');


--- SMART Collect --- 
CREATE TABLE connect.smartcollect_user(
  uuid uuid not null, state varchar(32) not null, 
  source varchar(4096) not null, 
  validation_sent_date timestamp, 
  validation_key varchar(64), 
  primary key (uuid), 
  unique(source)
);


CREATE TABLE smart.smartcollect_waypoint(
  wp_uuid uuid not null,  
  source varchar(32000), 
  primary key(wp_uuid)
);

ALTER TABLE smart.smartcollect_waypoint ADD FOREIGN KEY (wp_uuid) 
  REFERENCES smart.waypoint(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE smart.smartcollect_package(
  uuid uuid not null, 
  name varchar(512), 
  ca_uuid uuid not null, 
  cm_uuid uuid, 
  ctprofile_uuid uuid,
  basemapdef varchar(32672), primary key (uuid));

ALTER TABLE smart.smartcollect_package ADD FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.smartcollect_package ADD FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE smart.smartcollect_package ADD FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER trg_smartcollect_package AFTER INSERT OR UPDATE OR DELETE ON 
smart.smartcollect_package FOR EACH ROW execute procedure connect.trg_changelog_common();

CREATE OR REPLACE FUNCTION connect.smartcollect_waypoint() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'wp_uuid', ROW.wp_uuid, null, null, null, wp.ca_uuid 
 		FROM smart.waypoint wp WHERE wp.uuid = row.wp_uuid;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_smartcollect_waypoint AFTER INSERT OR UPDATE OR DELETE ON smart.smartcollect_waypoint FOR EACH ROW execute procedure connect.smartcollect_waypoint();


--- remove orphaned patrol waypoints ---
DELETE FROM smart.waypoint WHERE SOURCE = 'PATROL' 
AND uuid NOT IN (SELECT wp_uuid FROM smart.PATROL_WAYPOINT);
					
				
---- remove intelligence plugin ----
DROP TABLE IF EXISTS smart.patrol_intelligence;
DROP TABLE IF EXISTS smart.intelligence_attachment;
DROP TABLE IF EXISTS smart.intelligence_point;
DROP TABLE IF EXISTS smart.intelligence;
DROP TABLE IF EXISTS smart.informant;
DROP TABLE IF EXISTS smart.intelligence_source;
DROP TABLE IF EXISTS smart.intel_record_query;
DROP TABLE IF EXISTS smart.intel_summary_query;


DELETE FROM connect.connect_plugin_version where plugin_id = 'org.wcs.smart.intelligence';
DELETE FROM connect.connect_plugin_version where plugin_id = 'org.wcs.smart.intelligence.query';
DELETE FROM connect.ca_plugin_version where plugin_id = 'org.wcs.smart.intelligence';
DELETE FROM connect.ca_plugin_version where plugin_id = 'org.wcs.smart.intelligence.query';


------- field sensor updates ----------
CREATE TABLE smart.asset_deployment_disruption(uuid uuid not null,asset_deployment_uuid uuid not null, start_date timestamp not null, end_date timestamp not null, comment varchar(32672), primary key (uuid));
ALTER TABLE smart.asset_deployment_disruption ADD FOREIGN KEY (asset_deployment_uuid) REFERENCES smart.asset_deployment (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

CREATE OR REPLACE FUNCTION connect.trg_asset_deployment_disruption() RETURNS trigger AS $$ DECLARE ROW RECORD; BEGIN IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN ROW = NEW; ELSIF (TG_OP = 'DELETE') THEN ROW = OLD; END IF;
 	INSERT INTO connect.change_log 
 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) 
 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, asset.ca_uuid 
 		FROM smart.asset_deployment deploy, smart.asset asset WHERE asset.uuid = deploy.asset_uuid and deploy.uuid = row.asset_deployment_uuid ;
RETURN ROW; END$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_asset_deployment_disruption AFTER INSERT OR UPDATE OR DELETE ON smart.asset_deployment_disruption FOR EACH ROW execute procedure connect.trg_asset_deployment_disruption();

ALTER TABLE smart.asset_metadata_mapping ADD COLUMN state varchar(10);
UPDATE smart.asset_metadata_mapping SET state = 'ENABLED';
ALTER TABLE smart.asset_metadata_mapping ALTER COLUMN state set not null;

ALTER TABLE smart.asset_summary_query ADD COLUMN query_type_key varchar (32);
UPDATE smart.asset_summary_query SET query_type_key = 'assetsummary';
ALTER TABLE smart.asset_summary_query alter column query_type_key set not null;


ALTER TABLE smart.asset_station add column buffer double precision;
ALTER TABLE smart.asset_station_location add column buffer double precision;
				
UPDATE smart.asset_station set buffer = (select c.value::double precision from smart.asset_module_settings c where c.keyid = 'station_buffer' and c.ca_uuid = smart.asset_station.ca_uuid);
UPDATE smart.asset_station set buffer = 50 where buffer is null or buffer < 0;
			
UPDATE smart.asset_station_location set buffer = (select c.value::double precision from smart.asset_module_settings c, smart.ASSET_STATION d where c.ca_uuid = d.ca_uuid and c.keyid = 'location_buffer' and d.uuid = smart.asset_station_location.station_uuid);
UPDATE smart.asset_station_location set buffer = 5 where buffer is null or buffer < 0;
				
ALTER TABLE smart.asset_station alter column buffer set not null;
ALTER TABLE smart.asset_station_location alter column buffer set not null;

-------- ER -----------
alter table smart.mission add column start_date date;
alter table smart.mission add column end_date date;

update smart.mission set start_date = cast(Start_datetime as date), end_date = cast(end_datetime as date);

alter table smart.mission drop column start_datetime;
alter table smart.mission drop column end_datetime;

alter table smart.survey drop column start_date;
alter table smart.survey drop column end_date;
alter table smart.survey_design drop column start_date;
alter table smart.survey_design drop column end_date;

--- Incident Packages ------
CREATE TABLE smart.ct_incident_package(uuid uuid not null, name varchar(512), ca_uuid uuid not null,cm_uuid uuid, ctprofile_uuid uuid, basemapdef varchar(32672), primary key (uuid));
ALTER TABLE SMART.ct_incident_package ADD CONSTRAINT ct_incident_package_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.ct_incident_package ADD CONSTRAINT ct_incident_package_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE SMART.ct_incident_package ADD CONSTRAINT ct_incident_package_ctprofile_uuid_fk FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER ct_incident_package AFTER INSERT OR UPDATE OR DELETE ON  smart.ct_incident_package FOR EACH ROW execute procedure connect.trg_changelog_common();


---------- timezones -------
-- assumes the timezone of the database server is the same as the web server
alter table connect.data_queue alter column uploaded_date set data type timestamp with time zone;
alter table connect.data_queue alter column lastmodified_date set data type timestamp with time zone;

alter table connect.ct_package alter column uploaded_date set data type timestamp with time zone;
alter table connect.ct_navigation_layer alter column uploaded_date set data type timestamp with time zone;

alter table connect.shared_links alter column expires_at set data type timestamp with time zone;
alter table connect.shared_links alter column date_created set data type timestamp with time zone;
alter table connect.alerts alter column date set data type timestamp without time zone;

------------ VERSIONS ------------
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.asset';
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.asset.query';
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.event';
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';
update connect.connect_plugin_version set version = '7.0' where plugin_id = 'org.wcs.smart.cybertracker';
update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.er';

insert into connect.connect_plugin_version (version, plugin_id) values ('1.0', 'org.wcs.smart.paws');
insert into connect.connect_plugin_version (version, plugin_id) values ('2.0', 'org.wcs.smart.cybertracker.incident');
insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.smartcollect', '1.0');

update connect.connect_plugin_version set version = '7.0.0' where plugin_id = 'org.wcs.smart';

update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.asset';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.asset.query';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.event';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol';
update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.cybertracker.survey';
update connect.ca_plugin_version set version = '7.0' where plugin_id = 'org.wcs.smart.cybertracker';
update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.er';
update connect.ca_plugin_version set version = '7.0.0' where plugin_id = 'org.wcs.smart';

update connect.connect_version set version = '7.0.0', last_updated = now();		
--update connect.connect_version set filestore_version = '7.0.0';


---- change ca version so users cannot sync with this and cause problems ---- 
update connect.ca_info SET version = uuid_generate_v4();
delete from connect.change_log;
delete from connect.change_log_history;