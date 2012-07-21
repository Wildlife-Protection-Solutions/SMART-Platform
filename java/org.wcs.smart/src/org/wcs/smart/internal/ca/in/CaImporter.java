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
package org.wcs.smart.internal.ca.in;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.internal.ca.export.CaExporter;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Responsible for importing and exported conservation area.
 *  
 * @author egouge
 * @since 1.0.0
 */
public class CaImporter {

	/**
	 * Imports conservation area data from a conservation 
	 * area backup file.
	 * 
	 * @param file the conservation area backup file
	 * @param monitor the progress monitor
	 * @throws Exception
	 */
	public static void importCa(File file, IProgressMonitor monitor) throws Exception{
		CaImporter importer = new CaImporter();
		importer.importCaFromFile(file, monitor);
	}
	
	/**
	 * Imports conservation area data from backup file.
	 * @param f
	 * @param monitor
	 * @throws Exception
	 */
	private void importCaFromFile(File f, IProgressMonitor monitor) throws Exception{
		if (!f.exists()){
			throw new IOException("The file '" + f.getAbsolutePath() + "' cannot be found.");
		}
		
		monitor.beginTask("Importing Conservation Area", 4);
		
		//TODO: consider doing a disk space check to ensure enough disk space for this operation
		monitor.subTask("Backuping up current database");
		HibernateManager.endSessionFactory(true);
		File dbBackup = backup();
		monitor.worked(1);
		
		monitor.subTask("Unzipping File");
		File dir = unzipFile(f);
		
		/* need to login as admin user to restore */
		HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		
		Session session = HibernateManager.openSession();
		try{
			monitor.worked(1);
			
			monitor.subTask("Validating Conservation Area");
			byte[] cauuid = validateConservationAreaInfo(dir, session);
			monitor.worked(1);
			
			try{
				processDatabaseFiles(dir, session, monitor);
			}catch (Exception ex){
				try{
					HibernateManager.endSessionFactory(true);
					restoreBackup(dbBackup);
				}catch (Exception e){
					throw new Exception("Error occurred during database import.  Temporary backup could not be restored and system is in an inconsistant state.  It is recomended that you restore a previous system backup.\n\n" + ex.getMessage() + "\n\n" + e.getMessage(), e);
				}
				throw new Exception("Error occurred druing import. System restored to previous state. \n\n" + ex.getMessage(), ex);
			}
			
			try{
				monitor.worked(2);
				monitor.beginTask("Importing Conservation Area", 4);				
				importFileStore(dir, cauuid, monitor);
				monitor.worked(3);
			}catch (Exception ex){
				throw new Exception("Filestore data not imported.\n\nConservation area database information imported.  The filestore (images etc.) could NOT be imported.  This must be imported manually or the system backup should be restored.", ex);
			}
			
		}finally{
			monitor.subTask("Cleaning up");
			try{
				cleanUp(dbBackup);
			}catch (Exception ex){
				SmartPlugIn.log("Could not cleanup directory " + dbBackup.toString(), ex);
			}
			try{
				cleanUp(dir);
			}catch (Exception ex){
				SmartPlugIn.log("Could not cleanup directory " + dir.toString(), ex);
			}
			
			try{
				session.close();
			}catch (Exception ex){
				SmartPlugIn.log("Could not close session", ex);
			}
			/* disconnect from admin user */
			HibernateManager.endSessionFactory(true);
		}
		
	}
	
	/**
	 * Creates a copy of the existing smart database as a restore point.
	 * @return the name of the copy of the database
	 * @throws Exception
	 */
	private File backup() throws Exception{
		File databaseDir = new File(SmartProperties.getInstance().getProperty(SmartProperties.SMART_DB_KEY));
		File copyTo = new File(databaseDir.getParentFile(), "smartdb.bak");
		if (copyTo.exists()){
			FileUtils.deleteDirectory(copyTo);
		}
		
		FileUtils.copyDirectory(databaseDir, copyTo);
		
		return copyTo;
	}
	
	/**
	 * Deletes a given directory
	 * @param backup
	 * @throws Exception
	 */
	private void cleanUp(File backup) throws Exception{
		FileUtils.deleteDirectory(backup);
	}
	
