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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.hibernate.Session;
import org.opengis.feature.type.Name;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.LabelConstants;

/**
 * Geotools data store for SMART area layers.
 * @author Emily
 * @since 1.0.0
 */
public class PatrolDataSource extends ContentDataStore{

	public static final String WAYPOINT_TYPE = "Waypoint"; //$NON-NLS-1$
	public static final String TRACK_PART_TYPE = "Track"; //$NON-NLS-1$
	public static final String WAYPOINT_PRJ_TYPE = "WaypointRawPoints"; //$NON-NLS-1$
	
	private Patrol patrol;
	private Map<String,Attribute> attributeMap;
	
	private static final String[] ALL_NAMES = {
			WAYPOINT_TYPE, 
			TRACK_PART_TYPE
	};
	
	public PatrolDataSource(Patrol patrol){
		this.patrol = patrol;
	}

	/**
	 * updates the patrol object associated with the data source
	 * @param patrol
	 */
	public void updatePatrol(Patrol patrol){
		this.patrol = patrol;
	}

	public Patrol getPatrol() {
		return this.patrol;
	}
	
	@Override
	protected List<Name> createTypeNames() throws IOException {
		boolean dd = false;
		
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		attributeMap = new HashMap<>();
		List<Name> names = new ArrayList<Name>();
		for (String name : ALL_NAMES) {
			names.add(new NameImpl(name));
		}
		
		try (Session session = HibernateManager.openSession()){
			dd = ObservationHibernateManager.getPatrolOptions(ca, session).getTrackDistanceDirection();
			
			//find all geometry based data model
			List<Attribute> attributes = DataModelManager.INSTANCE.getGeometryAttributes(session);
			for (Attribute attribute : attributes) {
				String type = attribute.getType().name() + "." + attribute.getKeyId(); //$NON-NLS-1$
				names.add(new NameImpl(type)); 
				attributeMap.put(type, attribute);
			}	
		}
		
		if (dd) {
			names.add(new NameImpl(WAYPOINT_PRJ_TYPE));
		}
		return names;
	}
	

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new PatrolFeatureSource(entry);
	}
	
	public static boolean isLineAttribute(String name) {
		return (name.startsWith(Attribute.AttributeType.LINE.name() + ".")); //$NON-NLS-1$
	}
	public static boolean isPolgyonAttribute(String name) {
		return (name.startsWith(Attribute.AttributeType.POLYGON.name() + ".")); //$NON-NLS-1$
	}
	public static boolean isGeometryAttribute(String name) {
		return  (name.startsWith(Attribute.AttributeType.POLYGON.name() + ".") || //$NON-NLS-1$
				name.startsWith(Attribute.AttributeType.LINE.name() + ".")); //$NON-NLS-1$
	}

	public String getName(String typeName) {
		if (typeName.equals(PatrolDataSource.TRACK_PART_TYPE)) return LabelConstants.TRACKPOINTS;
		if (typeName.equals(PatrolDataSource.WAYPOINT_TYPE)) return Messages.PatrolFeatureSource_WaypointLayerName;
		if (typeName.equals(PatrolDataSource.WAYPOINT_PRJ_TYPE)) return Messages.PatrolFeatureSource_ProjectedWaypointLayerName;
		if (isGeometryAttribute(typeName)) {
			return attributeMap.get(typeName).getName();
		}
		return null;
	}
	
	public Attribute getAttribute(String typeName) {
		return attributeMap.get(typeName);
	}
}
