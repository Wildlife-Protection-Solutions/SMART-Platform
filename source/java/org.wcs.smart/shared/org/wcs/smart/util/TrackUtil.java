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
		coordinates = new ArrayList<>(coordinates);
		
		//check for initial duplicate and remove
		if (coordinates.size() > 2) {
			Coordinate c1 = coordinates.get(0);
			Coordinate c2 = coordinates.get(1);
			if (c1.x == c2.x && c1.y == c2.y && c1.z == c2.z) {
				coordinates.remove(0);
			}
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
	
//	public static void main(String[] args) {
//		Coordinate c1 = new Coordinate(0, 0, 0);
//		Coordinate c2 = new Coordinate(0, 0, 0.5);
//		Coordinate c3 = new Coordinate(1, 1, 1);
//		Coordinate c4 = new Coordinate(3, 3, 3);
//		Coordinate c5 = new Coordinate(2, 2, 2);
//		
//		List<Coordinate> items = new ArrayList<>();
//		items.add(c1);
//		items.add(c2);
//		items.add(c3);
//		items.add(c4);
//		items.add(c5);
//		
//		LineString ls = convertToLineString(items);
//		
//		for (Coordinate c : items) {
//			System.out.println("Coordinate(" + c.x + " " + c.y + " " + c.z + ")");
//		}
//		
//		System.out.println(ls.toText());
//	}
}

