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
package org.wcs.smart.entity.map;

import java.io.IOException;
import java.util.List;

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
/**
 * Data source for entity sighting queries.
 * @author Emily
 *
 */
public class EntityQueryFeatureSource extends ContentFeatureSource{

	public EntityQueryFeatureSource(ContentEntry entry) {
		super(entry, Query.ALL);
	}
	
	private EntityQueryDataSource getSource() {
		return (EntityQueryDataSource)entry.getDataStore();
	}
	
	
	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
		return null;
	}
	@Override
	protected int getCountInternal(Query query) throws IOException {
		return -1;
	}
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		
		return new EntityQueryDataSourceFeatureReader(getSource().getQuery(), getSchema());
	}
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			return createQuerySchema(getSource().getQuery().getQueryColumns(), true, false);
		} catch (SchemaException e) {
			throw new IOException(e);
		}
	}
	
	
	public static SimpleFeatureType createQuerySchema(List<QueryColumn> columns, boolean supportsTime, boolean forShape) throws SchemaException {
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Point:srid=4326,fid:String"); //$NON-NLS-1$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));	
		SimpleFeatureType type =  DataUtilities.createType(EntityQueryDataSource.TYPENAME, sb.toString()); 
		return type;
	}
}

