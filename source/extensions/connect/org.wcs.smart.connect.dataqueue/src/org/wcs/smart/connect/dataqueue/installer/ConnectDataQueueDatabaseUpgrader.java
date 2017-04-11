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
package org.wcs.smart.connect.dataqueue.installer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Connect upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ConnectDataQueueDatabaseUpgrader implements IDatabaseUpgrader {
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.ConnectDataQueueDatabaseUpgrader_TaskName, 1);
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			Map<String, String> versions = UpgradeEngine.getVersions(session);
			if (versions == null)
				throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			String currentPluginVersion = versions.get(ConnectDataQueuePlugin.PLUGIN_ID);

			if (currentPluginVersion == null) {
				(new AddConnectDataQueueJob()).installPlugin(session);
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
	 * 
	 * @param currentVersion
	 * @param session in active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		//nothing to do here
		if (currentVersion.equalsIgnoreCase(ConnectDataQueuePlugin.DB_VERSION_1)){
			String[] sql = new String[]{
					"ALTER TABLE smart.connect_data_queue DROP CONSTRAINT type_chk", //$NON-NLS-1$
					"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT type_chk CHECK (type IN ('PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML','JSON_CT', 'JSON_ZLIB_CT' ))" //$NON-NLS-1$
			};
			
			session.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					for (int i = 0; i < sql.length; i ++){
						c.createStatement().execute(sql[i]);
					}
				}
			});
			HibernateManager.setPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, ConnectDataQueuePlugin.DB_VERSION_2, session);
		}
	}

}
