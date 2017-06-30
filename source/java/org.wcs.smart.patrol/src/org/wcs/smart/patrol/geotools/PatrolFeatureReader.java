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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;

/**
 * Smart area feature reader
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private String thisType;
	private Iterator<?> fIterator;

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
			return new PatrolFeature(PatrolFeatureFactory.getWaypointAsFeature(ftype, (PatrolWaypoint)this.fIterator.next()));
		}else if (thisType.equals(PatrolDataSource.TRACK_TYPE)){
			return new PatrolFeature(PatrolFeatureFactory.getTrackAsFeature(ftype, (Track)this.fIterator.next()));
		}
		return null;
	}
}
