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
package org.wcs.smart.upgrade.v200;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Upgrade SMART from version 1.0.0 to 2.0.0.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class Upgrader112To200 {

	public static void upgrade(Session s, IProgressMonitor monitor) {
		monitor.subTask("Upgrading from 1.x.x to 2.x.x");
		s.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				try {
					c.setAutoCommit(false);
					upgrade112To200(c);
				} catch (final Exception e) {
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), "Error upgrading from 1.x.x to 2.x.x", e);
						}
					});
				} finally {
					c.setAutoCommit(true);
				}
			}
		});
	}

	public static void upgrade112To200(Connection c) throws Exception {
		
		InputStream in = Upgrader112To200.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v200/version_2.0.0.sql"); //$NON-NLS-1$
		UpgradeEngine.runScript(c, in);
		
		in = Upgrader112To200.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v200/smart-tables-dataentry.sql"); //$NON-NLS-1$
		UpgradeEngine.runScript(c, in);			
		
		//generate keys for required new fields
		KeyGenerator kg = new KeyGenerator();
		kg.generateKeys(c, "smart.patrol_mandate"); //$NON-NLS-1$
		kg.generateKeys(c, "smart.team"); //$NON-NLS-1$
		kg.generateKeys(c, "smart.patrol_transport"); //$NON-NLS-1$
		
		c.commit();
	}

	public static void upgradeCt111to200(Connection c) throws Exception{
		
		String sql = "select count(*) from sys.SYSTABLES a join sys.SYSSCHEMAS b on a.schemaid = b.schemaid WHERE a.tablename='CYBERTRACKER_PROPERTIES' and b.schemaname='SMART'";
		ResultSet rs = c.createStatement().executeQuery(sql);
		rs.next();
		int cnt = rs.getInt(1);
		rs.close();
		
		if (cnt > 0) {
			// ct is installed and needs to be updated
			InputStream in = Upgrader112To200.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v200/ct_11x_200.sql"); //$NON-NLS-1$
			UpgradeEngine.runScript(c, in);
		}
		
	}
	
}
