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
package org.wcs.smart.er.ui.mision.udig;

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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Data source for mission observations and tracks.
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionDataSource extends ContentDataStore{

	public static final String MISSIONWAYPOINT_TYPE = "MissionPoint"; //$NON-NLS-1$
	public static final String MISSIONTRACK_TYPE = "MissionTrack"; //$NON-NLS-1$
	public static final String MISSIONRAWWAYPOINT_TYPE = "MissionPointRaw"; //$NON-NLS-1$
		
	private MissionService service;
	private Map<String,Attribute> attributeMap;

	public MissionDataSource(MissionService service){
		this.service = service;
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	public Mission getMission() {
		return service.getMissionRecord();
	}
	
	@Override
	protected List<Name> createTypeNames() throws IOException {
		boolean dd = false;
		this.attributeMap = new HashMap<>();
		List<Name> names = new ArrayList<>();
		names.add(new NameImpl(MISSIONWAYPOINT_TYPE));
		names.add(new NameImpl(MISSIONTRACK_TYPE));
		
		try (Session session = HibernateManager.openSession()){
			Mission mission = session.get(Mission.class, service.getMissionUuid()); 
			dd = mission.getSurvey().getSurveyDesign().getTrackDistanceDirection();
			
			//find all geometry based data model
			List<Attribute> attributes = DataModelManager.INSTANCE.getGeometryAttributes(session);
			for (Attribute attribute : attributes) {
				String type = attribute.getType().name() + "." + attribute.getKeyId(); //$NON-NLS-1$
				names.add(new NameImpl(type)); 
				attributeMap.put(type, attribute);
			}
		}
		
		if (dd) names.add(new NameImpl(MISSIONRAWWAYPOINT_TYPE));
		return names;
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		return new MissionFeatureSource(entry);
	}
	
	public static boolean isLineAttribute(String name) {
		return (name.startsWith(Attribute.AttributeType.LINE.name() + ".")); //$NON-NLS-1$
	}
	public static boolean isPolygonAttribute(String name) {
		return (name.startsWith(Attribute.AttributeType.POLYGON.name() + ".")); //$NON-NLS-1$
	}
	public static boolean isGeometryAttribute(String name) {
		return  (name.startsWith(Attribute.AttributeType.POLYGON.name() + ".") || //$NON-NLS-1$
				name.startsWith(Attribute.AttributeType.LINE.name() + ".")); //$NON-NLS-1$
	}

	public String getName(String typeName) {
		if (typeName.equals(MissionDataSource.MISSIONTRACK_TYPE)) return Messages.MissionFeatureSource_TrackLayerName;
		if (typeName.equals(MissionDataSource.MISSIONWAYPOINT_TYPE)) return Messages.MissionFeatureSource_WaypointLayerName;
		if (typeName.equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)) return Messages.MissionFeatureSource_RawWaypointLayerName;
		if (isGeometryAttribute(typeName)) {
			return attributeMap.get(typeName).getName();
		}
		return null;

	}

	public Attribute getAttribute(String typeName) {
		return attributeMap.get(typeName);
	}
}
