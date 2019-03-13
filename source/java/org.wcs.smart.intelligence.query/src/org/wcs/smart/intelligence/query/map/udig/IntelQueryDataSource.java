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
package org.wcs.smart.intelligence.query.map.udig;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.geotools.data.DataUtilities;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;

/**
 * A geotools intelligence record query data source that 
 * gets the intelligence records returns
 * all points associated with the records.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class IntelQueryDataSource extends ContentDataStore{

	public static final String INTEL_TYPE = "Intelligence";  //$NON-NLS-1$
	
	private IntelligenceRecordQuery query;
	private List<QueryColumn> cachedColumns;
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public IntelQueryDataSource(IntelligenceRecordQuery query){
		this.query = query;
	}

	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		this.cachedColumns = null;
	}

	public IntelligenceRecordQuery getQuery() {
		return this.query;
	}
	
	public List<QueryColumn> getColumns(){
		return this.cachedColumns;
	}
	
	
	/**
	 * Creates the simple feature type for the intelligence record query
	 * from list of query columns included in the query.
	 * 
	 * @return the simple feature type for the query
	 * 
	 * @throws SchemaException
	 */
	public static SimpleFeatureType createIntelligenceRecordSchema(List<QueryColumn> columns, boolean forShape) throws SchemaException{
		SimpleFeatureType type =  DataUtilities.createType("smart." + INTEL_TYPE, getFeatureSchemaDef(columns, true, forShape)); //$NON-NLS-1$
		return type;
	}
	
	
	/**
	 * Create feature definition string from query columns.
	 */
	private static String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:MultiPoint:srid=4326"); //$NON-NLS-1$
		sb.append(",fid:String"); //$NON-NLS-1$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));
		return sb.toString();
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		if (cachedColumns == null) {
			cachedColumns = query.computeQueryColumns(Locale.getDefault(), null, null);
		}
		return Collections.singletonList(new NameImpl(INTEL_TYPE));
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		return new IntelQueryFeatureSource(entry);
	}
	
}
