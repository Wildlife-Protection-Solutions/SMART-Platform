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
package org.wcs.smart.upgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import org.apache.derby.iapi.services.io.FileUtil;
import org.apache.derby.tools.ij;

/**
 * Upgrade engine.  General upgrade process is as follows:
 * 1. unzip smart backup
 * 2. connect to database and ensure version is correct (matches old version)
 * 3. connect to database and run upgrade script
 * 4. connect to database and enusre version is correct (matches new version)
 * 
 * @author Emily
 *
 */
public class UpgradeSmartEngine {

	/**
	 * Processes the backup 
	 * @param backupFile SMART backup
	 * @param oldVersion old software version
	 * @param newVersion new software version 
	 * @param upgradeScript database upgrade script
	 * @return the upgraded backup file
	 * @throws Exception
	 */
	public File processBackupfile(File backupFile, String oldVersion,
			String newVersion, File upgradeScript) throws Exception {
		File newBackupFile = new File(backupFile.getParentFile(), backupFile.getName().substring(0,backupFile.getName().lastIndexOf(".")) + "." + newVersion + ".zip");
		if (newBackupFile.exists()){
			throw new Exception("The output file " +newBackupFile + " already exists.  Please move or delete this file before upgrading the database.");
		}
		File tempDir = unzipBackup(backupFile);
		try {

			File databaseFile = new File(tempDir, "smartdb");
			if (!databaseFile.exists()) {
				throw new Exception(
						"ERROR: Invalid backup file.  Does not contain smart database.");
			}
			try {
				checkVersion(oldVersion, databaseFile);
			} catch (Exception ex) {
				throw new Exception(
						"ERROR: Invalid database version in backup.  Cannot upgrade: "
								+ ex.getMessage(), ex);
			}

			try {
				runUpdateScript(databaseFile, upgradeScript);
			} catch (Exception ex) {
				throw new Exception(
						"ERROR: Upgrade script failed.  Please contact administrator: "
								+ ex.getMessage(), ex);
			}

			try {
				checkVersion(newVersion, databaseFile);
			} catch (Exception ex) {
				throw new Exception(
						"ERROR: Database not upgraded correctly.  New version does not match required version.  Please contact administrator: "
								+ ex.getMessage(), ex);
			}

			ZipUtil.createZip(tempDir.listFiles(), newBackupFile);
			return newBackupFile;
		} finally {
			
			FileUtil.removeDirectory(tempDir);
		}
	}

	
	/**
	 * Runs an file containing a set of sql commands.  
	 * Note: input stream is closed when complete 
	 * 
	 * @param databaseConnection current database connection
	 * @param updateScript inputstream representing the queries to run
	 */
	public static void runScript(Connection databaseConnection, InputStream in) throws Exception{
		try{
			ij.runScript(databaseConnection, in, "utf-8", System.out, "utf-8");
		}finally{
			in.close();
		}
	}
	
	
	/**
	 * Runs update script
	 * @param databaseFile
	 * @param updateScript
	 * @throws Exception
	 */
	private void runUpdateScript(File databaseFile, File updateScript)
			throws Exception {
		Connection c = getConnection(databaseFile);
		 
		try {
			FileInputStream fin = new FileInputStream(updateScript);
			try{
				ij.runScript(c, fin, "utf-8", System.out, "utf-8");
			}finally{
				fin.close();
			}
		} finally {
			c.close();
		}
	}

	/**
	 * Creates a temp directory
	 * @return
	 */
	private static File createTempDir() {
		File baseDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		String basename = "smart_" + Long.toString(System.nanoTime()); //$NON-NLS-1$

		for (int i = 0; i < 1000; i++) {
			File tempDir = new File(baseDir, basename + "_" + i); //$NON-NLS-1$
			if (tempDir.mkdir()) {
				return tempDir;
			}
		}
		return null;
	}

	/**
	 * Unzips backup file to temp directory
	 * @param backupFile
	 * @return temp dir file unzipped to
	 * @throws Exception
	 */
	public static File unzipBackup(File backupFile) throws Exception {
		File tempDir = createTempDir();
		if (tempDir == null) {
			throw new Exception("Unable to create temp directory.");
		}

		ZipUtil.unzipFolder(backupFile, tempDir);
		return tempDir;
	}

	/**
	 * 
	 * @param version
	 * @param dbFile
	 * @return true if version is correct
	 * @throws Exception if version is incorrect
	 */
	private boolean checkVersion(String version, File dbFile) throws Exception {
		Connection c = getConnection(dbFile.getAbsoluteFile());
		try{
			return checkVersion(version, c);
		} catch (Exception ex) {
			throw new Exception("Could not determine database version: "
					+ ex.getMessage());
		} finally {
			c.close();
		}

	}
	
	public static boolean checkVersion(String version, Connection c) throws Exception{
		ResultSet rs = c.createStatement().executeQuery("SELECT * FROM smart.db_version");
		try {
			rs.next();
			String dbVersion = rs.getString(1);

			if (dbVersion.trim().equals(version)) {
				return true;
			} else {
				throw new Exception("Invalid database version.  Got "
						+ dbVersion + " excepected " + version);
			}
		} finally {
			rs.close();
		}
	}

	/**
	 * Connects to database
	 * @param dbFile
	 * @return
	 * @throws Exception
	 */
	private boolean firstConnection = true;
	private Connection getConnection(File dbFile) throws Exception {
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			String dbURL = "jdbc:derby:" + dbFile.getAbsolutePath();
			if (firstConnection){
				dbURL += ";upgrade=true";
				firstConnection = false;
			}
			Connection conn = DriverManager.getConnection(dbURL, "smart_admin",
					"smart_derby");
			return conn;
		} catch (Exception ex) {
			throw new Exception("Could connect to database: "
					+ dbFile.getAbsolutePath() + ". " + ex.getMessage(), ex);
		}
	}
}
