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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.util.SmartUtils;

/**
 * Job removes all plan related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveIntelligenceJob extends Job {

	public RemoveIntelligenceJob() {
		super(Messages.RemoveIntelligenceJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<ConservationArea> cas = null;
		
		try(Session session = HibernateManager.openSession()){
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
				Intelligence2PlugIn.displayLog(Messages.RemoveIntelligenceJob_UninstallError, e);
				return new Status(Status.ERROR,Intelligence2PlugIn.PLUGIN_ID,e.getMessage());
			}
		}
		
		if (cas != null){
			//delete intelligence data from the filestore 
			for (ConservationArea ca : cas){
				try {
					Path folder = Paths.get(ca.getFileDataStoreLocation())
							.resolve(IntelAttachment.INTELLIGENCE_FS_DIR);
					SmartUtils.deleteDirectory(folder);
				} catch (IOException ex) {
					Intelligence2PlugIn.log(Messages.RemoveIntelligenceJob_DeleteFolderError + ex.getMessage(), ex);
				}
				try {
					Path folder = Paths.get(ca.getFileDataStoreLocation())
							.resolve(IntelReportManager.TEMP_DIRECTORY);
					SmartUtils.deleteDirectory(folder);
				} catch (IOException ex) {
					Intelligence2PlugIn.log(Messages.RemoveIntelligenceJob_DeleteFolderError + ex.getMessage(), ex);
				}
			}
		}
		
		return Status.OK_STATUS;
	}
	
	@SuppressWarnings("nls")
	private void uninstall(Session s){
		String[] sql = new String[]{
				"DROP TABLE smart.i_entity_location",
				"DROP TABLE smart.i_entity_search",
				"DROP TABLE smart.i_entity_attribute_value",
				"DROP TABLE smart.i_entity_relationship_attribute_value",
				"DROP TABLE smart.i_attribute_list_item",
				"DROP TABLE smart.i_relationship_type_attribute",
				"DROP TABLE smart.i_entity_type_attribute",
				"DROP TABLE smart.i_entity_record",
				"DROP TABLE smart.i_working_set_record",
				"DROP TABLE smart.i_record_attachment",
				"DROP TABLE smart.I_record_attribute_value_list",
				"DROP TABLE smart.i_record_attribute_value",
				"DROP TABLE smart.i_entity_relationship",
				"DROP TABLE smart.i_working_set_query",
				"DROP TABLE smart.i_working_set_entity",
				"DROP TABLE smart.i_working_set",
				"DROP TABLE smart.i_entity_attachment",
				"DROP TABLE smart.i_entity",
				"DROP TABLE smart.i_entity_type_attribute_group",
				"DROP TABLE smart.i_record_obs_query",
				"DROP TABLE smart.i_attachment",
				"DROP TABLE smart.i_recordsource_attribute",
				"DROP TABLE smart.i_observation_attribute_list",
				"DROP TABLE smart.i_observation_attribute",
				"DROP TABLE smart.i_observation",
				"DROP TABLE smart.i_location",
				"DROP TABLE smart.i_record",
				"DROP TABLE smart.I_PROFILE_ENTITY_TYPE",
				"DROP TABLE smart.I_PROFILE_RECORD_SOURCE",
				"DROP TABLE smart.i_recordsource",
				"drop table smart.I_DIAGRAM_ENTITY_TYPE_STYLE",
				"drop table smart.I_DIAGRAM_RELATIONSHIP_TYPE_STYLE",
				"drop table smart.I_DIAGRAM_STYLE",
				"DROP TABLE smart.i_relationship_type",
				"DROP TABLE smart.i_relationship_group",
				"DROP TABLE smart.i_entity_type",
				"DROP TABLE smart.i_attribute",
				"drop table smart.I_RECORD_QUERY",
				"drop table smart.I_RECORD_SUMMARY_QUERY",
				"drop table smart.I_PERMISSION",
				"drop table smart.I_PROFILE_CONFIG",
				"DROP TABLE smart.i_config_option",
				"DROP TABLE smart.i_entity_summary_query",
				"DROP TABLE smart.i_entity_record_query",
				
				"DROP FUNCTION smart.metaphoneContains",
		};
		
		String[] namedClasses = new String[] {
			
			"smart.i_entity_type",
			"smart.i_entity_type_attribute_group",
			"smart.i_working_set",

			"smart.i_attribute",
			"smart.i_attribute_list_item",
			"smart.i_entity_search",
			"smart.i_profile_config",
			
			"smart.i_recordsource",
			"smart.i_recordsource_attribute",
			"smart.i_relationship_group",
			"smart.i_relationship_type",
			
			"smart.i_entity_record_query",
			"smart.i_entity_summary_query",
			"smart.i_record_obs_query",
			"smart.i_record_query",
			"smart.i_record_summary_query"
		};
		
		s.doWork(new Work(){

			@Override
			public void execute(Connection connection) throws SQLException {
				for (String item : namedClasses) {
					connection.createStatement().execute("DELETE FROM smart.i18n_label WHERE element_uuid in (SELECT uuid FROM " + item + ")");
				}
				for (String s : sql){
					connection.createStatement().execute(s);
				}
			}
			
		});
		
	}
}
