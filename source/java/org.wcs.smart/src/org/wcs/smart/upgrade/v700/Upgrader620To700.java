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
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

public class Upgrader620To700 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.Upgrader620To700_TaskName); 
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
						thrownException = new Exception(Messages.Upgrader620To700_TaskError, e); 
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
				"CREATE FUNCTION smart.tempuuid() returns char(16) for bit data LANGUAGE JAVA NOT deterministic external name 'org.wcs.smart.util.DerbyUtils.createUuid' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT", //$NON-NLS-1$
				
				"CREATE TABLE smart.employee_team (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, primary key (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.employee_team_member (employee_uuid char(16) for bit data not null, team_uuid char(16) for bit data not null, primary key(employee_uuid, team_uuid))", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.employee_team TO admin,manager,data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.employee_team_member TO admin,manager,data_entry", //$NON-NLS-1$

				"ALTER TABLE smart.employee_team ADD CONSTRAINT employeeteam_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.employee_team_member ADD CONSTRAINT employeeteammem_euuid FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.employee_team_member ADD CONSTRAINT employeeteammem_tuuid FOREIGN KEY (team_uuid) REFERENCES smart.employee_team(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				
				/* PATROL ATTRIBUTES */
				"CREATE TABLE SMART.PATROL_ATTRIBUTE(  UUID CHAR(16) FOR BIT DATA NOT NULL,  CA_UUID CHAR(16) FOR BIT DATA NOT NULL,  KEYID VARCHAR(128) NOT NULL,  ATT_TYPE VARCHAR(7) NOT NULL,  IS_ACTIVE BOOLEAN NOT NULL,  PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE SMART.PATROL_ATTRIBUTE_LIST(  UUID CHAR(16) FOR BIT DATA NOT NULL,  PATROL_ATTRIBUTE_UUID CHAR(16) FOR BIT DATA NOT NULL,  KEYID VARCHAR(128) NOT NULL,  LIST_ORDER SMALLINT NOT NULL,  IS_ACTIVE BOOLEAN NOT NULL,  PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE SMART.PATROL_ATTRIBUTE_VALUE (PATROL_UUID CHAR(16) FOR BIT DATA NOT NULL, PATROL_ATTRIBUTE_UUID CHAR(16) FOR BIT DATA NOT NULL, STRING_VALUE VARCHAR(1024), NUMBER_VALUE DOUBLE, LIST_ITEM_UUID CHAR(16) FOR BIT DATA, PRIMARY KEY (PATROL_UUID, PATROL_ATTRIBUTE_UUID))", //$NON-NLS-1$
				
				"ALTER TABLE SMART.PATROL_ATTRIBUTE_LIST ADD CONSTRAINT PATROL_ATT_LIST_PATROL_ATT_UUID_FK FOREIGN KEY (PATROL_ATTRIBUTE_UUID) REFERENCES SMART.PATROL_ATTRIBUTE (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.PATROL_ATTRIBUTE ADD CONSTRAINT PATROL_ATT_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD CONSTRAINT PATROL_ATT_VALUE_PATROL_UUID FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD CONSTRAINT PATROL_ATT_VALUE_ATT_UUID FOREIGN KEY (PATROL_ATTRIBUTE_UUID) REFERENCES SMART.PATROL_ATTRIBUTE(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.PATROL_ATTRIBUTE_VALUE ADD CONSTRAINT PATROL_ATT_VALUE_LIST_ITEM_UUID FOREIGN KEY (LIST_ITEM_UUID) REFERENCES SMART.PATROL_ATTRIBUTE_LIST(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"GRANT SELECT ON smart.PATROL_ATTRIBUTE TO data_entry", //$NON-NLS-1$
				"GRANT SELECT ON smart.PATROL_ATTRIBUTE_LIST TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.PATROL_ATTRIBUTE_VALUE TO data_entry", //$NON-NLS-1$

				"GRANT SELECT ON smart.PATROL_ATTRIBUTE TO analyst", //$NON-NLS-1$
				"GRANT SELECT ON smart.PATROL_ATTRIBUTE_LIST TO analyst", //$NON-NLS-1$
				"GRANT SELECT ON smart.PATROL_ATTRIBUTE_VALUE TO ANALYST", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.PATROL_ATTRIBUTE TO manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.PATROL_ATTRIBUTE_LIST TO manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.PATROL_ATTRIBUTE_VALUE TO manager", //$NON-NLS-1$
				
	            "CREATE FUNCTION smart.waypointWithin(x double, y double, distance real, bearing real, x1 double, y1 double, x2 double, y2 double) returns boolean LANGUAGE JAVA deterministic external name 'org.wcs.smart.util.GeometryUtils.waypointWithin' PARAMETER STYLE JAVA NO SQL", //$NON-NLS-1$
	            
	            "DROP FUNCTION smart.pointInPolygon", //$NON-NLS-1$
	            "CREATE FUNCTION smart.pointInPolygon(x double, y double, distance real, direction real, wkb blob) returns boolean LANGUAGE JAVA deterministic external name 'org.wcs.smart.util.GeometryUtils.pointInPolygon' PARAMETER STYLE JAVA NO SQL", //$NON-NLS-1$
	            "GRANT EXECUTE ON FUNCTION smart.pointInPolygon TO analyst", //$NON-NLS-1$
				"GRANT EXECUTE ON FUNCTION smart.pointInPolygon TO manager", //$NON-NLS-1$
				"GRANT EXECUTE ON FUNCTION smart.pointInPolygon TO dataentry", //$NON-NLS-1$
				
	            "DROP FUNCTION smart.computeTileId", //$NON-NLS-1$
	            "CREATE FUNCTION smart.computeTileId(x double, y double, distance real, direction real, destCrsWkt varchar(32672), originX double, originY double, gridSize double) returns varchar(32672) LANGUAGE JAVA deterministic external name 'org.wcs.smart.util.ReprojectUtils.computeTileId' PARAMETER STYLE JAVA NO SQL", //$NON-NLS-1$ 
	            "GRANT EXECUTE ON FUNCTION smart.computeTileId TO analyst", //$NON-NLS-1$
				"GRANT EXECUTE ON FUNCTION smart.computeTileId TO manager", //$NON-NLS-1$
				"GRANT EXECUTE ON FUNCTION smart.computeTileId TO dataentry", //$NON-NLS-1$
				
				"alter table smart.cm_attribute_option alter column string_value set data type varchar(32672)", //$NON-NLS-1$
				
				//sub-incidents
				"CREATE TABLE smart.wp_observation_group (uuid char(16) for bit data not null, wp_uuid char(16) for bit data not null, primary key (uuid))", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.wp_observation_group TO manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.wp_observation_group TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.wp_observation_group TO data_entry", //$NON-NLS-1$
				
				"INSERT INTO smart.wp_observation_group (uuid, wp_uuid) SELECT smart.tempuuid(), uuid FROM smart.waypoint WHERE uuid in (SELECT wp_uuid FROM smart.wp_observation)", //$NON-NLS-1$
				
				"ALTER TABLE smart.wp_observation ADD COLUMN wp_group_uuid char(16) for bit data", //$NON-NLS-1$
				"UPDATE smart.wp_observation SET wp_group_uuid = (select a.uuid from smart.wp_observation_group a where a.wp_uuid = smart.wp_observation.wp_uuid)", //$NON-NLS-1$
				"alter table smart.wp_observation drop column wp_uuid", //$NON-NLS-1$
				
				//configure foreign keys - note we have to drop and recreate most of them to ensure 
				//they are created correctly
				"alter table smart.WP_OBSERVATION drop constraint obs_employee_uuid_fk", //$NON-NLS-1$
				"alter table smart.WP_OBSERVATION drop constraint observation_category_uuid_fk", //$NON-NLS-1$

				"alter table smart.DM_CATEGORY drop constraint dm_category_ca_uuid_fk", //$NON-NLS-1$
				"alter table smart.DM_ATTRIBUTE drop constraint dm_attribute_ca_uuid_fk", //$NON-NLS-1$

				"alter table smart.dm_category drop constraint dmcat_iconuuid_fk", //$NON-NLS-1$
				"alter table smart.dm_attribute drop constraint dmatt_iconuuid_fk", //$NON-NLS-1$
				"alter table smart.dm_attribute_list drop constraint dmattlist_iconuuid_fk", //$NON-NLS-1$
				"alter table smart.dm_attribute_tree drop constraint dmatttree_iconuuid_fk", //$NON-NLS-1$

				"alter table smart.configurable_model drop constraint cm_iconset_uuid_fk", //$NON-NLS-1$
				"alter table smart.iconset drop constraint iconset_cauuid_fk", //$NON-NLS-1$
				"alter table smart.icon drop constraint icon_cauuid_fk", //$NON-NLS-1$
				"alter table smart.iconfile drop constraint iconfile_iconuuid_fk", //$NON-NLS-1$
				"alter table smart.iconfile drop constraint iconfile_iconsetuuid_fk", //$NON-NLS-1$

				"ALTER table smart.wp_observation ADD CONSTRAINT wo_ob_group_uuid_fk FOREIGN KEY (wp_group_uuid) REFERENCES smart.wp_observation_group (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER table smart.wp_observation_group ADD CONSTRAINT wo_obs_grp_wp_uuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.wp_observation add constraint obs_employee_uuid_fk foreign key (employee_uuid) REFERENCES smart.employee (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.wp_observation add constraint observation_category_uuid_fk foreign key (category_uuid) REFERENCES smart.dm_category (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"alter table smart.dm_category add constraint dm_category_ca_uuid_fk foreign key (ca_uuid) references smart.CONSERVATION_AREA(uuid) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.DM_ATTRIBUTE add constraint dm_attribute_ca_uuid_fk foreign key (ca_uuid) references smart.CONSERVATION_AREA(uuid) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$


				"ALTER TABLE smart.dm_attribute ADD CONSTRAINT dmatt_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_list ADD CONSTRAINT dmattlist_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_tree ADD CONSTRAINT dmatttree_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_category ADD CONSTRAINT dmcat_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
								

				"ALTER TABLE smart.configurable_model ADD CONSTRAINT cm_iconset_uuid_fk FOREIGN KEY (iconset_uuid) REFERENCES smart.iconset(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
								
				"ALTER TABLE smart.iconset ADD CONSTRAINT iconset_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.icon ADD CONSTRAINT icon_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.iconfile ADD CONSTRAINT iconfile_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.iconfile ADD CONSTRAINT iconfile_iconsetuuid_fk FOREIGN KEY (iconset_uuid) REFERENCES smart.iconset(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"DROP FUNCTION smart.tempuuid", //$NON-NLS-1$
				
				"CREATE DERBY AGGREGATE smart.unionarea FOR blob RETURNS double precision EXTERNAL NAME 'org.wcs.smart.util.AreaUnionAggregate'", //$NON-NLS-1$
				"CREATE FUNCTION smart.buffer(geom blob, buffer double precision) returns blob LANGUAGE JAVA NOT deterministic external name 'org.wcs.smart.util.GeometryUtils.buffer' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT", //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V700.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

}
