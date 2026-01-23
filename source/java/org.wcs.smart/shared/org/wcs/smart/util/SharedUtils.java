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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.mindrot.jbcrypt.BCrypt;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

/**
 * Collection of utilities shared with Desktop and Connect applications.
 * 
 * @author Emily
 *
 */
public class SharedUtils {
	
	public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	//localtime.max gets converted to 23:59:59.99999 which in date/time gets
	//converted to 00:00 so calculations end up incorrect
	public static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);
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

	
	/**
	 * converts the z value from a track coordinate to a local date time
	 * @param z
	 * @return
	 */
	public static LocalDateTime toLocalDateTime(long z) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(z), ZoneOffset.UTC);
	}
	/**
	 * converts the z value from a track coordinate to the local date time
	 * @param c
	 * @return
	 */
	public static LocalDateTime toLocalDateTime(Coordinate c) {
		return toLocalDateTime((long)c.getZ());
	}
	/**
	 * converts a localdatetime to the z value for track coordinate
	 * @param z
	 * @return
	 */
	public static long toLongTime(LocalDateTime datetime) {
		return datetime.toInstant(ZoneOffset.UTC).toEpochMilli();		
	}
	
	/**
	 * generate a bcrypt password from plain-text password
	 * 
	 * @param password
	 * @return
	 */
	public static String generatePassword(String password){
		return BCrypt.hashpw(password, BCrypt.gensalt(13));
	}
	
	public static ReferencedEnvelope computeBounds(FeatureSource<SimpleFeatureType, SimpleFeature> fs) throws IOException {
		
		final ReferencedEnvelope env = new ReferencedEnvelope(fs.getSchema().getCoordinateReferenceSystem());
		env.setToNull();
		fs.getFeatures().accepts(new FeatureVisitor() {
			@Override
			public void visit(Feature f) {
				BoundingBox bb = f.getBounds();
				if (env.isNull()) {
					env.init(bb);
				}else {
					env.include(bb);
				}
			}
		}, null);
		if (!env.isNull() && env.isEmpty()) env.expandBy(0.001);
		return env;
	}
}
