/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.i2.patrol.plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.patrol.PatrolProfilePlugIn;
import org.wcs.smart.i2.patrol.internal.Messages;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * database upgrade scripts.  Upgrades the database
 * and file store options to match the current plugin version
 * 
 * @author Emily
 */
public class DatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return PatrolProfilePlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(PatrolProfilePlugIn.getDefault().getBundle());
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(PatrolProfilePlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(PatrolProfilePlugIn.PLUGIN_ID).equals(PatrolProfilePlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(Messages.DatabaseUpgrader_UpgradeMessage, SmartPatrolPlugIn.PLUGIN_ID));
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(PatrolProfilePlugIn.PLUGIN_ID), session);				
				session.getTransaction().commit();
			}catch (Exception ex){
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
	 * @param session is active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			return;
		}
	}
		

	
	private void createTables(Session session){
		String[] sql = new String[]{
				//Tables
				"CREATE TABLE smart.i_patrol_record_motivation(patrol_uuid char(16) for bit data NOT NULL, i_record_uuid char(16) for bit data NOT NULL, PRIMARY KEY (i_record_uuid, patrol_uuid))", //$NON-NLS-1$

				"ALTER TABLE smart.i_patrol_record_motivation ADD CONSTRAINT i_patrol_record_motivation_patrol_fk FOREIGN KEY (patrol_uuid) REFERENCES smart.patrol(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_patrol_record_motivation ADD CONSTRAINT i_patrol_record_motivation_record_fk FOREIGN KEY (i_record_uuid) REFERENCES smart.i_record(uuid) ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.i_patrol_record_motivation TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.i_patrol_record_motivation TO MANAGER", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.i_patrol_record_motivation TO DATA_ENTRY", //$NON-NLS-1$
		};
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					PatrolProfilePlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}
			}
			
		});
		HibernateManager.setPlugInVersion(PatrolProfilePlugIn.PLUGIN_ID, PatrolProfilePlugIn.DB_VERSION_1, session);
	}
}
