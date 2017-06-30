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

import java.text.SimpleDateFormat;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class PatrolFeatureFactory {
	
	private static final SimpleDateFormat TRACK_DT_FORMAT = new SimpleDateFormat("MMMddyyyy");  //$NON-NLS-1$
	
	public static SimpleFeatureType createWaypointSchema() throws SchemaException{
		String spec = "the_geom:Point:srid=4326,fid:String,id:Integer,date:Date,time:Date,observation:String,comment:String"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType(PatrolDataSource.WAYPOINT_TYPE, spec);
		return type;
	}
	
	public static SimpleFeatureType createTrackSchema() throws SchemaException{
		String spec = "the_geom:LineString:srid=4326,fid:String,distance:Double,day:Date,leg:String"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType(PatrolDataSource.TRACK_TYPE, spec);
		return type;
	}
	
	public static SimpleFeature getWaypointAsFeature(SimpleFeatureType ftype, PatrolWaypoint waypoint){
		//String spec = "geom:Point:srid=4326,fid:String,id:integer,date:Date,time:Time,comment:String";
		Object data[] = new Object[7];
		data[0] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(waypoint.getWaypoint().getX(), waypoint.getWaypoint().getY()));
		data[1] = ftype.getName() + "." + waypoint.getWaypoint().getId() + "." + UuidUtils.uuidToString(waypoint.getWaypoint().getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		data[2] = waypoint.getWaypoint().getId();
		data[3] = waypoint.getPatrolLegDay() == null ? null : waypoint.getPatrolLegDay().getDate();
		data[4] = waypoint.getWaypoint().getDateTime();
		if (waypoint.getWaypoint().getObservations() == null || waypoint.getWaypoint().getObservations().size() == 0){
			data[5] = ""; //$NON-NLS-1$
		}else{ 
			data[5] = waypoint.getWaypoint().getObservationsAsString();
		}
		data[6] = waypoint.getWaypoint().getComment();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
	public static SimpleFeature getTrackAsFeature(SimpleFeatureType ftype, Track track){
		//String spec = "geom:LineString:srid=4326,fid:String,distance:Double,day:Date,leg:String";
		String fid = ftype.getName() + "." + TRACK_DT_FORMAT.format(track.getPatrolLegDay().getDate()) + "." + track.getPatrolLegDay().getPatrolLeg().getId();  //$NON-NLS-1$ //$NON-NLS-2$
		Object data[] = new Object[5];
		try{
			data[0] = track.getLineString();
		}catch (Exception ex){
			SmartPatrolPlugIn.log(ex.getMessage(), ex);
		}
		data[1] = fid;
		data[2] = track.getDistance();
		data[3] = track.getPatrolLegDay().getDate();
		data[4] = track.getPatrolLegDay().getPatrolLeg().getId();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
}