	/**
	 * Restores the provided database backup.
	 * @param backup
	 * @throws Exception
	 */
	private void restoreBackup(File backup) throws Exception{
		File databaseDir = new File(SmartProperties.getInstance().getProperty(SmartProperties.SMART_DB_KEY));
		FileUtils.deleteDirectory(databaseDir);
		FileUtils.copyDirectory(backup, databaseDir);
		cleanUp(backup);
	}
	
	
	/**
	 * Reads the conservation area information and validates
	 * that the conservation area does not exist in the
	 * database.
	 * 
	 * @param dir directory for conservation area information file
	 * @param session
	 * @return conservation area uuid
	 * @throws Exception
	 */
	private byte[] validateConservationAreaInfo(File dir, Session session) throws Exception{
		File caInfo = new File(dir, CaExporter.CA_INFO_FILENAME);
		
		BufferedReader reader = new BufferedReader(new FileReader(caInfo));
		String cauuid = reader.readLine();
		String id = reader.readLine();
		String name = reader.readLine();
		
		session.beginTransaction();
		try{
			byte[] uuid = SmartUtils.decodeHex(cauuid);
			long cnt = (Long)session.createCriteria(ConservationArea.class).add(Restrictions.eq("uuid", uuid)).setProjection(Projections.rowCount()).list().get(0);
			if (cnt != 0){
				throw new Exception("The conservation area " + name + " (" + id + ") already exists in this database and cannot be loaded twice.");
			}
			return uuid;
		}finally{
			session.getTransaction().commit();
			reader.close();
		}
	}
	
	/**
	 * Creates a temporary directory and unzip the
	 * file to created directory.
	 * 
	 * @param file the file to unzip
	 * @return the temporary directory
	 * @throws Exception
	 */
	private File unzipFile(File file) throws Exception{
		File temp = SmartUtils.createTemporaryDirectory();
		ZipUtil.unzipFolder(file, temp);
		return temp;
	}
	
	/**
	 * Processes all database files, loading them into the database
	 * one at a time.
	 * 
	 * @param dir source directory for database tables
	 * @param session database connection 
	 * @param monitor progress monitor
	 * @throws Exception
	 */
	private void processDatabaseFiles(File dir, Session session, IProgressMonitor monitor) throws Exception{
		
		monitor.subTask("Scanning tables");
		HashMap<String, List<String>> keys = getTableConstraints(session);
		
		HashMap<String, TableInfo> tables = scanTables(dir);
		
		Set<String> processed = new HashSet<String>();
		Queue<String> tablesToProcess = new LinkedList<String>();
		for (String table : tables.keySet()){
			tablesToProcess.add(table);
		}
		
		monitor.beginTask("Processing Tables", tablesToProcess.size());

		String last = "";  		//used as a check here so we don't go on forever
		while(tablesToProcess.size() > 0){
			String tableName = tablesToProcess.poll();
			if (last.equals(tableName)){
				throw new Exception("System could not import database.  Circular dependencies in database.");
			}
			monitor.subTask(tableName);
			List<String> requires = keys.get(tableName);
			boolean exportTable = false;
			if (requires == null || requires.size() == 0){
				exportTable = true;
			}else{
				boolean canProcess = true;
				for (String tab : requires){
					if (!tab.equals(tableName) && !processed.contains(tab)){
						canProcess = false;
						break;
					}
				}
				if (canProcess){
					exportTable = true;
				}
			}
			
			if (exportTable){
				TableInfo info = tables.get(tableName);
				if (info == null) throw new Exception("Could not determine table info for table : " + tableName);
				if (!info.getDataFile().exists()) throw new Exception("Could not find data file for table " + tableName + ": " + info.getDataFile().getAbsolutePath());
				importData(session,tableName, info.getColumns(), info.getDataFile() );
				processed.add(tableName);
				monitor.worked(1);
				last = "";
			}else{
				tablesToProcess.add(tableName);
				last = tableName;
			}
		}
	}
	
