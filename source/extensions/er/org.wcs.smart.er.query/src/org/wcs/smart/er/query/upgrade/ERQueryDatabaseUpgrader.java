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
package org.wcs.smart.er.query.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.updatesite.AddERQueryJob;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.upgrade.v400.Upgrader331To400;

/**
 * Ecological Records Query upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ERQueryDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.ERDatabaseUpgrader_Info, 1);
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			Map<String, String> versions = UpgradeEngine.getVersions(session);
			if (versions == null)
				throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			String currentPluginVersion = versions.get(ERQueryPlugIn.PLUGIN_ID);

			if (currentPluginVersion == null) {
				(new AddERQueryJob()).installPlugin(session);
			} else {
				upgrade(currentPluginVersion, session);
			}
			session.getTransaction().commit();
		} catch (Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		} finally { 
			session.close();
		}
		monitor.done();

	}

	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * 
	 * @param currentVersion
	 * @param session
	 *            in active transaction
	 */
	public static final void upgrade(String currentVersion, Session session) {
		if (currentVersion.equals(ERQueryPlugIn.DB_VERSION_1)) {
			upgradeV1ToV2(session);
			upgradeV2ToV3(session);
			upgradeV3ToV4(session);
		} else if (currentVersion.equals(ERQueryPlugIn.DB_VERSION_2)) {
			upgradeV2ToV3(session);
			upgradeV3ToV4(session);
		} else if (currentVersion.equals(ERQueryPlugIn.DB_VERSION_3)) {
			upgradeV3ToV4(session);
		}
	}

	private static void upgradeV1ToV2(Session session) {
		String[] sql = new String[] {
				"alter table smart.survey_observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_mission_track_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_mission_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_gridded_query add column style long varchar", //$NON-NLS-1$
				
				//fix the spelling error with the install
				"GRANT ALL PRIVILEGES ON SMART.SURVEY_OBSERVATION_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.SURVEY_WAYPOINT_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.SURVEY_GRIDDED_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.SURVEY_SUMMARY_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.SURVEY_MISSION_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.SURVEY_MISSION_TRACK_QUERY TO analyst"}; //$NON-NLS-1$

		for (String s : sql) {
			ERQueryPlugIn.log(s, null);
			session.createSQLQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(ERQueryPlugIn.PLUGIN_ID,
				ERQueryPlugIn.DB_VERSION_2, session);
	}

	private static void upgradeV2ToV3(final Session session) {
		String[] sql = new String[] {
				"ALTER TABLE SMART.SURVEY_GRIDDED_QUERY DROP CONSTRAINT SVY_GRIDDED_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_GRIDDED_QUERY DROP CONSTRAINT SVY_GRIDDED_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_GRIDDED_QUERY DROP CONSTRAINT SVY_GRIDDED_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_QUERY DROP CONSTRAINT SVY_MISSION_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_QUERY DROP CONSTRAINT SVY_MISSION_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_QUERY DROP CONSTRAINT SVY_MISSION_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_TRACK_QUERY DROP CONSTRAINT SVY_MISSION_TRACK_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_TRACK_QUERY DROP CONSTRAINT SVY_MISSION_TRACK_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_TRACK_QUERY DROP CONSTRAINT SVY_MISSION_TRACK_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_OBSERVATION_QUERY DROP CONSTRAINT SVY_OBSERVATION_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_OBSERVATION_QUERY DROP CONSTRAINT SVY_OBSERVATION_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_OBSERVATION_QUERY DROP CONSTRAINT SVY_OBSERVATION_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_SUMMARY_QUERY DROP CONSTRAINT SVY_SUMMARY_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_SUMMARY_QUERY DROP CONSTRAINT SVY_SUMMARY_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_SUMMARY_QUERY DROP CONSTRAINT SVY_SUMMARY_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_WAYPOINT_QUERY DROP CONSTRAINT SVY_WAYPOINT_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_WAYPOINT_QUERY DROP CONSTRAINT SVY_WAYPOINT_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_WAYPOINT_QUERY DROP CONSTRAINT SVY_WAYPOINT_FOLDER_UUID_FK", //$NON-NLS-1$

				"ALTER TABLE SMART.SURVEY_GRIDDED_QUERY ADD CONSTRAINT SVY_GRIDDED_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_GRIDDED_QUERY ADD CONSTRAINT SVY_GRIDDED_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_GRIDDED_QUERY ADD CONSTRAINT SVY_GRIDDED_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_QUERY ADD CONSTRAINT SVY_MISSION_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_QUERY ADD CONSTRAINT SVY_MISSION_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_QUERY ADD CONSTRAINT SVY_MISSION_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_TRACK_QUERY ADD CONSTRAINT SVY_MISSION_TRACK_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_TRACK_QUERY ADD CONSTRAINT SVY_MISSION_TRACK_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_MISSION_TRACK_QUERY ADD CONSTRAINT SVY_MISSION_TRACK_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_OBSERVATION_QUERY ADD CONSTRAINT SVY_OBSERVATION_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_OBSERVATION_QUERY ADD CONSTRAINT SVY_OBSERVATION_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_OBSERVATION_QUERY ADD CONSTRAINT SVY_OBSERVATION_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_SUMMARY_QUERY ADD CONSTRAINT SVY_SUMMARY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_SUMMARY_QUERY ADD CONSTRAINT SVY_SUMMARY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_SUMMARY_QUERY ADD CONSTRAINT SVY_SUMMARY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_WAYPOINT_QUERY ADD CONSTRAINT SVY_WAYPOINT_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_WAYPOINT_QUERY ADD CONSTRAINT SVY_WAYPOINT_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.SURVEY_WAYPOINT_QUERY ADD CONSTRAINT SVY_WAYPOINT_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE" //$NON-NLS-1$
		};

		for (String s : sql) {
			ERQueryPlugIn.log(s, null);
			session.createSQLQuery(s).executeUpdate();
		}
		session.doWork(new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				upgradeCcaaQueriesAndReports(session, connection);
			}
		});
		HibernateManager.setPlugInVersion(ERQueryPlugIn.PLUGIN_ID,
				ERQueryPlugIn.DB_VERSION_3, session);
	}

	private static void upgradeCcaaQueriesAndReports(Session s, Connection c)
			throws SQLException {
		String[] queryTables = new String[] { "smart.SURVEY_GRIDDED_QUERY", //$NON-NLS-1$
				"smart.SURVEY_MISSION_QUERY", //$NON-NLS-1$
				"smart.SURVEY_MISSION_TRACK_QUERY", //$NON-NLS-1$
				"smart.SURVEY_OBSERVATION_QUERY", //$NON-NLS-1$
				"smart.SURVEY_WAYPOINT_QUERY", //$NON-NLS-1$
				"smart.SURVEY_SUMMARY_QUERY" //$NON-NLS-1$
		};
		Upgrader331To400.updateCCAAQueryTables(s, c, queryTables);
	}

	private static void upgradeV3ToV4(Session session) {
		String[] sql = new String[] {
	            // #1366: Deactivate columns without values in queries
				"ALTER TABLE smart.survey_observation_query ADD COLUMN show_data_columns_only BOOLEAN", //$NON-NLS-1$ //SurveyObservationQuery
		};

		for (String s : sql) {
			ERQueryPlugIn.log(s, null);
			session.createSQLQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(ERQueryPlugIn.PLUGIN_ID, ERQueryPlugIn.DB_VERSION_4, session);
	}
}
