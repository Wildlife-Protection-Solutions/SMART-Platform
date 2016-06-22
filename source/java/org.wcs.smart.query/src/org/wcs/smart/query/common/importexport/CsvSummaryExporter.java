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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Summary Exporter for exporting the
 * summary results as a csv file.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CsvSummaryExporter implements ICsvQueryExporter {

	private char delimiter = DEFAULT_DELIMITER;
	
	/**
	 * 
	 */
	public CsvSummaryExporter() {
	}


	/**
	 * Can only export summary queries
	 * 
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof SummaryQuery){
			return true;
		}
		return false;
	}

	/**
	 * Makes use of the sumQuery.getLastResults() so the summary
	 * query must be run before it can be exported.  
	 * <p>
	 * This is to ensure that the date filter has been set.
	 * </p>
	 * 
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, java.io.File, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(Query query, IQueryResult result, File file,
			HashMap<String, Object> parameters, IProgressMonitor monitor)
			throws Exception {
		SummaryQuery sumQuery = (SummaryQuery)query;
		SummaryQueryResult results = (SummaryQueryResult) sumQuery.getCachedResults();
		if (results == null){
			throw new Exception(Messages.CsvSummaryExporter_QueryNotRun);
		}
		
		if (parameters.get(DELIMITER_KEY) != null){
			try{
				this.delimiter = (Character) parameters.get(DELIMITER_KEY);
			}catch(Exception ex){}
		}
		
		//column headers
		try(CSVWriter writer = new CSVWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
				delimiter, '"',SharedUtils.LINE_SEPARATOR)){ 
		
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
			
				for (int j = 0; j < results.getRowHeaders().size(); j ++){
					data[j] = ""; //$NON-NLS-1$
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					data[k + results.getRowHeaders().size()]= results.getColumnHeaderValues()[i][k].getName();
				}
				
				writer.writeNext(data);
			}

		
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					data[j] = results.getRowHeaderValues()[i][j].getName();
				}
				for(int k = 0; k < results.getData()[i].length; k ++){
					if (results.getData()[i][k] == null){
						data[results.getRowHeaders().size() + k] = null;
					}else{
						data[results.getRowHeaders().size() + k] = String.valueOf(results.getData()[i][k]);
					}
				}
				writer.writeNext(data);
			}
		}

	}
	
	@Override
	public String getId(){
		return "org.wcs.smart.query.export.summary.csv"; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.export.IQueryExporter#getName()
	 */
	@Override
	public String getName() {
		return Messages.CsvSummaryExporter_CSV_ExporterName;
	}

	/**
	 * @see org.wcs.smart.query.export.IQueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "csv"; //$NON-NLS-1$
	}


	/**
	 * Summary queries do not support reprojection
	 */
	@Override
	public boolean supportsProjection() {
		return false;
	}

}
