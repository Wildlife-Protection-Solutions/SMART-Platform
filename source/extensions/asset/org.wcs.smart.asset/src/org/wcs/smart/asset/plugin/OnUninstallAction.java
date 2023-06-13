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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.p2.common.updatesite.UninstallProvisioningAction;
import org.wcs.smart.util.SmartUtils;

/**
 * Action that is called when asset plug-in is uninstalled
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class OnUninstallAction extends UninstallProvisioningAction {

	@Override
	protected String getPluginId() {
		return AssetPlugIn.PLUGIN_ID;
	}
	
	
	@Override
	protected void performRemove() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				uninstall(session);
				session.getTransaction().commit();
			} catch (Exception e) {
				try{
					session.getTransaction().rollback();
				}catch (Exception ex){
					AssetPlugIn.log(ex.getMessage(), ex);	
				}
				AssetPlugIn.displayLog(Messages.RemoveAssetJob_Error + e.getMessage(), e);
			}
		}	
	}

	
	private void uninstall(Session session){
		
		@SuppressWarnings("nls")
		String[] TABLES = new String[]{
				"asset_attribute_value",
				"asset_deployment_attribute_value",
				"asset_station_attribute_value",
				"asset_history_record",
				"asset_module_settings",
				"asset_type_attribute",
				"asset_type_deployment_attribute",
				"asset_waypoint_attachment",
				"asset_waypoint",
				"asset_deployment_disruption",
				"asset_deployment",
				"asset_metadata_mapping",
				"asset_station_location_history",
				"asset_station_location_attribute_value",
				"asset_station_location_attribute",
				"asset_station_location",
				"asset_station_attribute",
				"asset_station",
				"asset_attribute_list_item",
				"asset_attribute",
				"asset",
				"asset_type",
				"asset_map_style"
		};
		
		@SuppressWarnings("nls")
		String[] LABELTABLES = new String[]{
				"asset_attribute_list_item",
				"asset_attribute",
				"asset_type"
		};
		
		List<ConservationArea> cas = null;
		//drop tables
		
		
		cas = HibernateManager.getConservationAreas(session);

		//drop labels and tables
		for (String table : LABELTABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeMutationQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart." + table + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		for (String table : TABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeMutationQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
			}
		}		

		//delete all waypoints associated with type SURVEY
		session.createMutationQuery("DELETE Waypoint WHERE source = :src") //$NON-NLS-1$
			.setParameter("src", AssetWaypointSource.KEY) //$NON-NLS-1$
			.executeUpdate();
		
		//remove from plugin table
		HibernateManager.setPlugInVersion(AssetPlugIn.PLUGIN_ID, null, session);
		
		if (cas != null){
			for (ConservationArea ca : cas){
				try {
					Path deleteMe = Paths.get(ca.getFileDataStoreLocation()).resolve(AssetWaypointSource.FILESTORE_LOC);
					SmartUtils.deleteDirectory(deleteMe);
				} catch (IOException ex) {
					//some errors deleting filestore
					AssetPlugIn.log(ex.getMessage(), ex);
				}
			}
		}

		
	}
}