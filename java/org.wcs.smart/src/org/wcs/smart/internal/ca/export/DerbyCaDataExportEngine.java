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
package org.wcs.smart.internal.ca.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Derby implementation of a ICaDataExportEngine
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyCaDataExportEngine implements ICaDataExportEngine{

	private File outputLocation;
	private ConservationArea ca;
	private Session session;
	
	public DerbyCaDataExportEngine(File outputLocation, ConservationArea ca, Session session){
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
		String sql = "select a.columnname FROM " +
				"sys.syscolumns a, sys.systables b, sys.sysschemas c " +
				" WHERE a.referenceid = b.tableid and b.schemaid = c.schemaid and " +
				"c.schemaname || '.' || b.tablename = '" + 
				tableName.toUpperCase() + "' " +
				" AND (a.autoincrementvalue is null and columndefault is null) " + //not an generated always identity column
				"order by a.columnnumber";
		
		@SuppressWarnings("unchecked")
		List<String> data = getSession().createSQLQuery(sql).list();
		if (data.size() == 0){
			throw new IllegalStateException("Could not determine table columns for table " + tableName);
		}
		
		return (String[])data.toArray(new String[data.size()]);
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#writeTableDefinitionFile(java.io.File, java.lang.String, java.lang.String[])
	 */
	@Override
	public void writeTableDefinitionFile(String tableName,
			String[] columns) throws Exception {

		File columnFile = createFileName(getExportLocation(), tableName + ".def");
		BufferedWriter writer = new BufferedWriter(new FileWriter(columnFile));
		writer.write(tableName);
		writer.newLine();
		StringBuilder record = new StringBuilder();
		
		for (int i = 0; i < columns.length; i ++){
			record.append(columns[i]);
			if (i != columns.length - 1){
				record.append(",");
			}
		}
		writer.write(record.toString());
		writer.close();
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#exportTableData(java.io.File, java.lang.String, java.lang.String, org.hibernate.Session)
	 */
	@Override
	public void exportTableData(String tableName,
			String[] columns, 
			String conservationAreaProperty) throws Exception {
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (int i = 0; i < columns.length; i ++){
			query.append(columns[i]);
			if (i != columns.length -1){
				query.append(",");
			}
		}
		query.append(" FROM ");
		query.append(tableName);
		query.append(" WHERE ");
		query.append(conservationAreaProperty);
		query.append(" = x''" );
		query.append(SmartUtils.encodeHex(getConservationArea().getUuid()));
		query.append("''" );

		writeQuery(tableName, query.toString());
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
		
		
		Query q = getSession().createQuery("from "
				+ hibernateClass + " a where a"
				+ caPropertyQuery + " = :ca");
		
		//convert hql to sql
		String sql = SmartHibernateManager.toSql(q.getQueryString());
		
		//set sql parameter
		sql = sql.replace("?", " x''" + SmartUtils.encodeHex(getConservationArea().getUuid()) + "''");

		//find the alias for the first table in the query
		Pattern pattern = Pattern.compile(".*from\\s+[^\\s,]*\\s([^,\\s]*)[,\\s].*");
		Matcher matcher = pattern.matcher(sql);
		matcher.find();
		String key = matcher.group(1);

		int fromindex = sql.indexOf("from");
		
		//build sql query for exporting data
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (int i = 0; i < columns.length; i++) {
			query.append(key);
			query.append(".");
			query.append(columns[i]);
			if (i != columns.length - 1) {
				query.append(",");
			}
		}
		query.append(" ");
		query.append(sql.substring(fromindex));

	
		/* export data to file */
		writeQuery(tableName, query.toString());
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#writeQuery(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeQuery(String tableName, String query){
		SQLQuery sqlQuery = getSession().createSQLQuery("CALL SYSCS_UTIL.SYSCS_EXPORT_QUERY('" + query + "', '" +
				createFileName(getExportLocation(), tableName + ".dat").getAbsolutePath() + "', null, null, 'utf-8')" );
		
		sqlQuery.executeUpdate();
	}
	
	private File createFileName(File destDir, String tableName){
		File dir = new File(destDir, CaExporter.DATABASE_DIR);
		SmartUtils.createDirectory(dir);
		File f = new File(dir, File.separator + tableName);
		return f;
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

}
