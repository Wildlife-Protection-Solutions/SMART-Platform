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
package org.wcs.smart.i2.plugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.model.IntelAttachment;

/**
 * Job removes all plan related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveIntelligenceJob extends Job {

	public RemoveIntelligenceJob() {
		super("Uninstalling Intelligence Plugin");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<ConservationArea> cas = null;
		
		final Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			cas = HibernateManager.getConservationAreas(session);
			uninstall(session);
			HibernateManager.setPlugInVersion(Intelligence2PlugIn.PLUGIN_ID, null, session);
			session.getTransaction().commit();

		} catch (Exception e) {
			try{
				session.getTransaction().rollback();
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);	
			}
			Intelligence2PlugIn.displayLog("Error uninstalling intelligence plugin", e);
			return new Status(Status.ERROR,Intelligence2PlugIn.PLUGIN_ID,e.getMessage());
		} finally {
			try {
				session.close();
			} catch (Exception ex) {
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
		
		if (cas != null){
			//delete intelligence data from the filestore 
			for (ConservationArea ca : cas){
				try {
					File folder = new File(ca.getFileDataStoreLocation() + File.separator + IntelAttachment.INTELLIGENCE_FS_DIR);
					FileUtils.deleteDirectory(folder);
				} catch (IOException ex) {
					Intelligence2PlugIn.log("Unable to delete intelligence data folder:" + ex.getMessage(), ex);
				}
				try {
					File folder = new File(ca.getFileDataStoreLocation() + File.separator + IntelReportManager.TEMP_DIRECTORY);
					FileUtils.deleteDirectory(folder);
				} catch (IOException ex) {
					Intelligence2PlugIn.log("Unable to delete intelligence data folder:" + ex.getMessage(), ex);
				}
			}
		}
		
		return Status.OK_STATUS;
	}

	private void uninstall(Session s){
		String[] sql = new String[]{
				"DROP TABLE smart.i_record_attribute_value",
				"DROP TABLE smart.i_recordsource_attribute",
				"DROP TABLE smart.i_entity_location",
				"DROP TABLE smart.i_observation_attribute",
				"DROP TABLE smart.i_datamodel_event",
				"DROP TABLE smart.i_observation",
				"DROP TABLE smart.i_location",
				"DROP TABLE smart.i_entity_search",
				"DROP TABLE smart.i_entity_attribute_value",
				"DROP TABLE smart.i_entity_relationship_attribute_value",
				"DROP TABLE smart.i_attribute_list_item",
				"DROP TABLE smart.i_relationship_type_attribute",
				"DROP TABLE smart.i_entity_type_attribute",
				"DROP TABLE smart.i_entity_record",
				"DROP TABLE smart.i_working_set_record",
				"DROP TABLE smart.i_record_attachment",
				"DROP TABLE smart.i_record",
				"DROP TABLE smart.i_entity_relationship",
				"DROP TABLE smart.i_working_set_query",
				"DROP TABLE smart.i_working_set_entity",
				"DROP TABLE smart.i_working_set",
				"DROP TABLE smart.i_entity_attachment",
				"DROP TABLE smart.i_entity",
				"DROP TABLE smart.i_entity_type_attribute_group",
				"DROP TABLE smart.i_record_obs_query",
				"DROP TABLE smart.i_relationship_type",
				"DROP TABLE smart.i_attachment",
				"DROP TABLE smart.i_entity_type",
				"DROP TABLE smart.i_attribute",
				"DROP TABLE smart.i_relationship_group",
				"DROP TABLE smart.i_recordsource",
				"DROP TABLE smart.I_RECORD_ATTRIBUTE_VALUE_LIST",
				"DROP TABLE smart.I_RECORD_QUERY",
				"DROP FUNCTION smart.double_metaphone",
				"DROP FUNCTION smart.metaphoneContains",
		};
		s.doWork(new Work(){

			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					connection.createStatement().execute(s);
				}
			}
			
		});
		
	}
}
