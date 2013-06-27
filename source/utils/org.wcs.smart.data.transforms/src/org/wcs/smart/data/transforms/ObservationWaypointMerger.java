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
import org.wcs.smart.patrol.xml.model.WaypointType;

/**
 * This function merges all observations
 * at waypoints which have the same time and (x,y) location
 * into a single waypoint.
 * <p>
 * It assumes that the waypoints are ordered by date/time.
 * </p>
 * 
 * Example:
 * 
 * The xml:
 * <pre>
 * {@code
 * <waypoints time="10:00:03" y="15.430397" x="99.397924" id="8">
 * 	<observations categoryKey="mammals.track.">
 *		<attributes attributeKey="hkkspecieslist">
 *			<itemKey>redmuntjac</itemKey>
 *		</attributes>
 *		<attributes attributeKey="numberofanimalsseen">
 *			<dValue>1.0</dValue>
 *		</attributes>
 *	</observations>
 * </waypoints>
 * <waypoints time="10:00:03" y="15.430397" x="99.397924" id="9">
 *	<observations categoryKey="mammals.track.">
 *		<attributes attributeKey="hkkspecieslist">
 *			<itemKey>asianelephant</itemKey>
 *		</attributes>
 *		<attributes attributeKey="numberofanimalsseen">
 *			<dValue>1.0</dValue>
 *		</attributes>
 *	</observations>
 *</waypoints>
 * }
 * </pre>
 * would be transformed to:
 * <pre>
 * {@code
 * <waypoints time="10:00:03" y="15.430397" x="99.397924" id="8">
 * 	<observations categoryKey="mammals.track.">
 *		<attributes attributeKey="hkkspecieslist">
 *			<itemKey>redmuntjac</itemKey>
 *		</attributes>
 *		<attributes attributeKey="numberofanimalsseen">
 *			<dValue>1.0</dValue>
 *		</attributes>
 *	</observations>
 *	<observations categoryKey="mammals.track.">
 *		<attributes attributeKey="hkkspecieslist">
 *			<itemKey>asianelephant</itemKey>
 *		</attributes>
 *		<attributes attributeKey="numberofanimalsseen">
 *			<dValue>1.0</dValue>
 *		</attributes>
 *	</observations>
 *</waypoints>
 * }
 * </pre>
 * @author Emily
 *
 */
public class ObservationWaypointMerger implements IDataProcessor {

	
	private void mergeObservation(PatrolType patrol){
		
		for (PatrolLegType leg : patrol.getLegs()){
			for (PatrolLegDayType day : leg.getDays()){
				List<WaypointType> newWaypoints = new ArrayList<WaypointType>();
				
				WaypointType lastWaypoint = null;
				
				for (WaypointType wp : day.getWaypoints()){
					if (lastWaypoint != null){
						if (wp.getX().equals(lastWaypoint.getX()) &&
							wp.getY().equals(lastWaypoint.getY()) &&
							wp.getTime().equals(lastWaypoint.getTime())){
						
							//add all of this waypoints observations to the last
							//waypoint and ignore it
							lastWaypoint.getObservations().addAll(wp.getObservations());
						}else{
							//add the waypoint to list of new aypoints
							newWaypoints.add(lastWaypoint);
							lastWaypoint = wp;
						}
					}else{
						lastWaypoint = wp;
					}
				}
				newWaypoints.add(lastWaypoint);
				
				day.getWaypoints().clear();
				day.getWaypoints().addAll(newWaypoints);
			}
		}
		
	}
	
	@Override
	public void processFile(File in, File out) throws Exception{
		PatrolType pt = DataUtils.readPatrol(in);
		mergeObservation(pt);
		DataUtils.writePatrol(out, pt);
	}
	
	
	public static void main(String args[]){
		DataUtils.processConfiguration(args, new ObservationWaypointMerger());
		
	}
}
