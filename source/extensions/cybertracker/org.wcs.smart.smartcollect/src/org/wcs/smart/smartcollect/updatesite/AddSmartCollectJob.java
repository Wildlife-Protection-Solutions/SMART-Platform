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
package org.wcs.smart.smartcollect.updatesite;

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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;
import org.wcs.smart.smartcollect.internal.Messages;

/**
 * Create initial tables for SMART Collect Plugin
 * 
 * @author egouge
 * @since 3.0.0
 */
public class AddSmartCollectJob extends Job {

	public AddSmartCollectJob() {
		super(Messages.AddSmartCollectJob_InstallJobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();		
		try(Session session = HibernateManager.openSession()){
			monitor.beginTask(Messages.AddSmartCollectJob_TaskName, 1);
			session.beginTransaction();
			try{
				installPlugin(session);
				session.getTransaction().commit();
			}catch(Exception ex){
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								Messages.AddSmartCollectJob_ErrorTitle,
								Messages.AddSmartCollectJob_ErrorMessage);
					}
					
				});
				return new Status(Status.ERROR, SmartCollectPlugIn.PLUGIN_ID, "Error installing plugin tables.", ex); //$NON-NLS-1$
			}
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
		String currentVersion = HibernateManager.getPlugInVersion(SmartCollectPlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			createTables(session);
			HibernateManager.setPlugInVersion(SmartCollectPlugIn.PLUGIN_ID, SmartCollectPlugIn.DB_VERSION_1, session);
			currentVersion = SmartCollectPlugIn.DB_VERSION_1;
		}
		
		//run the upgrader to upgrade to the current version
		SmartCollectDatabaseUpgrader.upgrade(currentVersion, session);
	}
	
	private void createTables(Session session){
		
		final String[] sql = new String[]{
				"CREATE TABLE smart.smartcollect_waypoint(wp_uuid char(16) for bit data not null,  source varchar(32000), primary key(wp_uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.smartcollect_waypoint ADD CONSTRAINT smartcollect_wp_uuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(uuid) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$

				"CREATE TABLE smart.smartcollect_package(uuid char(16) for bit data not null, name varchar(512), ca_uuid char(16) for bit data not null,cm_uuid char(16) for bit data, ctprofile_uuid char(16) for bit data, basemapdef varchar(32672), primary key (uuid))", //$NON-NLS-1$

				"ALTER TABLE SMART.smartcollect_package ADD CONSTRAINT ct_community_package_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.smartcollect_package ADD CONSTRAINT ct_community_package_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.smartcollect_package ADD CONSTRAINT ct_community_package_ctprofile_uuid_fk FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.smartcollect_package to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.smartcollect_package to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.smartcollect_package to analyst", //$NON-NLS-1$
							
				"GRANT ALL PRIVILEGES ON smart.smartcollect_waypoint to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.smartcollect_waypoint to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.smartcollect_waypoint to analyst", //$NON-NLS-1$
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
