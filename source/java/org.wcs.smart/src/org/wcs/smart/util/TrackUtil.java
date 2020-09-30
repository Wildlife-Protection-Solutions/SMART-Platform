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
package org.wcs.smart.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * Util for track calculations.
 * 
 * @author elitvin
 * @since 3.3.0
 */
public class TrackUtil {

	/**
	 * Converts a set of coordinate to a line string.  Coordinates are first sorted
	 * by date/time which should be stored in the z coordinate IN GMT. Use
	 * SharedUtils.toLongTime to convert from local value to gmt;
	 * 
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static LineString convertToLineString(List<Coordinate> coordinates){
		if (coordinates.size() < 2) {
			return null;
		}

		// copy the list so changes don't affect other parts of the s/w
		coordinates = new ArrayList<Coordinate>(coordinates);
		for (int i = 0; i < coordinates.size(); i ++){
			coordinates.set(i, new Coordinate(coordinates.get(i)));
		}
		
		Collections.sort(coordinates, new Comparator<Coordinate>() {
			@Override
			public int compare(Coordinate o1, Coordinate o2) {
				return ((Double) o1.getZ()).compareTo((Double) o2.getZ());
			}
		});

		LineString track = GeometryFactoryProvider.getFactory().createLineString(coordinates
				.toArray(new Coordinate[coordinates.size()]));
		return track;
	}
	
}
