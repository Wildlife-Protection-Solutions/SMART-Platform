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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.TableInfo;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.Zipper;

/**
 * Derby implementation of a ICaDataExportEngine
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyCaDataExportEngine implements ICaDataExportEngine{

	private Path outputLocation;
	private ConservationArea ca;
	private Session session;
	private HashMap<String, String> options;
	
	private Set<Path> excludefiles = new HashSet<>();
	private Map<Path, Path> pathsToZip = new HashMap<>();
	
	public DerbyCaDataExportEngine(ConservationArea ca, Session session){
		this.session = session;
		this.ca = ca;
		this.options = new HashMap<String, String>();
		this.outputLocation = SmartUtils.createTemporaryDirectory();

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
				"c.schemaname || '.' || b.tablename = UPPER('" + tableName + "') " +  //$NON-NLS-1$ //$NON-NLS-2$
				" AND (a.autoincrementvalue is null or (a.autoincrementvalue is not null and a.columndefault is not null)) " + //not an generated always identity column //$NON-NLS-1$
				"order by a.columnnumber"; //$NON-NLS-1$
		
		
		List<String> data = getSession().createNativeQuery(sql, String.class).list();
		if (data.size() == 0){
			throw new IllegalStateException("Could not determine table columns for table " + tableName); //$NON-NLS-1$
		}
		
		return (String[])data.toArray(new String[data.size()]);
	}

	/**
	 * 
	 */
	@Override
	public void writeTableDefinitionFile(String tableName, String hibernateClass,
			String[] columns) throws Exception {
		Path columnFile = createFileName(tableName + "." + hibernateClass + ".def"); //$NON-NLS-1$ //$NON-NLS-2$
		try(BufferedWriter writer = Files.newBufferedWriter(columnFile)){
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
	 * 
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

	/* 
	 */
	@Override
	public void writeHibernateQuery(TableInfo info, String[] columns, String caPropertyFilter, String caUuidPropertyFilter) throws Exception{
		
		StringBuilder sb = new StringBuilder();
		sb.append("FROM "); //$NON-NLS-1$
		sb.append(info.getClazz().getSimpleName());
		sb.append(" a WHERE a"); //$NON-NLS-1$
		if (caUuidPropertyFilter != null && !caUuidPropertyFilter.isBlank()) {
			sb.append(caUuidPropertyFilter);
		}else {
			sb.append(caPropertyFilter);
		}
		sb.append(" = :ca "); //$NON-NLS-1$
		
		Query<?> q = getSession().createQuery(sb.toString(), info.getClazz());
		
		if (caUuidPropertyFilter != null && !caUuidPropertyFilter.isBlank()) {
			q.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$
		}else {
			q.setParameter("ca", SmartDB.getCurrentConservationArea());	 //$NON-NLS-1$
		}
		
		//convert hql to sql
		String sql = SmartHibernateManager.toSql(q);
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
		writeQuery(info.getTableName() + "." + info.getClazz().getSimpleName(), query.toString()); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#writeQuery(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeQuery(String fileName, String query){
		MutationQuery sqlQuery = getSession().createNativeMutationQuery(
				"CALL SYSCS_UTIL.SYSCS_EXPORT_QUERY('" + query + "', '" + //$NON-NLS-1$ //$NON-NLS-2$
				createFileName(fileName + ".dat"). //$NON-NLS-1$
				normalize().toAbsolutePath().toString() + "', null, null, 'utf-8')" ); //$NON-NLS-1$
		sqlQuery.executeUpdate();
	}
	
	private Path createFileName(String tableName){
		Path dir = this.outputLocation.resolve(ICaDataExportEngine.DATABASE_DIR);
		SmartUtils.createDirectory(dir);
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
	 * @see org.wcs.smart.ca.export.ICaDataExportEngine#getWorkingLocation()
	 */
	@Override
	public Path getWorkingLocation() {
		return this.outputLocation;
	}

	@Override
	public HashMap<String, String> getExportOptions() {
		return options;
	}

	@Override
	public void setExportOptions(HashMap<String, String> options) {
		this.options = options;
	}


	public void addPath(Path source, Path target) {
		this.pathsToZip.put(source, target);
	}
	
	@Override
	public void createExportFile(Path destZipFile, IProgressMonitor progress) throws IOException {
		
		SubMonitor sub = SubMonitor.convert(progress);
		progress.subTask(Messages.DerbyCaDataExportEngine_CompressingTaskName);
		long totalfiles = 0;
		try (Stream<Path> stream = Files.walk(this.outputLocation)){
			totalfiles = stream.filter(p->Files.isRegularFile(p) && !this.excludefiles.contains(p)).count();
		}
		for (Entry<Path,Path> toAdd : this.pathsToZip.entrySet()) {
			try (Stream<Path> stream = Files.walk(toAdd.getKey())){
				totalfiles += stream.filter(p->Files.isRegularFile(p) && !this.excludefiles.contains(p)).count();
			}
		}
		double ftotalfiles = totalfiles;
		sub.beginTask(Messages.DerbyCaDataExportEngine_CompressingTaskName, 100);
		Consumer<Path> updater = new Consumer<Path>() {
			long cnt; 
			long last = 0;
			@Override
			public void accept(Path t) {
				cnt ++;
				int next = (int)Math.round((cnt / ftotalfiles) * 100.0);				
				if (next != last) {
					sub.worked(1);
					sub.checkCanceled();
					last = next;
				}
			}};
			
		Zipper zipper = Zipper.create(destZipFile, updater)
			.excludeFiles(this.excludefiles)
			.addChildrenFiles(this.outputLocation);
		
		for (Entry<Path,Path> toAdd : this.pathsToZip.entrySet()) {
			zipper.addMappedChildren(toAdd.getKey(), toAdd.getValue());
		}
		
		zipper.close();
	}

	@Override
	public void cleanUp() {
		try {
			SmartUtils.deleteDirectory(this.outputLocation);
		} catch (IOException e) {
			SmartPlugIn.log(e.getMessage(),e);
		}
		
	}

	@Override
	public void excludePath(Path exclude) {
		excludefiles.add(exclude);
	}
}
