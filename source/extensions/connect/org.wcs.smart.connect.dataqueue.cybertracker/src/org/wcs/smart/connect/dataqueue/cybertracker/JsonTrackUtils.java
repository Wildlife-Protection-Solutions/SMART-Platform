/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue.cybertracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.wcs.smart.map.GeometryFactoryProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Track utilities.  Inserts a new point into the track sorting by the 
 * date/time associated with the z values 
 * 
 * @author Emily
 *
 */
public class JsonTrackUtils {
	public static TimeZone ZTIMEZONE = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$
	
	public static LineString addPointToTrack(LineString track, Coordinate pnt, Date pntTime){
		long z = convertTimeToGMT(pntTime);
		
		Coordinate[] c = null;
		if (track == null){
			c = new Coordinate[] {new Coordinate(pnt.x, pnt.y, z)};
		}else{
			c = track.getCoordinates();
			if (c[0].x == c[1].x && c[0].y == c[1].y && c[0].z == c[1].z){
				//the first two points are the same so lets remove one
				c = Arrays.copyOfRange(c, 1, c.length);		
			}
			for (Coordinate cd : c){
				if (cd.x == pnt.x && cd.y == pnt.y && cd.z == z){
					//point already exists; do not duplicate it
					return track;
				}
			}
		}
		
		c = Arrays.copyOf(c, c.length + 1);
		c[c.length-1] = new Coordinate(pnt.x, pnt.y, z);
		
		//sort
		Arrays.sort(c, 0, c.length, (Coordinate c1, Coordinate c2) -> ((Double)c1.z).compareTo(c2.z));

		return (LineString)GeometryFactoryProvider.getFactory().createLineString(c);		
	}
		
	public static LineString mergeLineStrings(LineString l1, LineString l2){
		List<Coordinate> all = new ArrayList<>();		
		Arrays.stream(l1.getCoordinates()).forEach(c -> all.add(c));
		Arrays.stream(l2.getCoordinates()).forEach(c -> all.add(c));	
		all.sort((c1, c2) -> ((Double)c1.z).compareTo(c2.z) );
		return (LineString)GeometryFactoryProvider.getFactory().createLineString(all.toArray(new Coordinate[all.size()]));
	}
	
	public static Long convertTimeToGMT(Date time){
		Calendar c1 = Calendar.getInstance();
		c1.setTimeInMillis(time.getTime());
		Calendar c2 = Calendar.getInstance();
		c2.setTimeZone(ZTIMEZONE);
		c2.setTimeInMillis(0);
		c2.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DATE), c1.get(Calendar.HOUR_OF_DAY), c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND));
		return c2.getTime().getTime();
	}
}
