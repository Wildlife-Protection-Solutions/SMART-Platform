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
package org.wcs.smart.entity.fixed.map;

import java.io.IOException;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.entity.query.EntityQuery;
import org.wcs.smart.query.model.QueryColumn;
/**
 * Data source for entity sighting queries.
 * @author Emily
 *
 */
public class EntityQueryDataSource extends AbstractDataStore{

	public static final String TYPENAME = "ENTITY_QUERY"; //$NON-NLS-1$

	private SimpleFeatureType featureSchema;
	private EntityQuery query;
	
	
	public EntityQueryDataSource(EntityQuery query){
		this.query = query;
	}
	
	@Override
	public void dispose(){
		super.dispose();
	}

	public void refresh(EntityQuery query){
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
					featureSchema = createQuerySchema();
				}
				return featureSchema;
			}
			return null;
		}catch (SchemaException ex){
			throw new IOException("Could not generate feature schema for entity query.", ex);
		}	
	}
	
	private SimpleFeatureType createQuerySchema() throws SchemaException {
		StringBuilder sb = new StringBuilder();
		sb.append("fid:String"); //$NON-NLS-1$
		for (QueryColumn col : query.getQueryColumns()){
			sb.append(","); //$NON-NLS-1$
			sb.append(col.getName());
			sb.append(":"); //$NON-NLS-1$
			sb.append(col.getType().geotoolsType);
		}
		sb.append(",geom:Point:srid=4326"); //$NON-NLS-1$
		
		SimpleFeatureType type =  DataUtilities.createType(TYPENAME, sb.toString()); //$NON-NLS-1$
		return type;
	}
}

