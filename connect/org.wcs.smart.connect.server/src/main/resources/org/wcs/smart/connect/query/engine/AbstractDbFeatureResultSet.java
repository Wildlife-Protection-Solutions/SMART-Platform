/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.connect.query.columns.QueryColumnUtils;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Converts query results to a feature data source for writing
 * query results to shapefile. 
 * 
 * @author Emily
 *
 */
public abstract class AbstractDbFeatureResultSet implements IDbTableResultSet {
	
	public static final String POINT_GEOM_TYPE = "Point"; //$NON-NLS-1$
	
	public static final String MULTI_POINT_GEOM_TYPE = "MultiPoint"; //$NON-NLS-1$
	
	public static final String LINESTRING_GEOM_TYPE = "LineString"; //$NON-NLS-1$
	
	public static final String MULTI_LINESTRING_GEOM_TYPE = "MultiLineString"; //$NON-NLS-1$
	
	protected GeometryFactory gf = new GeometryFactory();
	
	/**
	 * Creates the geometry for the given row in the results set.
	 * @param rs
	 * @return
	 * @throws Exception
	 */
	public abstract Geometry createGeometry(ResultSet rs) throws Exception;
	
	/**
	 * Creates a feature id for the given row in the result set
	 * @param rs
	 * @return
	 * @throws Exception
	 */
	public abstract String createId(ResultSet rs) throws Exception; 
	
	/**
	 * 
	 * @return the geometry type for the feature source
	 */
	public abstract String getGeometryType();

	
	/**
	 * Converts a results set record to a feature
	 * @param rs
	 * @param columns
	 * @param c
	 * @param ftype
	 * @return
	 * @throws Exception
	 */
	public SimpleFeature toFeature(ResultSet rs, List<QueryColumn> columns, Connection c, SimpleFeatureType ftype)  throws Exception {
		List<Object> data = new ArrayList<Object>();
		data.add(createGeometry(rs));
		data.add(createId(rs));  
		
		int i = 0;
		for (QueryColumn qc : columns){
			if (qc.isVisible()){
				Object x = getValue(rs, qc, c);
				if (x instanceof Boolean){
					if ((Boolean)x){
						x = 0;
					}else{
						x = 1;
					}
				}
				if (qc.getType() == QueryColumn.ColumnType.TIME && 
						ftype.getDescriptor(i++) .getType().getBinding().equals(String.class)
						){
					x = DateFormat.getTimeInstance().format((Date)x);
				}
				data.add(x);
			}
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
	}
	
	/**
	 * Creates a feature schema from the set of query columns
	 * @param columns
	 * @param supportsTime
	 * @return
	 */
	public String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:" + getGeometryType() + ":srid=4326,fid:String"); //$NON-NLS-1$ //$NON-NLS-2$
		String colDef = QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, true);
		sb.append(colDef);
		return sb.toString();
	}
}
