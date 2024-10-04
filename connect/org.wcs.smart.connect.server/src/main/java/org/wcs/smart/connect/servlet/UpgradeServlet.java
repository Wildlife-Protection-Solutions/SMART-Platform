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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.icon.FixedIconSet;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.i2.ProfileReport800Upgrader;
import org.wcs.smart.i2.model.IntelPermission;
import org.wcs.smart.incident.IncidentReport800Upgrader;
import org.wcs.smart.report.Report800Upgrader;
import org.wcs.smart.util.I18nUtil;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;

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
		List<String> warnings = new ArrayList<>();
		
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
				Tuple data = s.createNativeQuery(query, Tuple.class).uniqueResult();
				if (data == null) {
					request.setAttribute("javax.servlet.error.message", Messages.getString("UpgradeServlet.DbVersionInvalid", request.getLocale())); //$NON-NLS-1$ //$NON-NLS-2$ 
					request.getRequestDispatcher("WEB-INF/errorpages/unknown.jsp").forward(request, response); //$NON-NLS-1$
					return;
				}
				
				boolean updated = false;
				String version = (String) data.get(0);

				//if (!version.equals(HibernateManager.DATABASE_VERSION)) {
				//	request.setAttribute("javax.servlet.error.message", Messages.getString("UpgradeServlet.FSVersionInvalid", request.getLocale())); //$NON-NLS-1$ //$NON-NLS-2$ 
				//	request.getRequestDispatcher("WEB-INF/errorpages/unknown.jsp").forward(request, response); //$NON-NLS-1$
				//	return;
				//}
				if (!version.equals(HibernateManager.DATABASE_VERSION)) {
					if (version.equals("7.5.3") || version.equals("7.5.4")) { //$NON-NLS-1$ //$NON-NLS-2$
						//7.5.4 shouldn't exist as we didn't upgrade version number
						upgradeDb754to757(s);
						upgradeDb757to800(s, warnings);
						upgradeDb800to801(s, warnings);
						upgradeDb801to810(s, warnings);
						updated = true;
					}else if (version.equals("7.5.5") || version.equals("7.5.6") || version.equals("7.5.7")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						//7.5.5/6 shouldn't exist as we didn't upgrade version number
						upgradeDb757to800(s, warnings);
						upgradeDb800to801(s, warnings);
						upgradeDb801to810(s, warnings);
						updated = true;
					}else if (version.equals("8.0.0")) { //$NON-NLS-1$ 
						//7.5.5/6 shouldn't exist as we didn't upgrade version number
						upgradeDb800to801(s, warnings);
						upgradeDb801to810(s, warnings);
						updated = true;
					}else if (version.equals("8.0.1")) { //$NON-NLS-1$ 
						//7.5.5/6 shouldn't exist as we didn't upgrade version number
						upgradeDb801to810(s, warnings);
						updated = true;
					}else {
						request.setAttribute("javax.servlet.error.message", Messages.getString("UpgradeServlet.FSVersionInvalid", request.getLocale())); //$NON-NLS-1$ //$NON-NLS-2$ 
						request.getRequestDispatcher("WEB-INF/errorpages/unknown.jsp").forward(request, response); //$NON-NLS-1$
						return;	
					}	
				}

				
				String filestoreVersion = (String) data.get(1);

				if (!filestoreVersion.equals(HibernateManager.FILESTORE_VERSION)) {
					updated = true;
					if (filestoreVersion.equals("5.0.0")) { //$NON-NLS-1$
						upgrade500to600(s);
						upgrade600to620(s);
						upgrade620to630(s);
						upgrade630to700(s);
						upgrade700to751(s);
						upgrade751to752(s);
						upgrade752to800(s);
					}else if (filestoreVersion.equals("6.0.0")) { //$NON-NLS-1$
						upgrade600to620(s);
						upgrade620to630(s);
						upgrade630to700(s);
						upgrade700to751(s);
						upgrade751to752(s);
						upgrade752to800(s);
					}else if (filestoreVersion.equals("6.2.0")) { //$NON-NLS-1$
						upgrade620to630(s);
						upgrade630to700(s);
						upgrade700to751(s);
						upgrade751to752(s);
						upgrade752to800(s);
					}else if (filestoreVersion.equals("6.3.0")) { //$NON-NLS-1$
						upgrade630to700(s);
						upgrade700to751(s);
						upgrade751to752(s);
						upgrade752to800(s);
					}else if (filestoreVersion.equals("7.0.0")) { //$NON-NLS-1$
						upgrade700to751(s);
						upgrade751to752(s);
						upgrade752to800(s);
					}else if (filestoreVersion.equals("7.5.1")) { //$NON-NLS-1$
						upgrade751to752(s);
						upgrade752to800(s);
					}else if (filestoreVersion.equals("7.5.2")) { //$NON-NLS-1$
						upgrade751to752(s);
						upgrade752to800(s);
					}else {
						throw new Exception("Invalid filestore version - cannot perform upgrade"); //$NON-NLS-1$
					}
					
					//update filestore version
					String sql = "UPDATE connect.connect_version set filestore_version = :version"; //$NON-NLS-1$
					s.createNativeMutationQuery(sql)
						.setParameter("version",  HibernateManager.FILESTORE_VERSION) //$NON-NLS-1$
						.executeUpdate();
				}
				
				s.getTransaction().commit();
				
				if (!updated) {
					//we are up to date; there is nothing to do here
					request.setAttribute("org.wcs.smart.upgrade", "NOACTION");  //$NON-NLS-1$//$NON-NLS-2$
					request.getRequestDispatcher("WEB-INF/upgrade.jsp").forward(request, response); //$NON-NLS-1$
					return;
				}
				

				//we are up to date; there is nothing to do here
				request.setAttribute("org.wcs.smart.upgrade", "UPGRADE_COMPLETE"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!warnings.isEmpty()) {
					request.setAttribute("org.wcs.smart.warnings", warnings); //$NON-NLS-1$ 
				}
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
	
	private void upgrade600to620(Session s) {
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					createIcons(c);
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});
	}
	
	private void upgrade620to630(Session s) {
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					upgradeIcons(c, "afropavo_congensis", "xenopirostris_damii");  //$NON-NLS-1$//$NON-NLS-2$
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});
	}
	private void upgrade630to700(Session s) {
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					//disable triggers
					c.createStatement().executeUpdate("SET session_replication_role = replica"); //$NON-NLS-1$
					
					updateProfilesV6toV7(c);
					
					//run these commands at end
					//change ca version so users cannot sync with this and cause problems
					c.createStatement().execute("update connect.ca_info SET version = uuid_generate_v4()"); //$NON-NLS-1$
					c.createStatement().execute("delete from connect.change_log"); //$NON-NLS-1$
					c.createStatement().execute("delete from connect.change_log_history"); //$NON-NLS-1$
					
					
					//re-enable triggers
					c.createStatement().executeUpdate("SET session_replication_role = DEFAULT"); //$NON-NLS-1$
					
					
					//delete all intelligence data from datastore
					String dir = "intelligence"; //$NON-NLS-1$
					Path rootFs = Paths.get(SmartContext.INSTANCE.getFilestoreLocation());
					try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.conservation_area")){ //$NON-NLS-1$
						while(rs.next()) {
							UUID cauuid = (UUID) rs.getObject(1);
							Path inteldir = rootFs.resolve(UuidUtils.uuidToString(cauuid)).resolve(dir);
							if (Files.exists(inteldir)) {
								FileUtils.deleteDirectory(inteldir.toFile());
							}
						}
					}
					
					//upgrade icons
					upgradeIcons(c, "agouti_paca", "tragelaphus_strepsiceros");  //$NON-NLS-1$//$NON-NLS-2$
					
					//create ccaa icon sets
					createCcaaIconSets(c);
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});
		
		
		
	}
	
	
	private void upgrade700to751(Session s) {
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					//disable triggers
					c.createStatement().executeUpdate("SET session_replication_role = replica"); //$NON-NLS-1$
					
					//run these commands at end
					//change ca version so users cannot sync with this and cause problems
					c.createStatement().execute("update connect.ca_info SET version = uuid_generate_v4()"); //$NON-NLS-1$
					c.createStatement().execute("delete from connect.change_log"); //$NON-NLS-1$
					c.createStatement().execute("delete from connect.change_log_history"); //$NON-NLS-1$
					
					
					//upgrade icons
					upgradeIcons(c, "c38_special_2", "yes");  //$NON-NLS-1$//$NON-NLS-2$
					
					//re-enable triggers
					c.createStatement().executeUpdate("SET session_replication_role = DEFAULT"); //$NON-NLS-1$
					
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});	
	}
	
	private void upgrade751to752(Session s) throws IOException{
		//need to delete all cached conservation area exports
		
		
		Path exportDirectory = DataStoreManager.INSTANCE.getRootDirectory()
				.resolve(DataStoreManager.CA_EXPORT_LOCATION);
		if (Files.exists(exportDirectory)) {
			try(Stream<Path> files = Files.list(exportDirectory)){
				files.forEach(f->{
					if (!Files.isDirectory(f)) {
						try {
							Files.delete(f);
						}catch (Exception ex) {
							logger.log(Level.WARNING, MessageFormat.format("Could not delete file: {0} during upgrade. File should be removed manually or desktop data will be inconsistent.", f.toString())); //$NON-NLS-1$
						}
					}
				});
			}
		}
	}
	
	private void upgrade752to800(Session s) throws IOException{

	}
	
	private void updateProfilesV6toV7(Connection c) throws SQLException {
		String profilekey = "profile1"; //$NON-NLS-1$
		String profilename = "Profile 1"; //$NON-NLS-1$
		int color = (new java.awt.Color(51,68,107)).getRGB();
		
		//for each ca we need to create some sort of default profile
		HashMap<UUID, UUID> caProfileUuids = new HashMap<>();

		PreparedStatement ps = c.prepareStatement("INSERT INTO smart.i_profile_config(uuid, ca_uuid, keyid, color) VALUES(?,?,?,?)"); //$NON-NLS-1$
		try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.conservation_area")){ //$NON-NLS-1$
			
			while(rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				if (uuid.equals(ConservationArea.MULTIPLE_CA)) continue;
				
				UUID puuid = createUuid(c);
				
				ps.setObject(1, puuid);
				ps.setObject(2,  uuid);
				ps.setString(3, profilekey);
				ps.setInt(4, color);
				ps.execute();
				
		
				caProfileUuids.put(uuid, puuid);
			}
		}
		
		//add name to i18n table
		c.createStatement().execute("INSERT INTO smart.i18n_label (language_uuid, element_uuid, value) " //$NON-NLS-1$
				+ "SELECT a.uuid, b.uuid, '" + profilename + "' FROM smart.language a join smart.i_profile_config b " //$NON-NLS-1$ //$NON-NLS-2$
				+ " on a.ca_uuid = b.ca_uuid"); //$NON-NLS-1$
			
		
		String[] sql = new String[] {
				"INSERT INTO smart.i_profile_entity_type(entity_type_uuid, profile_uuid) SELECT  a.uuid, b.uuid FROM smart.i_entity_type a, smart.i_profile_config b where a.ca_uuid = b.ca_uuid ", //$NON-NLS-1$
				"INSERT INTO smart.i_profile_record_source(record_source_uuid, profile_uuid) SELECT a.uuid, b.uuid FROM smart.i_recordsource a, smart.i_profile_config b where a.ca_uuid = b.ca_uuid ", //$NON-NLS-1$
					
				"update smart.i_entity set profile_uuid = (select b.uuid from smart.I_PROFILE_CONFIG b where b.ca_uuid = smart.i_entity.ca_uuid)", //$NON-NLS-1$
				"update smart.i_record set profile_uuid = (select b.uuid from smart.I_PROFILE_CONFIG b where b.ca_uuid = smart.i_record.ca_uuid)", //$NON-NLS-1$
				
				"update smart.i_relationship_type set src_profile_uuid = (select b.uuid from smart.I_PROFILE_CONFIG b where b.ca_uuid = smart.i_relationship_type.ca_uuid)", //$NON-NLS-1$
				"update smart.i_relationship_type set target_profile_uuid = src_profile_uuid", //$NON-NLS-1$
				
				"ALTER TABLE smart.i_entity ALTER COLUMN profile_uuid set not null", //$NON-NLS-1$
				"ALTER TABLE smart.i_record ALTER COLUMN profile_uuid set not null", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ALTER COLUMN src_profile_uuid SET not null", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ALTER COLUMN target_profile_uuid SET not null", //$NON-NLS-1$
				
				"UPDATE smart.I_ENTITY_RECORD_QUERY set profile_filter = '" + profilekey + "'",  //$NON-NLS-1$ //$NON-NLS-2$
				"UPDATE smart.i_entity_summary_query set profile_filter = '" + profilekey + "'",  //$NON-NLS-1$ //$NON-NLS-2$
				"UPDATE smart.i_record_obs_query set profile_filter = '" + profilekey + "'", //$NON-NLS-1$ //$NON-NLS-2$
				
				
		};
		for (String s : sql) c.createStatement().execute(s);
		
		
		HashMap<UUID, Set<String>> usedkeys = new HashMap<>();
		
		//attribute sources
		ps = c.prepareStatement("UPDATE smart.i_recordsource_attribute SET keyid = ? WHERE uuid = ?"); //$NON-NLS-1$
		try(ResultSet rs = c.createStatement().executeQuery("select a.uuid, b.keyid, a.source_uuid from smart.I_RECORDSOURCE_ATTRIBUTE a join smart.i_attribute b on a.attribute_uuid = b.uuid")){ //$NON-NLS-1$
			while(rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				UUID srcuuid = (UUID)rs.getObject(3);
				String keyid = rs.getString(2);
			
				if (!usedkeys.containsKey(srcuuid)) usedkeys.put(srcuuid, new HashSet<>());
				
				Set<String> used = usedkeys.get(srcuuid);
				String root = keyid;
				int i = 1;
				while(used.contains(keyid)) {
					keyid = root + i;
					i++;
				}
				used.add(keyid);
				
				ps.setString(1,  keyid);
				ps.setObject(2,  uuid);
				ps.executeUpdate();
			}
		}
		//repeat for entity sources
		ps = c.prepareStatement("UPDATE smart.i_recordsource_attribute SET keyid = ? WHERE uuid = ?"); //$NON-NLS-1$

		try(ResultSet rs = c.createStatement().executeQuery("select a.uuid, b.keyid, a.source_uuid from smart.I_RECORDSOURCE_ATTRIBUTE a join smart.i_entity_type b on a.entity_type_uuid = b.uuid")){ //$NON-NLS-1$
			while(rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				UUID srcuuid = (UUID)rs.getObject(3);
				String keyid = rs.getString(2);
			
				if (!usedkeys.containsKey(srcuuid)) usedkeys.put(srcuuid, new HashSet<>());
				
				Set<String> used = usedkeys.get(srcuuid);
				String root = keyid;
				int i = 1;
				while(used.contains(keyid)) {
					keyid = root + i;
					i++;
				}
				used.add(keyid);
				
				ps.setString(1,  keyid);
				ps.setObject(2,  uuid);
				ps.executeUpdate();
			}
		}		

		c.createStatement().executeUpdate("alter table smart.i_recordsource_attribute alter column keyid set not null"); //$NON-NLS-1$


		//need to map old permissions to new ones
		//remove old permission from employee
		PreparedStatement psinsert = c.prepareStatement("INSERT INTO smart.i_permission (employee_uuid, profile_uuid, permissions) VALUES(?,?,?)"); //$NON-NLS-1$
		PreparedStatement psupdate = c.prepareStatement("UPDATE smart.employee set smartuserlevel = ? where uuid = ?"); //$NON-NLS-1$

		try(ResultSet rs = c.createStatement().executeQuery("select uuid, ca_uuid, smartuserlevel FROM smart.EMPLOYEE where smartuserlevel is not null")){ //$NON-NLS-1$
			while(rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				UUID cauuid = (UUID)rs.getObject(2);
				String userlevel = (String)rs.getString(3);
			
				String[] parts = userlevel.split(","); //$NON-NLS-1$
			
				List<String> newparts = new ArrayList<>();
				Set<Integer> intelpermissions = new HashSet<>();
				
				for (String bit : parts) {
					if (bit.equalsIgnoreCase("INTEL_ANALYST")) { //$NON-NLS-1$
						newparts.add("INTEL_ADMIN"); //$NON-NLS-1$
						intelpermissions.add(IntelPermission.ADMIN);
						
					}else if (bit.toUpperCase(Locale.ROOT).startsWith("INTEL_")) { //$NON-NLS-1$
						if(!newparts.contains("INTEL_USER")) newparts.add("INTEL_USER"); //$NON-NLS-1$ //$NON-NLS-2$
						
						if (bit.equalsIgnoreCase("INTEL_ENTITY_CREATE")) intelpermissions.add(IntelPermission.ENTITY_CREATE); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_ENTITY_DELETE")) intelpermissions.add(IntelPermission.ENTITY_DELETE); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_ENTITY_EDIT")) intelpermissions.add(IntelPermission.ENTITY_EDIT); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_ENTITY_VIEW")) intelpermissions.add(IntelPermission.ENTITY_VIEW); //$NON-NLS-1$
						
						if (bit.equalsIgnoreCase("INTEL_RECORD_CREATE")) intelpermissions.add(IntelPermission.RECORD_CREATE); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_RECORD_DELETE")) intelpermissions.add(IntelPermission.RECORD_DELETE); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_RECORD_EDIT")) intelpermissions.add(IntelPermission.RECORD_EDIT_NOTSTATUS); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_RECORD_EDIT_WITH_STATUS")) intelpermissions.add(IntelPermission.RECORD_EDIT_ALL); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_RECORD_VIEW")) intelpermissions.add(IntelPermission.RECORD_VIEW); //$NON-NLS-1$
						
						if (bit.equalsIgnoreCase("INTEL_QUERY_ALL")) intelpermissions.add(IntelPermission.QUERY); //$NON-NLS-1$
						if (bit.equalsIgnoreCase("INTEL_READ_ONLY")) intelpermissions.add(IntelPermission.READ_ONLY); //$NON-NLS-1$
						
					}else {
						newparts.add(bit);
					}
				}
				if (intelpermissions.isEmpty()) continue;
				
				if (newparts.contains("INTEL_ADMIN") && newparts.contains("INTEL_USER")) newparts.remove("INTEL_USER"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				if (intelpermissions.contains(IntelPermission.ADMIN)) {
					//remove all others
					intelpermissions.clear();
					intelpermissions.add(IntelPermission.ADMIN);
				}
				
				int permission = 0;
				for (Integer i : intelpermissions) permission = permission | i;
	
				//insert permission
				psinsert.setObject(1, uuid);
				psinsert.setObject(2,  caProfileUuids.get(cauuid));
				psinsert.setInt(3, permission);
				psinsert.execute();
				
				
				//update employee
				StringBuilder sb = new StringBuilder();
				sb.append(newparts.get(0));
				for (int i = 1; i < newparts.size(); i ++) {
					sb.append(","); //$NON-NLS-1$
					sb.append(newparts.get(i));
				}
				psupdate.setString(1, sb.toString());
				psupdate.setObject(2, uuid);
				psupdate.execute();
				
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
	
	private void createIcons(Connection c) throws SQLException {
		PreparedStatement psiconset = c.prepareStatement("INSERT INTO smart.iconset (uuid, keyid, ca_uuid, is_default) VALUES (?, ?, ?, ?)");		 //$NON-NLS-1$
		PreparedStatement pslabel = c.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psicon = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psiconfile = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) VALUES(?, ?, ?, ?)"); //$NON-NLS-1$

		
		//create default icon sets
		//NOTE: We cannot use hibernate objects here - this may cause issues in the future
		try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.conservation_area")){ //$NON-NLS-1$
			while(rs.next()) {
				UUID cuuid = (UUID)rs.getObject(1);
				if (cuuid.equals(ConservationArea.MULTIPLE_CA)) continue;
				
				PreparedStatement ps = c.prepareStatement("SELECT uuid FROM smart.language WHERE ca_uuid = ? and isdefault"); //$NON-NLS-1$
				ps.setObject(1, cuuid);
				
				UUID luuid = null;
				try(ResultSet rs2 = ps.executeQuery()){
					if (!rs2.next()) continue; //no default language for this ca; skip
					luuid = (UUID)rs2.getObject(1);
				}
				
				if (luuid == null) continue; 
				
				UUID lineuuid = createUuid(c);
				psiconset.setObject(1, lineuuid);
				psiconset.setString(2, "line"); //$NON-NLS-1$
				psiconset.setObject(3, cuuid);
				psiconset.setBoolean(4, false);
				psiconset.addBatch();
				
				pslabel.setObject(1, luuid);
				pslabel.setObject(2, lineuuid);
				pslabel.setString(3, "Outline Only"); //$NON-NLS-1$
				pslabel.addBatch();
				
				UUID blackuuid = createUuid(c);
				psiconset.setObject(1, blackuuid);
				psiconset.setString(2, "black"); //$NON-NLS-1$
				psiconset.setObject(3, cuuid);
				psiconset.setBoolean(4, false);
				psiconset.addBatch();
				
				pslabel.setObject(1, luuid);
				pslabel.setObject(2, blackuuid);
				pslabel.setString(3, "Black and White"); //$NON-NLS-1$
				pslabel.addBatch();
				
				UUID coloruuid =createUuid(c);
				psiconset.setObject(1, coloruuid);
				psiconset.setString(2, "color"); //$NON-NLS-1$
				psiconset.setObject(3, cuuid);
				psiconset.setBoolean(4, true);
				psiconset.addBatch();
				
				pslabel.setObject(1, luuid);
				pslabel.setObject(2, coloruuid);
				pslabel.setString(3, "Full Color"); //$NON-NLS-1$
				pslabel.addBatch();
				
				psiconset.executeBatch();
				pslabel.executeBatch();
				
				
				for (String[] icon : IconUtils.INSTANCE.SMART_ICON_MAPPING) {
					
					UUID iconuuid = createUuid(c);
					
					psicon.setObject(1, iconuuid);
					psicon.setString(2, icon[0]);
					psicon.setObject(3, cuuid);
					psicon.addBatch();
					
					pslabel.setObject(1, luuid);
					pslabel.setObject(2, iconuuid);
					pslabel.setString(3, icon[1]);
					pslabel.addBatch();
					
					UUID fileuuid = createUuid(c);
					psiconfile.setObject(1, fileuuid);
					psiconfile.setObject(2, iconuuid);
					psiconfile.setObject(3, blackuuid);
					psiconfile.setString(4, icon[2]);
					psiconfile.addBatch();
					
					fileuuid = createUuid(c);
					psiconfile.setObject(1, fileuuid);
					psiconfile.setObject(2, iconuuid);
					psiconfile.setObject(3, lineuuid);
					psiconfile.setString(4, icon[3]);
					psiconfile.addBatch();
					
					fileuuid = createUuid(c);
					psiconfile.setObject(1, fileuuid);
					psiconfile.setObject(2, iconuuid);
					psiconfile.setObject(3, coloruuid);
					psiconfile.setString(4, icon[4]);
					psiconfile.addBatch();
					
					
					psicon.executeBatch();
					pslabel.executeBatch();
					psiconfile.executeBatch();
					
					
					//update data model items
					IconUtils.INSTANCE.upgradeDataModel(c, iconuuid, icon[5], cuuid);
				}
				
			}
		}

	}
	
	
	private void upgradeIcons(Connection c, String startkey, String endkey) throws SQLException {

		PreparedStatement pslabel = c.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psicon = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psiconfile = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) VALUES(?, ?, ?, ?)"); //$NON-NLS-1$

		
		//create default icon sets
		//NOTE: We cannot use hibernate objects here - this may cause issues in the future
		try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.conservation_area")){ //$NON-NLS-1$
			while(rs.next()) {
				UUID cuuid = (UUID)rs.getObject(1);
				if (cuuid.equals(ConservationArea.MULTIPLE_CA)) continue;
				
				PreparedStatement ps = c.prepareStatement("SELECT uuid FROM smart.language WHERE ca_uuid = ? and isdefault"); //$NON-NLS-1$
				ps.setObject(1, cuuid);
				
				UUID luuid = null;
				try(ResultSet rs2 = ps.executeQuery()){
					if (!rs2.next()) continue; //no default language for this ca; skip
					luuid = (UUID)rs2.getObject(1);
				}
				
				if (luuid == null) continue; 
				
				UUID lineuuid = null;
				UUID blackuuid = null;
				UUID coloruuid = null;
				try(PreparedStatement ps1 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = 'line' AND ca_uuid = ?")){ //$NON-NLS-1$
					ps1.setObject(1,  cuuid);
					try(ResultSet rs1 = ps1.executeQuery()){
						if (rs1.next()) {
							lineuuid = (UUID)rs1.getObject(1);
						}
					}
				}
				try(PreparedStatement ps1 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = 'black' AND ca_uuid = ?")){ //$NON-NLS-1$
					ps1.setObject(1,  cuuid);
					try(ResultSet rs1 = ps1.executeQuery()){
						if (rs1.next()) {
							blackuuid = (UUID)rs1.getObject(1);
						}
					}
				}
				
				try(PreparedStatement ps1 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = 'color' AND ca_uuid = ?")){ //$NON-NLS-1$
					ps1.setObject(1,  cuuid);
					try(ResultSet rs1 = ps1.executeQuery()){
						if (rs1.next()) {
							coloruuid = (UUID)rs1.getObject(1);
						}
					}
				}
				
				boolean found = false;				
				for (String[] icon : IconUtils.INSTANCE.SMART_ICON_MAPPING) {
					//anything after this is new in SMART630
					if (icon[0].equalsIgnoreCase(startkey)) {
						found = true;
					}
					if (!found) continue;
					
					UUID iconuuid = createUuid(c);
					
					psicon.setObject(1, iconuuid);
					psicon.setString(2, icon[0]);
					psicon.setObject(3, cuuid);
					psicon.addBatch();
					
					pslabel.setObject(1, luuid);
					pslabel.setObject(2, iconuuid);
					pslabel.setString(3, icon[1]);
					pslabel.addBatch();
					
					UUID fileuuid = createUuid(c);
					psiconfile.setObject(1, fileuuid);
					psiconfile.setObject(2, iconuuid);
					psiconfile.setObject(3, blackuuid);
					psiconfile.setString(4, icon[2]);
					psiconfile.addBatch();
					
					fileuuid = createUuid(c);
					psiconfile.setObject(1, fileuuid);
					psiconfile.setObject(2, iconuuid);
					psiconfile.setObject(3, lineuuid);
					psiconfile.setString(4, icon[3]);
					psiconfile.addBatch();
					
					fileuuid = createUuid(c);
					psiconfile.setObject(1, fileuuid);
					psiconfile.setObject(2, iconuuid);
					psiconfile.setObject(3, coloruuid);
					psiconfile.setString(4, icon[4]);
					psiconfile.addBatch();
					
					
					psicon.executeBatch();
					pslabel.executeBatch();
					psiconfile.executeBatch();
					
					
					//update data model items
					IconUtils.INSTANCE.upgradeDataModel(c, iconuuid, icon[5], cuuid);
					
					if (icon[0].equalsIgnoreCase(endkey)) break;
				}
				
			}
		}

	}
	
	
	private void createCcaaIconSets(Connection c) throws SQLException {
		
		//first see if CCAA ca exists
		try(PreparedStatement psca = c.prepareStatement("SELECT count(*) FROM smart.conservation_area WHERE uuid = ?")){ //$NON-NLS-1$
			psca.setObject(1, ConservationArea.MULTIPLE_CA);
			try(ResultSet rs = psca.executeQuery()){
				rs.next();
				if (rs.getInt(1) == 0) return;
			}
		}
		
		PreparedStatement psiconset = c
				.prepareStatement("INSERT INTO smart.iconset (uuid, keyid, ca_uuid, is_default) VALUES (?, ?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement pslabel = c
				.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psicon = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) VALUES(?, ?, ?)"); //$NON-NLS-1$
		PreparedStatement psiconfile = c.prepareStatement(
				"INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) VALUES(?, ?, ?, ?)"); //$NON-NLS-1$

		UUID cuuid = ConservationArea.MULTIPLE_CA;

		UUID lineuuid = createUuid(c);
		UUID blackuuid = createUuid(c);
		UUID coloruuid = createUuid(c);

		PreparedStatement ps = c.prepareStatement("SELECT uuid FROM smart.language WHERE ca_uuid = ?"); //$NON-NLS-1$
		ps.setObject(1, cuuid);

		psiconset.setObject(1, lineuuid);
		psiconset.setString(2, FixedIconSet.LINE.key);
		psiconset.setObject(3, cuuid);
		psiconset.setBoolean(4, false);
		psiconset.addBatch();

		psiconset.setObject(1, blackuuid);
		psiconset.setString(2, FixedIconSet.BLACK.key);
		psiconset.setObject(3, cuuid);
		psiconset.setBoolean(4, false);
		psiconset.addBatch();

		psiconset.setObject(1, coloruuid);
		psiconset.setString(2, FixedIconSet.COLOR.key);
		psiconset.setObject(3, cuuid);
		psiconset.setBoolean(4, true);
		psiconset.addBatch();

		List<UUID> langs = new ArrayList<>();
		try (ResultSet rs2 = ps.executeQuery()) {
			while (rs2.next()) {
				UUID language = (UUID) rs2.getObject(1);
				langs.add(language);
			}
		}
		if (langs.isEmpty()) {
			UUID luuid = createUuid(c);
			langs.add(luuid);
			
			PreparedStatement l = c.prepareStatement("INSERT INTO smart.language (uuid, ca_uuid, isdefault, code) VALUES (?,?,?,?)"); //$NON-NLS-1$
			l.setObject(1, luuid);
			l.setObject(2, cuuid);
			l.setBoolean(3, true);
			l.setString(4, Locale.getDefault().getLanguage());
			
			l.execute();
		}
		
		for (UUID language : langs) {
			pslabel.setObject(1, language);
			pslabel.setObject(2, lineuuid);
			pslabel.setString(3, FixedIconSet.LINE.name);
			pslabel.addBatch();

			pslabel.setObject(1, language);
			pslabel.setObject(2, blackuuid);
			pslabel.setString(3, FixedIconSet.BLACK.name);
			pslabel.addBatch();

			pslabel.setObject(1, language);
			pslabel.setObject(2, coloruuid);
			pslabel.setString(3, FixedIconSet.COLOR.name);
			pslabel.addBatch();
		}

		psiconset.executeBatch();
		pslabel.executeBatch();

		for (String[] icon : IconUtils.INSTANCE.SMART_ICON_MAPPING) {

			UUID iconuuid = createUuid(c);

			psicon.setObject(1, iconuuid);
			psicon.setString(2, icon[0]);
			psicon.setObject(3, cuuid);
			psicon.addBatch();

			for (UUID luuid : langs) {
				pslabel.setObject(1, luuid);
				pslabel.setObject(2, iconuuid);
				pslabel.setString(3, icon[1]);
				pslabel.addBatch();
			}

			UUID fileuuid = createUuid(c);
			psiconfile.setObject(1, fileuuid);
			psiconfile.setObject(2, iconuuid);
			psiconfile.setObject(3, blackuuid);
			psiconfile.setString(4, icon[2]);
			psiconfile.addBatch();

			fileuuid = createUuid(c);
			psiconfile.setObject(1, fileuuid);
			psiconfile.setObject(2, iconuuid);
			psiconfile.setObject(3, lineuuid);
			psiconfile.setString(4, icon[3]);
			psiconfile.addBatch();

			fileuuid = createUuid(c);
			psiconfile.setObject(1, fileuuid);
			psiconfile.setObject(2, iconuuid);
			psiconfile.setObject(3, coloruuid);
			psiconfile.setString(4, icon[4]);
			psiconfile.addBatch();

			psicon.executeBatch();
			pslabel.executeBatch();
			psiconfile.executeBatch();

		}
	}
	
	
	private void upgradeDb754to757(Session s) {
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					//disable triggers
					c.createStatement().executeUpdate("SET session_replication_role = replica"); //$NON-NLS-1$
					
					//https://app.assembla.com/spaces/smart-cs/tickets/realtime_list?ticket=3518
					StringBuilder sb = new StringBuilder();
					sb.append("with errors as ("); //$NON-NLS-1$
					sb.append("select a.uuid AS list_item_uuid,  d.uuid AS correct_icon_uuid "); //$NON-NLS-1$
					sb.append("from smart.dm_attribute_list a join smart.dm_attribute b on a.attribute_uuid = b.uuid ");  //$NON-NLS-1$
					sb.append("join smart.icon c on c.uuid = a.icon_uuid ");  //$NON-NLS-1$
					sb.append("LEFT JOIN smart.icon d ON d.keyid = c.keyid AND d.ca_uuid = b.ca_uuid "); //$NON-NLS-1$
					sb.append("where b.ca_uuid != c.ca_uuid) "); //$NON-NLS-1$
					sb.append("update smart.dm_attribute_list set icon_uuid = e.correct_icon_uuid "); //$NON-NLS-1$
					sb.append("from errors e where e.list_item_uuid = smart.dm_attribute_list.uuid "); //$NON-NLS-1$

					c.createStatement().executeUpdate(sb.toString());
					
					String[] sql = new String[] {
						"update smart.waypoint set last_modified_by = null where last_modified_by not in (select uuid from smart.employee)", //$NON-NLS-1$
						"alter table smart.waypoint add constraint wp_last_modified_by_fk foreign key (last_modified_by) references smart.employee(uuid)", //$NON-NLS-1$
						
						//quick link support
						"CREATE TABLE smart.quicklink(uuid uuid not null, ca_uuid uuid not null, url varchar(32000), uiorder smallint, employee_uuid uuid, primary key (uuid) )", //$NON-NLS-1$
						"ALTER TABLE smart.quicklink ADD FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) on delete cascade on update restrict deferrable initially deferred", //$NON-NLS-1$
						"ALTER TABLE smart.quicklink ADD FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) on delete cascade on update restrict deferrable initially deferred", //$NON-NLS-1$
						"CREATE TRIGGER trg_quicklink AFTER INSERT OR UPDATE OR DELETE ON smart.quicklink FOR EACH ROW execute procedure connect.trg_changelog_common()", //$NON-NLS-1$
						
						//earth ranger
						"ALTER TABLE smart.configurable_model add column use_earth_ranger boolean default false", //$NON-NLS-1$
						
						//link incident to patrol
						"create table smart.incident_waypoint(wp_uuid uuid not null, patrol_uuid uuid, primary key (wp_uuid) )", //$NON-NLS-1$
						"ALTER TABLE smart.incident_waypoint ADD FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(uuid) on delete cascade on update restrict deferrable initially deferred", //$NON-NLS-1$
						"ALTER TABLE smart.incident_waypoint ADD FOREIGN KEY (patrol_uuid) REFERENCES smart.patrol(uuid) on delete cascade on update restrict deferrable initially deferred", //$NON-NLS-1$											
						
						
						"CREATE OR REPLACE FUNCTION connect.trg_incident_waypoint() RETURNS trigger AS $$ " + //$NON-NLS-1$
						"	DECLARE " + //$NON-NLS-1$
						"	ROW RECORD; " + //$NON-NLS-1$
						"BEGIN " + //$NON-NLS-1$
						"	IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN " +	 //$NON-NLS-1$
						" 	ROW = NEW; " + //$NON-NLS-1$
						" 	ELSIF (TG_OP = 'DELETE') THEN " + //$NON-NLS-1$
						" 		ROW = OLD; " + //$NON-NLS-1$
						" 	END IF; " + //$NON-NLS-1$
						" 	INSERT INTO connect.change_log " +  //$NON-NLS-1$
						" 		(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid) " + //$NON-NLS-1$
						" 		SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'wp_uuid', ROW.WP_UUID, 'patrol_uuid', ROW.patrol_uuid, null, wp.CA_UUID " +  //$NON-NLS-1$
						" 		FROM smart.waypoint wp WHERE wp.uuid = ROW.wp_uuid; " + //$NON-NLS-1$
						" RETURN ROW; " + //$NON-NLS-1$
						"END$$ LANGUAGE 'plpgsql'",  //$NON-NLS-1$
						
						"CREATE TRIGGER trg_incident_waypoint AFTER INSERT OR UPDATE OR DELETE ON smart.incident_waypoint FOR EACH ROW execute procedure connect.trg_incident_waypoint()", //$NON-NLS-1$
						
						"alter table smart.waypoint add column source_cm_uuid uuid", //$NON-NLS-1$
						"alter table smart.waypoint add foreign key (source_cm_uuid) references smart.configurable_model(uuid) on delete set null on update restrict deferrable initially deferred", //$NON-NLS-1$

						"delete from smart.CT_INCIDENT_LINK where obs_group_uuid is not null and obs_group_uuid not in (select uuid from smart.WP_OBSERVATION_GROUP)", //$NON-NLS-1$
						"alter table smart.ct_incident_link add foreign key (obs_group_uuid) references smart.wp_observation_group on delete cascade on update restrict deferrable initially deferred",  //$NON-NLS-1$

						
						"ALTER TABLE smart.cm_node add column integrate_incident_type varchar(32)", //$NON-NLS-1$
						
						"update connect.connect_plugin_version set version = '7.5' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '7.5' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$
								
						"update connect.connect_plugin_version set version = '7.5.7' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '7.5.7' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$
						"update connect.connect_version set version = '7.5.7', last_updated = now()" //$NON-NLS-1$
					};
					for (String s : sql) {
						//System.out.println(s);
						c.createStatement().executeUpdate(s);
					}
					
					//re-enable triggers
					c.createStatement().executeUpdate("SET session_replication_role = DEFAULT"); //$NON-NLS-1$
					
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});	
	}
	
	
	private void upgradeDb757to800(Session s, List<String> warnings) {
		//upgrade report files
		try {
			warnings.addAll( (new Report800Upgrader()).upgrade(s));
			warnings.addAll( (new IncidentReport800Upgrader()).upgrade(s));
			warnings.addAll( (new ProfileReport800Upgrader()).upgrade(s));
		} catch (SQLException e) {
			throw new HibernateException(e);
		}

		LocalDateTime utc = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
		String utcs = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss").format(utc); //$NON-NLS-1$
		
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					//disable triggers
					c.createStatement().executeUpdate("SET session_replication_role = replica"); //$NON-NLS-1$

					//breaking change for postgresql 14
					//Require custom server parameter names to use only characters that are valid in unquoted SQL identifiers (Tom Lane)
					StringBuilder sb = new StringBuilder();
					sb.append("CREATE OR REPLACE FUNCTION connect.dolog(cauuid uuid) RETURNS boolean "); //$NON-NLS-1$
					sb.append("    LANGUAGE plpgsql "); //$NON-NLS-1$
					sb.append("    AS $$ "); //$NON-NLS-1$
					sb.append("DECLARE "); //$NON-NLS-1$
					sb.append("canrun boolean; "); //$NON-NLS-1$
					sb.append("BEGIN "); //$NON-NLS-1$
					//sb.append("    --check if we should log this ca "); //$NON-NLS-1$
					sb.append("    select current_setting('ca.trigger.t' || replace(cauuid::varchar, '-', '')) into canrun; "); //$NON-NLS-1$
					sb.append("    return canrun; "); //$NON-NLS-1$
					sb.append("    EXCEPTION WHEN others THEN "); //$NON-NLS-1$
					sb.append("        RETURN TRUE; "); //$NON-NLS-1$
					sb.append("END$$; "); //$NON-NLS-1$
					
					//logger.log(Level.SEVERE, sb.toString());
					
					c.createStatement().execute(sb.toString());
					
					String[] sql = new String[] {
						"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY DROP COLUMN SURVEYDESIGN_KEY", //$NON-NLS-1$
						"ALTER TABLE smart.ct_metadata_value ADD COLUMN is_required boolean default false not null",  //$NON-NLS-1$
						
						//remove icon, iconfiles from conservation areas that are not referenced
						//leave any custom icons if they are used or not  
						"DELETE FROM smart.ICONFILE WHERE icon_uuid not in (select distinct icon_uuid from smart.dm_attribute where icon_uuid is not null union  select distinct icon_uuid from smart.DM_ATTRIBUTE_LIST where icon_uuid is not null union select distinct icon_uuid from smart.DM_ATTRIBUTE_TREE where icon_uuid is not null union select distinct icon_uuid from smart.DM_CATEGORY where icon_uuid is not null ) and  filename like 'platform%'", //$NON-NLS-1$
						"DELETE FROM smart.ICON WHERE uuid not in (SELECT icon_uuid FROM smart.iconfile)", //$NON-NLS-1$

						// patrol metadata icons
						"ALTER TABLE smart.patrol_mandate ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER table smart.patrol_mandate ADD CONSTRAINT pm_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.patrol_transport ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER table smart.patrol_transport ADD CONSTRAINT ptransport_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.team ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER table smart.team ADD CONSTRAINT team_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.patrol_attribute ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER table smart.patrol_attribute ADD CONSTRAINT patrol_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.patrol_attribute_list ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER TABLE smart.patrol_attribute_list ADD CONSTRAINT patrol_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.station ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER TABLE smart.station ADD CONSTRAINT station_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$


						//mission metadata icons
						// 5 to 6 upgrade for er
						"ALTER TABLE smart.mission_attribute ADD COLUMN icon_uuid uuid",  //$NON-NLS-1$
						"ALTER table smart.mission_attribute ADD CONSTRAINT mission_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.mission_attribute_list ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER table smart.mission_attribute_list ADD CONSTRAINT mission_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.sampling_unit_attribute ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER table smart.sampling_unit_attribute ADD CONSTRAINT su_attribute_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.sampling_unit_attribute_list ADD COLUMN icon_uuid uuid", //$NON-NLS-1$
						"ALTER table smart.sampling_unit_attribute_list ADD CONSTRAINT su_attribute_list_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon (uuid) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
					
						//working item				
						"alter table connect.work_item add column percent_complete smallint", //$NON-NLS-1$
						"alter table connect.work_item add column data varchar", //$NON-NLS-1$
						"ALTER TABLE connect.work_item drop CONSTRAINT type_chk",  //$NON-NLS-1$ 
						"ALTER TABLE connect.work_item ADD CONSTRAINT type_chk CHECK (((type)::text = ANY (ARRAY[('UP_CA'::character varying)::text, ('UP_SYNC'::character varying)::text, ('DOWN_CA'::character varying)::text, ('DOWN_SYNC'::character varying)::text, ('UP_DATAQUEUE'::character varying)::text, ('UP_CTPACKAGE'::character varying)::text, ('UP_NAVIGATION'::character varying)::text, ('RECOVERY_CA'::character varying)::text])))", //$NON-NLS-1$

						//ca property size
						"ALTER TABLE smart.conservation_area_property alter column value set data type varchar(32672)", //$NON-NLS-1$
						
						//add field_id to alert
						"alter table connect.alerts add column field_id varchar", //$NON-NLS-1$
						
						//hibernate 6 employee uuid cannot conflict with ccaa uuid
						//WRONG fiX - see comments in 8.0 to 8.0.1 upgrade section
						//"update smart.employee set uuid = '00000000000000000000000000000001' where uuid = '00000000000000000000000000000000'", //$NON-NLS-1$
						
						"alter table connect.data_queue drop constraint status_chk", //$NON-NLS-1$
						"alter table connect.data_queue add constraint status_chk CHECK (status = ANY (ARRAY['UPLOADING', 'QUEUED', 'PROCESSING', 'COMPLETE', 'COMPLETE_WARN', 'ERROR', 'DUPLICATE']))", //$NON-NLS-1$
								
						//warning message for connect processing smart mobile files
						"alter table connect.data_queue add column warning_message varchar", //$NON-NLS-1$
						
						"create table connect.settings(key varchar primary key, value varchar)", //$NON-NLS-1$
						"alter table connect.ca_info add column smartmobile_dq_processing boolean default true", //$NON-NLS-1$
						//delete from connect.settings where key = 'connect.dataqueue.smartmobile.processing'
						
						"insert into connect.settings(key, value) values ('connect.dataqueue.smartcollect.useroption', 'validaterequeue')", //$NON-NLS-1$
						
						"alter table smart.patrol_leg_day alter column end_time type time(0)", //$NON-NLS-1$
						"alter table smart.patrol_leg_day alter column start_time type time(0)", //$NON-NLS-1$
						"alter table smart.mission_day alter column end_time type time(0)", //$NON-NLS-1$
						"alter table smart.mission_day alter column start_time type time(0)", //$NON-NLS-1$
						
						//https://app.assembla.com/spaces/smart-cs/tickets/3643
						"alter table connect.alerts alter column date type timestamp with time zone", //$NON-NLS-1$

						//geometry support
						"ALTER TABLE smart.wp_observation_attributes add column geom bytea", //$NON-NLS-1$
						"ALTER TABLE smart.wp_observation_attributes add column number_value_2 double precision", //$NON-NLS-1$
						"ALTER TABLE smart.i_observation_attribute add column geom bytea", //$NON-NLS-1$
						"ALTER TABLE smart.i_observation_attribute add column double_value_2 double precision", //$NON-NLS-1$
						
						"ALTER TABLE smart.i_entity alter column created_by drop not null", //$NON-NLS-1$
						"ALTER TABLE smart.i_record alter column created_by drop not null", //$NON-NLS-1$
						"ALTER TABLE smart.i_attachment alter column created_by drop not null", //$NON-NLS-1$
						
						//remove cybertracker plugins
						"delete from smart.ct_properties_profile_option where option_id in ('AUTO_NEXT','USE_LARGE_TABS','USE_LARGE_TITLES','LARGE_SCROLL_BARS','USE_TITLE_BAR','SHOW_EDIT','SHOW_GPS','SIMPLE_CAMERA','USE_SD_CARD','RESET_ON_SYNC','RESET_ON_NEXT','SIGHTING_ACCURACY','TRACK_ACCURACY','GPS_TIME_ZONE','UTM_ZONE','DILUTION_OF_PRECISION','FIELD_MAP_FILENAME','LOCK100','DATA_FORMAT')", //$NON-NLS-1$
						"drop table smart.cm_ct_properties_profile", //$NON-NLS-1$
						"DROP TABLE smart.connect_alert", //$NON-NLS-1$
						"DROP TABLE smart.connect_ct_properties", //$NON-NLS-1$
						

						//missing constraint 
						"alter table smart.i_entity_type add constraint ca_entity_type_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
						"alter table smart.i_relationship_type add constraint ca_relationship_type_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
						"alter table smart.i_relationship_group add constraint ca_relationship_group_type_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
						"alter table smart.i_recordsource add constraint ca_recordsource_type_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
						"alter table smart.i_attribute add constraint ca_attribute_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
						"alter table smart.i_profile_config add constraint profile_config_ca_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
						
						//remove entity plugins
						"drop table smart.entity_gridded_query",  //$NON-NLS-1$
						"drop table smart.entity_observation_query",  //$NON-NLS-1$
						"drop table smart.entity_summary_query",  //$NON-NLS-1$
						"drop table smart.entity_waypoint_query",  //$NON-NLS-1$
						"drop table smart.entity_attribute_value",  //$NON-NLS-1$
						"drop table smart.entity",  //$NON-NLS-1$
						"drop table smart.entity_attribute",  //$NON-NLS-1$
						"drop table smart.entity_type",  //$NON-NLS-1$
						
						"DROP FUNCTION connect.trg_entity()", //$NON-NLS-1$
						"DROP FUNCTION connect.trg_entity_attribute()", //$NON-NLS-1$
						"DROP FUNCTION connect.trg_entity_attribute_value()", //$NON-NLS-1$
						
						"delete from  connect.connect_plugin_version where plugin_id = 'org.wcs.smart.entity'", //$NON-NLS-1$
						"delete from  connect.connect_plugin_version where plugin_id = 'org.wcs.smart.entity.query'", //$NON-NLS-1$
						"delete from  connect.ca_plugin_version where plugin_id = 'org.wcs.smart.entity'", //$NON-NLS-1$
						"delete from  connect.ca_plugin_version where plugin_id = 'org.wcs.smart.entity.query'", //$NON-NLS-1$
						
						
						//fix function st_length_spheriod is now st_lengthspheriod
						"CREATE or REPLACE FUNCTION smart.distanceinmeter(geom bytea) RETURNS double precision LANGUAGE plpgsql AS $$ BEGIN RETURN ST_LengthSpheroid(st_force2d(st_geomfromwkb(geom)), 'SPHEROID[\"WGS 84\",6378137,298.257223563]'); END; $$", //$NON-NLS-1$
						
						
						//attachment tags
						"CREATE TABLE smart.attachment_tag (uuid uuid not null, ca_uuid uuid not null, keyid varchar(128) not null, primary key (uuid), unique(ca_uuid, keyid))",  //$NON-NLS-1$
						"ALTER TABLE smart.attachment_tag  ADD CONSTRAINT attachment_tag_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						
						"CREATE TABLE SMART.attachment_tag_link (uuid uuid not null, obs_attachment_uuid uuid, wp_attachment_uuid uuid, tag_uuid uuid not null, primary key(uuid))", //$NON-NLS-1$
						
						"ALTER TABLE smart.attachment_tag_link  ADD CONSTRAINT attachment_tag_link_tag_uuid_fk FOREIGN KEY (tag_uuid) REFERENCES smart.attachment_tag(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.attachment_tag_link  ADD CONSTRAINT attachment_tag_link_obs_attachment_uuid_fk FOREIGN KEY (obs_attachment_uuid) REFERENCES smart.observation_attachment(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
						"ALTER TABLE smart.attachment_tag_link  ADD CONSTRAINT attachment_tag_link_wp_attachment_uuid_fk FOREIGN KEY (wp_attachment_uuid) REFERENCES smart.wp_attachments(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

						"ALTER TABLE smart.cm_node add column attachment_tags varchar", //$NON-NLS-1$

						"CREATE TRIGGER trg_attachment_tag AFTER INSERT OR UPDATE OR DELETE ON smart.attachment_tag FOR EACH ROW execute procedure connect.trg_changelog_common()", //$NON-NLS-1$
						
						"""								
						CREATE OR REPLACE FUNCTION connect.trg_attachment_tag_link() RETURNS trigger AS $$ 
								DECLARE
									ROW RECORD;
								BEGIN
									IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN 
										ROW = NEW; 
									ELSIF (TG_OP = 'DELETE') THEN 
										ROW = OLD; 
									END IF; 
									INSERT INTO connect.change_log 
										(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid)
										SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, t.CA_UUID
										FROM smart.attachment_tag t WHERE t.uuid = ROW.tag_uuid;
									RETURN ROW;
								END$$ LANGUAGE 'plpgsql'
						""", //$NON-NLS-1$
						
						"CREATE TRIGGER trg_attachment_tag_link AFTER INSERT OR UPDATE OR DELETE ON smart.attachment_tag_link FOR EACH ROW execute procedure connect.trg_attachment_tag_link()", //$NON-NLS-1$

						//survey created date
						"ALTER TABLE smart.survey ADD COLUMN date_created timestamp without time zone", //$NON-NLS-1$
						//2024-04-04 23:18:45
						"UPDATE smart.survey SET date_created = '" + utcs + "'", //$NON-NLS-1$ //$NON-NLS-2$
						"alter table smart.survey alter column date_created set not null", //$NON-NLS-1$
						
						//unique alerts
						//fix any non-unique user generated ids

						"""
						with newvalues as (
							select * from (
								select uuid, row_number() over (partition by user_generated_id) as rownum
								from connect.alerts
								where user_generated_id in (
									select user_generated_id from connect.alerts group by user_generated_id having count(*) > 1
									)
								)foo where rownum > 1
							)
							update connect.alerts set user_generated_id = user_generated_id || '_' || (rownum-1) 
							from newvalues a where a.uuid = alerts.uuid
						""", //$NON-NLS-1$
						//create index
						"alter table connect.alerts add constraint alerts_user_gen_id_unq unique(user_generated_id)", //$NON-NLS-1$
						
						//remove duplicate default mobile profiles
						//i tested on backup with this issue
						"""
						update smart.ct_properties_profile set is_default = false
						where uuid not in (
							select min(uuid::varchar)::uuid
							from smart.ct_properties_profile
							where is_default group by ca_uuid
							) and is_default = true
						""", //$NON-NLS-1$
						
						//versions
						"update connect.connect_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$
						
						"update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.connect.cybertracker'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.connect.cybertracker'", //$NON-NLS-1$
						
						"update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query'", //$NON-NLS-1$

						"update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa'", //$NON-NLS-1$

						"update connect.connect_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.er'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.er'", //$NON-NLS-1$

						"update connect.connect_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.i2'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.i2'", //$NON-NLS-1$
						
						"update connect.connect_plugin_version set version = '7.0' where plugin_id = 'org.wcs.smart.er'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '7.0' where plugin_id = 'org.wcs.smart.er'", //$NON-NLS-1$
						
						
						"update connect.connect_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$

						"insert into connect.connect_plugin_version (plugin_id, version) values ('org.wcs.smart.independentincident', '1.0')", //$NON-NLS-1$
						"insert into connect.ca_plugin_version (ca_uuid, plugin_id, version) select uuid, 'org.wcs.smart.independentincident', '1.0' from smart.conservation_area", //$NON-NLS-1$

						"update connect.connect_version set version = '8.0.0', last_updated = now()", //$NON-NLS-1$
						
						//---- change ca version so users cannot sync with this and cause problems ----
						//users must upgrade on connect OR desktop
						//the lastmodified/createddate requires this update
						"update connect.ca_info SET version = uuid_generate_v4()", //$NON-NLS-1$
						"delete from connect.change_log", //$NON-NLS-1$
						"delete from connect.change_log_history" //$NON-NLS-1$
						
					};
					for (String s : sql) {
						//System.out.println(s);
						c.createStatement().executeUpdate(s);
					}
					
					
					//update last modified/created by dates approximating the timezone based on 
					//the average of all waypoints in the database
					//if this won't work then users should upgrade CA in desktop where
					//they can select appropriate timezone
					String query = """ 
							with centroids as (
									SELECT ca_uuid, st_centroid(st_geomfromwkb( geometry))  as c 
									FROM smart.i_location il
							),
							points as(
									SELECT ca_uuid, st_x(c) as x, st_y(c) as y FROM centroids
									UNION ALL 
									SELECT ca_uuid, x, y FROM smart.waypoint w 
							)
							SELECT ca_uuid, avg(x), avg(y) FROM points GROUP BY ca_uuid;
					"""; //$NON-NLS-1$
					
					HashMap<UUID, Double> capoints = new HashMap<>();
					try(Statement s = c.createStatement();
							ResultSet rs = s.executeQuery(query)){
						while(rs.next()) {
							UUID cauuid = (UUID)rs.getObject(1);
							Double x = rs.getDouble(2);
							
							Double approxoffset = 0.0;
							if (x < -180 || x > 180 ) {
								//invalid longitude
							}else {
								approxoffset = Math.floor(Math.abs(x) / 15.0);
								if (x > 0) approxoffset = -approxoffset;	
							}
							capoints.put(cauuid, approxoffset);
						}						
					}
					
					query = "SELECT uuid FROM smart.conservation_area"; //$NON-NLS-1$
					try(Statement s = c.createStatement();
							ResultSet rs = s.executeQuery(query)){
						while(rs.next()) {
							UUID cauuid = (UUID)rs.getObject(1);
							if (cauuid.equals(ConservationArea.MULTIPLE_CA)) continue;
							
							Double offset = 0.0;
							if (capoints.containsKey(cauuid)) {
								offset = capoints.get(cauuid);
							}
							
							//update all the timezones += the offset hours
							String[] updates = new String[] {
								
								"update smart.waypoint set last_modified = last_modified + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								
								"update smart.i_entity set date_modified = date_modified + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_modified is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_entity set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

								"update smart.i_record set last_modified_date = last_modified_date + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified_date is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_record set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

								"update smart.i_working_set set last_modified_date = last_modified_date + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified_date is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_working_set set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

								"update smart.i_entity_record_query set last_modified_date = last_modified_date + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified_date is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_entity_record_query set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

								"update smart.i_entity_summary_query set last_modified_date = last_modified_date + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified_date is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_entity_summary_query set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

								"update smart.i_record_obs_query set last_modified_date = last_modified_date + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified_date is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_record_obs_query set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

								"update smart.i_record_query set last_modified_date = last_modified_date + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified_date is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_record_query set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

								"update smart.i_record_summary_query set last_modified_date = last_modified_date + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and last_modified_date is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"update smart.i_record_summary_query set date_created = date_created + interval '" + offset + " hour' where ca_uuid = '" + cauuid.toString() + "' and date_created is not null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

							};
							for (String update : updates) {
								try(Statement st = c.createStatement()){
									st.executeUpdate(update);
								}
							}
						}						
					}
							
					//for each coordinate find timezone
					
					
					//re-enable triggers
					c.createStatement().executeUpdate("SET session_replication_role = DEFAULT"); //$NON-NLS-1$
					
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});	
		
	}
	
	
	private void upgradeDb800to801(Session s, List<String> warnings) {
		
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					//disable triggers
					c.createStatement().executeUpdate("SET session_replication_role = replica"); //$NON-NLS-1$

					String[] sql = new String[] {
						"ALTER TABLE SMART.CM_NODE DROP COLUMN use_single_gps_point", //$NON-NLS-1$
						
						//versions
						"update connect.connect_plugin_version set version = '8.0.1' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '8.0.1' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$

						"update connect.connect_version set version = '8.0.1', last_updated = now()", //$NON-NLS-1$

					};
					
					for (String s : sql) {
						c.createStatement().executeUpdate(s);
					}
					
					//complicated fix to resolve the problem with the zero employee uuid I introduced in SMART8.0.0 upgrade script
					//only run this code if there is a employee with one uuid
					boolean hasOne = false;
					try(PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM smart.employee where uuid = ?")){ //$NON-NLS-1$
						ps.setObject(1, UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR));
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
							ps.setObject(1, UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR));
							ps.setObject(2, UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR));
							ps.execute();
						}
						
				
						try(ResultSet rs = c.getMetaData().getExportedKeys(null, "SMART", "EMPLOYEE")){ //$NON-NLS-1$ //$NON-NLS-2$
							while(rs.next()) {
								String schema = rs.getString(6);
								String table = rs.getString(7);
								String field = rs.getString(8);
								
								String query = "UPDATE " + schema + "." + table + " SET " + field + " = ? where " + field + " = ?";  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
								try(PreparedStatement ps = c.prepareStatement(query)){
									ps.setObject(1, UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR));
									ps.setObject(2, UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR));
									ps.execute();
								}			
							}
						}
						
						try(PreparedStatement ps = c.prepareStatement("DELETE FROM smart.employee WHERE uuid = ?")){ //$NON-NLS-1$
							ps.setObject(1, UuidUtils.stringToUuid(UuidUtils.ONE_UUID_STR));
							ps.execute();
						}
					}
					
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});	
		
	}
	
	private void upgradeDb801to810(Session s, List<String> warnings) {
		
		s.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				try {
					//disable triggers
					c.createStatement().executeUpdate("SET session_replication_role = replica"); //$NON-NLS-1$

					
					//ensure patrol_pilot_airplane, patrol_pilot_boat, and foot exist as icons for each ca
					try(Statement s = c.createStatement();
							ResultSet rs = s.executeQuery("select uuid from smart.conservation_area where uuid != '00000000-0000-0000-0000-000000000000'")){ //$NON-NLS-1$
						
						while(rs.next()) {
							UUID cauuid = (UUID) rs.getObject(1);
							for(String iconKey : new String[] {"patrol_pilot_airplane", "patrol_pilot_boat", "foot", "footprint_1"}) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									
								boolean hasicon = false;
								try(PreparedStatement s2 = 
										c.prepareStatement("SELECT uuid from smart.icon where ca_uuid = ? and keyid = ?")){ //$NON-NLS-1$
								
									s2.setObject(1, cauuid);
									s2.setObject(2, iconKey);
									
									try(ResultSet rs2 = s2.executeQuery()){
										hasicon = rs2.next();
									}
								}
								if (!hasicon) {
									UUID black = null;
									UUID color = null;
									UUID line = null;
									try(PreparedStatement ps3 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = ? and ca_uuid = ?")){ //$NON-NLS-1$
										ps3.setString(1, FixedIconSet.BLACK.key);
										ps3.setObject(2, cauuid);
										try(ResultSet rs3 = ps3.executeQuery()){
											if (rs3.next()) {
												black = (UUID) rs3.getObject(1);
											}
										}
										
										ps3.setString(1, FixedIconSet.COLOR.key);
										ps3.setObject(2, cauuid);
										try(ResultSet rs3 = ps3.executeQuery()){
											if (rs3.next()) {
												color = (UUID) rs3.getObject(1);
											}
										}
										
										ps3.setString(1, FixedIconSet.LINE.key);
										ps3.setObject(2, cauuid);
										try(ResultSet rs3 = ps3.executeQuery()){
											if (rs3.next()) {
												line = (UUID) rs3.getObject(1);
											}
										}
									}
									
									for (String[] icondef : IconUtils.INSTANCE.SMART_ICON_MAPPING) {
										if (!icondef[0].equalsIgnoreCase(iconKey)) continue;
										
										UUID iconUuid = UUID.randomUUID();
											
										try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) values (?,?,?)")){ //$NON-NLS-1$
											ps3.setObject(1, iconUuid);
											ps3.setString(2, iconKey);
											ps3.setObject(3, cauuid);
											
											ps3.execute();
										}
										
										try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) select uuid, ?, ? from smart.language where isdefault and ca_uuid = ?")){ //$NON-NLS-1$
											ps3.setObject(1, iconUuid);
											ps3.setString(2, icondef[1]);
											ps3.setObject(3, cauuid);
											ps3.execute();
										}
										
										if (black != null) {
											try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) values (?,?,?, ?)")){ //$NON-NLS-1$
												ps3.setObject(1, UUID.randomUUID());
												ps3.setObject(2, iconUuid);
												ps3.setObject(3, black);
												ps3.setString(4, icondef[2]);
												ps3.execute();
											}
										}
										if (line != null) {
											try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) values (?,?,?, ?)")){ //$NON-NLS-1$
												ps3.setObject(1, UUID.randomUUID());
												ps3.setObject(2, iconUuid);
												ps3.setObject(3, line);
												ps3.setString(4, icondef[3]);
												ps3.execute();
											}
										}
										if (color != null) {
											try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) values (?,?,?, ?)")){ //$NON-NLS-1$
												ps3.setObject(1, UUID.randomUUID());
												ps3.setObject(2, iconUuid);
												ps3.setObject(3, color);
												ps3.setString(4, icondef[4]);
												ps3.execute();
											}
										}	
									}
								}
							}
						}
					}
					
					String[] sql = new String[] {

							// support for patrol tree attribute
							"""
							CREATE TABLE smart.patrol_attribute_tree(
								uuid uuid,
								patrol_attribute_uuid uuid, 
								keyid varchar(128), 
								node_order smallint, 
								parent_uuid uuid, 
								is_active boolean, 
								hkey varchar(32672), 
								icon_uuid uuid, primary key (uuid))""",  //$NON-NLS-1$
							
							"""
							ALTER TABLE smart.patrol_attribute_tree 
							ADD CONSTRAINT patrol_att_tree_patrol_att_uuid_fk 
							FOREIGN KEY(patrol_attribute_uuid) 
							REFERENCES SMART.PATROL_ATTRIBUTE (UUID) 
							ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE
							""", //$NON-NLS-1$
							
							"""
							ALTER TABLE smart.patrol_attribute_tree 
							ADD CONSTRAINT patrol_att_tree_parent_uuid_fk 
							FOREIGN KEY(parent_uuid) REFERENCES smart.patrol_attribute_tree (UUID) 
							ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE""", //$NON-NLS-1$
							
							"""
							ALTER TABLE smart.patrol_attribute_tree 
							ADD CONSTRAINT patrol_att_tree_icon_uuid_fk 
							FOREIGN KEY(icon_uuid) 
							REFERENCES smart.icon (UUID) 
							ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE""", //$NON-NLS-1$
							
							"ALTER TABLE smart.patrol_attribute_value ADD COLUMN tree_node_uuid uuid", //$NON-NLS-1$
							
							"""
							ALTER TABLE smart.patrol_attribute_value 
							ADD CONSTRAINT patrol_att_value_tree_node_uuid_fk 
							FOREIGN KEY(tree_node_uuid) REFERENCES smart.patrol_attribute_tree (UUID) 
							ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE""", //$NON-NLS-1$
											
							//trigger
							"""								
							CREATE OR REPLACE FUNCTION connect.trg_patrol_attribute_tree() RETURNS trigger AS $$ 
									DECLARE
										ROW RECORD;
									BEGIN
										IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN 
											ROW = NEW; 
										ELSIF (TG_OP = 'DELETE') THEN 
											ROW = OLD; 
										END IF; 
										INSERT INTO connect.change_log 
											(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid)
											SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, t.CA_UUID
											FROM smart.patrol_attribute t WHERE t.uuid = ROW.patrol_attribute_uuid;
										RETURN ROW;
									END$$ LANGUAGE 'plpgsql'
							""", //$NON-NLS-1$
							
							"CREATE TRIGGER trg_patrol_attribute_tree AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute_tree FOR EACH ROW execute procedure connect.trg_patrol_attribute_tree()", //$NON-NLS-1$

							//device table
							"""
							CREATE TABLE smart.ct_device(
							  uuid uuid not null, 
							  device_id varchar(128), 
							  ca_uuid uuid not null, 
							  icon_uuid uuid, 
							  name varchar(1024), 
							  primary key (uuid, ca_uuid)) 
							""",  //$NON-NLS-1$
							
							"ALTER TABLE smart.ct_device ADD CONSTRAINT ct_device_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
							"ALTER TABLE smart.ct_device ADD CONSTRAINT ct_device_icon_uuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(UUID) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
							"ALTER TABLE smart.ct_incident_link add column ct_device_id varchar(36)", //$NON-NLS-1$
							
							"CREATE TRIGGER trg_ct_Device AFTER INSERT OR UPDATE OR DELETE ON smart.ct_device FOR EACH ROW execute procedure connect.trg_changelog_common()", //$NON-NLS-1$

							//patrol types -> track types
							"UPDATE smart.patrol_type SET patrol_type = lower(patrol_type)", //$NON-NLS-1$
							"alter table smart.patrol_type RENAME COLUMN patrol_type to keyId", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type alter column keyId set data type varchar(128)", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type add column uuid uuid", //$NON-NLS-1$
							"UPDATE smart.patrol_type set uuid = uuid_generate_v4()", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type alter column uuid set not null", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type drop constraint patrol_type_pkey", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type add primary key (uuid) ", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type add column requires_pilot boolean default false not null", //$NON-NLS-1$
							"UPDATE smart.patrol_type set requires_pilot = true where keyid in ('marine', 'air')", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport add column patrol_type_uuid uuid ", //$NON-NLS-1$
							"UPDATE smart.patrol_transport set patrol_type_uuid = a.uuid from smart.patrol_type a where a.keyid = lower(smart.patrol_transport.patrol_type) and a.ca_uuid = smart.patrol_transport.ca_uuid", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport drop column patrol_type", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport ADD CONSTRAINT pt_patrol_type_uuid_fk FOREIGN KEY(patrol_type_uuid) REFERENCES smart.patrol_type (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
							
							"ALTER TABLE smart.patrol add column patrol_type_uuid uuid", //$NON-NLS-1$
							"UPDATE smart.patrol set patrol_type_uuid = (select a.uuid from smart.patrol_type a where a.keyid = lower(smart.patrol.patrol_type) and a.ca_uuid = smart.patrol.ca_uuid)", //$NON-NLS-1$
							"ALTER TABLE smart.patrol drop column patrol_type", //$NON-NLS-1$
							"ALTER TABLE smart.patrol ADD CONSTRAINT patrol_patrol_type_uuid_fk FOREIGN KEY(patrol_type_uuid) REFERENCES smart.patrol_type (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
							
							"ALTER TABLE smart.patrol_type add column icon_uuid uuid ", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type add constraint patrol_type_icon_uuid_fk foreign key (icon_uuid) references smart.icon(uuid) on update restrict on delete set null deferrable initially immediate", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type add constraint patrol_type_unq unique(ca_uuid, keyid)", //$NON-NLS-1$

							"ALTER TABLE smart.patrol_transport add column requires_pilot boolean not null default false", //$NON-NLS-1$
							"UPDATE smart.patrol_transport set requires_pilot = (select requires_pilot from smart.patrol_type where smart.patrol_type.uuid = smart.patrol_transport.patrol_type_uuid)", //$NON-NLS-1$
							"alter table smart.patrol_type drop column requires_pilot", //$NON-NLS-1$
							
							"CREATE TABLE smart.patrol_transport_group(uuid uuid not null, patrol_type_uuid uuid not null, icon_uuid uuid, keyid varchar(128) not null, primary key (uuid))", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport_group add constraint ptg_icon_uuid_fk foreign key (icon_uuid) references smart.icon(uuid) on update restrict on delete set null deferrable initially immediate", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport add column patrol_transport_group_uuid uuid", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport add constraint pt_patrol_transport_group_uuid_fk foreign key (patrol_transport_group_uuid) references smart.patrol_transport_group(uuid) on update restrict on delete set null deferrable initially immediate", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport_group add constraint ptg_patrol_type_uuid_fk foreign key (patrol_type_uuid) references smart.patrol_type(uuid) on update restrict on delete cascade deferrable initially immediate", //$NON-NLS-1$
							"UPDATE smart.patrol_transport set patrol_transport_group_uuid = patrol_type_uuid", //$NON-NLS-1$
											
							//move max speed to transport type
							"ALTER TABLE smart.patrol_transport ADD COLUMN max_speed integer",  //$NON-NLS-1$
							"UPDATE smart.patrol_transport set max_speed = a.max_speed FROM smart.patrol_type a WHERE a.uuid = smart.patrol_transport.patrol_type_uuid", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_transport alter column max_speed set not null", //$NON-NLS-1$
							"ALTER TABLE smart.patrol_type drop column max_speed", //$NON-NLS-1$
							
							//update to a single patrol type and existing patrol types become patrols groups
							"insert into smart.patrol_type(uuid, ca_uuid, keyid, is_active) select uuid_generate_v4(), a.uuid, 'patrol', true FROM smart.conservation_area a where a.uuid != '00000000-0000-0000-0000-000000000000' ", //$NON-NLS-1$
							"update smart.patrol_transport set patrol_type_uuid = a.uuid from smart.patrol_type a where a.keyid = 'patrol' and a.ca_uuid = smart.patrol_transport.ca_uuid", //$NON-NLS-1$
							
							"update smart.patrol set patrol_type_uuid = a.uuid from smart.patrol_type a where a.keyid = 'patrol' and a.ca_uuid = smart.patrol.ca_uuid", //$NON-NLS-1$
							
							"insert into smart.patrol_transport_group(uuid, patrol_type_uuid, icon_uuid, keyid) select a.uuid, b.uuid, a.icon_uuid, a.keyid from smart.patrol_type a, smart.patrol_type b where a.ca_uuid = b.ca_uuid and b.keyid = 'patrol' and a.keyid not in ('patrol', 'mixed')", //$NON-NLS-1$
							"delete from smart.patrol_type where keyid != 'patrol'", //$NON-NLS-1$
							
							"update smart.patrol_transport_group set icon_uuid = a.uuid from smart.icon a, smart.patrol_type b where a.ca_uuid = b.ca_uuid and b.uuid = smart.patrol_transport_group.patrol_type_uuid and a.keyid = 'foot' and smart.patrol_transport_group.keyid = 'ground'", //$NON-NLS-1$
							"update smart.patrol_transport_group set icon_uuid = a.uuid from smart.icon a, smart.patrol_type b where a.ca_uuid = b.ca_uuid and b.uuid = smart.patrol_transport_group.patrol_type_uuid and a.keyid = 'patrol_pilot_boat' and smart.patrol_transport_group.keyid = 'marine'", //$NON-NLS-1$
							"update smart.patrol_transport_group set icon_uuid = a.uuid from smart.icon a, smart.patrol_type b where a.ca_uuid = b.ca_uuid and b.uuid = smart.patrol_transport_group.patrol_type_uuid and a.keyid = 'patrol_pilot_airplane' and smart.patrol_transport_group.keyid = 'air'", //$NON-NLS-1$
							
							"update smart.patrol_type set icon_uuid = a.uuid from smart.icon a where a.ca_uuid = smart.patrol_type.ca_uuid and a.keyid = 'footprint_1'", //$NON-NLS-1$
							"insert into smart.i18n_label (language_uuid, element_uuid, value) SELECT a.uuid, b.uuid, 'Patrol' FROM smart.language a, smart.patrol_type b where a.ca_uuid = b.ca_uuid", //$NON-NLS-1$

							//link custom attribute to patrol type
							"create table smart.patrol_attribute_patrol_type(patrol_attribute_uuid uuid not null, patrol_type_uuid uuid not null, is_active boolean not null default true, primary key (patrol_attribute_uuid, patrol_type_uuid))", //$NON-NLS-1$
							
							"alter table smart.patrol_attribute_patrol_type add constraint papt_patrol_attribute_uuid_fk foreign key (patrol_attribute_uuid) references smart.patrol_attribute (uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
							"alter table smart.patrol_attribute_patrol_type add constraint papt_patrol_type_uuid_fk foreign key (patrol_type_uuid ) references smart.patrol_type (uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
							//by default link all
							"""
							insert into smart.patrol_attribute_patrol_type(patrol_attribute_uuid, patrol_type_uuid) 
							select a.uuid, b.uuid from smart.patrol_attribute a, smart.patrol_type b 
							where a.ca_uuid = b.ca_uuid and b.keyid != 'mixed'
							""",  //$NON-NLS-1$
							
							"DROP TRIGGER trg_patrol_type ON smart.patrol_type ", //$NON-NLS-1$
							//"DROP FUNCTION connect.trg_patrol_type()", //$NON-NLS-1$
							"CREATE TRIGGER trg_patrol_type AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_type FOR EACH ROW execute procedure connect.trg_changelog_common()", //$NON-NLS-1$

							//trigger for patrol_attribute_patrol_type
							"""								
							CREATE OR REPLACE FUNCTION connect.trg_patrol_attribute_patrol_type() RETURNS trigger AS $$ 
									DECLARE
										ROW RECORD;
									BEGIN
										IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN 
											ROW = NEW; 
										ELSIF (TG_OP = 'DELETE') THEN 
											ROW = OLD; 
										END IF; 
										INSERT INTO connect.change_log 
											(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid)
											SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'patrol_attribute_uuid', ROW.patrol_attribute_uuid, 'patrol_type_uuid', ROW.patrol_type_uuid, null, t.CA_UUID
											FROM smart.patrol_type t WHERE t.uuid = ROW.patrol_type_uuid;
										RETURN ROW;
									END$$ LANGUAGE 'plpgsql'
							""", //$NON-NLS-1$
							
							"CREATE TRIGGER trg_patrol_attribute_patrol_type AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_attribute_patrol_type FOR EACH ROW execute procedure connect.trg_patrol_attribute_patrol_type()", //$NON-NLS-1$

							
							//trigger for patrol_transport_group
							"""								
							CREATE OR REPLACE FUNCTION connect.trg_patrol_transport_group() RETURNS trigger AS $$ 
									DECLARE
										ROW RECORD;
									BEGIN
										IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN 
											ROW = NEW; 
										ELSIF (TG_OP = 'DELETE') THEN 
											ROW = OLD; 
										END IF; 
										INSERT INTO connect.change_log 
											(uuid, action, tablename, key1_fieldname, key1, key2_fieldname, key2_uuid, key2_str, ca_uuid)
											SELECT uuid_generate_v4(), TG_OP, TG_TABLE_SCHEMA::TEXT || '.' || TG_TABLE_NAME::TEXT, 'uuid', ROW.uuid, null, null, null, t.CA_UUID
											FROM smart.patrol_type t WHERE t.uuid = ROW.patrol_type_uuid;
										RETURN ROW;
									END$$ LANGUAGE 'plpgsql'
							""", //$NON-NLS-1$
							
							"CREATE TRIGGER trg_patrol_transport_group AFTER INSERT OR UPDATE OR DELETE ON smart.patrol_transport_group FOR EACH ROW execute procedure connect.trg_patrol_transport_group()", //$NON-NLS-1$

							
							//datamodel attribute ordering
							"ALTER TABLE smart.dm_cat_att_map ADD COLUMN is_root boolean", //$NON-NLS-1$
							"UPDATE smart.dm_cat_att_map set is_root = true", //$NON-NLS-1$
							
							//ct patrol
							"ALTER TABLE smart.ct_patrol_package add column patrol_type_uuid uuid ", //$NON-NLS-1$
							"ALTER TABLE SMART.ct_patrol_package ADD CONSTRAINT ct_patrol_package_patrol_type_uuid_fk FOREIGN KEY (patrol_type_uuid) REFERENCES smart.patrol_type(UUID) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
							"UPDATE smart.ct_patrol_package set patrol_type_uuid = t.uuid from smart.patrol_type t where t.keyid = 'patrol' and t.ca_uuid =  smart.ct_patrol_package.ca_uuid", //$NON-NLS-1$
							
							"create table smart.pptemp (ca_uuid uuid, uuid uuid)", //$NON-NLS-1$
							"insert into smart.pptemp (ca_uuid, uuid)  select a.ca_uuid, a.uuid  from (select  ca_uuid, min(keyid) as keyid  from smart.patrol_type  group by ca_uuid) b join smart.patrol_type a on a.ca_uuid = b.ca_uuid and a.keyid = b.keyid", //$NON-NLS-1$
							"UPDATE smart.ct_patrol_package set patrol_type_uuid = t.uuid from smart.pptemp t where smart.ct_patrol_package.ca_uuid = t.ca_uuid and smart.ct_patrol_package.patrol_type_uuid is null", //$NON-NLS-1$
							"drop table smart.pptemp", //$NON-NLS-1$
							
							"alter table smart.ct_patrol_package alter column patrol_type_uuid set not null", //$NON-NLS-1$
							
							
							//add names to packages
							"insert into smart.i18n_label(language_uuid, element_uuid, value) select  a.uuid,b.uuid, b.name from smart.language a, smart.ct_incident_package b where a.ca_uuid = b.ca_uuid and a.isdefault", //$NON-NLS-1$
							"ALTER TABLE smart.ct_incident_package drop column name", //$NON-NLS-1$		
							
							"insert into smart.i18n_label(language_uuid, element_uuid, value) select  a.uuid,b.uuid, b.name from smart.language a, smart.ct_patrol_package b where a.ca_uuid = b.ca_uuid and a.isdefault", //$NON-NLS-1$
							"ALTER TABLE smart.ct_patrol_package drop column name", //$NON-NLS-1$
							
							"insert into smart.i18n_label(language_uuid, element_uuid, value) select  a.uuid,b.uuid, b.name from smart.language a, smart.ct_survey_package b where a.ca_uuid = b.ca_uuid and a.isdefault", //$NON-NLS-1$
							"ALTER TABLE smart.ct_survey_package drop column name", //$NON-NLS-1$
							
							"insert into smart.i18n_label(language_uuid, element_uuid, value) select  a.uuid,b.uuid, b.name from smart.language a, smart.smartcollect_package b where a.ca_uuid = b.ca_uuid and a.isdefault", //$NON-NLS-1$
							"ALTER TABLE smart.smartcollect_package drop column name", //$NON-NLS-1$
							
							//incident types
							"alter table smart.waypoint add column incident_type_uuid uuid", //$NON-NLS-1$
							
							"create table smart.incident_type(uuid uuid not null, ca_uuid uuid not null, keyid varchar(128) not null, is_active boolean not null default true, icon_uuid uuid, options varchar(32672), fallback_type_uuid uuid, primary key (uuid) )", //$NON-NLS-1$
							"ALTER TABLE smart.incident_type add constraint incident_type_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
							"ALTER TABLE smart.incident_type add constraint incident_unq_chk UNIQUE (ca_uuid, keyid)", //$NON-NLS-1$
							"ALTER TABLE smart.incident_type add constraint incident_type_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) on delete set null on update restrict deferrable initially immediate", //$NON-NLS-1$
							
							"insert into smart.incident_type(uuid, ca_uuid, keyid) select uuid_generate_v4(), uuid, 'incident' from smart.conservation_area where uuid != '00000000-0000-0000-0000-000000000000'", //$NON-NLS-1$
							"insert into smart.incident_type(uuid, ca_uuid, keyid) select uuid_generate_v4(), uuid, 'integrate' from smart.conservation_area where uuid != '00000000-0000-0000-0000-000000000000'", //$NON-NLS-1$
							"insert into smart.incident_type(uuid, ca_uuid, keyid, options) select uuid_generate_v4(), uuid, 'integratelink', 'linkpatrol' from smart.conservation_area where uuid != '00000000-0000-0000-0000-000000000000'", //$NON-NLS-1$
							"insert into smart.incident_type(uuid, ca_uuid, keyid, options) select uuid_generate_v4(), uuid, 'integratemove', 'movepatrol' from smart.conservation_area where uuid != '00000000-0000-0000-0000-000000000000'", //$NON-NLS-1$

							"update smart.incident_type set fallback_type_uuid = a.uuid from smart.incident_type a where a.ca_uuid = smart.incident_type.ca_uuid and a.keyid='integrate' and smart.incident_type.keyid in ('integratelink', 'integratemove')", //$NON-NLS-1$
							
							"update smart.waypoint set incident_type_uuid = a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'incident' and source = 'INDINC'", //$NON-NLS-1$
							"update smart.waypoint set incident_type_uuid = a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'integrate' and source = 'INTEGRATE'", //$NON-NLS-1$
							"update smart.waypoint set incident_type_uuid = a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'integratelink' and source = 'INTEGRATEPLLINK'", //$NON-NLS-1$
							"update smart.waypoint set incident_type_uuid = a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'integratemove' and source = 'INTEGRATEPATROL'", //$NON-NLS-1$
							
							"update smart.waypoint set source = 'INDINC' where source in ('INTEGRATE', 'INTEGRATEPLLINK', 'INTEGRATEPATROL')", //$NON-NLS-1$

							"CREATE TRIGGER trg_incident_type AFTER INSERT OR UPDATE OR DELETE ON smart.incident_type FOR EACH ROW execute procedure connect.trg_changelog_common()", //$NON-NLS-1$
							
							//versions
							"update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.smartcollect'", //$NON-NLS-1$
							"update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.smartcollect'", //$NON-NLS-1$
							"update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.cybertracker.survey'", //$NON-NLS-1$
							"update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.cybertracker.survey'", //$NON-NLS-1$
							"update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.cybertracker.incident'", //$NON-NLS-1$
							"update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.cybertracker.incident'", //$NON-NLS-1$
							"update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol'", //$NON-NLS-1$
							"update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.cybertracker.patrol'", //$NON-NLS-1$
							"update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.independentincident'", //$NON-NLS-1$
							"update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.independentincident'", //$NON-NLS-1$
														
							"update connect.connect_plugin_version set version = '8.1.0' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$
							"update connect.ca_plugin_version set version = '8.1.0' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$
							"update connect.connect_plugin_version set version = '8.1' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$
							"update connect.ca_plugin_version set version = '8.1' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$

							"update connect.connect_version set version = '8.1.0', last_updated = now()", //$NON-NLS-1$

					};
					
					for (String s : sql) {
						//System.out.println(s);
						c.createStatement().executeUpdate(s);
					}
					
					
					//insert i18n names for patrol types
					HashMap<String, String> ptTranslations = new HashMap<>();
					ptTranslations.put("air_en", "Air"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_en", "Ground"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_en", "Mixed"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_en", "Water"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_ar", "\u0647\u0648\u0627\u0621"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_ar", "\u0623\u0631\u0636\u064a"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_ar", "\u0645\u062e\u062a\u0644\u0637"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_ar", "\u0645\u0627\u0621"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_es", "Aereo"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_es", "Terrestre"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_es", "Mixto"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_es", "Acuatico"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_fr", "A\u00e9rien"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_fr", "Terrestre"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_fr", "Mixte"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_fr", "Eau"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_in", "Udara"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_in", "Darat"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_in", "Gabungan"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_in", "Air"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_ka", "\u10e1\u10d0\u10f0\u10d0\u10d4\u10e0\u10dd"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_ka", "\u10e1\u10d0\u10ee\u10db\u10d4\u10da\u10d4\u10d7\u10dd"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_ka", "\u10d0\u10e0\u10d4\u10e3\u10da\u10d8"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_ka", "\u10ec\u10e7\u10da\u10d8\u10e1"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_km", "\u1781\u17d2\u1799\u179b\u17cb"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_km", "\u178a\u17b8"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_km", "\u179b\u17b6\u1799"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_km", "\u1791\u17b9\u1780"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_lo", "\u0e97\u0eb2\u0e87\u200b\u0ead\u0eb2\u200b\u0e81\u0eb2\u0e94"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_lo", "\u0e97\u0eb2\u0e87\u0e9e\u0eb7\u0ec9\u0e99\u0e94\u0eb4\u0e99"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_mn", "\u0410\u0433\u0430\u0430\u0440"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_mn", "\u0413\u0430\u0437\u0430\u0440"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_mn", "\u0425\u043e\u0441\u043b\u043e\u0441\u043e\u043d"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_mn", "\u0423\u0441"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_ms", "Udara"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_ms", "Ground"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_pt", "A\u00e9reo"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_pt", "Terrestre"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_pt", "Misto"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_pt", "Aqu\u00e1tico"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_ru", "\u0412\u043e\u0437\u0434\u0443\u0448\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_ru", "\u041d\u0430\u0437\u0435\u043c\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_ru", "\u0421\u043c\u0435\u0448\u0430\u043d\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_ru", "\u0412\u043e\u0434\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_sw", "Hewa"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_sw", "Uwanja"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_sw", "Mchanganyiko");  //$NON-NLS-1$//$NON-NLS-2$
					ptTranslations.put("marine_sw", "Maji"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_th", "\u0e17\u0e32\u0e07\u0e2d\u0e32\u0e01\u0e32\u0e28"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_th", "\u0e17\u0e32\u0e07\u0e1e\u0e37\u0e49\u0e19\u0e14\u0e34\u0e19"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_th", "\u0e1c\u0e2a\u0e21\u0e1c\u0e2a\u0e32\u0e19"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_th", "\u0e17\u0e32\u0e07\u0e19\u0e49\u0e33"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_uk", "\u041f\u043e\u0432\u0456\u0442\u0440\u044f"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_uk", "\u0417\u0435\u043c\u043b\u044f"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_uk", "\u0417\u043c\u0456\u0449\u0430\u043d\u0438\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_uk", "\u0412\u043e\u0434\u0430"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_vi", "\u0110\u01b0\u1eddng kh\u00f4ng"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_vi", "\u0110\u01b0\u1eddng b\u1ed9"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_vi", "H\u1ed7n h\u1ee3p"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_vi", "\u0110\u01b0\u1eddng th\u1ee7y"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("air_zh", "\u822a\u7a7a"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("ground_zh", "\u5730\u9762"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("mixed_zh", "\u6df7\u5408"); //$NON-NLS-1$ //$NON-NLS-2$
					ptTranslations.put("marine_zh", "\u6c34\u4e0a"); //$NON-NLS-1$ //$NON-NLS-2$
					
					String query = "select ptg.uuid, ptg.keyid, l.code, l.uuid, l.isdefault from smart.patrol_transport_group ptg join smart.patrol_type pt on ptg.patrol_type_uuid = pt.uuid  join smart.language l on pt.ca_uuid = l.ca_uuid"; //$NON-NLS-1$
					String insertQuery2 = "insert into smart.i18n_label (language_uuid, element_uuid, value) values (?, ?, ?)"; //$NON-NLS-1$
					try(Statement s = c.createStatement(); PreparedStatement psinsert = c.prepareStatement(insertQuery2);
							ResultSet rs = s.executeQuery(query)){ 
						while(rs.next()) {
							UUID elementuuid = (UUID) rs.getObject(1);
							UUID languageuuid = (UUID) rs.getObject(4);
							String code = rs.getString(3);
							String key = rs.getString(2);
							boolean isDefault = rs.getBoolean(5);
							
							String lookup = key + "_" + I18nUtil.stringToLocale(code).getCountry(); //$NON-NLS-1$
							if (!ptTranslations.containsKey(lookup) && isDefault) {
								lookup = key + "_en"; //$NON-NLS-1$
							}
							String value = ptTranslations.get(lookup);
							if (value == null) value = key;
							
							String y = new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
							
							psinsert.setObject(1, languageuuid);
							psinsert.setObject(2, elementuuid);
							psinsert.setString(3, y);
							psinsert.addBatch();
							
						}
						psinsert.executeBatch();
					}
					
					
					//populate dm_cat_att_map table
					//with child category/attribute objects
					//so we can order them correctly 
					//https://app.assembla.com/spaces/smart-cs/tickets/3297		
					String attributeQuery = "select attribute_uuid, is_active from smart.dm_cat_att_map where category_uuid = ? order by att_order"; //$NON-NLS-1$
					String cateogryQuery = "select uuid, is_active from smart.dm_category where parent_category_uuid = ?"; //$NON-NLS-1$
					String insertQuery = "insert into smart.dm_cat_att_map (category_uuid, attribute_uuid, is_root, is_active, att_order) values (?,?,?,?,?)"; //$NON-NLS-1$
					String updateQuery = "update smart.dm_cat_att_map set att_order = ? where attribute_uuid = ? and category_uuid = ?"; //$NON-NLS-1$
					
					List<Category> toProcess = new ArrayList<>();
					try(Statement s = c.createStatement();
							ResultSet rs = s.executeQuery("SELECT uuid,  is_active FROM smart.dm_category WHERE parent_category_uuid is null")){ //$NON-NLS-1$
						while(rs.next()) {
							Category temp = new Category();
							temp.setUuid((UUID) rs.getObject(1));
							temp.setIsActive(rs.getBoolean(2));
							toProcess.add(temp);
						}
					}
					
					try(PreparedStatement attributeSelect = c.prepareStatement(attributeQuery);
							PreparedStatement categorySelect = c.prepareStatement(cateogryQuery);
							PreparedStatement insertStatement = c.prepareStatement(insertQuery);
							PreparedStatement updateStatement = c.prepareStatement(updateQuery);){
									
						for(Category category : toProcess) {
							processCategory801to810(category, new ArrayList<>(), c, attributeSelect, categorySelect, insertStatement, updateStatement);
						}
					}
					
					
					// ----upgrade patrol queries for transport type changes

					Map<String,String> oldnew = new HashMap<>();
					oldnew.put("patrol:patroltype equals \"GROUND\"", "patrol:transgroupkey equals \"ground\"");  //$NON-NLS-1$//$NON-NLS-2$
					oldnew.put("patrol:patroltype equals \"AIR\"", "patrol:transgroupkey equals \"air\"");  //$NON-NLS-1$//$NON-NLS-2$
					oldnew.put("patrol:patroltype equals \"MARINE\"", "patrol:transgroupkey equals \"marine\"");  //$NON-NLS-1$//$NON-NLS-2$
					
					for (String table : new String[] {"smart.observation_query", //$NON-NLS-1$
							"smart.waypoint_query",  //$NON-NLS-1$
							"smart.patrol_query"}) { //$NON-NLS-1$
						
						try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid, query_filter FROM " + table + " WHERE query_filter is not null "); //$NON-NLS-1$ //$NON-NLS-2$
								PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET query_filter = ? WHERE uuid = ?")){  //$NON-NLS-1$//$NON-NLS-2$
							
							
							while(rs.next()) {
								
								UUID uuid = (UUID) rs.getObject(1);
								String query2 = rs.getString(2);
								if (query2 == null) continue;
								
								boolean updated = false;
								for (Entry<String,String> replace : oldnew.entrySet()) {
									if (query2.contains(replace.getKey())) {
										updated = true;
										query2 = query2.replaceAll(replace.getKey(), replace.getValue());
									}
								}
								if (updated) {
									ps.setString(1, query2);
									ps.setObject(2, uuid);
									ps.execute();
								}
							}
						}
						
						//add column filter if columns are filtered and transporttype is included in the filter
						try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid, column_filter FROM " + table + " WHERE column_filter is not null "); //$NON-NLS-1$ //$NON-NLS-2$
								PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET column_filter = ? WHERE uuid = ?")){  //$NON-NLS-1$//$NON-NLS-2$
						
							while(rs.next()) {
								
								UUID uuid = (UUID) rs.getObject(1);
								String filter = rs.getString(2);

								if (filter.contains("patrol:transporttype")) { //$NON-NLS-1$
								
									filter += ",patrol:transportgroup"; //$NON-NLS-1$
								
									ps.setString(1, filter);
									ps.setObject(2, uuid);
									ps.execute();
								}
							}
						}
					}
					
					for (String table : new String[] {"smart.summary_query", //$NON-NLS-1$
							"smart.gridded_query"}) { //$NON-NLS-1$
						
						//summary & grid query
						try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid, query_def FROM " + table); //$NON-NLS-1$
								PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET query_def = ? WHERE uuid = ?")){ //$NON-NLS-1$ //$NON-NLS-2$
							
							while(rs.next()) {
								
								UUID uuid = (UUID) rs.getObject(1);
								String query2 = rs.getString(2);
								
								boolean updated = false;
								for (Entry<String,String> replace : oldnew.entrySet()) {
									if (query2.contains(replace.getKey())) {
										updated = true;
										query2 = query2.replaceAll(replace.getKey(), replace.getValue());
									}
								}
								
								if (table.equals("smart.summary_query")) { //$NON-NLS-1$
									String old = "patrol:patroltype:"; //$NON-NLS-1$
									if (query2.contains(old)) {
										updated = true;
										query2 = query2.replaceAll(old, "patrol:patroltransgroupkey:"); //$NON-NLS-1$
									
										for (String key : new String[] {"\"GROUND\"", "\"AIR\"", "\"MARINE\"", "\"MIXED\""}) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
											if (query2.contains(key)) {
												updated=true;
												String newvalue = key.substring(1, key.length()-1).toLowerCase();
												if (newvalue.equals("mixed")) { //$NON-NLS-1$
													newvalue = "patrol.mixed"; //$NON-NLS-1$
												}
												query2 = query2.replaceAll(key, newvalue);
											}
										}
									}
								}
								
								if (updated) {
									ps.setString(1, query);
									ps.setObject(2, uuid);
									ps.execute();
								}
							}
						}
					}
					
					
					//translations for incident types
					//translations
					String[][] data = new String[][]{
							{"incident", "en", "Independent Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "ar", "\u062d\u0627\u062f\u062b \u0645\u0633\u062a\u0642\u0644"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "es", "Incidente Independiente"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "fr", "Incident Independant"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "in", "Insiden Independen"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "ka", "\u10d3\u10d0\u10db\u10dd\u10e3\u10d9\u10d8\u10d3\u10d4\u10d1\u10d4\u10da\u10d8 \u10d8\u10dc\u10ea\u10d8\u10d3\u10d4\u10dc\u10e2\u10d8"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "km", "\u17a7\u1794\u17d2\u1794\u178f\u17d2\u178f\u17b7\u17a0\u17c1\u178f\u17bb\u17af\u1780\u179a\u17b6\u1787\u17d2\u1799"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "mn", "\u0411\u0438\u0435 \u0434\u0430\u0430\u0441\u0430\u043d \u0445\u044d\u0440\u044d\u0433"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "pt", "Incidente Independente"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "ru", "\u041e\u0431\u043e\u0441\u043e\u0431\u043b\u0435\u043d\u043d\u044b\u0439 \u0438\u043d\u0446\u0438\u0434\u0435\u043d\u0442"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "sw", "Tukio huru"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "th", "\u0e40\u0e2b\u0e15\u0e38\u0e01\u0e32\u0e23\u0e13\u0e4c\u0e2d\u0e34\u0e2a\u0e23\u0e30"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "uk", "\u041d\u0435\u0437\u0430\u043b\u0435\u0436\u043d\u0438\u0439 \u0456\u043d\u0446\u0438\u0434\u0435\u043d\u0442"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "vi", "V\u1ee5 vi\u1ec7c \u0111\u1ed9c l\u1eadp"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"incident", "zh", "\u72ec\u7acb\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

							{"integrate", "en", "SMART Integrate Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "es", " Incidente de SMART Integrate"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "in", " Insiden Integrasi SMART"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "pt", " Incidente Integrado SMART"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "ru", " \u0418\u043d\u0446\u0438\u0434\u0435\u043d\u0442 SMART Integrate"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "th", " \u0e40\u0e2b\u0e15\u0e38\u0e01\u0e32\u0e23\u0e13\u0e4c\u0e23\u0e27\u0e21\u0e02\u0e2d\u0e07\u0e2a\u0e21\u0e32\u0e23\u0e4c\u0e17"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "uk", " \u0406\u043d\u0442\u0435\u0433\u0440\u043e\u0432\u0430\u043d\u0438\u0439 SMART \u0406\u043d\u0446\u0438\u0434\u0435\u043d\u0442"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "vi", " S\u1ef1 c\u1ed1 t\u00edch h\u1ee3p SMART"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integrate", "zh", "SMART\u96c6\u6210\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

							{"integratemove", "en", "SMART Integrate Move To Patrol Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratemove", "es", "SMART Integrar Mover al Incidente de Patrullaje."}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratemove", "in", "SMART Integrasikan Pindah ke Insiden Patroli"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratemove", "km", "Smart \u1794\u17b6\u1793\u1792\u17d2\u179c\u17be\u179f\u1798\u17b6\u17a0\u179a\u178e\u1780\u1798\u17d2\u1798\u1785\u179b\u1793\u17b6\u1791\u17c5\u17a7\u1794\u17d2\u1794\u178f\u17d2\u178f\u17b7\u17a0\u17c1\u178f\u17bb\u179b\u17d2\u1794\u17b6\u178f"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratemove", "ru", "\u041f\u0435\u0440\u0435\u043d\u043e\u0441 SMART Integrate \u0432 \u0438\u043d\u0446\u0438\u0434\u0435\u043d\u0442 \u0440\u0435\u0439\u0434\u0430\t"},  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							{"integratemove", "uk", "\u0406\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0456\u044f \u043f\u0435\u0440\u0435\u043c\u0456\u0449\u0435\u043d\u043d\u044f SMART \u0434\u043e \u043f\u0430\u0442\u0440\u0443\u043b\u044c\u043d\u043e\u0433\u043e \u0456\u043d\u0446\u0438\u0434\u0435\u043d\u0442\u0443"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratemove", "zh", "SMART\u96c6\u6210\u79fb\u52a8\u5230\u5de1\u62a4\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratelink", "en", "SMART Integrate Link To Patrol Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratelink", "es", "SMART Integrar Enlace con Incidente de Patrullaje."}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratelink", "in", "SMART Integrasikan Tautan ke Insiden Patroli"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratelink", "km", "\u1780\u1798\u17d2\u1798\u179c\u17b7\u1792\u17b8 SMART \u179a\u17bd\u1798\u1794\u1789\u17d2\u1785\u17bc\u179b\u178f\u17c6\u178e\u1791\u17c5\u1793\u17b9\u1784\u17a7\u1794\u17d2\u1794\u178f\u17d2\u178f\u17b7\u17a0\u17c1\u178f\u17bb\u179b\u17d2\u1794\u17b6\u178f"},   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							{"integratelink", "ru", "C\u0441\u044b\u043b\u043a\u0430 SMART Integrate \u0432 \u0438\u043d\u0446\u0438\u0434\u0435\u043d\u0442 \u0440\u0435\u0439\u0434\u0430\t"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							{"integratelink", "uk", "\u0406\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0456\u044f \u043b\u0456\u043d\u043a\u0443 SMART \u0434\u043e \u043f\u0430\u0442\u0440\u0443\u043b\u044c\u043d\u043e\u0433\u043e \u0456\u043d\u0446\u0438\u0434\u0435\u043d\u0442\u0443"},   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							{"integratelink", "zh", "SMART\u96c6\u6210\u94fe\u63a5\u5230\u5de1\u62a4\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					};

					String upsql ="insert into smart.i18n_label(language_uuid, element_uuid, value) select l.uuid, a.uuid, ? from smart.language l join smart.incident_type a on a.ca_uuid = l.ca_uuid WHERE a.keyid = ? and l.code = ?"; //$NON-NLS-1$
					//name
					//key
					//language code
					try(PreparedStatement ps = c.prepareStatement(upsql)){
						for (String[] row : data) {
							ps.setString(1, row[2]);
							ps.setString(2, row[0]);
							ps.setString(3, row[1]);
							ps.execute();
						}
					}
				}catch (Exception ex) {
					throw new SQLException (ex);
				}
			}
		});	
		
	}

	
	private void processCategory801to810(Category category, List<CategoryAttribute> parentAttributes, Connection c,
			PreparedStatement attributeSelect, PreparedStatement categoryKidSelect, 
			PreparedStatement insertQuery, PreparedStatement updateQuery) throws SQLException {

		int order = 1;
		
		//find the existing root attributes for this category
		List<Attribute> attributes = new ArrayList<>();
		attributeSelect.setObject(1, category.getUuid());			
		try(ResultSet rs = attributeSelect.executeQuery()){
			while(rs.next()) {
				UUID attribute_uuid = (UUID) rs.getObject(1);
				boolean isActive = rs.getBoolean(2);
				Attribute temp = new Attribute();
				temp.setIsRequired(isActive);
				temp.setUuid(attribute_uuid);
				attributes.add(temp);
			}
		}
		
		for (CategoryAttribute parentAtt: parentAttributes) {
			insertQuery.setObject(1, category.getUuid());
			insertQuery.setObject(2, parentAtt.getAttribute().getUuid());
			insertQuery.setBoolean(3, false); //root
			insertQuery.setBoolean(4, parentAtt.getIsActive() && category.getIsActive()); //active
			insertQuery.setInt(5, order++); //order
			insertQuery.addBatch();
		}
		insertQuery.executeBatch();
		
		//find the existing root attributes for this category
		attributeSelect.setObject(1, category.getUuid());			
		for (Attribute temp : attributes) {
			CategoryAttribute  cao = new CategoryAttribute();
			cao.setCategory(category);
			cao.setAttribute(temp);
			cao.setOrder(order++);
			cao.setIsActive(temp.getIsRequired());
			parentAttributes.add(cao);
				
			updateQuery.setInt(1, cao.getOrder());
			updateQuery.setObject(2, temp.getUuid());
			updateQuery.setObject(3, category.getUuid());
			updateQuery.addBatch();
				
			
		}	
		updateQuery.executeBatch();
		
		
		//process children
		List<Category> toProcess = new ArrayList<>();
		categoryKidSelect.setObject(1, category.getUuid());
		try(ResultSet rs = categoryKidSelect.executeQuery()){
			while(rs.next()) {
				UUID category_uuid = (UUID) rs.getObject(1);
				Category temp = new Category();
				temp.setUuid(category_uuid);
				temp.setIsActive(rs.getBoolean(2));
				toProcess.add(temp);					
			}
		}
		
		for (Category kid : toProcess) {
			processCategory801to810(kid, new ArrayList<>(parentAttributes), c, attributeSelect, categoryKidSelect, insertQuery, updateQuery);
		}
	}

}
