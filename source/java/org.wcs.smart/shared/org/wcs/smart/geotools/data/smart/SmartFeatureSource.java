package org.wcs.smart.geotools.data.smart;

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
import org.wcs.smart.ca.Area;

public class SmartFeatureSource  extends ContentFeatureSource {

	private Area.AreaType atype;
	
	
	public SmartFeatureSource(ContentEntry entry, Area.AreaType type) {
		super(entry, null);
		this.atype = type;
		
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		String spec = "the_geom:MultiPolygon:srid=4326,fid:String,name:String,key:String,uuid:String"; //$NON-NLS-1$
		try {
			SimpleFeatureType type =  DataUtilities.createType("smart." + atype.name(), spec); //$NON-NLS-1$
			return type;
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
		
		try{
			return new SmartFeatureReader(((SmartDataSource)entry.getDataStore()).getConservationArea(), atype,getSchema(),((SmartDataSource)entry.getDataStore()).getDbConnection());
		}catch (Exception ex){
			throw new IOException (ex);
		}
	}

}

