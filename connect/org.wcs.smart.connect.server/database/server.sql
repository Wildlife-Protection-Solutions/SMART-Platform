
/* Drop Tables */
DROP SCHEMA If exists connect CASCADE;
CREATE SCHEMA connect;

DROP TABLE IF EXISTS connect.ca_plug_version;
DROP TABLE IF EXISTS connect.conservation_area_info;
DROP TABLE IF EXISTS connect.ca_info;
DROP TABLE IF EXISTS connect.alerts;
DROP TABLE IF EXISTS connect.alert_types;
DROP TABLE IF EXISTS connect.style_configuration;
DROP TABLE IF EXISTS connect.alert_filter_defaults;
DROP TABLE IF EXISTS connect.plugin_version;
DROP TABLE IF EXISTS connect.connect_plugin_version;
DROP TABLE IF EXISTS connect.user_actions;
DROP TABLE IF EXISTS connect.users;
DROP TABLE IF EXISTS connect.user_roles;
DROP TABLE IF EXISTS connect.role;
DROP TABLE IF EXISTS connect.role_actions;
DROP TABLE IF EXISTS connect.upload_status;
DROP TABLE IF EXISTS connect.upload_item;
DROP TABLE IF EXISTS connect.work_item;
DROP TABLE IF EXISTS connect.map_layers;

DROP TABLE IF EXISTS connect.change_log;
DROP TABLE IF EXISTS connect.data_queue;

/* Create Tables */
CREATE TABLE connect.work_item
(
	uuid uuid NOT NULL,
	ca_uuid uuid NOT NULL,
	start_datetime timestamp not null,
	total_bytes bigint not null,
	local_filename varchar not null,
	type varchar(16) not null,
	status varchar(16) not null,
	locale varchar(56) not null,
	message varchar,
	PRIMARY KEY (uuid)
) WITHOUT OIDS;

ALTER TABLE connect.work_item ADD CONSTRAINT status_chk 
CHECK (status IN ('UPLOADING', 'PROCESSING', 'COMPLETE', 'ERROR'));

ALTER TABLE connect.work_item ADD CONSTRAINT type_chk 
CHECK (type IN ('UP_CA', 'UP_SYNC', 'DOWN_CA', 'DOWN_SYNC', 'UP_DATAQUEUE'));

	
CREATE TABLE connect.ca_plugin_version
(
	ca_uuid uuid NOT NULL,
	plugin_id varchar NOT NULL,
	version varchar NOT NULL,
	PRIMARY KEY (ca_uuid, plugin_id)
) WITHOUT OIDS;


CREATE TABLE connect.ca_info
(
	ca_uuid uuid NOT NULL,
	version uuid,
	label varchar not null,
	status varchar not null,
	lock_key serial not null,
	unique(lock_key),
	PRIMARY KEY (ca_uuid)
) WITHOUT OIDS;

ALTER TABLE connect.ca_info ADD CONSTRAINT status_chk 
CHECK (status IN ('UPLOADING', 'DATA', 'NODATA', 'CCAA'));

CREATE TABLE connect.connect_plugin_version
(
	plugin_id varchar NOT NULL,
	version varchar NOT NULL,
	PRIMARY KEY (plugin_id)
) WITHOUT OIDS;


CREATE TABLE connect.users
(
	uuid uuid NOT NULL,
	username varchar(32) NOT NULL,
	password char(60) NOT NULL,
	email varchar,
	resetid varchar,
	resetdatetime timestamp,
	UNIQUE(username),
	UNIQUE (uuid),
	UNIQUE(resetid),
	PRIMARY KEY (uuid, username)
) WITHOUT OIDS;


CREATE TABLE connect.user_actions
(
	username varchar NOT NULL,
	action varchar NOT NULL,
	resource uuid,
	uuid uuid NOT NULL,
	
	PRIMARY KEY (username, action, uuid)
) WITHOUT OIDS;

