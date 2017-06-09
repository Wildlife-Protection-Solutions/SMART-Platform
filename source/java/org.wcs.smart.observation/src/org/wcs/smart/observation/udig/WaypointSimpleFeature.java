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
package org.wcs.smart.observation.udig;

import java.util.UUID;

/**
 * A feature that wraps a simple feature that represents a waypoint.  This
 * feature can be adapted to a waypoint when selected.
 */
import org.geotools.feature.DecoratingFeature;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Wrapper around a simple feature that identifies the feature as
 * a waypoint.  
 * Used to adapt the simple feature to a waypoint for displaying
 * details in WaypointInfoView
 * 
 * @author Emily
 *
 */
public class WaypointSimpleFeature extends DecoratingFeature{
	
	private UUID waypointUuid;

	public WaypointSimpleFeature(SimpleFeature wrapper, UUID waypointUuid){
		super(wrapper);
		this.waypointUuid = waypointUuid;
	}
	
	public UUID getWaypointUuid(){
		return this.waypointUuid;
	}
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		return super.equals(other);
	}
}
