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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.udig.WaypointSimpleFeature;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

/**
 * Class to convert a query result item to 
 * a feature.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyResultItemFeature {

	private static void addQueryColumnData(IResultItem it, SimpleFeatureType ftype, List<QueryColumn> columns, List<Object> data){
		int i = 0;
		for (QueryColumn c : columns){
			if (c.isVisible()){
				data.add(QueryColumnUtils.getValue(it, c, ftype.getDescriptor(i++)));
			}
		}
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
	public static SimpleFeature createObservationFeature(SurveyQueryResultItem it, List<QueryColumn> columns, SimpleFeatureType  ftype){
		List<Object> data = new ArrayList<Object>();
		
		data.add(it.asGeometry(SurveyQueryResultItem.WAYPOINT_GEOMCOLUMN_KEY));
		data.add(it.getMissionId() + "." + it.getWaypointId() + "." + System.nanoTime()); //$NON-NLS-1$ //$NON-NLS-2$
		addQueryColumnData(it, ftype, columns, data);
		return new WaypointSimpleFeature(SimpleFeatureBuilder.build(ftype, data, (String)data.get(1)), it.getWaypointUuid());
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
		List<Object> data = new ArrayList<Object>();
		
		data.add(it.asGeometry(SurveyQueryResultItem.TRACK_GEOMCOLUMN_KEY));
		data.add(it.getMissionId() + "." + it.getWaypointId() + "." + System.nanoTime()); //$NON-NLS-1$ //$NON-NLS-2$
		addQueryColumnData(it, ftype, columns, data);
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
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
	public static SimpleFeature createTrackFeature(MissionTrackResultItem it, List<QueryColumn> columns, SimpleFeatureType ftype){
		List<Object> data = new ArrayList<Object>();
		
		
		data.add(it.asGeometry(MissionTrackResultItem.TRACK_GEOMCOLUMN_KEY));
		data.add(it.getMissionId() + "." + UuidUtils.uuidToString(it.getTrackUuid())); //$NON-NLS-1$ 
		
		addQueryColumnData(it, ftype, columns, data);
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
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
		List<LineString> geoms = new ArrayList<LineString>();
		for (MissionDay md : mission.getMissionDays()){
			for (MissionTrack mt : md.getTracks()){
				try{
					geoms.add(mt.getLineString());
				}catch (Exception ex){
					ERQueryPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		
		if (geoms.size() > 0){
			Geometry g = GeometryFactoryProvider.getFactory().createMultiLineString(geoms.toArray(new LineString[geoms.size()]));
			data[0] = g;
		}else{
			data[0] = null;
		}
		
		data[1] = mission.getId() + "." + UuidUtils.uuidToString(mission.getUuid()); //$NON-NLS-1$
		data[2] = mission.getId();
		data[3] = mission.getStartDate();
		data[4] = mission.getEndDate();
		data[5] = mission.getComment();
		
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}

}
