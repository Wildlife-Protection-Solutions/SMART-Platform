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

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Collection of utilities shared with Desktop and Connect applications.
 * 
 * @author Emily
 *
 */
public class SharedUtils {
	
	/**
	 * <p>
	 * Strips double quotes off the beginning and end of the string
	 * only if they exist in both places.  Examples:</p>
	 * <p>"abc" -> abc</p>
	 * <p>abc" -> abc"</p>
	 * <p>"abc -> "abc</p>
	 * <p>""abc"" -> "abc"</p>
	 *  
	 * @param str 
	 * @return string with quotes removed
	 */
	public static String stripQuotes(String str){
		if (str == null) return str;
		if (str.length() == 0) return str;
		if (str.charAt(0)=='"' && str.charAt(str.length()-1) == '"'){
			return str.substring(1, str.length() - 1);
		}
		return str;
	}

	/**
	 * Gets only the date part of a given date. Sets the time to 0 is not
	 * endOfDay; sets the time to 23:59:59 if end of day.
	 * 
	 * @param dt
	 * @return date only date
	 */
	public static Date getDatePart(Date date, boolean endOfDay) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (!endOfDay) {
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
		} else {
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MILLISECOND, 0);
		}

		return calendar.getTime();
	}
	

	/**
	 * converts a Date to a Calendar object
	 */
	public static Calendar convertDate(Date d) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(d);
		return calendar;

	}

	public static boolean isSameDate(Date d1, Date d2) {
		Calendar c1 = convertDate(d1);
		Calendar c2 = convertDate(d2);
		return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH) 
				&& c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
				&& c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
	}

	public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	/**
	 * Compares the hour, minute, second and millisecond values of
	 * the two date objects and returns true if they are the same, 
	 * false otherwise
	 * 
	 * @param dt
	 * @return date only date
	 */
	public static boolean isSameTime(Date date1, Date date2) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date1);
		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTime(date2);
		
		int[] compare = new int[]{Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND};
		for (int field : compare){
			if( calendar.get(field) != calendar2.get(field) ) return false;
		}
		return true;
	}
	
	/**
	 * Get the filename part of a file
	 * @param part
	 * @return
	 */
	public static String getFilenameWithoutExtension(String part) {
		int index = part.lastIndexOf('.');
		if (index < 0) return part;
		return part.substring(0,index);
	}
	
	/**
	 * Get the extension part of the file
	 * @param part
	 * @return
	 */
	public static String getFilenameExtension(String part) {
		int index = part.lastIndexOf('.');
		if (index < 0) return ""; //$NON-NLS-1$
		return part.substring(index+1);
	}
	
	public static Time createPatrolTime(int hours, int minute, int second){
		Calendar cForProcessing = Calendar.getInstance();
		cForProcessing.setTimeInMillis(0);
		
		cForProcessing.set(Calendar.HOUR_OF_DAY, hours);
		cForProcessing.set(Calendar.MINUTE, minute);
		cForProcessing.set(Calendar.SECOND, second);
		cForProcessing.set(Calendar.MILLISECOND, 0);
		
		return new Time(cForProcessing.getTime().getTime());
	}
	
	public static Time convertDateToTime(Date d){
		Calendar c = Calendar.getInstance();
		c.setTime(d);		
		return createPatrolTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));		
	}
	
	
	public static LocalDateTime toLocalDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
	public static LocalDate toLocalDate(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
	public static Date toDate(LocalDateTime date) {
		return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	}

}
