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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.locationtech.jts.io.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.udig.IntelObservationAttributeFeatureFactory;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.util.UuidUtils;

/**
 * Entity record feature reader
 * @author Emily
 */
public class IntelRecordFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<?> fIterator= null;
	
	private LocationLayerType type;
	private String attributeKey;
	private Attribute.AttributeType attributeType;
	
	public IntelRecordFeatureReader(SimpleFeatureType ftype) {
		this.ftype = ftype;
		if (IntelRecordDataSource.isGeometryAttribute(ftype.getName())) {
			this.type = null;
			
			this.attributeKey = IntelRecordDataSource.getAttributeKeyFromType(ftype.getName());
			this.attributeType = IntelRecordDataSource.getAttributeTypeFromType(ftype.getName());
			
		}else {
			this.type = LocationLayerType.valueOf(ftype.getName().getLocalPart().substring(ftype.getName().getLocalPart().lastIndexOf(";") + 1)); //$NON-NLS-1$

			this.attributeKey = null;
			this.attributeType = null;
		}
		
	}
	public IntelRecordFeatureReader(IntelRecord record, SimpleFeatureType ftype) {
		this(ftype);
		this.initData(record, record.getUuid());		
	}
	
	public IntelRecordFeatureReader(UUID recordUuid, SimpleFeatureType ftype) {
		this(ftype);
		this.initData(null, recordUuid);	
	}
	
	private void initData(IntelRecord record, UUID recordUuid) {
		if (type != null && (type == LocationLayerType.POINT || type == LocationLayerType.POLYGON)) {
			Session session = null;
			try {
				if (record == null) {
					session = HibernateManager.openSession();
					record = session.get(IntelRecord.class, recordUuid);
				}
				ArrayList<IntelLocation> locations = new ArrayList<IntelLocation>();
				if (record.getLocations() != null){
					for (IntelLocation location : record.getLocations()){
						if (( location.isPoint() && type == LocationLayerType.POINT )||
								( location.isPolygon() && type == LocationLayerType.POLYGON)){
							locations.add(location);
						}
					}
				}
				fIterator = locations.iterator();
			}finally {
				if (session != null) session.close();
			}
			
		}else if (type == null && this.attributeKey != null) {
			Session session = null;
			try {
				if (record == null) {
					session = HibernateManager.openSession();
					record = session.get(IntelRecord.class, recordUuid);
				}
				List<IntelObservationAttribute> items = new ArrayList<>();
				for (IntelLocation l : record.getLocations()) {
					for (IntelObservation obs : l.getObservations()) {
						for (IntelObservationAttribute a : obs.getObservationAttributes()) {
							if (!a.getAttribute().getKeyId().equals(this.attributeKey)) continue;
							if (a.getGeom() == null || !a.getAttribute().getType().isGeometry()) continue;
							
							if (session != null) {
								Hibernate.initialize(a.getObservation());
								Hibernate.initialize(a.getObservation().getCategory());
							}
							items.add(a);
						}
					}
				}
				fIterator = items.iterator();
			}finally {
				if (session != null) session.close();
			}
		}
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
		return getIntelLocationAsFeature(fIterator.next());
	}
	
	private SimpleFeature getIntelLocationAsFeature(Object object){
		
		if (this.attributeKey != null) {
			boolean hasPolygon = this.attributeType == AttributeType.POLYGON;
			return IntelObservationAttributeFeatureFactory.getObservationAttributeAsGeometry(this.ftype, hasPolygon, (IntelObservationAttribute)object);
		}else {
			IntelLocation location = (IntelLocation) object;
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
	
}
