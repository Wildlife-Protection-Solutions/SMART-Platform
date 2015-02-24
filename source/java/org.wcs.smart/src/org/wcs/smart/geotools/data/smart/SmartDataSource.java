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
package org.wcs.smart.geotools.data.smart;

import java.io.IOException;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;

/**
 * Geotools data store for SMART area layers.
 * @author Emily
 * @since 1.0.0
 */
public class SmartDataSource extends AbstractDataStore{

	private ConservationArea ca = null;

	public SmartDataSource(ConservationArea ca){
		this.ca = ca;
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
		String[] types = new String[Area.AreaType.values().length];
		for (int i = 0; i < Area.AreaType.values().length; i ++){
			types[i] = Area.AreaType.values()[i].name();
		}
		return types;
	}
	/* (non-Javadoc)
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		return new SmartFeatureReader(ca, Area.AreaType.valueOf(typeName), getSchema(typeName));
	}

	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		String spec = "the_geom:MultiPolygon:srid=4326,fid:String,name:String,key:String,uuid:String"; //$NON-NLS-1$
		try {
			SimpleFeatureType type =  DataUtilities.createType("smart." + typeName, spec); //$NON-NLS-1$
			return type;
		} catch (SchemaException e) {
			SmartPlugIn.log(e.getMessage(), e);
		}
		return null;
	}

	
}
