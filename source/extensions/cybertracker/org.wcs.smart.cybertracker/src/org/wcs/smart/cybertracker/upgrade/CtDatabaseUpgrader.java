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
package org.wcs.smart.cybertracker.upgrade;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.updatesite.AddCyberTrackerJob;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * CyberTracker upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CtDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CtDatabaseUpgrader_UpgradeTask);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				if (versions == null)
					throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
				String currentPluginVersion = versions
						.get(CyberTrackerPlugIn.PLUGIN_ID);
	
				if (currentPluginVersion == null) {
					(new AddCyberTrackerJob()).installPlugin(session);
				} else {
					upgrade(currentPluginVersion, session);
				}
				session.getTransaction().commit();
			} catch (Exception ex) {
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
	 * @param session
	 *            in current transaction
	 */
	public static final void upgrade(String currentVersion, Session session) {
		if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_3_0)) {
			CtDatabaseUpgrader30To40 upgrader30To40 = new CtDatabaseUpgrader30To40();
			upgrader30To40.upgrade(session);
			update40to50(session);
			update50to60(session);
			update60to70(session);
		}
		if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_4_0)) {
			update40to50(session);
			update50to60(session);
			update60to70(session);
		}
		if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_5_0)) {
			update50to60(session);
			update60to70(session);
		}
		
		if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_6_0)) {
			update60to70(session);
		}
		
	}
	
	private static void update40to50(Session session) {
		String[] sql = new String[] {
				"CREATE TABLE smart.ct_incident_link (uuid char(16) for bit data not null, ct_group_id char(16) for bit data not null, wp_uuid char(16) for bit data not null, last_cnt integer not null, primary key (uuid))", //$NON-NLS-1$		
				"ALTER TABLE smart.ct_incident_link ADD CONSTRAINT ct_incident_link_wp_uuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.ct_incident_link to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_incident_link to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_incident_link to analyst", //$NON-NLS-1$ (for cleanup)
				"GRANT DELETE ON smart.ct_incident_link to login", //$NON-NLS-1$
				"GRANT SELECT ON smart.ct_incident_link to login", //$NON-NLS-1$
				"GRANT SELECT (uuid) ON smart.waypoint to login", //$NON-NLS-1$
				"GRANT SELECT (last_modified) ON smart.waypoint to login", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_5_0, session);
	}
	
	
	private static void update50to60(Session session) {
		String[] sql = new String[] {
				"CREATE TABLE smart.ct_metadata_value(uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, package_uuid char(16) for bit data not null, keyid varchar(32) not null, is_visible boolean not null, string_value varchar(8192), boolean_value boolean, uuid_value char(16) for bit data, primary key (uuid),unique(package_uuid, keyid))", //$NON-NLS-1$
				"CREATE TABLE smart.ct_metadata_value_uuid (uuid CHAR(16) for bit data NOT NULL,field_uuid CHAR(16) for bit data NOT NULL, uuid_value CHAR(16) for bit data NOT NULL, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value ADD CONSTRAINT ct_metadata_value_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value_uuid ADD CONSTRAINT field_uuid_option_uuid_fk FOREIGN KEY (field_uuid) REFERENCES smart.ct_metadata_value(UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_metadata_value to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_metadata_value to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_metadata_value to analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_metadata_value_uuid to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_metadata_value_uuid to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_metadata_value_uuid to analyst", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_6_0, session);
	}
	
	private static void update60to70(Session session) {
		String[] sql = new String[] {
				"CREATE TABLE smart.ct_navigation_layer(uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, name varchar(512), targets blob, created_date date not null, last_modified_date date, last_modified_by char(16) for bit data,  primary key (uuid))", //$NON-NLS-1$
				
				"ALTER TABLE smart.ct_navigation_layer ADD CONSTRAINT ct_navigation_layer_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.ct_navigation_layer ADD CONSTRAINT ct_navigation_layer_last_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee(UUID) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
	
				"GRANT ALL PRIVILEGES ON smart.ct_navigation_layer to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_navigation_layer to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_navigation_layer to analyst", //$NON-NLS-1$	
				
				"ALTER TABLE smart.ct_incident_link drop column last_cnt", //$NON-NLS-1$
				"ALTER TABLE smart.ct_incident_link add column ct_root_id char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.ct_incident_link add column obs_group_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.CT_INCIDENT_LINK alter column ct_group_id drop not null", //$NON-NLS-1$
				
				//drop unique constraint
				//this is the only way I could find to easily drop the unique constraint on these fields
				"RENAME COLUMN smart.ct_metadata_value.package_uuid to package_uuid_2", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value add column package_uuid char(16) for bit data", //$NON-NLS-1$
				"UPDATE  smart.ct_metadata_value  set package_uuid = package_uuid_2", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value alter column package_uuid set not null", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value drop column package_uuid_2", //$NON-NLS-1$

				"RENAME COLUMN smart.ct_metadata_value.keyid to keyid2", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value add column keyid varchar(32)", //$NON-NLS-1$
				"UPDATE smart.ct_metadata_value  set keyid = keyid2", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value alter column keyid set not null", //$NON-NLS-1$
				"ALTER TABLE smart.ct_metadata_value drop column keyid2", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_7_0, session);
	}
}
