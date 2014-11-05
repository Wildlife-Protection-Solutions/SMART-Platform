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
package org.wcs.smart.er.query.map.geotools;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Class to convert a query result item to 
 * a feature.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyResultItemFeature {

	private static GeometryFactory gf = new GeometryFactory();
	
	
	/**
	 * Converts a query result item to a feature.
	 * The feature type must have been generated 
	 * from the same set of query table columns.
	 * 
	 * @param it the query result item 
	 * @param columns the columns that make up the feature type
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	public static SimpleFeature createObservationFeature(SurveyQueryResultItem it, List<QueryColumn> columns, SimpleFeatureType  ftype){
		
		Object[] data = new Object[columns.size() + 2];
		data[0] = it.getMissionId() + "." + it.getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for (int i = 0; i < columns.size(); i ++){
			data[i+1] = QueryColumn.getValue(it, columns.get(i), ftype.getDescriptor(i + 1));
		}
		data[data.length -1] = gf.createPoint(new Coordinate(it.getWaypointX(), it.getWaypointY()));
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
		
	}
	
	/**
	 * Converts a query result item to a feature.
	 * The feature type must have been generated 
	 * from the same set of query table columns.
	 * 
	 * @param it the query result item 
	 * @param columns the columns that make up the feature type
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	public static SimpleFeature createTrackFeature(SurveyQueryResultItem it, List<QueryColumn> columns, SimpleFeatureType  ftype){
		
		Object[] data = new Object[columns.size() + 2];
		data[0] = it.getMissionId() + "." + it.getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for (int i = 0; i < columns.size(); i ++){
			data[i+1] = QueryColumn.getValue(it, columns.get(i), ftype.getDescriptor(i + 1));
		}
		
		Geometry g = null;
		if (it.getTracks() != null && it.getTracks().size() > 0){
			g = gf.createMultiLineString(it.getTracks().toArray(new LineString[it.getTracks().size()]));
		}
		data[data.length -1] = g;
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}
	
	/**
	 * Converts a query result item to a feature.
	 * The feature type must have been generated 
	 * from the same set of query table columns.
	 * 
	 * @param it the query result item 
	 * @param columns the columns that make up the feature type
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	public static SimpleFeature createTrackFeature(MissionTrackResultItem it, Session session,
			List<QueryColumn> columns, SimpleFeatureType ftype){
		Object[] data = new Object[columns.size() + 2];
		data[0] = it.getMissionId() + "." + SmartUtils.encodeHex(it.getTrackUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		
		for (int i = 0; i < columns.size(); i ++){
			if (i == 3){
//				data[i+1] = null;
				data[i+1] = QueryColumn.getValue(it, columns.get(i), ftype.getDescriptor(i + 1));
			}else{
				data[i+1] = QueryColumn.getValue(it, columns.get(i), ftype.getDescriptor(i + 1));
			}
		}
		MissionTrack mt = (MissionTrack) session.load(MissionTrack.class, it.getTrackUuid());
		data[data.length -1] = mt.getLineString();
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}
	
	/**
	 * Converts a mission to a feature
	 * 
	 * @param mission the mission 
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	public static SimpleFeature createObservationFeature(Mission mission, SimpleFeatureType  ftype){
		//"fid:String,id:String,start:Date,end:Date,comment:String,geom:LineString:srid=4326"
		
		Object[] data = new Object[6];
		data[0] = mission.getId() + "." + SmartUtils.encodeHex(mission.getUuid()); //$NON-NLS-1$
		data[1] = mission.getId();
		data[2] = mission.getStartDate();
		data[3] = mission.getEndDate();
		data[4] = mission.getComment();
		
		List<LineString> geoms = new ArrayList<LineString>();
		for (MissionDay md : mission.getMissionDays()){
			for (MissionTrack mt : md.getTracks()){
				geoms.add(mt.getLineString());
			}
		}
		
		if (geoms.size() > 0){
			Geometry g = gf.createMultiLineString(geoms.toArray(new LineString[geoms.size()]));
			data[5] = g;
		}else{
			data[5] = null;
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}

}
