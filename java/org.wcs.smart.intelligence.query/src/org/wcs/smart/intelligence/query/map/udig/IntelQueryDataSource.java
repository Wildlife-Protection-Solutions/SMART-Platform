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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.intelligence.query.internal.Messages;
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
public class IntelQueryDataSource extends AbstractDataStore{

	/**
	 * waypoint query data source
	 */
	public static final String INTEL_TYPE = "Intelligence";  //$NON-NLS-1$
	
	private IntelligenceRecordQuery query;
	
	private HashMap<String, SimpleFeatureType> schemas = new HashMap<String, SimpleFeatureType>();
	
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
	}

	/**
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		return new String[]{INTEL_TYPE};
	}
	
	
	/**
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		return new IntelQueryFeatureReader(this.query, getSchema(typeName));
	}

	/**
	 * @see org.geotools.data.AbstractDataStore#removeSchema(java.lang.String)
	 */
	@Override
	public void removeSchema(String typeName) throws IOException {
		schemas.remove(typeName);
	}
	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		SimpleFeatureType type = schemas.get(typeName);
		if (type == null){
			try {
				if (typeName.equals(INTEL_TYPE)) {
					type = createIntelligenceRecordSchema(query.getQueryColumns(Locale.getDefault(), null));
				} 
			}catch(SchemaException ex){
				throw new IOException(Messages.IntelQueryDataSource_SchemaError + ex.getLocalizedMessage(), ex);
			}
			schemas.put(typeName, type);
		}
		return type;
	}

	/**
	 * Creates the simple feature type for the intelligence record query
	 * from list of query columns included in the query.
	 * 
	 * @return the simple feature type for the query
	 * 
	 * @throws SchemaException
	 */
	public static SimpleFeatureType createIntelligenceRecordSchema(List<QueryColumn> columns) throws SchemaException{
		SimpleFeatureType type =  DataUtilities.createType("smart." + INTEL_TYPE, getFeatureSchemaDef(columns, true)); //$NON-NLS-1$
		return type;
	}
	
	
	/**
	 * Create feature definition string from query columns.
	 */
	private static String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:MultiPoint:srid=4326"); //$NON-NLS-1$
		sb.append(",fid:String"); //$NON-NLS-1$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime));
		return sb.toString();
	}
	
}
