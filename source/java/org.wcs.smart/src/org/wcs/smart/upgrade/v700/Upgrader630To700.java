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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.DerbyUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

public class Upgrader630To700 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.Upgrader630To700_TaskName); 
		thrownException = null;
		try(Session s = HibernateManager.openSession()){
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						
						String v = HibernateManager.getPlugInVersion("org.wcs.smart.intelligence", s); //$NON-NLS-1$
						if(v != null) {
							Display.getDefault().syncExec(()->{
								MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.Upgrader630To700_WarningTitle, Messages.Upgrader630To700_IntelRemovedWarning);
							});
						}
						
						upgrade(c, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception(Messages.Upgrader630To700_TaskError, e); 
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
		
		
		/* remove orphaned patrols */
		String delete = "delete from smart.waypoint where SOURCE = 'PATROL' " //$NON-NLS-1$
					+ "and uuid not in (select wp_uuid from smart.PATROL_WAYPOINT)"; //$NON-NLS-1$
		c.createStatement().execute(delete);
		
		
		String[] sql = new String[] {
				
				"alter table smart.LANGUAGE alter column code set data type varchar(8)",  //$NON-NLS-1$
				"CREATE FUNCTION smart.tempuuid() returns char(16) for bit data LANGUAGE JAVA NOT deterministic external name 'org.wcs.smart.util.DerbyUtils.createUuid' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT", //$NON-NLS-1$
				
				"CREATE TABLE smart.employee_team (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, primary key (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.employee_team_member (employee_uuid char(16) for bit data not null, team_uuid char(16) for bit data not null, primary key(employee_uuid, team_uuid))", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.employee_team TO admin,manager,analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.employee_team_member TO admin,manager,analyst", //$NON-NLS-1$

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
				
				//make a temp table
				"CREATE TABLE smart.wp_observation_temp (uuid char(16) for bit data not null, wp_group_uuid char(16) for bit data not null, category_uuid char(16) for bit data not null, employee_uuid char(16) for bit data, primary key (uuid))", //$NON-NLS-1$
				//insert data
				"INSERT INTO smart.wp_observation_temp (uuid, wp_group_uuid, category_uuid, employee_uuid) select a.uuid, b.uuid, a.category_uuid, a.employee_uuid from smart.wp_observation a join smart.wp_observation_group b on b.wp_uuid = a.wp_uuid", //$NON-NLS-1$
				"ALTER TABLE smart.WP_OBSERVATION_ATTRIBUTES drop constraint obs_attribute_obs_uuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.OBSERVATION_ATTACHMENT drop constraint OBSERVATION_ATTACHMENT_OBS_UUID_FK", //$NON-NLS-1$
				"DROP TABLE smart.WP_OBSERVATION", //$NON-NLS-1$
				"RENAME TABLE smart.WP_OBSERVATION_TEMP to wp_observation", //$NON-NLS-1$
				
				//configure foreign keys - note we have to drop and recreate most of them to ensure 
				//they are created correctly
				
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
				"ALTER TABLE SMART.OBSERVATION_ATTACHMENT ADD CONSTRAINT OBSERVATION_ATTACHMENT_OBS_UUID_FK FOREIGN KEY (OBS_UUID) REFERENCES SMART.WP_OBSERVATION(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.WP_OBSERVATION_ATTRIBUTES ADD CONSTRAINT OBS_ATTRIBUTE_OBS_UUID_FK FOREIGN KEY (OBSERVATION_UUID) REFERENCES SMART.WP_OBSERVATION(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
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
		
		
		
		/* REMOVE INTELLIGENCE DATA IF EXISTS */
		String[] tables = new String[] {
				"smart.patrol_intelligence", //$NON-NLS-1$
				"smart.intelligence_point", //$NON-NLS-1$
				"smart.intelligence_attachment", //$NON-NLS-1$
				"smart.intelligence", //$NON-NLS-1$
				"smart.informant",  //$NON-NLS-1$
				"smart.intelligence_source", //$NON-NLS-1$
				"smart.intel_record_query", //$NON-NLS-1$
				"smart.intel_summary_query", //$NON-NLS-1$
				
		};
		for (String t : tables) {
			try {
				c.createStatement().execute("DROP TABLE " + t); //$NON-NLS-1$
			}catch (Exception ex) {
				//assume table doesn't exist
				//ex.printStackTrace();
			}
		}
	
		c.createStatement().execute("DELETE FROM smart.db_version where plugin_id = 'org.wcs.smart.intelligence'"); //$NON-NLS-1$
		c.createStatement().execute("DELETE FROM smart.db_version where plugin_id = 'org.wcs.smart.intelligence.query'"); //$NON-NLS-1$
		
		//delete all intelligence data from datastore
		String dir = "intelligence"; //$NON-NLS-1$
		Path rootFs = Paths.get(SmartContext.INSTANCE.getFilestoreLocation());
		try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.conservation_area")){ //$NON-NLS-1$
			while(rs.next()) {
				UUID cauuid = UuidUtils.byteToUUID( rs.getBytes(1) );
				Path inteldir = rootFs.resolve(UuidUtils.uuidToString(cauuid)).resolve(dir);
				if (Files.exists(inteldir)) {
					SmartUtils.deleteDirectory(inteldir);
				}
			}
		}

		updateIcons(c);
		createCcaaIconSets(c);
		
		c.commit();
	}
	
	private void updateIcons(Connection c) throws SQLException {
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
					
					//anything after this is new in SMART7
					if (icon[0].equalsIgnoreCase("agouti_paca")) { //$NON-NLS-1$
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
					
					//end of updates
					if (icon[0].equalsIgnoreCase("xanthopsar_flavus")) break; //$NON-NLS-1$
				}
				
			}
		}

	}
	
	private void createCcaaIconSets(Connection c) throws SQLException {
		PreparedStatement psiconset = c
				.prepareStatement("INSERT INTO smart.iconset (uuid, keyid, ca_uuid, is_default) VALUES (?, ?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement pslabel = c
				.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psicon = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psiconfile = c.prepareStatement(
				"INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) VALUES(?, ?, ?, ?)"); //$NON-NLS-1$

		byte[] cuuid = UuidUtils.uuidToByte(ConservationArea.MULTIPLE_CA);

		byte[] lineuuid = DerbyUtils.createUuid();
		byte[] blackuuid = DerbyUtils.createUuid();
		byte[] coloruuid = DerbyUtils.createUuid();

		PreparedStatement ps = c.prepareStatement("SELECT uuid FROM smart.language WHERE ca_uuid = ?"); //$NON-NLS-1$
		ps.setBytes(1, cuuid);

		psiconset.setBytes(1, lineuuid);
		psiconset.setString(2, IconUtils.FixedIconSet.LINE.key);
		psiconset.setBytes(3, cuuid);
		psiconset.setBoolean(4, false);
		psiconset.addBatch();

		psiconset.setBytes(1, blackuuid);
		psiconset.setString(2, IconUtils.FixedIconSet.BLACK.key);
		psiconset.setBytes(3, cuuid);
		psiconset.setBoolean(4, false);
		psiconset.addBatch();

		psiconset.setBytes(1, coloruuid);
		psiconset.setString(2, IconUtils.FixedIconSet.COLOR.key);
		psiconset.setBytes(3, cuuid);
		psiconset.setBoolean(4, true);
		psiconset.addBatch();

		List<byte[]> langs = new ArrayList<>();
		try (ResultSet rs2 = ps.executeQuery()) {
			while (rs2.next()) {
				byte[] language = rs2.getBytes(1);
				langs.add(language);
			}
		}
		
		if (langs.isEmpty()) { 
			//make a default language
			byte[] languageuuid = DerbyUtils.createUuid();
			
			PreparedStatement l = c.prepareStatement("INSERT INTO smart.language (uuid, ca_uuid, isdefault, code) VALUES (?,?,?,?)"); //$NON-NLS-1$
			l.setBytes(1, languageuuid);
			l.setBytes(2, cuuid);
			l.setBoolean(3, true);
			l.setString(4, Locale.getDefault().getLanguage());
			
			l.execute();

			langs.add(languageuuid);
			
		}
		
		for (byte[] language : langs) {
			pslabel.setBytes(1, language);
			pslabel.setBytes(2, lineuuid);
			pslabel.setString(3, IconUtils.FixedIconSet.LINE.name);
			pslabel.addBatch();

			pslabel.setBytes(1, language);
			pslabel.setBytes(2, blackuuid);
			pslabel.setString(3, IconUtils.FixedIconSet.BLACK.name);
			pslabel.addBatch();

			pslabel.setBytes(1, language);
			pslabel.setBytes(2, coloruuid);
			pslabel.setString(3, IconUtils.FixedIconSet.COLOR.name);
			pslabel.addBatch();
		}

		psiconset.executeBatch();
		pslabel.executeBatch();

		for (String[] icon : IconUtils.SMART_ICON_MAPPING) {

			byte[] iconuuid = DerbyUtils.createUuid();

			psicon.setBytes(1, iconuuid);
			psicon.setString(2, icon[0]);
			psicon.setBytes(3, cuuid);
			psicon.addBatch();

			for (byte[] luuid : langs) {
				pslabel.setBytes(1, luuid);
				pslabel.setBytes(2, iconuuid);
				pslabel.setString(3, icon[1]);
				pslabel.addBatch();
			}

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

		}
	}

}
