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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryColumn.Type;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.util.GeometryUtils;

import au.com.bytecode.opencsv.CSVWriter;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Exports query results to csv file.
 * @author Emily
 *
 */
public class CsvQueryExporter implements IQueryExporter{

	@Override
	public void exportQuery(Session session, IPagedQueryResultSet results, Path destination,
			HashMap<ExportOption, Object> exportOptions) throws Exception {
	
		char delimiter = ',';
		if (exportOptions.containsKey(ExportOption.DELIMITER) && exportOptions.get(ExportOption.DELIMITER) instanceof Character){
			delimiter = (Character)exportOptions.get(ExportOption.DELIMITER);
		}
		Locale l = Locale.getDefault();
		if (exportOptions.containsKey(ExportOption.LOCALE) && exportOptions.get(ExportOption.LOCALE) instanceof Locale){
			l = (Locale) exportOptions.get(ExportOption.LOCALE);
		}
		
		MathTransform transform = null;
		if (exportOptions.containsKey(ExportOption.PROJECTION) && exportOptions.get(ExportOption.PROJECTION) instanceof Projection){
			Projection pp = (Projection) exportOptions.get(ExportOption.PROJECTION);
			transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, pp.getParsedCoordinateReferenceSystem());
		}
		
		try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(destination), delimiter)){
			
			//headers
			int dataSize = results.getQueryColumns().size();
			String[] data = new String[dataSize];
			for (int i = 0; i < dataSize; i ++){
				data[i] = results.getQueryColumns().get(i).getColumnName();
			}
			writer.writeNext(data);
			
			PagedResultSetIterator iterator = new PagedResultSetIterator(results, session);
			while(iterator.hasNext()){
				IResultItem item = iterator.next();
				data = new String[dataSize];
				for (int i = 0; i < dataSize; i ++){
					IQueryColumn cc = results.getQueryColumns().get(i);
					if (cc.getDataType() == Type.GEOMETRY && transform != null){
						try{
							Geometry g = (Geometry) results.getQueryColumns().get(i).getValue(item);
							data[i] = JTS.transform(g, transform).toText();
						}catch (Exception ex){
							data[i] = "Error parsing geometry: " + ex.getMessage();
						}
					}else{
						data[i] = results.getQueryColumns().get(i).getValue(item,l);
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
		return "csv";
	}

	@Override
	public boolean supportsOption(ExportOption option) {
		if (option == ExportOption.DELIMITER) return true;
		if (option == ExportOption.PROJECTION) return true;
		if (option == ExportOption.LOCALE) return true;
		return false;
	}
}
