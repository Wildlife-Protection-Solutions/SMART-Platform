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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Adds and or upgrades asset plugin
 * 
 * @author Emily
 *
 */
public class AddAssetJob extends Job {

	public AddAssetJob() {
		super("Install/update asset plugin");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				installPlugin(session);
				session.getTransaction().commit();
			} catch (final Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						AssetPlugIn.displayLog("Error installing plugin: " + ex.getMessage(), ex);
					}
				});
				return new Status(IStatus.ERROR, AssetPlugIn.PLUGIN_ID, 1, "Error installing plugin: " + ex.getMessage(), ex);
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	public void installPlugin(Session session){
		String currentVersion = HibernateManager.getPlugInVersion(AssetPlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			createTables(session);
			currentVersion = AssetPlugIn.DB_VERSION_1;
		}
		
		AssetDatabaseUpgrader.upgrade(AssetPlugIn.DB_VERSION_1, session);
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
				"CREATE TABLE smart.asset_waypoint ( wp_uuid char(16) for bit data NOT NULL, asset_deployment_uuid char(16) for bit data NOT NULL, PRIMARY KEY (wp_uuid, asset_deployment_uuid) )",
				"CREATE TABLE smart.asset_exif_metadata_mapping ( uuid char(16) for bit data not null, asset_type_uuid char(16) for bit data NOT NULL, metadata_tag varchar(512), metadata_value varchar(512), metadata_ns varchar(512), asset_field varchar(32), category_uuid char(16) for bit data, attribute_uuid char(16) for bit data, attribute_list_item_uuid char(16) for bit data, attribute_tree_node_uuid char(16) for bit data, PRIMARY KEY (uuid))",
				"CREATE TABLE smart.asset_station_location_history ( uuid char(16) for bit data NOT NULL, station_location_uuid char(16) for bit data NOT NULL, date timestamp NOT NULL, comment LONG VARCHAR, PRIMARY KEY (uuid) )",
				"CREATE TABLE smart.asset_station_location ( uuid char(16) for bit data NOT NULL, station_uuid char(16) for bit data NOT NULL, id varchar(128) NOT NULL, x double NOT NULL, y double NOT NULL, PRIMARY KEY (uuid) )",
				"alter table smart.asset_station_location add constraint asset_snlc_id_ca_unq UNIQUE(id, station_uuid)",
				"CREATE TABLE smart.asset_station_location_attribute ( attribute_uuid char(16) for bit data NOT NULL, seq_order integer NOT NULL, PRIMARY KEY (attribute_uuid) )",
				"CREATE TABLE smart.asset_station_location_attribute_value ( station_location_uuid char(16) for bit data NOT NULL, attribute_uuid char(16) for bit data NOT NULL, string_value varchar(1024), list_item_uuid char(16) for bit data, double_value1 double, double_value2 double, PRIMARY KEY (station_location_uuid, attribute_uuid) )",
				
				// Create Foreign Keys
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
				"ALTER TABLE smart.asset_exif_metadata_mapping ADD CONSTRAINT assetexif_type_uuid_fk FOREIGN KEY (asset_type_uuid) REFERENCES smart.asset_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_waypoint ADD CONSTRAINT wp_asset_wpuuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_deployment ADD CONSTRAINT assetdepl_assetuuid_fk FOREIGN KEY (asset_uuid) REFERENCES smart.asset (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_exif_metadata_mapping ADD CONSTRAINT assetexif_type_categoryuuid_fk FOREIGN KEY (category_uuid) REFERENCES smart.dm_category (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_exif_metadata_mapping ADD CONSTRAINT assetexif_type_attributeuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.dm_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_exif_metadata_mapping ADD CONSTRAINT assetexif_type_attlistitemuuid_fk FOREIGN KEY (attribute_list_item_uuid) REFERENCES smart.dm_attribute_list (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.asset_exif_metadata_mapping ADD CONSTRAINT assetexif_type_atttreenodeuuid_fk FOREIGN KEY (attribute_tree_node_uuid) REFERENCES smart.dm_attribute_tree (uuid) DEFERRABLE INITIALLY IMMEDIATE",
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
