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
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;
import org.wcs.smart.observation.query.internal.Messages;

public class QueryFeatureSource  extends ContentFeatureSource {

	private List<QueryColumn> cachedColumns;


	public QueryFeatureSource(ContentEntry entry) {
		super(entry, null);
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {

		try {
			if (entry.getTypeName().equals(QueryDataSource.WAYPOINT_TYPE)) {
				return createWaypointSchema();
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
		return new QueryFeatureReader( ((QueryDataSource)entry.getDataStore()).getQuery(), getSchema(), cachedColumns);

	}
	

	private SimpleFeatureType createWaypointSchema() throws SchemaException {
		cachedColumns = ((QueryDataSource)entry.getDataStore()).getQuery().computeQueryColumns(Locale.getDefault(), null, ((QueryDataSource)entry.getDataStore()).getProjectionProvider());
		SimpleFeatureType type = DataUtilities.createType("smart." + QueryDataSource.WAYPOINT_TYPE, //$NON-NLS-1$
				getFeatureSchemaDef(cachedColumns, true, false));
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
}
