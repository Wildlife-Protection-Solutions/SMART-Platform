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
package org.wcs.smart.upgrade.v800;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * 8.0.0 to 8.0.1 upgrader
 * 
 * @author Emily
 *
 */
public class Upgrader801To810 extends AbstractInteralDatabaseUpgrader { 
	
	private Exception thrownException = null;

	private HashMap<ConservationArea, String> caTimeZoneMapping;
	
	
	public HashMap<ConservationArea, String> getCaTimeZoneMapping(){
		return this.caTimeZoneMapping;
	}
	
	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, 
				UpgradeEngine.UpgradeFromVersion.V810.fromVersion, 
				UpgradeEngine.UpgradeFromVersion.V810.toVersion));  
		
		
		try(Session s = HibernateManager.openSession()){
			
			
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						upgrade(c, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception(MessageFormat.format(Messages.Upgrader700To741_UpgradeErrorMsage, 
								UpgradeEngine.UpgradeFromVersion.V810.fromVersion, 
								UpgradeEngine.UpgradeFromVersion.V810.toVersion), e); 
					}
				}
			});
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, IProgressMonitor monitor)
			throws Exception {

		String[] sql = new String[] {		
			"CREATE TABLE smart.patrol_attribute_tree (uuid char(16) for bit data , patrol_attribute_uuid char(16) for bit data, keyid varchar(128), node_order smallint, parent_uuid char(16) for bit data, is_active boolean, hkey varchar(32672), icon_uuid char(16) for bit data, primary key (uuid))",  //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_patrol_att_uuid_fk FOREIGN KEY(patrol_attribute_uuid) REFERENCES SMART.PATROL_ATTRIBUTE (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_parent_uuid_fk FOREIGN KEY(parent_uuid) REFERENCES smart.patrol_attribute_tree (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_icon_uuid_fk FOREIGN KEY(icon_uuid) REFERENCES smart.icon (UUID) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"ALTER TABLE smart.patrol_attribute_value ADD COLUMN tree_node_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_value ADD CONSTRAINT patrol_att_value_tree_node_uuid_fk FOREIGN KEY(tree_node_uuid) REFERENCES smart.patrol_attribute_tree (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V801.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}

}
