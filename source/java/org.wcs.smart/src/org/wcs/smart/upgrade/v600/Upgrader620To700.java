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
package org.wcs.smart.upgrade.v600;

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

public class Upgrader620To700 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.Upgrader620To700_TaskName); 
		thrownException = null;
		try(Session s = HibernateManager.openSession()){
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception(Messages.Upgrader620To700_TaskError, e); 
					}
				}
			});
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, Session session, IProgressMonitor monitor)
			throws Exception {

		String[] sql = new String[] {
				"CREATE TABLE smart.employee_team (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, primary key (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.employee_team_member (employee_uuid char(16) for bit data not null, team_uuid char(16) for bit data not null, primary key(employee_uuid, team_uuid))", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.employee_team TO admin,manager,data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.employee_team_member TO admin,manager,data_entry", //$NON-NLS-1$

				"ALTER TABLE smart.employee_team ADD CONSTRAINT employeeteam_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.employee_team_member ADD CONSTRAINT employeeteammem_euuid FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.employee_team_member ADD CONSTRAINT employeeteammem_tuuid FOREIGN KEY (team_uuid) REFERENCES smart.employee_team(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$				
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V700.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

}
