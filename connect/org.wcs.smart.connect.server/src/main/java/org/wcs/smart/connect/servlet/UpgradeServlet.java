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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.icon.FixedIconSet;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.i2.model.IntelPermission;
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
						upgradeDb757to800(s);
						updated = true;
					}else if (version.equals("7.5.5") || version.equals("7.5.6") || version.equals("7.5.7")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						//7.5.5/6 shouldn't exist as we didn't upgrade version number
						upgradeDb757to800(s);
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
					//System.out.println(sb.toString());
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
	
	
	private void upgradeDb757to800(Session s) {
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
						"update smart.employee set uuid = '00000000000000000000000000000001' where uuid = '00000000000000000000000000000000'", //$NON-NLS-1$
						
						"alter table connect.data_queue drop constraint status_chk", //$NON-NLS-1$
						"alter table connect.data_queue add constraint status_chk CHECK (status = ANY (ARRAY['UPLOADING', 'QUEUED', 'PROCESSING', 'COMPLETE', 'COMPLETE_WARN', 'ERROR', 'DUPLICATE']))", //$NON-NLS-1$
								
						//warning message for connect processing smart mobile files
						"alter table connect.data_queue add column warning_message varchar", //$NON-NLS-1$
						
						"create table connect.settings(key varchar primary key, value varchar)", //$NON-NLS-1$
						"insert into connect.settings(key, value) values ('connect.dataqueue.smartmobile.processing', 'true')", //$NON-NLS-1$
						"insert into connect.settings(key, value) values ('connect.dataqueue.smartcollect.useroption', 'validaterequeue')", //$NON-NLS-1$
						
						"alter table smart.patrol_leg_day alter column end_time type time(0)", //$NON-NLS-1$
						"alter table smart.patrol_leg_day alter column start_time type time(0)", //$NON-NLS-1$
						"alter table smart.mission_day alter column end_time type time(0)", //$NON-NLS-1$
						"alter table smart.mission_day alter column start_time type time(0)", //$NON-NLS-1$
						
						//https://app.assembla.com/spaces/smart-cs/tickets/3643
						"alter table connect.alerts alter column date type timestamp with time zone", //$NON-NLS-1$

						//versions
						"update connect.connect_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '8.0' where plugin_id = 'org.wcs.smart.cybertracker'", //$NON-NLS-1$

						"update connect.connect_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '3.0' where plugin_id = 'org.wcs.smart.asset.query'", //$NON-NLS-1$

						"update connect.connect_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '2.0' where plugin_id = 'org.wcs.smart.qa'", //$NON-NLS-1$

						"update connect.connect_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.er'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '6.0' where plugin_id = 'org.wcs.smart.er'", //$NON-NLS-1$

						"update connect.connect_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$
						"update connect.ca_plugin_version set version = '8.0.0' where plugin_id = 'org.wcs.smart'", //$NON-NLS-1$

						"update connect.connect_version set version = '8.0.0', last_updated = now()", //$NON-NLS-1$
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
}
