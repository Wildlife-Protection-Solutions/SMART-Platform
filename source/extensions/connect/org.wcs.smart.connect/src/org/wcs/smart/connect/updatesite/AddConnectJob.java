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
package org.wcs.smart.connect.updatesite;

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
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.upgrade.ConnectDatabaseUpgrader;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes adds entity plug-in related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddConnectJob extends Job {

	public AddConnectJob() {
		super("Create Connect Tables");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
						
		Session session = HibernateManager.openSession();
		try{
			monitor.beginTask("Creating Connect Database Tables", 10);
			String currentVersion = HibernateManager.getPlugInVersion(ConnectPlugIn.PLUGIN_ID, session);
			if (currentVersion == null){
				session.beginTransaction();
				try{
					createTables(session);
					ChangeLogInstaller.INSTANCE.setEnabled(true);
					ChangeLogInstaller.INSTANCE.installChangeLogTracking(session);
					HibernateManager.setPlugInVersion(ConnectPlugIn.PLUGIN_ID, ConnectPlugIn.DB_VERSION_1, session);
					session.getTransaction().commit();
				}catch(Exception ex){
					session.getTransaction().rollback();
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(),
									"Error",
									"An error occurred while installing the connect module (failed to create required database tables). Please restart the system, uninstall the module, then try reinstalling the module.  If the problem persists contact your system administrator.");
						}
						
					});
					return new Status(Status.ERROR,ConnectPlugIn.PLUGIN_ID, "Error installing plugin tables.", ex);
				}	
				currentVersion = ConnectPlugIn.DB_VERSION_1;
			}
			//run the upgrader to upgrade to the current version
			ConnectDatabaseUpgrader.upgrade(currentVersion, session);
					
		}finally{
			session.close();
		}
		return Status.OK_STATUS;
	}	
	
	
	@SuppressWarnings("nls")
	private void createTables(Session session){
		final String[] sql = new String[]{
			"CREATE TABLE SMART.CONNECT_SERVER(uuid char(16) for bit data not null, ca_uuid char(16) for bit data, url varchar(2064), options varchar(32600), certificate varchar(32000), PRIMARY KEY (uuid))",
			"ALTER TABLE smart.connect_server ADD CONSTRAINT server_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",
			"CREATE TABLE smart.connect_account( employee_uuid char(16) for bit data not null, connect_uuid char(16) for bit data not null, connect_user varchar(32), connect_pass varchar(1024), primary key(employee_uuid, connect_uuid))",
			"ALTER TABLE smart.connect_account ADD CONSTRAINT connect_employee_uuid_fk foreign key (employee_uuid) REFERENCES smart.employee (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE smart.connect_account ADD CONSTRAINT connect_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",			
			"CREATE TABLE smart.connect_status(ca_uuid char(16) for bit data not null, connect_uuid char(16) for bit data not null, version char(16) for bit data, server_revision bigint not null, status varchar(6), uploadurl long varchar, localfile long varchar,primary key (ca_uuid))",
			"ALTER TABLE smart.connect_status ADD CONSTRAINT connect_status_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE smart.connect_status ADD CONSTRAINT connect_status_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",
			"CREATE TABLE smart.connect_change_log(uuid char(16) for bit data NOT NULL, revision BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), action varchar(15) CONSTRAINT action_check CHECK (action in ('INSERT', 'UPDATE', 'DELETE', 'FS_INSERT', 'FS_DELETE', 'FS_UPDATE')), filename varchar(32672), tablename varchar(256), key1_fieldname varchar(256), key1 char(16) for bit data, key2_fieldname varchar(256), key2_str varchar(256), key2_uuid char(16) for bit data, ca_uuid char(16) for bit data, source varchar(6) default 'LOCAL', primary key (revision))",
			"ALTER TABLE smart.connect_change_log ADD CONSTRAINT connect_changelog_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE",
			"CREATE TABLE smart.connect_sync_history( uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, connect_uuid char(16) for bit data not null, datetime timestamp not null, sync_type varchar(16) not null CONSTRAINT type_check CHECK ( sync_type in ('UPLOAD', 'DOWNLOAD') ), status varchar(16) not null CONSTRAINT status_check CHECK ( status in ('ACTIVE', 'ERROR', 'DONE', 'NODATA') ), status_url varchar(32672), start_revision bigint, end_revision bigint, primary key(uuid))",
			"ALTER TABLE smart.connect_sync_history ADD CONSTRAINT connect_sync_history_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE smart.connect_sync_history ADD CONSTRAINT connect_sync_history_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server(uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",
			"CREATE FUNCTION smart.uuid() returns char(16) for bit data LANGUAGE JAVA NOT deterministic external name 'org.wcs.smart.util.DerbyUtils.createUuid' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT",
			
			"ALTER TABLE smart.connect_server ADD CONSTRAINT connect_server_ca_unq UNIQUE(ca_uuid)",
			
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO ANALYST",
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO MANAGER",
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO ADMIN",
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO DATAENTRY",
			
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO ANALYST",
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO MANAGER",
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO ADMIN",
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO DATAENTRY",
			
			"GRANT SELECT ON SMART.CONNECT_SERVER TO ANALYST",
			"GRANT SELECT ON SMART.CONNECT_SERVER TO DATAENTRY",
			"GRANT ALL PRIVILEGES ON SMART.CONNECT_SERVER TO MANAGER",

			"GRANT SELECT ON SMART.connect_account TO ANALYST",
			"GRANT SELECT ON SMART.connect_account TO DATAENTRY",
			"GRANT ALL PRIVILEGES  ON SMART.connect_account TO MANAGER",

			"GRANT ALL PRIVILEGES ON SMART.connect_status TO ANALYST",
			"GRANT ALL PRIVILEGES ON SMART.connect_status TO DATAENTRY",
			"GRANT ALL PRIVILEGES ON SMART.connect_status TO MANAGER",

			"GRANT ALL PRIVILEGES ON SMART.connect_change_log TO ANALYST",
			"GRANT ALL PRIVILEGES ON SMART.connect_change_log TO DATAENTRY",
			"GRANT ALL PRIVILEGES ON SMART.connect_change_log TO MANAGER",

			"GRANT ALL PRIVILEGES ON SMART.connect_sync_history TO ANALYST",
			"GRANT ALL PRIVILEGES ON SMART.connect_sync_history TO DATAENTRY",
			"GRANT ALL PRIVILEGES ON SMART.connect_sync_history TO MANAGER",

			"GRANT EXECUTE ON FUNCTION SMART.uuid TO ANALYST",
			"GRANT EXECUTE ON FUNCTION SMART.uuid TO DATAENTRY",
			"GRANT EXECUTE ON FUNCTION SMART.uuid TO MANAGER"
			
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
