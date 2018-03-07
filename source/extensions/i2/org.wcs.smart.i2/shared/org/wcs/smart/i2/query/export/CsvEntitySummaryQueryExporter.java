/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.export;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Exports entity summary query results to a csv file.
 * 
 * @author Emily
 *
 */
public class CsvEntitySummaryQueryExporter implements IQueryExporter{

	@Override
	public boolean canExport(String queryType) {
		return queryType.equalsIgnoreCase(IntelEntitySummaryQuery.KEY);
	}
	
	@Override
	public void exportQuery(Session session, IQueryResult result, Path destination,
			HashMap<ExportOption, Object> exportOptions) throws Exception {
		
		SummaryQueryResult results = (SummaryQueryResult) result;
		
		char delimiter = ',';
		if (exportOptions.containsKey(ExportOption.DELIMITER) && exportOptions.get(ExportOption.DELIMITER) instanceof Character){
			delimiter = (Character)exportOptions.get(ExportOption.DELIMITER);
		}
		
		try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(destination, StandardCharsets.UTF_8), delimiter, '"',SharedUtils.LINE_SEPARATOR)){ 
		
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
			
				for (int j = 0; j < results.getRowHeaders().size(); j ++){
					data[j] = ""; //$NON-NLS-1$
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					data[k + results.getRowHeaders().size()]= results.getColumnHeaderValues()[i][k].getFullName();
				}
				
				writer.writeNext(data);
			}

		
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					data[j] = results.getRowHeaderValues()[i][j].getFullName();
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
	public String getName(Locale l){
		return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(this, l);
	}
	
	@Override
	public String getExtension(){
		return "csv"; //$NON-NLS-1$
	}

	@Override
	public boolean supportsOption(ExportOption option) {
		if (option == ExportOption.DELIMITER) return true;
		return false;
	}
}
