--SMART.CONNECT_CHANGE_LOG
--SMART.CONNECT_STATUS
--SMART.DB_VERSION
--SMART.DM_AGGREGATION
--SMART.DM_AGGREGATION_I18N





DROP TRIGGER trg_agency_insert;
DROP TRIGGER trg_agency_update;
DROP TRIGGER trg_agency_delete;
DROP TRIGGER trg_area_geometries_insert;
DROP TRIGGER trg_area_geometries_update;
DROP TRIGGER trg_area_geometries_delete;
DROP TRIGGER trg_ca_projection_insert;
DROP TRIGGER trg_ca_projection_update;
DROP TRIGGER trg_ca_projection_delete;
DROP TRIGGER trg_connect_account_insert;
DROP TRIGGER trg_connect_account_update;
DROP TRIGGER trg_connect_account_delete;
DROP TRIGGER trg_connect_server_insert;
DROP TRIGGER trg_connect_server_update;
DROP TRIGGER trg_connect_server_delete;
DROP TRIGGER trg_conservation_area_insert;
DROP TRIGGER trg_conservation_area_update;
DROP TRIGGER trg_conservation_area_delete;
DROP TRIGGER trg_ct_properties_option_insert;
DROP TRIGGER trg_ct_properties_option_update;
DROP TRIGGER trg_ct_properties_option_delete;
DROP TRIGGER trg_dm_attribute_insert;
DROP TRIGGER trg_dm_attribute_update;
DROP TRIGGER trg_dm_attribute_delete;
DROP TRIGGER trg_dm_attribute_list_insert;
DROP TRIGGER trg_dm_attribute_list_update;
DROP TRIGGER trg_dm_attribute_list_delete;
DROP TRIGGER trg_dm_attribute_tree_insert;
DROP TRIGGER trg_dm_attribute_tree_update;
DROP TRIGGER trg_dm_attribute_tree_delete;
DROP TRIGGER trg_dm_category_insert;
DROP TRIGGER trg_dm_category_update;
DROP TRIGGER trg_dm_category_delete;
DROP TRIGGER trg_dm_att_agg_map_insert;
DROP TRIGGER trg_dm_att_agg_map_update;
DROP TRIGGER trg_dm_att_agg_map_delete;
DROP TRIGGER trg_dm_cat_att_map_insert;
DROP TRIGGER trg_dm_cat_att_map_update;
DROP TRIGGER trg_dm_cat_att_map_delete;
DROP TRIGGER trg_employee_insert;
DROP TRIGGER trg_employee_update;
DROP TRIGGER trg_employee_delete;
DROP TRIGGER trg_entity_insert;
DROP TRIGGER trg_entity_update;
DROP TRIGGER trg_entity_delete;
DROP TRIGGER trg_entity_attribute_insert;
DROP TRIGGER trg_entity_attribute_update;
DROP TRIGGER trg_entity_attribute_delete;
DROP TRIGGER trg_entity_attribute_value_insert;
DROP TRIGGER trg_entity_attribute_value_update;
DROP TRIGGER trg_entity_attribute_value_delete;
DROP TRIGGER trg_entity_gridded_query_insert;
DROP TRIGGER trg_entity_gridded_query_update;
DROP TRIGGER trg_entity_gridded_query_delete;
DROP TRIGGER trg_entity_observation_query_insert;
DROP TRIGGER trg_entity_observation_query_update;
DROP TRIGGER trg_entity_observation_query_delete;
DROP TRIGGER trg_entity_summary_query_insert;
DROP TRIGGER trg_entity_summary_query_update;
DROP TRIGGER trg_entity_summary_query_delete;
DROP TRIGGER trg_entity_type_insert;
DROP TRIGGER trg_entity_type_update;
DROP TRIGGER trg_entity_type_delete;
DROP TRIGGER trg_entity_waypoint_query_insert;
DROP TRIGGER trg_entity_waypoint_query_update;
DROP TRIGGER trg_entity_waypoint_query_delete;
DROP TRIGGER trg_gridded_query_insert;
DROP TRIGGER trg_gridded_query_update;
DROP TRIGGER trg_gridded_query_delete;
DROP TRIGGER trg_i18n_label_insert;
DROP TRIGGER trg_i18n_label_update;
DROP TRIGGER trg_i18n_label_delete;
DROP TRIGGER trg_informant_insert;
DROP TRIGGER trg_informant_update;
DROP TRIGGER trg_informant_delete;
DROP TRIGGER trg_intelligence_insert;
DROP TRIGGER trg_intelligence_update;
DROP TRIGGER trg_intelligence_delete;
DROP TRIGGER trg_intelligence_attachment_insert;
DROP TRIGGER trg_intelligence_attachment_update;
DROP TRIGGER trg_intelligence_attachment_delete;
DROP TRIGGER trg_intelligence_point_insert;
DROP TRIGGER trg_intelligence_point_update;
DROP TRIGGER trg_intelligence_point_delete;
DROP TRIGGER trg_intelligence_source_insert;
DROP TRIGGER trg_intelligence_source_update;
DROP TRIGGER trg_intelligence_source_delete;
DROP TRIGGER trg_intel_record_query_insert;
DROP TRIGGER trg_intel_record_query_update;
DROP TRIGGER trg_intel_record_query_delete;
DROP TRIGGER trg_intel_summary_query_insert;
DROP TRIGGER trg_intel_summary_query_update;
DROP TRIGGER trg_intel_summary_query_delete;
DROP TRIGGER trg_language_insert;
DROP TRIGGER trg_language_update;
DROP TRIGGER trg_language_delete;
DROP TRIGGER trg_map_styles_insert;
DROP TRIGGER trg_map_styles_update;
DROP TRIGGER trg_map_styles_delete;
DROP TRIGGER trg_mission_insert;
DROP TRIGGER trg_mission_update;
DROP TRIGGER trg_mission_delete;
DROP TRIGGER trg_mission_attribute_insert;
DROP TRIGGER trg_mission_attribute_update;
DROP TRIGGER trg_mission_attribute_delete;
DROP TRIGGER trg_mission_attribute_list_insert;
DROP TRIGGER trg_mission_attribute_list_update;
DROP TRIGGER trg_mission_attribute_list_delete;
DROP TRIGGER trg_mission_day_insert;
DROP TRIGGER trg_mission_day_update;
DROP TRIGGER trg_mission_day_delete;
DROP TRIGGER trg_mission_member_insert;
DROP TRIGGER trg_mission_member_update;
DROP TRIGGER trg_mission_member_delete;
DROP TRIGGER trg_mission_property_insert;
DROP TRIGGER trg_mission_property_update;
DROP TRIGGER trg_mission_property_delete;
DROP TRIGGER trg_mission_property_value_insert;
DROP TRIGGER trg_mission_property_value_update;
DROP TRIGGER trg_mission_property_value_delete;
DROP TRIGGER trg_mission_track_insert;
DROP TRIGGER trg_mission_track_update;
DROP TRIGGER trg_mission_track_delete;
DROP TRIGGER trg_observation_attachment_insert;
DROP TRIGGER trg_observation_attachment_update;
DROP TRIGGER trg_observation_attachment_delete;
DROP TRIGGER trg_observation_options_insert;
DROP TRIGGER trg_observation_options_update;
DROP TRIGGER trg_observation_options_delete;
DROP TRIGGER trg_observation_query_insert;
DROP TRIGGER trg_observation_query_update;
DROP TRIGGER trg_observation_query_delete;
DROP TRIGGER trg_obs_gridded_query_insert;
DROP TRIGGER trg_obs_gridded_query_update;
DROP TRIGGER trg_obs_gridded_query_delete;
DROP TRIGGER trg_obs_observation_query_insert;
DROP TRIGGER trg_obs_observation_query_update;
DROP TRIGGER trg_obs_observation_query_delete;
DROP TRIGGER trg_obs_summary_query_insert;
DROP TRIGGER trg_obs_summary_query_update;
DROP TRIGGER trg_obs_summary_query_delete;
DROP TRIGGER trg_obs_waypoint_query_insert;
DROP TRIGGER trg_obs_waypoint_query_update;
DROP TRIGGER trg_obs_waypoint_query_delete;
DROP TRIGGER trg_patrol_insert;
DROP TRIGGER trg_patrol_update;
DROP TRIGGER trg_patrol_delete;
DROP TRIGGER trg_patrol_intelligence_insert;
DROP TRIGGER trg_patrol_intelligence_update;
DROP TRIGGER trg_patrol_intelligence_delete;
DROP TRIGGER trg_patrol_leg_insert;
DROP TRIGGER trg_patrol_leg_update;
DROP TRIGGER trg_patrol_leg_delete;
DROP TRIGGER trg_patrol_leg_day_insert;
DROP TRIGGER trg_patrol_leg_day_update;
DROP TRIGGER trg_patrol_leg_day_delete;
DROP TRIGGER trg_patrol_leg_members_insert;
DROP TRIGGER trg_patrol_leg_members_update;
DROP TRIGGER trg_patrol_leg_members_delete;
DROP TRIGGER trg_patrol_mandate_insert;
DROP TRIGGER trg_patrol_mandate_update;
DROP TRIGGER trg_patrol_mandate_delete;
DROP TRIGGER trg_patrol_plan_insert;
DROP TRIGGER trg_patrol_plan_update;
DROP TRIGGER trg_patrol_plan_delete;
DROP TRIGGER trg_patrol_query_insert;
DROP TRIGGER trg_patrol_query_update;
DROP TRIGGER trg_patrol_query_delete;
DROP TRIGGER trg_patrol_transport_insert;
DROP TRIGGER trg_patrol_transport_update;
DROP TRIGGER trg_patrol_transport_delete;
DROP TRIGGER trg_patrol_type_insert;
DROP TRIGGER trg_patrol_type_update;
DROP TRIGGER trg_patrol_type_delete;
DROP TRIGGER trg_patrol_waypoint_insert;
DROP TRIGGER trg_patrol_waypoint_update;
DROP TRIGGER trg_patrol_waypoint_delete;
DROP TRIGGER trg_plan_insert;
DROP TRIGGER trg_plan_update;
DROP TRIGGER trg_plan_delete;
DROP TRIGGER trg_plan_target_insert;
DROP TRIGGER trg_plan_target_update;
DROP TRIGGER trg_plan_target_delete;
DROP TRIGGER trg_plan_target_point_insert;
DROP TRIGGER trg_plan_target_point_update;
DROP TRIGGER trg_plan_target_point_delete;
DROP TRIGGER trg_query_folder_insert;
DROP TRIGGER trg_query_folder_update;
DROP TRIGGER trg_query_folder_delete;
DROP TRIGGER trg_rank_insert;
DROP TRIGGER trg_rank_update;
DROP TRIGGER trg_rank_delete;
DROP TRIGGER trg_report_insert;
DROP TRIGGER trg_report_update;
DROP TRIGGER trg_report_delete;
DROP TRIGGER trg_report_folder_insert;
DROP TRIGGER trg_report_folder_update;
DROP TRIGGER trg_report_folder_delete;
DROP TRIGGER trg_report_query_insert;
DROP TRIGGER trg_report_query_update;
DROP TRIGGER trg_report_query_delete;
DROP TRIGGER trg_sampling_unit_insert;
DROP TRIGGER trg_sampling_unit_update;
DROP TRIGGER trg_sampling_unit_delete;
DROP TRIGGER trg_sampling_unit_attribute_insert;
DROP TRIGGER trg_sampling_unit_attribute_update;
DROP TRIGGER trg_sampling_unit_attribute_delete;
DROP TRIGGER trg_sampling_unit_attribute_list_insert;
DROP TRIGGER trg_sampling_unit_attribute_list_update;
DROP TRIGGER trg_sampling_unit_attribute_list_delete;
DROP TRIGGER trg_sampling_unit_attribute_value_insert;
DROP TRIGGER trg_sampling_unit_attribute_value_update;
DROP TRIGGER trg_sampling_unit_attribute_value_delete;
DROP TRIGGER trg_saved_maps_insert;
DROP TRIGGER trg_saved_maps_update;
DROP TRIGGER trg_saved_maps_delete;
DROP TRIGGER trg_screen_option_insert;
DROP TRIGGER trg_screen_option_update;
DROP TRIGGER trg_screen_option_delete;
DROP TRIGGER trg_screen_option_uuid_insert;
DROP TRIGGER trg_screen_option_uuid_update;
DROP TRIGGER trg_screen_option_uuid_delete;
DROP TRIGGER trg_station_insert;
DROP TRIGGER trg_station_update;
DROP TRIGGER trg_station_delete;
DROP TRIGGER trg_summary_query_insert;
DROP TRIGGER trg_summary_query_update;
DROP TRIGGER trg_summary_query_delete;
DROP TRIGGER trg_survey_insert;
DROP TRIGGER trg_survey_update;
DROP TRIGGER trg_survey_delete;
DROP TRIGGER trg_survey_design_insert;
DROP TRIGGER trg_survey_design_update;
DROP TRIGGER trg_survey_design_delete;
DROP TRIGGER trg_survey_design_property_insert;
DROP TRIGGER trg_survey_design_property_update;
DROP TRIGGER trg_survey_design_property_delete;
DROP TRIGGER trg_survey_design_sampling_unit_insert;
DROP TRIGGER trg_survey_design_sampling_unit_update;
DROP TRIGGER trg_survey_design_sampling_unit_delete;
DROP TRIGGER trg_survey_gridded_query_insert;
DROP TRIGGER trg_survey_gridded_query_update;
DROP TRIGGER trg_survey_gridded_query_delete;
DROP TRIGGER trg_survey_mission_query_insert;
DROP TRIGGER trg_survey_mission_query_update;
DROP TRIGGER trg_survey_mission_query_delete;
DROP TRIGGER trg_survey_mission_track_query_insert;
DROP TRIGGER trg_survey_mission_track_query_update;
DROP TRIGGER trg_survey_mission_track_query_delete;
DROP TRIGGER trg_survey_observation_query_insert;
DROP TRIGGER trg_survey_observation_query_update;
DROP TRIGGER trg_survey_observation_query_delete;
DROP TRIGGER trg_survey_summary_query_insert;
DROP TRIGGER trg_survey_summary_query_update;
DROP TRIGGER trg_survey_summary_query_delete;
DROP TRIGGER trg_survey_waypoint_insert;
DROP TRIGGER trg_survey_waypoint_update;
DROP TRIGGER trg_survey_waypoint_delete;
DROP TRIGGER trg_survey_waypoint_query_insert;
DROP TRIGGER trg_survey_waypoint_query_update;
DROP TRIGGER trg_survey_waypoint_query_delete;
DROP TRIGGER trg_team_insert;
DROP TRIGGER trg_team_update;
DROP TRIGGER trg_team_delete;
DROP TRIGGER trg_track_insert;
DROP TRIGGER trg_track_update;
DROP TRIGGER trg_track_delete;
DROP TRIGGER trg_waypoint_insert;
DROP TRIGGER trg_waypoint_update;
DROP TRIGGER trg_waypoint_delete;
DROP TRIGGER trg_waypoint_query_insert;
DROP TRIGGER trg_waypoint_query_update;
DROP TRIGGER trg_waypoint_query_delete;
DROP TRIGGER trg_wp_attachments_insert;
DROP TRIGGER trg_wp_attachments_update;
DROP TRIGGER trg_wp_attachments_delete;
DROP TRIGGER trg_wp_observation_insert;
DROP TRIGGER trg_wp_observation_update;
DROP TRIGGER trg_wp_observation_delete;
DROP TRIGGER trg_wp_observation_attributes_insert;                               
DROP TRIGGER trg_wp_observation_attributes_update;                               
DROP TRIGGER trg_wp_observation_attributes_delete;
DROP TRIGGER trg_cm_attribute_insert;
DROP TRIGGER trg_cm_attribute_update;
DROP TRIGGER trg_cm_attribute_delete;
DROP TRIGGER trg_cm_attribute_list_insert;
DROP TRIGGER trg_cm_attribute_list_update;
DROP TRIGGER trg_cm_attribute_list_delete;
DROP TRIGGER trg_cm_attribute_option_insert;
DROP TRIGGER trg_cm_attribute_option_update;
DROP TRIGGER trg_cm_attribute_option_delete;
DROP TRIGGER trg_cm_attribute_tree_node_insert;
DROP TRIGGER trg_cm_attribute_tree_node_update;
DROP TRIGGER trg_cm_attribute_tree_node_delete;
DROP TRIGGER trg_cm_node_insert;
DROP TRIGGER trg_cm_node_update;
DROP TRIGGER trg_cm_node_delete;
DROP TRIGGER trg_configurable_model_insert;
DROP TRIGGER trg_configurable_model_update;
DROP TRIGGER trg_configurable_model_delete;






