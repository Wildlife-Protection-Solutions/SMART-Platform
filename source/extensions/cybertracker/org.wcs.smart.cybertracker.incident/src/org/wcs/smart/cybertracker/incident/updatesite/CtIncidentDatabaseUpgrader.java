/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.incident.updatesite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.cybertracker.incident.CtIncidentPlugIn;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * SMART CT Incident upgrade progress
 * 
 */
public class CtIncidentDatabaseUpgrader implements IDatabaseUpgrader {
	
	@Override
	public String getPluginId() {
		return CtIncidentPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(CtIncidentPlugIn.getDefault().getBundle());
	}
	

	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(CtIncidentPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(CtIncidentPlugIn.PLUGIN_ID).equals(CtIncidentPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CtIncidentDatabaseUpgrader_Progress);
		try(Session session = HibernateManager.openSession()){
		
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(CtIncidentPlugIn.PLUGIN_ID), session);
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}
	
	/**
	 * 
	 * @param currentVersion
	 * @param session in active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
		}
	}
	
	
	private void createTables(Session session){
		
		final String[] sql = new String[]{

				"CREATE TABLE smart.ct_incident_package(uuid char(16) for bit data not null, name varchar(512), ca_uuid char(16) for bit data not null,cm_uuid char(16) for bit data, ctprofile_uuid char(16) for bit data, basemapdef varchar(32672), maplayersdef varchar(32672), primary key (uuid))", //$NON-NLS-1$

				"ALTER TABLE SMART.ct_incident_package ADD CONSTRAINT ct_incident_package_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_incident_package ADD CONSTRAINT ct_incident_package_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_incident_package ADD CONSTRAINT ct_incident_package_ctprofile_uuid_fk FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.ct_incident_package to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_incident_package to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_incident_package to analyst", //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}				
			}
		});
		
		HibernateManager.setPlugInVersion(CtIncidentPlugIn.PLUGIN_ID, CtIncidentPlugIn.DB_VERSION_1, session);
	}

}
