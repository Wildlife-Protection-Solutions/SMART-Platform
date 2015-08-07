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
package org.wcs.smart.intelligence.query.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import org.wcs.smart.common.filter.ISmartProgressMonitor;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Exports intelligence summary query results to csv file.
 * 
 * @author Emily
 *
 */
public class IntelligenceSummaryCsvExporter implements ICsvQueryExporter {

	private char delimiter = DEFAULT_DELIMITER;

	/**
	 * Can only export summary queries
	 * 
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (IntelligenceSummaryQuery.KEY.equals(query.getTypeKey())){
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
	public void export (Query query, IQueryResult result, 
			File file, HashMap<String, Object> parameters, 
			ISmartProgressMonitor monitor) throws Exception{
	
		if (result == null){
			throw new Exception(Messages.IntelligenceSummaryCsvExporter_MustRunQuery);
		}
		
		if (parameters.get(DELIMITER_KEY) != null){
			try{
				this.delimiter = (Character) parameters.get(DELIMITER_KEY);
			}catch(Exception ex){}
		}
		SummaryQueryResult results = (SummaryQueryResult) result;
		//column headers
		try(CSVWriter writer = new CSVWriter(
				new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
				delimiter, '"',SharedUtils.LINE_SEPARATOR)){ 
		
			for (int i = 0; i < results.getColumnHeaders().size(); i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
			
				for (int j = 0; j < results.getRowHeaders().size(); j ++){
					data[j] = ""; //$NON-NLS-1$
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					data[k + results.getRowHeaders().size()]= results.getColumnHeaderValues()[i][k].getName();
				}
				
				writer.writeNext(data);
			}
			
			//value headers
			String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
			for (int j = 0; j < results.getRowHeaders().size(); j ++){
				data[j] = ""; //$NON-NLS-1$
			}
			for (int k = 0; k < results.getValueHeaders().size(); k ++){
				data[k + results.getRowHeaders().size()]= results.getValueHeaders().get(k).getName();
			}	
			writer.writeNext(data);
		
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
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
		return Messages.IntelligenceSummaryCsvExporter_CSVLabel;
	}

	/**
	 * @see org.wcs.smart.query.export.IQueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "csv"; //$NON-NLS-1$
	}

}