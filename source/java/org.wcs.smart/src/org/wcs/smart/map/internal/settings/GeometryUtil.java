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
package org.wcs.smart.map.internal.settings;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * This utility class offers convenient method to handle geometries
 * 
 * @author Mauricio Pazos
 *
 */
final class GeometryUtil {

	
	private GeometryUtil(){
		// utility class
	}
	

	/**
	 * In this application the Envelop is encode has WKT using the Multipoint syntax. 
	 * @return envelop as WKT MULTIPOINT ( ( x y), (x y) )
	 * @throws ParseException 
	 */
	public static String envelopToWKT(Envelope envelope) throws ParseException{

		StringBuilder sb = new StringBuilder(40);

		sb.append("MULTIPOINT ((")
				.append(envelope.getMaxX()).append(" ").append(envelope.getMaxY())
				.append("),(")
				.append(envelope.getMinX()).append(" ").append(envelope.getMinY())
				.append("))");

		return sb.toString();
	}
	
	
	/**
	 * Transforms the envelop, encoded as MULTIPOINT, to an Envelop object
	 * 
	 * @param wktEnvelope MULTIPOINT ( ( x y), (x y) )
	 * @return {@link Envelope}
	 * @throws ParseException 
	 */
	public static Envelope wktToEnvelop(final String wktEnvelope) throws ParseException{
		
		WKTReader wkt = new WKTReader();
		MultiPoint mp = (MultiPoint) wkt.read(wktEnvelope);

		assert mp.getCoordinates().length == 2;
		
		Coordinate p1 = mp.getCoordinates()[0];
		Coordinate p2 = mp.getCoordinates()[1];
		
		Envelope envelope = new Envelope(p1, p2);
		
		return envelope;
		
	}
	
	
}
