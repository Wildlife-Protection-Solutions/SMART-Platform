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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.patrol.PatrolProfilePlugIn;
import org.wcs.smart.i2.patrol.internal.Messages;

import com.ibm.icu.text.MessageFormat;

/**
 * Adds and/or upgrades the plugin database tables.
 * 
 * @author Emily
 *
 */
public class AddPluginJob extends Job {

	public AddPluginJob() {
		super(MessageFormat.format(Messages.AddPluginJob_jobName, PatrolProfilePlugIn.PLUGIN_ID));
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				installPlugin(session);
				session.getTransaction().commit();
			} catch (final Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						PatrolProfilePlugIn.displayLog(MessageFormat.format(Messages.AddPluginJob_ErrorMsg, ex.getMessage()), ex);
					}
				});
				return new Status(IStatus.ERROR, PatrolProfilePlugIn.PLUGIN_ID, 1, MessageFormat.format(Messages.AddPluginJob_ErrorMsg, ex.getMessage()), ex);
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	/**
	 * This function performs the installation or upgrade
	 * of the database.
	 * @param session
	 */
	public void installPlugin(Session session){
		String currentVersion = HibernateManager.getPlugInVersion(PatrolProfilePlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			//the plug is not yet installed in this database; create the tables
			createTables(session);
			currentVersion = PatrolProfilePlugIn.DB_VERSION_1;
		}
		//upgrades from version1 to the current version
		DatabaseUpgrader.upgrade(currentVersion, session);
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
