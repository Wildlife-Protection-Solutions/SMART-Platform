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
public class Upgrader753To800 extends AbstractInteralDatabaseUpgrader { 
	
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
		
		//drop entity plugin tables if they exists
		String[] sql = new String[] {
			"drop table smart.entity_gridded_query",  //$NON-NLS-1$
			"drop table smart.entity_observation_query",  //$NON-NLS-1$
			"drop table smart.entity_summary_query",  //$NON-NLS-1$
			"drop table smart.entity_waypoint_query",  //$NON-NLS-1$
			"drop table smart.entity_attribute_value",  //$NON-NLS-1$
			"drop table smart.entity",  //$NON-NLS-1$
			"drop table smart.entity_attribute",  //$NON-NLS-1$
			"drop table smart.entity_type"  //$NON-NLS-1$
		};
		for (String s : sql) {
			try {
				c.createStatement().execute(s);
			}catch (Exception ex) {
				//don't worry if the table doesn't exists
			}
		}
		
		
		
		
		sql = new String[] {
		
				"delete from smart.db_version where plugin_id = 'org.wcs.smart.entity' or plugin_id = 'org.wcs.smart.entity.query'",  //$NON-NLS-1$
				
				//remove icon, iconfiles from conservation areas that are not referenced
				//leave any custom icons if they are used or not  
				"DELETE FROM smart.ICONFILE WHERE icon_uuid not in (select distinct icon_uuid from smart.dm_attribute where icon_uuid is not null union  select distinct icon_uuid from smart.DM_ATTRIBUTE_LIST where icon_uuid is not null union select distinct icon_uuid from smart.DM_ATTRIBUTE_TREE where icon_uuid is not null union select distinct icon_uuid from smart.DM_CATEGORY where icon_uuid is not null ) and  filename like 'platform%'", //$NON-NLS-1$
				"DELETE FROM smart.ICON WHERE uuid not in (SELECT icon_uuid FROM smart.iconfile)", //$NON-NLS-1$
				
				//add icon key to patrol attributes
				//modifications to constraints required
				"alter table smart.patrol_mandate drop constraint PATROL_MANDATE_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_mandate ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER table smart.patrol_mandate ADD CONSTRAINT pm_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"ALTER TABLE smart.patrol_transport DROP CONSTRAINT PATROL_TRANSPORT_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_transport ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER table smart.patrol_transport ADD CONSTRAINT ptransport_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.team DROP CONSTRAINT TEAM_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.team ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER table smart.team ADD CONSTRAINT team_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.patrol_attribute DROP CONSTRAINT PATROL_ATT_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_attribute ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER table smart.patrol_attribute ADD CONSTRAINT patrol_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.patrol_attribute_list ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_attribute_list ADD CONSTRAINT patrol_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
				
				"ALTER TABLE smart.station DROP CONSTRAINT STATION_CA_UUID_FK",   //$NON-NLS-1$
				"ALTER TABLE smart.station ADD COLUMN icon_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.station ADD CONSTRAINT station_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
				
				"ALTER TABLE smart.iconset DROP CONSTRAINT iconset_cauuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.icon DROP CONSTRAINT icon_cauuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.iconset ADD CONSTRAINT iconset_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.icon ADD CONSTRAINT icon_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE SMART.PATROL_MANDATE ADD CONSTRAINT PATROL_MANDATE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.patrol_transport ADD CONSTRAINT patrol_Transport_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.team ADD CONSTRAINT TEAM_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.station ADD CONSTRAINT STATION_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				//increase size of property field
				"ALTER TABLE smart.conservation_area_property alter column value set data type varchar(32672)", //$NON-NLS-1$
				
				//geometry support
				"ALTER TABLE smart.wp_observation_attributes add column geom BLOB", //$NON-NLS-1$
				"ALTER TABLE smart.wp_observation_attributes add column number_value_2 double", //$NON-NLS-1$
				
				//hibernate 6 employee uuid cannot conflict with ccaa uuid
				"update smart.employee set uuid = x'00000000000000000000000000000001' where uuid = x'00000000000000000000000000000000'" //$NON-NLS-1$
		};
		
		
		for (String s : sql) {
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V800.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}

}
