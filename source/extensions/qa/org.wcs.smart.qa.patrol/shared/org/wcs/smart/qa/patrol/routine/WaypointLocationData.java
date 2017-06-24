package org.wcs.smart.qa.patrol.routine;

import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.routine.ILocationRoutineData;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class WaypointLocationData implements ILocationRoutineData {

	private Waypoint wp;
	private Coordinate c = null;
	public WaypointLocationData(Waypoint wp) {
		this.wp = wp;
		c = new Coordinate(wp.getX(), wp.getY());
	}
	
	@Override
	public Geometry getGeometry() {
		return null;
	}

	@Override
	public Type getType() {
		return Type.POINT;
	}

	@Override
	public Coordinate getPoint() {
		return c;
	}

	public Waypoint getWaypoint(){
		return this.wp;
	}
}
