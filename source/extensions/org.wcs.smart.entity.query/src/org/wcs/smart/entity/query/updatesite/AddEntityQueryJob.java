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
package org.wcs.smart.entity.query.updatesite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes adds entity plug-in related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddEntityQueryJob extends Job {

	private static String[] CREATE_TABLE_SQL = new String[]{

		"CREATE TABLE smart.entity_waypoint_query(	UUID CHAR(16) FOR BIT DATA NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL, QUERY_FILTER VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, COLUMN_FILTER VARCHAR(32672), SHARED BOOLEAN DEFAULT false NOT NULL,ID VARCHAR(6) NOT NULL, PRIMARY KEY (UUID));", //$NON-NLS-1$
		"CREATE TABLE smart.entity_summary_query(UUID CHAR(16) for bit data NOT NULL,CREATOR_UUID CHAR(16) for bit data NOT NULL,CA_FILTER VARCHAR(32672),QUERY_DEF VARCHAR(32672),FOLDER_UUID CHAR(16) for bit data,SHARED BOOLEAN NOT NULL,CA_UUID CHAR(16) for bit data NOT NULL,ID VARCHAR(6) NOT NULL,PRIMARY KEY (UUID));", //$NON-NLS-1$
		"CREATE TABLE smart.entity_gridded_query(UUID CHAR(16) for bit data NOT NULL,CREATOR_UUID CHAR(16) for bit data NOT NULL,QUERY_FILTER VARCHAR(32672),CA_FILTER VARCHAR(32672),QUERY_DEF VARCHAR(32672),FOLDER_UUID CHAR(16) for bit data,SHARED BOOLEAN NOT NULL,CA_UUID CHAR(16) for bit data NOT NULL,ID VARCHAR(6) NOT NULL,CRS_DEFINITION VARCHAR(32672) NOT NULL,PRIMARY KEY (UUID));", //$NON-NLS-1$
		"CREATE TABLE smart.entity_observation_query (UUID CHAR(16) FOR BIT DATA NOT NULL,CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL,QUERY_FILTER VARCHAR(32672),CA_FILTER VARCHAR(32672),CA_UUID CHAR(16) FOR BIT DATA NOT NULL,FOLDER_UUID CHAR(16) FOR BIT DATA,COLUMN_FILTER VARCHAR(32672),SHARED BOOLEAN DEFAULT false NOT NULL,ID VARCHAR(6) NOT NULL,PRIMARY KEY (UUID));", //$NON-NLS-1$
		"ALTER TABLE smart.entity_waypoint_query ADD constraint entitywaypoint_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_waypoint_query ADD constraint entity_waypoint_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_waypoint_query ADD constraint entity_waypoint_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_observation_query ADD constraint entityobservation_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT 	ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_observation_query ADD constraint entity_observation_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_observation_query ADD constraint entity_observation_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_summary_query ADD constraint entity_summary_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_summary_query ADD constraint entity_summary_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_summary_query ADD constraint entity_summary_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_gridded_query ADD constraint entity_gridded_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_gridded_query ADD constraint entity_gridded_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"ALTER TABLE smart.entity_gridded_query ADD constraint entity_gridded_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_observation_query to manager;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_waypoint_query to manager;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_summary_query to manager;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_gridded_query to manager;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_observation_query to analyst;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_waypoint_query to analyst;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_summary_query to analyst;", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.entity_gridded_query to analyst;" //$NON-NLS-1$
	};
	
	
	public AddEntityQueryJob() {
		super(Messages.AddEntityQueryJob_AddQueryTablesJobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		monitor.beginTask(Messages.AddEntityQueryJob_Progress1, 10);

		//this must be run as Admin User
		//AND only admin users should be able to install plugins in the first place.
		//so we shouldn't need to do this
		//HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		Session session = HibernateManager.openSession();	
		
		try{
			String currentVersion = HibernateManager.getPlugInVersion(EntityPlugIn.PLUGIN_ID, session);
			if (currentVersion == null){
				return createDatabaseTables(session);
			}
		}catch(final Exception e){
			//TODO: figure out what to do here, because this will install the new 
			//version anyways
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					EntityPlugIn.displayLog(Messages.AddEntityQueryJob_InstallError + e.getLocalizedMessage(), e);
				}
			});
			return new Status(Status.ERROR, EntityPlugIn.PLUGIN_ID, Messages.AddEntityQueryJob_InstallError, e);
		}finally{
			try{
				session.close();
			}catch (Exception ex){
				//eat this
			}
		}
		
		return Status.OK_STATUS;
		
	}
	
	private IStatus createDatabaseTables(Session session){
		//check is required table exists		
		try {
			session.beginTransaction();
			
			for (int i = 0; i < CREATE_TABLE_SQL.length; i ++){
				session.createSQLQuery(CREATE_TABLE_SQL[i]).executeUpdate();
			}
	
			session.getTransaction().commit();
		} catch (final Exception e) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn.displayLog(null, Messages.AddEntityQueryJob_InstallError2, e);
				}
			});
			return new Status(IStatus.ERROR, EntityPlugIn.PLUGIN_ID, 1, Messages.AddEntityQueryJob_InstallError + e.getLocalizedMessage(),e);
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		}
		return Status.OK_STATUS;
	}
}
