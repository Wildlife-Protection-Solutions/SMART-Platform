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
package org.wcs.smart.upgrade.v320;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
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
public class Upgrader310To320 implements IDatabaseUpgrader {
	
	public void upgrade(Session s, IProgressMonitor monitor) {
		monitor.subTask(Messages.Upgrader310To320_ProgressMessage);
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
							SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), 
									Messages.Upgrader310To320_ErrorMessage, e);
						}
					});
				} finally {
					c.setAutoCommit(true);
				}
			}
		});
	}

	private static void upgrade(Connection c) throws Exception {
		
		String[] sql = new String[]{
				"alter table smart.obs_waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.obs_observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.obs_gridded_query add column style long varchar",  //$NON-NLS-1$
				
				"alter table smart.observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.patrol_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.gridded_query add column style long varchar", //$NON-NLS-1$
				
				"create table smart.map_styles (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, style_string long varchar not null, primary key (uuid))", //$NON-NLS-1$
				"alter table smart.map_styles add constraint mapstyle_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$

				"alter table smart.cm_attribute_tree_node add column cm_attribute_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node add column dm_attribute_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node add column parent_uuid CHAR(16) FOR BIT DATA", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node add column node_order SMALLINT", //$NON-NLS-1$

				"UPDATE smart.cm_attribute_tree_node SET NODE_ORDER=0", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node alter column DM_TREE_NODE_UUID NULL" //$NON-NLS-1$
				
		};
		
		for (String s : sql){
			c.createStatement().execute(s);
		}
		/* VERSION UDATE */ 
		String ssql = "update smart.db_version set version = '3.2.0' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$
		c.createStatement().execute(ssql);
		
		c.commit();
	}

	
}
