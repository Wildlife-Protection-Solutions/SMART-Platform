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
package org.wcs.smart.intelligence.query.updatesite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.query.IntelligenceQueryPlugIn;
import org.wcs.smart.intelligence.query.internal.Messages;

/**
 * Job removes adds entity plug-in related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddIntelligenceQueriesJob extends Job {

	private static String[] CREATE_TABLE_SQL = new String[]{

		"CREATE TABLE smart.intel_record_query(UUID CHAR(16) FOR BIT DATA NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL, QUERY_FILTER VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, COLUMN_FILTER VARCHAR(32672), SHARED BOOLEAN DEFAULT false NOT NULL,ID VARCHAR(6) NOT NULL, STYLE LONG VARCHAR, PRIMARY KEY (UUID))", //$NON-NLS-1$
		"CREATE TABLE smart.intel_summary_query(UUID CHAR(16) FOR BIT DATA NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL, CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, SHARED BOOLEAN DEFAULT false NOT NULL,ID VARCHAR(6) NOT NULL, PRIMARY KEY (UUID))", //$NON-NLS-1$
		
		"ALTER TABLE smart.intel_record_query ADD constraint intel_record_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
		"ALTER TABLE smart.intel_record_query ADD constraint intel_record_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
		"ALTER TABLE smart.intel_record_query ADD constraint intel_record_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
		
		"GRANT ALL PRIVILEGES ON smart.intel_record_query to manager", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.intel_record_query to analyst", //$NON-NLS-1$
		
		"ALTER TABLE smart.intel_summary_query ADD constraint intel_summary_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
		"ALTER TABLE smart.intel_summary_query ADD constraint intel_summary_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
		"ALTER TABLE smart.intel_summary_query ADD constraint intel_summary_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
		
		"GRANT ALL PRIVILEGES ON smart.intel_summary_query to manager", //$NON-NLS-1$
		"GRANT ALL PRIVILEGES ON smart.intel_summary_query to analyst", //$NON-NLS-1$
	};
	
	
	public AddIntelligenceQueriesJob() {
		super(Messages.AddIntelligenceQueriesJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
		
		monitor.beginTask(Messages.AddIntelligenceQueriesJob_Progress, 10);

		//this must be run as Admin User
		//AND only admin users should be able to install plugins in the first place.
		//so we shouldn't need to do this
		//HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		Session session = HibernateManager.openSession();	
		
		try{
			String currentVersion = HibernateManager.getPlugInVersion(IntelligenceQueryPlugIn.PLUGIN_ID, session);
			if (currentVersion == null){
				createDatabaseTables(session);
				currentVersion = IntelligenceQueryPlugIn.DB_VERSION_1;
			}
		}catch(final Throwable e){
			//TODO: figure out what to do here, because this will install the new 
			//version anyways
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					IntelligenceQueryPlugIn.displayLog(Messages.AddIntelligenceQueriesJob_Error1 + e.getLocalizedMessage(), e);
				}
			});
			return new Status(Status.ERROR, IntelligenceQueryPlugIn.PLUGIN_ID,Messages.AddIntelligenceQueriesJob_Error2, e);
		}finally{
			try{
				session.close();
			}catch (Exception ex){
				//eat this
			}
		}
		
		return Status.OK_STATUS;
		
	}
	
	private void createDatabaseTables(Session session){
		//check is required table exists		
		session.beginTransaction();
		try{
			
			for (int i = 0; i < CREATE_TABLE_SQL.length; i ++){
				IntelligenceQueryPlugIn.log(CREATE_TABLE_SQL[i], null);
				session.createSQLQuery(CREATE_TABLE_SQL[i]).executeUpdate();
			}
			HibernateManager.setPlugInVersion(IntelligenceQueryPlugIn.PLUGIN_ID, IntelligenceQueryPlugIn.DB_VERSION_1, session);
			session.getTransaction().commit();
		}finally{
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
		}
	}
}
