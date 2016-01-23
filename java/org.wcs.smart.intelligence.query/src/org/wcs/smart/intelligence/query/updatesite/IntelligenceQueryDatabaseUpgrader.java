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
package org.wcs.smart.intelligence.query.updatesite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.query.IntelligenceQueryPlugIn;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.upgrade.v400.Upgrader330To400;

/**
 * Entity query upgrade operations while upgrade/restore backup.
 * 
 * @author egouge
 * @since 3.1.0
 */
public class IntelligenceQueryDatabaseUpgrader implements IDatabaseUpgrader {

	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.IntelligenceQueryDatabaseUpgrader_InstallStatu, 1);
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			Map<String, String> versions = UpgradeEngine.getVersions(session);
			if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			String currentPluginVersion = versions.get(IntelligenceQueryPlugIn.PLUGIN_ID);
			
			if (currentPluginVersion == null) {
				(new AddIntelligenceQueriesJob()).installPlugin(session);
			}else{
				upgrade(currentPluginVersion, session);
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}
		monitor.done();
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session in active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.equals(IntelligenceQueryPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
		}
	}
	
	private static void upgradeV1ToV2(final Session session) {
		String[] sql = new String[] {
				"ALTER TABLE SMART.INTEL_RECORD_QUERY DROP CONSTRAINT INTEL_RECORD_QUERY_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_RECORD_QUERY DROP CONSTRAINT INTEL_RECORD_QUERY_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_RECORD_QUERY DROP CONSTRAINT INTEL_RECORD_QUERY_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY DROP CONSTRAINT INTEL_SUMMARY_QUERY_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY DROP CONSTRAINT INTEL_SUMMARY_QUERY_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY DROP CONSTRAINT INTEL_SUMMARY_QUERY_FOLDER_UUID_FK", //$NON-NLS-1$

				"ALTER TABLE SMART.INTEL_RECORD_QUERY ADD CONSTRAINT INTEL_RECORD_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_RECORD_QUERY ADD CONSTRAINT INTEL_RECORD_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_RECORD_QUERY ADD CONSTRAINT INTEL_RECORD_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY ADD CONSTRAINT INTEL_SUMMARY_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY ADD CONSTRAINT INTEL_SUMMARY_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY ADD CONSTRAINT INTEL_SUMMARY_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE"  //$NON-NLS-1$
		};
		
		for (String s : sql){
			session.createSQLQuery(s).executeUpdate();
		}
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				upgradeCcaaQueriesAndReports(session,connection);
			}});
		HibernateManager.setPlugInVersion(IntelligenceQueryPlugIn.PLUGIN_ID, IntelligenceQueryPlugIn.DB_VERSION_2, session);
	}
	
	private static void upgradeCcaaQueriesAndReports(Session s, Connection c) throws SQLException{
		String[] queryTables = new String[]{
				"smart.INTEL_RECORD_QUERY", //$NON-NLS-1$
				"smart.INTEL_SUMMARY_QUERY" //$NON-NLS-1$
		};
		Upgrader330To400.updateCCAAQueryTables(s, c, queryTables);
	}
}
