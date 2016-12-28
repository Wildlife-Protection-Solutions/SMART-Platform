package org.wcs.smart.upgrade.v500;

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
 * Upgrade from 4.1.0 to 5.0.0
 * 
 * @author Emily
 *
 */
public class Upgrader410To500 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.beginTask("Upgrading from 4.1.0 to 5.0.0", 1);
		thrownException = null;
		final Session s = HibernateManager.openSession();
		try {
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					try {
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
						c.setAutoCommit(true);
					} catch (final Exception e) {
						thrownException = new Exception(
								"Error upgrading from 4.1.0 to 5.0.0", e);
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

	private void upgrade(Connection c, Session session, IProgressMonitor monitor)
			throws Exception {
		@SuppressWarnings("nls")
		String[] sql = new String[] {
				"ALTER TABLE smart.employee ADD COLUMN usertemp VARCHAR(5000)",
				"UPDATE smart.employee set usertemp = case when smartuserlevel = 0 THEN 'ADMIN' when smartuserlevel = 1 THEN 'MANAGER' WHEN smartuserlevel = 2  THEN 'ANALYST' when smartuserlevel=3 THEN 'DATAENTRY' ELSE null END",
				"ALTER TABLE smart.employee DROP COLUMN smartuserlevel",
				"ALTER TABLE smart.employee ADD COLUMN smartuserlevel VARCHAR(5000)",
				"UPDATE smart.employee SET smartuserlevel = usertemp",
				"ALTER TABLE smart.employee DROP COLUMN usertemp",

	            // #1425: Enable CT to take waypoint at beginning rather than end of observation
	            "alter table smart.CONFIGURABLE_MODEL ADD COLUMN instant_gps BOOLEAN",
	            "alter table smart.CONFIGURABLE_MODEL ADD COLUMN photo_first BOOLEAN",
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}

		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V500.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

}
