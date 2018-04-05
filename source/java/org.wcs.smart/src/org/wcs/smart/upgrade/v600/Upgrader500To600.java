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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.DerbyUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Scripts for upgrading from 500 to 600
 * 
 * @author Emily
 *
 */
public class Upgrader500To600 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.Upgrader500To600_ProgressMessage, 1);
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
						thrownException = new Exception(Messages.Upgrader500To600_ErrorMessage, e);
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
				"ALTER TABLE smart.patrol_leg ADD COLUMN mandate_uuid char(16) for bit data", //$NON-NLS-1$
				"UPDATE smart.patrol_leg SET mandate_uuid = (SELECT p.mandate_uuid FROM smart.patrol p WHERE p.uuid = smart.patrol_leg.patrol_uuid)", //$NON-NLS-1$
				"ALTER TABLE SMART.PATROL_LEG ADD CONSTRAINT MANDATE_UUID_FK FOREIGN KEY (MANDATE_UUID) REFERENCES SMART.PATROL_MANDATE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.patrol DROP COLUMN mandate_uuid", //$NON-NLS-1$
				"CREATE TABLE SMART.LOGIN_LOG (uuid char(16) for bit data not null, smart_userid varchar(16) not null, smart_userlevels varchar(5000) not null, login_timestamp timestamp not null, ca_id varchar(8) not null, ca_name varchar(256) not null )", //$NON-NLS-1$
				"GRANT INSERT ON SMART.LOGIN_LOG TO PUBLIC", //$NON-NLS-1$

				"CREATE TABLE smart.patrol_folder (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, parent_uuid char(16) for bit data, folder_order smallint, primary key (uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_folder ADD CONSTRAINT PATROL_FOLDER_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_folder ADD CONSTRAINT PATROL_FOLDER_PARENT_UUID_FK FOREIGN KEY (PARENT_UUID) REFERENCES SMART.PATROL_FOLDER(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.patrol ADD COLUMN folder_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.patrol ADD CONSTRAINT PATROL_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.PATROL_FOLDER(UUID) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				//unique userid/ca combo
				"ALTER TABLE smart.employee ADD CONSTRAINT smartuseridunq UNIQUE(ca_uuid, smartuserid)", //$NON-NLS-1$
				
				"ALTER TABLE smart.agency ADD COLUMN keyid varchar(128)", //$NON-NLS-1$
				
				"UPDATE smart.db_version set plugin_id = 'org.wcs.smart.cybertracker.patrol' where plugin_id = 'org.wcs.smart.connect.dataqueue.cybertracker.patrol'", //$NON-NLS-1$
				"UPDATE smart.db_version set plugin_id = 'org.wcs.smart.cybertracker.survey' where plugin_id = 'org.wcs.smart.connect.dataqueue.cybertracker.survey'" //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		upgradeConfigurableModel(c);

		//create keys for agency rank
		addKeys(c);
		c.createStatement().execute("ALTER TABLE smart.agency ADD CONSTRAINT keyunq UNIQUE (keyid, ca_uuid)"); //$NON-NLS-1$
		
		//create qa plugin tables
		QaPlugInInstaller.createTables(session, c);
		
		//ecnrypt files
		encryptFilestoreData(c);
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V600.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

	/**
	 * Generate and add keys for agency upgrade
	 * @param c
	 * @throws SQLException
	 */
	private void addKeys(Connection c) throws SQLException {
		String query = "select a.value, b.uuid as item_uuid, b.ca_uuid from smart.I18N_LABEL a, smart.agency b, smart.language c  where b.uuid = a.element_uuid and c.uuid = a.language_uuid and c.isdefault ORDER BY b.uuid"; //$NON-NLS-1$
		
		String query2 = "UPDATE smart.agency SET keyid = ? WHERE uuid = ?";  //$NON-NLS-1$ 
		PreparedStatement ps = c.prepareStatement(query2);
		try(ResultSet rs = c.createStatement().executeQuery(query)){
			while(rs.next()) {
				String name = rs.getString(1);
				byte[] itemuuid = rs.getBytes(2);
				
				String keyId = name.toLowerCase().replaceAll("[^a-zA-Z0-9]", ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (keyId.isEmpty()) keyId = UuidUtils.uuidToString(UuidUtils.byteToUUID(itemuuid));
				ps.setString(1, keyId);
				ps.setBytes(2, itemuuid);
				ps.executeUpdate();
			}
		}
		
		//ensure unique
		query = "SELECT a.uuid, a.keyId FROM smart.agency a, (SELECT ca_uuid, keyid FROM smart.agency GROUP BY ca_uuid, keyid HAVING count(*) > 1) b  WHERE a.ca_uuid = b.ca_uuid and a.keyid = b.keyid"; //$NON-NLS-1$
		try(ResultSet rs = c.createStatement().executeQuery(query)){
			while(rs.next()) {
				byte[] itemuuid = rs.getBytes(1);
				String key = rs.getString(2);
				String keyId = key + UuidUtils.uuidToString(UuidUtils.byteToUUID(itemuuid));
				ps.setString(1, keyId);
				ps.setBytes(2, itemuuid);
				ps.executeUpdate();
			}
		}
	}
	
	
	/**
	 * Here we encrypt all files in the filestore including other plugins
	 * @param c
	 * @throws SQLException
	 */
	private void encryptFilestoreData(Connection c) throws Exception {
		//here we are encrypting all attachment files
		String[] subDirs = new String[]
				{"incidents", "intelligence", "intelligence2\\attachments", "patrol", "survey"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		String query = "SELECT uuid FROM smart.conservation_area"; //$NON-NLS-1$
		
		Path tempDir = Paths.get(SmartContext.INSTANCE.getFilestoreLocation())
				.resolve(EncryptUtils.TEMP_DIR);
		if (!Files.exists(tempDir)) {
			try{
				Files.createDirectory(tempDir);
			}catch (Exception ex) {
				throw new Exception("Unable to create temporary files directory in filestore.  Cannot upgrade SMART."); //$NON-NLS-1$
			}
		}
		
		try(ResultSet rs = c.createStatement().executeQuery(query)){
			while(rs.next()) {
				UUID cauuid = UuidUtils.byteToUUID(rs.getBytes(1));
				String uuid = UuidUtils.uuidToString( cauuid );
				
				Path caPath = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve(uuid);
				
				for (String subDir : subDirs) {
					Path p = caPath.resolve(subDir);
					if (!Files.exists(p)) continue; 	//nothing in this directory
					
					List<Path> allFiles = null;
					//walk directory recursively and encrypt files
					try {
						allFiles = Files.walk(p)
								.filter(Files::isRegularFile)
								.collect(Collectors.toList());
					}catch (Exception ex) {
						throw new Exception("Unable to determine files to encrypt in filestore: " + p.toString(), ex); //$NON-NLS-1$
					}
					
					if (allFiles == null) continue;
					for (Path file : allFiles) {
						//don't encrypt files in root directory except the intelligence2 attachments dir
						if (file.getParent().equals(p) && !subDir.equals(subDirs[2])) continue;	
						
						//encrypt the files
						Path outputFile = tempDir.resolve(file.getFileName().toString());
						try {
							EncryptUtils.encryptFile(file, outputFile,  cauuid);
						} catch (Exception e) {
							throw new Exception("Unable to encrypt filestore file: " + file.toString(), e); //$NON-NLS-1$
						}
						
						//copy file
						try {
							Files.copy(outputFile, file, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException e) {
							throw new Exception("Unable to encrypt filestore file.  Unable to encrypted files to original location " + file.toString(), e); //$NON-NLS-1$
						}				
					};
				}
				
			}
		}
		
	}
	
	private void upgradeConfigurableModel(Connection c) throws Exception {
		String[] sql = new String[] {
				"CREATE TABLE smart.cm_attribute_config(uuid char(16) for bit data not null, cm_uuid char(16) for bit data not null, dm_attribute_uuid char(16) for bit data not null, display_mode varchar(10), is_default boolean, primary key (uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_config ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_CM_UUID_FK FOREIGN KEY (CM_UUID) REFERENCES SMART.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_config ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_DM_ATTRIBUTE_UUID_FK FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO analyst", //$NON-NLS-1$

				"alter table smart.cm_attribute add column config_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"alter table smart.cm_attribute_list add column config_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE SMART.CM_ATTRIBUTE_LIST ADD CONSTRAINT CM_ATTRIBUTE_LIST_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$ 

				"alter table smart.cm_attribute_tree_node add column config_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE SMART.CM_ATTRIBUTE_TREE_NODE ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$ 
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		populateConfigs(c, "smart.CM_ATTRIBUTE_LIST"); //$NON-NLS-1$
		populateConfigs(c, "smart.CM_ATTRIBUTE_TREE_NODE"); //$NON-NLS-1$
		
		sql = new String[] {
				"drop table SMART.CM_DM_ATTRIBUTE_SETTINGS", //$NON-NLS-1$

				"alter table smart.cm_attribute_list drop column CM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_list drop column DM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_list drop column CM_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_list alter column config_uuid SET NOT NULL", //$NON-NLS-1$

				"alter table smart.cm_attribute_tree_node drop column CM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node drop column DM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node drop column CM_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node alter column config_uuid SET NOT NULL", //$NON-NLS-1$

				"delete from smart.CM_ATTRIBUTE_OPTION where OPTION_ID = 'DISPLAY_MODE' OR OPTION_ID = 'CUSTOM_CONFIG'", //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
	}

	private void populateConfigs(Connection c, String tableName) throws Exception {
		try (ResultSet rs = c.createStatement().executeQuery("select distinct CM_UUID, CM_ATTRIBUTE_UUID, DM_ATTRIBUTE_UUID from " + tableName)) { //$NON-NLS-1$
			while (rs.next()) {
				byte[] cm_uuid = rs.getBytes(1);
				byte[] cma_uuid = rs.getBytes(2);
				byte[] dma_uuid = rs.getBytes(3);
				byte[] cfg_uuid = DerbyUtils.createUuid();

				if (cma_uuid != null) {
					//this is custom config
					insertConfig(c, cfg_uuid, cm_uuid, getDmAttributeForCmAttribute(c, cma_uuid), getDisplayModeForCustomCmAttribute(c, cma_uuid), false);

					PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID = ? AND DM_ATTRIBUTE_UUID IS NULL"); //$NON-NLS-1$ //$NON-NLS-2$
					ps_upd.setBytes(1, cfg_uuid);
					ps_upd.setBytes(2, cm_uuid);
					ps_upd.setBytes(3, cma_uuid);
					ps_upd.executeUpdate();

					PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE UUID = ?"); //$NON-NLS-1$
					ps_cma_upd.setBytes(1, cfg_uuid);
					ps_cma_upd.setBytes(2, cma_uuid);
					ps_cma_upd.executeUpdate();

				} else if (dma_uuid != null) {
					//this is default config
					insertConfig(c, cfg_uuid, cm_uuid, dma_uuid, getDisplayModeForDefaultAttribute(c, cm_uuid, dma_uuid), true);

					PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID IS NULL AND DM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$ //$NON-NLS-2$
					ps_upd.setBytes(1, cfg_uuid);
					ps_upd.setBytes(2, cm_uuid);
					ps_upd.setBytes(3, dma_uuid);
					ps_upd.executeUpdate();

					PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE CONFIG_UUID IS NULL and ATTRIBUTE_UUID = ? AND NODE_UUID IN (select uuid from smart.cm_node where cm_uuid = ?)"); //$NON-NLS-1$
					ps_cma_upd.setBytes(1, cfg_uuid);
					ps_cma_upd.setBytes(2, dma_uuid);
					ps_cma_upd.setBytes(3, cm_uuid);
					ps_cma_upd.executeUpdate();
				}
			}
		}
		String attType = null;
		if (tableName.equals("smart.CM_ATTRIBUTE_LIST")){ //$NON-NLS-1$
			attType = Attribute.AttributeType.LIST.name();
		}else if (tableName.equals("smart.CM_ATTRIBUTE_TREE_NODE")) { //$NON-NLS-1$
			attType = Attribute.AttributeType.TREE.name();
		}
		if (attType == null) return;
		//for configurations with no elements
		try(ResultSet rs = c.createStatement().executeQuery("select c.cm_uuid as cm_uuid, a.uuid as cm_attribute_uuid, a.attribute_uuid as dm_attribute_uuid from smart.cm_node c, smart.cm_attribute a, smart.dm_attribute b where c.uuid = a.node_uuid and a.attribute_uuid = b.uuid and b.att_type = '" + attType + "' and config_uuid is null")){ //$NON-NLS-1$ //$NON-NLS-2$
			while (rs.next()) {
				byte[] cm_uuid = rs.getBytes(1);
				byte[] cma_uuid = rs.getBytes(2);
				byte[] cfg_uuid = DerbyUtils.createUuid();

				//create custom config and make default
				insertConfig(c, cfg_uuid, cm_uuid, getDmAttributeForCmAttribute(c, cma_uuid), getDisplayModeForCustomCmAttribute(c, cma_uuid), true);

				PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID = ? AND DM_ATTRIBUTE_UUID IS NULL"); //$NON-NLS-1$ //$NON-NLS-2$
				ps_upd.setBytes(1, cfg_uuid);
				ps_upd.setBytes(2, cm_uuid);
				ps_upd.setBytes(3, cma_uuid);
				ps_upd.executeUpdate();

				PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE UUID = ?"); //$NON-NLS-1$
				ps_cma_upd.setBytes(1, cfg_uuid);
				ps_cma_upd.setBytes(2, cma_uuid);
				ps_cma_upd.executeUpdate();
			}
		}
	}

	private void insertConfig(Connection c, byte[] cfg_uuid, byte[] cm_uuid, byte[] dma_uuid, String displayMode, boolean isDefault) throws Exception {
		PreparedStatement ps_cfg = c.prepareStatement("INSERT INTO smart.CM_ATTRIBUTE_CONFIG (UUID, CM_UUID, DM_ATTRIBUTE_UUID, DISPLAY_MODE, IS_DEFAULT) VALUES (?, ?, ?, ?, ?)"); //$NON-NLS-1$
		ps_cfg.setBytes(1, cfg_uuid);
		ps_cfg.setBytes(2, cm_uuid);
		ps_cfg.setBytes(3, dma_uuid);
		ps_cfg.setString(4, displayMode);
		ps_cfg.setBoolean(5, isDefault);
		ps_cfg.executeUpdate();

		//assigning name for the config
		byte[] lng_uuid = null;
		PreparedStatement ps_lng = c.prepareStatement("select lng.UUID from smart.LANGUAGE lng join smart.CONFIGURABLE_MODEL cm on lng.CA_UUID = cm.CA_UUID where lng.ISDEFAULT and cm.UUID = ?"); //$NON-NLS-1$
		ps_lng.setBytes(1, cm_uuid);
		try (ResultSet rs = ps_lng.executeQuery()) {
			if (rs.next()) {
				lng_uuid = rs.getBytes(1);
			} else {
				throw new Exception("Unable to detect default language while upgrading configurable model."); //$NON-NLS-1$
			}
		}

		String cfgName = "Configuration"; //$NON-NLS-1$ Some default that will be owerwritten
		PreparedStatement ps_name = c.prepareStatement("select VALUE from smart.I18N_LABEL where ELEMENT_UUID = ? AND LANGUAGE_UUID = ?"); //$NON-NLS-1$
		ps_name.setBytes(1, dma_uuid);
		ps_name.setBytes(2, lng_uuid);
		try (ResultSet rs = ps_name.executeQuery()) {
			if (rs.next()) {
				cfgName = rs.getString(1);
			}
		}
		if (!isDefault) {
			int customCount = 0;
			PreparedStatement ps_count = c.prepareStatement("select count(UUID) from smart.CM_ATTRIBUTE_CONFIG where DM_ATTRIBUTE_UUID = ? AND not IS_DEFAULT AND CM_UUID = ?"); //$NON-NLS-1$
			ps_count.setBytes(1, dma_uuid);
			ps_count.setBytes(2, cm_uuid);
			try (ResultSet rs = ps_count.executeQuery()) {
				if (rs.next()) {
					customCount = rs.getInt(1);
				}
			}
			cfgName = "Custom " + cfgName + " " + customCount; //$NON-NLS-1$ //$NON-NLS-2$
		}
		PreparedStatement ps_lbl = c.prepareStatement("INSERT INTO smart.I18N_LABEL (LANGUAGE_UUID, ELEMENT_UUID, VALUE) VALUES (?, ?, ?)"); //$NON-NLS-1$
		ps_lbl.setBytes(1, lng_uuid);
		ps_lbl.setBytes(2, cfg_uuid);
		ps_lbl.setString(3, cfgName);
		ps_lbl.executeUpdate();
	}

	private String getDisplayModeForDefaultAttribute(Connection c, byte[] cm_uuid, byte[] dma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select DISPLAY_MODE from smart.CM_DM_ATTRIBUTE_SETTINGS where CM_UUID = ? AND DM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$
		ps.setBytes(1, cm_uuid);
		ps.setBytes(2, dma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private String getDisplayModeForCustomCmAttribute(Connection c, byte[] cma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select STRING_VALUE from smart.CM_ATTRIBUTE_OPTION where OPTION_ID = 'DISPLAY_MODE' AND CM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$
		ps.setBytes(1, cma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private byte[] getDmAttributeForCmAttribute(Connection c, byte[] cma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select ATTRIBUTE_UUID from smart.CM_ATTRIBUTE where UUID = ?"); //$NON-NLS-1$
		ps.setBytes(1, cma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getBytes(1) : null;
		}
	}

}
