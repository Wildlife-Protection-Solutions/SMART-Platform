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

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Feature factory for creating survey feature types and features.
 * 
 * @author Emily
 *
 */
public class SurveyFeatureFactory {
	
	/**
	 * Create feature schema for mission waypoints
	 * @return
	 * @throws SchemaException
	 */
	public static SimpleFeatureType createWaypointSchema() throws SchemaException{
		String spec = "fid:String,id:Integer,date:Date,sampling_unit_id:String,observation:String,comment:String,geom:Point:srid=4326"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType("smart." + MissionDataSource.MISSIONWAYPOINT_TYPE, spec); //$NON-NLS-1$
		return type;
	}
	
	/**
	 * Create feature schema for mission tracks
	 * @return
	 * @throws SchemaException
	 */
	public static SimpleFeatureType createTrackSchema() throws SchemaException{
		String spec = "fid:String,id:String,date:Date,sampling_unit_id:String,mission_id:String,distance:Double,geom:LineString:srid=4326"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType("smart." + MissionDataSource.MISSIONTRACK_TYPE, spec); //$NON-NLS-1$
		return type;
	}

	/**
	 * creates a survey waypoint feature from a waypoint
	 * @param ftype
	 * @param point
	 * @return
	 */
	public static SimpleFeature createWaypointFeature(SimpleFeatureType ftype, SurveyWaypoint point){
		String fid = point.getWaypoint().getId() + "." + UuidUtils.uuidToString(point.getWaypoint().getUuid()); //$NON-NLS-1$
		
		Object[] data = new Object[7];
		data[0] = fid;
		data[1] = point.getWaypoint().getId();
		data[2] = point.getWaypoint().getDateTime();
		if (point.getSamplingUnit() != null){
			data[3] = point.getSamplingUnit().getId();
		}else{
			data[3] = ""; //$NON-NLS-1$
		}
		data[4] = point.getWaypoint().getObservationsAsString();
		data[5] = point.getWaypoint().getComment();
		data[6] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(point.getWaypoint().getX(),point.getWaypoint().getY()));		
		
		return new SurveyFeature(SimpleFeatureBuilder.build(ftype, data, fid));
	}
	
	
	/**
	 * Creates a track feature from a mission track.  
	 * 
	 * @param ftype
	 * @return
	 */
	public static SimpleFeature createTrackFeature(SimpleFeatureType ftype, MissionTrack track) {
		String fid = UuidUtils.uuidToString(track.getUuid());
		Object[] data = new Object[7];
		data[0] = fid;
		data[1] = track.getId();
		data[2] = track.getMissionDay().getDate();
		data[3] = track.getSamplingUnit() == null ? "" : track.getSamplingUnit().getId(); //$NON-NLS-1$
		data[4] = track.getMissionDay().getMission().getId();
		try{
			data[5] = track.getGeometryLengthKm();
			data[6] = track.getLineString();
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
		return SimpleFeatureBuilder.build(ftype, data, fid);
	}
}
