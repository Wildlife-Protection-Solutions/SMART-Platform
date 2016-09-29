package org.wcs.smart.i2.udig.entity;

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
import org.wcs.smart.i2.udig.IntelRecordDataSource;
import org.wcs.smart.i2.udig.IntelRecordDataSource.Type;
import org.wcs.smart.util.UuidUtils;

public class IntelEntityFeatureSource extends ContentFeatureSource {

	private UUID entityUuid;
	
	public IntelEntityFeatureSource(ContentEntry entry, UUID entityUuid) {
		super(entry, null);
		this.entityUuid = entityUuid;
		
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			IntelRecordDataSource.Type geomType = IntelRecordDataSource.Type.valueOf(entry.getName().getLocalPart());
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
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(
			Query arg0) throws IOException {
		return new IntelEntityFeatureReader(entityUuid, getSchema(), ((IntelEntityDataSource)entry.getDataStore()).getDateFilter());
	}

	
	public static String getFeatureSchemaString(Type geomType){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:");
		sb.append(geomType.getGeomType());
		sb.append(":srid=4326,fid:String,id:String,date:Date,comment:String,record:String,record_date:Date,record_uuid:String,system_id:String");
		return sb.toString();
	}
}
