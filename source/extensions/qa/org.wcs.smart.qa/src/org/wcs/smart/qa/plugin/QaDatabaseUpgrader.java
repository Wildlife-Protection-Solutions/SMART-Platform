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
package org.wcs.smart.qa.plugin;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Database table manager for qa plugin
 * 
 * @author Emily
 *
 */
public class QaDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return QaPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(QaPlugIn.getDefault().getBundle());
	}

	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(QaPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(QaPlugIn.PLUGIN_ID).equals(QaPlugIn.DB_VERSION);
	}

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(QaPlugIn.PLUGIN_ID), session);
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}
	
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			upgradeV1toV2(session);
		}else if (currentVersion.equals(QaPlugIn.DB_VERSION_1)) {
			upgradeV1toV2(session);
		}
	}
	
	private void createTables(Session session){
		String[] sql = new String[]{
				 // Create Tables
				"CREATE TABLE smart.qa_error( uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data not null, qa_routine_uuid char(16) for bit data NOT NULL, data_provider_id varchar(128) not null, status varchar(32) NOT NULL, validate_date timestamp NOT NULL, error_id varchar(1024) NOT NULL, error_description varchar(32600), fix_message varchar(32600), src_identifier char(16) for bit data NOT NULL, geometry blob, PRIMARY KEY (uuid) )", //$NON-NLS-1$
				"CREATE TABLE smart.qa_routine(uuid char(16) FOR BIT DATA NOT NULL, ca_uuid char(16) FOR BIT DATA NOT NULL, routine_type_id varchar(1024) NOT NULL, description varchar(32600), auto_check boolean DEFAULT false NOT NULL, PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.qa_routine_parameter( uuid char(16) FOR BIT DATA NOT NULL, qa_routine_uuid char(16) FOR BIT DATA NOT NULL, id varchar(256) NOT NULL, str_value varchar(32600), byte_value blob, PRIMARY KEY (uuid, qa_routine_uuid) )", //$NON-NLS-1$

				// Create Foreign Keys
				"ALTER TABLE smart.qa_routine ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.qa_error ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.qa_routine_parameter ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.qa_error ADD FOREIGN KEY (qa_routine_uuid) REFERENCES smart.qa_routine (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				//Permissions
				"GRANT ALL PRIVILEGES ON smart.qa_error TO admin,data_entry,manager,analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.qa_routine TO admin,manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.qa_error TO admin,manager", //$NON-NLS-1$
				"GRANT SELECT ON smart.qa_routine TO data_entry,analyst", //$NON-NLS-1$
				"GRANT SELECT ON smart.qa_error TO data_entry,analyst", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.qa_routine_parameter TO admin,manager,data_entry", //$NON-NLS-1$
		};
		
		for (String s : sql){
			SmartPlugIn.log(s, null);
			session.createNativeQuery(s).executeUpdate();
		}

		HibernateManager.setPlugInVersion(QaPlugIn.PLUGIN_ID, QaPlugIn.DB_VERSION_1, session);
	}

	
	private void upgradeV1toV2(Session session){
		String[] sql = new String[]{
				 // Create Tables
				"GRANT ALL PRIVILEGES ON smart.qa_routine_parameter TO admin,manager,data_entry", //$NON-NLS-1$
		};
		
		for (String s : sql){
			SmartPlugIn.log(s, null);
			session.createNativeQuery(s).executeUpdate();
		}

		HibernateManager.setPlugInVersion(QaPlugIn.PLUGIN_ID, QaPlugIn.DB_VERSION_2, session);
	}
}
