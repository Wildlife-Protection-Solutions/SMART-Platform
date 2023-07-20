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
package org.wcs.smart.cybertracker.updatesite;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
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
	public String getPluginId() {
		return CyberTrackerPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(CyberTrackerPlugIn.getDefault().getBundle());
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(CyberTrackerPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(CyberTrackerPlugIn.PLUGIN_ID).equals(CyberTrackerPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CtDatabaseUpgrader_UpgradeTask);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade( versions.get(CyberTrackerPlugIn.PLUGIN_ID), session);
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
	private void upgrade(String currentVersion, Session session) {
		if (currentVersion == null) {
			createTables(session);
			(new CtDatabaseUpgrader30To40()).upgrade(session);
			update40to50(session);
			update50to60(session);
			update60to70(session);
			update70to75(session);
			update75to80(session);
		}else if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_3_0)) {
			(new CtDatabaseUpgrader30To40()).upgrade(session);
			update40to50(session);
			update50to60(session);
			update60to70(session);
			update70to75(session);
			update75to80(session);
		}else if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_4_0)) {
			update40to50(session);
			update50to60(session);
			update60to70(session);
			update70to75(session);
			update75to80(session);
		}else if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_5_0)) {
			update50to60(session);
			update60to70(session);
			update70to75(session);
			update75to80(session);
		}else if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_6_0)) {
			update60to70(session);
			update70to75(session);
			update75to80(session);
		}else if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_7_0)) {
			update70to75(session);
			update75to80(session);
		}else if (currentVersion.equals(CyberTrackerPlugIn.DB_VERSION_7_5)) {
			update75to80(session);
		}
		
	}
	
	private void update40to50(Session session) {
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
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_5_0, session);
	}
	
	
	private void update50to60(Session session) {
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
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_6_0, session);
	}
	
	private void update60to70(Session session) {
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
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_7_0, session);
	}
	
	private static void update70to75(Session session) {
		String[] sql = new String[] {
			"delete from smart.CT_INCIDENT_LINK where obs_group_uuid is not null and obs_group_uuid not in (select uuid from smart.WP_OBSERVATION_GROUP)", //$NON-NLS-1$
			"alter table smart.ct_incident_link add constraint ct_incident_link_obs_group_fk foreign key (obs_group_uuid) references smart.wp_observation_group on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$		
		};
		
		for (String s : sql) {
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_7_5, session);
	}
	
	private void update75to80(Session session) {
		String[] sql = new String[] {
			"ALTER TABLE smart.ct_metadata_value ADD COLUMN is_required boolean default false not null", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_8_0, session);
	}
	
	private void createTables(Session session) {
		final boolean tables[] = {false, false}; //properties table
		
		//check is required table exists
		tables[0] = DerbyHibernateExtensions.tableExists(session, "CYBERTRACKER_PROPERTIES"); //$NON-NLS-1$
		tables[1] = DerbyHibernateExtensions.tableExists(session, "CT_PROPERTIES_OPTION"); //$NON-NLS-1$
		

		//this must be run as Admin User
		//AND only admin users should be able to install plugins in the first place.
		//so we shouldn't need to do this (if we bring this back we need to make sure
		//to reconnect back to the correct user)
		//HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		if (tables[0]) {
			//old table present, need to drop it
			String dropSql = "DROP TABLE smart.cybertracker_properties"; //$NON-NLS-1$
			session.createNativeMutationQuery(dropSql).executeUpdate();
		}
								
		if (!tables[1]) {
			String createSql = "CREATE TABLE smart.ct_properties_option ("+ //$NON-NLS-1$
					"uuid CHAR(16) for bit data NOT NULL, "+ //$NON-NLS-1$
					"ca_uuid CHAR(16) for bit data  NOT NULL, "+ //$NON-NLS-1$
					"OPTION_ID VARCHAR(32) NOT NULL, "+ //$NON-NLS-1$
					"DOUBLE_VALUE DOUBLE, "+ //$NON-NLS-1$
					"INTEGER_VALUE INTEGER, "+ //$NON-NLS-1$
					"STRING_VALUE VARCHAR(1024), "+ //$NON-NLS-1$
					"PRIMARY KEY (UUID))"; //$NON-NLS-1$
				String alterSql = "ALTER TABLE smart.ct_properties_option "+ //$NON-NLS-1$
					"ADD CONSTRAINT ct_properties_option_ca_uuid_fk FOREIGN KEY (CA_UUID) "+ //$NON-NLS-1$
					"REFERENCES smart.conservation_area(UUID) "+ //$NON-NLS-1$
					"ON UPDATE RESTRICT "+ //$NON-NLS-1$
					"ON DELETE CASCADE"; //$NON-NLS-1$
			session.createNativeMutationQuery(createSql).executeUpdate();
			session.createNativeMutationQuery(alterSql).executeUpdate();
				
						
			session.createNativeMutationQuery("GRANT ALL PRIVILEGES ON smart.ct_properties_option to data_entry").executeUpdate(); //$NON-NLS-1$
			session.createNativeMutationQuery("GRANT ALL PRIVILEGES ON smart.ct_properties_option to manager").executeUpdate(); //$NON-NLS-1$
			session.createNativeMutationQuery("GRANT ALL PRIVILEGES ON smart.ct_properties_option to analyst").executeUpdate(); //$NON-NLS-1$
			session.createNativeMutationQuery("GRANT SELECT ON smart.ct_properties_option to login").executeUpdate(); //$NON-NLS-1$
		}
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_3_0, session);
	}
}
