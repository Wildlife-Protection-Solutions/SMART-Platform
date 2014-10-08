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
package org.wcs.smart.tests;

import javax.sql.rowset.serial.SerialBlob;

import junit.framework.Assert;

import org.junit.Test;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;

@SuppressWarnings({"nls"})
public class GeomUtilsTest {

	@Test
	public void testComputeHours() throws Exception{
		
		String polygon = "POLYGON ((1176327.4927172633 406358.7587904736, 1176327.4927172633 414534.02937861584, 1188456.377770163 414534.02937861584, 1188456.377770163 406358.7587904736, 1176327.4927172633 406358.7587904736))";
		String line = "LINESTRING (1174736.2902668426 416068.46562956925 1, 1175698.71947458 413323.7601112074 2, 1176946.312892017 412575.204060745 3, 1178799.8802550666 414927.8087907695 4, 1180296.9923559914 412860.368270445 5, 1181223.776037516 415106.03642183193 6, 1181330.7126161535 405160.9346085467 7, 1184111.0636607278 415533.78273638187 8, 1184681.3920801277 408083.86775797105 9, 1185928.985497565 408012.57670554606 10, 1186855.7691790897 409901.7895948081 11, 1193949.2288953757 410115.6627520831 12)";
		WKTReader reader = new WKTReader();
		
		WKBWriter writer = new WKBWriter(3);
		byte[] bpolygon = writer.write(reader.read(polygon));
		byte[] bline = writer.write(reader.read(line));
		
		SerialBlob polyBlob =  new SerialBlob(bpolygon);
		SerialBlob lineBlob =  new SerialBlob(bline);
		
		double hours = GeometryUtils.computeHours(polyBlob, lineBlob);
		Assert.assertEquals(7.585078394186732 / 3600000.0, hours, 0.00000000000001);
	}
	
	@Test
	public void testLineStringSplit() throws Exception{
		WKTReader reader = new WKTReader();
		LineString ls = (LineString) reader.read("LINESTRING(0 0, 5 5)");
		
		LineString[] results = GeometryUtils.splitSimple(ls, new Coordinate(2,2));
		System.out.println(results[0].toText());
		Assert.assertEquals(results[0].toText(), "LINESTRING (0 0, 2 2)");
		Assert.assertEquals(results[1].toText(), "LINESTRING (2 2, 5 5)");
		
		
		ls = (LineString) reader.read("LINESTRING(0 0, 0 5)");
		results = GeometryUtils.splitSimple(ls, new Coordinate(2,2));
		Assert.assertEquals(results[0].toText(), "LINESTRING (0 0, 0 2)");
		Assert.assertEquals(results[1].toText(), "LINESTRING (0 2, 0 5)");
	}
}
