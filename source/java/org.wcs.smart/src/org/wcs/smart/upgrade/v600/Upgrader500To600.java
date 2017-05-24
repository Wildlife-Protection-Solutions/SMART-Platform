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

public class Upgrader500To600 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.Upgrader500To600_ProgressMessage, 1);
		thrownException = null;
		final Session s = HibernateManager.openSession();
		try {
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					try {
						c.setAutoCommit(false);
						upgrade(c, monitor);
						c.setAutoCommit(true);
					} catch (final Exception e) {
						thrownException = new Exception(Messages.Upgrader500To600_ErrorMessage, e);
					}
				}
			});

		} finally {
			s.close();
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, IProgressMonitor monitor)
			throws Exception {

		String[] sql = new String[] {
				"ALTER TABLE smart.patrol_leg ADD COLUMN mandate_uuid char(16) for bit data", //$NON-NLS-1$
				"UPDATE smart.patrol_leg SET mandate_uuid = (SELECT p.mandate_uuid FROM smart.patrol p WHERE p.uuid = smart.patrol_leg.patrol_uuid)", //$NON-NLS-1$
				"ALTER TABLE SMART.PATROL_LEG ADD CONSTRAINT MANDATE_UUID_FK FOREIGN KEY (MANDATE_UUID) REFERENCES SMART.PATROL_MANDATE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_leg ALTER COLUMN mandate_uuid SET NOT NULL", //$NON-NLS-1$
				"ALTER TABLE smart.patrol DROP COLUMN mandate_uuid", //$NON-NLS-1$

		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}

		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V600.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

}
