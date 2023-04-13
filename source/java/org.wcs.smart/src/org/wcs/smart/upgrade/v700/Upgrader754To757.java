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
package org.wcs.smart.upgrade.v700;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * 7.0.0 to 7.5.0 upgrader
 * 
 * @author Emily
 *
 */
public class Upgrader754To757 implements IDatabaseUpgrader { 
	
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, UpgradeEngine.UpgradeFromVersion.V754.fromVersion, UpgradeEngine.UpgradeFromVersion.V757.toVersion));  
		thrownException = null;
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
						thrownException = new Exception(MessageFormat.format(Messages.Upgrader700To741_UpgradeErrorMsage, UpgradeEngine.UpgradeFromVersion.V754.fromVersion, UpgradeEngine.UpgradeFromVersion.V757.toVersion), e); 
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

		//fix for incorrect icon associations
		//https://app.assembla.com/spaces/smart-cs/tickets/realtime_list?ticket=3518
		String[] sql = new String[] {
			"CREATE TABLE tempfix (list_item_uuid char(16) FOR bit data, correct_icon_uuid char(16) FOR bit data)", //$NON-NLS-1$
			"INSERT INTO tempfix (list_item_uuid, correct_icon_uuid) select a.uuid AS list_item_uuid,  d.uuid AS correct_icon_uuid from smart.dm_attribute_list a join smart.dm_attribute b on a.attribute_uuid = b.uuid join smart.icon c on c.uuid = a.icon_uuid  LEFT JOIN smart.icon d ON d.keyid = c.keyid AND d.ca_uuid = b.ca_uuid where b.ca_uuid != c.ca_uuid", //$NON-NLS-1$
			"UPDATE smart.dm_attribute_list SET icon_uuid = (SELECT e.correct_icon_uuid FROM tempfix e WHERE e.list_item_uuid  = smart.dm_attribute_list.uuid) WHERE uuid in (select list_item_uuid from tempfix)", //$NON-NLS-1$
			"drop table tempfix", //$NON-NLS-1$
			
			//quick links
			"CREATE TABLE smart.quicklink(uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, url varchar(32000), uiorder smallint, employee_uuid char(16) for bit data, primary key (uuid) )", //$NON-NLS-1$
			"ALTER TABLE smart.quicklink add constraint quicklink_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
			"ALTER TABLE smart.quicklink add constraint quicklink_employeeuuid_fk FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON  smart.quicklink to data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON  smart.quicklink to analyst", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON  smart.quicklink to manager", //$NON-NLS-1$
		};	

		for (String s : sql) {
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V757.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}
}
