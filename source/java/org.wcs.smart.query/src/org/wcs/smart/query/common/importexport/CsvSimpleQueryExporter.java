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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.common.filter.ISmartProgressMonitor;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IMemoryQuery;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.Query;
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
	protected char delimiter = DEFAULT_DELIMITER;
	
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
				new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"), //$NON-NLS-1$ 
				delimiter, '"', SharedUtils.LINE_SEPARATOR); 
		
		String data[] = new String[queryColumns.size()]; 
		for (int i = 0; i < data.length; i ++){
			data[i] = queryColumns.get(i).getName(); 
		}
		writer.writeNext(data);
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem, java.io.OutputStream)
	 */
	@Override
	protected void writeRow(IResultItem row)
			throws Exception {
		
		String data[] = new String[queryColumns.size()]; 
		for (int i = 0; i < data.length; i ++){
			Object temp = queryColumns.get(i).getValue(row);
			if(temp != null){
				data[i] = temp.toString();
			}else{
				data[i] = ""; //$NON-NLS-1$
			}
		}
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
		if (query instanceof SimpleQuery && (query instanceof IPagedQuery || query instanceof IMemoryQuery)){
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void export(Query query, IQueryResult result, File file,
			HashMap<String, Object> parameters, ISmartProgressMonitor monitor)
			throws Exception {
		if (parameters.get(DELIMITER_KEY) != null){
			try{
				this.delimiter = (Character) parameters.get(DELIMITER_KEY);
			}catch(Exception ex){}
		}
		if (result instanceof IPagedQueryResultSet){
			super.setData((IPagedQueryResultSet)result, ((SimpleQuery)query).getQueryColumns(Locale.getDefault(), null), file);
		}else if (result instanceof MemoryQueryResult){
			super.setData( ((MemoryQueryResult)result).getData(), 
					((SimpleQuery)query).getQueryColumns(Locale.getDefault(), null), file);
		}else if (result instanceof GridQueryResult){
			super.setData( ((GridQueryResult)result).getData(), 
					((SimpleQuery)query).getQueryColumns(Locale.getDefault(), null), file);
		}
		super.export(monitor);		
	}
}
