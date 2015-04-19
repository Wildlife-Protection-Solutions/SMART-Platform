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
package org.wcs.smart.conversion.tool;

import java.sql.Time;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.wcs.smart.conversion.tag.TagT;
import org.wcs.smart.conversion.util.CoordinateUtil;
import org.wcs.smart.conversion.util.SmartUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Creates LineString object from given data
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class LineStringBuilder {
	
	public LineString createLineString(List<TagT> tList) throws ParseException {
		if (tList == null) {
			return null;
		}
		
		List<Coordinate> coordinates = new ArrayList<Coordinate>();
		Date date;
		Time time;
		double x, y;
		DateTimeParser dateTimeParser = new DateTimeParser();
		for (TagT t : tList) {
			date = dateTimeParser.parseDate(t.getDate());
			time = dateTimeParser.parseTime(t.getTime());
			y = Double.valueOf(t.getLatitude());
			x = Double.valueOf(t.getLongitude());
			coordinates.add(new Coordinate(x, y, SmartUtil.combine(date, time).getTime()));
		}
		return CoordinateUtil.buildLineString(coordinates);
	}
	
}
