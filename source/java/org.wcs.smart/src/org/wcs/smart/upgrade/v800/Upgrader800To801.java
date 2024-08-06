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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.wcs.smart.util.UuidUtils;

/**
 * 8.0.0 to 8.0.1 upgrader
 * 
 * @author Emily
 *
 */
public class Upgrader800To801 extends AbstractInteralDatabaseUpgrader { 
	
	private Exception thrownException = null;

	private HashMap<ConservationArea, String> caTimeZoneMapping;
	
	
	public HashMap<ConservationArea, String> getCaTimeZoneMapping(){
		return this.caTimeZoneMapping;
	}
	
	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, 
				UpgradeEngine.UpgradeFromVersion.V801.fromVersion, 
				UpgradeEngine.UpgradeFromVersion.V801.toVersion));  
		
		
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
								UpgradeEngine.UpgradeFromVersion.V801.fromVersion, 
								UpgradeEngine.UpgradeFromVersion.V801.toVersion), e); 
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
			"ALTER TABLE smart.cm_node DROP COLUMN use_single_gps_point",  //$NON-NLS-1$
		};
		
		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		
		//complicated fix to resolve the problem with the zero employee uuid I introduced in SMART8.0.0 upgrade script
		//only run this code if there is a employee with one uuid
		boolean hasOne = false;
		try(PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM smart.employee where uuid = ?")){ //$NON-NLS-1$
			ps.setBytes(1,  UuidUtils.uuidToByte(UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR)));
			try(ResultSet rs = ps.executeQuery()){
				rs.next();
				hasOne = rs.getInt(1) > 0;
			}
		}
			
		if (hasOne) {

			String s = """
					INSERT INTO smart.employee (uuid, ca_uuid, id, givenname, familyname, startemploymentdate, endemploymentdate, datecreated, birthdate, gender, smartuserid, smartpassword, agency_uuid, rank_uuid, smartuserlevel) 
					SELECT ?, ca_uuid, id, givenname, familyname, startemploymentdate, endemploymentdate, datecreated, birthdate, gender, smartuserid, smartpassword, agency_uuid, rank_uuid, smartuserlevel
					FROM smart.employee where uuid = ?				
					"""; //$NON-NLS-1$
			try(PreparedStatement ps = c.prepareStatement(s)){
				ps.setBytes(1,  UuidUtils.uuidToByte(UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR)));
				ps.setBytes(2,  UuidUtils.uuidToByte(UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR)));
				ps.execute();
			}
			
	
			try(ResultSet rs = c.getMetaData().getExportedKeys(null, "SMART", "EMPLOYEE")){ //$NON-NLS-1$ //$NON-NLS-2$
				while(rs.next()) {
					String schema = rs.getString(6);
					String table = rs.getString(7);
					String field = rs.getString(8);
					
					String query = "UPDATE " + schema + "." + table + " SET " + field + " = ? where " + field + " = ?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					try(PreparedStatement ps = c.prepareStatement(query)){
						ps.setBytes(1,  UuidUtils.uuidToByte(UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR)));
						ps.setBytes(2,  UuidUtils.uuidToByte(UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR)));
						ps.execute();
					}			
				}
			}
			
			try(PreparedStatement ps = c.prepareStatement("DELETE FROM smart.employee WHERE uuid = ?")){ //$NON-NLS-1$
				ps.setBytes(1,  UuidUtils.uuidToByte(UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR)));
				ps.execute();
			}
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V801.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}

}
