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
package org.wcs.smart.event.plugin;

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
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Adds and or upgrades event plugin
 * 
 * @author Emily
 *
 */
public class AddEventJob extends Job {

	public AddEventJob() {
		super("Install/update event plugin");
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
						EventPlugIn.displayLog("Error installing plugin: " + ex.getMessage(), ex);
					}
				});
				return new Status(IStatus.ERROR, EventPlugIn.PLUGIN_ID, 1, "Error installing plugin: " + ex.getMessage(), ex);
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	public void installPlugin(Session session){
		String currentVersion = HibernateManager.getPlugInVersion(EventPlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			createTables(session);
			currentVersion = EventPlugIn.DB_VERSION_1;
		}
		
		EventDatabaseUpgrader.upgrade(EventPlugIn.DB_VERSION_1, session);
	}
	
	@SuppressWarnings("nls")
	private void createTables(Session session){
		String[] sql = new String[]{
			"CREATE TABLE smart.e_event_filter(uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, id varchar(128) not null, filter_string varchar(32000) not null, PRIMARY KEY(uuid))",
			"ALTER TABLE smart.e_event_filter ADD CONSTRAINT eeventfilter_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE", 
			"GRANT SELECT ON smart.e_event_filter TO ANALYST", 
			"GRANT SELECT ON smart.e_event_filter TO DATA_ENTRY", 
			"GRANT ALL PRIVILEGES ON smart.e_event_filter TO MANAGER",

			"CREATE TABLE smart.e_action( uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, id varchar(128) not null, type_key varchar(128) not null, PRIMARY KEY (uuid))",
			"ALTER TABLE smart.e_action ADD CONSTRAINT eaction_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE",
			"GRANT SELECT ON smart.e_action TO ANALYST",
			"GRANT SELECT ON smart.e_action TO DATA_ENTRY",
			"GRANT ALL PRIVILEGES ON smart.e_action TO MANAGER",


			"CREATE TABLE smart.e_action_parameter_value( action_uuid char(16) for bit data not null, parameter_key varchar(128)  not null, parameter_value varchar(4096) not null, PRIMARY KEY (action_uuid, parameter_key) )",
			"ALTER TABLE smart.e_action_parameter_value ADD CONSTRAINT eactionparametervalue_actionuuid_fk FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) DEFERRABLE INITIALLY IMMEDIATE",
			"GRANT SELECT ON smart.e_action_parameter_value TO ANALYST",
			"GRANT SELECT ON smart.e_action_parameter_value TO DATA_ENTRY",
			"GRANT ALL PRIVILEGES ON smart.e_action_parameter_value TO MANAGER",


			"CREATE TABLE smart.e_event_action(uuid char(16) for bit data not null, filter_uuid char(16) for bit data not null, action_uuid char(16) for bit data not null, is_enabled boolean not null default true, PRIMARY KEY (uuid) )",
			"ALTER TABLE smart.e_event_action ADD CONSTRAINT eeventaction_actionuuid_fk FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE smart.e_event_action ADD CONSTRAINT eeventaction_filteruuid_fk FOREIGN KEY (filter_uuid) REFERENCES smart.e_event_filter(uuid) DEFERRABLE INITIALLY IMMEDIATE",
			"GRANT SELECT ON smart.e_event_action TO ANALYST",
			"GRANT SELECT ON smart.e_event_action TO DATA_ENTRY",
			"GRANT ALL PRIVILEGES ON smart.e_event_action TO MANAGER",

		};
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					EventPlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}
			}
			
		});
		HibernateManager.setPlugInVersion(EventPlugIn.PLUGIN_ID, EventPlugIn.DB_VERSION_1, session);
	}
	
}