--SMART.AGENCY

CREATE TRIGGER trg_agency_insert AFTER INSERT ON smart.agency REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.agency', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_agency_update AFTER UPDATE ON smart.agency REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.agency', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_agency_delete AFTER DELETE ON smart.agency REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.agency', 'uuid', old.uuid, old.ca_uuid);

--SMART.AREA_GEOMETRIES


CREATE TRIGGER trg_area_geometries_insert AFTER INSERT ON smart.area_geometries REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.area_geometries', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_area_geometries_update AFTER UPDATE ON smart.area_geometries REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.area_geometries', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_area_geometries_delete AFTER DELETE ON smart.area_geometries REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.area_geometries', 'uuid', old.uuid, old.ca_uuid);

--SMART.CA_PROJECTION


CREATE TRIGGER trg_ca_projection_insert AFTER INSERT ON smart.ca_projection REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.ca_projection', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_ca_projection_update AFTER UPDATE ON smart.ca_projection REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.ca_projection', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_ca_projection_delete AFTER DELETE ON smart.ca_projection REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.ca_projection', 'uuid', old.uuid, old.ca_uuid);



--SMART.CONNECT_ACCOUNT


CREATE TRIGGER trg_connect_account_insert AFTER INSERT ON smart.connect_account REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.connect_account', 'employee_uuid', new.employee_uuid, server.ca_uuid from smart.connect_server server where server.uuid = new.connect_uuid;
CREATE TRIGGER trg_connect_account_update AFTER UPDATE ON smart.connect_account REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.connect_account', 'employee_uuid', new.employee_uuid, server.ca_uuid from smart.connect_server server where server.uuid = new.connect_uuid;
CREATE TRIGGER trg_connect_account_delete AFTER DELETE ON smart.connect_account REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.connect_account', 'employee_uuid', old.employee_uuid, server.ca_uuid from smart.connect_server server where server.uuid = old.connect_uuid;

