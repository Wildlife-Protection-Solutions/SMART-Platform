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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.ca.export.CaExporter;
import org.wcs.smart.ca.export.ICaDataImportEngine;
import org.wcs.smart.ca.export.ICaDataImporter;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.internal.Messages;

/**
 * Imports all the database data tables.
 * 
 * @author Emily
 *
 */
public class HibernateDataImporter implements ICaDataImporter{

	@Override
	public void importData(ICaDataImportEngine engine,
			IProgressMonitor monitor) throws Exception {
		try{
			processDatabaseFiles(engine.getImportDataDirectory(), engine.getSession(), monitor);
		}finally{
			monitor.done();
		}
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
		
		monitor.beginTask(Messages.CaImporter_Progress_processingTables, tables.size());
		
		HashMap<String, List<String>> keys = getTableConstraints(session);
		Set<String> processed = new HashSet<String>();
		
		Queue<String> tablesToProcess = new LinkedList<String>();
		tablesToProcess.addAll(tables.keySet());

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
			try(BufferedReader reader = new BufferedReader(new FileReader(new File(dataFileDir, files[i])))){
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
			}
		}

		return map;
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
