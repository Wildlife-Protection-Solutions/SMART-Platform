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
package org.wcs.smart.cybertracker.survey.updatesite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.cybertracker.survey.SurveyCyberTrackerPlugIn;
import org.wcs.smart.er.updatesite.ERDatabaseUpgrader;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * Connect upgrade operations while upgrade/restore backup.
 * 
 * @author egouge
 * 
 */
public class DataQueueCtMissionDatabaseUpgrader implements IDatabaseUpgrader {
	
	@Override
	public String getPluginId() {
		return SurveyCyberTrackerPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(SurveyCyberTrackerPlugIn.getDefault().getBundle());
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		
		SubMonitor progress = SubMonitor.convert(monitor, MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()), 2);
		
		progress.subTask( MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		//we need to ensure the mission table is installed first
		(new ERDatabaseUpgrader()).upgrade(progress.split(1));
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(SurveyCyberTrackerPlugIn.PLUGIN_ID), session);
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
		progress.worked(1);
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(SurveyCyberTrackerPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(SurveyCyberTrackerPlugIn.PLUGIN_ID).equals(SurveyCyberTrackerPlugIn.DB_VERSION);
	}
	
	/**
	 * 
	 * @param currentVersion
	 * @param session in active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			upgradeV1ToV2(session);
			upgradeV2ToV3(session);
		}else if (currentVersion.contentEquals(SurveyCyberTrackerPlugIn.DB_VERSION_1)) {
			upgradeV1ToV2(session);
			upgradeV2ToV3(session);
		}else if (currentVersion.contentEquals(SurveyCyberTrackerPlugIn.DB_VERSION_2)) {
			upgradeV2ToV3(session);
		}
	}
	
	private void upgradeV2ToV3(Session session) {
		String[] sql = new String[] {
				"insert into smart.i18n_label(language_uuid, element_uuid, value) select  a.uuid,b.uuid, b.name from smart.language a, smart.ct_survey_package b where a.ca_uuid = b.ca_uuid and a.isdefault", //$NON-NLS-1$
				"ALTER TABLE smart.ct_survey_package drop column name", //$NON-NLS-1$				
		};
		
		for (String s : sql) {
			session.createNativeMutationQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(SurveyCyberTrackerPlugIn.PLUGIN_ID, SurveyCyberTrackerPlugIn.DB_VERSION_3, session);
	}
	
	private void upgradeV1ToV2(Session session) {
		String[] sql = new String[] {
				"create table smart.ct_survey_package(uuid char(16) for bit data not null, name varchar(512), ca_uuid char(16) for bit data not null, sd_uuid char(16) for bit data, ctprofile_uuid char(16) for bit data, has_incident boolean default false, incident_uuid char(16) for bit data, basemapdef varchar(32672), maplayersdef varchar(32672), primary key (uuid))", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_survey_package ADD CONSTRAINT ct_survey_package_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_survey_package ADD CONSTRAINT ct_survey_package_sd_uuid_fk FOREIGN KEY (SD_UUID) REFERENCES smart.survey_design (UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_survey_package ADD CONSTRAINT ct_survey_package_incident_uuid_fk FOREIGN KEY (incident_uuid) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_survey_package ADD CONSTRAINT ct_survey_package_ctprofile_uuid_fk FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.ct_survey_package to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_survey_package to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_survey_package to analyst", //$NON-NLS-1$ 
				
				"create table smart.ct_mission_wplink(uuid char(16) for bit data not null, ct_mission_link_uuid char(16) for bit data, ct_root_id char(16) for bit data, ct_group_id char(16) for bit data,  wp_uuid char(16) for bit data, obs_group_uuid char(16) for bit data, primary key (uuid))", //$NON-NLS-1$
				"ALTER TABLE SMART.ct_mission_wplink ADD CONSTRAINT ct_mission_link_uuid_fk FOREIGN KEY (ct_mission_link_uuid) REFERENCES smart.ct_mission_link(CT_UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$		
				"GRANT ALL PRIVILEGES ON smart.ct_mission_wplink to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_mission_wplink to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.ct_mission_wplink to analyst", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeMutationQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(SurveyCyberTrackerPlugIn.PLUGIN_ID, SurveyCyberTrackerPlugIn.DB_VERSION_2, session);
	}

	private void createTables(Session session){
		if (DerbyHibernateExtensions.tableExists(session, "ct_mission_link")){ //$NON-NLS-1$
			return;
		}
		final String[] sql = new String[]{
			"CREATE TABLE smart.ct_mission_link ( CT_UUID CHAR(16) for bit data NOT NULL, MISSION_UUID CHAR(16) for bit data  NOT NULL, ct_device_id varchar(36) not null, last_observation_cnt integer, group_start_time timestamp, su_uuid char(16) for bit data, PRIMARY KEY (CT_UUID))", //$NON-NLS-1$
			
			"GRANT ALL PRIVILEGES ON smart.ct_mission_link TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.ct_mission_link TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.ct_mission_link TO MANAGER", //$NON-NLS-1$
			
			"ALTER TABLE smart.ct_mission_link ADD CONSTRAINT mission_uuid_fk FOREIGN KEY (mission_uuid) REFERENCES smart.mission ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.ct_mission_link ADD CONSTRAINT su_uuid_fk FOREIGN KEY (su_uuid) REFERENCES smart.sampling_unit ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE" //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}				
			}
		});
		HibernateManager.setPlugInVersion(SurveyCyberTrackerPlugIn.PLUGIN_ID, SurveyCyberTrackerPlugIn.DB_VERSION_1, session);
	}
}
