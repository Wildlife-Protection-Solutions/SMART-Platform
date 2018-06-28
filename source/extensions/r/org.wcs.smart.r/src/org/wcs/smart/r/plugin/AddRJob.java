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
package org.wcs.smart.r.plugin;

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
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.internal.Messages;

/**
 * Adds and/or upgrades the R plugin database tables.
 * 
 * @author Emily
 *
 */
public class AddRJob extends Job {

	public AddRJob() {
		super(Messages.AddRJob_JobName);
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
						RPlugIn.displayLog(Messages.AddRJob_Error + ex.getMessage(), ex);
					}
				});
				return new Status(IStatus.ERROR, RPlugIn.PLUGIN_ID, 1, Messages.AddRJob_Error + ex.getMessage(), ex);
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
		String currentVersion = HibernateManager.getPlugInVersion(RPlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			//the plug is not yet installed in this database; create the tables
			createTables(session);
			currentVersion = RPlugIn.DB_VERSION_1;
		}
		//upgrades from version1 to the current version
		RDatabaseUpgrader.upgrade(RPlugIn.DB_VERSION_1, session);
	}
	

	private void createTables(Session session){
		String[] sql = new String[]{
				//Tables
				"CREATE TABLE smart.r_script(uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data NOT NULL, filename varchar(2048) NOT NULL, creator_uuid char(16) for bit data  NOT NULL, default_parameters varchar(32672), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.r_query(uuid char(16) for bit data NOT NULL, script_uuid char(16) for bit data NOT NULL, ca_uuid char(16) for bit data not null, config varchar(32672), PRIMARY KEY (uuid))", //$NON-NLS-1$

				/* Create Foreign Keys */
				"ALTER TABLE smart.r_script ADD CONSTRAINT rscript_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.r_script ADD CONSTRAINT rscript_creatoruuid_fk FOREIGN KEY (creator_uuid) REFERENCES smart.employee(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.r_query ADD CONSTRAINT rquery_scriptuuid_fk FOREIGN KEY (script_uuid) REFERENCES smart.r_script(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.r_query ADD CONSTRAINT rquery_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.r_script TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.r_query TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.r_script TO MANAGER", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.r_query TO MANAGER", //$NON-NLS-1$
		};
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					RPlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}
			}
			
		});
		HibernateManager.setPlugInVersion(RPlugIn.PLUGIN_ID, RPlugIn.DB_VERSION_1, session);
	}
	
}
