drop schema If exists smart cascade;
create schema smart; 

CREATE OR REPLACE FUNCTION smart.trimhkeytolevel(level integer, str varchar) RETURNS VARCHAR AS $$
BEGIN
	RETURN (regexp_matches(str, '(?:[a-zA-Z_0-9]*\.){' || level+1 || '}'))[1];
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION smart.pointinpolygon(x double precision ,y double precision, geom bytea) RETURNS BOOLEAN AS $$
BEGIN
	RETURN ST_INTERSECTS(ST_MAKEPOINT(x, y), st_geomfromwkb(geom));

END;
$$LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION smart.intersects(geom1 bytea, geom2 bytea) RETURNS BOOLEAN AS $$
BEGIN
	RETURN ST_INTERSECTS(st_geomfromwkb(geom1), st_geomfromwkb(geom2));

END;
$$LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION smart.distanceInMeter(geom bytea) RETURNS DOUBLE PRECISION AS $$
BEGIN
	RETURN ST_Length_Spheroid(st_force2d(st_geomfromwkb(geom)), 'SPHEROID["WGS 84",6378137,298.257223563]');

END;
$$LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION smart.intersection(geom1 bytea, geom2 bytea) RETURNS bytea AS $$
BEGIN
	RETURN st_asewkb(ST_INTERSECTION(st_geomfromwkb(geom1), st_geomfromwkb(geom2)), 'XDR');

END;
$$LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION smart.computeTileId(x double precision, y double precision, srid integer, originX double precision, originY double precision, gridSize double precision) RETURNS VARCHAR AS $$
DECLARE 
  pnt geometry;
  tx integer;
  ty integer;
BEGIN
	pnt := st_transform(st_setsrid(st_makepoint(x,y), 4326), srid);
	tx := floor ( (st_x(pnt) - originX ) / gridSize) + 1;
	ty := floor ( (st_y(pnt) - originY ) / gridSize) + 1;
	RETURN tx || '_' || ty;
END;
$$ LANGUAGE plpgsql;

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
	
	--wholly contained use entire time
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

CREATE OR REPLACE FUNCTION smart.computeHours(geometry bytea, linestring bytea) RETURNS double precision AS $$
DECLARE
  type varchar;
  value double precision;
  i integer;
  p geometry;
BEGIN
	p := st_geomfromwkb(geometry);
	type := st_geometrytype(p);
	IF (upper(type) = 'ST_POLYGON') THEN
		RETURN smart.computeHoursPoly(geometry, linestring);
	ELSIF (upper(type) = 'ST_MULTIPOLYGON') THEN
		value := 0;
		FOR i in 1..ST_NumGeometries(p) LOOP
			value := value + computeHoursPoly( st_asewkb(ST_GeometryN(p, i), 'XDR'), linestring);
		END LOOP;
		RETURN value;
	ELSIF (upper(type) = 'ST_GEOMETRYCOLLECTION') THEN
		value := 0;
		FOR i in 1..ST_NumGeometries(p) LOOP
			value := value + computeHours(ST_GeometryN(p, i), linestring);
		END LOOP;
		RETURN value;
	END IF;
	RETURN 0;

END;
$$LANGUAGE plpgsql;

CREATE TABLE smart.agency
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.area_geometries
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   AREA_TYPE varchar(5) NOT NULL,
   KEYID varchar(256),
   GEOM bytea NOT NULL,
   PRIMARY KEY (UUID) 
);


CREATE TABLE smart.ca_projection
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   NAME varchar(1024) NOT NULL,
   DEFINITION varchar NOT NULL,
   IS_DEFAULT boolean,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute
