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

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.query.EntitySightingQuery;
import org.wcs.smart.query.model.QueryColumn;
/**
 * Data source for entity sighting queries.
 * @author Emily
 *
 */
public class EntityQueryDataSource extends AbstractDataStore{

	public static final String TYPENAME = "ENTITY_QUERY"; //$NON-NLS-1$

	private SimpleFeatureType featureSchema;
	private EntitySightingQuery query;
	
	
	public EntityQueryDataSource(EntitySightingQuery query){
		this.query = query;
	}
	
	@Override
	public void dispose(){
		super.dispose();
	}

	public void refresh(EntitySightingQuery query){
		this.query = query;
		this.featureSchema = null;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		return new String[]{TYPENAME};
	}
	/* (non-Javadoc)
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		if (typeName.equals(TYPENAME)){
			return new EntityQueryDataSourceFeatureReader(query, getSchema(typeName));
		}
		return null;
	}

	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		try{
			if (typeName.equals(TYPENAME)){
				if (featureSchema==null){
					featureSchema = createQuerySchema(query.getQueryColumns(), true);
				}
				return featureSchema;
			}
			return null;
		}catch (SchemaException ex){
			throw new IOException(Messages.EntityQueryDataSource_SchemaError, ex);
		}	
	}
	
	public static SimpleFeatureType createQuerySchema(List<QueryColumn> columns, boolean supportsTime) throws SchemaException {
		StringBuilder sb = new StringBuilder();
		sb.append("fid:String"); //$NON-NLS-1$
		sb.append(QueryColumn.createFeatureDefinitionString(columns, supportsTime));
		sb.append(",geom:Point:srid=4326"); //$NON-NLS-1$
		
		SimpleFeatureType type =  DataUtilities.createType(TYPENAME, sb.toString()); 
		return type;
	}
}

