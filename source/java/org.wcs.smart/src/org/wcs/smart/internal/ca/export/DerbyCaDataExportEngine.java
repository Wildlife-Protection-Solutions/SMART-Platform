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
import org.wcs.smart.util.UuidUtils;

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
		String sql = "select a.columnname FROM " + //$NON-NLS-1$
				"sys.syscolumns a, sys.systables b, sys.sysschemas c " + //$NON-NLS-1$
				" WHERE a.referenceid = b.tableid and b.schemaid = c.schemaid and " + //$NON-NLS-1$
				"c.schemaname || '.' || b.tablename = '" +  //$NON-NLS-1$
				tableName.toUpperCase() + "' " + //$NON-NLS-1$
				" AND (a.autoincrementvalue is null or (a.autoincrementvalue is not null and a.columndefault is not null)) " + //not an generated always identity column //$NON-NLS-1$
				"order by a.columnnumber"; //$NON-NLS-1$
		
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
		File columnFile = createFileName(getExportLocation(), tableName + "." + hibernateClass + ".def"); //$NON-NLS-1$ //$NON-NLS-2$
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(columnFile))){
			writer.write(tableName);
			writer.newLine();
			StringBuilder record = new StringBuilder();
			for (int i = 0; i < columns.length; i ++){
				record.append(columns[i]);
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
		query.append(" = x''" ); //$NON-NLS-1$
		query.append(UuidUtils.uuidToString(getConservationArea().getUuid()));
		query.append("''" ); //$NON-NLS-1$

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
		String sql = SmartHibernateManager.toSql(q.getQueryString());
		sql = sql.replaceAll("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
		
		//set sql parameter
		sql = sql.replace("?", " x''" + UuidUtils.uuidToString(getConservationArea().getUuid()) + "''"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#writeQuery(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeQuery(String fileName, String query){
		SQLQuery sqlQuery = getSession().createSQLQuery("CALL SYSCS_UTIL.SYSCS_EXPORT_QUERY('" + query + "', '" + //$NON-NLS-1$ //$NON-NLS-2$
				createFileName(getExportLocation(), fileName + ".dat").getAbsolutePath() + "', null, null, 'utf-8')" ); //$NON-NLS-1$ //$NON-NLS-2$
		sqlQuery.executeUpdate();
	}
	
	private File createFileName(File destDir, String tableName){
		File dir = new File(destDir, ICaDataExportEngine.DATABASE_DIR);
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
