/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.upgrade.v600;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.DerbyUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Upgrade from 620 to 630 - adds new species icons to datasets
 */
public class Upgrader620To630 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.Upgrader610To620_UpgradeMsg); 
		thrownException = null;
		try(Session s = HibernateManager.openSession()){
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception(Messages.Upgrader620To630_620to630Error, e); 
					}
				}
			});
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, Session session, IProgressMonitor monitor)
			throws Exception {

		c.createStatement().execute("ALTER TABLE smart.icon ADD CONSTRAINT keyidcaunq UNIQUE (keyid, ca_uuid)"); //$NON-NLS-1$
		
		PreparedStatement pslabel = c.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psicon = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psiconfile = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) VALUES(?, ?, ?, ?)"); //$NON-NLS-1$

		
		//add to default icon sets
		//NOTE: We cannot use hibernate objects here - this may cause issues in the future
		try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.conservation_area")){ //$NON-NLS-1$
			while(rs.next()) {
				byte[] cuuid = rs.getBytes(1);
				if (UuidUtils.byteToUUID(cuuid).equals(ConservationArea.MULTIPLE_CA)) continue;
				PreparedStatement ps = c.prepareStatement("SELECT uuid FROM smart.language WHERE ca_uuid = ? and isdefault"); //$NON-NLS-1$
				ps.setBytes(1, cuuid);
				
				byte[] luuid = null;
				try(ResultSet rs2 = ps.executeQuery()){
					if (!rs2.next()) continue; //no default language for this ca; skip
					luuid = rs2.getBytes(1);
				}
				
				if (luuid == null) continue; 
				
				byte[] lineuuid = null;
				byte[] blackuuid = null;
				byte[] coloruuid = null;
				try(PreparedStatement ps1 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = 'line' AND ca_uuid = ?")){ //$NON-NLS-1$
					ps1.setBytes(1,  cuuid);
					try(ResultSet rs1 = ps1.executeQuery()){
						if (rs1.next()) {
							lineuuid = rs1.getBytes(1);
						}
					}
				}
				try(PreparedStatement ps1 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = 'black' AND ca_uuid = ?")){ //$NON-NLS-1$
					ps1.setBytes(1,  cuuid);
					try(ResultSet rs1 = ps1.executeQuery()){
						if (rs1.next()) {
							blackuuid = rs1.getBytes(1);
						}
					}
				}
				
				try(PreparedStatement ps1 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = 'color' AND ca_uuid = ?")){ //$NON-NLS-1$
					ps1.setBytes(1,  cuuid);
					try(ResultSet rs1 = ps1.executeQuery()){
						if (rs1.next()) {
							coloruuid = rs1.getBytes(1);
						}
					}
				}
				
				if (lineuuid == null && blackuuid == null && coloruuid == null) {
					//not iconsets in this conservation area
					continue;
				}
				boolean found = false;
				for (String[] icon : IconUtils.SMART_ICON_MAPPING) {
					
					//anything after this is new in SMART630
					if (icon[0].equalsIgnoreCase("afropavo_congensis")) { //$NON-NLS-1$
						found = true;
					}
					if (!found) continue;				
					
					byte[] iconuuid = DerbyUtils.createUuid();
					
					psicon.setBytes(1, iconuuid);
					psicon.setString(2, icon[0]);
					psicon.setBytes(3, cuuid);
					psicon.addBatch();
					
					pslabel.setBytes(1, luuid);
					pslabel.setBytes(2, iconuuid);
					pslabel.setString(3, icon[1]);
					pslabel.addBatch();
					
					if (blackuuid != null) {
						byte[] fileuuid = DerbyUtils.createUuid();
						psiconfile.setBytes(1, fileuuid);
						psiconfile.setBytes(2, iconuuid);
						psiconfile.setBytes(3, blackuuid);
						psiconfile.setString(4, icon[2]);
						psiconfile.addBatch();
					}
					
					if (lineuuid != null) {
						byte[] fileuuid = DerbyUtils.createUuid();
						psiconfile.setBytes(1, fileuuid);
						psiconfile.setBytes(2, iconuuid);
						psiconfile.setBytes(3, lineuuid);
						psiconfile.setString(4, icon[3]);
						psiconfile.addBatch();
					}
					
					if (coloruuid != null) {
						byte[] fileuuid = DerbyUtils.createUuid();
						psiconfile.setBytes(1, fileuuid);
						psiconfile.setBytes(2, iconuuid);
						psiconfile.setBytes(3, coloruuid);
						psiconfile.setString(4, icon[4]);
						psiconfile.addBatch();
					}
					
					psicon.executeBatch();
					pslabel.executeBatch();
					psiconfile.executeBatch();
					
					
					//update data model items
					IconUtils.upgradeDataModel(c, iconuuid, icon[5], cuuid);
					
					if (icon[0].equalsIgnoreCase("xenopirostris_damii")) break; //$NON-NLS-1$
				}
				
			}
		}

		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V630.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

	

	
	
}
