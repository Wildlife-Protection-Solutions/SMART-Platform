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
package org.wcs.smart.upgrade.v600;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.DerbyUtils;
import org.wcs.smart.util.UuidUtils;

public class Upgrader610To620 implements IDatabaseUpgrader { 
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
						thrownException = new Exception(Messages.Upgrader610To620_UpgradeError, e); 
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

		String[] sql = new String[] {
				"CREATE TABLE smart.iconset (uuid char(16) for bit data not null, keyid varchar(64) not null, ca_uuid char(16) for bit data not null, is_default boolean default false not null, primary key(uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.icon (uuid char(16) for bit data not null, keyid varchar(64) not null, ca_uuid char(16) for bit data not null, primary key(uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.iconfile (uuid char(16) for bit data not null, icon_uuid char(16) for bit data not null, iconset_uuid char(16) for bit data not null, filename varchar(2064) not null, primary key(uuid))", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.iconset TO admin,analyst,manager,data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.icon TO admin,analyst,manager,data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.iconfile TO admin,analyst,manager,data_entry", //$NON-NLS-1$
				
				"ALTER TABLE smart.dm_category add column icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute add column icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_list add column icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_tree add column icon_uuid char(16) for bit data", //$NON-NLS-1$
				
				"ALTER TABLE smart.dm_attribute ADD CONSTRAINT dmatt_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_list ADD CONSTRAINT dmattlist_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_tree ADD CONSTRAINT dmatttree_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_category ADD CONSTRAINT dmcat_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.configurable_model add column iconset_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.configurable_model ADD CONSTRAINT cm_iconset_uuid_fk FOREIGN KEY (iconset_uuid) REFERENCES smart.iconset(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.iconset ADD CONSTRAINT iconset_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.icon ADD CONSTRAINT icon_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.iconfile ADD CONSTRAINT iconfile_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.iconfile ADD CONSTRAINT iconfile_iconsetuuid_fk FOREIGN KEY (iconset_uuid) REFERENCES smart.iconset(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		PreparedStatement psiconset = c.prepareStatement("INSERT INTO smart.iconset (uuid, keyid, ca_uuid, is_default) VALUES (?, ?, ?, ?)");		 //$NON-NLS-1$
		PreparedStatement pslabel = c.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psicon = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psiconfile = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) VALUES(?, ?, ?, ?)"); //$NON-NLS-1$

		
		//create default icon sets
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
				
				byte[] lineuuid = DerbyUtils.createUuid();
				psiconset.setBytes(1, lineuuid);
				psiconset.setString(2, IconUtils.FixedIconSet.LINE.key);
				psiconset.setBytes(3, cuuid);
				psiconset.setBoolean(4, false);
				psiconset.addBatch();
				
				pslabel.setBytes(1, luuid);
				pslabel.setBytes(2, lineuuid);
				pslabel.setString(3, IconUtils.FixedIconSet.LINE.name); 
				pslabel.addBatch();
				
				byte[] blackuuid = DerbyUtils.createUuid();
				psiconset.setBytes(1, blackuuid);
				psiconset.setString(2, IconUtils.FixedIconSet.BLACK.key);
				psiconset.setBytes(3, cuuid);
				psiconset.setBoolean(4, false);
				psiconset.addBatch();
				
				pslabel.setBytes(1, luuid);
				pslabel.setBytes(2, blackuuid);
				pslabel.setString(3, IconUtils.FixedIconSet.BLACK.name);
				pslabel.addBatch();
				
				byte[] coloruuid = DerbyUtils.createUuid();
				psiconset.setBytes(1, coloruuid);
				psiconset.setString(2, IconUtils.FixedIconSet.COLOR.key);
				psiconset.setBytes(3, cuuid);
				psiconset.setBoolean(4, true);
				psiconset.addBatch();
				
				pslabel.setBytes(1, luuid);
				pslabel.setBytes(2, coloruuid);
				pslabel.setString(3, IconUtils.FixedIconSet.COLOR.name); 
				pslabel.addBatch();
				
				psiconset.executeBatch();
				pslabel.executeBatch();
				
				
				for (String[] icon : IconUtils.SMART_ICON_MAPPING) {
					if (icon[0].equalsIgnoreCase("afropavo_congensis")) break; //anything after this is dealth with in smart630 //$NON-NLS-1$
					
					byte[] iconuuid = DerbyUtils.createUuid();
					
					psicon.setBytes(1, iconuuid);
					psicon.setString(2, icon[0]);
					psicon.setBytes(3, cuuid);
					psicon.addBatch();
					
					pslabel.setBytes(1, luuid);
					pslabel.setBytes(2, iconuuid);
					pslabel.setString(3, icon[1]);
					pslabel.addBatch();
					
					byte[] fileuuid = DerbyUtils.createUuid();
					psiconfile.setBytes(1, fileuuid);
					psiconfile.setBytes(2, iconuuid);
					psiconfile.setBytes(3, blackuuid);
					psiconfile.setString(4, icon[2]);
					psiconfile.addBatch();
					
					fileuuid = DerbyUtils.createUuid();
					psiconfile.setBytes(1, fileuuid);
					psiconfile.setBytes(2, iconuuid);
					psiconfile.setBytes(3, lineuuid);
					psiconfile.setString(4, icon[3]);
					psiconfile.addBatch();
					
					fileuuid = DerbyUtils.createUuid();
					psiconfile.setBytes(1, fileuuid);
					psiconfile.setBytes(2, iconuuid);
					psiconfile.setBytes(3, coloruuid);
					psiconfile.setString(4, icon[4]);
					psiconfile.addBatch();
					
					
					psicon.executeBatch();
					pslabel.executeBatch();
					psiconfile.executeBatch();
					
					
					//update data model items
					IconUtils.upgradeDataModel(c, iconuuid, icon[5], cuuid);
				}
				
			}
		}

		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V620.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

	

	
	
}
