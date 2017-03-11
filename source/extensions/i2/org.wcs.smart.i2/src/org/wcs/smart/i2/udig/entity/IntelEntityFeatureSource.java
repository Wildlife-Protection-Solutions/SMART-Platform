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
package org.wcs.smart.i2.udig.entity;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.filter.visitor.AbstractFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.expression.PropertyName;
import org.wcs.smart.i2.udig.LocationLayerType;

/**
 * Feature source for entity locations
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class IntelEntityFeatureSource extends ContentFeatureSource {

	private UUID entityUuid;
	private LocationLayerType geomType = null;
	
	public IntelEntityFeatureSource(ContentEntry entry, UUID entityUuid) {
		super(entry, null);
		this.entityUuid = entityUuid;
		geomType = LocationLayerType.valueOf(entry.getName().getLocalPart());
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {

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
		return -1;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		//do something special for single between dates filter
		if (geomType == LocationLayerType.ATTRIBUTE){
			try{
				return new IntelEntityAttributeFeatureReader(entityUuid, getSchema());
			}catch (Exception ex){
				throw new IOException (ex);
			}
		}
		
		IntelEntityFeatureReader[] reader = new IntelEntityFeatureReader[]{new IntelEntityFeatureReader(entityUuid, getSchema())};
		
		
		query.getFilter().accept(new AbstractFilterVisitor() {
			@Override
			public Object visit(PropertyIsBetween filter, Object object) {
				PropertyIsBetween betweenFilter = (PropertyIsBetween) filter;
				if (betweenFilter.getExpression() instanceof PropertyName && (((PropertyName)betweenFilter.getExpression()).getPropertyName().equalsIgnoreCase("date"))){ //$NON-NLS-1$
					Date startDate = (Date) betweenFilter.getLowerBoundary().evaluate(null);
					Date endDate = (Date) betweenFilter.getUpperBoundary().evaluate(null);
					if (startDate != null && endDate != null){
						reader[0] =  new IntelEntityFeatureReader(entityUuid, getSchema(), new Date[]{startDate, endDate});			
					}
				}
				return null;
			}
			
		}, null);
		
		return reader[0];
	}

	
	public static String getFeatureSchemaString(LocationLayerType geomType){
		switch(geomType){
			case POINT:
			case POLYGON:
				StringBuilder sb = new StringBuilder();
				sb.append("the_geom:"); //$NON-NLS-1$
				sb.append(geomType.getGeomType());
				sb.append(":srid=4326,fid:String,id:String,date:Date,comment:String,record:String,record_date:Date,record_uuid:String,system_id:String"); //$NON-NLS-1$
				return sb.toString();
			case ATTRIBUTE:
				sb = new StringBuilder();
				sb.append("the_geom:"); //$NON-NLS-1$
				sb.append(geomType.getGeomType());
				sb.append(":srid=4326,fid:String,attribute:String"); //$NON-NLS-1$
				return sb.toString();
		}
		return null;
	}
}
