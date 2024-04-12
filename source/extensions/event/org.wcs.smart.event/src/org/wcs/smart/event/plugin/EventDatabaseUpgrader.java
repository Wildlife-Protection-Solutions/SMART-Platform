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
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * Event upgrade operations while upgrade/restore backup.
 * 
 * @author Emily
 */
public class EventDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return EventPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(EventPlugIn.getDefault().getBundle());
	}
	
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(EventPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(EventPlugIn.PLUGIN_ID).equals(EventPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(EventPlugIn.PLUGIN_ID), session);
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
			upgradev1tov2(session);
		}else if (currentVersion.equalsIgnoreCase(EventPlugIn.DB_VERSION_1)) {
			upgradev1tov2(session);
		}
	}
	
	private void upgradev1tov2(Session session) {
		//create profile parameters for intel events
		StringBuilder sb = new StringBuilder();
		sb.append("insert into smart.e_action_parameter_value(action_uuid, parameter_key, parameter_value)"); //$NON-NLS-1$
		sb.append("select uuid, 'org.wcs.smart.profile.common.profile', 'profile1'"); //$NON-NLS-1$
		sb.append(" FROM smart.e_action where type_key in ('org.wcs.smart.profile.newrecord', 'org.wcs.smart.profile.i2.newentity')"); //$NON-NLS-1$
		
		session.createNativeMutationQuery(sb.toString()).executeUpdate();
		
		HibernateManager.setPlugInVersion(EventPlugIn.PLUGIN_ID, EventPlugIn.DB_VERSION_2, session);
	}
		
	
	private void createTables(Session session){
		String[] sql = new String[]{
			"CREATE TABLE smart.e_event_filter(uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, id varchar(128) not null, filter_string varchar(32000) not null, PRIMARY KEY(uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.e_event_filter ADD CONSTRAINT eeventfilter_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
			"GRANT SELECT ON smart.e_event_filter TO ANALYST",  //$NON-NLS-1$
			"GRANT SELECT ON smart.e_event_filter TO DATA_ENTRY",  //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.e_event_filter TO MANAGER", //$NON-NLS-1$

			"CREATE TABLE smart.e_action( uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, id varchar(128) not null, type_key varchar(128) not null, PRIMARY KEY (uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.e_action ADD CONSTRAINT eaction_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"GRANT SELECT ON smart.e_action TO ANALYST", //$NON-NLS-1$
			"GRANT SELECT ON smart.e_action TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.e_action TO MANAGER", //$NON-NLS-1$


			"CREATE TABLE smart.e_action_parameter_value( action_uuid char(16) for bit data not null, parameter_key varchar(128)  not null, parameter_value varchar(4096) not null, PRIMARY KEY (action_uuid, parameter_key) )", //$NON-NLS-1$
			"ALTER TABLE smart.e_action_parameter_value ADD CONSTRAINT eactionparametervalue_actionuuid_fk FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"GRANT SELECT ON smart.e_action_parameter_value TO ANALYST", //$NON-NLS-1$
			"GRANT SELECT ON smart.e_action_parameter_value TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.e_action_parameter_value TO MANAGER", //$NON-NLS-1$


			"CREATE TABLE smart.e_event_action(uuid char(16) for bit data not null, filter_uuid char(16) for bit data not null, action_uuid char(16) for bit data not null, is_enabled boolean not null default true, PRIMARY KEY (uuid) )", //$NON-NLS-1$
			"ALTER TABLE smart.e_event_action ADD CONSTRAINT eeventaction_actionuuid_fk FOREIGN KEY (action_uuid) REFERENCES smart.e_action(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.e_event_action ADD CONSTRAINT eeventaction_filteruuid_fk FOREIGN KEY (filter_uuid) REFERENCES smart.e_event_filter(uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"GRANT SELECT ON smart.e_event_action TO ANALYST", //$NON-NLS-1$
			"GRANT SELECT ON smart.e_event_action TO DATA_ENTRY", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.e_event_action TO MANAGER", //$NON-NLS-1$

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