CREATE UNIQUE INDEX useractions_unq1 ON connect.user_actions(username, action) WHERE resource IS NULL;
CREATE UNIQUE INDEX useractions_unq2 ON connect.user_actions(username, action, resource) WHERE resource IS NOT NULL;


CREATE TABLE connect.user_roles
(
	username varchar NOT NULL,
	role_id varchar(32) NOT NULL,
	PRIMARY KEY (username, role_id)
) WITHOUT OIDS;

CREATE TABLE connect.roles
(
	role_id varchar(32) NOT NULL,
	rolename varchar NOT NULL,
	is_system boolean not null default false,
	PRIMARY KEY (role_id)
) WITHOUT OIDS;

CREATE TABLE connect.role_actions
(
	uuid uuid NOT NULL,	
	role_id varchar(32) NOT NULL,
	action varchar NOT NULL,
	resource uuid,
	PRIMARY KEY (uuid)
) WITHOUT OIDS;
CREATE UNIQUE INDEX roleactions_unq1 ON connect.role_actions(role_id, action) WHERE resource IS NULL;
CREATE UNIQUE INDEX roleactions_unq2 ON connect.role_actions(role_id, action, resource) WHERE resource IS NOT NULL;


CREATE TYPE connect.alert_status AS ENUM ('ACTIVE', 'DISABLED');

-- A list of all alerts in the system
CREATE TABLE connect.alerts
(
	-- A unqiue identifier for hibernate.
	uuid uuid NOT NULL,
	-- A unqiue identifier that the user generates.
	user_generated_id varchar NOT NULL,
	-- The date/time the alert was created.
	date timestamp with time zone NOT NULL, 
	-- Description associated with alert.
	description varchar,
	-- A link to the alert type.
	type_uuid uuid NOT NULL,
	-- A value of 1 (high) - 5(low).
	level smallint NOT NULL,
	-- Associated Conservation Area UUID
	ca_uuid uuid NOT NULL,
	--alert status, custom enum type defined above
	status connect.alert_status NOT NULL,
	-- the longitude of the alert location
	x double precision NOT NULL,
	-- the latitude of the alert location
	y double precision NOT NULL,
	-- The past points for this alert, as a line in json format:[[1, 1], [2, 2]]
	track varchar,
	-- A link to the user who created the alert.  The user will always be able to modify the alert.
	creator_uuid uuid NOT NULL,
	PRIMARY KEY (uuid)
) WITHOUT OIDS;

CREATE TABLE connect.alert_types(
	-- A unqiue identifier for hibernate.
	uuid uuid NOT NULL,
	-- Label for the type.
	label varchar(64),
	color varchar(16),
	fillColor varchar(16),
	opacity varchar(8),
	--http://fortawesome.github.io/Font-Awesome/icons/   shows valid values
	markerIcon varchar(16),
	--possible values for color are 'red', 'darkred', 'orange', 'green', 'darkgreen', 'blue', 'purple', 'darkpuple', 'cadetblue'
	markerColor varchar(16),
	spin boolean not null,
	PRIMARY KEY (uuid)
) WITHOUT OIDS;

insert into connect.alert_types values( '00000000-0000-0000-0000-000000000000' ,'Unknown Type','#000000','#000000','1', '', 'red', 'false');


CREATE TABLE connect.style_configuration(
	uuid uuid NOT NULL,
	style_id varchar(64) NOT NULL,
	active boolean NOT NULL,
	header_image bytea,
	header_style varchar(256),
	background_image bytea,
	body_style varchar(256),
	login_image bytea,
	server_name varchar(256),
	footer_text text,
	PRIMARY KEY(style_id)
) WITHOUT OIDS;

CREATE TABLE connect.map_layers(
	uuid uuid NOT NULL,
	-- layer type  1-mapbox.com layer, 2-giscloud.com (WMS-published), 3 - generic WMS
	layer_type int NOT NULL,
	active boolean NOT NULL,
	token varchar(256),
	mapboxid varchar(64),
	wms_layer_list text,
	layer_name varchar(32),
	layer_order int NOT NULL
) WITHOUT OIDS;

