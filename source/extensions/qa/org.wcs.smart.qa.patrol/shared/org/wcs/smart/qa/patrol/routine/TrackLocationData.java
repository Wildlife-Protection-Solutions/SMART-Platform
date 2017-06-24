package org.wcs.smart.qa.patrol.routine;

import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.qa.routine.ILocationRoutineData;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class TrackLocationData implements ILocationRoutineData {

	private Track track;
	
	public TrackLocationData(Track track) {
		this.track = track;
	}
	
	@Override
	public Geometry getGeometry() {
		try {
			return track.getLineString();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Type getType() {
		return Type.LINESTRING;
	}

	@Override
	public Coordinate getPoint() {
		return null;
	}

	public Track getTrack(){
		return this.track;
	}
}
