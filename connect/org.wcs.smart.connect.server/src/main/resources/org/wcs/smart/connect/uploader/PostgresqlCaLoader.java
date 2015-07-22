package org.wcs.smart.connect.uploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.wcs.smart.shared.ZipUtil;

public class PostgresqlCaLoader {

	/**
	 * The name of the directory where the database data is stored
	 */
	public static final String DATABASE_DIR = "database"; //$NON-NLS-1$
	
	private Session session;
	
	public PostgresqlCaLoader(Session session){
		this.session = session;
	}
	
	public void importData(File zipFile) throws Exception {
		File tempDir = ZipUtil.createTemporaryDirectory();
		ZipUtil.unzipFolder(zipFile, tempDir);
		processDatabaseFiles(tempDir);
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
					if (!info.getDataFile().exists()) throw new Exception(MessageFormat.format("Missing data file ({1}) for table {0}.", new Object[]{ tableName, info.getDataFile().getAbsolutePath()}));
					importData(tableName, info.getColumns(), info.getDataFile() );
					processed.add(tableName);
				}
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
				
				CopyManager copy = new CopyManager((BaseConnection) ((javax.sql.PooledConnection)connection).getConnection());
				try{
					copy.copyIn(query.toString(), new FileReader(dataFile));
				}catch(IOException ex){
					throw new SQLException(ex);
				}
					
			}
		});
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
