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
package org.wcs.smart.patrol.geotools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.locationtech.jts.io.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.TrackPart;

/**
 * Smart area feature reader
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private String thisType;
	private Iterator<?> fIterator;

	public PatrolFeatureReader(Patrol patrol,
			String typeName, SimpleFeatureType ftype) {
		this.ftype = ftype;
		this.thisType = typeName;
	
		if (thisType.equals(PatrolDataSource.WAYPOINT_TYPE) || 
				thisType.equals(PatrolDataSource.WAYPOINT_PRJ_TYPE)) {
			
			List<PatrolWaypoint> pnts = new ArrayList<PatrolWaypoint>();

			try(Session session = HibernateManager.openSession()){
				patrol = session.get(Patrol.class, patrol.getUuid());
				if (patrol != null) {
					for (PatrolLeg l : patrol.getLegs()){
						for (PatrolLegDay d : l.getPatrolLegDays()){
							if (d.getWaypoints() != null){
								pnts.addAll(d.getWaypoints());
							}
						}
					}
				}
				for (PatrolWaypoint pw : pnts) {
					Hibernate.initialize(pw.getWaypoint());
					pw.getWaypoint().getObservationsAsString();
				}
			}
			
			
			fIterator = pnts.iterator();
			
		
		}else if (thisType.equals(PatrolDataSource.TRACK_PART_TYPE)){
			List<TrackPart> pnts = new ArrayList<>();
			for (PatrolLeg l : patrol.getLegs()){
				for (PatrolLegDay d : l.getPatrolLegDays()){
					if (d.getTrack() != null){
						try {
							pnts.addAll(d.getTrack().getTrackParts());
						} catch (ParseException e) {
							SmartPatrolPlugIn.displayLog(e.getMessage(), e);
						}
					}
				}
			}
			fIterator = pnts.iterator();
		}else if (PatrolDataSource.isGeometryAttribute(thisType)) {
			
			AttributeType matching = AttributeType.LINE;
			if (PatrolDataSource.isPolgyonAttribute(thisType)) {
				matching = AttributeType.POLYGON;
			}
			
			String attributeKey = thisType.split("\\.")[1]; //$NON-NLS-1$
			try(Session session = HibernateManager.openSession()){
				
				patrol = session.get(Patrol.class, patrol.getUuid());
				
				List<WaypointObservationAttribute> attributes = new ArrayList<>();
				for (PatrolLeg l : patrol.getLegs()) {
					for (PatrolLegDay d : l.getPatrolLegDays()) {
						for (PatrolWaypoint pw:d.getWaypoints()) {
							for (WaypointObservation wo : pw.getWaypoint().getAllObservations()) {
								for (WaypointObservationAttribute a : wo.getAttributes()) {
									if (a.getAttribute().getKeyId().equals(attributeKey) &&
											a.getGeom() != null && a.getAttribute().getType() == matching) {
										attributes.add(a);
										a.getAttributeValueAsString(Locale.getDefault());
										a.getObservation().getCategory().getName();
									}
								}
							}
						}
					}
				}
				fIterator = attributes.iterator();
			}
		}else{
			fIterator = null;
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
		if (thisType.equals(PatrolDataSource.WAYPOINT_TYPE)){
			return new PatrolFeature(PatrolFeatureFactory.getWaypointAsFeature(ftype, (PatrolWaypoint)this.fIterator.next()));
		}else if (thisType.equals(PatrolDataSource.TRACK_PART_TYPE)){
			return new PatrolFeature(PatrolFeatureFactory.getTrackPartAsFeature(ftype, (TrackPart)this.fIterator.next()));
		}else if (thisType.equals(PatrolDataSource.WAYPOINT_PRJ_TYPE)) {
			return new PatrolFeature(PatrolFeatureFactory.getWaypointAsPrjFeature(ftype, (PatrolWaypoint)this.fIterator.next()));
		}else if (PatrolDataSource.isGeometryAttribute(thisType)) {
			return new PatrolFeature(PatrolFeatureFactory.getObservationAttributeAsGeometry(ftype, (WaypointObservationAttribute)this.fIterator.next() ));
		}
		return null;
	}
}
