CREATE TABLE smart.configurable_model
(
	UUID CHAR(16) for bit data NOT NULL,
	CA_UUID CHAR(16) for bit data  NOT NULL,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_node
(
	UUID CHAR(16) for bit data NOT NULL,
	CM_UUID CHAR(16) for bit data  NOT NULL,
	CATEGORY_UUID CHAR(16) for bit data,
	PARENT_NODE_UUID CHAR(16) for bit data,
	NODE_ORDER SMALLINT,
	PHOTO_ALLOWED BOOLEAN,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute
(
	UUID CHAR(16) for bit data NOT NULL,
	NODE_UUID CHAR(16) for bit data  NOT NULL,
	ATTRIBUTE_UUID CHAR(16) for bit data  NOT NULL,
	ATTRIBUTE_ORDER SMALLINT,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute_option
(
	UUID CHAR(16) for bit data NOT NULL,
	CM_ATTRIBUTE_UUID CHAR(16) for bit data  NOT NULL,
	OPTION_ID VARCHAR(128) NOT NULL,
	NUMBER_VALUE DOUBLE,
	STRING_VALUE VARCHAR(1024),
	UUID_VALUE CHAR(16) for bit data,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute_list
(
	UUID CHAR(16) for bit data NOT NULL,
	CM_UUID CHAR(16) for bit data  NOT NULL,
	LIST_ELEMENT_UUID CHAR(16) for bit data  NOT NULL,
	IS_ACTIVE BOOLEAN  NOT NULL,
	PRIMARY KEY (UUID)
);

CREATE TABLE smart.cm_attribute_tree_node
(
	UUID CHAR(16) for bit data NOT NULL,
	CM_UUID CHAR(16) for bit data  NOT NULL,
	DM_TREE_NODE_UUID CHAR(16) for bit data  NOT NULL,
	IS_ACTIVE BOOLEAN  NOT NULL,
	PRIMARY KEY (UUID)
);


ALTER TABLE smart.configurable_model
	ADD CONSTRAINT configurable_model_ca_uuid_fk FOREIGN KEY (CA_UUID)
	REFERENCES smart.conservation_area(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_node
	ADD CONSTRAINT cm_node_cm_uuid_fk FOREIGN KEY (CM_UUID)
	REFERENCES smart.configurable_model(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_node
	ADD CONSTRAINT cm_node_category_uuid_fk FOREIGN KEY (CATEGORY_UUID)
	REFERENCES smart.dm_category(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_attribute
	ADD CONSTRAINT cm_attribute_node_uuid_fk FOREIGN KEY (NODE_UUID)
	REFERENCES smart.cm_node(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_attribute
	ADD CONSTRAINT cm_attribute_attribute_uuid_fk FOREIGN KEY (ATTRIBUTE_UUID)
	REFERENCES smart.dm_attribute(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_attribute_option
	ADD CONSTRAINT cm_attribute_option_cm_attribute_uuid_fk FOREIGN KEY (CM_ATTRIBUTE_UUID)
	REFERENCES smart.cm_attribute(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_attribute_list
	ADD CONSTRAINT cm_attribute_list_cm_uuid_fk FOREIGN KEY (CM_UUID)
	REFERENCES smart.configurable_model(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_attribute_list
	ADD CONSTRAINT cm_attribute_list_list_element_uuid_fk FOREIGN KEY (LIST_ELEMENT_UUID)
	REFERENCES smart.dm_attribute_list(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_attribute_tree_node
	ADD CONSTRAINT cm_attribute_tree_node_tree_node_uuid_fk FOREIGN KEY (DM_TREE_NODE_UUID)
	REFERENCES smart.dm_attribute_tree(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;

ALTER TABLE smart.cm_attribute_tree_node
	ADD CONSTRAINT cm_attribute_tree_node_cm_uuid_fk FOREIGN KEY (CM_UUID)
	REFERENCES smart.configurable_model(UUID)
	ON UPDATE RESTRICT
	ON DELETE CASCADE
;