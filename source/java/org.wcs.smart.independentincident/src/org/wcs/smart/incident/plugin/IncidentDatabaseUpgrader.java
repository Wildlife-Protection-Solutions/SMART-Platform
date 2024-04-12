/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.incident.plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * Installer/upgrader for incident plugin
 * @since 8.0.0
 */
public class IncidentDatabaseUpgrader implements IDatabaseUpgrader {

	public IncidentDatabaseUpgrader() {
	}

	@Override
	public String getPluginId() {
		return IncidentPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(IncidentPlugIn.getDefault().getBundle());
	}

	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(IncidentPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(IncidentPlugIn.PLUGIN_ID).equals(IncidentPlugIn.DB_VERSION);
		
	}

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(IncidentPlugIn.PLUGIN_ID), session);
				
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
	 * @param currentVersion
	 * @param session is active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
		}
	}
	
	private void createTables(Session session) {
		
		Integer cnt = session.createNativeQuery("select count(*) from sys.systables where tablename = 'INCIDENT_WAYPOINT'", Integer.class).uniqueResult(); //$NON-NLS-1$
		if (cnt == 0) {
			String[] sql = new String[] {
				"create table smart.incident_waypoint(wp_uuid char(16) for bit data not null, patrol_uuid char(16) for bit data, primary key (wp_uuid) )", //$NON-NLS-1$
				"ALTER TABLE smart.incident_waypoint add constraint incident_wp_wpuuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
				"ALTER TABLE smart.incident_waypoint add constraint incident_wp_patroluuid_fk FOREIGN KEY (patrol_uuid) REFERENCES smart.patrol(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
			};
			session.doWork(new Work(){
				@Override
				public void execute(Connection connection) throws SQLException {
					for (String s : sql){
						IncidentPlugIn.log(s, null);
						connection.createStatement().executeUpdate(s);
					}
				}
				
			});
		}
		HibernateManager.setPlugInVersion(IncidentPlugIn.PLUGIN_ID, IncidentPlugIn.DB_VERSION_1, session);
	}
}
