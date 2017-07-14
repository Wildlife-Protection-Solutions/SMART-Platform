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
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.io.FileUtils;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * General utility functions.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartUtils {

	/**
	 * Various pre-defined regex expressions
	 * for validating strings in smart.
	 */
	public static enum RegExLevel{
		/**
		 * Allows only letters and digits (a-Z and 0-9)
		 */
		ALLOWED_CHARS_SIMPLE_REGEX(Messages.SmartUtils_ValidationMessage_1, "[^\\p{L}\\p{M}\\p{Nd}-]"),  //\p{L} - letters \p{Nd} - digits	is a digit zero through nine //$NON-NLS-1$
		/**
		 * Allows chars, digits, - _ and :
		 */
		ALLOWED_CHARS_MED_REGEX(Messages.SmartUtils_ValidationMessage_2, "[^^\\p{L}\\p{M}\\p{Nd}-:_]" ), //$NON-NLS-1$
		/**
		 * Allows chars, digits, _ : & and spaces
		 */
		ALLOWED_CHARS_COMPLEX_REGEX(Messages.SmartUtils_ValidationMessage_3a, "[^^\\p{L}\\p{M}\\p{Nd}- :_&'<>,\\(\\)\\.\\#;\\/]"); //$NON-NLS-1$
		
		public final String textDesc;
		public final String regex;
	
		RegExLevel(String textDesc, String regex){
			this.textDesc = textDesc;
			this.regex = regex;
		}
	}
	
	
	public static NullComparator nullStringCaseInsensitiveComparator = new NullComparator(new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
	});
	
	public static NullComparator nullIntegerComparator = new NullComparator(new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o1.compareTo(o2);
		}
	});
	public static NullComparator nullLongComparator = new NullComparator(new Comparator<Long>() {
		@Override
		public int compare(Long o1, Long o2) {
			return o1.compareTo(o2);
		}
	});
	public static NullComparator nullDoubleComparator = new NullComparator(new Comparator<Double>() {
		@Override
		public int compare(Double o1, Double o2) {
			return o1.compareTo(o2);
		}
	});
	
	public static NullComparator nullFloatComparator = new NullComparator(new Comparator<Float>() {
		@Override
		public int compare(Float o1, Float o2) {
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
	
	/**
	 * Converts a datetime widget to a date object only setting year, month and
	 * day. The hour, minute, second and millisecond are all set to 0;
	 * 
	 */
	public static Date getDate(DateTime dt) {
		return getCalendar(dt).getTime();
	}

	/**
	 * Converts a date time widget to a date object only setting year
	 * month and day.  The hour, minute, second and millisecond are all set to 0
	 * 
	 * @param dt
	 * @return
	 */
	public static Calendar getCalendar(DateTime dt){
		
		//DateTime widget seems to always return data in Gregorian calendar
		Calendar calendar = new GregorianCalendar();
		
		calendar.set(Calendar.YEAR, dt.getYear());
		calendar.set(Calendar.MONTH, dt.getMonth());
		calendar.set(Calendar.DAY_OF_MONTH, dt.getDay());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		//return calendar in correct locale
		Calendar current = Calendar.getInstance();
		current.setTime(calendar.getTime());
		return current;
	}

	/**
	 * initialized the date fields of a datetime widget
	 * @param dtWidget
	 * @param date
	 */
	public static void initDateDateTimeWidget(DateTime dtWidget, Date date){
		//datetime widget uses gregorian calendar for fields
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		
		dtWidget.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
	}
	
	/**
	 * initialized the time fields of a datetime widget
	 * @param dtWidget
	 * @param date
	 */
	public static void initTimeDateTimeWidget(DateTime dtWidget, Date date){
		//datetime widget uses gregorian calendar for fields
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		dtWidget.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
	}
	
	

	/**
	 * Gets only the date part of a given date. Sets the time to 0.
	 * 
	 * @param dt
	 * @return date only date
	 */
	public static Date combineDateTime(Date date, Date time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTime(time);

		calendar.set(Calendar.HOUR_OF_DAY, calendar2.get(Calendar.HOUR_OF_DAY));
		calendar.set(Calendar.MINUTE, calendar2.get(Calendar.MINUTE));
		calendar.set(Calendar.SECOND, calendar2.get(Calendar.SECOND));
		calendar.set(Calendar.MILLISECOND, calendar2.get(Calendar.MILLISECOND));

		return calendar.getTime();
	}

	/**
	 * Combines the date and time into a single object
	 * 
	 * @param dt
	 * @return date only date
	 */
	public static Date combineDateTime(Date date, Time time) {
		return combineDateTime(date, new Date(time.getTime()));
	}

	/**
	 * Converts a datetime widget to a date object only setting hour, minute,
	 * seconde
	 */
	public static Date getTime(DateTime dt) {
		Calendar calendar = Calendar.getInstance();
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
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		return cal.getTime();
	}


	/**
	 * Creates the given directory.  
	 * @param dir the directory to created
	 * @return <code>false</code> if not created, <code>true</code> otherwise
	 */
	public static boolean createDirectory(final File dir) {
		try {
			FileUtils.forceMkdir(dir);
			return true;
		} catch (final IOException ex) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn
					.displayLog(
							Messages.SmartUtils_Error_CouldNotCreateDir + dir.getAbsolutePath(),
							ex);
				}});
			
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
			SmartPlugIn.displayLog(MessageFormat.format(
					Messages.SmartUtils_Error_CouldNotCopy,
					new Object[]{from.getAbsolutePath(), to.getAbsolutePath()}), e);
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
	public static String encodeGeometry(byte[] data) {
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

	/**
	 * Adapted from apache-commons-codec HEX class.
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] decodeGeometry(String str) throws Exception {
		char[] data = str.toCharArray();
		int len = data.length;

		if ((len & 0x01) != 0) {
			throw new IllegalStateException(Messages.SmartUtils_Illegal_HexString);
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
			throw new IllegalStateException(
					MessageFormat.format(Messages.SmartUtils_Illegal_HexChar,
							new Object[]{String.valueOf(ch),String.valueOf(index)}));
		}
		return digit;
	}
	
	/**
	 * Creates a new conservation area with not name, id
	 * languages or other values.
	 * 
	 * @return conservation area 
	 */
	public static ConservationArea createConservationArea(){
		
		ConservationArea newCa = new ConservationArea();
		
		return newCa;
	}
	
	/**
	 * Checks a string:
	 * <ol>
	 * <li> only contains letters number and a few other common characters like spaces and _ - : & 
	 * <li>is between 1-64 chars long 
	 * </ol>
	 * @param str - the string which to validate
	 * @param level the regex level to match to
	 * @param maxChars the maximum number of characters in the string
	 * @param minChar the minimum number of characters
	 * @return <code>true</code> if matches the required parameters, <code>false</code> otherwise 
	 */
	public static Boolean isSimpleString (String str, RegExLevel level, 
			Integer maxChars, Integer minChar){
		Pattern p = Pattern.compile( level.regex , Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(str);
		boolean b = m.find();
		if (b || str.length() > maxChars || str.length() < minChar ){
			return false;
		}else{ 
			return true;
		}		
	}

	/**
	 * Determines if a string matches the given requirements and is
	 * at least one character long.
	 * 
	 * @param str the string to match
	 * @param level the regexlevel to match it to
	 * @param maxChars the maximum number of characters
	 * @return <code>true</code> if meets the above requirements, <code>false</code> otherwise
	 */
	public static Boolean isSimpleString (String str, RegExLevel level, Integer maxChars){
		return isSimpleString(str, level, maxChars,1);
	}
	
	/**
	 * Determines if a string is at least one character long,
	 * no longer than 64 characters, and matches the 
	 * ALLOWED_CHARS_MED_REGEX expression.
	 * 
	 * @param str the string to match
	 * @return <code>true</code> if meets the above requirements, <code>false</code> otherwise
	 */
	public static Boolean isSimpleString (String str){
		return isSimpleString(str, SmartUtils.RegExLevel.ALLOWED_CHARS_MED_REGEX, 64,1);
	}

	/**
	 * Determines if a string is at least one character long,
	 * no longer than 64 characters, and macthes the 
	 * provided expression.
	 * 
	 * @param str the string to match
	 * @param level the regex level to match to
	 * @return <code>true</code> if meets the above requirements, <code>false</code> otherwise
	 */
	public static Boolean isSimpleString (String str, RegExLevel level){
		return isSimpleString(str, level, 64, 1);
	}
	
	/**
	 * Counts all the files in a directory including
	 * all sub-directories.
	 * 
	 * @param directory the directory to start at
	 * @return the total number of files
	 */
	public static int countFiles(File directory){
		int count = 0;
		for (File f : directory.listFiles()){
			if (f.isFile()){
				count++;
			}else{
				count += countFiles(f);
			}
		}
		return count;
	}
	
	/**
	 * Creates a temporary directory.
	 * 
	 * @return the temporary directory 
	 */
	public static File createTemporaryDirectory(){
		File baseDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		String basename = "smart_" + Long.toString(System.nanoTime()); //$NON-NLS-1$
		
		for (int i = 0; i < 1000; i ++){
			File tempDir = new File(baseDir, basename + "_"+ i); //$NON-NLS-1$
			if (tempDir.mkdir()){
				return tempDir;
			}
		}
		throw new IllegalStateException(Messages.SmartUtils_Error_CouldNotCreateTempDir);
		
	}
	
	/**
	 * Determine if the given file is a zip file or not.
	 * 
	 * @param file the file to check
	 * @return <code>true</code> if file is zip file, <code>false</code> otherwise
	 */
	public static boolean isZip(File file){
		try(ZipFile zout = new ZipFile(file)){
			zout.entries();
			zout.close();
			return true;
		}catch (Exception ex){
			
		}
		return false;
		
	}
	
	/**
	 * Formats a string for placement in an swt label.
	 * <p>
	 * Replaces all & with &&
	 * </p>
	 * @param text
	 * @return
	 */
	public static String formatStringForLabel(String text){
		if (text == null) return ""; //$NON-NLS-1$
		return text.replaceAll("&", "&&");  //$NON-NLS-1$//$NON-NLS-2$
	}

	
	/**
	 * Finds the language that best matches the current
	 * logged in language.
	 * <p>
	 * Looks first for codes (language & locale) that fully match,
	 * then codes whose first (language) part matches.  If
	 * not found returns null.
	 * 
	 * @param languages
	 * @return
	 */
	public static Language findLanguageMatch(Collection<Language> languages){
		String currentCode = SmartDB.getCurrentLanguage().getCode();
		for (Language l : languages){
			if (l.getCode().equals(currentCode)){
				return l;
			}
		}
		String[] bits = currentCode.split("_"); //$NON-NLS-1$
		String lang = bits[0];
		
		for (Language l : languages){
			if (l.getCode().split("_")[0].equals(lang)){ //$NON-NLS-1$
				return l;
			}
		}
		return null;
	}

	/**
	 * Converts a date to an xml date.  Sets the
	 * XML timezone field to undefined so timezone information
	 * is not included in output.
	 * 
	 * @param d
	 * @return
	 * @throws DatatypeConfigurationException
	 */
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

	/**
	 * Determines the output file name for given smart object name by
	 * removing characters from the name that are not valid for file name
	 * 
	 * @param smartName
	 * @return
	 */
	public static String getFileName(String smartName) {
		//remove all special characters from the name
		smartName = URLUtils.cleanFilename(smartName);
		if (smartName.isEmpty()) {
			smartName = "object"; //$NON-NLS-1$
		}
		return smartName;
	}
	
	public static String replaceAll(String toReplace, String search, String replace){
		return toReplace.replaceAll(search, replace);
	}
	
	public static Transform getExifImageTransform(File file, int width, int height){
		
		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata(file.getAbsoluteFile());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
		int orientation = 1;
		try {
			if (exifIFD0Directory != null && exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)){
				orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			}
		} catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return null;
		}	
		
		Transform imageTransform = new Transform(Display.getDefault());
		switch (orientation) {
		case 1:
			return null; //default
		case 2: // Flip X
			imageTransform.scale(-1.0f, 1.0f);
			imageTransform.translate(-width, 0);
			break;
		case 3: // PI rotation
			imageTransform.translate(width, height);
			imageTransform.rotate(180f);
			break;
		case 4: // Flip Y
			imageTransform.scale(1.0f, -1.0f);
			imageTransform.translate(0, -height);
			break;
		case 5: // - PI/2 and Flip X
			imageTransform.rotate(-90f);
			imageTransform.scale(-1.0f, 1.0f);
			break;
		case 6: // -PI/2 and -width
			imageTransform.translate(height, 0);
			imageTransform.rotate(90);
			break;
		case 7: // PI/2 and Flip
			imageTransform.scale(-1.0f, 1.0f);
			imageTransform.translate(-height, 0);
			imageTransform.translate(0, width);
			imageTransform.rotate(270f);
			break;
		case 8: // PI / 2
			imageTransform.translate(0, width);
			imageTransform.rotate(270f);
			break;
		default:
			return null;
		}
		return imageTransform;
	}
}
