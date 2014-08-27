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
package org.wcs.smart.er.ui.mision.udig;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;

/**
 * Data source for mission record.
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionDataSource extends AbstractDataStore{

	public static final String MISSION_TYPE = "MissionPoint"; //$NON-NLS-1$
	
	private Mission mission;
	private HashMap<String, SimpleFeatureType> schemas = new HashMap<String, SimpleFeatureType>();
	
	public MissionDataSource(Mission mission){
		this.mission = mission;
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		return new String[]{MISSION_TYPE};
	}
	/* (non-Javadoc)
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		return new MissionFeatureReader(this.mission, getSchema(typeName));
	}

	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		SimpleFeatureType type = schemas.get(typeName);
		if (type == null){
			try {
				if (typeName.equals(MISSION_TYPE)) {
					type = createPointSchema();
				}
			}catch(SchemaException ex){
				throw new IOException("Schema type not supported." + ex.getLocalizedMessage(), ex);
			}
			schemas.put(typeName, type);
		}
		return type;
	}

	private SimpleFeatureType createPointSchema() throws SchemaException{
		String spec = "fid:String,geom:Point:srid=4326"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType("smart." + MISSION_TYPE, spec); //$NON-NLS-1$
		return type;
	}
}
