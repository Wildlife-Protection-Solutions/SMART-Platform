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
package org.wcs.smart.intelligence.upgrade;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Upgrader from version 3.0 to 3.2
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class IntelligenceDbUpgrader30To32 implements IIntelligenceUpgrader {

	public void upgrade(Session s, IProgressMonitor monitor) {
		monitor.subTask(Messages.IntelligenceDbUpgrader30To32_ProgressMessage);
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
									Messages.IntelligenceDbUpgrader30To32_ErrorMessage, e);
						}
					});
				} finally {
					c.setAutoCommit(true);
				}
			}
		});
	}

	private void upgrade(Connection c) throws Exception {
		
		String[] sql = new String[] {
				"CREATE TABLE smart.informant (uuid CHAR(16) for bit data NOT NULL, ca_uuid CHAR(16) for bit data  NOT NULL, ID varchar(128), IS_ACTIVE BOOLEAN NOT NULL, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"ALTER TABLE smart.informant ADD CONSTRAINT informant_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"GRANT SELECT ON smart.informant to data_entry", //$NON-NLS-1$
				"GRANT SELECT ON smart.informant to manager", //$NON-NLS-1$
				"GRANT SELECT ON smart.informant to analyst", //$NON-NLS-1$
				
				"alter table smart.intelligence add column INFORMANT_UUID CHAR(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.intelligence ADD CONSTRAINT intelligence_informant_uuid_fk FOREIGN KEY (INFORMANT_UUID) REFERENCES smart.informant(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT" //$NON-NLS-1$
		};
		
		for (String s : sql){
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */ 
		String ssql = "update smart.db_version set version = '3.2' where plugin_id = 'org.wcs.smart.intelligence'"; //$NON-NLS-1$
		c.createStatement().execute(ssql);
		
		c.commit();
	}

}
