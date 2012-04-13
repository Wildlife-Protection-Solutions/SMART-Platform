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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.SmartUtils;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;

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
	private Iterator fIterator;
	private static GeometryFactory gf = new GeometryFactory();
	
	public PatrolFeatureReader(Patrol patrol,
			String type, SimpleFeatureType ftype) {
		this.ftype = ftype;
		this.thisType = type;
		
		if (type.equals(PatrolDataSource.WAYPOINT_TYPE)){
			List<Waypoint> pnts = new ArrayList<Waypoint>();
			for (PatrolLeg l : patrol.getLegs()){
				for (PatrolLegDay d : l.getPatrolLegDays()){
					pnts.addAll(d.getWaypoints());
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
			return getWaypointAsFeature((Waypoint)this.fIterator.next());
		}else if (thisType.equals(PatrolDataSource.TRACK_TYPE)){
			return getTrackAsFeature((Track)this.fIterator.next());
		}
		return null;
	}
	
	
	private SimpleFeature getWaypointAsFeature(Waypoint waypoint){
		//String spec = "fid:String,id:integer,date:Date,time:Time,comment:String,geom:Point:srid=4326";
		Object data[] = new Object[7];
		data[0] = waypoint.getId() + "_" + SmartUtils.encodeHex(waypoint.getUuid());
		data[1] = waypoint.getId();
		data[2] = waypoint.getPatrolLegDay().getDate();
		data[3] = waypoint.getTime();
		if (waypoint.getObservations() == null || waypoint.getObservations().size() == 0){
			data[4] = "";
		}else if (waypoint.getObservations().size() == 1){
			data[4] = waypoint.getObservations().get(0).getCategory().getName();
		}else{
			data[4] = "Multiple Observations";
		}
		data[5] = waypoint.getComment();
		data[6] = gf.createPoint(new Coordinate(waypoint.getX(), waypoint.getY()));
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}
	
	private SimpleFeature getTrackAsFeature(Track track){
		//String spec = "uuid:String,distance:Double,geom:Linestring:srid=4326";
		String fid = DateFormat.getDateInstance(DateFormat.MEDIUM).format(track.getPatrolLegDay().getDate()) + "_" +SmartUtils.encodeHex(track.getUuid()); 
		Object data[] = new Object[4];
		data[0] = fid;
		data[1] = track.getDistance();
		data[2] = track.getPatrolLegDay().getDate();
		data[3] = track.getLineString();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}
}
