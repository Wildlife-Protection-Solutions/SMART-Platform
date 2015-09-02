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
package org.wcs.smart.connect.uploader.ca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.type.PostgresUUIDType;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Loads a conservation area export into the postgresql
 * the database.
 * 
 * @author Emily
 *
 */
public class PostgresqlCaLoader {

	/**
	 * The name of the directory where the database data is stored
	 */
	public static final String DATABASE_DIR = "database"; //$NON-NLS-1$
	
	public static final String[] TABLES_TO_IGNORE = new String[]{
		"smart.connect_status", 
		"smart.connect_change_log", 
		"smart.connect_sync_history"};
	
	private Session session;
	
	public PostgresqlCaLoader(Session session){
		this.session = session;
	}
	
	public void importData(File zipFile, ConservationAreaInfo ca) throws Exception {
		File tempDir = ZipUtil.createTemporaryDirectory();
		try{
			ZipUtil.unzipFolder(zipFile, tempDir);
			processDatabaseFiles(tempDir);
			inportPlugInVersionFile(tempDir, ca);
			processFilestore(tempDir, ca);
		}finally{
			tempDir.delete();
		}
	}
	
	private void processFilestore(File dir, ConservationAreaInfo ca) throws Exception{
		File toDir = DataStoreManager.INSTANCE.getConservationAreaFullPath(ca);
		if (!toDir.exists()){
			FileUtils.forceMkdir(toDir);
		}
		
		File filestore = new File(dir, "filestore");
		
		for (File f: filestore.listFiles()){
			if (f.isDirectory()){
				FileUtils.copyDirectory(f, new File(toDir, f.getName()));
			}else if (f.isFile()){
				FileUtils.copyFile(f, new File(toDir, f.getName()));
			}
			
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
	private void processDatabaseFiles(File dir) throws Exception{
		HashMap<String, List<TableInfo>> tables = scanTables(dir);

		//for each table check to ensure the table exists in the database
		//if it does not exist we cannot import it
		Set<String> allTables = new HashSet<String>();
		allTables.addAll(tables.keySet());
		
		HashMap<String, List<String>> keys = getTableConstraints(session);
		Set<String> processed = new HashSet<String>();
		
		Queue<String> tablesToProcess = new LinkedList<String>();
		tablesToProcess.addAll(tables.keySet());

		List<String> toIngore = Arrays.asList(TABLES_TO_IGNORE);
		String last = "";  		//used as a check here so we don't go on forever //$NON-NLS-1$
		while(tablesToProcess.size() > 0){
			String tableName = tablesToProcess.poll();
			if (last.equals(tableName)){
				throw new Exception("Circular table dependencies");
			}
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
				if (infos == null) throw new Exception("Could not get tableinfo for table. " + tableName);
				for (TableInfo info : infos){
					if (!info.getDataFile().exists()){
						throw new Exception(MessageFormat.format("Missing data file ({1}) for table {0}.", new Object[]{ tableName, info.getDataFile().getAbsolutePath()}));
					}
					if (!toIngore.contains(tableName.toLowerCase())){
						importData(tableName, info.getColumns(), info.getDataFile() );
					}
					processed.add(tableName);
				}
				last = ""; //$NON-NLS-1$
			}else{
				tablesToProcess.add(tableName);
				last = tableName;
			}
		}
	}

	private void inportPlugInVersionFile(File dir, ConservationAreaInfo info) throws Exception{
		File f = new File(new File(dir, DATABASE_DIR), "db_versions.dat");
		
		try(CSVReader reader = new CSVReader(new FileReader(f))){
			String[] data = null;
			while((data = reader.readNext()) != null){
				SQLQuery insert = session.createSQLQuery("INSERT INTO connect.ca_plugin_version (ca_uuid, plugin_id, version) values (?, ?, ?)");
				insert.setParameter(0, info.getUuid(), PostgresUUIDType.INSTANCE);
				insert.setParameter(1, data[0]);
				insert.setParameter(2, data[1]);
				insert.executeUpdate();
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
		File dataFileDir = new File(dir, DATABASE_DIR);
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
	private void importData(String tableName, 
			String columns, File dataFile) throws Exception{
		
		String bits[] = tableName.split("\\."); //$NON-NLS-1$
		final StringBuilder query = new StringBuilder();
		query.append("COPY "); //$NON-NLS-1$
		query.append(bits[0] + "." + bits[1]); //schema.table //$NON-NLS-1$
		query.append("(" + columns + ") "); //$NON-NLS-1$ //$NON-NLS-2$
		query.append("FROM STDIN " ); //columns //$NON-NLS-1$
		query.append("WITH (FORMAT CSV, HEADER FALSE, ENCODING 'UTF-8')"); //column indexes //$NON-NLS-1$
		System.out.println(dataFile.toString());
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
		
				String metadataqueryquery = "SELECT " + columns + "  FROM " + tableName + " WHERE false";
				ResultSet rs = connection.createStatement().executeQuery(metadataqueryquery);
				ResultSetMetaData md = rs.getMetaData();
				List<Integer> colsToModified = new ArrayList<Integer>();
				for (int i = 0; i < md.getColumnCount(); i ++){
					String x = md.getColumnTypeName(i+1);
					String y = md.getColumnClassName(i+1);
					if (x.equals("bytea") && y.equals(byte[].class.getName())){
						colsToModified.add(i);
					}
				}
				
				CopyManager copy = new CopyManager((BaseConnection) ((javax.sql.PooledConnection)connection).getConnection());
				try{
					if (colsToModified.size() > 0){
						fixHexData(dataFile, colsToModified);
					}
					
					
					copy.copyIn(query.toString(), new FileReader(dataFile));
				}catch(Exception ex){
					throw new SQLException(ex);
				}
					
			}
		});
	}
	
	private void fixHexData(File dataFile, List<Integer> colsToModify ) throws Exception{
		Path tempFile = FileSystems.getDefault().getPath(dataFile.getAbsolutePath() + ".temp");
		
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile.toPath()));
				CSVWriter writer = new CSVWriter(Files.newBufferedWriter(tempFile)) ){
			
			String[] line = null;
			while( (line = reader.readNext()) != null){
				for (int i : colsToModify){
					line[i] = "\\x" + line[i];
				}
				for (int i = 0; i < line.length; i ++){
					if (line[i].length() == 0) line[i]=null;
				}
				writer.writeNext(line);
			}
		}
		
		Files.delete(dataFile.toPath());
		Files.move(tempFile, dataFile.toPath());
		
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
		sql.append("SELECT tc.table_schema || '.' || tc.table_name as sourcetable, "); //$NON-NLS-1$
		sql.append("ccu.table_schema || '.' || ccu.table_name as requiredtable "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$
		sql.append(" information_schema.table_constraints AS tc "); //$NON-NLS-1$
		sql.append(" JOIN information_schema.key_column_usage AS kcu "); //$NON-NLS-1$
		sql.append(" ON tc.constraint_name = kcu.constraint_name "); //$NON-NLS-1$
		sql.append(" JOIN information_schema.constraint_column_usage AS ccu "); //$NON-NLS-1$
		sql.append(" ON ccu.constraint_name = tc.constraint_name "); //$NON-NLS-1$
		sql.append(" WHERE tc.table_schema = 'smart'"); //$NON-NLS-1$
		
		HashMap<String, List<String>> results = new HashMap<String, List<String>>();
		@SuppressWarnings("unchecked")
		List<Object[]> data = session.createSQLQuery(sql.toString()).list();
		for (Object[] d : data){
			String source = ((String) d[0]).toUpperCase();
			String req = ((String)d[1]).toUpperCase();
			if (source.equals(req)) continue;
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
