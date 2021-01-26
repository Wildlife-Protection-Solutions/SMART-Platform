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

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.survey.SurveyCyberTrackerPlugIn;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.er.upgrade.ERDatabaseUpgrader;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Connect upgrade operations while upgrade/restore backup.
 * 
 * @author egouge
 * 
 */
public class DataQueueCtMissionDatabaseUpgrader implements IDatabaseUpgrader {
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DataQueueCtMissionDatabaseUpgrader_UpgradeTask, 2);
		progress.subTask( Messages.DataQueueCtMissionDatabaseUpgrader_UpgradeTask);
		
		//we need to ensure the mission table is installed first
		(new ERDatabaseUpgrader()).upgrade(progress.split(1));
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				
				if (versions == null)
					throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
				String currentPluginVersion = versions.get(SurveyCyberTrackerPlugIn.PLUGIN_ID);
	
				if (currentPluginVersion == null) {
					(new AddDataQueueCtMissionJob()).installPlugin(session);
				} else {
					upgrade(currentPluginVersion, session);
				}
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
		progress.worked(1);
	}
	
	/**
	 * 
	 * @param currentVersion
	 * @param session in active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.contentEquals(SurveyCyberTrackerPlugIn.DB_VERSION_1)) {
			upgradeV1ToV2(session);
		}
	}
	
	private static final void upgradeV1ToV2(Session session) {
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
			session.createNativeQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(SurveyCyberTrackerPlugIn.PLUGIN_ID, SurveyCyberTrackerPlugIn.DB_VERSION_2, session);
	}

}
