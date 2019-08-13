/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.qa.routine;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Implementation of ILocationRoutine data that wraps a waypoint
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class WaypointLocationData implements ILocationRoutineData {

	protected Waypoint wp;
	protected Coordinate c = null;
	protected Coordinate c2 = null;
	
	public WaypointLocationData(Waypoint wp) {
		this.wp = wp;
		c = new Coordinate(wp.getRawX(), wp.getRawY());
		
		if (wp.getDirection() != null && wp.getDistance() != null) {
			c2 = new Coordinate(wp.getX(), wp.getY());
		}
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

	@Override
	public Coordinate getProjectedPoint() {
		return c2;
	}
	
	public Waypoint getWaypoint(){
		return this.wp;
	}

}
