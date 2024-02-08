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
package org.wcs.smart.query.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.IStyledQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.QueryColumnUtils;

public class QueryFeatureSource extends ContentFeatureSource {

	protected List<QueryColumn> cachedColumns = null;

	public QueryFeatureSource(ContentEntry entry) {
		super(entry, null);
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		String querycolunnkey = entry.getName().getLocalPart();
		
		QueryColumn geomColumn = null;
		
		for (QueryColumn c : getCachedColumns()) {
			if (c.getKey().equalsIgnoreCase(querycolunnkey)) {
				geomColumn = c;
			}
		}
		if (geomColumn == null) return null;
		
		try {
			return createWaypointSchema(geomColumn);
		} catch (SchemaException ex) {
			throw new IOException(ex.getMessage(), ex);
		}
	}

	@Override
	protected ReferencedEnvelope getBoundsInternal(Query arg0)
			throws IOException {
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		return -1;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		if (((QueryDataSource)entry.getDataStore()).getQuery() instanceof SummaryQuery sq) {
			return new SummaryQueryFeatureReader(sq, getSchema(), getCachedColumns());
		}else {
			String geometryColumn = entry.getName().getLocalPart();
			for (QueryColumn c : getCachedColumns()) {
				if (c.getKey().equalsIgnoreCase(geometryColumn)){
					return new QueryFeatureReader(
						((QueryDataSource)entry.getDataStore()).getQuery(), 
						c, getSchema(), getCachedColumns());
						
				}
			}
		}
			
		return null;

	}
	
	private synchronized List<QueryColumn> getCachedColumns(){
		if (this.cachedColumns != null) return this.cachedColumns;
		this.cachedColumns = ((IStyledQuery)((QueryDataSource)entry.getDataStore()).getQuery()).computeQueryColumns(Locale.getDefault(), null, ((QueryDataSource)entry.getDataStore()).getProjectionProvider());
		return this.cachedColumns;
	}
	
		
	
	private SimpleFeatureType createWaypointSchema(QueryColumn geomColumn) throws SchemaException {
		SimpleFeatureType type = DataUtilities.createType("smart." + geomColumn.getKey(), //$NON-NLS-1$
				getFeatureSchemaDef(getCachedColumns(), geomColumn, true, false));
		return type;
	}

	/**
	 * 
	 * @param columns
	 * @param supportsTime if the defintion supports the Time datatype or if Time
	 *                     datatype needs to be converted to string
	 * @return
	 */
	public static String getFeatureSchemaDef(List<QueryColumn> columns, 
			QueryColumn geomColumn,
			boolean supportsTime, boolean forShape) {
		
		if (!(geomColumn instanceof IGeometryColumn igeom)) throw new IllegalStateException();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("the_geom:"); //$NON-NLS-1$
		sb.append(igeom.getGeometryType().geoToolsType);
		sb.append(":srid="); //$NON-NLS-1$
		sb.append(igeom.getSRID());
		sb.append(",fid:String"); //$NON-NLS-1$
		List<QueryColumn> copy = new ArrayList<>(columns);
		copy.remove(geomColumn);
		sb.append(QueryColumnUtils.createFeatureDefinitionString(copy, supportsTime, forShape));
		return sb.toString();
	}
	
	/**
	 * 
	 * @param columns
	 * @param supportsTime if the defintion supports the Time datatype or if Time
	 *                     datatype needs to be converted to string
	 * @return
	 */
	public static String getFeatureSchemaDef(Attribute.AttributeType type,  List<QueryColumn> columns, boolean supportsTime, boolean forShape) {
		if (!type.isGeometry()) throw new IllegalStateException();
		
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:"); //$NON-NLS-1$
		if (type == Attribute.AttributeType.POLYGON) {
			sb.append("MultiPolygon"); //$NON-NLS-1$
		}else if (type == Attribute.AttributeType.LINE) {
			sb.append("MultiLineString"); //$NON-NLS-1$
		}
		sb.append(":srid=4326,fid:String,"); //$NON-NLS-1$
		sb.append("attribute:"); //$NON-NLS-1$
		sb.append(ColumnType.STRING.geotoolsType);
		sb.append(","); //$NON-NLS-1$
		sb.append("attribute_key:"); //$NON-NLS-1$
		sb.append(ColumnType.STRING.geotoolsType);
		sb.append(","); //$NON-NLS-1$
		sb.append("geometry_source:"); //$NON-NLS-1$
		sb.append(ColumnType.STRING.geotoolsType);
		sb.append(","); //$NON-NLS-1$
		if (type == Attribute.AttributeType.POLYGON) {
			sb.append("geometry_area_km2:"); //$NON-NLS-1$
			sb.append(ColumnType.NUMBER.geotoolsType);
			sb.append(","); //$NON-NLS-1$
		}
		sb.append("geometry_perimeter_km:"); //$NON-NLS-1$
		sb.append(ColumnType.NUMBER.geotoolsType);
		
		
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));
		return sb.toString();
	}
}
