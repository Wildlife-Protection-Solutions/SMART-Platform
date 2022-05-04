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
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * R plugin database upgrade scripts.  Upgrades the database
 * and file store options to match the current 
 * plugin version
 * 
 * @author Emily
 * @since 3.0.0
 */
public class RDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return RPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(RPlugIn.getDefault().getBundle());
	}
	
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(RPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(RPlugIn.PLUGIN_ID).equals(RPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.RDatabaseUpgrader_UpgradeTaskName);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade( versions.get(RPlugIn.PLUGIN_ID), session);
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
		if(currentVersion == null) {
			createTables(session);
		}
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
