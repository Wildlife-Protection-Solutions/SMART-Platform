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
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * PAWS plugin database upgrade scripts.  Upgrades the database
 * and file store options to match the current 
 * plugin version
 * 
 * @author Emily
 * @since 3.0.0
 */
public class PawsDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return PawsPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(PawsPlugIn.getDefault().getBundle());
	}
	
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(PawsPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(PawsPlugIn.PLUGIN_ID).equals(PawsPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.PawsDatabaseUpgrader_TaskName);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(PawsPlugIn.PLUGIN_ID), session);
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}

	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * 
	 * @param currentVersion
	 * @param session is active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			return;
		}
	}
		
	
	private void createTables(Session session){
		String[] sql = new String[]{
				//Tables
				"CREATE TABLE smart.paws_configuration(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, name varchar(8192) NOT NULL, PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_parameter( uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data NOT NULL, keyid varchar(8192) NOT NULL, value varchar(8192), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_query_class(uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data NOT NULL, query_uuid char(16) for bit data NOT NULL, query_type varchar(32) NOT NULL, classification varchar(512) NOT NULL, PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_run(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data, id varchar(256) NOT NULL, server_run_id varchar(256), run_date timestamp, package_file varchar(256), container varchar(8192), result_location varchar(256), status varchar(32) NOT NULL, status_message long varchar, server_status_json long varchar, train_start_year smallint, train_end_year smallint, forecast_start_year smallint, forecast_end_year smallint, paws_task_id varchar(8192), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.paws_simple_class(uuid char(16) for bit data NOT NULL, config_uuid char(16) for bit data NOT NULL, classification varchar(512) NOT NULL, date_range varchar(512), category_hkey varchar(32672) NOT NULL, attribute_key varchar(128), list_key varchar(128), tree_hkey varchar(32672), PRIMARY KEY (uuid))", //$NON-NLS-1$
				
				"CREATE TABLE smart.paws_service(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL UNIQUE, paws_api varchar(8192), task_api varchar(8192), paws_api_key varchar(8192), oauth_url varchar(8192), client_id varchar(8192), storage_account_url varchar(8192), PRIMARY KEY (uuid))", //$NON-NLS-1$
				
				"ALTER TABLE smart.paws_configuration ADD CONSTRAINT paws_config_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_run ADD CONSTRAINT paws_run_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.paws_service ADD CONSTRAINT pawsservice_ca_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
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
