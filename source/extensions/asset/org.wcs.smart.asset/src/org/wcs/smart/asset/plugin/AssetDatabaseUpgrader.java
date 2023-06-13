/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.asset.plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Asset upgrade operations while upgrade/restore backup.
 * 
 * @author Emily
 * @since 3.0.0
 */
public class AssetDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return AssetPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(AssetPlugIn.getDefault().getBundle());
	}
	
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(AssetPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(AssetPlugIn.PLUGIN_ID).equals(AssetPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.AssetDatabaseUpgrader_TaskName);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(AssetPlugIn.PLUGIN_ID), session);
				
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}

	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session is active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			upgradeV1toV2(session);
		}else if (currentVersion.equals(AssetPlugIn.DB_VERSION_1)) {
			upgradeV1toV2(session);
		}
	}
	
	private void upgradeV1toV2(Session session) {
		String[] sql = new String[]{
				//primary date field
				"create table smart.asset_deployment_disruption(uuid char(16) for bit data not null, asset_deployment_uuid char(16) for bit data not null, start_date timestamp not null, end_date timestamp not null, comment varchar(32672), primary key (uuid))", //$NON-NLS-1$
				
				"alter table smart.asset_deployment_disruption add constraint assdep_uuid_fk foreign key (asset_deployment_uuid) REFERENCES smart.asset_deployment (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"GRANT SELECT ON smart.asset_deployment_disruption TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.asset_deployment_disruption TO MANAGER", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.asset_deployment_disruption TO DATA_ENTRY", //$NON-NLS-1$
				
				"ALTER TABLE smart.asset_metadata_mapping ADD COLUMN state varchar(10)", //$NON-NLS-1$
				"UPDATE smart.asset_metadata_mapping SET state = 'ENABLED'", //$NON-NLS-1$
				"ALTER TABLE smart.asset_metadata_mapping ALTER COLUMN state set not null", //$NON-NLS-1$
				
				//add buffers to station/location objects
				"ALTER TABLE smart.asset_station add column buffer double precision", //$NON-NLS-1$
				"ALTER TABLE smart.asset_station_location add column buffer double precision", //$NON-NLS-1$
				
				"CREATE FUNCTION smart.to_double( stringvalue varchar(32000) ) RETURNS double precision PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA EXTERNAL NAME 'org.wcs.smart.asset.plugin.AssetDatabaseUpgrader.toDouble'", //$NON-NLS-1$

				"UPDATE smart.asset_station set buffer = (select smart.to_double(c.value) from smart.asset_module_settings c where c.keyid = 'station_buffer' and c.ca_uuid = smart.asset_station.ca_uuid)", //$NON-NLS-1$
				"UPDATE smart.asset_station set buffer = " + AssetModuleSettings.STATION_BUFFER_DEFAULT_VALUE + " where buffer is null or buffer < 0", //$NON-NLS-1$ //$NON-NLS-2$
				
				"UPDATE smart.asset_station_location set buffer = (select smart.to_double(c.value) from smart.asset_module_settings c, smart.ASSET_STATION d where c.ca_uuid = d.ca_uuid and c.keyid = 'location_buffer' and d.uuid = smart.asset_station_location.station_uuid)", //$NON-NLS-1$
				"UPDATE smart.asset_station_location set buffer = " + AssetModuleSettings.LOCATION_BUFFER_DEFAULT_VALUE + " where buffer is null or buffer < 0", //$NON-NLS-1$ //$NON-NLS-2$
				
				"DROP FUNCTION smart.to_double", //$NON-NLS-1$
				
				"ALTER TABLE smart.asset_station alter column buffer set not null",  //$NON-NLS-1$
				"ALTER TABLE smart.asset_station_location alter column buffer set not null", //$NON-NLS-1$

				"ALTER TABLE smart.asset_attribute_value alter column string_value set data type varchar(8200)",  //$NON-NLS-1$
				"ALTER TABLE smart.asset_deployment_attribute_value alter column string_value set data type varchar(8200)",  //$NON-NLS-1$
				"ALTER TABLE smart.asset_station_attribute_value alter column string_value set data type varchar(8200)",  //$NON-NLS-1$
				"ALTER TABLE smart.asset_deployment_attribute_value alter column string_value set data type varchar(8200)",  //$NON-NLS-1$
		};
		
		for (String s : sql){
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(AssetPlugIn.PLUGIN_ID, AssetPlugIn.DB_VERSION_2, session);

	}
	
	public static Double toDouble(String value) {
		try {
			return Double.valueOf(value);
		}catch (Exception ex) {
			return -1.0;
		}
	}
	
	
	@SuppressWarnings("nls")
	private void createTables(Session session){
		String[] sql = new String[]{
				//Tables
				"CREATE TABLE smart.asset( uuid char(16) for bit data NOT NULL, asset_type_uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, id varchar(128) NOT NULL, is_retired boolean DEFAULT false NOT NULL, PRIMARY KEY (uuid))",
				"alter table smart.asset add constraint id_ca_uuid_unq UNIQUE(id, ca_uuid)",
				"CREATE TABLE smart.asset_attribute ( uuid char(16) for bit data NOT NULL, keyId varchar(128) NOT NULL, type char(8) NOT NULL, ca_uuid char(16) for bit data NOT NULL, PRIMARY KEY (uuid) )",
				"alter table smart.asset_attribute add constraint keyid_ca_uuid_unq UNIQUE(keyId, ca_uuid)",
				"CREATE TABLE smart.asset_attribute_list_item ( uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, keyid varchar(128) NOT NULL, PRIMARY KEY (uuid) )",
				"alter table smart.asset_attribute_list_item add constraint asset_li_keyid_attribute_uuid_unq UNIQUE(keyId, attribute_uuid)",
				"CREATE TABLE smart.asset_attribute_value ( asset_uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, string_value varchar(1024), list_item_uuid char(16) for bit data, double_value1 double, double_value2 double, PRIMARY KEY (asset_uuid, attribute_uuid) )",
				"CREATE TABLE smart.asset_deployment ( uuid char(16) for bit data NOT NULL, asset_uuid char(16) for bit data NOT NULL, station_location_uuid char(16) for bit data NOT NULL, start_date timestamp NOT NULL, end_date timestamp, track blob, PRIMARY KEY (uuid))",
				"CREATE TABLE smart.asset_deployment_attribute_value ( asset_deployment_uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, string_value varchar(1024), list_item_uuid char(16) for bit data, double_value1 double, double_value2 double, PRIMARY KEY (asset_deployment_uuid, attribute_uuid) )",
				"CREATE TABLE smart.asset_history_record ( uuid char(16) for bit data NOT NULL, asset_uuid char(16) for bit data NOT NULL, date timestamp NOT NULL, comment LONG VARCHAR, PRIMARY KEY (uuid) )",
				"CREATE TABLE smart.asset_module_settings ( uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, keyid varchar(128), value varchar(32000), PRIMARY KEY (uuid) )",
				"alter table smart.asset_module_settings add constraint asset_module_key_ca_unq UNIQUE(keyid, ca_uuid)",
				"CREATE TABLE smart.asset_station ( uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, id varchar(128) NOT NULL, x double NOT NULL, y double NOT NULL, PRIMARY KEY (uuid) )",
				"alter table smart.asset_station add constraint asset_sn_id_ca_unq UNIQUE(id, ca_uuid)",
				"CREATE TABLE smart.asset_station_attribute ( attribute_uuid char(16) for bit data NOT NULL, seq_order integer NOT NULL, PRIMARY KEY (attribute_uuid) )",
				"CREATE TABLE smart.asset_station_attribute_value ( station_uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, string_value varchar(1024), list_item_uuid char(16) for bit data, double_value1 double, double_value2 double, PRIMARY KEY (station_uuid, attribute_uuid) )",
				"CREATE TABLE smart.asset_type ( uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, keyid varchar(128), icon blob, incident_cutoff integer, PRIMARY KEY (uuid) )",
				"alter table smart.asset_type add constraint asset_type_ca_keyid_unq unique(keyid, ca_uuid)",
				"CREATE TABLE smart.asset_type_attribute ( asset_type_uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, seq_order integer NOT NULL, PRIMARY KEY (asset_type_uuid, attribute_uuid) )",
				"CREATE TABLE smart.asset_type_deployment_attribute ( asset_type_uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, seq_order integer NOT NULL, PRIMARY KEY (asset_type_uuid, attribute_uuid) )",
				"CREATE TABLE smart.asset_waypoint ( uuid char(16) for bit data not null, wp_uuid char(16) for bit data NOT NULL, asset_deployment_uuid char(16) for bit data NOT NULL, state smallint not null, incident_length integer not null, PRIMARY KEY (uuid), UNIQUE(wp_uuid, asset_deployment_uuid) )",
				"CREATE TABLE smart.asset_waypoint_attachment ( wp_attachment_uuid char(16) for bit data NOT NULL, asset_waypoint_uuid char(16) for bit data NOT NULL, PRIMARY KEY (wp_attachment_uuid, asset_waypoint_uuid) )",
				"CREATE TABLE smart.asset_metadata_mapping ( uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null,metadata_type varchar(16) not null, metadata_key varchar(32672) not null, search_order integer not null, asset_field varchar(32), category_uuid char(16) for bit data,attribute_uuid char(16) for bit data, attribute_list_item_uuid char(16) for bit data, attribute_tree_node_uuid char(16) for bit data,  PRIMARY KEY (uuid))",
				"CREATE TABLE smart.asset_station_location_history ( uuid char(16) for bit data NOT NULL, station_location_uuid char(16) for bit data NOT NULL, date timestamp NOT NULL, comment LONG VARCHAR, PRIMARY KEY (uuid) )",
				"CREATE TABLE smart.asset_station_location ( uuid char(16) for bit data NOT NULL, station_uuid char(16) for bit data NOT NULL, id varchar(128) NOT NULL, x double NOT NULL, y double NOT NULL, PRIMARY KEY (uuid) )",
				"alter table smart.asset_station_location add constraint asset_snlc_id_ca_unq UNIQUE(id, station_uuid)",
				"CREATE TABLE smart.asset_station_location_attribute ( attribute_uuid char(16) for bit data NOT NULL, seq_order integer NOT NULL, PRIMARY KEY (attribute_uuid) )",
				"CREATE TABLE smart.asset_station_location_attribute_value ( station_location_uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, string_value varchar(1024), list_item_uuid char(16) for bit data, double_value1 double, double_value2 double, PRIMARY KEY (station_location_uuid, attribute_uuid) )",
				"CREATE TABLE smart.asset_map_style ( uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, name varchar(1024), style_string varchar(32672), PRIMARY KEY (uuid) )",
				
				// Create Foreign Keys
				"ALTER TABLE smart.asset_station ADD CONSTRAINT assetstation_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_location_history ADD CONSTRAINT assetstnlochistory_stnloc_fk FOREIGN KEY (station_location_uuid) REFERENCES smart.asset_station_location(uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_location ADD CONSTRAINT assetstnloc_stn_fk FOREIGN KEY (station_uuid) REFERENCES smart.asset_station(uuid) DEFERRABLE INITIALLY IMMEDIATE",			
				"ALTER TABLE smart.asset_station_location_attribute_value ADD CONSTRAINT sntlocattvalu_stnloc_fk FOREIGN KEY (station_location_uuid) REFERENCES smart.asset_station_location(uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_location_attribute_value ADD CONSTRAINT sntlocattvalu_att_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute(uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_attribute_value ADD CONSTRAINT assetattvalu_asset_fk FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_attribute_value ADD CONSTRAINT asset_att_value_assetuuid_fk FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_deployment ADD CONSTRAINT asset_deploy_assetuuid_fk FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_history_record ADD CONSTRAINT asset_historyrecord_assetuuid_fk FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_attribute_list_item ADD CONSTRAINT asset_attlistitem_attributeuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_attribute_value ADD CONSTRAINT asset_attvalue_attuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_deployment_attribute_value ADD CONSTRAINT asset_dplattvalue_attuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_attribute ADD CONSTRAINT asset_stn_attuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_location_attribute ADD CONSTRAINT asset_stn_loc_attuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_attribute_value ADD CONSTRAINT asset_stn_attvalue_attuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_type_attribute ADD CONSTRAINT assettypeatt_attuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_type_deployment_attribute ADD CONSTRAINT assettypedpl_attuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.asset_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_attribute_value ADD CONSTRAINT assetattvalu_listitem_fk FOREIGN KEY (list_item_uuid) REFERENCES smart.asset_attribute_list_item (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_deployment_attribute_value ADD CONSTRAINT assetdplvalue_listitem_fk FOREIGN KEY (list_item_uuid) REFERENCES smart.asset_attribute_list_item (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_attribute_value ADD CONSTRAINT stnattvalue_listitem_fk FOREIGN KEY (list_item_uuid) REFERENCES smart.asset_attribute_list_item (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_deployment_attribute_value ADD CONSTRAINT assetdplvalue_depl_fk FOREIGN KEY (asset_deployment_uuid) REFERENCES smart.asset_deployment (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_waypoint ADD CONSTRAINT assetwp_dpl_fk FOREIGN KEY (asset_deployment_uuid) REFERENCES smart.asset_deployment (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_deployment ADD CONSTRAINT assetdepl_stnlocuuid_fk FOREIGN KEY (station_location_uuid) REFERENCES smart.asset_station_location (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_station_attribute_value ADD CONSTRAINT assetstnatt_stnuuid_fk FOREIGN KEY (station_uuid) REFERENCES smart.asset_station (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset ADD CONSTRAINT asset_typeuuid_fk FOREIGN KEY (asset_type_uuid) REFERENCES smart.asset_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_type_attribute ADD CONSTRAINT assettypeatt_typeuuid_fk FOREIGN KEY (asset_type_uuid) REFERENCES smart.asset_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_type_deployment_attribute ADD CONSTRAINT assetypdep_att_typeuuid_fk FOREIGN KEY (asset_type_uuid) REFERENCES smart.asset_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset ADD CONSTRAINT asset_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_attribute ADD CONSTRAINT assetatt_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_module_settings ADD CONSTRAINT assetmodset_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_type ADD CONSTRAINT assettype_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_waypoint ADD CONSTRAINT wp_asset_wpuuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_deployment ADD CONSTRAINT assetdepl_assetuuid_fk FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_metadata_mapping ADD CONSTRAINT assetmapping_type_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_metadata_mapping ADD CONSTRAINT assetmapping_type_categoryuuid_fk FOREIGN KEY (category_uuid) REFERENCES smart.dm_category (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_metadata_mapping ADD CONSTRAINT assetmapping_type_attributeuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.dm_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_metadata_mapping ADD CONSTRAINT assetmapping_type_attlistitemuuid_fk FOREIGN KEY (attribute_list_item_uuid) REFERENCES smart.dm_attribute_list (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_metadata_mapping ADD CONSTRAINT assetmapping_type_atttreenodeuuid_fk FOREIGN KEY (attribute_tree_node_uuid) REFERENCES smart.dm_attribute_tree (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				
				"ALTER TABLE smart.asset_waypoint_attachment ADD CONSTRAINT assetwpuuid_dpl_fk FOREIGN KEY (asset_waypoint_uuid) REFERENCES smart.asset_waypoint (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_waypoint_attachment ADD CONSTRAINT wpatt_asset_wpuuid_fk FOREIGN KEY (wp_attachment_uuid) REFERENCES smart.wp_attachments (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				
				"ALTER TABLE smart.asset_map_style ADD CONSTRAINT assetmapstyle_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",

				//permissions
				"GRANT SELECT ON smart.asset TO ANALYST",	 
				"GRANT SELECT ON smart.asset_attribute TO ANALYST",
				"GRANT SELECT ON smart.asset_attribute_list_item TO ANALYST",
				"GRANT SELECT ON smart.asset_attribute_value TO ANALYST",
				"GRANT SELECT ON smart.asset_deployment TO ANALYST",
				"GRANT SELECT ON smart.asset_deployment_attribute_value TO ANALYST",
				"GRANT SELECT ON smart.asset_history_record TO ANALYST",
				"GRANT SELECT ON smart.asset_module_settings TO ANALYST",
				"GRANT SELECT ON smart.asset_station TO ANALYST",
				"GRANT SELECT ON smart.asset_station_attribute TO ANALYST",
				"GRANT SELECT ON smart.asset_station_attribute_value TO ANALYST",
				"GRANT SELECT ON smart.asset_type TO ANALYST",
				"GRANT SELECT ON smart.asset_type_attribute TO ANALYST",
				"GRANT SELECT ON smart.asset_type_deployment_attribute TO ANALYST",
				"GRANT SELECT ON smart.asset_waypoint TO ANALYST",
				"GRANT SELECT ON smart.asset_waypoint_attachment TO ANALYST",
				"GRANT SELECT ON smart.asset_metadata_mapping TO ANALYST",
				"GRANT SELECT ON smart.asset_station_location_history TO ANALYST",
				"GRANT SELECT ON smart.asset_station_location TO ANALYST",
				"GRANT SELECT ON smart.asset_station_location_attribute TO ANALYST",
				"GRANT SELECT ON smart.asset_station_location_attribute_value TO ANALYST",
				"GRANT SELECT ON smart.asset_map_style TO ANALYST",
				
				"GRANT ALL PRIVILEGES ON smart.asset TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_attribute TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_attribute_list_item TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_attribute_value TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_deployment TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_deployment_attribute_value TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_history_record TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_module_settings TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_station TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_station_attribute TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_station_attribute_value TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_type TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_type_attribute TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_type_deployment_attribute TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_waypoint TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_waypoint_attachment TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_metadata_mapping TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_station_location_history TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_station_location TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_station_location_attribute TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_station_location_attribute_value TO MANAGER",
				"GRANT ALL PRIVILEGES ON smart.asset_map_style TO MANAGER",
				
				"GRANT ALL PRIVILEGES ON smart.asset TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_attribute TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_attribute_list_item TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_attribute_value TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_deployment TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_deployment_attribute_value TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_history_record TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_module_settings TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_station TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_station_attribute TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_station_attribute_value TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_type TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_type_attribute TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_type_deployment_attribute TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_waypoint TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_waypoint_attachment TO DATA_ENTRY",
				"GRANT SELECT ON smart.asset_metadata_mapping TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_station_location_history TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES  ON smart.asset_station_location TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_station_location_attribute TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_station_location_attribute_value TO DATA_ENTRY",
				"GRANT ALL PRIVILEGES ON smart.asset_map_style TO DATA_ENTRY",
				
		};
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					AssetPlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}
			}
			
		});
		HibernateManager.setPlugInVersion(AssetPlugIn.PLUGIN_ID, AssetPlugIn.DB_VERSION_1, session);
	}
}