--SMART.CONNECT_SERVER


CREATE TRIGGER trg_connect_server_insert AFTER INSERT ON smart.connect_server REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.connect_server', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_connect_server_update AFTER UPDATE ON smart.connect_server REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.connect_server', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_connect_server_delete AFTER DELETE ON smart.connect_server REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.connect_server', 'uuid', old.uuid, old.ca_uuid);

--SMART.CONSERVATION_AREA


CREATE TRIGGER trg_conservation_area_insert AFTER INSERT ON smart.conservation_area REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.conservation_area', 'uuid', new.uuid, new.uuid);
CREATE TRIGGER trg_conservation_area_update AFTER UPDATE ON smart.conservation_area REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.conservation_area', 'uuid', new.uuid, new.uuid);
CREATE TRIGGER trg_conservation_area_delete AFTER DELETE ON smart.conservation_area REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.conservation_area', 'uuid', old.uuid, old.uuid);

--SMART.CT_PROPERTIES_OPTION


CREATE TRIGGER trg_ct_properties_option_insert AFTER INSERT ON smart.ct_properties_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.ct_properties_option', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_ct_properties_option_update AFTER UPDATE ON smart.ct_properties_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.ct_properties_option', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_ct_properties_option_delete AFTER DELETE ON smart.ct_properties_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.ct_properties_option', 'uuid', old.uuid, old.ca_uuid);

--SMART.DM_ATTRIBUTE

CREATE TRIGGER trg_dm_attribute_insert AFTER INSERT ON smart.dm_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.dm_attribute', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_dm_attribute_update AFTER UPDATE ON smart.dm_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.dm_attribute', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_dm_attribute_delete AFTER DELETE ON smart.dm_attribute REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.dm_attribute', 'uuid', old.uuid, old.ca_uuid);

--SMART.DM_ATTRIBUTE_LIST

CREATE TRIGGER trg_dm_attribute_list_insert AFTER INSERT ON smart.dm_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.dm_attribute_list', 'uuid', new.uuid, da.ca_uuid from smart.dm_attribute da where da.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_attribute_list_update AFTER UPDATE ON smart.dm_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.dm_attribute_list', 'uuid', new.uuid, da.ca_uuid from smart.dm_attribute da where da.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_attribute_list_delete AFTER DELETE ON smart.dm_attribute_list REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.dm_attribute_list', 'uuid', old.uuid, da.ca_uuid from smart.dm_attribute da where da.uuid = old.attribute_uuid;

--SMART.DM_ATTRIBUTE_TREE

CREATE TRIGGER trg_dm_attribute_tree_insert AFTER INSERT ON smart.dm_attribute_tree REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.dm_attribute_tree', 'uuid', new.uuid, da.ca_uuid from smart.dm_attribute da where da.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_attribute_tree_update AFTER UPDATE ON smart.dm_attribute_tree REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.dm_attribute_tree', 'uuid', new.uuid, da.ca_uuid from smart.dm_attribute da where da.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_attribute_tree_delete AFTER DELETE ON smart.dm_attribute_tree REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.dm_attribute_tree', 'uuid', old.uuid, da.ca_uuid from smart.dm_attribute da where da.uuid = old.attribute_uuid;

--SMART.DM_CATEGORY

CREATE TRIGGER trg_dm_category_insert AFTER INSERT ON smart.dm_category REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.dm_category', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_dm_category_update AFTER UPDATE ON smart.dm_category REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.dm_category', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_dm_category_delete AFTER DELETE ON smart.dm_category REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.dm_category', 'uuid', old.uuid, old.ca_uuid);

--SMART.DM_ATT_AGG_MAP

CREATE TRIGGER trg_dm_att_agg_map_insert AFTER INSERT ON smart.dm_att_agg_map REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.uuid(), 'INSERT', 'smart.dm_att_agg_map', 'attribute_uuid', new.attribute_uuid, 'agg_name', new.agg_name, a.ca_uuid from smart.dm_attribute a where a.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_att_agg_map_update AFTER UPDATE ON smart.dm_att_agg_map REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.dm_att_agg_map', 'attribute_uuid', new.attribute_uuid, 'agg_name', new.agg_name, a.ca_uuid from smart.dm_attribute a where a.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_att_agg_map_delete AFTER DELETE ON smart.dm_att_agg_map REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) select smart.uuid(), 'DELETE', 'smart.dm_att_agg_map', 'attribute_uuid', old.attribute_uuid, 'agg_name', old.agg_name, a.ca_uuid from smart.dm_attribute a where a.uuid = old.attribute_uuid;

--SMART.DM_CAT_ATT_MAP


CREATE TRIGGER trg_dm_cat_att_map_insert AFTER INSERT ON smart.dm_cat_att_map REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.dm_cat_att_map', 'attribute_uuid', new.attribute_uuid, 'category_uuid', new.category_uuid, a.ca_uuid from smart.dm_attribute a where a.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_cat_att_map_update AFTER UPDATE ON smart.dm_cat_att_map REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.dm_cat_att_map', 'attribute_uuid', new.attribute_uuid, 'category_uuid', new.category_uuid, a.ca_uuid from smart.dm_attribute a where a.uuid = new.attribute_uuid;
CREATE TRIGGER trg_dm_cat_att_map_delete AFTER DELETE ON smart.dm_cat_att_map REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.dm_cat_att_map', 'attribute_uuid', old.attribute_uuid, 'category_uuid', old.category_uuid, a.ca_uuid from smart.dm_attribute a where a.uuid = old.attribute_uuid;


--SMART.EMPLOYEE


CREATE TRIGGER trg_employee_insert AFTER INSERT ON smart.employee REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.employee', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_employee_update AFTER UPDATE ON smart.employee REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.employee', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_employee_delete AFTER DELETE ON smart.employee REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.employee', 'uuid', old.uuid, old.ca_uuid);









--SMART.ENTITY


CREATE TRIGGER trg_entity_insert AFTER INSERT ON smart.entity REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.entity', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid;
CREATE TRIGGER trg_entity_update AFTER UPDATE ON smart.entity REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.entity', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid;
CREATE TRIGGER trg_entity_delete AFTER DELETE ON smart.entity REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.entity', 'uuid', old.uuid, et.ca_uuid from smart.entity_type et where et.uuid = old.entity_type_uuid;


--SMART.ENTITY_ATTRIBUTE


CREATE TRIGGER trg_entity_attribute_insert AFTER INSERT ON smart.entity_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.entity_attribute', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid;
CREATE TRIGGER trg_entity_attribute_update AFTER UPDATE ON smart.entity_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.entity_attribute', 'uuid', new.uuid, et.ca_uuid from smart.entity_type et where et.uuid = new.entity_type_uuid;
CREATE TRIGGER trg_entity_attribute_delete AFTER DELETE ON smart.entity_attribute REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.entity_attribute', 'uuid', old.uuid, et.ca_uuid from smart.entity_type et where et.uuid = old.entity_type_uuid;

--SMART.ENTITY_ATTRIBUTE_VALUE


CREATE TRIGGER trg_entity_attribute_value_insert AFTER INSERT ON smart.entity_attribute_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.entity_attribute_value', 'entity_attribute_uuid', new.entity_attribute_uuid, 'entity_uuid', new.entity_uuid, et.ca_uuid from smart.entity_type et, smart.entity e where e.entity_type_uuid = et.uuid and e.uuid = new.entity_uuid;
CREATE TRIGGER trg_entity_attribute_value_update AFTER UPDATE ON smart.entity_attribute_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.entity_attribute_value', 'entity_attribute_uuid', new.entity_attribute_uuid, 'entity_uuid', new.entity_uuid, et.ca_uuid from smart.entity_type et, smart.entity e where e.entity_type_uuid = et.uuid and e.uuid = new.entity_uuid;
CREATE TRIGGER trg_entity_attribute_value_delete AFTER DELETE ON smart.entity_attribute_value REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.entity_attribute_value', 'entity_attribute_uuid', old.entity_attribute_uuid, 'entity_uuid', old.entity_uuid, et.ca_uuid from smart.entity_type et, smart.entity e where e.entity_type_uuid = et.uuid and e.uuid = old.entity_uuid;

--SMART.ENTITY_GRIDDED_QUERY


CREATE TRIGGER trg_entity_gridded_query_insert AFTER INSERT ON smart.entity_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.entity_gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_gridded_query_update AFTER UPDATE ON smart.entity_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.entity_gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_gridded_query_delete AFTER DELETE ON smart.entity_gridded_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.entity_gridded_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.ENTITY_OBSERVATION_QUERY


CREATE TRIGGER trg_entity_observation_query_insert AFTER INSERT ON smart.entity_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.entity_observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_observation_query_update AFTER UPDATE ON smart.entity_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.entity_observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_observation_query_delete AFTER DELETE ON smart.entity_observation_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.entity_observation_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.ENTITY_SUMMARY_QUERY


CREATE TRIGGER trg_entity_summary_query_insert AFTER INSERT ON smart.entity_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.entity_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_summary_query_update AFTER UPDATE ON smart.entity_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.entity_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_summary_query_delete AFTER DELETE ON smart.entity_summary_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.entity_summary_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.ENTITY_TYPE


