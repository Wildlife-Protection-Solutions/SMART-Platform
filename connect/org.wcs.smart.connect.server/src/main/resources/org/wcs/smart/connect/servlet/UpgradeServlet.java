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
}