(
   UUID uuid NOT NULL,
   NODE_UUID uuid NOT NULL,
   ATTRIBUTE_UUID uuid NOT NULL,
   ATTRIBUTE_ORDER smallint,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute_list
(
   UUID uuid NOT NULL,
   CM_UUID uuid NOT NULL,
   LIST_ELEMENT_UUID uuid NOT NULL,
   IS_ACTIVE boolean NOT NULL,
   CM_ATTRIBUTE_UUID uuid,
   DM_ATTRIBUTE_UUID uuid,
   LIST_ORDER smallint,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute_option
(
   UUID uuid NOT NULL,
   CM_ATTRIBUTE_UUID uuid NOT NULL,
   OPTION_ID varchar(128) NOT NULL,
   NUMBER_VALUE float(52),
   STRING_VALUE varchar(1024),
   UUID_VALUE uuid,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute_tree_node
(
   UUID uuid NOT NULL,
   CM_UUID uuid NOT NULL,
   DM_TREE_NODE_UUID uuid,
   IS_ACTIVE boolean NOT NULL,
   CM_ATTRIBUTE_UUID uuid,
   DM_ATTRIBUTE_UUID uuid,
   PARENT_UUID uuid,
   NODE_ORDER smallint,
   DISPLAY_MODE VARCHAR(10),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_node
(
   UUID uuid NOT NULL,
   CM_UUID uuid NOT NULL,
   CATEGORY_UUID uuid,
   PARENT_NODE_UUID uuid,
   NODE_ORDER smallint,
   PHOTO_ALLOWED boolean,
   PHOTO_REQUIRED boolean,
   COLLECT_MULTIPLE_OBS boolean,
   USE_SINGLE_GPS_POINT boolean,
   DISPLAY_MODE VARCHAR(10),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.connect_alert
(
	UUID UUID NOT NULL,
	CM_UUID UUID NOT NULL,
	ALERT_ITEM_UUID UUID NOT NULL,
	CM_ATTRIBUTE_UUID UUID,
	LEVEL SMALLINT NOT NULL,
	TYPE VARCHAR(64),
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.configurable_model
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   DISPLAY_MODE VARCHAR(10),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.connect_ct_properties 
( 
	UUID uuid NOT NULL, 
	CM_UUID uuid NOT NULL, 
	PING_FREQUENCY INTEGER, 
	PRIMARY KEY (UUID)
);

CREATE TABLE SMART.CM_DM_ATTRIBUTE_SETTINGS 
(
	CM_UUID UUID NOT NULL, 
	DM_ATTRIBUTE_UUID UUID NOT NULL, 
	DISPLAY_MODE VARCHAR(10), 
	PRIMARY KEY (CM_UUID, DM_ATTRIBUTE_UUID)
);

CREATE TABLE smart.conservation_area
(
   UUID uuid NOT NULL,
   ID varchar(8) NOT NULL,
   NAME varchar(256),
   DESIGNATION varchar(1024),
   DESCRIPTION varchar(2056),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.ct_properties_option
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   OPTION_ID varchar(32) NOT NULL,
   DOUBLE_VALUE float(52),
   INTEGER_VALUE int,
   STRING_VALUE varchar(1024),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.ct_properties_profile (
  uuid UUID NOT NULL, 
  ca_uuid UUID NOT NULL, 
  IS_DEFAULT BOOLEAN, 
  PRIMARY KEY (UUID)
);

CREATE TABLE smart.ct_properties_profile_option (
  uuid UUID NOT NULL, 
  profile_uuid UUID NOT NULL, 
  OPTION_ID VARCHAR(32) NOT NULL, 
  DOUBLE_VALUE DOUBLE PRECISION, 
  INTEGER_VALUE INTEGER, 
  STRING_VALUE VARCHAR(1024), 
  PRIMARY KEY (UUID));

CREATE TABLE smart.cm_ct_properties_profile (
	cm_uuid UUID NOT NULL, 
	profile_uuid UUID NOT NULL, PRIMARY KEY (cm_uuid));


CREATE TABLE smart.db_version
(
   VERSION varchar(15) NOT NULL,
   PLUGIN_ID varchar(512) NOT NULL
);

CREATE TABLE smart.dm_aggregation
(
   NAME varchar(16) NOT NULL,
   PRIMARY KEY (NAME)
);

CREATE TABLE smart.dm_aggregation_i18n
(
   NAME varchar(16) NOT NULL,
   LANG_CODE varchar(5) NOT NULL,
   GUI_NAME varchar(96) NOT NULL,
   PRIMARY KEY (NAME,LANG_CODE)
);

CREATE TABLE smart.dm_att_agg_map
(
   ATTRIBUTE_UUID uuid NOT NULL,
   AGG_NAME varchar(16) NOT NULL,
   PRIMARY KEY (ATTRIBUTE_UUID,AGG_NAME)
);

CREATE TABLE smart.dm_attribute
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   IS_REQUIRED boolean NOT NULL,
   ATT_TYPE varchar(7) NOT NULL,
   MIN_VALUE float(52),
   MAX_VALUE float(52),
   REGEX varchar(1024),
   PRIMARY KEY (uuid) 
);

CREATE TABLE smart.dm_attribute_list
(
   UUID uuid NOT NULL,
   ATTRIBUTE_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   LIST_ORDER smallint NOT NULL,
   IS_ACTIVE boolean NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.dm_attribute_tree
(
   UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   NODE_ORDER smallint NOT NULL,
   PARENT_UUID uuid,
   ATTRIBUTE_UUID uuid,
   IS_ACTIVE boolean NOT NULL,
   HKEY varchar NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.dm_cat_att_map
(
   CATEGORY_UUID uuid NOT NULL,
   ATTRIBUTE_UUID uuid NOT NULL,
   ATT_ORDER smallint NOT NULL,
   IS_ACTIVE boolean NOT NULL,
   PRIMARY KEY (CATEGORY_UUID,ATTRIBUTE_UUID)
);

CREATE TABLE smart.dm_category
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   PARENT_CATEGORY_UUID uuid,
   IS_MULTIPLE boolean,
   CAT_ORDER smallint,
   IS_ACTIVE boolean NOT NULL,
   HKEY varchar NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.employee
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(32) NOT NULL,
   GIVENNAME varchar(64) NOT NULL,
   FAMILYNAME varchar(64) NOT NULL,
   STARTEMPLOYMENTDATE date NOT NULL,
   ENDEMPLOYMENTDATE date,
   DATECREATED date NOT NULL,
   BIRTHDATE date,
   GENDER char(1) NOT NULL,
   SMARTUSERID varchar(16),
   SMARTPASSWORD varchar(256),
   SMARTUSERLEVEL smallint,
   AGENCY_UUID uuid,
   RANK_UUID uuid,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.entity
(
   UUID uuid NOT NULL,
   ENTITY_TYPE_UUID uuid NOT NULL,
   ID varchar(32) NOT NULL,
   STATUS varchar(8) NOT NULL,
   ATTRIBUTE_LIST_ITEM_UUID uuid NOT NULL,
   X float(52),
   Y float(52),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.entity_attribute
(
   UUID uuid NOT NULL,
   ENTITY_TYPE_UUID uuid NOT NULL,
   DM_ATTRIBUTE_UUID uuid NOT NULL,
   IS_REQUIRED boolean DEFAULT false NOT NULL,
   IS_PRIMARY boolean DEFAULT true NOT NULL,
   ATTRIBUTE_ORDER int DEFAULT 1 NOT NULL,
   KEYID varchar(128) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.entity_attribute_value
(
   ENTITY_ATTRIBUTE_UUID uuid NOT NULL,
   ENTITY_UUID uuid NOT NULL,
   NUMBER_VALUE float(52),
   STRING_VALUE varchar(1024),
   LIST_ELEMENT_UUID uuid,
   TREE_NODE_UUID uuid,
   PRIMARY KEY (ENTITY_ATTRIBUTE_UUID,ENTITY_UUID)
);

CREATE TABLE smart.entity_gridded_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   QUERY_DEF varchar,
   FOLDER_UUID uuid,
   SHARED boolean NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(6) NOT NULL,
   CRS_DEFINITION varchar NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.entity_observation_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.entity_summary_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   CA_FILTER varchar,
   QUERY_DEF varchar,
   FOLDER_UUID uuid,
   SHARED boolean NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(6) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.entity_type
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   DATE_CREATED timestamp NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   STATUS varchar(16) NOT NULL,
   DM_ATTRIBUTE_UUID uuid NOT NULL,
   ENTITY_TYPE varchar(16),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.entity_waypoint_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.gridded_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   QUERY_DEF varchar,
   FOLDER_UUID uuid,
   SHARED boolean NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(6) NOT NULL,
   CRS_DEFINITION varchar NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.i18n_label
(
   LANGUAGE_UUID uuid NOT NULL,
   ELEMENT_UUID uuid NOT NULL,
   VALUE varchar(1024) NOT NULL,
   PRIMARY KEY (LANGUAGE_UUID,ELEMENT_UUID)
);

CREATE TABLE smart.informant
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(128),
   IS_ACTIVE boolean NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.intel_record_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.intel_summary_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.intelligence
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   RECEIVED_DATE date NOT NULL,
   PATROL_UUID uuid,
   FROM_DATE date NOT NULL,
   TO_DATE date,
   DESCRIPTION varchar,
   CREATOR_UUID uuid,
   SOURCE_UUID uuid,
   INFORMANT_UUID uuid,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.intelligence_attachment
(
   UUID uuid NOT NULL,
   INTELLIGENCE_UUID uuid NOT NULL,
   FILENAME varchar(1024) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.intelligence_point
(
   UUID uuid NOT NULL,
   INTELLIGENCE_UUID uuid NOT NULL,
   X float NOT NULL,
   Y float NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.intelligence_source
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   KEYID varchar(128),
   IS_ACTIVE boolean NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.language
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   ISDEFAULT boolean DEFAULT false NOT NULL,
   CODE varchar(5),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.map_styles
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   STYLE_STRING varchar NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.mission
(
   UUID uuid NOT NULL,
   SURVEY_UUID uuid NOT NULL,
   ID varchar(128) NOT NULL,
   START_DATETIME timestamp NOT NULL,
   END_DATETIME timestamp NOT NULL,
   COMMENT varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.mission_attribute
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   ATT_TYPE varchar(7) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.mission_attribute_list
(
   UUID uuid  NOT NULL,
   MISSION_ATTRIBUTE_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   LIST_ORDER smallint NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.mission_day
(
   UUID uuid NOT NULL,
   MISSION_UUID uuid NOT NULL,
   MISSION_DAY date NOT NULL,
   START_TIME time NOT NULL,
   END_TIME time NOT NULL,
   REST_MINUTES int,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.mission_member
(
   MISSION_UUID uuid NOT NULL,
   EMPLOYEE_UUID uuid NOT NULL,
   IS_LEADER boolean NOT NULL,
   PRIMARY KEY (MISSION_UUID,EMPLOYEE_UUID)
);

CREATE TABLE smart.mission_property
(
   SURVEY_DESIGN_UUID uuid NOT NULL,
   MISSION_ATTRIBUTE_UUID uuid NOT NULL,
   ATTRIBUTE_ORDER int NOT NULL,
   PRIMARY KEY (SURVEY_DESIGN_UUID,MISSION_ATTRIBUTE_UUID)
);

CREATE TABLE smart.mission_property_value
(
   MISSION_UUID uuid NOT NULL,
   MISSION_ATTRIBUTE_UUID uuid NOT NULL,
   NUMBER_VALUE float(52),
   STRING_VALUE varchar(1024),
   LIST_ELEMENT_UUID uuid,
   PRIMARY KEY (MISSION_UUID,MISSION_ATTRIBUTE_UUID)
);

CREATE TABLE smart.mission_track
(
   UUID uuid NOT NULL,
   MISSION_DAY_UUID uuid NOT NULL,
   SAMPLING_UNIT_UUID uuid,
   TRACK_TYPE varchar(32) NOT NULL,
   GEOMETRY bytea NOT NULL,
   ID varchar(128),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.obs_gridded_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   QUERY_DEF varchar,
   FOLDER_UUID uuid,
   SHARED boolean NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(6) NOT NULL,
   CRS_DEFINITION varchar NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.obs_observation_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.obs_summary_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   CA_FILTER varchar,
   QUERY_DEF varchar,
   FOLDER_UUID uuid,
   SHARED boolean NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(6) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.obs_waypoint_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.observation_attachment
(
   UUID uuid NOT NULL,
   OBS_UUID uuid NOT NULL,
   FILENAME varchar(1024) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.observation_options
(
   CA_UUID uuid NOT NULL,
   DISTANCE_DIRECTION boolean NOT NULL,
   EDIT_TIME smallint,
   VIEW_PROJECTION_UUID uuid,
   OBSERVER boolean DEFAULT false NOT NULL,
   PRIMARY KEY (CA_UUID)
);

CREATE TABLE smart.observation_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.patrol
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(32) NOT NULL,
   STATION_UUID uuid,
   TEAM_UUID uuid,
   OBJECTIVE varchar,
   MANDATE_UUID uuid,
   PATROL_TYPE varchar(6) NOT NULL,
   IS_ARMED boolean NOT NULL,
   START_DATE date NOT NULL,
   END_DATE date NOT NULL,
   COMMENT varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.patrol_intelligence
(
   PATROL_UUID uuid NOT NULL,
   INTELLIGENCE_UUID uuid NOT NULL,
   PRIMARY KEY (PATROL_UUID,INTELLIGENCE_UUID)
)
;
CREATE TABLE smart.patrol_leg
(
   UUID uuid NOT NULL,
   PATROL_UUID uuid NOT NULL,
   START_DATE date NOT NULL,
   END_DATE date NOT NULL,
   TRANSPORT_UUID uuid NOT NULL,
   ID varchar(50) NOT NULL,
   PRIMARY KEY (UUID)
   
);

CREATE TABLE smart.patrol_leg_day
(
   UUID uuid NOT NULL,
   PATROL_LEG_UUID uuid NOT NULL,
   PATROL_DAY date NOT NULL,
   START_TIME time,
   REST_MINUTES int,
   END_TIME time,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.patrol_leg_members
(
   PATROL_LEG_UUID uuid NOT NULL,
   EMPLOYEE_UUID uuid NOT NULL,
   IS_LEADER boolean  NOT NULL,
   IS_PILOT boolean NOT NULL,
   PRIMARY KEY (PATROL_LEG_UUID,EMPLOYEE_UUID)
);

CREATE TABLE smart.patrol_mandate
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   IS_ACTIVE boolean NOT NULL,
   KEYID varchar(128),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.patrol_plan
(
   PATROL_UUID uuid NOT NULL,
   PLAN_UUID uuid NOT NULL,
   PRIMARY KEY (PATROL_UUID,PLAN_UUID)
);

CREATE TABLE smart.patrol_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.patrol_transport
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   IS_ACTIVE boolean NOT NULL,
   PATROL_TYPE varchar(6) NOT NULL,
   KEYID varchar(128),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.patrol_type
(
   CA_UUID uuid NOT NULL,
   PATROL_TYPE varchar(6) NOT NULL,
   IS_ACTIVE boolean NOT NULL,
   max_speed INTEGER,
   PRIMARY KEY (CA_UUID,PATROL_TYPE)
);

CREATE TABLE smart.patrol_waypoint
(
   WP_UUID uuid NOT NULL,
   LEG_DAY_UUID uuid NOT NULL,
   PRIMARY KEY (WP_UUID,LEG_DAY_UUID)
);

CREATE TABLE smart.plan
(
   UUID uuid NOT NULL,
   ID varchar(32) NOT NULL,
   START_DATE date NOT NULL,
   END_DATE date,
   TYPE varchar(32) NOT NULL,
   DESCRIPTION varchar(256),
   CA_UUID uuid NOT NULL,
   STATION_UUID uuid,
   TEAM_UUID uuid,
   ACTIVE_EMPLOYEES int,
   UNAVAILABLE_EMPLOYEES int,
   PARENT_UUID uuid,
   CREATOR_UUID uuid,
   COMMENT varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.plan_target
(
   UUID uuid NOT NULL,
   NAME varchar(32) NOT NULL,
   DESCRIPTION varchar(256),
   VALUE float,
   OP varchar(10),
   TYPE varchar(32),
   PLAN_UUID uuid NOT NULL,
   CATEGORY varchar(16) NOT NULL,
   COMPLETED boolean DEFAULT false NOT NULL,
   SUCCESS_DISTANCE int,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.plan_target_point
(
   UUID uuid NOT NULL,
   PLAN_TARGET_UUID uuid NOT NULL,
   X float NOT NULL,
   Y float NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.query_folder
(
   UUID uuid NOT NULL,
   EMPLOYEE_UUID uuid,
   CA_UUID uuid NOT NULL,
   PARENT_UUID uuid,
   PRIMARY KEY (UUID)
   
);

CREATE TABLE smart.rank
(
   UUID uuid NOT NULL,
   AGENCY_UUID uuid NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.report
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   ID varchar(6) NOT NULL,
   FILENAME varchar(2048) NOT NULL,
   CA_UUID uuid NOT NULL,
   SHARED boolean NOT NULL,
   FOLDER_UUID uuid,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.report_folder
(
   UUID uuid NOT NULL,
   EMPLOYEE_UUID uuid,
   CA_UUID uuid NOT NULL,
   PARENT_UUID uuid,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.report_query
(
   REPORT_UUID uuid NOT NULL,
   QUERY_UUID uuid NOT NULL,
   PRIMARY KEY (REPORT_UUID,QUERY_UUID)
);

CREATE TABLE smart.sampling_unit
(
   UUID uuid NOT NULL,
   SURVEY_DESIGN_UUID uuid NOT NULL,
   UNIT_TYPE varchar(32) NOT NULL,
   ID varchar(128),
   STATE varchar(8) NOT NULL,
   GEOMETRY bytea NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.sampling_unit_attribute
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   KEYID varchar(128),
   ATT_TYPE varchar(7),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.sampling_unit_attribute_list
(
   UUID uuid NOT NULL,
   SAMPLING_UNIT_ATTRIBUTE_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   LIST_ORDER smallint NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.sampling_unit_attribute_value
(
   SU_ATTRIBUTE_UUID uuid NOT NULL,
   SU_UUID uuid NOT NULL,
   STRING_VALUE varchar(1024),
   NUMBER_VALUE float(52),
   LIST_ELEMENT_UUID uuid,
   PRIMARY KEY (SU_ATTRIBUTE_UUID,SU_UUID)
);

CREATE TABLE smart.saved_maps
(
   UUID uuid NOT NULL,
   CA_UUID uuid,
   IS_DEFAULT boolean NOT NULL,
   MAP_DEF text NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.screen_option
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   TYPE varchar(10),
   IS_VISIBLE boolean,
   STRING_VALUE varchar,
   BOOLEAN_VALUE boolean,
   UUID_VALUE uuid,
   RESOURCE varchar(10),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.screen_option_uuid
(
   UUID uuid NOT NULL,
   OPTION_UUID uuid NOT NULL,
   UUID_VALUE uuid NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.station
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   DESC_UUID uuid,
   IS_ACTIVE boolean NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.summary_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   CA_FILTER varchar,
   QUERY_DEF  varchar,
   FOLDER_UUID uuid,
   SHARED boolean NOT NULL,
   CA_UUID uuid NOT NULL,
   ID varchar(6) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey
(
   UUID uuid NOT NULL,
   SURVEY_DESIGN_UUID uuid NOT NULL,
   ID varchar(128) NOT NULL,
   START_DATE date,
   END_DATE date,
   PRIMARY KEY (UUID)
)
;
CREATE TABLE smart.survey_design
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   KEYID varchar(128) NOT NULL,
   STATE varchar(16) NOT NULL,
   START_DATE date,
   END_DATE date,
   DISTANCE_DIRECTION boolean DEFAULT FALSE NOT NULL,
   DESCRIPTION varchar,
   CONFIGURABLE_MODEL_UUID uuid,
   OBSERVER boolean DEFAULT false NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey_design_property
(
   UUID uuid NOT NULL,
   SURVEY_DESIGN_UUID uuid NOT NULL,
   NAME varchar(256) NOT NULL,
   VALUE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey_design_sampling_unit
(
   SURVEY_DESIGN_UUID uuid NOT NULL,
   SU_ATTRIBUTE_UUID uuid NOT NULL,
   PRIMARY KEY (SURVEY_DESIGN_UUID,SU_ATTRIBUTE_UUID)
);

CREATE TABLE smart.survey_gridded_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   QUERY_DEF varchar,
   SHARED boolean NOT NULL,
   ID varchar(6) NOT NULL,
   CRS_DEFINITION varchar,
   SURVEYDESIGN_KEY varchar(128),
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey_mission_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SURVEYDESIGN_KEY varchar(128),
   SHARED boolean NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey_mission_track_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SURVEYDESIGN_KEY varchar(128),
   SHARED boolean NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey_observation_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SURVEYDESIGN_KEY varchar(128),
   SHARED boolean NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey_summary_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_DEF varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   SHARED boolean NOT NULL,
   ID varchar(6) NOT NULL,
   SURVEYDESIGN_KEY varchar(128),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.survey_waypoint
(
   WP_UUID uuid NOT NULL,
   MISSION_DAY_UUID uuid NOT NULL,
   SAMPLING_UNIT_UUID uuid,
   MISSION_TRACK_UUID uuid,
   PRIMARY KEY (WP_UUID)
);

CREATE TABLE smart.survey_waypoint_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SURVEYDESIGN_KEY varchar(128),
   SHARED boolean NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.team
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   IS_ACTIVE boolean NOT NULL,
   DESC_UUID uuid,
   PATROL_MANDATE_UUID uuid,
   KEYID varchar(128),
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.track
(
   UUID uuid NOT NULL,
   PATROL_LEG_DAY_UUID uuid NOT NULL,
   GEOMETRY bytea NOT NULL,
   DISTANCE real NOT NULL,
   PRIMARY KEY (UUID,PATROL_LEG_DAY_UUID)
);

CREATE TABLE smart.waypoint
(
   UUID uuid NOT NULL,
   CA_UUID uuid NOT NULL,
   SOURCE varchar(16) NOT NULL,
   ID int NOT NULL,
   X float NOT NULL,
   Y float NOT NULL,
   DATETIME timestamp NOT NULL,
   DIRECTION real,
   DISTANCE real,
   WP_COMMENT varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.waypoint_query
(
   UUID uuid NOT NULL,
   CREATOR_UUID uuid NOT NULL,
   QUERY_FILTER varchar,
   CA_FILTER varchar,
   CA_UUID uuid NOT NULL,
   FOLDER_UUID uuid,
   COLUMN_FILTER varchar,
   SHARED boolean DEFAULT false NOT NULL,
   ID varchar(6) NOT NULL,
   STYLE varchar,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.wp_attachments
(
   UUID uuid NOT NULL,
   WP_UUID uuid NOT NULL,
   FILENAME varchar(1024) NOT NULL,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.wp_observation
(
   UUID uuid NOT NULL,
   WP_UUID uuid NOT NULL,
   CATEGORY_UUID uuid NOT NULL,
   EMPLOYEE_UUID uuid,
   PRIMARY KEY (UUID)
);

CREATE TABLE smart.wp_observation_attributes
(
   OBSERVATION_UUID uuid NOT NULL,
   ATTRIBUTE_UUID uuid NOT NULL,
   LIST_ELEMENT_UUID uuid,
   TREE_NODE_UUID uuid,
   NUMBER_VALUE float(52),
   STRING_VALUE varchar(1024),
   PRIMARY KEY (OBSERVATION_UUID,ATTRIBUTE_UUID)
);

ALTER TABLE smart.agency
ADD CONSTRAINT AGENCY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE
DEFERRABLE;

ALTER TABLE smart.AREA_GEOMETRIES
ADD CONSTRAINT AREA_GEOMETRIES_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE
DEFERRABLE;

ALTER TABLE smart.CA_PROJECTION
ADD CONSTRAINT CA_PROJECTION_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE 
DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE
ADD CONSTRAINT CM_ATTRIBUTE_NODE_UUID_FK
FOREIGN KEY (NODE_UUID)
REFERENCES smart.CM_NODE(UUID) ON DELETE CASCADE
DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE
ADD CONSTRAINT CM_ATTRIBUTE_ATTRIBUTE_UUID_FK
FOREIGN KEY (ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE
DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_LIST
ADD CONSTRAINT CM_ATTRIBUTE_LIST_CM_UUID_FK
FOREIGN KEY (CM_UUID)
REFERENCES smart.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE
DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_LIST
ADD CONSTRAINT CM_ATTRIBUTE_LIST_CM_ATTRIBUTE_UUID_FK
FOREIGN KEY (CM_ATTRIBUTE_UUID)
REFERENCES smart.CM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_LIST
ADD CONSTRAINT CM_ATTRIBUTE_LIST_DM_ATTRIBUTE_UUID_FK
FOREIGN KEY (DM_ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_LIST
ADD CONSTRAINT CM_ATTRIBUTE_LIST_LIST_ELEMENT_UUID_FK
FOREIGN KEY (LIST_ELEMENT_UUID)
REFERENCES smart.DM_ATTRIBUTE_LIST(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_OPTION
ADD CONSTRAINT CM_ATTRIBUTE_OPTION_CM_ATTRIBUTE_UUID_FK
FOREIGN KEY (CM_ATTRIBUTE_UUID)
REFERENCES smart.CM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_TREE_NODE
ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_TREE_NODE_UUID_FK
FOREIGN KEY (DM_TREE_NODE_UUID)
REFERENCES smart.DM_ATTRIBUTE_TREE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_TREE_NODE
ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_CM_UUID_FK
FOREIGN KEY (CM_UUID)
REFERENCES smart.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_TREE_NODE
ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_CM_ATTRIBUTE_UUID_FK
FOREIGN KEY (CM_ATTRIBUTE_UUID)
REFERENCES smart.CM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_TREE_NODE
ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_DM_ATTRIBUTE_UUID_FK
FOREIGN KEY (DM_ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_ATTRIBUTE_TREE_NODE
ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_PARENT_UUID_FK
FOREIGN KEY (PARENT_UUID)
REFERENCES smart.CM_ATTRIBUTE_TREE_NODE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_NODE
ADD CONSTRAINT CM_NODE_CATEGORY_UUID_FK
FOREIGN KEY (CATEGORY_UUID)
REFERENCES smart.DM_CATEGORY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CM_NODE
ADD CONSTRAINT CM_NODE_CM_UUID_FK
FOREIGN KEY (CM_UUID)
REFERENCES smart.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE SMART.CM_DM_ATTRIBUTE_SETTINGS 
ADD CONSTRAINT CM_DM_ATTRIBUTE_SETTINGS_CM_UUID_FK 
FOREIGN KEY (CM_UUID) 
REFERENCES SMART.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE SMART.CM_DM_ATTRIBUTE_SETTINGS 
ADD CONSTRAINT CM_DM_ATTRIBUTE_SETTINGS_DM_ATTRIBUTE_UUID_FK 
FOREIGN KEY (DM_ATTRIBUTE_UUID) 
REFERENCES SMART.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CONFIGURABLE_MODEL
ADD CONSTRAINT CONFIGURABLE_MODEL_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.connect_alert 
ADD CONSTRAINT connect_alert_cm_uuid_fk 
FOREIGN KEY (CM_UUID) 
REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.connect_alert 
ADD CONSTRAINT connect_alert_cm_attribute_uuid_fk 
FOREIGN KEY (CM_ATTRIBUTE_UUID) 
REFERENCES smart.cm_attribute(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.CT_PROPERTIES_OPTION
ADD CONSTRAINT CT_PROPERTIES_OPTION_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ct_properties_profile 
ADD CONSTRAINT CT_PROPERTIES_PROFILE_CA_UUID_FK 
FOREIGN KEY (CA_UUID) 
REFERENCES SMART.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE ;

ALTER TABLE smart.ct_properties_profile_option 
ADD CONSTRAINT CT_PROPERTIES_PROFILE_OPTION_PROFILE_UUID_FK 
FOREIGN KEY (profile_uuid) 
REFERENCES smart.ct_properties_profile(UUID) ON DELETE CASCADE DEFERRABLE ;

ALTER TABLE smart.cm_ct_properties_profile 
ADD CONSTRAINT CM_CT_PROPERTIES_PROFILE_CM_UUID_FK 
FOREIGN KEY (CM_UUID) 
REFERENCES SMART.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.cm_ct_properties_profile 
ADD CONSTRAINT CM_CT_PROPERTIES_PROFILE_PROFILE_UUID_FK 
FOREIGN KEY (PROFILE_UUID)
REFERENCES SMART.CT_PROPERTIES_PROFILE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.DM_AGGREGATION_I18N
ADD CONSTRAINT DM_AGGREGATION_I18N_FK
FOREIGN KEY (NAME)
REFERENCES smart.DM_AGGREGATION(NAME) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.DM_ATT_AGG_MAP
ADD CONSTRAINT DM_ATT_AGG_MAP_AGG_NAME_FK
FOREIGN KEY (AGG_NAME)
REFERENCES smart.DM_AGGREGATION(NAME)  DEFERRABLE;

ALTER TABLE smart.DM_ATT_AGG_MAP
ADD CONSTRAINT DM_ATT_AGG_MAP_ATTRIBUTE_UUID_FK
FOREIGN KEY (ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.DM_ATTRIBUTE
ADD CONSTRAINT DM_ATTRIBUTE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.DM_ATTRIBUTE_LIST
ADD CONSTRAINT DM_ATTRIBUTE_LIST_ATTRIBUTE_UUID_FK
FOREIGN KEY (ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.DM_ATTRIBUTE_TREE
ADD CONSTRAINT DM_ATTRIBUT_UUID_FK
FOREIGN KEY (ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.DM_ATTRIBUTE_TREE
ADD CONSTRAINT DM_ATTRIBUT_TREE_PARENT_UUID_FK
FOREIGN KEY (PARENT_UUID)
REFERENCES smart.DM_ATTRIBUTE_TREE(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.DM_CAT_ATT_MAP
ADD CONSTRAINT DM_CAT_ATT_MAP_ATTRIBUTE_UUID_FK
FOREIGN KEY (ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.DM_CAT_ATT_MAP
ADD CONSTRAINT DM_CAT_ATT_MAP_CATEGORY_UUID_FK
FOREIGN KEY (CATEGORY_UUID)
REFERENCES smart.DM_CATEGORY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.DM_CATEGORY
ADD CONSTRAINT DM_CATEGORY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.DM_CATEGORY
ADD CONSTRAINT DM_CATEGORY_PARENT_CATEGORY_UUID_FK
FOREIGN KEY (PARENT_CATEGORY_UUID)
REFERENCES smart.DM_CATEGORY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.EMPLOYEE
ADD CONSTRAINT EMPLOYEE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.EMPLOYEE
ADD CONSTRAINT EMPLOYEE_AGENCY_UUID_FK
FOREIGN KEY (AGENCY_UUID)
REFERENCES smart.AGENCY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.EMPLOYEE
ADD CONSTRAINT EMPLOYEE_RANK_UUID_FK
FOREIGN KEY (RANK_UUID)
REFERENCES smart.RANK(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY
ADD CONSTRAINT ENTITY_TYPE_UUID_FK
FOREIGN KEY (ENTITY_TYPE_UUID)
REFERENCES smart.ENTITY_TYPE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY
ADD CONSTRAINT ENTITY_ATTRIBUTE_LIST_ITEM_UUID_FK
FOREIGN KEY (ATTRIBUTE_LIST_ITEM_UUID)
REFERENCES smart.DM_ATTRIBUTE_LIST(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_ATTRIBUTE
ADD CONSTRAINT ENTITY_ATTRIBUTE_DM_ATTRIBUTE_FK
FOREIGN KEY (DM_ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_ATTRIBUTE
ADD CONSTRAINT ENTITY_ATTRIBUTE_TYPE_UUID_FK
FOREIGN KEY (ENTITY_TYPE_UUID)
REFERENCES smart.ENTITY_TYPE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_ATTRIBUTE_VALUE
ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_LISTELEMENT_FK
FOREIGN KEY (LIST_ELEMENT_UUID)
REFERENCES smart.DM_ATTRIBUTE_LIST(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_ATTRIBUTE_VALUE
ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_ENTITY_FK
FOREIGN KEY (ENTITY_UUID)
REFERENCES smart.ENTITY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_ATTRIBUTE_VALUE
ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_TREENODE_FK
FOREIGN KEY (TREE_NODE_UUID)
REFERENCES smart.DM_ATTRIBUTE_TREE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_ATTRIBUTE_VALUE
ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_ATTRIBUTE_FK
FOREIGN KEY (ENTITY_ATTRIBUTE_UUID)
REFERENCES smart.ENTITY_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_GRIDDED_QUERY
ADD CONSTRAINT ENTITY_GRIDDED_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_GRIDDED_QUERY
ADD CONSTRAINT ENTITY_GRIDDED_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_GRIDDED_QUERY
ADD CONSTRAINT ENTITY_GRIDDED_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_OBSERVATION_QUERY
ADD CONSTRAINT ENTITY_OBSERVATION_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_OBSERVATION_QUERY
ADD CONSTRAINT ENTITYOBSERVATION_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_OBSERVATION_QUERY
ADD CONSTRAINT ENTITY_OBSERVATION_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_SUMMARY_QUERY
ADD CONSTRAINT ENTITY_SUMMARY_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_SUMMARY_QUERY
ADD CONSTRAINT ENTITY_SUMMARY_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_SUMMARY_QUERY
ADD CONSTRAINT ENTITY_SUMMARY_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_TYPE
ADD CONSTRAINT ENTITY_TYPE_DM_ATTRIBUTE_FK
FOREIGN KEY (DM_ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_TYPE
ADD CONSTRAINT ENTITY_TYPE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_WAYPOINT_QUERY
ADD CONSTRAINT ENTITY_WAYPOINT_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_WAYPOINT_QUERY
ADD CONSTRAINT ENTITY_WAYPOINT_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.ENTITY_WAYPOINT_QUERY
ADD CONSTRAINT ENTITYWAYPOINT_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.GRIDDED_QUERY
ADD CONSTRAINT GRIDDED_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.GRIDDED_QUERY
ADD CONSTRAINT GRIDDED_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.GRIDDED_QUERY
ADD CONSTRAINT GRIDDED_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.I18N_LABEL
ADD CONSTRAINT LANGUAGES_CA_UUID_FK
FOREIGN KEY (LANGUAGE_UUID)
REFERENCES smart.LANGUAGE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INFORMANT
ADD CONSTRAINT INFORMANT_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTEL_RECORD_QUERY
ADD CONSTRAINT INTEL_RECORD_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTEL_RECORD_QUERY
ADD CONSTRAINT INTEL_RECORD_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTEL_RECORD_QUERY
ADD CONSTRAINT INTEL_RECORD_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTEL_SUMMARY_QUERY
ADD CONSTRAINT INTEL_SUMMARY_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTEL_SUMMARY_QUERY
ADD CONSTRAINT INTEL_SUMMARY_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTEL_SUMMARY_QUERY
ADD CONSTRAINT INTEL_SUMMARY_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE
ADD CONSTRAINT INTELLIGENCE_INFORMANT_UUID_FK
FOREIGN KEY (INFORMANT_UUID)
REFERENCES smart.INFORMANT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE
ADD CONSTRAINT INTELLIGENCE_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE
ADD CONSTRAINT INTELLIGENCE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE
ADD CONSTRAINT INTELLIGENCE_PATROL_UUID_FK
FOREIGN KEY (PATROL_UUID)
REFERENCES smart.PATROL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE
ADD CONSTRAINT INTELLIGENCE_SOURCE_UUID_FK
FOREIGN KEY (SOURCE_UUID)
REFERENCES smart.INTELLIGENCE_SOURCE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE_ATTACHMENT
ADD CONSTRAINT INTELLIGENCE_ATTACHMENT_INTELLIGENCE_UUID_FK
FOREIGN KEY (INTELLIGENCE_UUID)
REFERENCES smart.INTELLIGENCE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE_POINT
ADD CONSTRAINT INTELLIGENCE_POINT_INTELLIGENCE_UUID_FK
FOREIGN KEY (INTELLIGENCE_UUID)
REFERENCES smart.INTELLIGENCE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.INTELLIGENCE_SOURCE
ADD CONSTRAINT INTELLIGENCE_SOURCE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.LANGUAGE
ADD CONSTRAINT LANGUAGE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MAP_STYLES
ADD CONSTRAINT MAPSTYLE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION
ADD CONSTRAINT MISSION_SURVEY_UUID
FOREIGN KEY (SURVEY_UUID)
REFERENCES smart.SURVEY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_ATTRIBUTE
ADD CONSTRAINT MISSION_ATT_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_ATTRIBUTE_LIST
ADD CONSTRAINT MISSION_ATT_LIST_MISSION_ATT_UUID_FK
FOREIGN KEY (MISSION_ATTRIBUTE_UUID)
REFERENCES smart.MISSION_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_DAY
ADD CONSTRAINT MISSION_DAY_MISSION_UUID_FK
FOREIGN KEY (MISSION_UUID)
REFERENCES smart.MISSION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_MEMBER
ADD CONSTRAINT MISSION_MEMBER_MISSION_UUID_FK
FOREIGN KEY (MISSION_UUID)
REFERENCES smart.MISSION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_PROPERTY
ADD CONSTRAINT MISSION_PROP_SURVEY_DSG_UUID
FOREIGN KEY (SURVEY_DESIGN_UUID)
REFERENCES smart.SURVEY_DESIGN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_PROPERTY
ADD CONSTRAINT MISSION_PROP_MISSION_ATT_UUID_FK
FOREIGN KEY (MISSION_ATTRIBUTE_UUID)
REFERENCES smart.MISSION_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_PROPERTY_VALUE
ADD CONSTRAINT MISSION_PROP_VALUE_MISSION_ATT_UUID
FOREIGN KEY (MISSION_ATTRIBUTE_UUID)
REFERENCES smart.MISSION_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_PROPERTY_VALUE
ADD CONSTRAINT MISSION_PROP_VALUE_LISTELEMENT_UUID
FOREIGN KEY (LIST_ELEMENT_UUID)
REFERENCES smart.MISSION_ATTRIBUTE_LIST(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_PROPERTY_VALUE
ADD CONSTRAINT MISSION_PROP_VALUE_MISSION_UUID
FOREIGN KEY (MISSION_UUID)
REFERENCES smart.MISSION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_TRACK
ADD CONSTRAINT MISSION_TRACK_MISSIONDAY_UUID
FOREIGN KEY (MISSION_DAY_UUID)
REFERENCES smart.MISSION_DAY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.MISSION_TRACK
ADD CONSTRAINT MISSION_TRACK
FOREIGN KEY (SAMPLING_UNIT_UUID)
REFERENCES smart.SAMPLING_UNIT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_GRIDDED_QUERY
ADD CONSTRAINT OBS_GRIDDED_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_GRIDDED_QUERY
ADD CONSTRAINT OBS_GRIDDED_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_GRIDDED_QUERY
ADD CONSTRAINT OBS_GRIDDED_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_OBSERVATION_QUERY
ADD CONSTRAINT OBSOBSERVATION_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_OBSERVATION_QUERY
ADD CONSTRAINT OBS_OBSERVATION_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_OBSERVATION_QUERY
ADD CONSTRAINT OBS_OBSERVATION_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_SUMMARY_QUERY
ADD CONSTRAINT OBS_SUMMARY_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_SUMMARY_QUERY
ADD CONSTRAINT OBS_SUMMARY_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_SUMMARY_QUERY
ADD CONSTRAINT OBS_SUMMARY_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_WAYPOINT_QUERY
ADD CONSTRAINT OBS_WAYPOINT_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_WAYPOINT_QUERY
ADD CONSTRAINT OBSWAYPOINT_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBS_WAYPOINT_QUERY
ADD CONSTRAINT OBS_WAYPOINT_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBSERVATION_ATTACHMENT
ADD CONSTRAINT OBSERVATION_ATTACHMENT_OBS_UUID_FK
FOREIGN KEY (OBS_UUID)
REFERENCES smart.WP_OBSERVATION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBSERVATION_OPTIONS
ADD CONSTRAINT PATROL_OPTIONS_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBSERVATION_QUERY
ADD CONSTRAINT OBSERVATION_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBSERVATION_QUERY
ADD CONSTRAINT OBSERVATION_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.OBSERVATION_QUERY
ADD CONSTRAINT OBSERVATION_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL
ADD CONSTRAINT PATROL_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL
ADD CONSTRAINT PATROL_MANDATE_UUID_FK
FOREIGN KEY (MANDATE_UUID)
REFERENCES smart.PATROL_MANDATE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL
ADD CONSTRAINT PATROL_TEAM_UUID_FK
FOREIGN KEY (TEAM_UUID)
REFERENCES smart.TEAM(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL
ADD CONSTRAINT PATROL_STATION_UUID_FK
FOREIGN KEY (STATION_UUID)
REFERENCES smart.STATION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_INTELLIGENCE
ADD CONSTRAINT PATROL_INTELLIGENCE_INTELLIGENCE_UUID_FK
FOREIGN KEY (INTELLIGENCE_UUID)
REFERENCES smart.INTELLIGENCE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_INTELLIGENCE
ADD CONSTRAINT PATROL_INTELLIGENCE_PATROL_UUID_FK
FOREIGN KEY (PATROL_UUID)
REFERENCES smart.PATROL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_LEG
ADD CONSTRAINT PATROL_LEG_PATROL_UUID_FK
FOREIGN KEY (PATROL_UUID)
REFERENCES smart.PATROL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_LEG
ADD CONSTRAINT PATROL_LEG_TRANSPORT_UUID_FK
FOREIGN KEY (TRANSPORT_UUID)
REFERENCES smart.PATROL_TRANSPORT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_LEG_DAY
ADD CONSTRAINT PATROL_LEG_DAY_LEG_UUID_FK
FOREIGN KEY (PATROL_LEG_UUID)
REFERENCES smart.PATROL_LEG(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_LEG_MEMBERS
ADD CONSTRAINT LEG_MEMBERS_EMPLOYEE_UUID_FK
FOREIGN KEY (EMPLOYEE_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_LEG_MEMBERS
ADD CONSTRAINT LEG_MEMBERS_PATROL_LEG_UUID_FK
FOREIGN KEY (PATROL_LEG_UUID)
REFERENCES smart.PATROL_LEG(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_MANDATE
ADD CONSTRAINT PATROL_MANDATE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_PLAN
ADD CONSTRAINT PATROL_PLAN_PATROL_UUID_FK
FOREIGN KEY (PATROL_UUID)
REFERENCES smart.PATROL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_PLAN
ADD CONSTRAINT PATROL_PLAN_PLAN_UUID_FK
FOREIGN KEY (PLAN_UUID)
REFERENCES smart.PLAN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_QUERY
ADD CONSTRAINT PATROL_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_QUERY
ADD CONSTRAINT PATROL_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_QUERY
ADD CONSTRAINT PATROL_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_TRANSPORT
ADD CONSTRAINT PATROL_TRANSPORT_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_TYPE
ADD CONSTRAINT PATROL_TYPE_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_WAYPOINT
ADD CONSTRAINT PATROL_WAYPOINT_WP_UUID_FK
FOREIGN KEY (WP_UUID)
REFERENCES smart.WAYPOINT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PATROL_WAYPOINT
ADD CONSTRAINT PATROL_WAYPOINT_LEG_DAY_UUID_FK
FOREIGN KEY (LEG_DAY_UUID)
REFERENCES smart.PATROL_LEG_DAY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PLAN
ADD CONSTRAINT PLAN_PARENT_UUID_FK
FOREIGN KEY (PARENT_UUID)
REFERENCES smart.PLAN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PLAN
ADD CONSTRAINT PLAN_TEAM_UUID_FK
FOREIGN KEY (TEAM_UUID)
REFERENCES smart.TEAM(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PLAN
ADD CONSTRAINT PLAN_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PLAN
ADD CONSTRAINT PLAN_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PLAN
ADD CONSTRAINT PLAN_STATION_UUID_FK
FOREIGN KEY (STATION_UUID)
REFERENCES smart.STATION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PLAN_TARGET
ADD CONSTRAINT TARGET_PLAN_UUID_FK
FOREIGN KEY (PLAN_UUID)
REFERENCES smart.PLAN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.PLAN_TARGET_POINT
ADD CONSTRAINT PLAN_TARGET_POINT_PLAN_TARGET_UUID_FK
FOREIGN KEY (PLAN_TARGET_UUID)
REFERENCES smart.PLAN_TARGET(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.QUERY_FOLDER
ADD CONSTRAINT QUERY_FOLDER_EMPLOYEE_UUID_FK
FOREIGN KEY (EMPLOYEE_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.QUERY_FOLDER
ADD CONSTRAINT QUERY_FOLDER_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.QUERY_FOLDER
ADD CONSTRAINT QUERY_FOLDER_PARENT_UUID_FK
FOREIGN KEY (PARENT_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.RANK
ADD CONSTRAINT RANK_AGENCY_UUID_FK
FOREIGN KEY (AGENCY_UUID)
REFERENCES smart.AGENCY(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.REPORT
ADD CONSTRAINT REPORT_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.REPORT
ADD CONSTRAINT REPORT_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.REPORT
ADD CONSTRAINT REPORT_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.REPORT_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.REPORT_FOLDER
ADD CONSTRAINT REPORT_FOLDER_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.REPORT_FOLDER
ADD CONSTRAINT REPORT_FOLDER_PARENT_UUID_FK
FOREIGN KEY (PARENT_UUID)
REFERENCES smart.REPORT_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.REPORT_FOLDER
ADD CONSTRAINT REPORT_EMPLOYEE_UUID_FK
FOREIGN KEY (EMPLOYEE_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.REPORT_QUERY
ADD CONSTRAINT REPORT_QUERY_REPORT_UUID_FK
FOREIGN KEY (REPORT_UUID)
REFERENCES smart.REPORT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SAMPLING_UNIT
ADD CONSTRAINT SAMPLING_UNIT_SURVEY_DSG_UUID
FOREIGN KEY (SURVEY_DESIGN_UUID)
REFERENCES smart.SURVEY_DESIGN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SAMPLING_UNIT_ATTRIBUTE
ADD CONSTRAINT SU_ATTRIBUTE_CA_UUID
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;


ALTER TABLE smart.SAMPLING_UNIT_ATTRIBUTE_LIST
ADD CONSTRAINT SU_ATT_LIST_MISSION_ATT_UUID_FK
FOREIGN KEY (SAMPLING_UNIT_ATTRIBUTE_UUID)
REFERENCES smart.SAMPLING_UNIT_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SAMPLING_UNIT_ATTRIBUTE_VALUE
ADD CONSTRAINT SU_SU_ATTRIBUTE_UUID
FOREIGN KEY (SU_ATTRIBUTE_UUID)
REFERENCES smart.SAMPLING_UNIT_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SAMPLING_UNIT_ATTRIBUTE_VALUE
ADD CONSTRAINT SU_SU_LIST_ELEMENT_UUID
FOREIGN KEY (LIST_ELEMENT_UUID)
REFERENCES smart.SAMPLING_UNIT_ATTRIBUTE_LIST(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SAMPLING_UNIT_ATTRIBUTE_VALUE
ADD CONSTRAINT SU_SU_UUID
FOREIGN KEY (SU_UUID)
REFERENCES smart.SAMPLING_UNIT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SAVED_MAPS
ADD CONSTRAINT SAVED_MAPS_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SCREEN_OPTION
ADD CONSTRAINT SCREEN_OPTION_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SCREEN_OPTION_UUID
ADD CONSTRAINT SCREEN_OPTION_UUID_OPTION_UUID_FK
FOREIGN KEY (OPTION_UUID)
REFERENCES smart.SCREEN_OPTION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.STATION
ADD CONSTRAINT STATION_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SUMMARY_QUERY
ADD CONSTRAINT SUMMARY_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SUMMARY_QUERY
ADD CONSTRAINT SUMMARY_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SUMMARY_QUERY
ADD CONSTRAINT SUMMARY_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY
ADD CONSTRAINT SURVEY_SURVEY_DSG_UUID
FOREIGN KEY (SURVEY_DESIGN_UUID)
REFERENCES smart.SURVEY_DESIGN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_DESIGN
ADD CONSTRAINT CONFIGURABLE_MODEL_UUID_FK
FOREIGN KEY (CONFIGURABLE_MODEL_UUID)
REFERENCES smart.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_DESIGN
ADD CONSTRAINT SD_CAL_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_DESIGN_PROPERTY
ADD CONSTRAINT SURVEY_DSG_PROP_SURVEY_DSG_UUID
FOREIGN KEY (SURVEY_DESIGN_UUID)
REFERENCES smart.SURVEY_DESIGN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_DESIGN_SAMPLING_UNIT
ADD CONSTRAINT SD_SU_SURVEY_DESIGN_UUID
FOREIGN KEY (SURVEY_DESIGN_UUID)
REFERENCES smart.SURVEY_DESIGN(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_DESIGN_SAMPLING_UNIT
ADD CONSTRAINT SD_SU_SU_ATTRIBUTE_UUID
FOREIGN KEY (SU_ATTRIBUTE_UUID)
REFERENCES smart.SAMPLING_UNIT_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_GRIDDED_QUERY
ADD CONSTRAINT SVY_GRIDDED_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_GRIDDED_QUERY
ADD CONSTRAINT SVY_GRIDDED_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_GRIDDED_QUERY
ADD CONSTRAINT SVY_GRIDDED_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_MISSION_QUERY
ADD CONSTRAINT SVY_MISSION_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_MISSION_QUERY
ADD CONSTRAINT SVY_MISSION_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_MISSION_QUERY
ADD CONSTRAINT SVY_MISSION_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_MISSION_TRACK_QUERY
ADD CONSTRAINT SVY_MISSION_TRACK_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_MISSION_TRACK_QUERY
ADD CONSTRAINT SVY_MISSION_TRACK_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_MISSION_TRACK_QUERY
ADD CONSTRAINT SVY_MISSION_TRACK_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_OBSERVATION_QUERY
ADD CONSTRAINT SVY_OBSERVATION_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_OBSERVATION_QUERY
ADD CONSTRAINT SVY_OBSERVATION_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_OBSERVATION_QUERY
ADD CONSTRAINT SVY_OBSERVATION_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_SUMMARY_QUERY
ADD CONSTRAINT SVY_SUMMARY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_SUMMARY_QUERY
ADD CONSTRAINT SVY_SUMMARY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_SUMMARY_QUERY
ADD CONSTRAINT SVY_SUMMARY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_WAYPOINT
ADD CONSTRAINT SURVEY_WP_MISSION_TRK_UUID
FOREIGN KEY (MISSION_TRACK_UUID)
REFERENCES smart.MISSION_TRACK(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_WAYPOINT
ADD CONSTRAINT SURVEY_WP_SAMPLING_UNIT_UUID
FOREIGN KEY (SAMPLING_UNIT_UUID)
REFERENCES smart.SAMPLING_UNIT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_WAYPOINT
ADD CONSTRAINT SURVEY_WP_MISSIONDAY_UUID
FOREIGN KEY (MISSION_DAY_UUID)
REFERENCES smart.MISSION_DAY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_WAYPOINT_QUERY
ADD CONSTRAINT SVY_WAYPOINT_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_WAYPOINT_QUERY
ADD CONSTRAINT SVY_WAYPOINT_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.SURVEY_WAYPOINT_QUERY
ADD CONSTRAINT SVY_WAYPOINT_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.TEAM
ADD CONSTRAINT TEAM_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.TEAM
ADD CONSTRAINT TEAM_PATROL_MANDATE_UUID_FK
FOREIGN KEY (PATROL_MANDATE_UUID)
REFERENCES smart.PATROL_MANDATE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.TRACK
ADD CONSTRAINT TRACK_LEG_DAY_UUID_FK
FOREIGN KEY (PATROL_LEG_DAY_UUID)
REFERENCES smart.PATROL_LEG_DAY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WAYPOINT
ADD CONSTRAINT WAYPOINT_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WAYPOINT_QUERY
ADD CONSTRAINT WAYPOINT_QUERY_CA_UUID_FK
FOREIGN KEY (CA_UUID)
REFERENCES smart.CONSERVATION_AREA(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WAYPOINT_QUERY
ADD CONSTRAINT WAYPOINT_QUERY_FOLDER_UUID_FK
FOREIGN KEY (FOLDER_UUID)
REFERENCES smart.QUERY_FOLDER(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WAYPOINT_QUERY
ADD CONSTRAINT WAYPOINT_QUERY_CREATOR_UUID_FK
FOREIGN KEY (CREATOR_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_ATTACHMENTS
ADD CONSTRAINT WP_ATTACHMENTS_WP_UUID_FK
FOREIGN KEY (WP_UUID)
REFERENCES smart.WAYPOINT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_OBSERVATION
ADD CONSTRAINT OBSERVATION_WP_UUID_FK
FOREIGN KEY (WP_UUID)
REFERENCES smart.WAYPOINT(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_OBSERVATION
ADD CONSTRAINT OBS_EMPLOYEE_UUID_FK
FOREIGN KEY (EMPLOYEE_UUID)
REFERENCES smart.EMPLOYEE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_OBSERVATION
ADD CONSTRAINT OBSERVATION_CATEGORY_UUID_FK
FOREIGN KEY (CATEGORY_UUID)
REFERENCES smart.DM_CATEGORY(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_OBSERVATION_ATTRIBUTES
ADD CONSTRAINT OBSERVATION_ATTRIBUTE_ATT_TREE_UUID_FK
FOREIGN KEY (TREE_NODE_UUID)
REFERENCES smart.DM_ATTRIBUTE_TREE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_OBSERVATION_ATTRIBUTES
ADD CONSTRAINT OBSERVATION_ATTRIBUTE_ATT_LIST_UUID_FK
FOREIGN KEY (LIST_ELEMENT_UUID)
REFERENCES smart.DM_ATTRIBUTE_LIST(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_OBSERVATION_ATTRIBUTES
ADD CONSTRAINT OBS_ATTRIBUTE_OBS_UUID_FK
FOREIGN KEY (OBSERVATION_UUID)
REFERENCES smart.WP_OBSERVATION(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.WP_OBSERVATION_ATTRIBUTES
ADD CONSTRAINT OBSERVATION_ATTRIBUTE_ATT_UUID_FK
FOREIGN KEY (ATTRIBUTE_UUID)
REFERENCES smart.DM_ATTRIBUTE(UUID) ON DELETE CASCADE DEFERRABLE;

ALTER TABLE smart.connect_ct_properties 
ADD CONSTRAINT connect_ct_properties_cm_uuid_fk 
FOREIGN KEY (CM_UUID) 
REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE DEFERRABLE;

-- Unique Constraints
alter table smart.PATROL_MANDATE add constraint patrol_mandate_keyid_unq UNIQUE(ca_uuid, keyid) DEFERRABLE ;
alter table smart.PATROL_TRANSPORT add constraint patrol_transport_keyid_unq UNIQUE(ca_uuid, keyid) DEFERRABLE ;
alter table smart.TEAM add constraint team_keyid_unq UNIQUE(ca_uuid, keyid) DEFERRABLE ;
alter table smart.DM_ATTRIBUTE add constraint dm_attribute_keyid_unq UNIQUE(ca_uuid, keyid) DEFERRABLE ;
alter table smart.DM_ATTRIBUTE_LIST add constraint dm_attribute_list_keyid_unq UNIQUE(attribute_uuid, keyid) DEFERRABLE ;
alter table smart.DM_CATEGORY add constraint dm_category_keyid_unq UNIQUE(ca_uuid, hkey) DEFERRABLE ;
alter table smart.DM_ATTRIBUTE_TREE add constraint dm_attribute_tree_keyid_unq UNIQUE(attribute_uuid, hkey) DEFERRABLE ;
alter table smart.INTELLIGENCE_SOURCE add constraint intell_source_keyid_unq unique(ca_uuid, keyid) DEFERRABLE ;
alter table smart.ENTITY_ATTRIBUTE add constraint entity_attribute_keyid_unq unique(entity_type_uuid, keyid) DEFERRABLE ;
alter table smart.ENTITY_TYPE add constraint entity_type_keyid_unq unique(ca_uuid, keyid) DEFERRABLE ;
alter table smart.MISSION_ATTRIBUTE add constraint mission_attribute_keyid_unq unique(ca_uuid, keyid) DEFERRABLE ;
alter table smart.MISSION_ATTRIBUTE_LIST add constraint mission_attribute_list_keyid_unq unique(mission_attribute_uuid, keyid) DEFERRABLE ;
alter table smart.SAMPLING_UNIT_ATTRIBUTE add constraint su_attribute_keyid_unq unique(ca_uuid, keyid) DEFERRABLE ;
alter table smart.SAMPLING_UNIT_ATTRIBUTE_LIST add constraint su_list_attribute_keyid_unq unique(sampling_unit_attribute_uuid, keyid) DEFERRABLE ;
alter table smart.SURVEY_DESIGN add constraint survey_design_keyid_unq unique(ca_uuid, keyid) DEFERRABLE ;


insert into smart.dm_aggregation(name) values ('sum');
insert into smart.dm_aggregation(name) values ('avg');
insert into smart.dm_aggregation(name) values ('min');
insert into smart.dm_aggregation(name) values ('max');
insert into smart.dm_aggregation(name) values ('stddev_samp');
insert into smart.dm_aggregation(name) values ('var_samp');

insert into smart.dm_aggregation_i18n values ('stddev_samp', 'en', 'standard deviation (samp.)');
insert into smart.dm_aggregation_i18n values ('var_samp', 'en', 'variance (samp.)');
insert into smart.dm_aggregation_i18n values ('stddev_samp', 'es', 'DesviaciÃ³n estÃ¡ndar');
insert into smart.dm_aggregation_i18n values ('var_samp', 'es', 'Varianza');
insert into smart.dm_aggregation_i18n values ('stddev_samp', 'fr', 'Ecart type');
insert into smart.dm_aggregation_i18n values ('var_samp', 'fr', 'Variance');
insert into smart.dm_aggregation_i18n values ('stddev_samp', 'vi', 'Ä�á»™ lá»‡ch chuáº©n');
insert into smart.dm_aggregation_i18n values ('var_samp', 'vi', 'PhÆ°Æ¡ng sai');
insert into smart.dm_aggregation_i18n values ('stddev_samp', 'th', 'à¸ªà¹ˆà¸§à¸™à¹€à¸šà¸µà¹ˆà¸¢à¸‡à¹€à¸šà¸™à¸¡à¸²à¸•à¸£à¸�à¸²à¸™');
insert into smart.dm_aggregation_i18n values ('var_samp', 'th', 'à¸„à¹ˆà¸²à¸„à¸§à¸²à¸¡à¹�à¸›à¸£à¸›à¸£à¸§à¸™');
insert into smart.dm_aggregation_i18n values ('stddev_samp', 'zh', 'æ ‡å‡†å·®');
insert into smart.dm_aggregation_i18n values ('var_samp', 'zh', 'æ–¹å·®');
insert into smart.dm_aggregation_i18n values ('stddev_samp', 'in', 'Standar Deviasi');
insert into smart.dm_aggregation_i18n values ('var_samp', 'in', 'Varians');		

			
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('sum','en','sum');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('min','en','minimum');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('max','en','maximum');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('avg','en','average');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('sum','fr','total');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('min','fr','minimum');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('max','fr','maximum');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('avg','fr','moyenne');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('sum','es','total');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('min','es','mÃ­nimo');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('max','es','mÃ¡ximo');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('avg','es','promedio');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('sum','in','jumlah');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('min','in','minimum');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('max','in','maksimum');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('avg','in','rata-rata');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('sum','th','à¸œà¸¥à¸£à¸§à¸¡');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('min','th','à¸„à¹ˆà¸²à¸•à¹ˆà¸³à¸ªà¸¸à¸”');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('max','th','à¸„à¹ˆà¸²à¸ªà¸¹à¸‡à¸ªà¸¸à¸”');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('avg','th','à¸„à¹ˆà¸²à¹€à¸‰à¸¥à¸µà¹ˆà¸¢');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('sum','zh','å�ˆè®¡');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('min','zh','æœ€å°�');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('max','zh','æœ€å¤§');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('avg','zh','å¹³å�‡');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('sum','ru','Ð¸Ñ‚Ð¾Ð³Ð°');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('min','ru','Ð¼Ð¸Ð½Ð¸Ð¼Ð°Ð»ÑŒÐ½Ñ‹Ð¹');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('max','ru','Ð¼Ð°ÐºÑ�Ð¸Ð¼Ð°Ð»ÑŒÐ½Ñ‹Ð¹');
insert into smart.DM_AGGREGATION_i18n (name, lang_code, gui_name) values ('avg','ru','Ñ�Ñ€ÐµÐ´Ð½Ð¸Ð¹');


create table smart.connect_server(
uuid UUID not null,
ca_uuid UUID,
url varchar(2064),
certificate varchar(32000),
PRIMARY KEY (uuid));

alter table smart.connect_server add constraint server_ca_uuid_fk foreign key (ca_uuid) 
references smart.conservation_area (uuid) on update restrict on delete cascade DEFERRABLE;

CREATE TABLE smart.connect_server_option(
server_uuid UUID not null, 
option_key varchar(32), 
value varchar(2048), 
primary key (server_uuid, option_key));

ALTER TABLE smart.connect_server_option ADD CONSTRAINT cnt_svr_opt_server_fk FOREIGN KEY (server_uuid) 
REFERENCES smart.connect_server (uuid)   ON UPDATE restrict ON DELETE cascade DEFERRABLE ;


create table smart.connect_account(
employee_uuid UUID not null,
connect_uuid UUID not null,
connect_user varchar(32),
connect_pass varchar(1024),
primary key(employee_uuid, connect_uuid));

alter table smart.connect_account add constraint connect_employee_uuid_fk foreign key (employee_uuid) 
references smart.employee (uuid) on update restrict on delete cascade DEFERRABLE;

-- DATA PROCESSING QUEUE TABLES
CREATE TABLE smart.connect_data_queue(
	uuid UUID NOT NULL,
	type VARCHAR(32) NOT NULL,
	ca_uuid UUID,
	name VARCHAR(4096),
	status varchar(32) NOT NULL,
	queue_order integer,
	error_message VARCHAR(8192),
	local_file varchar(4096),
	date_processed timestamp,
	server_item_uuid UUID,
	PRIMARY KEY (uuid)
);
		
ALTER TABLE smart.connect_data_queue ADD CONSTRAINT 
connect_data_queue_ca_uuid_fk foreign key (ca_uuid) 
REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE;

ALTER TABLE smart.connect_data_queue ADD CONSTRAINT status_chk 
CHECK (status IN ('DOWNLOADING', 'REQUEUED', 'QUEUED', 'PROCESSING', 'COMPLETE', 'COMPLETE_WARN', 'ERROR'));
		
ALTER TABLE smart.connect_data_queue ADD CONSTRAINT type_chk 
CHECK (type IN ('PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML'));


CREATE TABLE smart.connect_data_queue_option(
	ca_uuid UUID not null, 
	keyid varchar(256) NOT NULL, 
	value varchar(512), 
	primary key (ca_uuid, keyid)
);
ALTER TABLE smart.connect_data_queue_option 
ADD CONSTRAINT data_queue_option_ca_uuid_fk foreign key (ca_uuid) 
REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE;