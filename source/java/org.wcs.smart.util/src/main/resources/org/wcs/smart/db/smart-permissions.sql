

GRANT ALL PRIVILEGES ON smart.conservation_area TO login;
GRANT ALL PRIVILEGES ON smart.employee TO login;
GRANT ALL PRIVILEGES ON smart.language TO login;
GRANT ALL PRIVILEGES  ON smart.i18n_label TO login;
GRANT ALL PRIVILEGES  ON smart.station TO login;
GRANT ALL PRIVILEGES  ON smart.ca_projection TO login;
GRANT USAGE ON SEQUENCE smart.smart_user_id_seq TO login;

GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE TO login;
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_FREEZE_DATABASE TO login;
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE TO data_entry;
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_FREEZE_DATABASE TO data_entry;
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE TO analyst;
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_FREEZE_DATABASE TO analyst;
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE TO manager;
GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_FREEZE_DATABASE TO manager;

GRANT SELECT ON smart.conservation_area TO manager;
GRANT SELECT ON smart.conservation_area TO analyst;
GRANT SELECT ON smart.conservation_area TO data_entry;

GRANT SELECT ON smart.employee TO manager;
GRANT SELECT ON smart.employee TO analyst;
GRANT SELECT ON smart.employee TO data_entry;

GRANT UPDATE ON smart.employee TO manager;
GRANT UPDATE ON smart.employee TO analyst;
GRANT UPDATE ON smart.employee TO data_entry;

GRANT SELECT ON smart.language TO manager;
GRANT SELECT ON smart.language TO analyst;
GRANT SELECT ON smart.language TO data_entry;

GRANT ALL PRIVILEGES ON smart.i18n_label TO manager;
GRANT ALL PRIVILEGES ON smart.i18n_label TO analyst;
GRANT SELECT ON smart.i18n_label TO data_entry;

GRANT SELECT ON smart.station TO manager;
GRANT SELECT ON smart.station TO analyst;
GRANT SELECT ON smart.station TO data_entry;

GRANT SELECT ON smart.saved_maps TO manager;
GRANT SELECT ON smart.saved_maps TO analyst;
GRANT SELECT ON smart.saved_maps TO data_entry;

GRANT SELECT ON smart.ca_projection TO manager;
GRANT SELECT ON smart.ca_projection TO analyst;
GRANT SELECT ON smart.ca_projection TO data_entry;

--data_entry
GRANT SELECT ON smart.dm_category TO data_entry;
GRANT SELECT ON smart.dm_cat_att_map TO data_entry;
GRANT SELECT ON smart.dm_attribute TO data_entry;
GRANT SELECT ON smart.dm_att_agg_map TO data_entry;
GRANT SELECT ON smart.dm_aggregation TO data_entry;
GRANT SELECT ON smart.dm_attribute_list TO data_entry;
GRANT SELECT ON smart.dm_attribute_tree TO data_entry;

--manager
GRANT SELECT ON smart.dm_category TO manager;
GRANT SELECT ON smart.dm_cat_att_map TO manager;
GRANT SELECT ON smart.dm_attribute TO manager;
GRANT SELECT ON smart.dm_att_agg_map TO manager;
GRANT SELECT ON smart.dm_aggregation TO manager;
GRANT SELECT ON smart.dm_attribute_list TO manager;
GRANT SELECT ON smart.dm_attribute_tree TO manager;

--analyst
GRANT SELECT ON smart.dm_category TO analyst;
GRANT SELECT ON smart.dm_cat_att_map TO analyst;
GRANT SELECT ON smart.dm_attribute TO analyst;
GRANT SELECT ON smart.dm_att_agg_map TO analyst;
GRANT SELECT ON smart.dm_aggregation TO analyst;
GRANT SELECT ON smart.dm_attribute_list TO analyst;
GRANT SELECT ON smart.dm_attribute_tree TO analyst;

GRANT SELECT ON smart.AREA_GEOMETRIES to analyst;
GRANT SELECT ON smart.AREA_GEOMETRIES to manager;
GRANT SELECT ON smart.AREA_GEOMETRIES to data_entry;


-- PATROL
GRANT SELECT ON smart.patrol_mandate to analyst;
GRANT SELECT ON smart.patrol_mandate to manager;
GRANT SELECT ON smart.patrol_mandate to data_entry;

GRANT SELECT ON smart.patrol_transport to analyst;
GRANT SELECT ON smart.patrol_transport to manager;
GRANT SELECT ON smart.patrol_transport to data_entry;

GRANT SELECT ON smart.patrol_type to analyst;
GRANT SELECT ON smart.patrol_type to manager;
GRANT SELECT ON smart.patrol_type to data_entry;

