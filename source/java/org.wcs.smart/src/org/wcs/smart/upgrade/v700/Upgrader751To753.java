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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
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
public class Upgrader751To753 implements IDatabaseUpgrader { 
	
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, UpgradeEngine.UpgradeFromVersion.V750.fromVersion, UpgradeEngine.UpgradeFromVersion.V751.toVersion));  
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
						thrownException = new Exception(MessageFormat.format(Messages.Upgrader700To741_UpgradeErrorMsage, UpgradeEngine.UpgradeFromVersion.V751.fromVersion, UpgradeEngine.UpgradeFromVersion.V753.toVersion), e); 
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
		
		//drop existing unique constraints on data_link table
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT a.constraintname "); //$NON-NLS-1$
		sb.append("FROM sys.SYSCONSTRAINTS a JOIN sys.SYSTABLES b on a.tableid = b.tableid "); //$NON-NLS-1$
		sb.append("JOIN sys.SYSSCHEMAS c on c.schemaid = b.schemaid "); //$NON-NLS-1$
		sb.append("WHERE a.type='U' and c.schemaname = 'SMART' and b.tablename = 'DATA_LINK'"); //$NON-NLS-1$
		
		try(ResultSet rs = c.createStatement().executeQuery(sb.toString())){
			while(rs.next()) {
				String name = rs.getString(1);
				
				try(Statement x = c.createStatement()){
					String s = "ALTER TABLE smart.data_link DROP CONSTRAINT \"" + name + "\""; //$NON-NLS-1$ //$NON-NLS-2$
					SmartPlugIn.logInfo(s);
					x.execute(s);
				}
			}
		}
		
		String[] sql = new String[] {
			"ALTER TABLE smart.data_link ADD CONSTRAINT provider_unq UNIQUE(provider_id, data_type)", //$NON-NLS-1$
			"ALTER TABLE smart.data_link ADD CONSTRAINT smart_id_unq UNIQUE(smart_id)" //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V753.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}
	

}
