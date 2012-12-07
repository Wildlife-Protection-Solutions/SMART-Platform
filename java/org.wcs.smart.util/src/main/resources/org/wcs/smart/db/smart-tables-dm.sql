

CREATE TABLE smart.dm_aggregation 
(
	name VARCHAR(10) NOT NULL,
	PRIMARY KEY (NAME)
);

CREATE TABLE smart.dm_aggregation_i18n(
	name VARCHAR(10) NOT NULL,
	lang_code VARCHAR(5) NOT NULL,
	gui_name VARCHAR(48) NOT NULL,
	PRIMARY KEY(name, lang_code)
);

/* Create Foreign Keys */

ALTER TABLE smart.dm_aggregation_i18n
	ADD CONSTRAINT dm_aggregation_i18n_fk FOREIGN KEY (name)
	REFERENCES smart.dm_aggregation (name)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;


INSERT INTO smart.dm_aggregation_i18n (NAME, GUI_NAME) VALUES ( 'sum');
INSERT INTO smart.dm_aggregation_i18n (NAME, GUI_NAME) VALUES ( 'min');
INSERT INTO smart.dm_aggregation_i18n (NAME, GUI_NAME) VALUES ( 'max');
INSERT INTO smart.dm_aggregation_i18n (NAME, GUI_NAME) VALUES ( 'avg');

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'en', 'Sum' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'en', 'Minimum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'en', 'Maximum');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'en', 'Average' );

INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'sum', 'fr', 'somme' );
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'min', 'fr', 'minimale');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'max', 'fr', 'maximale');
INSERT INTO smart.dm_aggregation_i18n (NAME, lang_code, GUI_NAME) VALUES ( 'avg', 'fr', 'moyenne' );

--INSERT INTO smart.dm_aggregation  (NAME, GUI_NAME) VALUES ( 'stdev', 'Standard Deviation' );

CREATE TABLE smart.dm_attribute
(
	uuid CHAR(16) FOR BIT DATA NOT NULL,
	ca_uuid CHAR(16) FOR BIT DATA NOT NULL,
	keyid VARCHAR(128) NOT NULL,
	is_required BOOLEAN NOT NULL,
	att_type VARCHAR(7) NOT NULL,
	min_value DOUBLE,
	max_value DOUBLE,
	regex VARCHAR(1024),
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.dm_attribute_list
(
	uuid CHAR(16) FOR BIT DATA NOT NULL,
	attribute_uuid CHAR(16)  FOR BIT DATA NOT NULL,
	keyid  VARCHAR(128) NOT NULL,
	list_order SMALLINT NOT NULL,
	is_active BOOLEAN NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.dm_attribute_tree
(
	uuid CHAR(16) FOR BIT DATA NOT NULL,
	keyid VARCHAR(128) NOT NULL,
	node_order SMALLINT NOT NULL,
	parent_uuid CHAR(16) FOR BIT DATA,
	attribute_uuid CHAR(16) FOR BIT DATA,
	is_active BOOLEAN NOT NULL,
	hkey VARCHAR(32672) NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.dm_att_agg_map
(
	attribute_uuid CHAR(16) FOR BIT DATA NOT NULL,
	agg_name VARCHAR(10) NOT NULL,
	PRIMARY KEY (ATTRIBUTE_UUID, AGG_NAME)
);

CREATE TABLE smart.dm_category
(
	uuid CHAR(16) FOR BIT DATA NOT NULL,
	ca_uuid CHAR(16) FOR BIT DATA NOT NULL,
	keyid VARCHAR(128) NOT NULL,
	parent_category_uuid CHAR(16) FOR BIT DATA,
	is_multiple BOOLEAN,
	cat_order SMALLINT,
	is_active BOOLEAN NOT NULL,
	hkey VARCHAR(32672) NOT NULL,
	PRIMARY KEY (UUID)
);


CREATE TABLE smart.dm_cat_att_map
(
	category_uuid CHAR(16) FOR BIT DATA NOT NULL,
	attribute_uuid CHAR(16) FOR BIT DATA NOT NULL,
	att_order SMALLINT NOT NULL,
	is_active BOOLEAN NOT NULL,
	PRIMARY KEY (CATEGORY_UUID, ATTRIBUTE_UUID)
);




/* Create Foreign Keys */

ALTER TABLE smart.dm_attribute
	ADD CONSTRAINT dm_attribute_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;


ALTER TABLE smart.dm_category
	ADD CONSTRAINT dm_category_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;



ALTER TABLE smart.dm_att_agg_map
	ADD CONSTRAINT dm_att_agg_map_agg_name_fk FOREIGN KEY (AGG_NAME)
	REFERENCES smart.dm_aggregation (NAME)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE smart.dm_attribute_list
	ADD CONSTRAINT dm_attribute_list_attribute_uuid_fk FOREIGN KEY (ATTRIBUTE_UUID)
	REFERENCES smart.dm_attribute (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;


ALTER TABLE smart.dm_att_agg_map
	ADD CONSTRAINT dm_att_agg_map_attribute_uuid_fk FOREIGN KEY (ATTRIBUTE_UUID)
	REFERENCES smart.dm_attribute (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.dm_cat_att_map
	ADD CONSTRAINT dm_cat_att_map_attribute_uuid_fk FOREIGN KEY (ATTRIBUTE_UUID)
	REFERENCES smart.dm_attribute (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;


ALTER TABLE smart.dm_cat_att_map 
	ADD CONSTRAINT dm_cat_att_map_category_uuid_fk FOREIGN KEY (CATEGORY_UUID)
	REFERENCES smart.dm_category (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.dm_category 
	ADD CONSTRAINT dm_category_parent_category_uuid_fk FOREIGN KEY (PARENT_CATEGORY_UUID)
	REFERENCES smart.dm_category (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.dm_attribute_tree 
	ADD CONSTRAINT dm_attribut_tree_parent_uuid_fk FOREIGN KEY (PARENT_UUID)
	REFERENCES smart.dm_attribute_tree (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.dm_attribute_tree 
	ADD CONSTRAINT dm_attribut_uuid_fk FOREIGN KEY (ATTRIBUTE_UUID)
	REFERENCES smart.dm_attribute (UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;
create index dm_category_hkey_idx on smart.dm_category(hkey);

create index dm_attribute_keyid_idx on smart.dm_attribute(keyid);
create index dm_attribute_list_keyid_idx on smart.dm_attribute_list(keyid);
create index dm_attribute_tree_hkey_idx on smart.dm_attribute_tree(hkey);
