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

import java.time.format.DateTimeFormatter;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.udig.ObservationAttributeFeatureFactory;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.TrackPart;
import org.wcs.smart.util.UuidUtils;

public class PatrolFeatureFactory {
	
	private static final DateTimeFormatter TRACK_DT_FORMAT = DateTimeFormatter.ofPattern("MMMddyyyy");  //$NON-NLS-1$
	
	public static SimpleFeatureType createObservationPolygonSchema(String typeName) throws SchemaException{
		return ObservationAttributeFeatureFactory.createObservationPolygonSchema(typeName);
	}
	
	public static SimpleFeatureType createObservationLineStringSchema(String typeName) throws SchemaException{
		return ObservationAttributeFeatureFactory.createObservationLineStringSchema(typeName);		
	}
	
	public static SimpleFeatureType createWaypointPrjSchema() throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:LineString:srid=4326,"); //$NON-NLS-1$
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("id:String,"); //$NON-NLS-1$
		sb.append("date:java.time.LocalDate,"); //$NON-NLS-1$
		sb.append("time:java.time.LocalTime,"); //$NON-NLS-1$
		sb.append("rawx:Double,"); //$NON-NLS-1$
		sb.append("rawy:Double,"); //$NON-NLS-1$
		sb.append("distance:Double,"); //$NON-NLS-1$
		sb.append("bearing:Double,"); //$NON-NLS-1$
		sb.append("x:Double,"); //$NON-NLS-1$
		sb.append("y:Double,"); //$NON-NLS-1$
		sb.append("wp_uuid:String"); //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType(PatrolDataSource.WAYPOINT_PRJ_TYPE, sb.toString());
		return type;
	}
	
	public static SimpleFeatureType createWaypointSchema() throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Point:srid=4326,"); //$NON-NLS-1$
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("id:String,"); //$NON-NLS-1$
		sb.append("date:java.time.LocalDate,"); //$NON-NLS-1$
		sb.append("time:java.time.LocalTime,"); //$NON-NLS-1$
		sb.append("obs:String,"); //$NON-NLS-1$
		sb.append("comment:String,"); //$NON-NLS-1$
		sb.append("wp_uuid:String"); //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType(PatrolDataSource.WAYPOINT_TYPE, sb.toString());
		return type;
	}
	
	public static SimpleFeatureType createTrackPartSchema() throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:LineString:srid=4326,"); //$NON-NLS-1$
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("distance:Double,"); //$NON-NLS-1$
		sb.append("day:java.time.LocalDate,"); //$NON-NLS-1$
		sb.append("leg:String,"); //$NON-NLS-1$
		sb.append("transport_type_key:String,"); //$NON-NLS-1$
		sb.append("uid:String"); //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType(PatrolDataSource.TRACK_PART_TYPE, sb.toString());
		return type;
	}

	public static SimpleFeature getObservationAttributeAsGeometry(SimpleFeatureType ftype, WaypointObservationAttribute value) {
		boolean hasArea = PatrolDataSource.isPolgyonAttribute(ftype.getName().getLocalPart());
		return ObservationAttributeFeatureFactory.getObservationAttributeAsGeometry(ftype, hasArea, value);
	}
	
	public static SimpleFeature getWaypointAsFeature(SimpleFeatureType ftype, PatrolWaypoint waypoint){
		//
		Object data[] = new Object[8];
		data[0] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(waypoint.getWaypoint().getX(), waypoint.getWaypoint().getY()));
		data[1] = ftype.getName() + "." + waypoint.getWaypoint().getId() + "." + UuidUtils.uuidToString(waypoint.getWaypoint().getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		data[2] = waypoint.getWaypoint().getId();
		data[3] = waypoint.getWaypoint().getDateTime().toLocalDate();
		data[4] = waypoint.getWaypoint().getDateTime().toLocalTime();
		String obs = waypoint.getWaypoint().getObservationsAsString();
		if (obs == null) obs = ""; //$NON-NLS-1$
		data[5] = obs;
		
		data[6] = waypoint.getWaypoint().getComment();
		data[7] = UuidUtils.uuidToString(waypoint.getWaypoint().getUuid());
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
	public static SimpleFeature getWaypointAsPrjFeature(SimpleFeatureType ftype, PatrolWaypoint waypoint){
		Object data[] = new Object[12];
		data[0] = GeometryFactoryProvider.getFactory().createLineString(new Coordinate[] {
				new Coordinate(waypoint.getWaypoint().getRawX(), waypoint.getWaypoint().getRawY()),
				new Coordinate(waypoint.getWaypoint().getX(), waypoint.getWaypoint().getY()),
		});
		data[1] = ftype.getName() + "." + waypoint.getWaypoint().getId() + "." + UuidUtils.uuidToString(waypoint.getWaypoint().getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		data[2] = waypoint.getWaypoint().getId();
		data[3] = waypoint.getWaypoint().getDateTime().toLocalDate();
		data[4] = waypoint.getWaypoint().getDateTime().toLocalTime();
		
		data[5] = waypoint.getWaypoint().getRawX();
		data[6] = waypoint.getWaypoint().getRawY();
		
		data[7] = waypoint.getWaypoint().getDistance();
		data[8] = waypoint.getWaypoint().getDirection();
		
		data[9] = waypoint.getWaypoint().getX();
		data[10] = waypoint.getWaypoint().getY();
		data[11] = UuidUtils.uuidToString(waypoint.getWaypoint().getUuid());
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
	public static SimpleFeature getTrackAsFeature(SimpleFeatureType ftype, Track track){
		String fid = ftype.getName() + "." + TRACK_DT_FORMAT.format(track.getPatrolLegDay().getDate()) + "." + track.getPatrolLegDay().getPatrolLeg().getId();  //$NON-NLS-1$ //$NON-NLS-2$
		Object data[] = new Object[7];
		try{
			data[0] = track.getGeometry();
		}catch (Exception ex){
			SmartPatrolPlugIn.log(ex.getMessage(), ex);
		}
		data[1] = fid;
		data[2] = track.getDistance();
		data[3] = track.getPatrolLegDay().getDate();
		data[4] = track.getPatrolLegDay().getPatrolLeg().getId();
		data[5] = track.getPatrolLegDay().getPatrolLeg().getType().getKeyId();
		data[6] = track.getUuid();
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}

	public static SimpleFeature getTrackPartAsFeature(SimpleFeatureType ftype, TrackPart trackPart){
		Track track = trackPart.getTrack();
		String fid = ftype.getName() + "." + TRACK_DT_FORMAT.format(track.getPatrolLegDay().getDate()) + "." + track.getPatrolLegDay().getPatrolLeg().getId();  //$NON-NLS-1$ //$NON-NLS-2$
		Object data[] = new Object[7];
		try{
			data[0] = trackPart.getLineString();
		}catch (Exception ex){
			SmartPatrolPlugIn.log(ex.getMessage(), ex);
		}
		data[1] = fid;
		data[2] = track.getDistance();
		data[3] = track.getPatrolLegDay().getDate();
		data[4] = track.getPatrolLegDay().getPatrolLeg().getId();
		data[5] = track.getPatrolLegDay().getPatrolLeg().getType().getKeyId();
		data[6] = trackPart.getUid();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
}

