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

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * 7.0.0 to 7.5.0 upgrader
 * 
 * @author Emily
 *
 */
public class Upgrader750To800 extends AbstractInteralDatabaseUpgrader { 
	
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, UpgradeEngine.UpgradeFromVersion.V800.fromVersion, UpgradeEngine.UpgradeFromVersion.V800.toVersion));  
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
						thrownException = new Exception(MessageFormat.format(Messages.Upgrader700To741_UpgradeErrorMsage, UpgradeEngine.UpgradeFromVersion.V800.fromVersion, UpgradeEngine.UpgradeFromVersion.V800.toVersion), e); 
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
				//add icon key to patrol attributes
				//modifications to contraints required
				"alter table smart.patrol_mandate drop constraint PATROL_MANDATE_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_mandate ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER table smart.patrol_mandate ADD CONSTRAINT pm_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"ALTER TABLE smart.patrol_transport DROP CONSTRAINT PATROL_TRANSPORT_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_transport ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER table smart.patrol_transport ADD CONSTRAINT ptransport_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.iconset DROP CONSTRAINT iconset_cauuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.icon DROP CONSTRAINT icon_cauuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.iconset ADD CONSTRAINT iconset_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.icon ADD CONSTRAINT icon_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE SMART.PATROL_MANDATE ADD CONSTRAINT PATROL_MANDATE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.patrol_transport ADD CONSTRAINT patrol_Transport_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

		};
		
		
		for (String s : sql) {
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V800.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}

}
