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
package org.wcs.smart.intelligence.map;

import java.io.IOException;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Data source for intelligence record.
 * 
 * @author Emily
 *
 */
public class IntelligenceFeatureSource extends ContentFeatureSource{

	public IntelligenceFeatureSource(ContentEntry entry) {
		super(entry, Query.ALL);
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
		return new IntelligenceFeatureReader(((IntelligenceDataSource)entry.getDataStore()).getIntelligence(), getSchema());
	}
	
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			if (entry.getTypeName().equals(IntelligenceDataSource.INTEL_TYPE)) {
				return createPointSchema();
			}
		}catch(SchemaException ex){
			throw new IOException(Messages.IntelligenceDataSource_SchemaNotSupported + ex.getLocalizedMessage(), ex);
		}
		return null;
	}
	
	private SimpleFeatureType createPointSchema() throws SchemaException{
		String spec = "the_geom:Point:srid=4326,fid:String"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType("smart." + IntelligenceDataSource.INTEL_TYPE, spec); //$NON-NLS-1$
		return type;
	}
}
