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
package org.wcs.smart.i2.udig.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.io.ParseException;

/**
 * Smart entity location feature reader
 * 
 * @author Emily
 */
public class IntelEntityFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<IntelLocation> fIterator= null;
	
	public IntelEntityFeatureReader(UUID entityUuid, SimpleFeatureType ftype, Date[] dFilter) {
		this.ftype = ftype;
		LocationLayerType geomType = LocationLayerType.valueOf(ftype.getName().getLocalPart());
		ArrayList<IntelLocation> locations = new ArrayList<IntelLocation>();
		
		Session s = HibernateManager.openSession();
		try{
			List<IntelEntityLocation> alllocations = EntityManager.INSTANCE.getEntityLocations(s, entityUuid, dFilter);
			
			for (IntelEntityLocation location : alllocations){
				if (( location.getLocation().isPoint() && geomType == LocationLayerType.POINT )||
						( location.getLocation().isPolygon() && geomType == LocationLayerType.POLYGON)){
					locations.add(location.getLocation());
					location.getLocation().getRecord().getTitle();
				}
			}
		}finally{
			s.close();
		}		
		fIterator = locations.iterator();
	}
	
	public IntelEntityFeatureReader(UUID entityUuid, SimpleFeatureType ftype) {
		this(entityUuid, ftype, null);
	}
	

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#getFeatureType()
	 */
	@Override
	public SimpleFeatureType getFeatureType() {
		return ftype;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#hasNext()
	 */
	@Override
	public boolean hasNext() throws IOException {
		if (fIterator == null) return false;
		return fIterator.hasNext();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {		
		return IntelEntityFeatureReader.getIntelLocationAsFeature(fIterator.next(), ftype);
	}
	
	private static SimpleFeature getIntelLocationAsFeature(IntelLocation location, SimpleFeatureType ftype){
		//sb.append(":srid=4326,fid:String,id:String,date:Date,time:Date,comment:String,record:String,record_date:Date,record_time:Date,record_uuid:String,system_id:String");
		Object data[] = new Object[9];
		
		try {
			data[0] = location.getGeometry();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		data[1] = ftype.getName() + "." + location.getId() + "." + UuidUtils.uuidToString(location.getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		data[2] = location.getId();
		data[3] = location.getDateTime();
		data[4] = location.getComment();
		data[5] = location.getRecord().getTitle();
		data[6] = location.getRecord().getDateCreated();
		data[7] = UuidUtils.uuidToString(location.getRecord().getUuid());
		data[8] = UuidUtils.uuidToString(location.getUuid());
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
}