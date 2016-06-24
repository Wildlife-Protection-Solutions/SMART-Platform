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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import org.wcs.smart.map.GeometryFactoryProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Util for track calculations.
 * 
 * @author elitvin
 * @since 3.3.0
 */
public class TrackUtil {

	/**
	 * Converts a set of coordinate to a line string.  Coordinates are first sorted
	 * by date/time which should be stored in the z coordinate
	 * as the the date/time in the current timezone.  This function
	 * convert the z to GMT time.
	 * 
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static LineString convertToLineString(List<Coordinate> coordinates, TimeZone timezone){
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
				return ((Double) o1.z).compareTo((Double) o2.z);
			}
		});

		for (Coordinate c : coordinates){
			//c.z is the date taking into account the current timezone.  We want to compute
			//the date of GMT timezone and assign that to the point.
			//we need to take the year,month,date, hour, min, sec and assign it to a date with
			//a time zone of gmt
			Calendar c1 = Calendar.getInstance();
			c1.setTimeInMillis((long)c.z);
			Calendar c2 = Calendar.getInstance();
			c2.setTimeZone(timezone);
			c2.setTimeInMillis(0);
			c2.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DATE), c1.get(Calendar.HOUR_OF_DAY), c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND));

			c.z = c2.getTime().getTime();

		}
		LineString track = GeometryFactoryProvider.getFactory().createLineString(coordinates
				.toArray(new Coordinate[coordinates.size()]));
		return track;
	}
	
}
