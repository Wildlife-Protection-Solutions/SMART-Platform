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
package org.wcs.smart.asset.query.plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * Ecological Records Query upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class AssetQueryDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return AssetQueryPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(AssetQueryPlugIn.getDefault().getBundle());
	}
	
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(AssetQueryPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(AssetQueryPlugIn.PLUGIN_ID).equals(AssetQueryPlugIn.DB_VERSION);
	}
	
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		try(Session session = HibernateManager.openSession()){
		
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(AssetQueryPlugIn.PLUGIN_ID), session);
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
	 *            in active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		//nothing to do
		if (currentVersion == null) {
			createTables(session);
			upgradeV1toV2(session);
			upgradeV2toV3(session);
		}else if (currentVersion.equals(AssetQueryPlugIn.DB_VERSION_1)) {
			upgradeV1toV2(session);
			upgradeV2toV3(session);
		}else if (currentVersion.equals(AssetQueryPlugIn.DB_VERSION_2)) {
			upgradeV2toV3(session);
		}
	}
	
	private void upgradeV1toV2(Session session) {
		String[] sql = new String[]{
			"ALTER TABLE smart.asset_summary_query ADD COLUMN query_type_key varchar (32)", //$NON-NLS-1$
			"UPDATE smart.asset_summary_query SET query_type_key = '" + AssetSummaryQuery.ASSET_SUMMARY_KEY + "'", //$NON-NLS-1$ //$NON-NLS-2$
			"ALTER TABLE smart.asset_summary_query alter column query_type_key set not null", //$NON-NLS-1$
		};
		for (String s : sql){
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(AssetQueryPlugIn.PLUGIN_ID, AssetQueryPlugIn.DB_VERSION_2, session);

	}
	
	private void upgradeV2toV3(Session session) {
		String[] sql = new String[]{
				"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY DROP COLUMN SURVEYDESIGN_KEY", //$NON-NLS-1$
		};
		for (String s : sql){
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(AssetQueryPlugIn.PLUGIN_ID, AssetQueryPlugIn.DB_VERSION_3, session);
	}

	
	private void createTables(Session session){
		final String[] sql = new String[]{
				"CREATE TABLE SMART.ASSET_OBSERVATION_QUERY(UUID CHAR(16) FOR BIT DATA NOT NULL, ID VARCHAR(6) NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL, QUERY_FILTER VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, COLUMN_FILTER VARCHAR(32672), STYLE LONG VARCHAR, SHARED BOOLEAN NOT NULL, SHOW_DATA_COLUMNS_ONLY BOOLEAN, PRIMARY KEY (UUID) )", //$NON-NLS-1$
				"CREATE TABLE SMART.ASSET_WAYPOINT_QUERY( UUID CHAR(16) FOR BIT DATA NOT NULL, ID VARCHAR(6) NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL, QUERY_FILTER VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, COLUMN_FILTER VARCHAR(32672), SURVEYDESIGN_KEY VARCHAR(128), SHARED BOOLEAN NOT NULL, STYLE LONG VARCHAR, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE SMART.ASSET_SUMMARY_QUERY(UUID CHAR(16) FOR BIT DATA NOT NULL, ID VARCHAR(6) NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL, QUERY_DEF VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA ,  SHARED BOOLEAN NOT NULL,  STYLE LONG VARCHAR, PRIMARY KEY (UUID))", //$NON-NLS-1$

				"ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD CONSTRAINT ASST_OBSERVATION_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD CONSTRAINT ASST_OBSERVATION_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD CONSTRAINT ASST_OBSERVATION_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD CONSTRAINT ASST_WAYPOINT_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD CONSTRAINT ASST_WAYPOINT_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD CONSTRAINT ASST_WAYPOINT_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD CONSTRAINT ASST_SUMMARY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",  				 //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD CONSTRAINT ASST_SUMMARY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",   				 //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD CONSTRAINT ASST_SUMMARY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",				 //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON SMART.ASSET_OBSERVATION_QUERY TO manager", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ASSET_OBSERVATION_QUERY TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_OBSERVATION_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_WAYPOINT_QUERY TO manager", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ASSET_WAYPOINT_QUERY TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_WAYPOINT_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_SUMMARY_QUERY TO manager", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ASSET_SUMMARY_QUERY TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_SUMMARY_QUERY TO analyst", //$NON-NLS-1$
			};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}	
			}
		});
		HibernateManager.setPlugInVersion(AssetQueryPlugIn.PLUGIN_ID, AssetQueryPlugIn.DB_VERSION_1, session);

	}
}
