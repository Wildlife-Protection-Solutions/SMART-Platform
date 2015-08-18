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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Smart area feature reader
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private String thisType;
	private Iterator<?> fIterator;
	
	private SimpleDateFormat trackDt = new SimpleDateFormat("MMMddyyyy");  //$NON-NLS-1$
	private static GeometryFactory gf = new GeometryFactory();
	
	public PatrolFeatureReader(Patrol patrol,
			String type, SimpleFeatureType ftype) {
		this.ftype = ftype;
		this.thisType = type;
	
		if (type.equals(PatrolDataSource.WAYPOINT_TYPE)){
			List<PatrolWaypoint> pnts = new ArrayList<PatrolWaypoint>();
			for (PatrolLeg l : patrol.getLegs()){
				for (PatrolLegDay d : l.getPatrolLegDays()){
					if (d.getWaypoints() != null){
						pnts.addAll(d.getWaypoints());
					}
				}
			}
			fIterator = pnts.iterator();
		}else if (type.equals(PatrolDataSource.TRACK_TYPE)){
			List<Track> pnts = new ArrayList<Track>();
			for (PatrolLeg l : patrol.getLegs()){
				for (PatrolLegDay d : l.getPatrolLegDays()){
					if (d.getTrack() != null){
						pnts.add(d.getTrack());
					}
				}
			}
			fIterator = pnts.iterator();
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
			return new PatrolFeature(getWaypointAsFeature((PatrolWaypoint)this.fIterator.next()));
		}else if (thisType.equals(PatrolDataSource.TRACK_TYPE)){
			return new PatrolFeature(getTrackAsFeature((Track)this.fIterator.next()));
		}
		return null;
	}
	
	
	private SimpleFeature getWaypointAsFeature(PatrolWaypoint waypoint){
		//String spec = "fid:String,id:integer,date:Date,time:Time,comment:String,geom:Point:srid=4326";
		Object data[] = new Object[7];
		data[0] = ftype.getName() + "." + waypoint.getWaypoint().getId() + "." + UuidUtils.uuidToString(waypoint.getWaypoint().getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		data[1] = waypoint.getWaypoint().getId();
		data[2] = waypoint.getPatrolLegDay() == null ? null : waypoint.getPatrolLegDay().getDate();
		data[3] = waypoint.getWaypoint().getDateTime();
		if (waypoint.getWaypoint().getObservations() == null || waypoint.getWaypoint().getObservations().size() == 0){
			data[4] = ""; //$NON-NLS-1$
		}else{ 
			data[4] = waypoint.getWaypoint().getObservationsAsString();
		}
		data[5] = waypoint.getWaypoint().getComment();
		data[6] = gf.createPoint(new Coordinate(waypoint.getWaypoint().getX(), waypoint.getWaypoint().getY()));
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}
	
	private SimpleFeature getTrackAsFeature(Track track){
		//String spec = "fid:String,distance:Double,day:Date,leg:String,geom:LineString:srid=4326";
		String fid = ftype.getName() + "." + trackDt.format(track.getPatrolLegDay().getDate()) + "." + track.getPatrolLegDay().getPatrolLeg().getId();  //$NON-NLS-1$ //$NON-NLS-2$
		Object data[] = new Object[5];
		data[0] = fid;
		data[1] = track.getDistance();
		data[2] = track.getPatrolLegDay().getDate();
		data[3] = track.getPatrolLegDay().getPatrolLeg().getId();
		try{
			data[4] = track.getLineString();
		}catch (Exception ex){
			SmartPatrolPlugIn.log(ex.getMessage(), ex);
		}
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}
}