CREATE TRIGGER trg_entity_type_insert AFTER INSERT ON smart.entity_type REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.entity_type', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_type_update AFTER UPDATE ON smart.entity_type REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.entity_type', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_type_delete AFTER DELETE ON smart.entity_type REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.entity_type', 'uuid', old.uuid, old.ca_uuid);

--SMART.ENTITY_WAYPOINT_QUERY


CREATE TRIGGER trg_entity_waypoint_query_insert AFTER INSERT ON smart.entity_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.entity_waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_waypoint_query_update AFTER UPDATE ON smart.entity_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.entity_waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_entity_waypoint_query_delete AFTER DELETE ON smart.entity_waypoint_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.entity_waypoint_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.GRIDDED_QUERY


CREATE TRIGGER trg_gridded_query_insert AFTER INSERT ON smart.gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_gridded_query_update AFTER UPDATE ON smart.gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_gridded_query_delete AFTER DELETE ON smart.gridded_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.gridded_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.I18N_LABEL


CREATE TRIGGER trg_i18n_label_insert AFTER INSERT ON smart.i18n_label REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.i18n_label', 'element_uuid', new.element_uuid, 'language_uuid', new.language_uuid, l.ca_uuid from smart.language l where l.uuid = new.language_uuid;
CREATE TRIGGER trg_i18n_label_update AFTER UPDATE ON smart.i18n_label REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.i18n_label', 'element_uuid', new.element_uuid, 'language_uuid', new.language_uuid, l.ca_uuid from smart.language l where l.uuid = new.language_uuid;
CREATE TRIGGER trg_i18n_label_delete AFTER DELETE ON smart.i18n_label REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.i18n_label', 'element_uuid', old.element_uuid, 'language_uuid', old.language_uuid, l.ca_uuid from smart.language l where l.uuid = old.language_uuid;

--SMART.INFORMANT


CREATE TRIGGER trg_informant_insert AFTER INSERT ON smart.informant REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.informant', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_informant_update AFTER UPDATE ON smart.informant REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.informant', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_informant_delete AFTER DELETE ON smart.informant REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.informant', 'uuid', old.uuid, old.ca_uuid);

--SMART.INTELLIGENCE


CREATE TRIGGER trg_intelligence_insert AFTER INSERT ON smart.intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.intelligence', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intelligence_update AFTER UPDATE ON smart.intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.intelligence', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intelligence_delete AFTER DELETE ON smart.intelligence REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.intelligence', 'uuid', old.uuid, old.ca_uuid);

--SMART.INTELLIGENCE_ATTACHMENT


CREATE TRIGGER trg_intelligence_attachment_insert AFTER INSERT ON smart.intelligence_attachment REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.intelligence_attachment', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid;
CREATE TRIGGER trg_intelligence_attachment_update AFTER UPDATE ON smart.intelligence_attachment REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.intelligence_attachment', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid;
CREATE TRIGGER trg_intelligence_attachment_delete AFTER DELETE ON smart.intelligence_attachment REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.intelligence_attachment', 'uuid', old.uuid, i.ca_uuid from smart.intelligence i where i.uuid = old.intelligence_uuid;

--SMART.INTELLIGENCE_POINT


CREATE TRIGGER trg_intelligence_point_insert AFTER INSERT ON smart.intelligence_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.intelligence_point', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid;
CREATE TRIGGER trg_intelligence_point_update AFTER UPDATE ON smart.intelligence_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.intelligence_point', 'uuid', new.uuid, i.ca_uuid from smart.intelligence i where i.uuid = new.intelligence_uuid;
CREATE TRIGGER trg_intelligence_point_delete AFTER DELETE ON smart.intelligence_point REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.intelligence_point', 'uuid', old.uuid, i.ca_uuid from smart.intelligence i where i.uuid = old.intelligence_uuid;

--SMART.INTELLIGENCE_SOURCE


CREATE TRIGGER trg_intelligence_source_insert AFTER INSERT ON smart.intelligence_source REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.intelligence_source', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intelligence_source_update AFTER UPDATE ON smart.intelligence_source REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.intelligence_source', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intelligence_source_delete AFTER DELETE ON smart.intelligence_source REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.intelligence_source', 'uuid', old.uuid, old.ca_uuid);

--SMART.INTEL_RECORD_QUERY


CREATE TRIGGER trg_intel_record_query_insert AFTER INSERT ON smart.intel_record_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.intel_record_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intel_record_query_update AFTER UPDATE ON smart.intel_record_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.intel_record_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intel_record_query_delete AFTER DELETE ON smart.intel_record_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.intel_record_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.INTEL_SUMMARY_QUERY


CREATE TRIGGER trg_intel_summary_query_insert AFTER INSERT ON smart.intel_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.intel_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intel_summary_query_update AFTER UPDATE ON smart.intel_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.intel_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_intel_summary_query_delete AFTER DELETE ON smart.intel_summary_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.intel_summary_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.LANGUAGE


CREATE TRIGGER trg_language_insert AFTER INSERT ON smart.language REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.language', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_language_update AFTER UPDATE ON smart.language REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.language', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_language_delete AFTER DELETE ON smart.language REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.language', 'uuid', old.uuid, old.ca_uuid);

--SMART.MAP_STYLES


CREATE TRIGGER trg_map_styles_insert AFTER INSERT ON smart.map_styles REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.map_styles', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_map_styles_update AFTER UPDATE ON smart.map_styles REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.map_styles', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_map_styles_delete AFTER DELETE ON smart.map_styles REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.map_styles', 'uuid', old.uuid, old.ca_uuid);

--SMART.MISSION
CREATE TRIGGER trg_mission_insert AFTER INSERT ON smart.mission REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.mission', 'uuid', new.uuid, sd.ca_uuid from smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = new.survey_uuid;
CREATE TRIGGER trg_mission_update AFTER UPDATE ON smart.mission REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.mission', 'uuid', new.uuid, sd.ca_uuid from smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = new.survey_uuid;
CREATE TRIGGER trg_mission_delete AFTER DELETE ON smart.mission REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.mission', 'uuid', old.uuid, sd.ca_uuid from smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = old.survey_uuid;

--SMART.MISSION_ATTRIBUTE

CREATE TRIGGER trg_mission_attribute_insert AFTER INSERT ON smart.mission_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.mission_attribute', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_mission_attribute_update AFTER UPDATE ON smart.mission_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.mission_attribute', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_mission_attribute_delete AFTER DELETE ON smart.mission_attribute REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.mission_attribute', 'uuid', old.uuid, old.ca_uuid);

--SMART.MISSION_ATTRIBUTE_LIST

CREATE TRIGGER trg_mission_attribute_list_insert AFTER INSERT ON smart.mission_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.mission_attribute_list', 'uuid', new.uuid, ma.ca_uuid FROM smart.mission_attribute ma WHERE ma.uuid = new.mission_attribute_uuid;
CREATE TRIGGER trg_mission_attribute_list_update AFTER UPDATE ON smart.mission_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.mission_attribute_list', 'uuid', new.uuid, ma.ca_uuid FROM smart.mission_attribute ma WHERE ma.uuid = new.mission_attribute_uuid;
CREATE TRIGGER trg_mission_attribute_list_delete AFTER DELETE ON smart.mission_attribute_list REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.mission_attribute_list', 'uuid', old.uuid, ma.ca_uuid FROM smart.mission_attribute ma WHERE ma.uuid = old.mission_attribute_uuid;

--SMART.MISSION_DAY


CREATE TRIGGER trg_mission_day_insert AFTER INSERT ON smart.mission_day REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.mission_day', 'uuid', new.uuid, sd.ca_uuid from smart.mission m, smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = new.mission_uuid;
CREATE TRIGGER trg_mission_day_update AFTER UPDATE ON smart.mission_day REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.mission_day', 'uuid', new.uuid, sd.ca_uuid from smart.mission m, smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = new.mission_uuid;
CREATE TRIGGER trg_mission_day_delete AFTER DELETE ON smart.mission_day REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.mission_day', 'uuid', old.uuid, sd.ca_uuid from smart.mission m, smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = old.mission_uuid;

--SMART.MISSION_MEMBER


CREATE TRIGGER trg_mission_member_insert AFTER INSERT ON smart.mission_member REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.mission_member', 'mission_uuid', new.mission_uuid, 'employee_uuid', new.employee_uuid, e.ca_uuid from smart.employee e where e.uuid = new.employee_uuid;
CREATE TRIGGER trg_mission_member_update AFTER UPDATE ON smart.mission_member REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.mission_member', 'mission_uuid', new.mission_uuid, 'employee_uuid', new.employee_uuid, e.ca_uuid from smart.employee e where e.uuid = new.employee_uuid;
CREATE TRIGGER trg_mission_member_delete AFTER DELETE ON smart.mission_member REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.mission_member', 'mission_uuid', old.mission_uuid, 'employee_uuid', old.employee_uuid, e.ca_uuid from smart.employee e where e.uuid = old.employee_uuid;

--SMART.MISSION_PROPERTY


CREATE TRIGGER trg_mission_property_insert AFTER INSERT ON smart.mission_property REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.mission_property', 'survey_design_uuid', new.survey_design_uuid, 'mission_attribute_uuid', new.mission_attribute_uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_mission_property_update AFTER UPDATE ON smart.mission_property REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.mission_property', 'survey_design_uuid', new.survey_design_uuid, 'mission_attribute_uuid', new.mission_attribute_uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_mission_property_delete AFTER DELETE ON smart.mission_property REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.mission_property', 'survey_design_uuid', old.survey_design_uuid, 'mission_attribute_uuid', old.mission_attribute_uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = old.survey_design_uuid;

