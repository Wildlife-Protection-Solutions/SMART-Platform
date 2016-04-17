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
package org.wcs.smart.connect.updatesite;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.upgrade.ConnectDatabaseUpgrader;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes adds entity plug-in related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddConnectJob extends Job {

	public AddConnectJob() {
		super(Messages.AddConnectJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
						
		Session session = HibernateManager.openSession();
		try{
			monitor.beginTask(Messages.AddConnectJob_TaskName, 10);
			String currentVersion = HibernateManager.getPlugInVersion(ConnectPlugIn.PLUGIN_ID, session);
			if (currentVersion == null){
				session.beginTransaction();
				try{
					installPlugin(session, true);
					HibernateManager.setPlugInVersion(ConnectPlugIn.PLUGIN_ID, ConnectPlugIn.DB_VERSION_1, session);
					session.getTransaction().commit();
				}catch(Exception ex){
					session.getTransaction().rollback();
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(),
									Messages.AddConnectJob_ErrorTitle,
									Messages.AddConnectJob_ErrorMessage);
						}
						
					});
					return new Status(Status.ERROR,ConnectPlugIn.PLUGIN_ID, Messages.AddConnectJob_ErrorLog, ex);
				}	
				currentVersion = ConnectPlugIn.DB_VERSION_1;
			}
			//run the upgrader to upgrade to the current version
			ConnectDatabaseUpgrader.upgrade(currentVersion, session);
					
		}finally{
			session.close();
		}
		return Status.OK_STATUS;
	}	
	
	/**
	 * Creates the tables and enables change logging in core of SMART. 
	 * 
	 * @param session
	 * @param installChangeTracking if true this attempts to install change log tracking
	 * for required plugins (this install for plugins that are up-to-date).  Otherwise it will not
	 * attempt to do install tracking (this case is used for upgrades where tracking is installed
	 * after all updates/install are completed).
	 * @throws Exception
	 */
	public void installPlugin(Session session, boolean installChangeTracking) throws Exception{
		createTables(session);
		ChangeLogInstaller.INSTANCE.setEnabled(true);
		if  (installChangeTracking) ChangeLogInstaller.INSTANCE.installChangeLogTracking(session);;
		HibernateManager.setPlugInVersion(ConnectPlugIn.PLUGIN_ID, ConnectPlugIn.DB_VERSION_1, session);
	}
	
	private void createTables(Session session){
		final String[] sql = new String[]{
			"CREATE TABLE SMART.CONNECT_SERVER(uuid char(16) for bit data not null, ca_uuid char(16) for bit data NOT NULL, url varchar(2064), certificate varchar(32000), PRIMARY KEY (uuid))", //$NON-NLS-1$
			"CREATE TABLE smart.connect_server_option(server_uuid char(16) for bit data not null, option_key varchar(32), value varchar(2048), PRIMARY KEY (server_uuid, option_key))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_server ADD CONSTRAINT server_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_server_option ADD CONSTRAINT cnt_svr_opt_server_fk FOREIGN KEY (server_uuid) REFERENCES smart.connect_server (uuid)   ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"CREATE TABLE smart.connect_account( employee_uuid char(16) for bit data not null, connect_uuid char(16) for bit data not null, connect_user varchar(32), connect_pass varchar(1024), primary key(employee_uuid, connect_uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_account ADD CONSTRAINT connect_employee_uuid_fk foreign key (employee_uuid) REFERENCES smart.employee (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_account ADD CONSTRAINT connect_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE",			 //$NON-NLS-1$
			"CREATE TABLE smart.connect_status(ca_uuid char(16) for bit data not null, connect_uuid char(16) for bit data not null, version char(16) for bit data, server_revision bigint not null, status varchar(6), uploadurl long varchar, localfile long varchar,primary key (ca_uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_status ADD CONSTRAINT connect_status_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_status ADD CONSTRAINT connect_status_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server (uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"CREATE TABLE smart.connect_change_log(uuid char(16) for bit data NOT NULL, revision BIGINT not null, action varchar(15) not null CONSTRAINT action_check CHECK (action in ('INSERT', 'UPDATE', 'DELETE', 'FS_INSERT', 'FS_DELETE', 'FS_UPDATE')), filename varchar(32672), tablename varchar(256), key1_fieldname varchar(256), key1 char(16) for bit data, key2_fieldname varchar(256), key2_str varchar(256), key2_uuid char(16) for bit data, ca_uuid char(16) for bit data not null, source varchar(6) default 'LOCAL', primary key (uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_change_log ADD CONSTRAINT connect_changelog_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE cascade DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"CREATE TABLE smart.connect_sync_history( uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, connect_uuid char(16) for bit data not null, datetime timestamp not null, sync_type varchar(16) not null CONSTRAINT type_check CHECK ( sync_type in ('UPLOAD', 'DOWNLOAD') ), status varchar(16) not null CONSTRAINT status_check CHECK ( status in ('ACTIVE', 'ERROR', 'DONE', 'NODATA') ), status_url varchar(32672), start_revision bigint, end_revision bigint, primary key(uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_sync_history ADD CONSTRAINT connect_sync_history_ca_uuid_fk foreign key (ca_uuid) REFERENCES smart.conservation_area(uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_sync_history ADD CONSTRAINT connect_sync_history_connect_uuid_fk foreign key (connect_uuid) REFERENCES smart.connect_server(uuid) ON UPDATE restrict ON DELETE restrict DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"CREATE INDEX connect_change_log_revision_idx on smart.connect_change_log (revision)", //$NON-NLS-1$
			
			"CREATE FUNCTION smart.uuid() returns char(16) for bit data LANGUAGE JAVA NOT deterministic external name 'org.wcs.smart.util.DerbyUtils.createUuid' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT", //$NON-NLS-1$
			"CREATE FUNCTION smart.next_revision_id(cauuid char(16) for bit data) RETURNS BIGINT LANGUAGE JAVA NOT DETERMINISTIC PARAMETER STYLE JAVA READS SQL DATA EXTERNAL NAME 'org.wcs.smart.connect.util.DerbyUtil.getNextRevisionId'", //$NON-NLS-1$
			"CREATE FUNCTION smart.is_replication_enabled_ca(cauuid char(16) for bit data) RETURNS BOOLEAN LANGUAGE JAVA NOT DETERMINISTIC PARAMETER STYLE JAVA READS SQL DATA EXTERNAL NAME 'org.wcs.smart.connect.util.DerbyUtil.isReplicationEnabled'", //$NON-NLS-1$
			
			//trigger removes rows whose ca's are not being logged
			"CREATE TRIGGER trg_change_log_insert AFTER INSERT ON smart.connect_change_log REFERENCING NEW AS new FOR EACH ROW WHEN (smart.is_replication_enabled_ca(new.ca_uuid) = false) delete from smart.connect_change_log where uuid = new.uuid", //$NON-NLS-1$
			
			"ALTER TABLE smart.connect_server ADD CONSTRAINT connect_server_ca_unq UNIQUE(ca_uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_change_log ADD CONSTRAINT revision_ca_unq UNIQUE(ca_uuid, revision) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO ANALYST", //$NON-NLS-1$
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO MANAGER", //$NON-NLS-1$
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO ADMIN", //$NON-NLS-1$
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT EXECUTE ON PROCEDURE SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY TO LOGIN", //$NON-NLS-1$
			
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO ANALYST", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO MANAGER", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO ADMIN", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY TO LOGIN", //$NON-NLS-1$
			
			"GRANT SELECT ON SMART.CONNECT_SERVER TO LOGIN", //$NON-NLS-1$
			"GRANT SELECT ON SMART.CONNECT_SERVER TO ANALYST", //$NON-NLS-1$
			"GRANT SELECT ON SMART.CONNECT_SERVER TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.CONNECT_SERVER TO MANAGER", //$NON-NLS-1$

			"GRANT SELECT ON SMART.CONNECT_SERVER_OPTION TO LOGIN", //$NON-NLS-1$
			"GRANT SELECT ON SMART.CONNECT_SERVER_OPTION TO ANALYST", //$NON-NLS-1$
			"GRANT SELECT ON SMART.CONNECT_SERVER_OPTION TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.CONNECT_SERVER_OPTION TO MANAGER", //$NON-NLS-1$
			
			"GRANT ALL PRIVILEGES ON SMART.connect_account TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_account TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_account TO MANAGER", //$NON-NLS-1$
			"GRANT SELECT ON SMART.connect_account TO LOGIN", //$NON-NLS-1$

			"GRANT ALL PRIVILEGES ON SMART.connect_status TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_status TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_status TO MANAGER", //$NON-NLS-1$

			"GRANT ALL PRIVILEGES ON SMART.connect_change_log TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_change_log TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_change_log TO MANAGER", //$NON-NLS-1$

			"GRANT ALL PRIVILEGES ON SMART.connect_sync_history TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_sync_history TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.connect_sync_history TO MANAGER", //$NON-NLS-1$

			"GRANT EXECUTE ON FUNCTION SMART.uuid TO ANALYST", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SMART.uuid TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SMART.uuid TO MANAGER", //$NON-NLS-1$

			"GRANT EXECUTE ON FUNCTION SMART.next_revision_id TO ANALYST", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SMART.next_revision_id TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SMART.next_revision_id TO MANAGER", //$NON-NLS-1$			
			
			"GRANT EXECUTE ON FUNCTION SMART.is_replication_enabled_ca TO ANALYST", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SMART.is_replication_enabled_ca TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT EXECUTE ON FUNCTION SMART.is_replication_enabled_ca TO MANAGER", //$NON-NLS-1$

			"CREATE TABLE smart.connect_alert ( UUID CHAR(16) for bit data NOT NULL, CM_UUID CHAR(16) for bit data  NOT NULL, ALERT_ITEM_UUID CHAR(16) for bit data  NOT NULL, CM_ATTRIBUTE_UUID CHAR(16) for bit data, LEVEL SMALLINT NOT NULL, TYPE VARCHAR(64), PRIMARY KEY (UUID))", //$NON-NLS-1$
			"ALTER TABLE smart.connect_alert ADD CONSTRAINT connect_alert_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.connect_alert ADD CONSTRAINT connect_alert_cm_attribute_uuid_fk FOREIGN KEY (CM_ATTRIBUTE_UUID) REFERENCES smart.cm_attribute(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

			"GRANT ALL PRIVILEGES ON smart.connect_alert TO ANALYST", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_alert TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.connect_alert TO MANAGER" //$NON-NLS-1$

		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}				
			}
		});
	}
}