CREATE TABLE connect.alert_filter_defaults(
	uuid uuid NOT NULL,
	default_past_hours int,
	default_type_uuids varchar(925), --max 25 types to default to on, comma separated.
	default_active boolean,
	default_disabled boolean,
	default_level1 boolean,
	default_level2 boolean,
	default_level3 boolean,
	default_level4 boolean,
	default_level5 boolean,
	default_ca_uuids varchar(925),--max 25 uuids, comma separated.
	default_text varchar(128),
	seconds_refresh int,
	starting_zoom_level int,
	starting_long real,
	starting_lat real
)WITHOUT OIDS;


create table connect.connect_version 
(version varchar(16), 
last_updated timestamp default (now())
)WITHOUT OIDS;

--insert into connect.alert_filter_defaults values( 'a1bcbc77-9c0b-4ef8-bb6d-6bb9bd380a53' , 24, '',true, true, true, true, true, true, true,'','', 30, 8 , -7.5, 34);



/* Create Foreign Keys */
ALTER TABLE connect.work_item
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES connect.ca_info (ca_uuid)
	ON UPDATE RESTRICT
	ON DELETE CASCADE;

ALTER TABLE connect.ca_plugin_version
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES connect.ca_info (ca_uuid)
	ON UPDATE RESTRICT
	ON DELETE CASCADE;


ALTER TABLE connect.user_actions
	ADD FOREIGN KEY (username)
	REFERENCES connect.users (username)
	ON UPDATE CASCADE
	ON DELETE CASCADE;

ALTER TABLE connect.user_roles
	ADD FOREIGN KEY (username)
	REFERENCES connect.users (username)
	ON UPDATE CASCADE
	ON DELETE CASCADE;

ALTER TABLE connect.user_roles
	ADD FOREIGN KEY (role_id)
	REFERENCES connect.roles (role_id)
	ON UPDATE CASCADE
	ON DELETE CASCADE;
	
ALTER TABLE connect.role_actions
	ADD FOREIGN KEY (role_id)
	REFERENCES connect.roles (role_id)
	ON UPDATE CASCADE
	ON DELETE CASCADE;
	
	
ALTER TABLE connect.alerts
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES connect.ca_info (ca_uuid)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE connect.alerts
	ADD FOREIGN KEY (creator_uuid)
	REFERENCES connect.users (uuid)
	ON UPDATE RESTRICT
;

ALTER TABLE connect.alerts
	ADD FOREIGN KEY (type_uuid)
	REFERENCES connect.alert_types (uuid)
	ON UPDATE RESTRICT
;

INSERT INTO connect.roles (role_id, rolename, is_system) values ('smart', 'SYSTEM ROLE', true);

	
CREATE OR REPLACE FUNCTION manage_user_roles() RETURNS TRIGGER AS $$
    BEGIN
        --
        -- should only be called on insert; adds necessary smart role
        -- for web access
        --
        IF (TG_OP = 'INSERT') THEN
            INSERT INTO connect.user_roles (username, role_id) VALUES (NEW.username, 'smart');
            RETURN NEW;
        END IF;
        RETURN NULL; -- result is ignored since this is an AFTER trigger
    END;
$$ LANGUAGE plpgsql;


--AFTER INSERT OR UPDATE OR DELETE ON connect.users
CREATE TRIGGER web_roles_mgr
AFTER INSERT ON connect.users
    FOR EACH ROW EXECUTE PROCEDURE manage_user_roles();


