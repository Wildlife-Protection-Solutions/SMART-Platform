/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.udig.record;

import java.io.IOException;
import java.util.UUID;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.i2.udig.LocationLayerType;

/**
 * Records location feature source
 * @author Emily
 *
 */
public class IntelRecordFeatureSource extends ContentFeatureSource {

	private UUID recordUuid;
	
	
	public IntelRecordFeatureSource(ContentEntry entry, UUID recordUuid) {
		super(entry, null);
		this.recordUuid = recordUuid;
	}

	public static String getFeatureSchemaString(LocationLayerType geomType){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:");
		sb.append(geomType.getGeomType());
		sb.append(":srid=4326,fid:String,id:String,date:Date,time:Date,comment:String,system_id:String");
		return sb.toString();
	}
	
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			LocationLayerType geomType = LocationLayerType.valueOf(entry.getName().getLocalPart());	
			return DataUtilities.createType(entry.getTypeName(), getFeatureSchemaString(geomType));
		} catch (SchemaException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected ReferencedEnvelope getBoundsInternal(Query arg0)
			throws IOException {
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		//return record.getLocations().size();
		//TODO:
		return -1;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(
			Query arg0) throws IOException {
		return new IntelRecordFeatureReader(recordUuid,  getSchema());
	}

}
