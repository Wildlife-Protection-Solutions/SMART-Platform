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
package org.wcs.smart.qa.patrol.routine;

import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.qa.routine.ILocationRoutineData;

/**
 * Implementation of ILocationRoutine data that wraps a waypoint
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class PatrolLocationData implements ILocationRoutineData {

	protected Patrol patrol;
	
	public PatrolLocationData(Patrol patrol) {
		this.patrol = patrol;
	}
	
	@Override
	public Geometry getGeometry() {
		return null;
	}

	@Override
	public Type getType() {
		return Type.LINESTRING;
	}

	public Patrol getPatrol() {
		return this.patrol;
	}

}
