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

import java.awt.image.BufferedImage;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.geotools.util.factory.GeoTools;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.filter.FilterFactory;
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
	
	public static NullComparator nullDateComparator = new NullComparator(new Comparator<LocalDate>() {
		@Override
		public int compare(LocalDate o1, LocalDate o2) {
			return o1.compareTo(o2);
		}
	});

	/**
	 * Converts DateTime ui widgets to a LocalDateTime 
	 * 
	 * @param date date widget
	 * @param time time widget
	 * @return
	 */
	public static LocalDateTime toDateTime(DateTime date, DateTime time) {
		LocalDateTime ldt = LocalDateTime.of(date.getYear(), date.getMonth()+1, date.getDay(), 
				time.getHours(), time.getMinutes(), time.getSeconds());
		return ldt;
		
	}
	
	/**
	 * Converts DateTime widget to LocalDate
	 * @param date
	 * @return
	 */
	public static LocalDate toDate(DateTime date) {
		return LocalDate.of(date.getYear(), date.getMonth()+1, date.getDay());
	}
	
	/**
	 * Converts DateTime widget to LocalTime
	 * @param date
	 * @return
	 */
	public static LocalTime toTime(DateTime date) {
		return LocalTime.of(date.getHours(), date.getMinutes(), date.getSeconds());
	}
	
	/**
	 * Initializes DateTime widget with LocalDate and LocalTime
	 * @param dtWidget
	 * @param date
	 * @param time
	 */
	public static void initDateTimeWidget(DateTime dtWidget, LocalDate date, LocalTime time){
		initDateTimeWidget(dtWidget, date);
		initDateTimeWidget(dtWidget, time);
	}
	
	/**
	 * Initializes DateTime widget with LocalDate
	 * @param dtWidget
	 * @param date
	 */
	public static void initDateTimeWidget(DateTime dtWidget, LocalDate date){
		dtWidget.setDate(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth());
	}
	
	/**
	 * Initializes DateTime widget with LocalTime
	 * @param dtWidget
	 * @param time
	 */
	public static void initDateTimeWidget(DateTime dtWidget, LocalTime time){
		dtWidget.setTime(time.getHour(), time.getMinute(), time.getSecond());
	}

	/**
	 * Delete directory and all files within the directory
	 * @param dir
	 * @throws IOException
	 */
	public static void deleteDirectory(final Path dir) throws IOException { 
		if (!Files.exists(dir)) return;

		try (Stream<Path> tree = Files.walk(dir).sorted(Comparator.reverseOrder())) {
			 Iterator<Path> i = tree.iterator();
			 while(i.hasNext()) {
				 Path next = i.next();
				 Files.delete(next);
			 }
		 }
	}

	public static void copyDirectory(Path source, Path target) throws IOException {
		try (Stream<Path> tree = Files.walk(source)) {
		    Iterator<Path> i = tree.iterator();
		    while (i.hasNext()) {
		        Path sourceItem = i.next();
		        Path dest = target.resolve(source.relativize(sourceItem));
		        if (Files.isDirectory(sourceItem)) {
		            Files.createDirectories(dest);
		        } else {
		            Files.copy(sourceItem, dest);
		        }
		    }
		}

	}
	
	public static void moveDirectory(Path source, Path target) throws IOException {
		//SmartPlugIn.log("moving from " + source.toString() + " to " + target.toString(), null);
		//this doesn't work when moving between drives, so use copy and delete instead
		//https://app.asana.com/0/1199928770649381/1201025180297056
		//Files.move(source, target);		
		Files.walkFileTree(source, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(target.resolve(source.relativize(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, target.resolve(source.relativize(file)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				if (exc != null) SmartPlugIn.log(exc.getMessage(), exc);
				return FileVisitResult.TERMINATE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) SmartPlugIn.log(exc.getMessage(), exc);
				return FileVisitResult.CONTINUE;
			}});
		
		deleteDirectory(source);

	}
	
	
	
	/**
	 * Creates the given directory.  
	 * @param dir the directory to created
	 * @return <code>false</code> if not created, <code>true</code> otherwise
	 */
	public static boolean createDirectory(final Path dir) {
		try {
			Files.createDirectories(dir);
			return true;
		} catch (final IOException ex) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn
					.displayLog(Messages.SmartUtils_Error_CouldNotCreateDir + dir.toAbsolutePath().toString(),ex);
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
	public static boolean copyFile(Path from, Path to, CopyOption... options) {
		try {
			Files.createDirectories(to.getParent());
			Files.copy(from, to, options);
			return true;
		} catch (IOException e) {
			SmartPlugIn.displayLog(MessageFormat.format(
					Messages.SmartUtils_Error_CouldNotCopy,
					new Object[]{from.toAbsolutePath().toString(), to.toAbsolutePath().toString()}), e);
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
	 * Creates a temporary directory.
	 * 
	 * @return the temporary directory 
	 */
	public static Path createTemporaryDirectory(){
		Path baseDir = Paths.get(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		String basename = "smart_" + Long.toString(System.nanoTime()); //$NON-NLS-1$
		
		for (int i = 0; i < 1000; i ++){
			Path tempDir = baseDir.resolve(basename + "_"+ i); //$NON-NLS-1$
			if (createDirectory(tempDir)) return tempDir;
		}
		throw new IllegalStateException(Messages.SmartUtils_Error_CouldNotCreateTempDir);
		
	}
	
	/**
	 * Determine if the given file is a zip file or not.
	 * 
	 * @param file the file to check
	 * @return <code>true</code> if file is zip file, <code>false</code> otherwise
	 */
	public static boolean isZip(Path file){
		try(ZipFile zout = new ZipFile(file.toAbsolutePath().toFile())){
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
	public static XMLGregorianCalendar toXmlDate(LocalDate d) throws DatatypeConfigurationException {
		if (d == null) return null;
		
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(
				d.getYear(), d.getMonthValue(),d.getDayOfMonth(),
				0,0,0,
				DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
	}

	/**
	 * Converts localtime to xml time
	 * @param d
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	public static XMLGregorianCalendar toXmlTime(LocalTime d) throws DatatypeConfigurationException{
		
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(
				DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
				d.getHour(), d.getMinute(), d.getSecond(),
				DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
		
	}
	
	/**
	 * Converts localdatetime to xml date without timezone
	 * @param d
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	public static XMLGregorianCalendar toXmlDateTime(LocalDateTime d) throws DatatypeConfigurationException {
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(
				d.getYear(), d.getMonthValue(),d.getDayOfMonth(),
				d.getHour(), d.getMinute(), d.getSecond(),
				DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
	}
	
	/**
	 * For exporting to GPX files, coverts the LocalDateTime to a xml time with timezone
	 * @param d
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	public static XMLGregorianCalendar toXmlDateTimeUTC(LocalDateTime d) throws DatatypeConfigurationException {

		return DatatypeFactory.newInstance().newXMLGregorianCalendar(
				d.getYear(), d.getMonthValue(),d.getDayOfMonth(),
				d.getHour(), d.getMinute(), d.getSecond(),
				DatatypeConstants.FIELD_UNDEFINED,
				ZoneOffset.systemDefault().getRules().getOffset(d).getTotalSeconds() / 60);
		
	}
	
	public static LocalDate toLocalDate(XMLGregorianCalendar d) {
		return d.toGregorianCalendar().toZonedDateTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
	}
	
	public static LocalTime toLocalTime(XMLGregorianCalendar d) {
		return d.toGregorianCalendar().toZonedDateTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalTime();
	}
	
	public static LocalDateTime toLocalDateTime(XMLGregorianCalendar d) {
		return d.toGregorianCalendar().toZonedDateTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
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
	
	/**
	 * Get the rotation in degrees of the image from the metadata.  Will return
	 * 0 if it cannot be found.
	 *   
	 * @param file
	 * @param width
	 * @param height
	 * @return scalex, scaley, rotation - assumption is the scaling is applied before rotation
	 */
	public static double[] getExifRotation(Path file){
		
		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata(file.toAbsolutePath().toFile());
		} catch (Exception e) {
			return new double[] {0,1,1};
		}
		ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
		int orientation = 1;
		try {
			if (exifIFD0Directory != null && exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)){
				orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			}
		} catch (Exception ex) {
			return new double[] {0,1,1};
		}	
		
		switch (orientation) {
		case 1:
			return new double[] {0,1,1}; //default
		case 2: // Flip X
			return new double[] {0,-1,1};
		case 3: // PI rotation
			return new double[] {180,1,1};
		case 4: // Flip Y
			return new double[] {0,1,-1};
		case 5: // - PI/2 and Flip X
			return new double[] {90,1,-1};
		case 6: // -PI/2 and -width 
			return new double[] {90,1,1};
		case 7: // PI/2 and Flip
			return new double[] {270,1,-1};
		case 8: // PI / 2
			return new double[] {270,1,1};
		default:
			return new double[] {0,1,1};
		}
	}

	public static Transform getExifImageTransform(InputStream is, int width, int height){
		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata(is);
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
	
	public static Transform getExifImageTransform(Path file, int width, int height) throws IOException{
		try(InputStream is = Files.newInputStream(file)){
			return getExifImageTransform(is, width, height);
		}
	}
	
	/**
	 * Returns a color that represent the color of list items when
	 * users mouse over the item
	 * 
	 * User must dispose of color when finished with it.
	 * @param d
	 * @return
	 */
	public static Color getListHighlightColor(Display display) {
		return new Color(display, blend(new RGB(255, 255, 255), display.getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB(), 90));
	}
	
	/**
	 * Returns a color that represent the color of list items when
	 * users selects an item
	 * 
	 * User must dispose of color when finished with it.
	 * @param d
	 * @return
	 */
	public static Color getListSelectedColor(Display display) {
		return new Color(display, blend(new RGB(255, 255, 255), display.getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB(), 75));
	}

	public static RGB blend(RGB c1, RGB c2, int ratio) {
		int r = blend(c1.red, c2.red, ratio);
		int g = blend(c1.green, c2.green, ratio);
		int b = blend(c1.blue, c2.blue, ratio);
		return new RGB(r, g, b);
	}
	 /**
     * Blends c1 and c2 based in the provided ratio.
     * 
     * @param c1
     *            first color
     * @param c2
     *            second color
     * @param ratio
     *            percentage of the first color in the blend (0-100)
     * @return the RGB value of the blended color
     */
	private static int blend(int v1, int v2, int ratio) {
		int b = (ratio * v1 + (100 - ratio) * v2) / 100;
		return Math.min(255, b);
	}
	
	
	/**
	 * Reads an svg file into an SWT image. User must correctly dispose of the image
	 * when done with it.
	 * 
	 * @param display
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws TranscoderException
	 */
	public static Image readSvg(Display display, Path file, Integer size) throws IOException, TranscoderException {
		try(InputStream reader = Files.newInputStream(file)){
			return readSvg(display, reader, size);
		}catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}
	
	/**
	 * Loads the non text svg image into a local image object for the given size
	 * 
	 * @param display
	 * @param size size of image
	 * @return the converted image or null if error occurs
	 * @throws IOException
	 * @throws TranscoderException
	 */
	public static Image getSvgLogoNoText(Display display, Integer size) {
		try(InputStream reader = SmartPlugIn.class.getResourceAsStream("/images/smartonly.svg")){ //$NON-NLS-1$
			return readSvg(display, reader, size);
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return null;
	}
	
	public static Image readSvg(Display display, InputStream is, Integer size) throws IOException, TranscoderException {
		PNGTranscoder transcoder = new PNGTranscoder() {
			//hack because teh transcoder batik jar doesn't require the codec jar which contains
			//the PNGTranscoderInternalCodecWriteAdapter; so the osgi class loader fails 
			public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException {

				OutputStream ostream = output.getOutputStream();
				if (ostream == null) {
			            throw new TranscoderException("Invalid output stream"); //$NON-NLS-1$
				}

				//
				// This is a trick so that viewers which do not support the alpha
				// channel will see a white background (and not a black one).
				//
				boolean forceTransparentWhite = false;

				if (hints.containsKey(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE)) {
					forceTransparentWhite = (Boolean) hints.get(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE);
				}

				if (forceTransparentWhite) {
					SinglePixelPackedSampleModel sppsm;
					sppsm = (SinglePixelPackedSampleModel) img.getSampleModel();
					forceTransparentWhite(img, sppsm);
				}

				try {
					Class<?> clazz = Class
							.forName("org.apache.batik.ext.awt.image.codec.png.PNGTranscoderInternalCodecWriteAdapter"); //$NON-NLS-1$
					WriteAdapter adapter = (WriteAdapter) clazz.getDeclaredConstructor().newInstance();
					if (adapter == null) {
						throw new TranscoderException("Could not write PNG file because no WriteAdapter is availble"); //$NON-NLS-1$
					}
					adapter.writeImage(this, img, output);
				} catch (TranscoderException e) {
					throw e;
				} catch (Exception e) {
					throw new TranscoderException(e);
				}

			}
		};
		
		if (size != null) {
			transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float)size);
			transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float)size);
		}
		
		TranscoderInput input = new TranscoderInput(is);
		try(ByteArrayOutputStream bout = new ByteArrayOutputStream()){
			TranscoderOutput output = new TranscoderOutput(bout);
			transcoder.transcode(input, output);
			Image img = new Image(display, new ByteArrayInputStream(bout.toByteArray()));
			return img;
		}
	}
	
	
	/**
	 * Converts the file to an image of the given size.  Works for svg as well as png etc.
	 * Users must dispose of image when done with it.
	 * Assumes svg files are identified by ".svg" extension
	 * @param file 
	 * @param size can be null in which case image is not resized
	 * @return
	 */
	public static Image getImage(Path file, Integer size) {
		if (size != null && size < 0) return null;
		if (file.getFileName().toString().endsWith(".svg")) { //$NON-NLS-1$
			try {
				return readSvg(Display.getDefault(), file, size);
			}catch (Exception ex) {
				return null;
			}
		}
		
		Image rawImage = null;
		try(InputStream is  = Files.newInputStream(file)){
			rawImage = new Image(Display.getDefault(), is); 
		}catch (Exception ex) {
			return null;
		}
		
		//resize/scale
		Rectangle bounds = rawImage.getBounds();
		
		int sizeW = size == null ? bounds.width : size;
		int sizeH = size == null ? bounds.height : size;
		
		int x = 0, y = 0, width = 0, height = 0;
		if (bounds.width > bounds.height) {
			width = sizeW;
			height = bounds.height * sizeW / bounds.width;
			y = (sizeW - height) / 2;
		} else {
			height = sizeH;
			width = bounds.width * sizeH / bounds.height;
			x = (sizeH - width) / 2;
		}
		
		
		try {
			//check fo exif metadata for transform
			Transform imageTransform = SmartUtils.getExifImageTransform(file, sizeW, sizeH);
		 	
		 		try {
		 			if (imageTransform != null) {
						Image image3 = new Image(Display.getDefault(), sizeW, sizeH);
						GC gc3 = new GC(image3);
						try {
							gc3.setTransform(imageTransform);
							gc3.drawImage(rawImage, 0, 0, rawImage.getBounds().width, rawImage.getBounds().height, x, y, width, height);
						}finally {
							gc3.dispose();
							rawImage.dispose();
						}
						return image3;
		 			}
		 		}finally {
		 			if (imageTransform != null) imageTransform.dispose();
		 		}
			
		}catch (Exception ex) {
			
		}
		
		if (size == null) {
			return rawImage;
		}
				
		// resize image
		Image image2 = new Image(Display.getDefault(), sizeW, sizeH);
		GC gc = new GC(image2);
		try {
			gc.drawImage(rawImage, 0, 0, bounds.width, bounds.height, x, y, width, height);
		}finally {
			gc.dispose();
			rawImage.dispose();
		}
		
		return image2;
	}
	
	
	/**
	 * Reads an image from a url resizing to the provided size.  If size
	 * is null then the full image size is returned
	 * 
	 * @param url
	 * @param size
	 * @return
	 */
	public static Image getImage(URL url, Integer size) {
		
		if (url.toString().endsWith(".svg")) { //$NON-NLS-1$
			try(InputStream is = url.openConnection().getInputStream()){
				return readSvg(Display.getDefault(), is, size);
			}catch (Exception ex) {
				return null;
			}
		}
		
		Image rawImage = null;
		try(InputStream is = url.openConnection().getInputStream()){
			rawImage = new Image(Display.getDefault(), is); 
		}catch (Exception ex) {
			return null;
		}
		
		//resize/scale
		Rectangle bounds = rawImage.getBounds();
		
		int sizeW = size == null ? bounds.width : size;
		int sizeH = size == null ? bounds.height : size;
		
		int x = 0, y = 0, width = 0, height = 0;
		if (bounds.width > bounds.height) {
			width = sizeW;
			height = bounds.height * sizeW / bounds.width;
			y = (sizeW - height) / 2;
		} else {
			height = sizeH;
			width = bounds.width * sizeH / bounds.height;
			x = (sizeH - width) / 2;
		}
		
		//check fo exif metadata for transform
		Transform imageTransform = null;
		try(InputStream is = url.openConnection().getInputStream()){
			imageTransform = SmartUtils.getExifImageTransform(is, sizeW, sizeH);
		}catch (IOException ex) {
			
		}
		if (imageTransform != null) {
			try {
				Image image3 = new Image(Display.getDefault(), sizeW, sizeH);
				GC gc3 = new GC(image3);
				try {
					gc3.setTransform(imageTransform);
					gc3.drawImage(rawImage, 0, 0, rawImage.getBounds().width, rawImage.getBounds().height, x, y, width, height);
					imageTransform.dispose();
				}finally {
					gc3.dispose();
					rawImage.dispose();
				}
				return image3;
			}finally {
				imageTransform.dispose();
			}
		}
		
		if (size == null) return rawImage;
				
		// resize image
		Image image2 = new Image(Display.getDefault(), sizeW, sizeH);
		GC gc = new GC(image2);
		try {
			gc.drawImage(rawImage, 0, 0, bounds.width, bounds.height, x, y, width, height);
		}finally {
			gc.dispose();
			rawImage.dispose();
		}
		return image2;
		
	}
	
	public static Style getDefaultPrjWaypointStyle() {
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		StyleBuilder sb = new StyleBuilder(sf);
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        
		Stroke linestroke = sb.createStroke(new java.awt.Color(91, 91, 91), 1., new float[] {5.0f, 2.0f});
		LineSymbolizer lines = sb.createLineSymbolizer(linestroke);
		
		Stroke circlestroke = sb.createStroke(new java.awt.Color(0,0,0), 1);
		Fill circlefill = sb.createFill(new java.awt.Color(255,100,100));
		Mark circlemark = sb.createMark(sb.literalExpression("circle"), circlefill, circlestroke); //$NON-NLS-1$
		Graphic circleg = sb.createGraphic(null,  circlemark,  null);
		circleg.setSize(sb.literalExpression(8));
        PointSymbolizer endpoint = sb.createPointSymbolizer(circleg);
		endpoint.setGeometry(ff.function("endPoint", ff.property("the_geom")));  //$NON-NLS-1$ //$NON-NLS-2$
		
		Fill squarefill = sb.createFill(new java.awt.Color(91, 91, 91));
		Mark squaremark = sb.createMark(sb.literalExpression("square"), squarefill, null); //$NON-NLS-1$
		Graphic squareg = sb.createGraphic(null,  squaremark,  null);
		squareg.setSize(sb.literalExpression(8));
        PointSymbolizer startpoint = sb.createPointSymbolizer(squareg);
        startpoint.setGeometry(ff.function("startPoint", ff.property("the_geom")));  //$NON-NLS-1$ //$NON-NLS-2$
		
		Rule rr = sb.createRule(new Symbolizer[] {lines, endpoint, startpoint});
		
		org.geotools.styling.FeatureTypeStyle fts = sf.createFeatureTypeStyle();
    	fts.setName("Projection Style"); //$NON-NLS-1$
    	fts.rules().add(rr);
		
		Style style = sf.createStyle();
    	style.featureTypeStyles().add(fts);
		return style;
	}

	public static Style getDefaultWaypointStyle() {
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		StyleBuilder sb = new StyleBuilder(sf);
       
		Stroke starstroke = sb.createStroke(new java.awt.Color(0,0,0), 1);
		Fill starfill = sb.createFill(new java.awt.Color(255,100,100));
		Mark starmark = sb.createMark(sb.literalExpression("circle"), starfill, starstroke); //$NON-NLS-1$
		Graphic starg = sb.createGraphic(null,  starmark,  null);
		starg.setSize(sb.literalExpression(8));
        PointSymbolizer endpoint = sb.createPointSymbolizer(starg);
		
		Rule rr = sb.createRule(new Symbolizer[] {endpoint});
		
		org.geotools.styling.FeatureTypeStyle fts = sf.createFeatureTypeStyle();
    	fts.setName("Waypoint Style"); //$NON-NLS-1$
    	fts.rules().add(rr);
		
		Style style = sf.createStyle();
    	style.featureTypeStyles().add(fts);
		return style;
	}

}
