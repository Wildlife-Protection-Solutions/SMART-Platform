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
package org.wcs.smart.observation.query.map.geotools;

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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class QueryFeatureSource  extends ContentFeatureSource {

	private List<QueryColumn> cachedColumns = null;


	public QueryFeatureSource(ContentEntry entry) {
		super(entry, null);
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {

		try {
			if (entry.getTypeName().equals(QueryDataSource.WAYPOINT_TYPE)) {
				return createWaypointSchema();
			}else if (entry.getTypeName().equals(QueryDataSource.LINESTRING_GEOM_ATTRIBUTE_TYPE)) {
				return createGeometryAttributeSchema();
			}else if (entry.getTypeName().equals(QueryDataSource.POLYGON_GEOM_ATTRIBUTE_TYPE)) {
				return createGeometryAttributeSchema();
			}
		} catch (SchemaException ex) {
			throw new IOException(Messages.QueryDataSource_SchemaError + ex.getLocalizedMessage(), ex);
		}
		return null;
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
		if (entry.getTypeName().equals(QueryDataSource.WAYPOINT_TYPE)) {
			return new QueryFeatureReader( ((QueryDataSource)entry.getDataStore()).getQuery(), getSchema(), getCachedColumns());
		}
		if (entry.getTypeName().equals(QueryDataSource.POLYGON_GEOM_ATTRIBUTE_TYPE)) {
			return new GeometryAttributeQueryFeatureReader(((QueryDataSource)entry.getDataStore()).getQuery(), getSchema(), getCachedColumns());
		}
		if (entry.getTypeName().equals(QueryDataSource.LINESTRING_GEOM_ATTRIBUTE_TYPE)) {
			return new GeometryAttributeQueryFeatureReader(((QueryDataSource)entry.getDataStore()).getQuery(), getSchema(), getCachedColumns());
		}
		return null;

	}
	
	private synchronized List<QueryColumn> getCachedColumns(){
		if (this.cachedColumns != null) return this.cachedColumns;
		this.cachedColumns = ((QueryDataSource)entry.getDataStore()).getQuery().computeQueryColumns(Locale.getDefault(), null, ((QueryDataSource)entry.getDataStore()).getProjectionProvider());
		return this.cachedColumns;
	}
	
	private SimpleFeatureType createGeometryAttributeSchema() throws SchemaException {
		ArrayList<QueryColumn> nonAttributeColumns = new ArrayList<>();
		for (QueryColumn qc : getCachedColumns()) {
			if (!(qc instanceof AttributeQueryColumn ac)) {
				nonAttributeColumns.add(qc);
			}
		}
		if (entry.getTypeName().equals(QueryDataSource.POLYGON_GEOM_ATTRIBUTE_TYPE)) {

			SimpleFeatureType type = DataUtilities.createType("smart." + entry.getTypeName(), //$NON-NLS-1$
					getFeatureSchemaDef(Attribute.AttributeType.POLYGON, nonAttributeColumns, true, false));
			return type;
		}
		if (entry.getTypeName().equals(QueryDataSource.LINESTRING_GEOM_ATTRIBUTE_TYPE)) {

			SimpleFeatureType type = DataUtilities.createType("smart." + entry.getTypeName(), //$NON-NLS-1$
					getFeatureSchemaDef(Attribute.AttributeType.LINE, nonAttributeColumns, true, false));
			return type;
		}
		return null;
	}
		
	
	private SimpleFeatureType createWaypointSchema() throws SchemaException {
		SimpleFeatureType type = DataUtilities.createType("smart." + QueryDataSource.WAYPOINT_TYPE, //$NON-NLS-1$
				getFeatureSchemaDef(getCachedColumns(), true, false));
		return type;
	}

	/**
	 * 
	 * @param columns
	 * @param supportsTime if the defintion supports the Time datatype or if Time
	 *                     datatype needs to be converted to string
	 * @return
	 */
	public static String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime, boolean forShape) {
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Point:srid=4326,fid:String"); //$NON-NLS-1$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));
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
		sb.append("the_geom:");
		if (type == Attribute.AttributeType.POLYGON) {
			sb.append("MultiPolygon");
		}else if (type == Attribute.AttributeType.LINE) {
			sb.append("MultiLineString");
		}
		sb.append(":srid=4326,fid:String,"); //$NON-NLS-1$
		sb.append("Attribute:");
		sb.append(ColumnType.STRING.geotoolsType);
		sb.append(",");
		sb.append("attribute_key:");
		sb.append(ColumnType.STRING.geotoolsType);
		sb.append(",");
		sb.append("Geometry Source:");
		sb.append(ColumnType.STRING.geotoolsType);
		sb.append(",");
		if (type == Attribute.AttributeType.POLYGON) {
			sb.append("Geometry Area_km2:");
			sb.append(ColumnType.NUMBER.geotoolsType);
			sb.append(",");
		}
		sb.append("Geometry Perimeter_km:");
		sb.append(ColumnType.NUMBER.geotoolsType);
		
		
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));
		return sb.toString();
	}
}
