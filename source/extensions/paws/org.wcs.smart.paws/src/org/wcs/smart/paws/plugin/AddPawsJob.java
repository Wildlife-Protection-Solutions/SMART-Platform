/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.plugin;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;

/**
 * Adds and/or upgrades the PAWS plugin database tables.
 * 
 * @author Emily
 *
 */
public class AddPawsJob extends Job {

	public AddPawsJob() {
		super(Messages.AddPawsJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				installPlugin(session);
				session.getTransaction().commit();
			} catch (final Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						PawsPlugIn.displayLog(Messages.AddPawsJob_InstallError + ex.getMessage(), ex);
					}
				});
				return new Status(IStatus.ERROR, PawsPlugIn.PLUGIN_ID, 1, Messages.AddPawsJob_InstallError + ex.getMessage(), ex);
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	/**
	 * This function performs the installation or upgrade
	 * of the database.
	 * @param session
	 */
	public void installPlugin(Session session){
		String currentVersion = HibernateManager.getPlugInVersion(PawsPlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			//the plug is not yet installed in this database; create the tables
			createTables(session);
			currentVersion = PawsPlugIn.DB_VERSION_1;
		}
		//upgrades from version1 to the current version
		PawsDatabaseUpgrader.upgrade(PawsPlugIn.DB_VERSION_1, session);
	}
	

	private void createTables(Session session){
		String[] sql = new String[]{
				//Tables
				"CREATE TABLE smart.paws_configuration(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, name varchar(8192) NOT NULL, PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_parameter( uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data NOT NULL, keyid varchar(8192) NOT NULL, value varchar(8192), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_query_class(uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data NOT NULL, query_uuid char(16) for bit data NOT NULL, query_type varchar(32) NOT NULL, classification varchar(512) NOT NULL, PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_run(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data, id varchar(256) NOT NULL, server_run_id varchar(256), run_date timestamp, package_file varchar(256), result_location varchar(256), status varchar(32) NOT NULL, status_message long varchar, server_status_json long varchar, train_start_year smallint, train_end_year smallint, forecast_start_year smallint, forecast_end_year smallint, paws_task_id varchar(8192), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_service(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL UNIQUE, heatmap_api varchar(8192), task_api varchar(8192), api_key varchar(8192), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_simple_class(uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data NOT NULL, classification varchar(512) NOT NULL, date_range varchar(512), category_hkey varchar(32672) NOT NULL, attribute_key varchar(128), list_key varchar(128), tree_hkey varchar(32672), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_workspace(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL UNIQUE, url varchar(8192), client_id varchar(8192), storage_account_url varchar(8192), container_name varchar(8192), PRIMARY KEY (uuid))", //$NON-NLS-1$
				
				"ALTER TABLE smart.paws_configuration ADD CONSTRAINT paws_config_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_run ADD CONSTRAINT paws_run_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_service ADD CONSTRAINT pawsservice_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_workspace ADD CONSTRAINT pawsworkspace_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_parameter ADD CONSTRAINT paws_parameter_config_fk FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_query_class ADD CONSTRAINT paws_queryclass_config_fk FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_simple_class ADD CONSTRAINT paws_simpleclass_config_fk FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_run ADD CONSTRAINT paws_run_config_fk FOREIGN KEY (config_uuid) REFERENCES smart.paws_configuration (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.paws_configuration TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_configuration TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_configuration TO MANAGER", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.paws_parameter TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_parameter TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_parameter TO MANAGER", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.paws_query_class TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_query_class TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_query_class TO MANAGER", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.paws_run TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_run TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_run TO MANAGER", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.paws_service TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_service TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_service TO MANAGER", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.paws_simple_class TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_simple_class TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_simple_class TO MANAGER", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.paws_workspace TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_workspace TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.paws_workspace TO MANAGER", //$NON-NLS-1$
		};
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					PawsPlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}
			}
			
		});
		HibernateManager.setPlugInVersion(PawsPlugIn.PLUGIN_ID, PawsPlugIn.DB_VERSION_1, session);
	}
	
}
