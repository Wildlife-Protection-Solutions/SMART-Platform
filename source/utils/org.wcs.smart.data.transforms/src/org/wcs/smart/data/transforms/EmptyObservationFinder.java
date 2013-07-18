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

import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.WaypointType;

/**
 * Data processor that searches for waypoints with no observations
 * and writes out the date/time of the waypoint to standard out.
 * @author Emily
 *
 */
public class EmptyObservationFinder implements IDataProcessor {

	@Override
	public void processFile(File in, File out) throws Exception {
		PatrolType pt = DataUtils.readPatrol(in);
		findObservation(pt, in.toString());
	}

	private void findObservation(PatrolType pt, String fileName) {
		for(PatrolLegType leg: pt.getLegs()){
			for (PatrolLegDayType day : leg.getDays()){
				for (WaypointType wp : day.getWaypoints()){
					if (wp.getObservations().size() == 0){
						System.out.println(pt.getId() + "," + day.getDate() + "," + wp.getTime() + "," + wp.getId() + "," + wp.getX() + "," + wp.getY() + "," + fileName);
					}
				}
			}
		}		
	}
	public static void main(String args[]){
		DataUtils.processConfiguration(args, new EmptyObservationFinder(), false);
	}
}