/* Comments */
COMMENT ON TABLE connect.work_item IS 'A table for tracking uploads and supporting upload apis.';
COMMENT ON COLUMN connect.work_item.uuid IS 'A unique system generated identifier.';
COMMENT ON COLUMN connect.work_item.ca_uuid IS 'The unique Conservation Area identifier.';
COMMENT ON COLUMN connect.work_item.start_datetime IS 'The start time of the upload.';
COMMENT ON COLUMN connect.work_item.total_bytes IS 'Total number of bytes to upload.';
COMMENT ON COLUMN connect.work_item.local_filename IS 'Name of the file in the local filestore.';
COMMENT ON COLUMN connect.work_item.type IS 'File type.';
COMMENT ON COLUMN connect.work_item.status IS 'Status of upload and processing';
COMMENT ON COLUMN connect.work_item.message IS 'Error message or other info message asociated with upload.';
COMMENT ON TABLE connect.ca_info IS 'Contains server details for Conservation Areas.';
COMMENT ON COLUMN connect.ca_info.ca_uuid IS 'The unique Conservation Area identifier.';
COMMENT ON COLUMN connect.ca_info.version IS 'The version of the data for the conservation area.';
COMMENT ON TABLE connect.ca_plugin_version IS 'A list of SMART plugins and their database schema version for each Conservation Area.';
COMMENT ON COLUMN connect.ca_plugin_version.ca_uuid IS 'The unique Conservation Area identifier.';
COMMENT ON COLUMN connect.ca_plugin_version.plugin_id IS 'The unique plugin identifier.';
COMMENT ON COLUMN connect.ca_plugin_version.version IS 'The plugin database schema version.';
COMMENT ON TABLE connect.connect_plugin_version IS 'The list of plugin supported by the SMART Connect Server and their associated versions.  The version field should be the database schema version not the code version.';
COMMENT ON COLUMN connect.connect_plugin_version.plugin_id IS 'The unique plugin identifier.';
COMMENT ON COLUMN connect.connect_plugin_version.version IS 'The plugin database schema version.';
COMMENT ON TABLE connect.users IS 'A list of smart connect users.';
COMMENT ON COLUMN connect.users.uuid IS 'A unqiue identifier for hibernate.';
COMMENT ON COLUMN connect.users.username IS 'The unique username';
COMMENT ON COLUMN connect.users.password IS 'The bcrypt has encoded password for the user.';
COMMENT ON COLUMN connect.users.email IS 'The user email address';
COMMENT ON COLUMN connect.users.resetid IS 'A unique key sent to the users for resetting their password.';
COMMENT ON COLUMN connect.users.resetdatetime IS 'The date/time the last reset link was sent to the user.';
COMMENT ON TABLE connect.user_actions IS 'A table for listing user permissions and associated resources.';
COMMENT ON COLUMN connect.user_actions.username IS 'The unique user identifier.';
COMMENT ON COLUMN connect.user_actions.action IS 'The action the user has permission to perform.';
COMMENT ON COLUMN connect.user_actions.resource IS 'Unique identifier to the resource (null implies all resources)';
COMMENT ON COLUMN connect.user_actions.uuid IS 'A unqiue identifier for hibernate.';
COMMENT ON TABLE connect.user_roles IS 'A list of webserver roles supported by each user.';
COMMENT ON COLUMN connect.user_roles.username IS 'The unique username.';
COMMENT ON COLUMN connect.user_roles.role_id IS 'The webserver role.';

CREATE TABLE connect.change_log(
	uuid UUID,
	revision BIGSERIAL,
	action varchar(15) CONSTRAINT action_check CHECK (action in ('INSERT', 'UPDATE', 'DELETE', 'FS_INSERT', 'FS_DELETE', 'FS_UPDATE')),
	filename varchar(32672),
	tablename varchar(256),
	ca_uuid UUID,
	key1_fieldname varchar(256),
	key1 UUID,
	key2_fieldname varchar(256),
	key2_str varchar(256),
	key2_uuid UUID,
	datetime timestamp default now(),
	primary key (revision)
);
ALTER TABLE connect.change_log ADD CONSTRAINT connect_changelog_ca_uuid_fk foreign key (ca_uuid) REFERENCES connect.ca_info(ca_uuid) ON UPDATE restrict ON DELETE cascade;
CREATE INDEX connect_change_log_uuid_idx on connect.change_log (uuid);
CREATE INDEX connect_change_log_ca_uuid_idx on connect.change_log (ca_uuid);

