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
package org.wcs.smart.upgrade.v321;

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
 * Upgrades from database version 320 to 321.
 *
 * @author elitvin
 * @since 3.2.0
 */
public class Upgrader320To321 implements IDatabaseUpgrader {
	
	public void upgrade(final IProgressMonitor monitor) {
		monitor.subTask(Messages.Upgrader320To321_ProcessMessage);
		final Session s = HibernateManager.openSession();
		try{
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					try {
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
					} catch (final Exception e) {
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.Upgrader320To321_Error, e);
							}
						});
					} finally {
						c.setAutoCommit(true);
					}
				}
			});
			
			monitor.subTask(Messages.Upgrader320To321_ProcessMessage + ": " + Messages.Upgrader320To321_UpdateDbStructure); //$NON-NLS-1$
			CmUpgrader320To321 cmUpgrader = new CmUpgrader320To321();
			cmUpgrader.upgrade(s);
			
		}finally{
			s.close();
		}
	}

	private void upgrade(Connection c, Session session, IProgressMonitor monitor) throws Exception {
		String[] sql = new String[]{
				"alter table smart.cm_attribute_list add column cm_attribute_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_list add column dm_attribute_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_list add column list_order SMALLINT", //$NON-NLS-1$

				"UPDATE smart.cm_attribute_list SET list_order=0", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_list ADD CONSTRAINT cm_attribute_list_cm_attribute_uuid_fk FOREIGN KEY (CM_ATTRIBUTE_UUID) REFERENCES smart.cm_attribute (UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_list ADD CONSTRAINT cm_attribute_list_dm_attribute_uuid_fk FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES smart.dm_attribute (UUID) ON UPDATE RESTRICT ON DELETE CASCADE" //$NON-NLS-1$
		};
		
		for (String s : sql){
			c.createStatement().execute(s);
		}
				
		/* VERSION UDATE */ 
		String ssql = "update smart.db_version set version = '3.2.1' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$
		c.createStatement().execute(ssql);
		
		c.commit();
	}
	
}
