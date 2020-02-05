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
package org.wcs.smart.i2.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;

/**
 * Spatial search
 * 
 * @author Emily
 *
 */
public class SpatialEntitySearch implements IIntelEntitySearch {

	public static final String ATTRIBUTE_SEPERATOR = ":"; //$NON-NLS-1$
	
	private IntelEntitySearch search;
	private UUID recordUuid;
	private Geometry customGeom;
	private ConservationArea ca;
	
	public SpatialEntitySearch(IntelEntitySearch search, UUID recordUuid) {
		this.search = search;
		this.recordUuid = recordUuid;
		
		if (search.getType() != IntelEntitySearch.Type.RECORD) {
			throw new IllegalStateException("Not a valid record search object"); //$NON-NLS-1$
		}
	}
	
	public SpatialEntitySearch(IntelEntitySearch search, Geometry customPoint, ConservationArea ca) {
		this.search = search;
		this.customGeom = customPoint;
		this.ca = ca;
		
		if (search.getType() != IntelEntitySearch.Type.RECORD) {
			throw new IllegalStateException("Not a valid record search object"); //$NON-NLS-1$
		}
	}
	
	@Override
	public IntelSearchResult doSearch(Set<IntelProfile> profiles, Session session, Locale locale, IProgressMonitor monitor) throws Exception {
		if (profiles.isEmpty()) return new IntelSearchResult(Collections.emptyList(),0);
		
		//find active intel record editor
		IntelRecord record = null;
		if (this.ca == null && recordUuid != null) {
			record = session.get(IntelRecord.class, recordUuid);
			if (record != null) this.ca = record.getConservationArea();
		}
		if (this.ca == null) throw new IllegalStateException("Conservation area not found."); //$NON-NLS-1$
		
		
		String items[] = search.getSearchString().split(IntelEntitySearch.SEPARATOR);
		//parse distance
		Double maxDistance = 0.0;
		try {
			maxDistance = Double.parseDouble( items[1] );
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format("Unable to part distance.  Cannot parse number from: {0}", items[1])); //$NON-NLS-1$
		}
		//parse the attributes
		List<IntelEntityTypeAttribute> attributes = new ArrayList<>();
		if (items.length > 2) {
			for (int i = 2; i < items.length; i ++) {
				String[] bits = items[i].split(ATTRIBUTE_SEPERATOR);
				String entityTypeKey = bits[0];
				String attributeTypeKey = bits[1];
				
				Query<IntelEntityTypeAttribute> q = session.createQuery("FROM IntelEntityTypeAttribute WHERE id.attribute.keyId = :attribute and id.entityType.keyId = :entity and id.attribute.conservationArea = :ca", IntelEntityTypeAttribute.class); //$NON-NLS-1$
				q.setParameter("attribute",  attributeTypeKey); //$NON-NLS-1$
				q.setParameter("entity", entityTypeKey); //$NON-NLS-1$
				q.setParameter("ca", ca); //$NON-NLS-1$
				IntelEntityTypeAttribute attribute = (IntelEntityTypeAttribute) q.uniqueResult();
				if (attribute != null) attributes.add(attribute);
			}
		}else {
			//search all 
			Query<IntelEntityTypeAttribute> q = session.createQuery("FROM IntelEntityTypeAttribute WHERE id.attribute.conservationArea = :ca", IntelEntityTypeAttribute.class); //$NON-NLS-1$
			q.setParameter("ca", ca); //$NON-NLS-1$
			attributes.addAll(q.list());
		}
		
