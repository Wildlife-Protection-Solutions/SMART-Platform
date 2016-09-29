package org.wcs.smart.i2.udig;

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
import org.opengis.feature.type.Name;
import org.wcs.smart.i2.udig.IntelRecordDataSource.Type;

public class IntelRecordFeatureSource extends ContentFeatureSource {

	private UUID recordUuid;
	
	
	public IntelRecordFeatureSource(ContentEntry entry, UUID recordUuid) {
		super(entry, null);
		this.recordUuid = recordUuid;
	}

	public static String getFeatureSchemaString(Type geomType){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:");
		sb.append(geomType.getGeomType());
		sb.append(":srid=4326,fid:String,id:String,date:Date,time:Date,comment:String,system_id:String");
		return sb.toString();
	}
	
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			IntelRecordDataSource.Type geomType = IntelRecordDataSource.Type.valueOf(entry.getName().getLocalPart());	
			Name name = IntelRecordDataSource.generateName(geomType, recordUuid);
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
