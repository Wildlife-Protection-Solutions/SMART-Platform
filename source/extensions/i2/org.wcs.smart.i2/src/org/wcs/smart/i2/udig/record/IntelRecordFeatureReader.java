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
package org.wcs.smart.i2.udig.record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.io.ParseException;

/**
 * Entity record feature reader
 * @author Emily
 */
public class IntelRecordFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<IntelLocation> fIterator= null;
	
	public IntelRecordFeatureReader(IntelRecord record, SimpleFeatureType ftype) {
		this.ftype = ftype;
		LocationLayerType geomType = LocationLayerType.valueOf(ftype.getName().getLocalPart());
		ArrayList<IntelLocation> locations = new ArrayList<IntelLocation>();
		
		if (record.getLocations() != null){
			for (IntelLocation location : record.getLocations()){
				if (( location.isPoint() && geomType == LocationLayerType.POINT )||
					( location.isPolygon() && geomType == LocationLayerType.POLYGON)){
					locations.add(location);
				}
			}
		}
				
		fIterator = locations.iterator();
	}
	
	public IntelRecordFeatureReader(UUID recordUuid, SimpleFeatureType ftype) {
		this.ftype = ftype;
		LocationLayerType geomType = LocationLayerType.valueOf(ftype.getName().getLocalPart());
		ArrayList<IntelLocation> locations = new ArrayList<IntelLocation>();
		
		Session s = HibernateManager.openSession();
		try{
			IntelRecord record = (IntelRecord) s.get(IntelRecord.class, recordUuid);
			if (record != null && record.getLocations() != null){
				for (IntelLocation location : record.getLocations()){
					if (( location.isPoint() && geomType == LocationLayerType.POINT )||
						( location.isPolygon() && geomType == LocationLayerType.POLYGON)){
						locations.add(location);
					}
				}
			}
		}finally{
			s.close();
		}		
		fIterator = locations.iterator();
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
		return IntelRecordFeatureReader.getIntelLocationAsFeature(fIterator.next(), ftype);
	}
	
	private static SimpleFeature getIntelLocationAsFeature(IntelLocation location, SimpleFeatureType ftype){
		Object data[] = new Object[7];
		try {
			data[0] = location.getGeometry();
		} catch (ParseException e) {
			Logger.getLogger(IntelRecordFeatureReader.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		data[1] = location.getFeatureId(); 
		data[2] = location.getId();
		data[3] = location.getDateTime();
		data[4] = location.getDateTime();
		data[5] = location.getComment();
		data[6] = UuidUtils.uuidToString(location.getUuid());
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
}
