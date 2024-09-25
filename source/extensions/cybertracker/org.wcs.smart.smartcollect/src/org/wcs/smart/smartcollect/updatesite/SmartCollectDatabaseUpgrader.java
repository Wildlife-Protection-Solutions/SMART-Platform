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
package org.wcs.smart.smartcollect.updatesite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * SMART Collect upgrade operations.
 * 
 * @author egouge
 * @since 7.0.0
 */
public class SmartCollectDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return SmartCollectPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(SmartCollectPlugIn.getDefault().getBundle());
	}	

	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(SmartCollectPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(SmartCollectPlugIn.PLUGIN_ID).equals(SmartCollectPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		try(Session session = HibernateManager.openSession()){
		
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(SmartCollectPlugIn.PLUGIN_ID), session);
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
			upgradeV1toV2(session);
		}else if (currentVersion.equals(SmartCollectPlugIn.DB_VERSION_1)) {
			upgradeV1toV2(session);
		}
	}
	
	private void upgradeV1toV2(Session session) {
		String[] sql = new String[] {
				"insert into smart.i18n_label(language_uuid, element_uuid, value) select  a.uuid,b.uuid, b.name from smart.language a, smart.smartcollect_package b where a.ca_uuid = b.ca_uuid and a.isdefault", //$NON-NLS-1$
				"ALTER TABLE smart.smartcollect_package drop column name", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(SmartCollectPlugIn.PLUGIN_ID, SmartCollectPlugIn.DB_VERSION_2, session);
	}
	
	private void createTables(Session session){
		
		final String[] sql = new String[]{
				"CREATE TABLE smart.smartcollect_waypoint(wp_uuid char(16) for bit data not null,  source varchar(32000), primary key(wp_uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.smartcollect_waypoint ADD CONSTRAINT smartcollect_wp_uuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(uuid) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$

				"CREATE TABLE smart.smartcollect_package(uuid char(16) for bit data not null, name varchar(512), ca_uuid char(16) for bit data not null,cm_uuid char(16) for bit data, ctprofile_uuid char(16) for bit data, basemapdef varchar(32672), maplayersdef varchar(32672), primary key (uuid))", //$NON-NLS-1$

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
		HibernateManager.setPlugInVersion(SmartCollectPlugIn.PLUGIN_ID, SmartCollectPlugIn.DB_VERSION_1, session);

	}

}
