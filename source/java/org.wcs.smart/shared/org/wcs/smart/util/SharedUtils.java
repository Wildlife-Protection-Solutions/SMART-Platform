package org.wcs.smart.util;

import java.util.Calendar;
import java.util.Date;

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
}