--SMART.MISSION_PROPERTY_VALUE


CREATE TRIGGER trg_mission_property_value_insert AFTER INSERT ON smart.mission_property_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.mission_property_value', 'mission_uuid', new.mission_uuid, 'mission_attribute_uuid', new.mission_attribute_uuid, ma.ca_uuid from smart.mission_attribute ma where ma.uuid = new.mission_attribute_uuid;
CREATE TRIGGER trg_mission_property_value_update AFTER UPDATE ON smart.mission_property_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.mission_property_value', 'mission_uuid', new.mission_uuid, 'mission_attribute_uuid', new.mission_attribute_uuid, ma.ca_uuid from smart.mission_attribute ma where ma.uuid = new.mission_attribute_uuid;
CREATE TRIGGER trg_mission_property_value_delete AFTER DELETE ON smart.mission_property_value REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.mission_property_value', 'mission_uuid', old.mission_uuid, 'mission_attribute_uuid', old.mission_attribute_uuid, ma.ca_uuid from smart.mission_attribute ma where ma.uuid = old.mission_attribute_uuid;

--SMART.MISSION_TRACK


CREATE TRIGGER trg_mission_track_insert AFTER INSERT ON smart.mission_track REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.mission_track', 'uuid', new.uuid, sd.ca_uuid from smart.mission_day md, smart.mission m, smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = md.mission_uuid and md.uuid = new.mission_day_uuid;
CREATE TRIGGER trg_mission_track_update AFTER UPDATE ON smart.mission_track REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.mission_track', 'uuid', new.uuid, sd.ca_uuid from smart.mission_day md, smart.mission m, smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = md.mission_uuid and md.uuid = new.mission_day_uuid;
CREATE TRIGGER trg_mission_track_delete AFTER DELETE ON smart.mission_track REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.mission_track', 'uuid', old.uuid, sd.ca_uuid from smart.mission_day md, smart.mission m, smart.survey s, smart.survey_design sd where s.survey_design_uuid = sd.uuid and s.uuid = m.survey_uuid and m.uuid = md.mission_uuid and md.uuid = old.mission_day_uuid;

--SMART.OBSERVATION_ATTACHMENT


CREATE TRIGGER trg_observation_attachment_insert AFTER INSERT ON smart.observation_attachment REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.observation_attachment', 'uuid', new.uuid, wp.ca_uuid from smart.wp_observation ob, smart.waypoint wp where ob.wp_uuid = wp.uuid and ob.uuid = new.obs_uuid;
CREATE TRIGGER trg_observation_attachment_update AFTER UPDATE ON smart.observation_attachment REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.observation_attachment', 'uuid', new.uuid, wp.ca_uuid from smart.wp_observation ob, smart.waypoint wp where ob.wp_uuid = wp.uuid and ob.uuid = new.obs_uuid;
CREATE TRIGGER trg_observation_attachment_delete AFTER DELETE ON smart.observation_attachment REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.observation_attachment', 'uuid', old.uuid, wp.ca_uuid from smart.wp_observation ob, smart.waypoint wp where ob.wp_uuid = wp.uuid and ob.uuid = old.obs_uuid;

--SMART.OBSERVATION_OPTIONS


CREATE TRIGGER trg_observation_options_insert AFTER INSERT ON smart.observation_options REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.observation_options', 'ca_uuid', new.ca_uuid, new.ca_uuid);
CREATE TRIGGER trg_observation_options_update AFTER UPDATE ON smart.observation_options REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.observation_options', 'ca_uuid', new.ca_uuid, new.ca_uuid);
CREATE TRIGGER trg_observation_options_delete AFTER DELETE ON smart.observation_options REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.observation_options', 'ca_uuid', old.ca_uuid, old.ca_uuid);

--SMART.OBSERVATION_QUERY


CREATE TRIGGER trg_observation_query_insert AFTER INSERT ON smart.observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_observation_query_update AFTER UPDATE ON smart.observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_observation_query_delete AFTER DELETE ON smart.observation_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.observation_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.OBS_GRIDDED_QUERY


CREATE TRIGGER trg_obs_gridded_query_insert AFTER INSERT ON smart.obs_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.obs_gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_gridded_query_update AFTER UPDATE ON smart.obs_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.obs_gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_gridded_query_delete AFTER DELETE ON smart.obs_gridded_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.obs_gridded_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.OBS_OBSERVATION_QUERY


CREATE TRIGGER trg_obs_observation_query_insert AFTER INSERT ON smart.obs_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.obs_observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_observation_query_update AFTER UPDATE ON smart.obs_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.obs_observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_observation_query_delete AFTER DELETE ON smart.obs_observation_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.obs_observation_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.OBS_SUMMARY_QUERY


CREATE TRIGGER trg_obs_summary_query_insert AFTER INSERT ON smart.obs_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.obs_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_summary_query_update AFTER UPDATE ON smart.obs_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.obs_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_summary_query_delete AFTER DELETE ON smart.obs_summary_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.obs_summary_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.OBS_WAYPOINT_QUERY


CREATE TRIGGER trg_obs_waypoint_query_insert AFTER INSERT ON smart.obs_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.obs_waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_waypoint_query_update AFTER UPDATE ON smart.obs_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.obs_waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_obs_waypoint_query_delete AFTER DELETE ON smart.obs_waypoint_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.obs_waypoint_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.PATROL


CREATE TRIGGER trg_patrol_insert AFTER INSERT ON smart.patrol REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.patrol', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_update AFTER UPDATE ON smart.patrol REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.patrol', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_delete AFTER DELETE ON smart.patrol REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.patrol', 'uuid', old.uuid, old.ca_uuid);

--SMART.PATROL_INTELLIGENCE

CREATE TRIGGER trg_patrol_intelligence_insert AFTER INSERT ON smart.patrol_intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.patrol_intelligence', 'patrol_uuid', new.patrol_uuid, 'intelligence_uuid', new.intelligence_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid;
CREATE TRIGGER trg_patrol_intelligence_update AFTER UPDATE ON smart.patrol_intelligence REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.patrol_intelligence', 'patrol_uuid', new.patrol_uuid, 'intelligence_uuid', new.intelligence_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid;
CREATE TRIGGER trg_patrol_intelligence_delete AFTER DELETE ON smart.patrol_intelligence REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.patrol_intelligence', 'patrol_uuid', old.patrol_uuid, 'intelligence_uuid', old.intelligence_uuid, p.ca_uuid from smart.patrol p where p.uuid = old.patrol_uuid;

--SMART.PATROL_LEG

CREATE TRIGGER trg_patrol_leg_insert AFTER INSERT ON smart.patrol_leg REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.patrol_leg', 'uuid', new.uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid;
CREATE TRIGGER trg_patrol_leg_update AFTER UPDATE ON smart.patrol_leg REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.patrol_leg', 'uuid', new.uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid;
CREATE TRIGGER trg_patrol_leg_delete AFTER DELETE ON smart.patrol_leg REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.patrol_leg', 'uuid', old.uuid, p.ca_uuid from smart.patrol p where p.uuid = old.patrol_uuid;

--SMART.PATROL_LEG_DAY

CREATE TRIGGER trg_patrol_leg_day_insert AFTER INSERT ON smart.patrol_leg_day REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.patrol_leg_day', 'uuid', new.uuid, p.ca_uuid from smart.patrol p, smart.patrol_leg pl where pl.patrol_uuid = p.uuid and pl.uuid = new.patrol_leg_uuid;
CREATE TRIGGER trg_patrol_leg_day_update AFTER UPDATE ON smart.patrol_leg_day REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.patrol_leg_day', 'uuid', new.uuid, p.ca_uuid from smart.patrol p, smart.patrol_leg pl where pl.patrol_uuid = p.uuid and pl.uuid = new.patrol_leg_uuid;
CREATE TRIGGER trg_patrol_leg_day_delete AFTER DELETE ON smart.patrol_leg_day REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.patrol_leg_day', 'uuid', old.uuid, p.ca_uuid from smart.patrol p, smart.patrol_leg pl where pl.patrol_uuid = p.uuid and pl.uuid = old.patrol_leg_uuid;

--SMART.PATROL_LEG_MEMBERS


CREATE TRIGGER trg_patrol_leg_members_insert AFTER INSERT ON smart.patrol_leg_members REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.patrol_leg_members', 'patrol_leg_uuid', new.patrol_leg_uuid, 'employee_uuid', new.employee_uuid, e.ca_uuid from smart.employee e where e.uuid = new.employee_uuid;
CREATE TRIGGER trg_patrol_leg_members_update AFTER UPDATE ON smart.patrol_leg_members REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.patrol_leg_members', 'patrol_leg_uuid', new.patrol_leg_uuid, 'employee_uuid', new.employee_uuid, e.ca_uuid from smart.employee e where e.uuid = new.employee_uuid;
CREATE TRIGGER trg_patrol_leg_members_delete AFTER DELETE ON smart.patrol_leg_members REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.patrol_leg_members', 'patrol_leg_uuid', old.patrol_leg_uuid, 'employee_uuid', old.employee_uuid, e.ca_uuid from smart.employee e where e.uuid = old.employee_uuid;

