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
package org.wcs.smart.cybertracker.patrol.json.updatesite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.cybertracker.patrol.PatrolCyberTrackerPlugIn;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * Connect upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CtPatrolDatabaseUpgrader implements IDatabaseUpgrader {
	
	@Override
	public String getPluginId() {
		return PatrolCyberTrackerPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(PatrolCyberTrackerPlugIn.getDefault().getBundle());
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(PatrolCyberTrackerPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(PatrolCyberTrackerPlugIn.PLUGIN_ID).equals(PatrolCyberTrackerPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		try(Session session = HibernateManager.openSession()){
		
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(PatrolCyberTrackerPlugIn.PLUGIN_ID), session);
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
			update10to20(session);
			update20to30(session);
		}else if (currentVersion.equals(PatrolCyberTrackerPlugIn.DB_VERSION_1)) {
			update10to20(session);
			update20to30(session);
		}else if (currentVersion.equals(PatrolCyberTrackerPlugIn.DB_VERSION_2)) {
			update20to30(session);
		}
	}
	
	private void update10to20(Session session) {
		String[] sql = new String[] {
				"create table smart.ct_patrol_package(uuid char(16) for bit data not null, name varchar(512), ca_uuid char(16) for bit data not null, cm_uuid char(16) for bit data, ctprofile_uuid char(16) for bit data, has_incident boolean default false, incident_uuid char(16) for bit data, basemapdef varchar(32672), maplayersdef varchar(32672), primary key (uuid))", //$NON-NLS-1$

				"ALTER TABLE SMART.ct_patrol_package ADD CONSTRAINT ct_patrol_package_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_patrol_package ADD CONSTRAINT ct_patrol_package_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_patrol_package ADD CONSTRAINT ct_patrol_package_incident_uuid_fk FOREIGN KEY (incident_uuid) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_patrol_package ADD CONSTRAINT ct_patrol_package_ctprofile_uuid_fk FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.ct_patrol_package to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_patrol_package to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_patrol_package to analyst", //$NON-NLS-1$ 
								
				"create table smart.ct_patrol_wplink(uuid char(16) for bit data not null, ct_patrol_link_uuid char(16) for bit data, ct_root_id char(16) for bit data, ct_group_id char(16) for bit data,  wp_uuid char(16) for bit data, obs_group_uuid char(16) for bit data, primary key (uuid))", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_patrol_wplink ADD CONSTRAINT ct_patrol_link_uuid_fk FOREIGN KEY (ct_patrol_link_uuid) REFERENCES smart.ct_patrol_link(CT_UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$		
				"GRANT ALL PRIVILEGES ON smart.ct_patrol_wplink to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_patrol_wplink to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_patrol_wplink to analyst", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(PatrolCyberTrackerPlugIn.PLUGIN_ID, PatrolCyberTrackerPlugIn.DB_VERSION_2, session);
	}

	
	private void update20to30(Session session) {
		String[] sql = new String[] {
				"ALTER TABLE smart.ct_patrol_package add column patrol_type_uuid char(16) for bit data ", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_patrol_package ADD CONSTRAINT ct_patrol_package_patrol_type_uuid_fk FOREIGN KEY (patrol_type_uuid) REFERENCES smart.patrol_type(UUID) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"UPDATE smart.ct_patrol_package set patrol_type_uuid = (select uuid from smart.patrol_type where keyid = 'patrol' and smart.patrol_type.ca_uuid =  smart.ct_patrol_package.ca_uuid)", //$NON-NLS-1$
				
				"create table smart.pptemp (ca_uuid char(16) for bit data, uuid char(16) for bit data)", //$NON-NLS-1$
				"insert into smart.pptemp (ca_uuid, uuid)  select a.ca_uuid, a.uuid  from (select  ca_uuid, min(keyid) as keyid  from smart.patrol_type  group by ca_uuid) b join smart.patrol_type a on a.ca_uuid = b.ca_uuid and a.keyid = b.keyid", //$NON-NLS-1$
				"UPDATE smart.ct_patrol_package set patrol_type_uuid = (select uuid from smart.pptemp where smart.ct_patrol_package.ca_uuid = smart.pptemp.ca_uuid) where patrol_type_uuid is null", //$NON-NLS-1$
				"drop table smart.pptemp", //$NON-NLS-1$
				
				"alter table smart.ct_patrol_package alter column patrol_type_uuid set not null", //$NON-NLS-1$
				
				"insert into smart.i18n_label(language_uuid, element_uuid, value) select  a.uuid,b.uuid, b.name from smart.language a, smart.ct_patrol_package b where a.ca_uuid = b.ca_uuid and a.isdefault", //$NON-NLS-1$
				"ALTER TABLE smart.ct_patrol_package drop column name", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(PatrolCyberTrackerPlugIn.PLUGIN_ID, PatrolCyberTrackerPlugIn.DB_VERSION_3, session);
	}


	
	private void createTables(Session session){
		if (DerbyHibernateExtensions.tableExists(session, "ct_patrol_link")){ //$NON-NLS-1$
			return;
		}
		
		final String[] sql = new String[]{
			"CREATE TABLE smart.ct_patrol_link ( CT_UUID CHAR(16) for bit data NOT NULL, PATROL_LEG_UUID CHAR(16) for bit data  NOT NULL, ct_device_id varchar(36) not null, last_observation_cnt integer, group_start_time timestamp, PRIMARY KEY (CT_UUID))", //$NON-NLS-1$
			
			"GRANT ALL PRIVILEGES ON smart.ct_patrol_link TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.ct_patrol_link TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.ct_patrol_link TO MANAGER", //$NON-NLS-1$
			
			"ALTER TABLE smart.ct_patrol_link ADD CONSTRAINT patrol_key_uuid_fk FOREIGN KEY (patrol_leg_uuid) REFERENCES smart.patrol_leg ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE" //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}				
			}
		});
		HibernateManager.setPlugInVersion(PatrolCyberTrackerPlugIn.PLUGIN_ID, PatrolCyberTrackerPlugIn.DB_VERSION_1, session);
	}
}
