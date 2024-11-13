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
package org.wcs.smart.query.common.importexport;

import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.engine.IColumnInfoProvider;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.IColumnAutoConfigQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IMemoryQuery;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter for simple queries.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CsvSimpleQueryExporter extends SimpleQueryExporter implements ICsvQueryExporter {

	private CSVWriter writer = null;
	private WKTWriter wktWriter = null;
	
	protected char delimiter = DEFAULT_DELIMITER;
	protected Charset cs = StandardCharsets.UTF_8;
	
	
	/**
	 * Creates a new exporter that exports to csv format
	 */
	public CsvSimpleQueryExporter(){}
	
	/**
	 * Close csv writer
	 * 
	 */
	protected void finish() throws Exception{
		writer.close();
	}

	/**
	 * Initialise csv writer and writer
	 * header line.
	 * 
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#init()
	 */
	@Override
	protected void init() throws Exception {
		writer = new CSVWriter(
				new OutputStreamWriter(Files.newOutputStream(outputFile), cs),
				delimiter, '"', SharedUtils.LINE_SEPARATOR); 
		
		wktWriter = new WKTWriter();
		
		String data[] = new String[queryColumns.size()+1]; 
		for (int i = 0; i < data.length-1; i ++){
			data[i] = queryColumns.get(i).getName(); 
		}
		data[data.length - 1] = "Geometry"; //$NON-NLS-1$
		writer.writeNext(data);
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem, java.io.OutputStream)
	 */
	@Override
	protected void writeRow(IResultItem row)
			throws Exception {
		
		String data[] = new String[queryColumns.size()+1]; 
		for (int i = 0; i < data.length-1; i ++){
			QueryColumn qc = queryColumns.get(i);
			data[i] = qc.getValueAsString(qc.getValue(row), Locale.getDefault(), false);
		}
		Geometry value = (Geometry)geometryColumn.getValue(row);
		if (value != null)	data[data.length - 1] = wktWriter.write(value);
		ICsvDataExporter.removeLineFeeds(data);
		writer.writeNext(data);
	}

	@Override
	public String getId(){
		return "org.wcs.smart.query.export.simple.csv"; //$NON-NLS-1$
	}
	
	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#getName()
	 */
	@Override
	public String getName() {
		return Messages.CsvSimpleQueryExporter_CSV_ExpoterName;
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "csv"; //$NON-NLS-1$
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof SimpleQuery 
				&& (query instanceof IPagedQuery || query instanceof IMemoryQuery)){
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void export(Query query, IQueryResult result, Path file,
			Map<String, Object> parameters, IProgressMonitor monitor)
			throws Exception {
		
		//delimiter
		if (parameters.get(DELIMITER_KEY) != null){
			try{
				this.delimiter = (Character) parameters.get(DELIMITER_KEY);
			}catch(Exception ex){}
		}

		
		if (parameters.containsKey(ENCODING_KEY)) {
			cs = (Charset)parameters.get(ENCODING_KEY);
		}
		
		//projection
		IProjectionProvider provider = null;
		if (parameters.get(IQueryExporter.PROJECTION_PARAM_KEY) != null){
			final Projection prj = (Projection) parameters.get(IQueryExporter.PROJECTION_PARAM_KEY);
			provider = new IProjectionProvider() {
				@Override
				public Projection getProjection() {
					return prj;
				}
			};
		}
		
		List<QueryColumn> columns = (List<QueryColumn>) parameters.get(IQueryExporter.QUERY_COLUMN_KEY);
		if (columns == null) {
			try(Session session = HibernateManager.openSession()){
				columns = ((SimpleQuery)query).computeQueryColumns(Locale.getDefault(), null, provider);
			}
		}
		this.geometryColumn = (QueryColumn) parameters.get(IQueryExporter.GEOMETRY_COLUMN_KEY);
		if (this.geometryColumn == null) {
			for (QueryColumn qc : columns) {
				if (qc.isDefaultGeometryColumn()) {
					this.geometryColumn = qc;
					break;
				}
			}
		}
		
		//filter visible columns 
		//in SMART this returns all possible query columns; we only want to include visible query columns

		SimpleQuery simpleQuery = (SimpleQuery) query;
		boolean isDataFiltering = query instanceof IColumnAutoConfigQuery && result instanceof IColumnInfoProvider && ((IColumnAutoConfigQuery)simpleQuery).isShowDataColumnsOnly();
		for (Iterator<QueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
			QueryColumn column = iterator.next();
			boolean isVisibleColumn = isDataFiltering ? ((IColumnInfoProvider)result).isDataColumn(column) : column.isVisible();
			if (!isVisibleColumn){
				iterator.remove();
			}
		}
		
		//set data
		if (result instanceof IPagedQueryResultSet){
			super.setData((IPagedQueryResultSet<?>)result, geometryColumn, columns, file);
		}else if (result instanceof MemoryQueryResult){
			super.setData( ((MemoryQueryResult<IResultItem>)result).getData(), geometryColumn, columns, file);
		}else if (result instanceof GridQueryResult){
			super.setData( ((GridQueryResult)result).getData(), geometryColumn, columns, file);
		}
		//export
		super.export(monitor);		
	}
	
	/**
	 * Simple queries not support reprojection
	 */
	@Override
	public boolean supportsProjection() {
		return true;
	}

	@Override
	public boolean supportsCharEncodings() {
		return true;
	}
}
