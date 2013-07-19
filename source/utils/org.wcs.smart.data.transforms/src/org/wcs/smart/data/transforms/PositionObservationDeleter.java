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
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;


/**
 * Deletes all observation which have a categoryKey of "position." and
 * a single attribute, 'positiontype' with a itemKey of 'delete'.
 * <pre>
 * {@code
 * 
 *  <observations categoryKey="position.">
 *      <attributes attributeKey="positiontype">
 *        <sValue></sValue>
 *        <dValue></dValue>
 *        <itemKey>delete</itemKey>
 *        <bValue></bValue>
 *      </attributes>
 *  </observations>
 * }
 * 
 * 
 * </pre>
 * 
 * @author Emily
 *
 */
public class PositionObservationDeleter implements IDataProcessor {

	@Override
	public void processFile(File in, File out) throws Exception {
		PatrolType pt = DataUtils.readPatrol(in);
		
		for (PatrolLegType plt : pt.getLegs()){
			for (PatrolLegDayType day : plt.getDays()){
				for (WaypointType wp : day.getWaypoints()){
					List<WaypointObservationType> toRemove = new ArrayList<WaypointObservationType>();
					for (WaypointObservationType wpt : wp.getObservations()){
						if (wpt.getCategoryKey().equals("position.")){
							if (wpt.getAttributes().size() == 1){
								WaypointObservationAttributeType att = wpt.getAttributes().get(0);
								if (att.getAttributeKey().equals("positiontype") && att.getItemKey().equals("delete")){
									toRemove.add(wpt);
								}
							}
						}
					}
					wp.getObservations().removeAll(toRemove);
				}
			}
		}
		
		DataUtils.writePatrol(out, pt);

	}
	
	public static void main(String args[]){
		DataUtils.processConfiguration(args, new PositionObservationDeleter());
	}

}
