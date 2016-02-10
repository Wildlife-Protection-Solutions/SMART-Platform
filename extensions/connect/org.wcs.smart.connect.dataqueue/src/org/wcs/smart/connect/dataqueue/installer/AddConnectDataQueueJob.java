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
package org.wcs.smart.connect.dataqueue.installer;

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
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes adds entity plug-in related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddConnectDataQueueJob extends Job {

	public AddConnectDataQueueJob() {
		super("Adding Connect Data Queue Plugin Tables");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
						
		Session session = HibernateManager.openSession();
		try{
			monitor.beginTask("Validating versions", 10);
			String currentVersion = HibernateManager.getPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, session);
			if (currentVersion == null){
				session.beginTransaction();
				try{
					installPlugin(session, true);
					HibernateManager.setPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, ConnectDataQueuePlugin.DB_VERSION_1, session);
					session.getTransaction().commit();
				}catch(Exception ex){
					session.getTransaction().rollback();
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(),
									"Error",
									"An error occurred while installing the Connect Data Processing Queue module (failed to create required database tables). Please restart the system, uninstall the module, then try reinstalling the module.  If the problem persists contact your system administrator.");
						}
						
					});
					return new Status(Status.ERROR,ConnectDataQueuePlugin.PLUGIN_ID, "Error installing plugin tables.", ex);
				}	
				currentVersion = ConnectDataQueuePlugin.DB_VERSION_1;
			}
			//run the upgrader to upgrade to the current version
			ConnectDataQueueDatabaseUpgrader.upgrade(currentVersion, session);
					
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
	public void installPlugin(Session session, boolean installChangeTracking) throws Exception{
		createTables(session);
		HibernateManager.setPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, ConnectDataQueuePlugin.DB_VERSION_1, session);
	}
	
	private void createTables(Session session){
		final String[] sql = new String[]{
			
			"CREATE TABLE smart.connect_data_queue(uuid char(16) for bit data NOT NULL, type VARCHAR(32) NOT NULL, ca_uuid char(16) for bit data,name VARCHAR(4096),status varchar(32) NOT NULL,queue_order integer,error_message VARCHAR(8192),local_file varchar(4096),date_processed timestamp,server_item_uuid char(16) for bit data,PRIMARY KEY (uuid))",
			"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT data_queue_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT status_chk CHECK (status IN ('DOWNLOADING', 'QUEUED', 'REQUEUED', 'PROCESSING', 'COMPLETE', 'COMPLETE_WARN', 'ERROR'))",
			"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT type_chk CHECK (type IN ('PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML'))",
			
			"CREATE TABLE smart.data_queue_processing_op(ca_uuid  char(16) for bit data not null, keyid varchar(256) NOT NULL, value varchar(512), primary key (ca_uuid, keyid))",
			"ALTER TABLE smart.data_queue_processing_op ADD CONSTRAINT data_queue_option_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE",
				
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue TO MANAGER", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue TO DATA_ENTRY", //$NON-NLS-1$
			
			"GRANT ALL PRIVILEGES ON SMART.data_queue_processing_op TO MANAGER", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.data_queue_processing_op TO DATA_ENTRY", //$NON-NLS-1$
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
