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
package org.wcs.smart.er.query.map;

import java.io.IOException;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.map.QueryDataSource;
import org.wcs.smart.query.map.QueryFeatureSource;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryFeatureSource extends QueryFeatureSource{


	public SurveyQueryFeatureSource(ContentEntry entry) {
		super(entry);
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		if (SurveyQueryDataSource.isMissionTrack(entry.getName())) {
			return new MissionFeatureReader( (SimpleQuery)((QueryDataSource)entry.getDataStore()).getQuery(), getSchema());
		}
		return super.getReaderInternal(query);	
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		if (SurveyQueryDataSource.isMissionTrack(entry.getName())) {
			try {
				SimpleFeatureType type = 
						DataUtilities.createType(entry.getTypeName(), 
								getMissionTrackFeatureSchemaDef());
				return type;
			}catch (SchemaException ex ) {
				throw new IOException(ex);
			}
		}
		return super.buildFeatureType();
	}
	
	
	
	public static String getMissionTrackFeatureSchemaDef(){
		StringBuilder sb = new StringBuilder();
		sb.append("geom:MultiLineString:srid=4326,"); //$NON-NLS-1$
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("id:String,"); //$NON-NLS-1$
		sb.append("start:Date,"); //$NON-NLS-1$
		sb.append("end:Date,"); //$NON-NLS-1$
		sb.append("comment:String"); //$NON-NLS-1$
		return sb.toString();
	}
}
