/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.servlet;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.util.UuidUtils;

/**
 * Upgrade servlet for performing non-database upgrades.
 * 
 * @author Emily
 *
 */
@WebServlet(urlPatterns = {"/upgradeconnect"})
public class UpgradeServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private final Logger logger = Logger.getLogger(UpgradeServlet.class.getName());
	
	/*
	 * lock to ensure only one person upgrades filestore at a time
	 */
	final static AtomicBoolean upgradeLock = new AtomicBoolean(false);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		
		if (!upgradeLock.compareAndSet(false, true)) {
			//somebody else is already running this code; we don't want to run it twice to lets get out of here
			request.setAttribute("org.wcs.smart.upgrade", "RUNNING");  //$NON-NLS-1$//$NON-NLS-2$
			try {
				request.getRequestDispatcher("WEB-INF/upgrade.jsp").forward(request, response); //$NON-NLS-1$
			} catch (IOException e) {
				throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR,e);
			} 
			return;
		}
		try {
		
			Session s = HibernateManager.getSession(request.getServletContext());
			try{
				s.beginTransaction();
				String query = "SELECT version, filestore_version FROM connect.connect_version"; //$NON-NLS-1$
				Object[] data = (Object[])s.createNativeQuery(query).uniqueResult();
				if (data == null) {
					request.setAttribute("javax.servlet.error.message", Messages.getString("UpgradeServlet.DbVersionInvalid", request.getLocale())); //$NON-NLS-1$ //$NON-NLS-2$ 
					request.getRequestDispatcher("WEB-INF/errorpages/unknown.jsp").forward(request, response); //$NON-NLS-1$
					return;
				}
				
				String version = (String) data[0];
				if (!version.equals(HibernateManager.DATABASE_VERSION)) {
					request.setAttribute("javax.servlet.error.message", Messages.getString("UpgradeServlet.FSVersionInvalid", request.getLocale())); //$NON-NLS-1$ //$NON-NLS-2$ 
					request.getRequestDispatcher("WEB-INF/errorpages/unknown.jsp").forward(request, response); //$NON-NLS-1$
					return;
				}
				String filestoreVersion = (String) data[1];
				if (filestoreVersion.equals(HibernateManager.FILESTORE_VERSION)) {
					//we are up to date; there is nothing to do here
					request.setAttribute("org.wcs.smart.upgrade", "NOACTION");  //$NON-NLS-1$//$NON-NLS-2$
					request.getRequestDispatcher("WEB-INF/upgrade.jsp").forward(request, response); //$NON-NLS-1$
					return;
				}
				
				if (filestoreVersion.equals("5.0.0")) { //$NON-NLS-1$
					upgrade500to600(s);
				}
				
				//update filestore version
				String sql = "UPDATE connect.connect_version set filestore_version = :version"; //$NON-NLS-1$
				s.createNativeQuery(sql)
					.setParameter("version",  HibernateManager.FILESTORE_VERSION) //$NON-NLS-1$
					.executeUpdate();
				s.getTransaction().commit();
				
				//we are up to date; there is nothing to do here
				request.setAttribute("org.wcs.smart.upgrade", "UPGRADE_COMPLETE"); //$NON-NLS-1$ //$NON-NLS-2$
				request.getRequestDispatcher("WEB-INF/upgrade.jsp").forward(request, response); //$NON-NLS-1$
				return;
	
			}catch (Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				s.getTransaction().rollback();
				
				request.setAttribute("org.wcs.smart.upgrade", "UPGRADE_ERROR");  //$NON-NLS-1$//$NON-NLS-2$
				try {
					request.getRequestDispatcher("WEB-INF/upgrade.jsp").forward(request, response); //$NON-NLS-1$
				} catch (IOException e) {
					throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR);
				} 
				return;
			}finally {
				if (s.getTransaction().isActive()) {
					s.getTransaction().rollback();
				}
			}
		}finally {
			upgradeLock.set(false);
		}
	}
	
	private void upgrade500to600(Session s) {
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					upgradeConfigurableModel5To6(c);
					encryptFilestoreData(c);
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
			
		});
		
		
		
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
		
		Path tempDir = Paths.get(SmartContext.INSTANCE.getFilestoreLocation())
				.resolve(EncryptUtils.TEMP_DIR);
		if (!Files.exists(tempDir)) {
			try{
				Files.createDirectory(tempDir);
			}catch (Exception ex) {
				throw new Exception("Unable to create temporary files directory in filestore.  Cannot upgrade SMART."); //$NON-NLS-1$
			}
		}
		
		String query = "SELECT uuid FROM smart.conservation_area"; //$NON-NLS-1$
		try(ResultSet rs = c.createStatement().executeQuery(query)){
			while(rs.next()) {
				UUID cauuid = (UUID) rs.getObject(1);
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
	
	private void upgradeConfigurableModel5To6(Connection c) throws SQLException {
		//this code is run as a part of the databaes sql script; it is run there so we also create necessary triggers 
//		String[] sql = new String[] {
//				"CREATE TABLE smart.cm_attribute_config(uuid char(16) for bit data not null, cm_uuid char(16) for bit data not null, dm_attribute_uuid char(16) for bit data not null, display_mode varchar(10), is_default boolean, primary key (uuid))", //$NON-NLS-1$
//				"ALTER TABLE smart.cm_attribute_config ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_CM_UUID_FK FOREIGN KEY (CM_UUID) REFERENCES SMART.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
//				"ALTER TABLE smart.cm_attribute_config ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_DM_ATTRIBUTE_UUID_FK FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
//
//				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO manager", //$NON-NLS-1$
//				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO data_entry", //$NON-NLS-1$
//				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO analyst", //$NON-NLS-1$
//
//				"alter table smart.cm_attribute add column config_uuid char(16) for bit data", //$NON-NLS-1$
//				"ALTER TABLE smart.cm_attribute ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
//
//				"alter table smart.cm_attribute_list add column config_uuid char(16) for bit data", //$NON-NLS-1$
//				"ALTER TABLE SMART.CM_ATTRIBUTE_LIST ADD CONSTRAINT CM_ATTRIBUTE_LIST_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$ 
//
//				"alter table smart.cm_attribute_tree_node add column config_uuid char(16) for bit data", //$NON-NLS-1$
//				"ALTER TABLE SMART.CM_ATTRIBUTE_TREE_NODE ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$ 
//		};
		
		//disable triggers
		c.createStatement().executeUpdate("SET session_replication_role = replica"); //$NON-NLS-1$
		
		populateConfigs(c, "smart.CM_ATTRIBUTE_LIST"); //$NON-NLS-1$
		populateConfigs(c, "smart.CM_ATTRIBUTE_TREE_NODE"); //$NON-NLS-1$
		
		String[] sql = new String[] {
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

				//change ca version so users cannot sync with this and cause problems
				"update connect.ca_info SET version = uuid_generate_v4()", //$NON-NLS-1$
				"delete from connect.change_log", //$NON-NLS-1$
				"delete from connect.change_log_history", //$NON-NLS-1$
		};

		for (String s : sql) {
			logger.log(Level.INFO, s);
			c.createStatement().execute(s);
		}
		//re-enable triggers
		c.createStatement().executeUpdate("SET session_replication_role = DEFAULT"); //$NON-NLS-1$
	}
	
	private UUID createUuid(Connection c) throws SQLException {
		try(ResultSet rs2 = c.createStatement().executeQuery("select uuid_generate_v4()")){ //$NON-NLS-1$
			if (rs2.next()) return (UUID)rs2.getObject(1);
		}
		throw new SQLException("Unable to generate a new uuid"); //$NON-NLS-1$
	}
	
	private void populateConfigs(Connection c, String tableName) throws SQLException {
		try (ResultSet rs = c.createStatement().executeQuery("select distinct CM_UUID, CM_ATTRIBUTE_UUID, DM_ATTRIBUTE_UUID from " + tableName)) { //$NON-NLS-1$
			while (rs.next()) {
				UUID cm_uuid = (UUID) rs.getObject(1);
				UUID cma_uuid = (UUID) rs.getObject(2);
				UUID dma_uuid = (UUID) rs.getObject(3);
				UUID cfg_uuid = createUuid(c);

				if (cma_uuid != null) {
					//this is custom config
					insertConfig(c, cfg_uuid, cm_uuid, getDmAttributeForCmAttribute(c, cma_uuid), getDisplayModeForCustomCmAttribute(c, cma_uuid), false);

					PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID = ? AND DM_ATTRIBUTE_UUID IS NULL"); //$NON-NLS-1$ //$NON-NLS-2$
					ps_upd.setObject(1, cfg_uuid);
					ps_upd.setObject(2, cm_uuid);
					ps_upd.setObject(3, cma_uuid);
					ps_upd.executeUpdate();

					PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE UUID = ?"); //$NON-NLS-1$
					ps_cma_upd.setObject(1, cfg_uuid);
					ps_cma_upd.setObject(2, cma_uuid);
					ps_cma_upd.executeUpdate();

				} else if (dma_uuid != null) {
					//this is default config
					insertConfig(c, cfg_uuid, cm_uuid, dma_uuid, getDisplayModeForDefaultAttribute(c, cm_uuid, dma_uuid), true);

					PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID IS NULL AND DM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$ //$NON-NLS-2$
					ps_upd.setObject(1, cfg_uuid);
					ps_upd.setObject(2, cm_uuid);
					ps_upd.setObject(3, dma_uuid);
					ps_upd.executeUpdate();

					PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE CONFIG_UUID IS NULL and ATTRIBUTE_UUID = ? AND NODE_UUID IN (select uuid from smart.cm_node where cm_uuid = ?)"); //$NON-NLS-1$
					ps_cma_upd.setObject(1, cfg_uuid);
					ps_cma_upd.setObject(2, dma_uuid);
					ps_cma_upd.setObject(3, cm_uuid);
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
				UUID cm_uuid = (UUID) rs.getObject(1);
				UUID cma_uuid = (UUID) rs.getObject(2);
				UUID cfg_uuid = createUuid(c);

				//create custom config and make default
				insertConfig(c, cfg_uuid, cm_uuid, getDmAttributeForCmAttribute(c, cma_uuid), getDisplayModeForCustomCmAttribute(c, cma_uuid), true);

				PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID = ? AND DM_ATTRIBUTE_UUID IS NULL"); //$NON-NLS-1$ //$NON-NLS-2$
				ps_upd.setObject(1, cfg_uuid);
				ps_upd.setObject(2, cm_uuid);
				ps_upd.setObject(3, cma_uuid);
				ps_upd.executeUpdate();

				PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE UUID = ?"); //$NON-NLS-1$
				ps_cma_upd.setObject(1, cfg_uuid);
				ps_cma_upd.setObject(2, cma_uuid);
				ps_cma_upd.executeUpdate();
			}
		}
	}

	private void insertConfig(Connection c, UUID cfg_uuid, UUID cm_uuid, UUID dma_uuid, String displayMode, boolean isDefault) throws SQLException {
		try(PreparedStatement ps_cfg = c.prepareStatement("INSERT INTO smart.CM_ATTRIBUTE_CONFIG (UUID, CM_UUID, DM_ATTRIBUTE_UUID, DISPLAY_MODE, IS_DEFAULT) VALUES (?, ?, ?, ?, ?)")){ //$NON-NLS-1$
			ps_cfg.setObject(1, cfg_uuid);
			ps_cfg.setObject(2, cm_uuid);
			ps_cfg.setObject(3, dma_uuid);
			ps_cfg.setString(4, displayMode);
			ps_cfg.setBoolean(5, isDefault);
			ps_cfg.executeUpdate();
		}
		//assigning name for the config
		UUID lng_uuid = null;
		PreparedStatement ps_lng = c.prepareStatement("select lng.UUID from smart.LANGUAGE lng join smart.CONFIGURABLE_MODEL cm on lng.CA_UUID = cm.CA_UUID where lng.ISDEFAULT and cm.UUID = ?"); //$NON-NLS-1$
		ps_lng.setObject(1, cm_uuid);
		try (ResultSet rs = ps_lng.executeQuery()) {
			if (rs.next()) {
				lng_uuid = (UUID) rs.getObject(1);
			} else {
				throw new SQLException("Unable to detect default language while upgrading configurable model."); //$NON-NLS-1$
			}
		}

		String cfgName = "Configuration"; //$NON-NLS-1$ Some default that will be owerwritten
		try(PreparedStatement ps_name = c.prepareStatement("select VALUE from smart.I18N_LABEL where ELEMENT_UUID = ? AND LANGUAGE_UUID = ?")){ //$NON-NLS-1$
			ps_name.setObject(1, dma_uuid);
			ps_name.setObject(2, lng_uuid);
			try (ResultSet rs = ps_name.executeQuery()) {
				if (rs.next()) {
					cfgName = rs.getString(1);
				}
			}
		}
		if (!isDefault) {
			int customCount = 0;
			try(PreparedStatement ps_count = c.prepareStatement("select count(UUID) from smart.CM_ATTRIBUTE_CONFIG where DM_ATTRIBUTE_UUID = ? AND not IS_DEFAULT AND CM_UUID = ?")){ //$NON-NLS-1$
				ps_count.setObject(1, dma_uuid);
				ps_count.setObject(2, cm_uuid);
				try (ResultSet rs = ps_count.executeQuery()) {
					if (rs.next()) {
						customCount = rs.getInt(1);
					}
				}
				cfgName = "Custom " + cfgName + " " + customCount; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		try(	PreparedStatement ps_lbl = c.prepareStatement("INSERT INTO smart.I18N_LABEL (LANGUAGE_UUID, ELEMENT_UUID, VALUE) VALUES (?, ?, ?)") ){ //$NON-NLS-1$
			ps_lbl.setObject(1, lng_uuid);
			ps_lbl.setObject(2, cfg_uuid);
			ps_lbl.setString(3, cfgName);
			ps_lbl.executeUpdate();
		}
	}

	private String getDisplayModeForDefaultAttribute(Connection c, UUID cm_uuid, UUID dma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select DISPLAY_MODE from smart.CM_DM_ATTRIBUTE_SETTINGS where CM_UUID = ? AND DM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$
		ps.setObject(1, cm_uuid);
		ps.setObject(2, dma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private String getDisplayModeForCustomCmAttribute(Connection c, UUID cma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select STRING_VALUE from smart.CM_ATTRIBUTE_OPTION where OPTION_ID = 'DISPLAY_MODE' AND CM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$
		ps.setObject(1, cma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private UUID getDmAttributeForCmAttribute(Connection c, UUID cma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select ATTRIBUTE_UUID from smart.CM_ATTRIBUTE where UUID = ?"); //$NON-NLS-1$
		ps.setObject(1, cma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? (UUID)rs.getObject(1) : null;
		}
	}
}
