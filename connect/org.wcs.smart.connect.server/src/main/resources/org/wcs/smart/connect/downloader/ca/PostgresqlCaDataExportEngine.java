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
package org.wcs.smart.connect.downloader.ca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jdbc.Work;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.export.ICaDataExportEngine;

/**
 * Derby implementation of a ICaDataExportEngine
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PostgresqlCaDataExportEngine implements ICaDataExportEngine{

	private File outputLocation;
	private ConservationArea ca;
	private Session session;
	private HashMap<String, String> options;
	
	public PostgresqlCaDataExportEngine(File outputLocation, ConservationArea ca, Session session){
		this.session = session;
		this.ca = ca;
		this.outputLocation = outputLocation;
	}
	
	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#getTableColumns(java.lang.String, org.hibernate.Session)
	 */
	@Override
	public String[] getTableColumns(String tableName)
			throws Exception {
		/* write column listing to file */
		String sql = "select column_name " + //$NON-NLS-1$
				"FROM information_schema.columns " + //$NON-NLS-1$
				" WHERE table_schema || '.' || table_name = '" + tableName.toLowerCase() + "'  " + //$NON-NLS-1$ //$NON-NLS-2$
				" AND ( column_default is null or (column_default is not null and " + //$NON-NLS-1$
				" column_default not like 'nextval(%::regclass)')) " + //$NON-NLS-1$
				" ORDER BY ordinal_position";  //$NON-NLS-1$
		
		@SuppressWarnings("unchecked")
		List<String> data = getSession().createSQLQuery(sql).list();
		if (data.size() == 0){
			throw new IllegalStateException("Could not determine table columns for table " + tableName); //$NON-NLS-1$
		}
		
		return (String[])data.toArray(new String[data.size()]);
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#writeTableDefinitionFile(java.io.File, java.lang.String, java.lang.String[])
	 */
	@Override
	public void writeTableDefinitionFile(String tableName, String hibernateClass,
			String[] columns) throws Exception {
		Path columnFile = createFileName( getExportLocation().toPath(), tableName + "." + hibernateClass + ".def"); //$NON-NLS-1$ //$NON-NLS-2$
		try(BufferedWriter writer = Files.newBufferedWriter(columnFile, StandardCharsets.UTF_8)){
			writer.write(tableName.toUpperCase());
			writer.newLine();
			StringBuilder record = new StringBuilder();
			for (int i = 0; i < columns.length; i ++){
				record.append(columns[i].toUpperCase());
				if (i != columns.length - 1){
					record.append(","); //$NON-NLS-1$
				}
			}
			writer.write(record.toString());
			writer.close();
		}
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#exportTableData(java.io.File, java.lang.String, java.lang.String, org.hibernate.Session)
	 */
	@Override
	public void exportTableData(String tableName,
			String hibernateClass,
			String[] columns, 
			String conservationAreaProperty) throws Exception {
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT "); //$NON-NLS-1$
		for (int i = 0; i < columns.length; i ++){
			query.append(columns[i]);
			if (i != columns.length -1){
				query.append(","); //$NON-NLS-1$
			}
		}
		query.append(" FROM "); //$NON-NLS-1$
		query.append(tableName);
		query.append(" WHERE "); //$NON-NLS-1$
		query.append(conservationAreaProperty);
		query.append(" = '" ); //$NON-NLS-1$
		query.append(getConservationArea().getUuid().toString());
		query.append("'" ); //$NON-NLS-1$

		writeQuery(tableName + "." + hibernateClass, query.toString()); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.internal.ca.export.CaDataExporter#exportQueryData(java.io.File, java.lang.String, java.lang.String, org.hibernate.Session)
	 */
	@Override
	public void writeHibernateQuery( 
			String tableName,
			String hibernateClass,
			String[] columns,
			String caPropertyQuery) throws Exception {
		
		
		Query q = getSession().createQuery("from " //$NON-NLS-1$
				+ hibernateClass + " a where a" //$NON-NLS-1$
				+ caPropertyQuery + " = :ca"); //$NON-NLS-1$
		
		//convert hql to sql
		String sql = toSql(q.getQueryString());
		//sql = sql.replaceAll("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
		
		//set sql parameter
		sql = sql.replace("?", "  '" + getConservationArea().getUuid().toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		//find the alias for the first table in the query
		Pattern pattern = Pattern.compile(".*from\\s+[^\\s,]*\\s([^,\\s]*)[,\\s].*"); //$NON-NLS-1$
		Matcher matcher = pattern.matcher(sql);
		matcher.find();
		String key = matcher.group(1);

		int fromindex = sql.indexOf("from"); //$NON-NLS-1$
		
		//build sql query for exporting data
		StringBuilder query = new StringBuilder();
		query.append("SELECT "); //$NON-NLS-1$
		for (int i = 0; i < columns.length; i++) {
			query.append(key);
			query.append("."); //$NON-NLS-1$
			query.append(columns[i]);
			if (i != columns.length - 1) {
				query.append(","); //$NON-NLS-1$
			}
		}
		query.append(" "); //$NON-NLS-1$
		query.append(sql.substring(fromindex));

	
		/* export data to file */
		writeQuery(tableName + "." + hibernateClass, query.toString()); //$NON-NLS-1$
	}

	/**
	 * @throws IOException 
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#writeQuery(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeQuery(String fileName, String query) throws IOException{
		
		final Path outputFile = createFileName(getExportLocation().toPath(), fileName + ".dat"); //$NON-NLS-1$
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				
				String mdquery = query;
				if (mdquery.toUpperCase().contains(" WHERE ")){ //$NON-NLS-1$
					mdquery += " AND false"; //$NON-NLS-1$
				}else{
					mdquery += " WHERE false"; //$NON-NLS-1$
				}
				ResultSet rs = connection.createStatement().executeQuery(mdquery);
				ResultSetMetaData md = rs.getMetaData();
				String[] parts = query.substring(query.toUpperCase().indexOf("SELECT") + "SELECT".length(), query.toUpperCase().indexOf(" FROM ")).split(","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				
				
				for (int i = 0; i < md.getColumnCount(); i ++){
					String classname = md.getColumnClassName(i+1);
					if (classname.equals(UUID.class.getName())){
//					if (md.getColumnType(i) == -2){
						//TODO: assume uuid type ?? probably not correct
						parts[i] = " replace(cast(" + parts[i] + " as varchar), '-', '')";	 //$NON-NLS-1$ //$NON-NLS-2$
					}else if (classname.equals(Boolean.class.getName())){
						parts[i] = " case when " + parts[i] + " then 'true' else 'false' end"; //$NON-NLS-1$ //$NON-NLS-2$
					}else if (classname.equals(byte[].class.getName())){
						parts[i] = " encode (" + parts[i] + ", 'hex')"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				String newQuery = "SELECT "; //$NON-NLS-1$
				for (String x : parts){
					newQuery += x + ","; //$NON-NLS-1$
				}
				newQuery = newQuery.substring(0, newQuery.length() - 1);
				newQuery = newQuery + query.substring(query.toUpperCase().indexOf(" FROM ")); //$NON-NLS-1$
				
				String sql = ("COPY (" + newQuery + ") TO STDOUT WITH (FORMAT CSV, ENCODING 'utf-8', HEADER false, QUOTE '\"', FORCE_QUOTE *)"); //$NON-NLS-1$ //$NON-NLS-2$
				CopyManager copy = new CopyManager((BaseConnection) ((javax.sql.PooledConnection)connection).getConnection());
				
				
				try(BufferedWriter out = Files.newBufferedWriter(outputFile, Charset.forName("UTF-8"))){ //$NON-NLS-1$
					copy.copyOut(sql, out);
				}catch(IOException ex){
					throw new SQLException(ex);
				}
					
			}
		});
	}
	
	private Path createFileName(Path destDir, String tableName) throws IOException{
		Path dir = destDir.resolve(DATABASE_DIR);
		Files.createDirectories(dir);
		return dir.resolve(tableName);
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#getConservationArea()
	 */
	@Override
	public ConservationArea getConservationArea() {
		return this.ca;
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#getSession()
	 */
	@Override
	public Session getSession() {
		return this.session;
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#getExportLocation()
	 */
	@Override
	public File getExportLocation() {
		return this.outputLocation;
	}

	
	public String toSql(String hqlQueryText) {
		
		if (hqlQueryText != null && hqlQueryText.trim().length() > 0) {

			final QueryTranslatorFactory translatorFactory = ((SessionFactoryImpl)  session.getSessionFactory())
					.getSettings().getQueryTranslatorFactory();

			final SessionFactoryImplementor factory = (SessionFactoryImplementor) session.getSessionFactory();

			final QueryTranslator translator = translatorFactory
					.createQueryTranslator(hqlQueryText, hqlQueryText,
							Collections.EMPTY_MAP, factory);
			translator.compile(Collections.EMPTY_MAP, false);
			return translator.getSQLString();
		}
		return null;
	}

	@Override
	public HashMap<String, String> getExportOptions() {
		return this.options;
	}

	@Override
	public void setExportOptions(HashMap<String, String> options) {
		this.options = options;
	}
}
