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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.io.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * SMART entity location feature reader.  Reads features from
 * both the intel location and waypoint datasets.
 * 
 * @author Emily
 */
public class IntelEntityFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<?> fIterator= null;
	
	private LocationLayerType geomType;
	
	private Session session;
	
	public IntelEntityFeatureReader(UUID entityUuid, SimpleFeatureType ftype, LocalDateTime[] dFilter) {
		this.ftype = ftype;
		geomType = LocationLayerType.valueOf(ftype.getName().getLocalPart());
		session = HibernateManager.openSession();
		
		if(geomType == LocationLayerType.POINT || geomType == LocationLayerType.POLYGON) {
			
			ArrayList<IntelLocation> locations = new ArrayList<IntelLocation>();
			List<IntelEntityLocation> alllocations = EntityManager.INSTANCE.getEntityLocations(session, entityUuid, dFilter);
			
			for (IntelEntityLocation location : alllocations){
				if (( location.getLocation().isPoint() && geomType == LocationLayerType.POINT )||
						( location.getLocation().isPolygon() && geomType == LocationLayerType.POLYGON)){
					locations.add(location.getLocation());
				}
			}
			fIterator = locations.iterator();
		}else if (geomType == LocationLayerType.DM_OBS) {
			AttributeListItem item = session.get(IntelEntity.class,entityUuid).getDmAttributeListItem();
			if (item != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT DISTINCT wp FROM Waypoint wp "); //$NON-NLS-1$
				sb.append("JOIN wp.observationGroups grp JOIN grp.observations o "); //$NON-NLS-1$
				sb.append("JOIN o.attributes a WHERE a.attributeListItem = :li "); //$NON-NLS-1$
				
				boolean hasDate = dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null;

				if (hasDate) {
					sb.append(" AND wp.dateTime BETWEEN :d1 AND :d2 "); //$NON-NLS-1$
				}
				
				Query<Waypoint> query = session.createQuery(sb.toString(), Waypoint.class);
				query.setParameter("li", item); //$NON-NLS-1$
				if (hasDate) {
					query.setParameter("d1",  dFilter[0]); //$NON-NLS-1$
					query.setParameter("d2",  dFilter[1]); //$NON-NLS-1$
				}
				
				fIterator = query.list().iterator();
			}
		}
		
	}
	
	public IntelEntityFeatureReader(UUID entityUuid, SimpleFeatureType ftype) {
		this(entityUuid, ftype, null);
	}
	

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		session.close();
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
		if (geomType == LocationLayerType.POINT || geomType == LocationLayerType.POLYGON) {
			return IntelEntityFeatureReader.getIntelLocationAsFeature((IntelLocation)fIterator.next(), ftype);
		}else if (geomType == LocationLayerType.DM_OBS) {
			return IntelEntityFeatureReader.getWaypointAsFeature((Waypoint)fIterator.next(), ftype, session);
		}
		return null;
	}
	
	private static SimpleFeature getIntelLocationAsFeature(IntelLocation location, SimpleFeatureType ftype){
		Object data[] = new Object[9];		
		try {
			data[0] = location.getGeometry();
		} catch (ParseException e) {
			Logger.getLogger(IntelEntityFeatureReader.class.getName()).log(Level.INFO, e.getMessage(), e);
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
	
	private static SimpleFeature getWaypointAsFeature(Waypoint wp, SimpleFeatureType ftype, Session session) {
		
		Object data[] = new Object[7];		
		data[0] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(wp.getX(), wp.getY()));
		data[1] = ftype.getName() + "." + wp.getId() + "." + UuidUtils.uuidToString(wp.getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		data[2] = wp.getId();
		data[3] = wp.getDateTime();
		data[4] = WaypointSourceEngine.INSTANCE.getSource( wp.getSourceId() );
		data[5] = WaypointSourceEngine.INSTANCE.getSource( wp.getSourceId() ).getSourceLabel(wp, session, Locale.getDefault());
		data[6] = UuidUtils.uuidToString(wp.getUuid());
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);

	}
}