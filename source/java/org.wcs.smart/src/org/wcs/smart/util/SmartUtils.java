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

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.DateTime;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;

/**
 * General utility functions.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartUtils {
	
	
	public static enum regExLevel{
		ALLOWED_CHARS_SIMPLE_REGEX("a-Z, 0-9", "[^-0-9a-z]"),
		ALLOWED_CHARS_MED_REGEX("a-Z, 0-9 or - _ :", "[^-0-9a-z:_]" ),
		ALLOWED_CHARS_COMPLEX_REGEX("a-Z, 0-9 or - _ : and spaces", "[^-0-9a-z :_]");
	
		public final String textDesc;
		public final String regex;
	
		regExLevel(String textDesc, String regex){
			this.textDesc = textDesc;
			this.regex = regex;
		}
	}
	
	
	public static NullComparator nullStringComparator = new NullComparator();
	
	public static NullComparator nullIntegerComparator = new NullComparator(new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o1.compareTo(o2);
		}
	});
	
	public static NullComparator nullDoubleComparator = new NullComparator(new Comparator<Double>() {
		@Override
		public int compare(Double o1, Double o2) {
			return o1.compareTo(o2);
		}
	});
	
	public static NullComparator nullBooleanComparator = new NullComparator(new Comparator<Boolean>() {
		@Override
		public int compare(Boolean o1, Boolean o2) {
			return o1.compareTo(o2);
		}
	});
	
	public static NullComparator nullDateComparator = new NullComparator(new Comparator<Date>() {
		@Override
		public int compare(Date o1, Date o2) {
			return o1.compareTo(o2);
		}
	});
	
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	
	/**
	 * Converts a datetime widget to a date object only setting year, month and
	 * day. The hour, minute, second and millisecond are all set to 0;
	 * 
	 */
	public static Date getDate(DateTime dt) {
		//Calendar calendar = new GregorianCalendar();
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.set(Calendar.YEAR, dt.getYear());
		calendar.set(Calendar.MONTH, dt.getMonth());
		calendar.set(Calendar.DAY_OF_MONTH, dt.getDay());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar.getTime();
	}
	

	/**
	 * Gets only the date part of a given date. Sets the time to 0 is not
	 * endOfDay; sets the time to 23:59:59 if end of day.
	 * 
	 * @param dt
	 * @return date only date
	 */
	public static Date getDatePart(Date date, boolean endOfDay) {
		//Calendar calendar = new GregorianCalendar();
		Calendar calendar = GregorianCalendar.getInstance();
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
	 * Gets only the date part of a given date. Sets the time to 0.
	 * 
	 * @param dt
	 * @return date only date
	 */
	public static Date combineDateTime(Date date, Time time) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		Calendar calendar2 = new GregorianCalendar();
		calendar2.setTime(time);

		calendar.set(Calendar.HOUR_OF_DAY, calendar2.get(Calendar.HOUR_OF_DAY));
		calendar.set(Calendar.MINUTE, calendar2.get(Calendar.MINUTE));
		calendar.set(Calendar.SECOND, calendar2.get(Calendar.SECOND));
		calendar.set(Calendar.MILLISECOND, calendar2.get(Calendar.MILLISECOND));

		return calendar.getTime();
	}

	/**
	 * Converts a datetime widget to a date object only setting hour, minute,
	 * seconde
	 */
	public static Date getTime(DateTime dt) {
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(0);
		calendar.set(Calendar.HOUR_OF_DAY, dt.getHours());
		calendar.set(Calendar.MINUTE, dt.getMinutes());
		calendar.set(Calendar.SECOND, dt.getSeconds());
		return calendar.getTime();
	}

	/**
	 * Gets the time midnight
	 * @return
	 */
	public static Date getMidnight() {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		return cal.getTime();
	}

	/**
	 * converts a Date to a Calendar object
	 */
	public static GregorianCalendar convertDate(Date d) {
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(d);
		return (GregorianCalendar)calendar;

	}

	public static String getDirectoryPath(byte[] uuid) {
		return Arrays.toString(uuid).replaceAll(", ", "").replace("[", "")
				.replace("]", "");
	}

	public static String getUuidAsString(byte[] uuid) {
		return Arrays.toString(uuid).replaceAll(", ", "").replace("[", "")
				.replace("]", "").replace("-", "_");
	}

	/**
	 * Crestes the given directory.  
	 * @param dir the directory to created
	 * @return <code>false</code> if not created, <code>true</code> otherwise
	 */
	public static boolean createDirectory(File dir) {
		try {
			FileUtils.forceMkdir(dir);
			return true;
		} catch (IOException ex) {
			SmartPlugIn
					.displayLog(
							null,
							"Could not create directory.  Attachments will not be imported.",
							ex);
		}
		return false;
	}

	/**
	 * Copies a file to a new file
	 * @param from file to copy from
	 * @param to file to copy to
	 * @return
	 */
	public static boolean copyFile(File from, File to) {
		// if (true) return false;
		try {
			FileUtils.copyFile(from, to);
			return true;
		} catch (IOException e) {
			SmartPlugIn.displayLog(null,
					"Could not copy file " + from.getAbsolutePath() + " to "
							+ to.getAbsolutePath() + ". ", e);
		}
		return false;
	}

	/**
	 * Encodes a byte array has a hex string. This is adapted from the Apache
	 * commons HEX class.
	 * 
	 * @param data
	 *            input byte array
	 * @return hex encoded string
	 */
	public static String encodeHex(byte[] data) {
		if (data == null) return "";
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

	/**
	 * Adapted from apache-commons-codec HEX class.
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] decodeHex(String str) throws Exception {
		char[] data = str.toCharArray();
		int len = data.length;

		if ((len & 0x01) != 0) {
			throw new IllegalStateException("Odd number of characters.");
		}

		byte[] out = new byte[len >> 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; j < len; i++) {
			int f = toDigit(data[j], j) << 4;
			j++;
			f = f | toDigit(data[j], j);
			j++;
			out[i] = (byte) (f & 0xFF);
		}

		return out;
	}

	/**
	 * 
	 * Adapted from apache-commons-codec HEX class. Converts a hexadecimal
	 * character to an integer.
	 * 
	 * @param ch
	 *            A character to convert to an integer digit
	 * @param index
	 *            The index of the character in the source
	 * @return An integer
	 * @throws DecoderException
	 *             Thrown if ch is an illegal hex character
	 */
	protected static int toDigit(char ch, int index) throws Exception {
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new IllegalStateException("Illegal hexadecimal character "
					+ ch + " at index " + index);
		}
		return digit;
	}
	
	/**
	 * Creates a new conservation area with default language
	 * setup.  Has no name, id or any other values;
	 * 
	 * @return conservation area 
	 */
	public static ConservationArea createConservationArea(){
		//create new CA with single default language
		ConservationArea newCa = new ConservationArea();
		
		Language lang = new Language();
		lang.setCa(newCa);
		String code = Platform.getNL();
		lang.setCode(code);
		lang.setName(SmartPlugIn.lookupLocaleName(code));
		lang.setDefault(true);
		
		HashSet<Language> langs = new HashSet<Language>();
		langs.add(lang);
		
		newCa.setLanguages(langs);
		return newCa;
	}
	
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
		if (str.charAt(0)=='"' && str.charAt(str.length()-1) == '"'){
			return str.substring(1, str.length() - 1);
		}
		return str;
		
	}
	
	/**
	 * check a string:
	 * 1) only contains letters number and a few other common characters like spaces and _ - : & 
	 * 2) is between 1-64 chars long 
	 * 
	 * @param str - the string which to validate
	 * @return true if 
	 */

	public static Boolean isSimpleString (String str, regExLevel level, Integer maxChars, Integer minChar){
		Pattern p = Pattern.compile( level.regex , Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(str);
		boolean b = m.find();


		if (b || str.length() > maxChars || str.length() < minChar ){
			return false;
		}else{ 
			return true;
		}
		
	}
	//overloaded function to allow calls without passing last parameter
	public static Boolean isSimpleString (String str, regExLevel level, Integer maxChars){
		return isSimpleString(str, level, maxChars,1);
	}
	//overloaded function to allow calls without passing last 3 parameters
	public static Boolean isSimpleString (String str){
		return isSimpleString(str, SmartUtils.regExLevel.ALLOWED_CHARS_MED_REGEX, 64,1);
	}
	//overloaded function to allow calls without passing maxchars or minchars
	public static Boolean isSimpleString (String str, regExLevel level){
		return isSimpleString(str, level, 64, 1);
	}
}
