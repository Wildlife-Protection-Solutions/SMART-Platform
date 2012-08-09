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
package org.wcs.smart.query.export;

import java.io.File;
import java.io.FileWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.model.observation.ObservationQuery;
import org.wcs.smart.query.model.patrol.PatrolQuery;
import org.wcs.smart.util.SmartUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter for simple queries.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CsvSimpleQueryExporter extends SimpleQueryExporter implements IQueryExporter {

	private CSVWriter writer = null;
	
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
		writer = new CSVWriter(new FileWriter(outputFile), ',', '"',SmartUtils.LINE_SEPARATOR);
		
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
	protected void writeRow(QueryResultItem row)
			throws Exception {
		
		String data[] = new String[queryColumns.size()]; 
		for (int i = 0; i < data.length; i ++){
			Object temp = queryColumns.get(i).getValue(row);
			if(temp != null){
				data[i] = temp.toString();
			}else{
				data[i] = "";
			}
		}
		writer.writeNext(data);
	}

	@Override
	public String getId(){
		return "org.wcs.smart.query.export.simple.csv";
	}
	
	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#getName()
	 */
	@Override
	public String getName() {
		return "Comma Separated Values";
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "csv";
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof ObservationQuery || query instanceof PatrolQuery){
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(Query query, File file, IProgressMonitor monitor) throws Exception {
		SimpleQuery q = ((SimpleQuery)query);
		
		super.setData(q.getLastResults(), q.getQueryColumns(), file);
		super.export(monitor);
		
	}
}