GRANT SELECT ON smart.team to analyst;
GRANT SELECT ON smart.team to manager;
GRANT SELECT ON smart.team to data_entry;

GRANT SELECT ON smart.patrol_options to analyst;
GRANT SELECT ON smart.patrol_options to manager;
GRANT SELECT ON smart.patrol_options to data_entry;


GRANT ALL PRIVILEGES  ON  smart.track TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.track TO manager;
GRANT SELECT ON  smart.track TO analyst;

GRANT ALL PRIVILEGES  ON  smart.wp_attachments TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.wp_attachments TO manager;
GRANT SELECT ON  smart.wp_attachments TO analyst;

GRANT ALL PRIVILEGES  ON  smart.WP_OBSERVATION_ATTRIBUTES TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.WP_OBSERVATION_ATTRIBUTES TO manager;
GRANT SELECT ON  smart.WP_OBSERVATION_ATTRIBUTES TO analyst;

GRANT ALL PRIVILEGES  ON  smart.WP_OBSERVATION TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.WP_OBSERVATION TO manager;
GRANT SELECT ON  smart.WP_OBSERVATION TO analyst;


GRANT ALL PRIVILEGES  ON  smart.WAYPOINT TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.WAYPOINT TO manager;
GRANT SELECT ON  smart.WAYPOINT TO analyst;

GRANT ALL PRIVILEGES  ON  smart.PATROL_LEG_DAY TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.PATROL_LEG_DAY TO manager;
GRANT SELECT ON  smart.PATROL_LEG_DAY TO analyst;

GRANT ALL PRIVILEGES  ON  smart.PATROL_LEG_MEMBERS TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.PATROL_LEG_MEMBERS TO manager;
GRANT SELECT ON  smart.PATROL_LEG_MEMBERS TO analyst;

GRANT ALL PRIVILEGES  ON  smart.PATROL_LEG TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.PATROL_LEG TO manager;
GRANT SELECT ON  smart.PATROL_LEG TO analyst;

GRANT ALL PRIVILEGES  ON  smart.PATROL TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.PATROL TO manager;
GRANT SELECT ON  smart.PATROL TO analyst;

GRANT ALL PRIVILEGES  ON  smart.TRACK TO data_entry;
GRANT ALL PRIVILEGES  ON  smart.TRACK TO manager;
GRANT SELECT ON  smart.TRACK TO analyst;


-- QUERY
GRANT ALL PRIVILEGES ON smart.QUERY_FOLDER to manager;
GRANT ALL PRIVILEGES  ON smart.WAYPOINT_QUERY to manager;
GRANT ALL PRIVILEGES  ON smart.PATROL_QUERY to manager;
GRANT ALL PRIVILEGES  ON smart.SUMMARY_QUERY to manager;
GRANT ALL PRIVILEGES  ON smart.GRIDDED_QUERY to manager;
GRANT ALL PRIVILEGES ON smart.QUERY_FOLDER to analyst;
GRANT ALL PRIVILEGES  ON smart.WAYPOINT_QUERY to analyst;
GRANT ALL PRIVILEGES  ON smart.PATROL_QUERY to analyst;
GRANT ALL PRIVILEGES  ON smart.SUMMARY_QUERY to analyst;
GRANT ALL PRIVILEGES  ON smart.GRIDDED_QUERY to analyst;



-- REPORT
GRANT ALL PRIVILEGES ON smart.REPORT_FOLDER to manager;
GRANT ALL PRIVILEGES  ON smart.REPORT to manager;
GRANT ALL PRIVILEGES ON smart.REPORT_QUERY to manager;
GRANT ALL PRIVILEGES ON smart.REPORT_FOLDER to analyst;
GRANT ALL PRIVILEGES  ON smart.REPORT to analyst;
GRANT ALL PRIVILEGES ON smart.REPORT_QUERY to analyst;


-- INTELLIGENCE
GRANT ALL PRIVILEGES ON smart.intelligence to data_entry;
GRANT ALL PRIVILEGES ON smart.intelligence_point to data_entry;
GRANT ALL PRIVILEGES ON smart.intelligence_attachment to data_entry;
GRANT ALL PRIVILEGES ON smart.patrol_intelligence to data_entry;

GRANT ALL PRIVILEGES ON smart.intelligence to manager;
GRANT ALL PRIVILEGES ON smart.intelligence_point to manager;
GRANT ALL PRIVILEGES ON smart.intelligence_attachment to manager;
GRANT ALL PRIVILEGES ON smart.patrol_intelligence to manager;

GRANT SELECT ON smart.intelligence to analyst;
GRANT SELECT ON smart.intelligence_point to analyst;
GRANT SELECT ON smart.intelligence_attachment to analyst;
GRANT SELECT ON smart.patrol_intelligence to analyst;