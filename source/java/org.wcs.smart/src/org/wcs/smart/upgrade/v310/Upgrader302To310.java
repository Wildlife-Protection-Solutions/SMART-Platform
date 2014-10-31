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
package org.wcs.smart.upgrade.v310;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;

/**
 * Upgrades from database version 300 to 301.  The only
 * change in this version is the removal of orphaned
 * observations from the patrol editor bug #1076.
 *
 * @author Emily
 *
 */
public class Upgrader302To310 {
	
	public static void upgrade(Session s, IProgressMonitor monitor) {
		monitor.subTask(Messages.Upgrader302To310_progresslabel);
		s.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				try {
					c.setAutoCommit(false);
					upgrade(c);
				} catch (final Exception e) {
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.Upgrader200To300_Error, e);
						}
					});
				} finally {
					c.setAutoCommit(true);
				}
			}
		});
	}

	private static void upgrade(Connection c) throws Exception {
		
		/* add employee to observation table */
		String sql = "alter table smart.wp_observation add column employee_uuid char(16) for bit data"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		
		sql = "alter table smart.wp_observation add constraint obs_employee_uuid_fk FOREIGN KEY (employee_uuid) references smart.employee(uuid) on UPDATE RESTRICT ON DELETE RESTRICT"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		
		/* add observer options */
		sql = "alter table smart.observation_options add column observer boolean"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		
		sql = "update smart.observation_options set observer = false"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		
		/* configurable model */
		sql = "grant select on smart.configurable_model to analyst"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		
		/* VERSION UDATE */ 
		sql = "update smart.db_version set version = '3.1.0' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$
		c.createStatement().execute(sql);
		
		c.commit();
	}

	
}
