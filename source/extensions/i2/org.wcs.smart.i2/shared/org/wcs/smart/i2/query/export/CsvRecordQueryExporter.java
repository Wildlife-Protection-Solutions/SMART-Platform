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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.util.GeometryUtils;

import com.ibm.icu.text.MessageFormat;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Exports query results to csv file.
 * @author Emily
 *
 */
public class CsvRecordQueryExporter implements IQueryExporter{

	@Override
	public boolean canExport(String queryType) {
		return queryType.equalsIgnoreCase(IntelRecordObservationQuery.KEY) || 
				queryType.equalsIgnoreCase(IntelEntityRecordQuery.KEY) || 
				queryType.equalsIgnoreCase(IntelRecordQuery.KEY);
	}
	
	@Override
	public Collection<Path> exportQuery(Session session, IQueryResult result, Path destination,
			HashMap<ExportOption, Object> exportOptions) throws Exception {
		
		IPagedQueryResultSet results = (IPagedQueryResultSet) result;
		
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
		
		Charset cs = StandardCharsets.UTF_8;
		if (exportOptions.containsKey(ExportOption.ENCODING) && exportOptions.get(ExportOption.ENCODING) instanceof Charset){
			cs = (Charset)exportOptions.get(ExportOption.ENCODING);
		}
		
		try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(destination, cs), delimiter)){
			
			//headers
			int dataSize = results.getQueryColumns().size();
			
			List<String> data = new ArrayList<>();
			for (int i = 0; i < dataSize; i ++){
				if (results.getQueryColumns().get(i).getDataType().isGeometry()) {
					data.add( MessageFormat.format("{0} X", results.getQueryColumns().get(i).getColumnName() )); //$NON-NLS-1$
					data.add( MessageFormat.format("{0} Y", results.getQueryColumns().get(i).getColumnName() )); //$NON-NLS-1$
					data.add( MessageFormat.format("{0} Geometry", results.getQueryColumns().get(i).getColumnName() )); //$NON-NLS-1$
				}else {
					data.add( results.getQueryColumns().get(i).getColumnName() );
				}
			}
			writer.writeNext(data.toArray(new String[data.size()]));
			
			
			PagedResultSetIterator iterator = new PagedResultSetIterator(results, session);
			while(iterator.hasNext()){
				IResultItem item = iterator.next();
				data.clear();
				for (int i = 0; i < dataSize; i ++){
					IQueryColumn cc = results.getQueryColumns().get(i);
					if (cc.getDataType().isGeometry() && transform != null){
						try{
							Object v = results.getQueryColumns().get(i).getValue(item);
							
							if (v == null) {
								data.add(null);
								data.add(null);
								data.add(null);
							}else {
								Geometry g = null;
								if (v instanceof Geometry) {
									g = (Geometry) v;
								}else  if( v instanceof Double[]) {
									double x = ((Double[])v)[0];
									double y = ((Double[])v)[1];
									g = (new GeometryFactory()).createPoint(new Coordinate(x, y));
								}else {
									throw new Exception("Cannot covert object of " + v.toString() + " to Geometry"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								g= JTS.transform(g, transform);
								if (g instanceof Point) {
									data.add( String.valueOf(((Point)g).getX()));
									data.add( String.valueOf(((Point)g).getY()));
								}else {
									data.add(null);
									data.add(null);
								}
								data.add(g.toText());
							}
						}catch (Exception ex){
							data.add(null);
							data.add(null);
							data.add("Error parsing geometry: " + ex.getMessage()); //$NON-NLS-1$
							
						}
					}else{
						data.add(results.getQueryColumns().get(i).getValue(item,l));
					}
				}
				String[] sdata = data.toArray(new String[data.size()]);
				ICsvDataExporter.removeLineFeeds(sdata);
				writer.writeNext(sdata);	
			}		
		}
		return Collections.singletonList(destination);
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
		if (option == ExportOption.ENCODING) return true;
		if (option == ExportOption.PROJECTION) return true;
		if (option == ExportOption.LOCALE) return true;
		return false;
	}
}
