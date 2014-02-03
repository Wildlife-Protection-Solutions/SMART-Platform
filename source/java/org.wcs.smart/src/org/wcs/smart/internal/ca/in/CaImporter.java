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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.export.CaExporter;
import org.wcs.smart.internal.ca.export.PlugInConfigurationExporter;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

import au.com.bytecode.opencsv.CSVReader;

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
			throw new IOException(Messages.CaImporter_Error_CouldNotFindImportFile + f.getAbsolutePath());
		}
		
		monitor.beginTask(Messages.CaImporter_Progress_ImportingCa, 5);
		
		//TODO: consider doing a disk space check to ensure enough disk space for this operation
		monitor.subTask(Messages.CaImporter_Progress_BackupCurrent);
		HibernateManager.endSessionFactory(true);
		File dbBackup = backup();
		monitor.worked(1);
		
		monitor.subTask(Messages.CaImporter_Progress_UnzippingFile);
		File dir = unzipFile(f);
		
		/* need to login as admin user to restore */
		HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		
		Session session = HibernateManager.openSession();
		try{
			monitor.worked(1);
			
			monitor.subTask(Messages.CaImporter_Progress_ValidatingCaImport);
			byte[] cauuid = validateConservationAreaInfo(dir, session);
			monitor.worked(1);
			
			monitor.subTask(Messages.CaImporter_ValidatingPluginProgressMessage);
			if (!validatePlugInConfiguration(dir, session)){
				return;
			}
			monitor.worked(1);
			
			try{
				processDatabaseFiles(dir, session, monitor);
			}catch (Exception ex){
				try{
					try{
						session.close();
					}catch (Exception ex2){}
					HibernateManager.endSessionFactory(true);
					restoreBackup(dbBackup);
				}catch (Exception e){
					throw new Exception(Messages.CaImporter_Error_ImportErrorBackupNoRestored + ex.getLocalizedMessage(), e);
				}
				throw new Exception(Messages.CaImporter_Error_SystemRestored + ex.getLocalizedMessage(), ex);
			}
			
			try{
				monitor.worked(2);
				monitor.beginTask(Messages.CaImporter_Progress_ImportingCa, 4);				
				importFileStore(dir, cauuid, monitor);
				monitor.worked(3);
			}catch (Exception ex){
				throw new Exception(Messages.CaImporter_Error_FilestoreNotImported, ex);
			}
			
		}finally{
			monitor.subTask(Messages.CaImporter_Progress_CleanUp);
			try{
				cleanUp(dbBackup);
			}catch (Exception ex){
				SmartPlugIn.log(Messages.CaImporter_Error_CleanUpFailed + dbBackup.toString(), ex);
			}
			try{
				cleanUp(dir);
			}catch (Exception ex){
				SmartPlugIn.log(Messages.CaImporter_Error_CleanUpFailed + dir.toString(), ex);
			}
			
			try{
				if (session.isOpen()){
					session.close();
				}
			}catch (Exception ex){
				SmartPlugIn.log(Messages.CaImporter_Error_CouldNotCloseSession, ex);
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
		File databaseDir = new File(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
		File copyTo = new File(databaseDir.getParentFile(), "smartdb.bak"); //$NON-NLS-1$
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
		File databaseDir = new File(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
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
		if (cauuid == null){
			throw new Exception(Messages.CaImporter_Error_NoCaIdentifierFound);
		}
		String id = reader.readLine();
		String name = reader.readLine();
		reader.readLine();	//description;
		String dbVersion = Messages.SmartPlugIn_UnknownVersion;
		String line = reader.readLine();
		if (line != null){
			dbVersion = line;
		}
		session.beginTransaction();
		byte[] uuid = null;
		try{
			uuid = SmartUtils.decodeHex(cauuid);
			long cnt = (Long)session.createCriteria(ConservationArea.class).add(Restrictions.eq("uuid", uuid)).setProjection(Projections.rowCount()).list().get(0); //$NON-NLS-1$
			if (cnt != 0){				
				throw new Exception(MessageFormat.format(Messages.CaImporter_Error_CaAlreadyExists, new Object[]{name, id}));
			}
			
		}finally{
			session.getTransaction().commit();
			reader.close();
		}
		/*validate backup file version */
		String smartDbVersion = SmartProperties.getInstance().getProperty(SmartProperties.DB_VERSION_KEY); 
		if (!dbVersion.equals(smartDbVersion)){
			throw new Exception(MessageFormat.format(Messages.CaImporter_InvalidExportVersion, new Object[]{dbVersion, smartDbVersion}));
		}
		return uuid;
	}
	
	/**
	 * Validates that the plugin version are consistent:
	 * <p>
	 * Checks that the software as the same version
	 * of all the plugins included in the backup.
	 * </p><p>
	 * If a plugin is missing from the software and included in the backup
	 * then the user is warned that they the related plugin data
	 * will not be imported and not included in future exports.
	 * </p><p>
	 * If a plugin has a different version from the software
	 * users will be unable to import.  They must update
	 * their systems first.
	 * </p>
	 * 
	 * 
	 * @param dir directory for conservation area information file
	 * @param session
	 * @throws Exception
	 */
	private boolean validatePlugInConfiguration(File dir, Session session) throws Exception{
		File caInfo = new File(new File(dir, CaExporter.DATABASE_DIR), PlugInConfigurationExporter.CONFIG_TABLE_NAME + ".dat"); //$NON-NLS-1$
		
		HashMap<String, String> versions = new HashMap<String, String>();
		CSVReader reader = new CSVReader(new FileReader(caInfo));
		try{
			String[] data= reader.readNext();
			while(data != null){
				versions.put(data[0], data[1]);
				data= reader.readNext();
			}
		}finally{
			reader.close();
		}
		
		session.beginTransaction();
		try{
			final StringBuilder warnings = new StringBuilder();
			for (Entry<String, String> e : versions.entrySet()){
				String requiredVersion = HibernateManager.getPlugInVersion(e.getKey(), session);
				if (requiredVersion == null){
					//plugin not found; warn user
					warnings.append(e.getKey());
					warnings.append(", "); //$NON-NLS-1$
				}else if (  !requiredVersion.equals(e.getValue()) ){
					//versions don't match; don't allow user to 
					throw new Exception(MessageFormat.format(Messages.CaImporter_VersionError, new Object[]{e.getKey(), requiredVersion, e.getValue()}));
				}
			}
			
			
			if (warnings.length() > 0){
				final boolean[] x = new boolean[]{true};
				warnings.deleteCharAt(warnings.length() - 1);
				warnings.deleteCharAt(warnings.length() - 1);
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						String msg = MessageFormat.format(Messages.CaImporter_ExtraDataWarning, new Object[]{warnings.toString()});
						if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.CaImporter_WarningDialogTitle, msg)){
							x[0] = false;
						}
						
					}});
				if (!x[0]){
					return false;
				}
			}
			
		}finally{
			session.getTransaction().commit();
			reader.close();
		}
		return true;
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
		
		monitor.subTask(Messages.CaImporter_Progress_ScanningTable);
		HashMap<String, List<String>> keys = getTableConstraints(session);
		
		HashMap<String, List<TableInfo>> tables = scanTables(dir);
		//for each table check to ensure the table exists in the database
		//if it does not exist we cannot import it
		Set<String> allTables = new HashSet<String>();
		allTables.addAll(tables.keySet());
		for (String tableName : allTables){
			String table = tableName;
			if (table.indexOf('.') >= 0){
				table = table.substring(table.indexOf('.')+1);
			}
			if (!DerbyHibernateExtensions.tableExists(session, table)){
				tables.remove(tableName);
			}
		}
		
		
		Set<String> processed = new HashSet<String>();
		
		Queue<String> tablesToProcess = new LinkedList<String>();
		tablesToProcess.addAll(tables.keySet());
		
		
		monitor.beginTask(Messages.CaImporter_Progress_processingTables, tablesToProcess.size());

		String last = "";  		//used as a check here so we don't go on forever //$NON-NLS-1$
		while(tablesToProcess.size() > 0){
			String tableName = tablesToProcess.poll();
			if (last.equals(tableName)){
				throw new Exception(Messages.CaImporter_Error_CirculateTableDependencies);
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
				List<TableInfo> infos = tables.get(tableName);
				if (infos == null) throw new Exception(Messages.CaImporter_Error_TableInfo + tableName);
				for (TableInfo info : infos){
					if (!info.getDataFile().exists()) throw new Exception(MessageFormat.format(Messages.CaImporter_Error_TableDataFiles, new Object[]{ tableName, info.getDataFile().getAbsolutePath()}));
					importData(session,tableName, info.getColumns(), info.getDataFile() );
					processed.add(tableName);
				}
				monitor.worked(1);
				last = ""; //$NON-NLS-1$
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
		monitor.setTaskName(Messages.CaImporter_Progress_ImportingFileStore);
		File sourceFile = new File(dir, CaExporter.FILESTORE_DIR);
		
		
		String filestore = SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE);
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
		String bits[] = tableName.split("\\."); //$NON-NLS-1$
		StringBuilder query = new StringBuilder();
		query.append("CALL SYSCS_UTIL.SYSCS_IMPORT_DATA("); //$NON-NLS-1$
		query.append("'" + bits[0] + "',"); //schema //$NON-NLS-1$ //$NON-NLS-2$
		query.append("'" + bits[1] + "',"); //table //$NON-NLS-1$ //$NON-NLS-2$
		query.append("'" + columns + "'," ); //columns //$NON-NLS-1$ //$NON-NLS-2$
		query.append("NULL,"); //column indexes //$NON-NLS-1$
		query.append("'" + dataFile.getCanonicalPath() + "',"); //filename //$NON-NLS-1$ //$NON-NLS-2$
		query.append("NULL,"); //column delimiter //$NON-NLS-1$
		query.append("NULL,"); //character delimiter //$NON-NLS-1$
		query.append("'utf-8',"); //codeset //$NON-NLS-1$
		query.append("0"); //replace //$NON-NLS-1$
		query.append(")");  //$NON-NLS-1$
		
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
		sql.append("SELECT g.schemaname || '.' || c.tablename as sourcetable, "); //$NON-NLS-1$
		sql.append("h.schemaname || '.' || f.tablename as requiredtable "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$
		sql.append("SYS.SYSFOREIGNKEYS a,  "); //$NON-NLS-1$
		sql.append("SYS.SYSCONSTRAINTS b, "); //$NON-NLS-1$
		sql.append("SYS.SYSTABLES c, "); //$NON-NLS-1$
		sql.append("SYS.SYSCONSTRAINTS e, "); //$NON-NLS-1$
		sql.append("SYS.SYSTABLES f, "); //$NON-NLS-1$
		sql.append("SYS.SYSSCHEMAS g, "); //$NON-NLS-1$
		sql.append("SYS.SYSSCHEMAS h "); //$NON-NLS-1$
		sql.append("WHERE a.constraintid = b.constraintid "); //$NON-NLS-1$
		sql.append("AND b.tableid = c.tableid "); //$NON-NLS-1$
		sql.append("AND e.constraintid = a.keyconstraintid "); //$NON-NLS-1$
		sql.append("AND f.tableid = e.tableid "); //$NON-NLS-1$
		sql.append("AND g.schemaid = f.schemaid "); //$NON-NLS-1$
		sql.append("AND h.schemaid = c.schemaid"); //$NON-NLS-1$
		
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
	 * <p>
	 * Note: it is possible for a single table to have multiple 
	 * export files.  This occurs when multiple hibernate classes
	 * are mapped to a single table in the database.
	 * </p>
	 * @param dir the root directory of the conservation area 
	 * backup file.
	 * 
	 * @return map of tableNames to list of tableInfo for all def files
	 * found in the directory
	 * 
	 * @throws Exception
	 */
	private HashMap<String, List<TableInfo>> scanTables(File dir) throws Exception{
		File dataFileDir = new File(dir, CaExporter.DATABASE_DIR);
		//list all .def file
		String files[] = dataFileDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(".def")); //$NON-NLS-1$
			}
		});
		
		//read info in files
		HashMap<String, List<TableInfo>> map = new HashMap<String, List<TableInfo>>();
		for (int i = 0; i < files.length; i++){
			BufferedReader reader = new BufferedReader(new FileReader(new File(dataFileDir, files[i])));
			try{
				String tablename = reader.readLine().toUpperCase();
				String columns = reader.readLine();
				String data = files[i].substring(0,files[i].lastIndexOf(".def")); //$NON-NLS-1$
				
				List<TableInfo> tablefiles = map.get(tablename);
				if (tablefiles  == null){
					tablefiles = new ArrayList<TableInfo>();
					map.put(tablename, tablefiles);
				}
				tablefiles.add(new TableInfo(tablename, columns, 
						new File(dataFileDir,data + ".dat"))); //$NON-NLS-1$
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

