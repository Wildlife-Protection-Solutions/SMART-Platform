
/* Drop Tables */

DROP TABLE paws_parameter;
DROP TABLE paws_query_class;
DROP TABLE paws_run;
DROP TABLE paws_simple_classification;
DROP TABLE paws_configuration;
DROP TABLE paws_service;
DROP TABLE paws_workspace;
DROP TABLE conservation_area;




/* Create Tables */

CREATE TABLE conservation_area
(
	uuid char(16) NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE paws_configuration
(
	uuid char(16) NOT NULL,
	ca_uuid char(16) NOT NULL,
	name varchar(8192) NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE paws_parameter
(
	uuid char(16) NOT NULL,
	config_uuid char(16) NOT NULL,
	key varchar(8192) NOT NULL,
	value varchar(8192),
	PRIMARY KEY (uuid)
);


CREATE TABLE paws_query_class
(
	uuid char(16) NOT NULL,
	config_uuid char(16) NOT NULL,
	query_uuid char(16) NOT NULL,
	query_type varchar(32) NOT NULL,
	date_range varchar(512),
	classification varchar(512) NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE paws_run
(
	uuid char(16) NOT NULL,
	ca_uuid char(16) NOT NULL,
	config_uuid char(16) NOT NULL,
	id varchar(256) NOT NULL,
	server_run_id varchar(256),
	package_file varchar(256),
	result_location varchar(256),
	status varchar(32) NOT NULL,
	PRIMARY KEY (uuid)
);


CREATE TABLE paws_service
(
	uuid char(16) NOT NULL,
	ca_uuid char(16) NOT NULL UNIQUE,
	url varchar(8192),
	api_key varchar(8192),
	PRIMARY KEY (uuid)
);


CREATE TABLE paws_simple_classification
(
	uuid char(16) NOT NULL,
	config_uuid char(16) NOT NULL,
	classification varchar(512) NOT NULL,
	date_range varchar(512),
	category_uuid char(16) NOT NULL,
	attribute_uuid char(16),
	list_uuid char(16),
	tree_uuid char(16),
	PRIMARY KEY (uuid)
);


CREATE TABLE paws_workspace
(
	uuid char(16) NOT NULL,
	ca_uuid char(16) NOT NULL UNIQUE,
	url varchar(8192),
	api_key varchar(8192),
	PRIMARY KEY (uuid)
);



/* Create Foreign Keys */

ALTER TABLE paws_configuration
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE paws_run
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE paws_service
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE paws_workspace
	ADD FOREIGN KEY (ca_uuid)
	REFERENCES conservation_area (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE paws_parameter
	ADD FOREIGN KEY (config_uuid)
	REFERENCES paws_configuration (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE paws_query_class
	ADD FOREIGN KEY (config_uuid)
	REFERENCES paws_configuration (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE paws_run
	ADD FOREIGN KEY (config_uuid)
	REFERENCES paws_configuration (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE paws_simple_classification
	ADD FOREIGN KEY (config_uuid)
	REFERENCES paws_configuration (uuid)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;