COMMENT ON TABLE connect.change_log IS 'Change log items.';
COMMENT ON COLUMN connect.change_log.uuid IS 'A unique identifier for each change log item.';
COMMENT ON COLUMN connect.change_log.revision IS 'The server defined revision number.';
COMMENT ON COLUMN connect.change_log.action IS 'Change log action.';
COMMENT ON COLUMN connect.change_log.filename IS 'The filename, if a datastore action.';
COMMENT ON COLUMN connect.change_log.tablename IS 'The tablename if a database action.';
COMMENT ON COLUMN connect.change_log.ca_uuid IS 'The conservation area uuid.';
COMMENT ON COLUMN connect.change_log.key1_fieldname IS 'The first unique key field name (required if database action).';
COMMENT ON COLUMN connect.change_log.key1 IS 'The first unique key uuid value (required if database action).';
COMMENT ON COLUMN connect.change_log.key2_fieldname IS 'The second unique key field name (optional, only required for composite primary keys).';
COMMENT ON COLUMN connect.change_log.key2_str IS 'The second unique key uuid value (optional).';
COMMENT ON COLUMN connect.change_log.key2_uuid IS 'The second unique key string value (optional)';
COMMENT ON COLUMN connect.change_log.datetime IS 'The server managed datetime the action is added to the table.';


CREATE TABLE connect.change_log_history(
	ca_uuid UUID,
	last_delete_revision BIGINT,
	primary key (ca_uuid)
);
ALTER TABLE connect.change_log_history ADD CONSTRAINT connect_changelog_history_ca_uuid_fk foreign key (ca_uuid) REFERENCES connect.ca_info(ca_uuid) ON UPDATE restrict ON DELETE cascade;
COMMENT ON TABLE connect.change_log_history IS 'Tracks history infor about the change log table, in particular the last removed records for each conservation area';
COMMENT ON COLUMN connect.change_log_history.ca_uuid IS 'The conservation area unique identifier.';
COMMENT ON COLUMN connect.change_log_history.last_delete_revision IS 'The last deleted revision number.';

-- DATA PROCESSING QUEUE TABLES
CREATE TABLE connect.data_queue(
	uuid UUID,
	type VARCHAR(32) NOT NULL,
	ca_uuid UUID NOT NULL,
	name VARCHAR,
	uploaded_date timestamp not null,
	lastmodified_date timestamp,
	uploaded_by varchar not null,
	file varchar,
	status varchar(32) NOT NULL,
	status_message varchar,
	work_item_uuid UUID,
	PRIMARY KEY (uuid)
);
ALTER TABLE connect.data_queue ADD CONSTRAINT 
data_queue_ca_uuid_fk foreign key (ca_uuid) 
REFERENCES connect.ca_info(ca_uuid) ON UPDATE restrict ON DELETE cascade;

ALTER TABLE connect.data_queue ADD CONSTRAINT status_chk 
CHECK (status IN ('UPLOADING', 'QUEUED', 'PROCESSING', 'COMPLETE', 'ERROR'));

ALTER TABLE connect.data_queue ADD CONSTRAINT type_chk 
CHECK (type IN ('PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML'));

CREATE OR REPLACE FUNCTION connect.dq_update_modified_column()	
RETURNS TRIGGER AS $$
BEGIN
    NEW.lastmodified_date = now();
    RETURN NEW;	
END;
$$ language 'plpgsql';
CREATE TRIGGER dq_last_modified_trigger BEFORE UPDATE ON connect.data_queue FOR EACH ROW EXECUTE PROCEDURE connect.dq_update_modified_column();