--SMART.PATROL_MANDATE


CREATE TRIGGER trg_patrol_mandate_insert AFTER INSERT ON smart.patrol_mandate REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.patrol_mandate', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_mandate_update AFTER UPDATE ON smart.patrol_mandate REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.patrol_mandate', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_mandate_delete AFTER DELETE ON smart.patrol_mandate REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.patrol_mandate', 'uuid', old.uuid, old.ca_uuid);

--SMART.PATROL_PLAN


CREATE TRIGGER trg_patrol_plan_insert AFTER INSERT ON smart.patrol_plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.patrol_plan', 'patrol_uuid', new.patrol_uuid, 'plan_uuid', new.plan_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid;
CREATE TRIGGER trg_patrol_plan_update AFTER UPDATE ON smart.patrol_plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.patrol_plan', 'patrol_uuid', new.patrol_uuid, 'plan_uuid', new.plan_uuid, p.ca_uuid from smart.patrol p where p.uuid = new.patrol_uuid;
CREATE TRIGGER trg_patrol_plan_delete AFTER DELETE ON smart.patrol_plan REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.patrol_plan', 'patrol_uuid', old.patrol_uuid, 'plan_uuid', old.plan_uuid, p.ca_uuid from smart.patrol p where p.uuid = old.patrol_uuid;

--SMART.PATROL_QUERY


CREATE TRIGGER trg_patrol_query_insert AFTER INSERT ON smart.patrol_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.patrol_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_query_update AFTER UPDATE ON smart.patrol_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.patrol_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_query_delete AFTER DELETE ON smart.patrol_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.patrol_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.PATROL_TRANSPORT


CREATE TRIGGER trg_patrol_transport_insert AFTER INSERT ON smart.patrol_transport REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.patrol_transport', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_transport_update AFTER UPDATE ON smart.patrol_transport REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.patrol_transport', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_patrol_transport_delete AFTER DELETE ON smart.patrol_transport REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.patrol_transport', 'uuid', old.uuid, old.ca_uuid);

--SMART.PATROL_TYPE


CREATE TRIGGER trg_patrol_type_insert AFTER INSERT ON smart.patrol_type REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.patrol_type', 'ca_uuid', new.ca_uuid, 'patrol_type', new.patrol_type, new.ca_uuid);
CREATE TRIGGER trg_patrol_type_update AFTER UPDATE ON smart.patrol_type REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.patrol_type', 'ca_uuid', new.ca_uuid, 'patrol_type', new.patrol_type, new.ca_uuid);
CREATE TRIGGER trg_patrol_type_delete AFTER DELETE ON smart.patrol_type REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_str, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.patrol_type', 'ca_uuid', old.ca_uuid, 'patrol_type', old.patrol_type, old.ca_uuid);

--SMART.PATROL_WAYPOINT


CREATE TRIGGER trg_patrol_waypoint_insert AFTER INSERT ON smart.patrol_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.patrol_waypoint', 'leg_day_uuid', new.leg_day_uuid, 'wp_uuid', new.wp_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_patrol_waypoint_update AFTER UPDATE ON smart.patrol_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.patrol_waypoint', 'leg_day_uuid', new.leg_day_uuid, 'wp_uuid', new.wp_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_patrol_waypoint_delete AFTER DELETE ON smart.patrol_waypoint REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.patrol_waypoint', 'leg_day_uuid', old.leg_day_uuid, 'wp_uuid', old.wp_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = old.wp_uuid;

--SMART.PLAN


CREATE TRIGGER trg_plan_insert AFTER INSERT ON smart.plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.plan', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_plan_update AFTER UPDATE ON smart.plan REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.plan', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_plan_delete AFTER DELETE ON smart.plan REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.plan', 'uuid', old.uuid, old.ca_uuid);

--SMART.PLAN_TARGET


CREATE TRIGGER trg_plan_target_insert AFTER INSERT ON smart.plan_target REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.plan_target', 'uuid', new.uuid, p.ca_uuid from smart.plan p where p.uuid = new.plan_uuid;
CREATE TRIGGER trg_plan_target_update AFTER UPDATE ON smart.plan_target REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.plan_target', 'uuid', new.uuid, p.ca_uuid from smart.plan p where p.uuid = new.plan_uuid;
CREATE TRIGGER trg_plan_target_delete AFTER DELETE ON smart.plan_target REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.plan_target', 'uuid', old.uuid, p.ca_uuid from smart.plan p where p.uuid = old.plan_uuid;

--SMART.PLAN_TARGET_POINT


CREATE TRIGGER trg_plan_target_point_insert AFTER INSERT ON smart.plan_target_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.plan_target_point', 'uuid', new.uuid, p.ca_uuid from smart.plan_target pt, smart.plan p where p.uuid = pt.plan_uuid and pt.uuid = new.plan_target_uuid;
CREATE TRIGGER trg_plan_target_point_update AFTER UPDATE ON smart.plan_target_point REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.plan_target_point', 'uuid', new.uuid, p.ca_uuid from smart.plan_target pt, smart.plan p where p.uuid = pt.plan_uuid and pt.uuid = new.plan_target_uuid;
CREATE TRIGGER trg_plan_target_point_delete AFTER DELETE ON smart.plan_target_point REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.plan_target_point', 'uuid', old.uuid, p.ca_uuid from smart.plan_target pt, smart.plan p where p.uuid = pt.plan_uuid and pt.uuid = old.plan_target_uuid;

--SMART.QUERY_FOLDER


CREATE TRIGGER trg_query_folder_insert AFTER INSERT ON smart.query_folder REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.query_folder', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_query_folder_update AFTER UPDATE ON smart.query_folder REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.query_folder', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_query_folder_delete AFTER DELETE ON smart.query_folder REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.query_folder', 'uuid', old.uuid, old.ca_uuid);

--SMART.RANK


CREATE TRIGGER trg_rank_insert AFTER INSERT ON smart.rank REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.rank', 'uuid', new.uuid, a.ca_uuid from smart.agency a where a.uuid = new.agency_uuid;
CREATE TRIGGER trg_rank_update AFTER UPDATE ON smart.rank REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.rank', 'uuid', new.uuid, a.ca_uuid from smart.agency a where a.uuid = new.agency_uuid;
CREATE TRIGGER trg_rank_delete AFTER DELETE ON smart.rank REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.rank', 'uuid', old.uuid, a.ca_uuid from smart.agency a where a.uuid = old.agency_uuid;

--SMART.REPORT


CREATE TRIGGER trg_report_insert AFTER INSERT ON smart.report REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.report', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_report_update AFTER UPDATE ON smart.report REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.report', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_report_delete AFTER DELETE ON smart.report REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.report', 'uuid', old.uuid, old.ca_uuid);

--SMART.REPORT_FOLDER


CREATE TRIGGER trg_report_folder_insert AFTER INSERT ON smart.report_folder REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.report_folder', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_report_folder_update AFTER UPDATE ON smart.report_folder REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.report_folder', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_report_folder_delete AFTER DELETE ON smart.report_folder REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.report_folder', 'uuid', old.uuid, old.ca_uuid);

--SMART.REPORT_QUERY


CREATE TRIGGER trg_report_query_insert AFTER INSERT ON smart.report_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.report_query', 'report_uuid', new.report_uuid, 'query_uuid', new.query_uuid, r.ca_uuid from smart.report r where r.uuid = new.report_uuid;
CREATE TRIGGER trg_report_query_update AFTER UPDATE ON smart.report_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.report_query', 'report_uuid', new.report_uuid, 'query_uuid', new.query_uuid, r.ca_uuid from smart.report r where r.uuid = new.report_uuid;
CREATE TRIGGER trg_report_query_delete AFTER DELETE ON smart.report_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.report_query', 'report_uuid', old.report_uuid, 'query_uuid', old.query_uuid, r.ca_uuid from smart.report r where r.uuid = old.report_uuid;

--SMART.SAMPLING_UNIT


CREATE TRIGGER trg_sampling_unit_insert AFTER INSERT ON smart.sampling_unit REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.sampling_unit', 'uuid', new.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_sampling_unit_update AFTER UPDATE ON smart.sampling_unit REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.sampling_unit', 'uuid', new.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_sampling_unit_delete AFTER DELETE ON smart.sampling_unit REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.sampling_unit', 'uuid', old.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = old.survey_design_uuid;

--SMART.SAMPLING_UNIT_ATTRIBUTE


CREATE TRIGGER trg_sampling_unit_attribute_insert AFTER INSERT ON smart.sampling_unit_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.sampling_unit_attribute', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_sampling_unit_attribute_update AFTER UPDATE ON smart.sampling_unit_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.sampling_unit_attribute', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_sampling_unit_attribute_delete AFTER DELETE ON smart.sampling_unit_attribute REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.sampling_unit_attribute', 'uuid', old.uuid, old.ca_uuid);

--SMART.SAMPLING_UNIT_ATTRIBUTE_LIST


