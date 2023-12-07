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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.udig.ObservationAttributeFeatureFactory;

/**
 * Feature reader for mission observation points.
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType featureType;
	private Iterator<?> iterator;
	private String typename;
	
	public MissionFeatureReader(Mission mission, SimpleFeatureType type, String typename){

		this.featureType = type;
		this.typename = typename;
		
		if (typename.equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE) || 
			(typename.equals(MissionDataSource.MISSIONWAYPOINT_TYPE))){ 	
		
			List<SurveyWaypoint> me = new ArrayList<SurveyWaypoint>();
			
			for (MissionDay md : mission.getMissionDays()){
				me.addAll(md.getWaypoints());
			}
			iterator = me.iterator();
		}else if (typename.equals(MissionDataSource.OBS_ATTRIBUTE_LINESTRING) ||
				typename.equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON)) {
			
			AttributeType matching = AttributeType.LINE;
			if (typename.equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON)) {
				matching = AttributeType.POLYGON;
			}
			
			try(Session session = HibernateManager.openSession()){
				
				mission= session.get(Mission.class, mission.getUuid());
				
				List<WaypointObservationAttribute> attributes = new ArrayList<>();
				for (MissionDay l : mission.getMissionDays()) {
					for (SurveyWaypoint wp : l.getWaypoints()) {
						
						for (WaypointObservation wo : wp.getWaypoint().getAllObservations()) {
							for (WaypointObservationAttribute a : wo.getAttributes()) {
								if (a.getGeom() != null && a.getAttribute().getType() == matching) {
									attributes.add(a);
									a.getAttributeValueAsString(Locale.getDefault());
									a.getObservation().getCategory().getName();
								}
							}
						}
					}
				}
				iterator = attributes.iterator();
			}
		}else{
			iterator = null;
		}
		
	}
	
	@Override
	public SimpleFeatureType getFeatureType() {
		return this.featureType;
	}

	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		SimpleFeature feature = createFeature(iterator.next());
		return feature;
	}

	@Override
	public boolean hasNext() throws IOException {
		return iterator != null && iterator.hasNext();
	}

	@Override
	public void close() throws IOException {
	}
	
	//String spec = 
	//"fid:String,id:Integer,date:Date,sampling_unit_id:String,observation:String,comment:String,geom:Point:srid=4326"; //$NON-NLS-1$
	private SimpleFeature createFeature(Object feature){
		if (typename.equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)){
			return SurveyFeatureFactory.createWaypointPrjFeature(featureType, (SurveyWaypoint)feature);
		}else if (typename.equals(MissionDataSource.MISSIONWAYPOINT_TYPE)){ 	
			return SurveyFeatureFactory.createWaypointFeature(featureType, (SurveyWaypoint)feature);
		}else if (typename.equals(MissionDataSource.OBS_ATTRIBUTE_LINESTRING) || typename.equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON)) {
			boolean hasArea = typename.equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON); 
			return new SurveyFeature(ObservationAttributeFeatureFactory.getObservationAttributeAsGeometry(featureType, hasArea, (WaypointObservationAttribute)feature ));
		}
		return null;
	}
}
