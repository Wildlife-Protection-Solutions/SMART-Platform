/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.model.map;

import java.io.IOException;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.model.QaError;

/**
 * Feature Source for providing features for QaError objects
 * @author Emily
 *
 */
public class QaErrorFeatureSource  extends ContentFeatureSource {
	
	private QaErrorMemoryDatastore dataStore;
	
	public QaErrorFeatureSource(ContentEntry entry, QaErrorMemoryDatastore dataStore) {
		super(entry, null);
		this.dataStore = dataStore;
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		return dataStore.getFeatureType(entry.getTypeName());
	}
  
	@Override
	protected ReferencedEnvelope getBoundsInternal(Query arg0)
			throws IOException {
		ReferencedEnvelope env = null;
		for(QaError error : dataStore.getErrors()){
			if (QaErrorMemoryDatastore.isValid(error, getSchema())){
				if (env == null){
					env = new ReferencedEnvelope(error.getGeometryObject().getEnvelopeInternal(), SmartDB.DATABASE_CRS);
				}else{
					env.expandToInclude(error.getGeometryObject().getEnvelopeInternal());
				}
			}
		}
		return env;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		int cnt = 0;
		for(QaError error : dataStore.getErrors()){
			if (QaErrorMemoryDatastore.isValid(error, getSchema())){
				cnt++;
			}
		}
		return cnt;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		QaErrorFeatureReader reader = new QaErrorFeatureReader(dataStore.getErrors(), getSchema());
		return reader;
	}
}