CREATE TRIGGER trg_sampling_unit_attribute_list_insert AFTER INSERT ON smart.sampling_unit_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.sampling_unit_attribute_list', 'uuid', new.uuid, sa.ca_uuid from smart.sampling_unit_attribute sa where sa.uuid = new.sampling_unit_attribute_uuid;
CREATE TRIGGER trg_sampling_unit_attribute_list_update AFTER UPDATE ON smart.sampling_unit_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.sampling_unit_attribute_list', 'uuid', new.uuid, sa.ca_uuid from smart.sampling_unit_attribute sa where sa.uuid = new.sampling_unit_attribute_uuid;
CREATE TRIGGER trg_sampling_unit_attribute_list_delete AFTER DELETE ON smart.sampling_unit_attribute_list REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.sampling_unit_attribute_list', 'uuid', old.uuid, sa.ca_uuid from smart.sampling_unit_attribute sa where sa.uuid = old.sampling_unit_attribute_uuid;

--SMART.SAMPLING_UNIT_ATTRIBUTE_VALUE


CREATE TRIGGER trg_sampling_unit_attribute_value_insert AFTER INSERT ON smart.sampling_unit_attribute_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.sampling_unit_attribute_value', 'su_attribute_uuid', new.su_attribute_uuid, 'su_uuid', new.su_uuid, sa.ca_uuid from smart.sampling_unit_attribute sa where sa.uuid = new.su_attribute_uuid;
CREATE TRIGGER trg_sampling_unit_attribute_value_update AFTER UPDATE ON smart.sampling_unit_attribute_value REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.sampling_unit_attribute_value', 'su_attribute_uuid', new.su_attribute_uuid, 'su_uuid', new.su_uuid, sa.ca_uuid from smart.sampling_unit_attribute sa where sa.uuid = new.su_attribute_uuid;
CREATE TRIGGER trg_sampling_unit_attribute_value_delete AFTER DELETE ON smart.sampling_unit_attribute_value REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.sampling_unit_attribute_value', 'su_attribute_uuid', old.su_attribute_uuid, 'su_uuid', old.su_uuid, sa.ca_uuid from smart.sampling_unit_attribute sa where sa.uuid = old.su_attribute_uuid;

--SMART.SAVED_MAPS


CREATE TRIGGER trg_saved_maps_insert AFTER INSERT ON smart.saved_maps REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.saved_maps', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_saved_maps_update AFTER UPDATE ON smart.saved_maps REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.saved_maps', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_saved_maps_delete AFTER DELETE ON smart.saved_maps REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.saved_maps', 'uuid', old.uuid, old.ca_uuid);

--SMART.SCREEN_OPTION


CREATE TRIGGER trg_screen_option_insert AFTER INSERT ON smart.screen_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.screen_option', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_screen_option_update AFTER UPDATE ON smart.screen_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.screen_option', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_screen_option_delete AFTER DELETE ON smart.screen_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.screen_option', 'uuid', old.uuid, old.ca_uuid);

--SMART.SCREEN_OPTION_UUID

CREATE TRIGGER trg_screen_option_uuid_insert AFTER INSERT ON smart.screen_option_uuid REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.screen_option_uuid', 'uuid', new.uuid, op.ca_uuid from smart.screen_option op where op.uuid = new.option_uuid;
CREATE TRIGGER trg_screen_option_uuid_update AFTER UPDATE ON smart.screen_option_uuid REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.screen_option_uuid', 'uuid', new.uuid, op.ca_uuid from smart.screen_option op where op.uuid = new.option_uuid;
CREATE TRIGGER trg_screen_option_uuid_delete AFTER DELETE ON smart.screen_option_uuid REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.screen_option_uuid', 'uuid', old.uuid, op.ca_uuid from smart.screen_option op where op.uuid = old.option_uuid;

--SMART.STATION


CREATE TRIGGER trg_station_insert AFTER INSERT ON smart.station REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.station', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_station_update AFTER UPDATE ON smart.station REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.station', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_station_delete AFTER DELETE ON smart.station REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.station', 'uuid', old.uuid, old.ca_uuid);

--SMART.SUMMARY_QUERY


CREATE TRIGGER trg_summary_query_insert AFTER INSERT ON smart.summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_summary_query_update AFTER UPDATE ON smart.summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_summary_query_delete AFTER DELETE ON smart.summary_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.summary_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.SURVEY


CREATE TRIGGER trg_survey_insert AFTER INSERT ON smart.survey REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.survey', 'uuid', new.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_survey_update AFTER UPDATE ON smart.survey REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.survey', 'uuid', new.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_survey_delete AFTER DELETE ON smart.survey REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.survey', 'uuid', old.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = old.survey_design_uuid;

--SMART.SURVEY_DESIGN


CREATE TRIGGER trg_survey_design_insert AFTER INSERT ON smart.survey_design REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.survey_design', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_design_update AFTER UPDATE ON smart.survey_design REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.survey_design', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_design_delete AFTER DELETE ON smart.survey_design REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.survey_design', 'uuid', old.uuid, old.ca_uuid);

--SMART.SURVEY_DESIGN_PROPERTY


CREATE TRIGGER trg_survey_design_property_insert AFTER INSERT ON smart.survey_design_property REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.survey_design_property', 'uuid', new.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_survey_design_property_update AFTER UPDATE ON smart.survey_design_property REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.survey_design_property', 'uuid', new.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_survey_design_property_delete AFTER DELETE ON smart.survey_design_property REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.survey_design_property', 'uuid', old.uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = old.survey_design_uuid;

--SMART.SURVEY_DESIGN_SAMPLING_UNIT


CREATE TRIGGER trg_survey_design_sampling_unit_insert AFTER INSERT ON smart.survey_design_sampling_unit REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.survey_design_sampling_unit', 'survey_design_uuid', new.survey_design_uuid, 'su_attribute_uuid', new.su_attribute_uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_survey_design_sampling_unit_update AFTER UPDATE ON smart.survey_design_sampling_unit REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.survey_design_sampling_unit', 'survey_design_uuid', new.survey_design_uuid, 'su_attribute_uuid', new.su_attribute_uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = new.survey_design_uuid;
CREATE TRIGGER trg_survey_design_sampling_unit_delete AFTER DELETE ON smart.survey_design_sampling_unit REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.survey_design_sampling_unit', 'survey_design_uuid', old.survey_design_uuid, 'su_attribute_uuid', old.su_attribute_uuid, sd.ca_uuid from smart.survey_design sd where sd.uuid = old.survey_design_uuid;

--SMART.SURVEY_GRIDDED_QUERY


CREATE TRIGGER trg_survey_gridded_query_insert AFTER INSERT ON smart.survey_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.survey_gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_gridded_query_update AFTER UPDATE ON smart.survey_gridded_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.survey_gridded_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_gridded_query_delete AFTER DELETE ON smart.survey_gridded_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.survey_gridded_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.SURVEY_MISSION_QUERY


CREATE TRIGGER trg_survey_mission_query_insert AFTER INSERT ON smart.survey_mission_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.survey_mission_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_mission_query_update AFTER UPDATE ON smart.survey_mission_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.survey_mission_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_mission_query_delete AFTER DELETE ON smart.survey_mission_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.survey_mission_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.SURVEY_MISSION_TRACK_QUERY


CREATE TRIGGER trg_survey_mission_track_query_insert AFTER INSERT ON smart.survey_mission_track_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.survey_mission_track_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_mission_track_query_update AFTER UPDATE ON smart.survey_mission_track_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.survey_mission_track_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_mission_track_query_delete AFTER DELETE ON smart.survey_mission_track_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.survey_mission_track_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.SURVEY_OBSERVATION_QUERY


CREATE TRIGGER trg_survey_observation_query_insert AFTER INSERT ON smart.survey_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.survey_observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_observation_query_update AFTER UPDATE ON smart.survey_observation_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.survey_observation_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_observation_query_delete AFTER DELETE ON smart.survey_observation_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.survey_observation_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.SURVEY_SUMMARY_QUERY


CREATE TRIGGER trg_survey_summary_query_insert AFTER INSERT ON smart.survey_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.survey_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_summary_query_update AFTER UPDATE ON smart.survey_summary_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.survey_summary_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_summary_query_delete AFTER DELETE ON smart.survey_summary_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.survey_summary_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.SURVEY_WAYPOINT


CREATE TRIGGER trg_survey_waypoint_insert AFTER INSERT ON smart.survey_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'INSERT', 'smart.survey_waypoint', 'wp_uuid', new.wp_uuid, 'mission_day_uuid', new.mission_day_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_survey_waypoint_update AFTER UPDATE ON smart.survey_waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.survey_waypoint', 'wp_uuid', new.wp_uuid, 'mission_day_uuid', new.mission_day_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_survey_waypoint_delete AFTER DELETE ON smart.survey_waypoint REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, ca_uuid) select smart.uuid(), 'DELETE', 'smart.survey_waypoint', 'wp_uuid', old.wp_uuid, 'mission_day_uuid', old.mission_day_uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = old.wp_uuid;

--SMART.SURVEY_WAYPOINT_QUERY


CREATE TRIGGER trg_survey_waypoint_query_insert AFTER INSERT ON smart.survey_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.survey_waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_waypoint_query_update AFTER UPDATE ON smart.survey_waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.survey_waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_survey_waypoint_query_delete AFTER DELETE ON smart.survey_waypoint_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.survey_waypoint_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.TEAM


CREATE TRIGGER trg_team_insert AFTER INSERT ON smart.team REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.team', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_team_update AFTER UPDATE ON smart.team REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.team', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_team_delete AFTER DELETE ON smart.team REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.team', 'uuid', old.uuid, old.ca_uuid);

--SMART.TRACK


