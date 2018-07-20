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
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Scripts for upgrading from 500 to 600
 * 
 * @author Emily
 *
 */
public class Upgrader601To610 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.beginTask("Upgrading from 6.0.1 to 6.1.0", 1);
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
						thrownException = new Exception("Error upgrading from 6.0.1 to 6.1.0", e);
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
				"ALTER TABLE smart.waypoint ADD COLUMN last_modified timestamp", //$NON-NLS-1$
				"UPDATE smart.waypoint SET last_modified = datetime", //$NON-NLS-1$
				"ALTER TABLE smart.waypoint ALTER COLUMN last_modified SET NOT NULL", //$NON-NLS-1$
				"ALTER TABLE smart.waypoint ADD COLUMN last_modified_by char(16) for bit data" //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}

		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V601.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

	

}
