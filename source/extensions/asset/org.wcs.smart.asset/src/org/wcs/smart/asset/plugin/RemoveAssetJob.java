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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all plan related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveAssetJob extends Job {

	public RemoveAssetJob() {
		super("Uninstalling asset plugin");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
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
				AssetPlugIn.displayLog("Error uninstalling asset plugin: " + e.getMessage(), e);
				return new Status(Status.ERROR,AssetPlugIn.PLUGIN_ID,e.getMessage());
			}
		}	
		return Status.OK_STATUS;
	}

	private void uninstall(Session session){
		
		@SuppressWarnings("nls")
		String[] TABLES = new String[]{
				"smart.asset_attribute_value",
				"smart.asset_deployment_attribute_value",
				"smart.asset_station_attribute_value",
				"smart.asset_history_record",
				"smart.asset_module_settings",
				"smart.asset_type_attribute",
				"smart.asset_type_deployment_attribute",
				"smart.asset_waypoint_attachment",
				"smart.asset_waypoint",
				"smart.asset_deployment",
				"smart.asset_metadata_mapping",
				"smart.asset_station_location_history",
				"smart.asset_station_location_attribute_value",
				"smart.asset_station_location_attribute",
				"smart.asset_station_location",
				"smart.asset_station_attribute",
				"smart.asset_station",
				"smart.asset_attribute_list_item",
				"smart.asset_attribute",
				"smart.asset",
				"smart.asset_type"
		};
		
		String[] LABELTABLES = new String[]{
				"smart.asset_attribute_list_item",
				"smart.asset_attribute",
				"smart.asset_type"
		};
		
		List<ConservationArea> cas = null;
		//drop tables
		
		
		cas = HibernateManager.getConservationAreas(session);
		//delete all waypoints associated with type SURVEY
		Query<?> q = session.createQuery("DELETE Waypoint WHERE source = :src"); //$NON-NLS-1$
		q.setParameter("src", AssetWaypointSource.KEY); //$NON-NLS-1$
		q.executeUpdate();
		
		for (String table : LABELTABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart." + table + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
			
		for (String table : TABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
			}
		}		

		//remove from plugin table
		HibernateManager.setPlugInVersion(AssetPlugIn.PLUGIN_ID, null, session);
		
		if (cas != null){
			for (ConservationArea ca : cas){
				try {
					File deleteMe = new File(ca.getFileDataStoreLocation(), AssetWaypointSource.FILESTORE_LOC);
					FileUtils.deleteDirectory(deleteMe);
				} catch (IOException ex) {
					//some errors deleting filestore
					AssetPlugIn.log(ex.getMessage(), ex);
				}
			}
		}

		
	}
}
