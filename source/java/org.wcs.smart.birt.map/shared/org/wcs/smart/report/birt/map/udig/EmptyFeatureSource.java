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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.model.api.MemberHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ResultSetColumnHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
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
 * Empty feature source for styling of vector layers
 * 
 * @author Emily
 *
 */
public class EmptyFeatureSource extends ContentFeatureSource {

	private OdaDataSetHandle handle;
	private MapLayerInfo mapInfo;
	
	public EmptyFeatureSource(ContentEntry entry, OdaDataSetHandle handle, MapLayerInfo mapInfo) {
		super(entry, null);
		this.handle = handle;
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
		return new FeatureReader<SimpleFeatureType, SimpleFeature>(){

			@Override
			public void close() throws IOException {
			}

			@Override
			public SimpleFeatureType getFeatureType() {
				return EmptyFeatureSource.this.getSchema();
			}

			@Override
			public boolean hasNext() throws IOException {
				return false;
			}

			@Override
			public SimpleFeature next() throws IOException, IllegalArgumentException,
					NoSuchElementException {
				return null;
			}
		};
		
	}
	
	
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try{
			return createSchema();
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
	private SimpleFeatureType createSchema() throws Exception{
		HashSet<String> names = new HashSet<String>();
		StringBuilder sb = new StringBuilder();
		
		sb.append("the_geom:" + mapInfo.getLayerType().getGeotoolsType() + ":srid=4326"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(",fid:String"); //$NON-NLS-1$
		
		MemberHandle resultSet = handle.getCachedMetaDataHandle().getResultSet();
		
		String geomName = mapInfo.getGeometryColumn();
		
		List<?> items = handle.getListProperty("columnHints"); //$NON-NLS-1$
		if (items != null){
			for(Object c : items){
				String n = ((ColumnHint)c).getStringProperty(handle.getModule(), "columnName");  //$NON-NLS-1$
				if (n.equals(geomName)){
					String o = ((ColumnHint)c).getStringProperty(handle.getModule(), "alias"); //$NON-NLS-1$
					if (o != null) geomName = o;
					break; 
				}
			}
		}
		
		for (int i=0; i < resultSet.getListValue().size(); i++) {
			ResultSetColumnHandle resultSetColumn = (ResultSetColumnHandle)resultSet.getAt(i);
			if (resultSetColumn.getColumnName().equalsIgnoreCase(geomName)) continue;
			
			String colType = resultSetColumn.getDataType();
			String maptype;
			if (colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_INTEGER)){
				maptype = "Integer"; //$NON-NLS-1$
			}else if (colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_DECIMAL) ||
				colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_FLOAT)){ 
				maptype = "Double"; //$NON-NLS-1$
			}else if (colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_STRING)){
				maptype = "String"; //$NON-NLS-1$
			}else if (colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_TIME) ||
					colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_DATE) ||
					colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_DATETIME)){ 
				maptype = "Date"; //$NON-NLS-1$
			}else if (colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_BOOLEAN)){
				maptype = "Integer"; //$NON-NLS-1$
			//}else if (colType.equalsIgnoreCase(DesignChoiceConstants.COLUMN_DATA_TYPE_JAVA_OBJECT)){
				
			}else{
				Logger.getLogger(QueryFeatureSource.class.getName()).log(Level.SEVERE, "Query type not supported: " + colType, (Exception)null); //$NON-NLS-1$
				continue;
			}
			
			sb.append(","); //$NON-NLS-1$
			String name = resultSetColumn.getColumnName();
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
