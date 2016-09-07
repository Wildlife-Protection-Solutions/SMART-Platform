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
package org.wcs.smart.upgrade.v410;

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
 * Upgrade from 4.0.1 to 4.1.0
 * 
 * @author elitvin
 * @since 4.1.0
 */
public class Upgrader401To410 implements IDatabaseUpgrader {

	private Exception thrownException = null;
	
	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.Upgrader401To410_UpgradeMsg, 1);
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
						thrownException = new Exception(Messages.Upgrader401To410_UpgradeError, e);
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
			"insert into smart.PATROL_TYPE (CA_UUID, PATROL_TYPE, IS_ACTIVE, MAX_SPEED) select DISTINCT CA_UUID, 'MIXED', true, 10000 from smart.PATROL_TYPE",
		};
		
		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */ 
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V410.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}
	
}
