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

import java.io.FileWriter;

import org.wcs.smart.SmartUtils;
import org.wcs.smart.query.model.QueryResultItem;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CsvQueryExporter extends QueryExporter {

	private CSVWriter writer = null;
	
	/**
	 * Creates a new exporter that exports to csv format
	 */
	public CsvQueryExporter(){}
	
	/**
	 * Close csv writer
	 * @see org.wcs.smart.query.export.QueryExporter#finish()
	 */
	protected void finish() throws Exception{
		writer.close();
	}

	/**
	 * Initialise csv writer and writer
	 * header line.
	 * 
	 * @see org.wcs.smart.query.export.QueryExporter#init()
	 */
	@Override
	protected void init() throws Exception {
		writer = new CSVWriter(new FileWriter(outputFile), ',', '"',SmartUtils.LINE_SEPARATOR);
		
		String data[] = new String[queryColumns.length]; 
		for (int i = 0; i < queryColumns.length; i ++){
			data[i] = queryColumns[i].getName(); 
		}
		writer.writeNext(data);
	}

	/**
	 * @see org.wcs.smart.query.export.QueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem, java.io.OutputStream)
	 */
	@Override
	protected void writeRow(QueryResultItem row)
			throws Exception {
		
		String data[] = new String[queryColumns.length];
		for (int i = 0; i < queryColumns.length; i ++){
			data[i] = queryColumns[i].getLabelProvider().getText(row);
		}
		writer.writeNext(data);
	}

	/**
	 * @see org.wcs.smart.query.export.QueryExporter#getName()
	 */
	@Override
	public String getName() {
		return "Comma Separated Values";
	}

	/**
	 * @see org.wcs.smart.query.export.QueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "csv";
	}

	/**
	 * @see org.wcs.smart.query.export.QueryExporter#writeResults()
	 */
	@Override
	protected boolean writeResults() {
		return true;
	}
}
