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
	public String getPluginId() {
		return ConnectDataQueuePlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(ConnectDataQueuePlugin.getDefault().getBundle());
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(ConnectDataQueuePlugin.PLUGIN_ID)) return false;
		return currentVersions.get(ConnectDataQueuePlugin.PLUGIN_ID).equals(ConnectDataQueuePlugin.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.ConnectDataQueueDatabaseUpgrader_TaskName);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(ConnectDataQueuePlugin.PLUGIN_ID), session);
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
		//nothing to do here
		if (currentVersion == null) {
			createTables(session);
			upgradeV1toV2(session);
			upgradeV2toV3(session);
		}else if (currentVersion.equalsIgnoreCase(ConnectDataQueuePlugin.DB_VERSION_1)){
			upgradeV1toV2(session);
			upgradeV2toV3(session);
		}else if (currentVersion.equalsIgnoreCase(ConnectDataQueuePlugin.DB_VERSION_2)) {
			upgradeV2toV3(session);
		}
	}

	private void upgradeV2toV3(Session session) {
		String[] sql = new String[]{
				"ALTER TABLE smart.connect_data_queue DROP CONSTRAINT type_chk", //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					ConnectDataQueuePlugin.log(sql[i], null);
					c.createStatement().execute(sql[i]);
				}
			}
		});
		HibernateManager.setPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, ConnectDataQueuePlugin.DB_VERSION_3, session);
	}

	private void upgradeV1toV2(Session session) {
		String[] sql = new String[]{
				"ALTER TABLE smart.connect_data_queue DROP CONSTRAINT type_chk", //$NON-NLS-1$
				"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT type_chk CHECK (type IN ('PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML','JSON_CT', 'JSON_ZLIB_CT' ))" //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					ConnectDataQueuePlugin.log(sql[i], null);
					c.createStatement().execute(sql[i]);
				}
			}
		});
		HibernateManager.setPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, ConnectDataQueuePlugin.DB_VERSION_2, session);		
	}

	private void createTables(Session session){
		final String[] sql = new String[]{
			
			"CREATE TABLE smart.connect_data_queue(uuid char(16) for bit data NOT NULL, type VARCHAR(32) NOT NULL, ca_uuid char(16) for bit data NOT NULL, name VARCHAR(4096),status varchar(32) NOT NULL,queue_order integer,error_message VARCHAR(8192),local_file varchar(4096), date_processed timestamp, server_item_uuid char(16) for bit data NOT NULL,PRIMARY KEY (uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT data_queue_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT status_chk CHECK (status IN ('DOWNLOADING', 'QUEUED', 'REQUEUED', 'PROCESSING', 'COMPLETE', 'COMPLETE_WARN', 'ERROR'))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_data_queue ADD CONSTRAINT type_chk CHECK (type IN ('PATROL_XML', 'INCIDENT_XML', 'MISSION_XML', 'INTELL_XML'))", //$NON-NLS-1$
			
			"CREATE TABLE smart.connect_data_queue_option(ca_uuid  char(16) for bit data not null, keyid varchar(256) NOT NULL, value varchar(512), primary key (ca_uuid, keyid))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_data_queue_option ADD CONSTRAINT data_queue_option_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue TO MANAGER", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue TO ANALYST", //$NON-NLS-1$
			
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue_option TO MANAGER", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue_option TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_data_queue_option TO ANALYST", //$NON-NLS-1$
		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					ConnectDataQueuePlugin.log(sql[i], null);
					c.createStatement().execute(sql[i]);
				}				
			}
		});
		HibernateManager.setPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, ConnectDataQueuePlugin.DB_VERSION_1, session);
	}
	
}
