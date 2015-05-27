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
package org.wcs.smart.conversion.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.LineString;

public class SmartUtil {

	//copy from SmartUtil
	public static XMLGregorianCalendar toXmlDateTime(Date d) throws DatatypeConfigurationException {
		if (d == null) {
			return null;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(d);
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		return xgc;
	}
	
	//copy from SmartUtil
	public static XMLGregorianCalendar toXmlDate(Date d) throws DatatypeConfigurationException {
		if (d == null) {
			return null;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(d);
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		xgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setHour(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setMinute(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setSecond(DatatypeConstants.FIELD_UNDEFINED);
		
		return xgc;
	}

	//copy from PatrolToXmlConverter
	public static XMLGregorianCalendar toXmlTime(Date d) throws DatatypeConfigurationException{
		if (d == null) {
			return null;
		}
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		cal.setTime(d);
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		xgc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setYear(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setMonth(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setDay(DatatypeConstants.FIELD_UNDEFINED);
		
		return xgc;
	}

	public static Date combine(XMLGregorianCalendar xmlDate, XMLGregorianCalendar xmlTime) {
		Date date = xmlDate != null ? xmlDate.toGregorianCalendar().getTime() : null;
		Date time = xmlTime != null ? xmlTime.toGregorianCalendar().getTime() : null;
		return combine(date, time);
	}
	
	//copy from SmartImporter
	public static Date combine(Date date, Date time) {
		if (date == null)
			return time;
		if (time == null)
			return date;
		Calendar timeCalendar = Calendar.getInstance();
		timeCalendar.setTime(time);
		Calendar dateCalendar = Calendar.getInstance();
		dateCalendar.setTime(date);
		dateCalendar.add(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
		dateCalendar.add(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
		dateCalendar.add(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
		return dateCalendar.getTime();
	}

	//copy from GeometryUtils
	public static double distanceInMeters(LineString ls) {
		GeodeticCalculator cal = new GeodeticCalculator();
		double distance = 0;
		for (int i = 1; i < ls.getCoordinates().length; i ++){
			cal.setStartingGeographicPoint(ls.getCoordinateN(i-1).x, ls.getCoordinateN(i-1).y);
			cal.setDestinationGeographicPoint(ls.getCoordinateN(i).x, ls.getCoordinateN(i).y);
			
			distance +=cal.getOrthodromicDistance();
		}
		return distance;
	}

	//copy from SmartUtils
	public static String encodeHex(byte[] data) {
		if (data == null) return ""; //$NON-NLS-1$
		char[] toDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		int l = data.length;
		char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return new String(out);
	}
	
}