		List<IntelEntityAttributeValue> valuesToSearch = new ArrayList<>();
		for (IntelEntityTypeAttribute attribute : attributes) {
			Query<IntelEntityAttributeValue> values = session.createQuery("FROM IntelEntityAttributeValue WHERE id.entity.profile in (:profiles) AND id.attribute = :attribute and id.entity.entityType = :type ", IntelEntityAttributeValue.class); //$NON-NLS-1$
			values.setParameter("attribute", attribute.getAttribute()); //$NON-NLS-1$
			values.setParameter("type", attribute.getEntityType()); //$NON-NLS-1$
			values.setParameter("profiles", profiles); //$NON-NLS-1$
			valuesToSearch.addAll(values.list());
		}
		
		
		HashMap<IntelEntity, Double> results = new HashMap<IntelEntity, Double>();
		if (this.recordUuid != null) {
			if (record == null) {
				throw new Exception("Record not found"); //$NON-NLS-1$
			}
			if (record.getLocations() == null) {
				throw new Exception(MessageFormat.format("The record {0} has not locations to search", record.getTitle())); //$NON-NLS-1$
			}
			
			for (IntelLocation location : record.getLocations()) {
				Geometry g = location.getGeometry();
				processGeometry(results, g, valuesToSearch, maxDistance);
			}
		}else if (customGeom != null && customGeom instanceof Point) {
			Point p = ((Point)customGeom);
			processGeometry(results, p, valuesToSearch, maxDistance);
		}else {
			throw new Exception("Record search not supported."); //$NON-NLS-1$
		}
		
		List<IntelSearchResultItem> results2 = new ArrayList<>();
		for (Entry<IntelEntity, Double> searches : results.entrySet()) {
			IntelSearchResultItem item = new IntelSearchResultItem(searches.getKey().getUuid(), searches.getValue().doubleValue(), MessageFormat.format("{0} m", searches.getValue().doubleValue())); //$NON-NLS-1$
			results2.add(item);
		}
		//reverse sort as rating is distance
		results2.sort((a,b)->a.getRating().compareTo(b.getRating()));
		return new IntelSearchResult(results2, 0);
	}
	
	private void processGeometry(HashMap<IntelEntity, Double> results, Geometry geometry,  List<IntelEntityAttributeValue> valuesToSearch, Double maxDistance) throws TransformException {
		if (geometry instanceof Point) {
			processPoint(results, (Point)geometry, valuesToSearch, maxDistance);
		}else {
			processOtherGeometry(results, geometry, valuesToSearch, maxDistance);
		}
	}
	
	private void processPoint(HashMap<IntelEntity, Double> results, Point geometry,  List<IntelEntityAttributeValue> valuesToSearch, Double maxDistance) throws TransformException {
		Coordinate locationc = ((Point)geometry).getCoordinate();
		for (IntelEntityAttributeValue value : valuesToSearch) {
			if (value.getAttribute().getType() != AttributeType.POSITION) continue;
			if (value.getNumberValue() != null && value.getNumberValue2() != null) {
				Coordinate vc = new Coordinate(value.getNumberValue(), value.getNumberValue2());
				try {
					double distance = JTS.orthodromicDistance(locationc, vc, GeometryUtils.SMART_CRS);
					if (distance <= maxDistance) {
						Double d = results.get(value.getEntity());
						if (d == null) {
							d = distance;
						}else if (distance < d) {
							d = distance;
						}
						results.put(value.getEntity(), d);				
					}
				}catch (Exception ex) {
					Logger.getLogger(SpatialEntitySearch.class.getName()).log(Level.INFO, ex.getMessage(), ex);
				}
			}
		}
	}

	private void processOtherGeometry(HashMap<IntelEntity, Double> results, Geometry geometry,  List<IntelEntityAttributeValue> valuesToSearch, Double maxDistance) throws TransformException {
		for (IntelEntityAttributeValue value : valuesToSearch) {
			if (value.getNumberValue() == null || value.getNumberValue2() == null) continue;
			
			Point vPnt = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(value.getNumberValue(), value.getNumberValue2()));
			Double distance = null;
			if (geometry.intersects(vPnt)) {
				distance = 0.0;
			}else {
				//potentially not accurate but should be close
				//find the closest points in lat/long then computes the ortho distance between them
				Coordinate[] closest = DistanceOp.nearestPoints(vPnt,  geometry);
				distance = JTS.orthodromicDistance(closest[0], closest[1], GeometryUtils.SMART_CRS);
			}
			
			Double d = results.get(value.getEntity());
			if (distance <= maxDistance) {
				if (d == null) {
					d = distance;
				}else if (distance < d) {
					d = distance;
				}
				results.put(value.getEntity(), d);
			}
		}
	}

		
	@Override
	public String serialize() {
		return search.getSearchString();
	}

}
