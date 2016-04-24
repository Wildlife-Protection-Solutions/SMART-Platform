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
package org.wcs.smart.connect.cybertracker.updatesite;

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
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.connect.cybertracker.ConnectCtPlugIn;
import org.wcs.smart.connect.cybertracker.upgrade.ConnectCtDatabaseUpgrader;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job adds Connect for CyberTracker plug-in related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddConnectCtJob extends Job {

	public AddConnectCtJob() {
		super("Create Connect for CyberTracker Tables");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
						
		Session session = HibernateManager.openSession();
		try{
			monitor.beginTask("Creating Connect for CyberTracker Database Tables", 10);
			String currentVersion = HibernateManager.getPlugInVersion(ConnectCtPlugIn.PLUGIN_ID, session);
			if (currentVersion == null){
				session.beginTransaction();
				try{
					installPlugin(session);
					HibernateManager.setPlugInVersion(ConnectCtPlugIn.PLUGIN_ID, ConnectCtPlugIn.DB_VERSION_1, session);
					session.getTransaction().commit();
				}catch(Exception ex){
					session.getTransaction().rollback();
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(),
									"ErrorTitle",
									"An error occurred while installing the 'SMART Connect for CyberTracker' module (failed to create required database tables). Please restart the system, uninstall the module, then try reinstalling the module.  If the problem persists contact your system administrator.");
						}
						
					});
					return new Status(Status.ERROR, ConnectCtPlugIn.PLUGIN_ID, "Error installing plugin tables.", ex);
				}	
				currentVersion = ConnectCtPlugIn.DB_VERSION_1;
			}
			//run the upgrader to upgrade to the current version
			ConnectCtDatabaseUpgrader.upgrade(currentVersion, session);
					
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
		createTables(session);
		HibernateManager.setPlugInVersion(ConnectCtPlugIn.PLUGIN_ID, ConnectCtPlugIn.DB_VERSION_1, session);
	}
	
	private void createTables(Session session){
		final String[] sql = new String[]{
			"CREATE TABLE smart.connect_alert ( UUID CHAR(16) for bit data NOT NULL, CM_UUID CHAR(16) for bit data  NOT NULL, ALERT_ITEM_UUID CHAR(16) for bit data  NOT NULL, CM_ATTRIBUTE_UUID CHAR(16) for bit data, LEVEL SMALLINT NOT NULL, TYPE VARCHAR(64), PRIMARY KEY (UUID))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_alert ADD CONSTRAINT connect_alert_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_alert ADD CONSTRAINT connect_alert_cm_attribute_uuid_fk FOREIGN KEY (CM_ATTRIBUTE_UUID) REFERENCES smart.cm_attribute(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

			"GRANT ALL PRIVILEGES ON smart.connect_alert TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_alert TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_alert TO MANAGER" //$NON-NLS-1$
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