	/**
	 * Imports the data from the file store to the 
	 *
	 * @param dir the source directory
	 * @param cauuid the conservation area uuid
	 * @param monitor progress monitor
	 * @throws IOException
	 */
	private void importFileStore(File dir, byte[] cauuid, IProgressMonitor monitor) throws IOException{
		monitor.setTaskName("Importing filestore");
		File sourceFile = new File(dir, CaExporter.FILESTORE_DIR);
		
		
		String filestore = SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY);
		filestore = filestore + File.separator + SmartUtils.getDirectoryPath(cauuid);
		File destLocation = new File(filestore);
		if (!destLocation.exists()){
			destLocation.mkdir();
		}
		if (sourceFile.isDirectory()){
			FileUtils.copyDirectory(sourceFile, destLocation);
		}
	}

	
	/**
	 * Imports all data from a given file into the database.
	 * 
	 * @param session database connection
	 * @param tableName table to import data into
	 * @param columns list of columns of data in the file
	 * @param dataFile the data file
	 * @throws Exception
	 */
	private void importData(Session session, String tableName, 
			String columns, File dataFile) throws Exception{
		String bits[] = tableName.split("\\.");
		StringBuilder query = new StringBuilder();
		query.append("CALL SYSCS_UTIL.SYSCS_IMPORT_DATA(");
		query.append("'" + bits[0] + "',"); //schema
		query.append("'" + bits[1] + "',"); //table
		query.append("'" + columns + "'," ); //columns
		query.append("NULL,"); //column indexes
		query.append("'" + dataFile.getCanonicalPath() + "',"); //filename
		query.append("NULL,"); //column delimiter
		query.append("NULL,"); //character delimiter
		query.append("'utf-8',"); //codeset
		query.append("0"); //replace
		query.append(")"); 
		
		SQLQuery sqlQuery = session.createSQLQuery(query.toString());		
		sqlQuery.executeUpdate();
	}
	
	
	/**
	 * Determines the foreign key constraints in the database. 
	 *  
	 * @param session hibernate session
	 * @return a map from a table name to a list of other 
	 * tables with foreign keys related to it
	 */
	private HashMap<String, List<String>> getTableConstraints(Session session){
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT g.schemaname || '.' || c.tablename as sourcetable, ");
		sql.append("h.schemaname || '.' || f.tablename as requiredtable ");
		sql.append("FROM ");
		sql.append("SYS.SYSFOREIGNKEYS a,  ");
		sql.append("SYS.SYSCONSTRAINTS b, ");
		sql.append("SYS.SYSTABLES c, ");
		sql.append("SYS.SYSCONSTRAINTS e, ");
		sql.append("SYS.SYSTABLES f, ");
		sql.append("SYS.SYSSCHEMAS g, ");
		sql.append("SYS.SYSSCHEMAS h ");
		sql.append("WHERE a.constraintid = b.constraintid ");
		sql.append("AND b.tableid = c.tableid ");
		sql.append("AND e.constraintid = a.keyconstraintid ");
		sql.append("AND f.tableid = e.tableid ");
		sql.append("AND g.schemaid = f.schemaid ");
		sql.append("AND h.schemaid = c.schemaid");
		
		HashMap<String, List<String>> results = new HashMap<String, List<String>>();
		@SuppressWarnings("unchecked")
		List<Object[]> data = session.createSQLQuery(sql.toString()).list();
		for (Object[] d : data){
			String source = (String) d[0];
			String req = (String)d[1];
			
			List<String> requires = results.get(source);
			if (requires == null){
				requires = new ArrayList<String>();
				results.put(source, requires);
			}
			requires.add(req);
		}
		return results;
	}
	
	/**
	 * Scans the directory/database for all table definition 
	 * files (*.def).
	 * 
	 * @param dir the root directory of the conservation area 
	 * backup file.
	 * 
	 * @return map of tableNames to tableInfo of all def files
	 * found in the directory.
	 * 
	 * @throws Exception
	 */
	private HashMap<String, TableInfo> scanTables(File dir) throws Exception{
		File dataFileDir = new File(dir, CaExporter.DATABASE_DIR);
		//list all .def file
		String files[] = dataFileDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(".def"));
			}
		});
		
		//read info in files
		HashMap<String, TableInfo> map = new HashMap<String, TableInfo>();
		for (int i = 0; i < files.length; i++){
			BufferedReader reader = new BufferedReader(new FileReader(new File(dataFileDir, files[i])));
			try{
				String tablename = reader.readLine().toUpperCase();
				String columns = reader.readLine();
				String data = files[i].substring(0,files[i].lastIndexOf(".def"));
				
				map.put(tablename, new TableInfo(tablename, columns, 
						new File(dataFileDir,data + ".dat")));
			}finally{
				reader.close();
			}
		}
		return map;
	}

	/**
	 * Internal class for tracking table information
	 * include the table name, list of columns to import,
	 * and the data file location.
	 * 
	 * @author egouge
	 * @since 1.0.0
	 */
	class TableInfo{


		private String tableName;
		private String columns;
		private File dataFile;
		
		/**
		 * @param tableName the table name
		 * @param columns the table columns
		 * @param dataFile the data file
		 */
		public TableInfo(String tableName, String columns, File dataFile){
			this.tableName = tableName;
			this.columns = columns;
			this.dataFile = dataFile;
		}
		
		/**
		 * @return the table name
		 */
		public String getTableName() {
			return tableName;
		}

		/**
		 * @return the columns of data in 
		 * the data file
		 */
		public String getColumns() {
			return columns;
		}

		/**
		 * @return the data file
		 */
		public File getDataFile() {
			return dataFile;
		}
	}

	
}

