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
package org.wcs.smart.upgrade.v400;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Upgrade from 4.0.0 to 4.0.1
 * 
 * @author Emily
 *
 */
public class Upgrader400To401 implements IDatabaseUpgrader {
	
	private Exception thrownException = null;
	
	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.Upgrader400To401_UpgradeMsg, 1);
		thrownException = null;
		final Session s = HibernateManager.openSession();
		try{
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					try {
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
						c.setAutoCommit(true);
					} catch (final Exception e) {
						thrownException = new Exception(Messages.Upgrader400To401_UpgradeError, e);
					}
				}
			});
			
		}finally{
			s.close();
		}
		if (thrownException != null) throw thrownException;
		
		
		monitor.done();
	}
	
	private void upgrade(Connection c, Session session, IProgressMonitor monitor) throws Exception {
		@SuppressWarnings("nls")
		String[] sql = new String[]{
			"UPDATE smart.ca_projection set IS_DEFAULT = 'false' WHERE ca_uuid in (SELECT ca_uuid FROM smart.observation_options)",
			"UPDATE smart.ca_projection set IS_DEFAULT = 'true' WHERE uuid IN (SELECT view_projection_uuid FROM smart.observation_options)",
			"ALTER TABLE smart.observation_options DROP column view_projection_uuid",
			
			//TODO: need to configure triggers
			//new compound query tables
			"CREATE TABLE smart.compound_query(uuid char(16) for bit data not null, creator_uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, ca_filter varchar(32672), folder_uuid char(16) for bit data, shared boolean, id varchar(6), primary key (uuid))",
			"CREATE TABLE smart.compound_query_layer(uuid char(16) for bit data not null, compound_query_uuid char(16) for bit data not null, query_uuid char(16) for bit data not null, query_type varchar(32), style long varchar, primary key (uuid))",
			
			"ALTER TABLE SMART.COMPOUND_QUERY ADD CONSTRAINT COMPOUNDQUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE SMART.COMPOUND_QUERY ADD CONSTRAINT COMPOUNDQUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE SMART.COMPOUND_QUERY ADD CONSTRAINT COMPOUNDQUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",			
			"ALTER TABLE SMART.COMPOUND_QUERY_LAYER ADD CONSTRAINT COMPOUNDQUERYLAYER_PARENT_UUID_FK FOREIGN KEY (COMPOUND_QUERY_UUID) REFERENCES SMART.COMPOUND_QUERY(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			
			"GRANT ALL PRIVILEGES ON SMART.COMPOUND_QUERY_LAYER TO manager", //$NON-NLS-1$
			"GRANT SELECT ON SMART.COMPOUND_QUERY_LAYER TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON SMART.COMPOUND_QUERY_LAYER TO analyst", //$NON-NLS-1$
			
		};
		
		for (String s : sql){
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */ 
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V401.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

}
