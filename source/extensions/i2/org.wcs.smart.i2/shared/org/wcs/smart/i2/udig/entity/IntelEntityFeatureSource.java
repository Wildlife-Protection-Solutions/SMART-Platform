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

public class IntelEntityFeatureSource extends ContentFeatureSource {

	private UUID entityUuid;
	
	public IntelEntityFeatureSource(ContentEntry entry, UUID entityUuid) {
		super(entry, null);
		this.entityUuid = entityUuid;
		
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
//		return record.getLocations().size();
		return -1;
		//TODO:
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		
		//do something special for single between dates filter
		IntelEntityFeatureReader[] reader = new IntelEntityFeatureReader[]{new IntelEntityFeatureReader(entityUuid, getSchema())};
		
		query.getFilter().accept(new AbstractFilterVisitor() {
			@Override
			public Object visit(PropertyIsBetween filter, Object object) {
				PropertyIsBetween betweenFilter = (PropertyIsBetween) filter;
				if (betweenFilter.getExpression() instanceof PropertyName && (((PropertyName)betweenFilter.getExpression()).getPropertyName().equalsIgnoreCase("date"))){
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
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:");
		sb.append(geomType.getGeomType());
		sb.append(":srid=4326,fid:String,id:String,date:Date,comment:String,record:String,record_date:Date,record_uuid:String,system_id:String");
		return sb.toString();
	}
}
