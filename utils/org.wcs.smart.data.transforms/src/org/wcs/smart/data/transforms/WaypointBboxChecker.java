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

import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.TrackType;
import org.wcs.smart.patrol.xml.model.WaypointType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * This function finds searches all waypoints and track point to ensure
 * they are inside a bounding box.  The bounding box must
 * be supplied as a parameter to the script in the format
 * "xmin ymin xmax ymax"
 * 
 * @author Emily
 *
 */
public class WaypointBboxChecker implements IDataProcessor {

	private double xMin = 0;
	private double yMin = 0;
	private double xMax = 0;
	private double yMax = 0;
	
	private void checkWaypointsTrack(PatrolType patrol) throws Exception{
		
		WKBReader reader = new WKBReader();
		List<String> waypointErrors = new ArrayList<String>();
		List<String> trackErrors = new ArrayList<String>();
		for (PatrolLegType leg : patrol.getLegs()){
			for (PatrolLegDayType day : leg.getDays()){
				for (WaypointType wp : day.getWaypoints()){
					if (wp.getX() < xMin || 
							wp.getX() > xMax || 
							wp.getY() < yMin || 
							wp.getY() > yMax){
						waypointErrors.add(day.getDate() + " [" + wp.getId() + "]");
					}
				}
				TrackType tt = day.getTrack();
				if (tt != null){
					LineString ls = (LineString)reader.read(decodeHex(tt.getGeom()));
					for (Coordinate c : ls.getCoordinates()){
						if (c.x < xMin || c.x > xMax || c.y < yMin || c.y > yMax){
							trackErrors.add(day.getDate().toString());
							break;
						}
					}
				}
			}
		}
		
		if (waypointErrors.size() > 0){
			System.out.print("Waypoint Outside Bbox " + patrol.getId() + ":");
			for (String error: waypointErrors){
				System.out.print(error + " ");
			}
			System.out.println();
		}
		if (trackErrors.size() > 0){
			System.out.print("Tracks Outsite Boox " + patrol.getId() + ":");
			for (String error: trackErrors){
				System.out.print(error + " ");
			}
			System.out.println();
		}
	}
	
	@Override
	public void processFile(File in, File out) throws Exception{
		PatrolType pt = DataUtils.readPatrol(in);
		checkWaypointsTrack(pt);
	}
	
	
	public static void main(String args[]){
		if (args.length != 6){
			System.err.println("Bounding box not provided to WaypointBbox Checker");
			return;
		}
		WaypointBboxChecker checker = new WaypointBboxChecker();
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
}
