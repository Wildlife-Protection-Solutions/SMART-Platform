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
package org.wcs.smart.connect.cybertracker.updatesite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.cybertracker.ConnectCtPlugIn;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Connect upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCtDatabaseUpgrader implements IDatabaseUpgrader {
	
	@Override
	public String getPluginId() {
		return ConnectCtPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(ConnectCtPlugIn.getDefault().getBundle());
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(ConnectCtPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(ConnectCtPlugIn.PLUGIN_ID).equals(ConnectCtPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.ConnectCtDatabaseUpgrader_UpgageTaskName);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(ConnectCtPlugIn.PLUGIN_ID), session);
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
			upgradeV1toV2(session);
		}else if (currentVersion.equalsIgnoreCase(ConnectCtPlugIn.DB_VERSION_1)){
			upgradeV1toV2(session);
		}
	}

	private void upgradeV1toV2(Session session) {
		String[] sql = new String[]{
				"ALTER TABLE smart.connect_ct_properties add column data_frequency INTEGER", //$NON-NLS-1$
				"ALTER TABLE smart.connect_ct_properties add column ping_type char(16) for bit data" //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}
			}
		});
		HibernateManager.setPlugInVersion(ConnectCtPlugIn.PLUGIN_ID, ConnectCtPlugIn.DB_VERSION_2, session);
	}
	
	private void createTables(Session session){
		final String[] sql = new String[]{
			"CREATE TABLE smart.connect_alert ( UUID CHAR(16) for bit data NOT NULL, CM_UUID CHAR(16) for bit data  NOT NULL, ALERT_ITEM_UUID CHAR(16) for bit data  NOT NULL, CM_ATTRIBUTE_UUID CHAR(16) for bit data, LEVEL SMALLINT NOT NULL, TYPE VARCHAR(64), PRIMARY KEY (UUID))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_alert ADD CONSTRAINT connect_alert_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_alert ADD CONSTRAINT connect_alert_cm_attribute_uuid_fk FOREIGN KEY (CM_ATTRIBUTE_UUID) REFERENCES smart.cm_attribute(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

			"GRANT ALL PRIVILEGES ON smart.connect_alert TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_alert TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_alert TO MANAGER", //$NON-NLS-1$

			"CREATE TABLE smart.connect_ct_properties ( UUID CHAR(16) for bit data NOT NULL, CM_UUID CHAR(16) for bit data  NOT NULL, PING_FREQUENCY INTEGER, PRIMARY KEY (UUID))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_ct_properties ADD CONSTRAINT connect_ct_properties_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

			"GRANT ALL PRIVILEGES ON smart.connect_ct_properties TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_ct_properties TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_ct_properties TO MANAGER" //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}				
			}
		});
		HibernateManager.setPlugInVersion(ConnectCtPlugIn.PLUGIN_ID, ConnectCtPlugIn.DB_VERSION_1, session);
	}

}
