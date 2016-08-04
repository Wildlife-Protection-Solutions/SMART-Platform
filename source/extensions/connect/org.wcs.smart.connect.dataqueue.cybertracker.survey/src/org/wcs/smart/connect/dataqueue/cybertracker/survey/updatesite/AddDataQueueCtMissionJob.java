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
package org.wcs.smart.connect.dataqueue.cybertracker.survey.updatesite;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.dataqueue.cybertracker.survey.PlugIn;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job adds Connect for CyberTracker plug-in related tabled to the database
 * 
 * @author egouge
 * 
 */
public class AddDataQueueCtMissionJob extends Job {

	public AddDataQueueCtMissionJob() {
		super("Installing Cybertracker Connect Mission DataQueue Processor");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
						
		Session session = HibernateManager.openSession();
		try{
			monitor.beginTask("Creating Tables", 10);
			session.beginTransaction();
			installPlugin(session);
			session.getTransaction().commit();
		}catch(Exception ex){
			if (session.getTransaction().isActive()) session.getTransaction().rollback();
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Error",
							"Could not installed SMART Connect Cybertracker Mission Data Queue Processor");
				}
				
			});
			return new Status(Status.ERROR, PlugIn.PLUGIN_ID, "Error installing plugin tables.", ex); //$NON-NLS-1$
		}finally{
			session.close();
		}
		return Status.OK_STATUS;
	}	
	
	/**
	 * Creates the tables and enables change logging in core of SMART. 
	 * 
	 * @param session
	 * @param installChangeTracking if true this attempts to install change log tracking
	 * for required plugins (this install for plugins that are up-to-date).  Otherwise it will not
	 * attempt to do install tracking (this case is used for upgrades where tracking is installed
	 * after all updates/install are completed).
	 * @throws Exception
	 */
	public void installPlugin(Session session) throws Exception{
		String currentVersion = HibernateManager.getPlugInVersion(PlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			createTables(session);
			HibernateManager.setPlugInVersion(PlugIn.PLUGIN_ID, PlugIn.DB_VERSION_1, session);
			currentVersion = PlugIn.DB_VERSION_1;
		}
		
		//run the upgrader to upgrade to the current version
		DataQueueCtMissionDatabaseUpgrader.upgrade(currentVersion, session);
	}
	
	private void createTables(Session session){
		final String[] sql = new String[]{
			"CREATE TABLE smart.ct_mission_link ( CT_UUID CHAR(16) for bit data NOT NULL, MISSION_UUID CHAR(16) for bit data  NOT NULL, ct_device_id varchar(36) not null, last_observation_cnt integer, group_start_time timestamp, PRIMARY KEY (CT_UUID))", //$NON-NLS-1$
			
			"GRANT ALL PRIVILEGES ON smart.ct_mission_link TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.ct_mission_link TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.ct_mission_link TO MANAGER", //$NON-NLS-1$
			
			"ALTER TABLE smart.ct_mission_link ADD CONSTRAINT mission_uuid_fk FOREIGN KEY (mission_uuid) REFERENCES smart.mission ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE" //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}				
			}
		});
	}
}
