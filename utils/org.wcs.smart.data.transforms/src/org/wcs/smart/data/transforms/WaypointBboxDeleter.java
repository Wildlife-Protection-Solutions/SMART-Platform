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
package org.wcs.smart.data.transforms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.dsig.TransformException;

import org.geotools.referencing.GeodeticCalculator;
import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.TrackType;
import org.wcs.smart.patrol.xml.model.WaypointType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * This function finds searches all waypoints and track point to ensure
 * they are inside a bounding box.  Any waypoints or track points outside
 * the bounding box are removed.  The bounding box must
 * be supplied as a parameter to the script in the format
 * "xmin ymin xmax ymax"
 * 
 * @author Emily
 *
 */
public class WaypointBboxDeleter implements IDataProcessor {

	private double xMin = 0;
	private double yMin = 0;
	private double xMax = 0;
	private double yMax = 0;
	
	private void deleteWaypointsTrack(PatrolType patrol) throws Exception{
		
		WKBReader reader = new WKBReader();
		WKBWriter writer = new WKBWriter();
		GeometryFactory gf = new GeometryFactory();
		
		for (PatrolLegType leg : patrol.getLegs()){
			for (PatrolLegDayType day : leg.getDays()){
				List<WaypointType> toRemove = new ArrayList<WaypointType>();
				for (WaypointType wp : day.getWaypoints()){
					if (wp.getX() < xMin || 
							wp.getX() > xMax || 
							wp.getY() < yMin || 
							wp.getY() > yMax){
						toRemove.add(wp);
						//System.out.println("modified wp");
					}
				}
				day.getWaypoints().removeAll(toRemove);
				
				TrackType tt = day.getTrack();
				if (tt != null){
					LineString ls = (LineString)reader.read(decodeHex(tt.getGeom()));
					List<Coordinate> coords = new ArrayList<Coordinate>();
					for (Coordinate c : ls.getCoordinates()){
						coords.add(c);
					}
					List<Coordinate> toRemovec = new ArrayList<Coordinate>();
					for (Coordinate c : coords){
						if (c.x < xMin || c.x > xMax || c.y < yMin || c.y > yMax){
							toRemovec.add(c);
						}
					}
					if (toRemovec.size() > 0){
						//System.out.println("modified track");
						coords.removeAll(toRemovec);
						if (coords.size() > 1){
							LineString newLs = gf.createLineString(coords.toArray(new Coordinate[coords.size()]));
							tt.setGeom(encodeHex(writer.write(newLs)));
							tt.setDistance(distanceInMeters(newLs));
						}else{
							System.out.println("Track for " + patrol.getId() + " (" + day.getDate() + ") will be removed as fewer than 1 points is inside the bbox.");	
							day.setTrack(null);
						}
					}
				}
			}
		}

	}
	
	@Override
	public void processFile(File in, File out) throws Exception{
		PatrolType pt = DataUtils.readPatrol(in);
		deleteWaypointsTrack(pt);
		DataUtils.writePatrol(out, pt);
	}
	
	
	public static void main(String args[]){
		if (args.length != 6){
			System.err.println("Bounding box not provided to WaypointBbox Checker");
			return;
		}
		WaypointBboxDeleter checker = new WaypointBboxDeleter();
		checker.xMin = Double.parseDouble(args[2]);
		checker.yMin = Double.parseDouble(args[3]);
		checker.xMax = Double.parseDouble(args[4]);
		checker.yMax = Double.parseDouble(args[5]);
		
		DataUtils.processConfiguration(args, checker, false);
	}
	
	public static byte[] decodeHex(String str) throws Exception {
		char[] data = str.toCharArray();
		int len = data.length;

		if ((len & 0x01) != 0) {
			throw new IllegalStateException("Invalid track string");
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
	
	protected static int toDigit(char ch, int index) throws Exception {
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new IllegalStateException("Invalid hex char.");
		}
		return digit;
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
	 * Computes the distance in  meters of the given linestring
	 * in 4326 projections.
	 * @param ls
	 * @return
	 * @throws TransformException
	 */
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
}