CREATE TRIGGER trg_track_insert AFTER INSERT ON smart.track REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.track', 'uuid', new.uuid, p.ca_uuid from smart.patrol p, smart.patrol_leg pl, smart.patrol_leg_day pld where p.uuid = pl.patrol_uuid and pl.uuid = pld.patrol_leg_uuid and pld.uuid = new.patrol_leg_day_uuid;
CREATE TRIGGER trg_track_update AFTER UPDATE ON smart.track REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.track', 'uuid', new.uuid, p.ca_uuid from smart.patrol p, smart.patrol_leg pl, smart.patrol_leg_day pld where p.uuid = pl.patrol_uuid and pl.uuid = pld.patrol_leg_uuid and pld.uuid = new.patrol_leg_day_uuid;
CREATE TRIGGER trg_track_delete AFTER DELETE ON smart.track REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.track', 'uuid', old.uuid, p.ca_uuid from smart.patrol p, smart.patrol_leg pl, smart.patrol_leg_day pld where p.uuid = pl.patrol_uuid and pl.uuid = pld.patrol_leg_uuid and pld.uuid = old.patrol_leg_day_uuid;


--SMART.WAYPOINT

CREATE TRIGGER trg_waypoint_insert AFTER INSERT ON smart.waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.waypoint', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_waypoint_update AFTER UPDATE ON smart.waypoint REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.waypoint', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_waypoint_delete AFTER DELETE ON smart.waypoint REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.waypoint', 'uuid', old.uuid, old.ca_uuid);

--SMART.WAYPOINT_QUERY


CREATE TRIGGER trg_waypoint_query_insert AFTER INSERT ON smart.waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_waypoint_query_update AFTER UPDATE ON smart.waypoint_query REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.waypoint_query', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_waypoint_query_delete AFTER DELETE ON smart.waypoint_query REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.waypoint_query', 'uuid', old.uuid, old.ca_uuid);

--SMART.WP_ATTACHMENTS


CREATE TRIGGER trg_wp_attachments_insert AFTER INSERT ON smart.wp_attachments REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.wp_attachments', 'uuid', new.uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_wp_attachments_update AFTER UPDATE ON smart.wp_attachments REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.wp_attachments', 'uuid', new.uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_wp_attachments_delete AFTER DELETE ON smart.wp_attachments REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.wp_attachments', 'uuid', old.uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = old.wp_uuid;

--SMART.WP_OBSERVATION


CREATE TRIGGER trg_wp_observation_insert AFTER INSERT ON smart.wp_observation REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.wp_observation', 'uuid', new.uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_wp_observation_update AFTER UPDATE ON smart.wp_observation REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.wp_observation', 'uuid', new.uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = new.wp_uuid;
CREATE TRIGGER trg_wp_observation_delete AFTER DELETE ON smart.wp_observation REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.wp_observation', 'uuid', old.uuid, wp.ca_uuid from smart.waypoint wp where wp.uuid = old.wp_uuid;

--SMART.WP_OBSERVATION_ATTRIBUTES

CREATE TRIGGER trg_wp_observation_attributes_insert AFTER INSERT ON smart.wp_observation_attributes REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid,ca_uuid) select smart.uuid(), 'INSERT', 'smart.wp_observation_attributes', 'observation_uuid', new.observation_uuid, 'attribute_uuid', new.attribute_uuid, a.ca_uuid from smart.dm_attribute a where a.uuid = new.attribute_uuid;                    
CREATE TRIGGER trg_wp_observation_attributes_update AFTER UPDATE ON smart.wp_observation_attributes REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid,ca_uuid) select smart.uuid(), 'UPDATE', 'smart.wp_observation_attributes', 'observation_uuid', new.observation_uuid, 'attribute_uuid', new.attribute_uuid, a.ca_uuid from smart.dm_attribute a where a.uuid = new.attribute_uuid;                    
CREATE TRIGGER trg_wp_observation_attributes_delete AFTER DELETE ON smart.wp_observation_attributes REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid,ca_uuid) select smart.uuid(), 'DELETE', 'smart.wp_observation_attributes', 'observation_uuid', old.observation_uuid, 'attribute_uuid', old.attribute_uuid, a.ca_uuid from smart.dm_attribute a where a.uuid = old.attribute_uuid;                    



--SMART.CM_ATTRIBUTE


CREATE TRIGGER trg_cm_attribute_insert AFTER INSERT ON smart.cm_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.cm_attribute', 'uuid', new.uuid, a.ca_uuid FROM smart.dm_attribute a WHERE a.uuid = new.attribute_uuid;
CREATE TRIGGER trg_cm_attribute_update AFTER UPDATE ON smart.cm_attribute REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.cm_attribute', 'uuid', new.uuid, a.ca_uuid FROM smart.dm_attribute a WHERE a.uuid = new.attribute_uuid;
CREATE TRIGGER trg_cm_attribute_delete AFTER DELETE ON smart.cm_attribute REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.cm_attribute', 'uuid', old.uuid, a.ca_uuid FROM smart.dm_attribute a WHERE a.uuid = old.attribute_uuid;

--SMART.CM_ATTRIBUTE_LIST


CREATE TRIGGER trg_cm_attribute_list_insert AFTER INSERT ON smart.cm_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.cm_attribute_list', 'uuid', new.uuid, a.ca_uuid FROM smart.dm_attribute a WHERE a.uuid = new.dm_attribute_uuid;
CREATE TRIGGER trg_cm_attribute_list_update AFTER UPDATE ON smart.cm_attribute_list REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.cm_attribute_list', 'uuid', new.uuid, a.ca_uuid FROM smart.dm_attribute a WHERE a.uuid = new.dm_attribute_uuid;
CREATE TRIGGER trg_cm_attribute_list_delete AFTER DELETE ON smart.cm_attribute_list REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.cm_attribute_list', 'uuid', old.uuid, a.ca_uuid FROM smart.dm_attribute a WHERE a.uuid = old.dm_attribute_uuid;

--SMART.CM_ATTRIBUTE_OPTION


CREATE TRIGGER trg_cm_attribute_option_insert AFTER INSERT ON smart.cm_attribute_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.cm_attribute_option', 'uuid', new.uuid, dm.ca_uuid FROM smart.cm_attribute cm, smart.dm_attribute dm where cm.attribute_uuid = dm.uuid and cm.uuid = new.cm_attribute_uuid;
CREATE TRIGGER trg_cm_attribute_option_update AFTER UPDATE ON smart.cm_attribute_option REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.cm_attribute_option', 'uuid', new.uuid, dm.ca_uuid FROM smart.cm_attribute cm, smart.dm_attribute dm where cm.attribute_uuid = dm.uuid and cm.uuid = new.cm_attribute_uuid;
CREATE TRIGGER trg_cm_attribute_option_delete AFTER DELETE ON smart.cm_attribute_option REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.cm_attribute_option', 'uuid', old.uuid, dm.ca_uuid FROM smart.cm_attribute cm, smart.dm_attribute dm where cm.attribute_uuid = dm.uuid and cm.uuid = old.cm_attribute_uuid;


--SMART.CM_ATTRIBUTE_TREE_NODE


CREATE TRIGGER trg_cm_attribute_tree_node_insert AFTER INSERT ON smart.cm_attribute_tree_node REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.cm_attribute_tree_node', 'uuid', new.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = new.cm_uuid;
CREATE TRIGGER trg_cm_attribute_tree_node_update AFTER UPDATE ON smart.cm_attribute_tree_node REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.cm_attribute_tree_node', 'uuid', new.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = new.cm_uuid;
CREATE TRIGGER trg_cm_attribute_tree_node_delete AFTER DELETE ON smart.cm_attribute_tree_node REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.cm_attribute_tree_node', 'uuid', old.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = old.cm_uuid;

--SMART.CM_NODE


CREATE TRIGGER trg_cm_node_insert AFTER INSERT ON smart.cm_node REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'INSERT', 'smart.cm_node', 'uuid', new.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = new.cm_uuid;
CREATE TRIGGER trg_cm_node_update AFTER UPDATE ON smart.cm_node REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'UPDATE', 'smart.cm_node', 'uuid', new.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = new.cm_uuid;
CREATE TRIGGER trg_cm_node_delete AFTER DELETE ON smart.cm_node REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) select smart.uuid(), 'DELETE', 'smart.cm_node', 'uuid', old.uuid, cm.ca_uuid FROM smart.configurable_model cm where cm.uuid = old.cm_uuid;

--SMART.CONFIGURABLE_MODEL


CREATE TRIGGER trg_configurable_model_insert AFTER INSERT ON smart.configurable_model REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'INSERT', 'smart.configurable_model', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_configurable_model_update AFTER UPDATE ON smart.configurable_model REFERENCING NEW AS new FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'UPDATE', 'smart.configurable_model', 'uuid', new.uuid, new.ca_uuid);
CREATE TRIGGER trg_configurable_model_delete AFTER DELETE ON smart.configurable_model REFERENCING OLD AS old FOR EACH ROW WHEN (syscs_util.syscs_get_database_property( 'org.wcs.smart.isLogging' ) = true) INSERT INTO smart.connect_change_log (uuid, action, tablename, key1_fieldname, key1, ca_uuid) values (smart.uuid(), 'DELETE', 'smart.configurable_model', 'uuid', old.uuid, old.ca_uuid);

