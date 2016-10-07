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
package org.wcs.smart.report.birt.map.udig;

import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultMetaData;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * Feature reader for waypoint/observation query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFeatureSource extends ContentFeatureSource {

	private IQueryResults queryResults;
	private MapLayerInfo mapInfo;
	
	public QueryFeatureSource(ContentEntry entry, IQueryResults queryResults, MapLayerInfo mapInfo) {
		super(entry, null);
		this.queryResults = queryResults;
		this.mapInfo = mapInfo;
	}

	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query)
			throws IOException {
		return null;
	}
	@Override
	protected int getCountInternal(Query query) throws IOException {
		return -1;
	}
	
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(
			Query query) throws IOException {
		return new QueryFeatureReader(queryResults, getSchema(), mapInfo);
	}
	
	
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try{
			return createSchema(queryResults.getResultMetaData());
		}catch (Exception ex){
			throw new IOException(ex);
		}
	}
	
	/**
	 * Creates the simple feature type for the query
	 * 
	 * @return the simple feature type for the query
	 * 
	 * @throws SchemaException
	 */
	private SimpleFeatureType createSchema(IResultMetaData md) throws Exception{
		HashSet<String> names = new HashSet<String>();
		StringBuilder sb = new StringBuilder();
		
		sb.append("the_geom:" + mapInfo.getLayerType().getGeotoolsType() + ":srid=4326"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(",fid:String"); //$NON-NLS-1$
		for (int i = 1; i <= md.getColumnCount(); i++){
			if (md.getColumnName(i).equalsIgnoreCase(mapInfo.getGeometryColumn())) continue;
			
			String maptype = null;
			int type = md.getColumnType(i);
			if (type ==  DataType.INTEGER_TYPE){
				maptype = "Integer"; //$NON-NLS-1$
			}else if (type == DataType.DOUBLE_TYPE ||
					type == DataType.DECIMAL_TYPE){
				maptype = "Double"; //$NON-NLS-1$
			}else if (type == DataType.STRING_TYPE){
				maptype = "String"; //$NON-NLS-1$
			}else if (type == DataType.DATE_TYPE ||
					type == DataType.SQL_DATE_TYPE ||
					type == DataType.SQL_TIME_TYPE){
				maptype = "Date"; //$NON-NLS-1$
			}else if (type == DataType.BOOLEAN_TYPE){
				maptype = "Integer"; //$NON-NLS-1$
			}else{
				Logger.getLogger(QueryFeatureSource.class.getName()).log(Level.SEVERE, "Query type not supported: " + type, (Exception)null); //$NON-NLS-1$
				continue;
			}
			
			sb.append(","); //$NON-NLS-1$
			String name = md.getColumnName(i);
			name = name.replaceAll(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
			name = name.replaceAll("[^\\p{L}\\p{Nd}_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
			
			String tempname = name;
			int cnt = 1;
			while(names.contains(tempname)){
				tempname = name + "_" + cnt; //$NON-NLS-1$
				cnt++;
			}
			//Name is not a valid attribute name
			if (tempname.equalsIgnoreCase("Name")){ //$NON-NLS-1$
				tempname = tempname +"_"; //$NON-NLS-1$
			}
			
			sb.append(tempname);
			sb.append(":"); //$NON-NLS-1$
			sb.append(maptype);
		}
		
		SimpleFeatureType type =  DataUtilities.createType("smart." + entry.getTypeName(), sb.toString()); //$NON-NLS-1$
		
		return type;
	}
}
