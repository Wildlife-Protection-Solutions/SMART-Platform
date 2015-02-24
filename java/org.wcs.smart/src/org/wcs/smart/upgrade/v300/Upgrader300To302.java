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
package org.wcs.smart.upgrade.v300;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;

/**
 * Upgrades from database version 300 to 301.  The only
 * change in this version is the removal of orphaned
 * observations from the patrol editor bug #1076.
 *
 * @author Emily
 *
 */
public class Upgrader300To302 implements IDatabaseUpgrader{
	
	public void upgrade(IProgressMonitor monitor) {
		monitor.subTask(Messages.Upgrader300To302_ProgressMessage);
		Session s = HibernateManager.openSession();
		try{
		s.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				try {
					c.setAutoCommit(false);
					upgrade300To301(c);
				} catch (final Exception e) {
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							SmartPlugIn.displayLog(Messages.Upgrader200To300_Error, e);
						}
					});
				} finally {
					c.setAutoCommit(true);
				}
			}
		});
		}finally{
			s.close();
		}
	}

	private static void upgrade300To301(Connection c) throws Exception {
		
		/* remove orphaned patrols */
		String sql = "delete from smart.waypoint where SOURCE = 'PATROL' " //$NON-NLS-1$
					+ "and uuid not in (select wp_uuid from smart.PATROL_WAYPOINT)"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		

		/* VERSION UDATE */ 
		sql = "update smart.db_version set version = '3.0.2' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		
		c.commit();
	}

	
}
