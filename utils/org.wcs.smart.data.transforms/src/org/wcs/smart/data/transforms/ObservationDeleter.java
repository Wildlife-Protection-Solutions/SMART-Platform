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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;

/**
 * <p>
 * What I'd like is: for all the cases where there is a matching category with 2 observations, 
 * one with actiontaken=observedonly and the other with actiontaken=destroyed, 
 * just toss out the observed only one and keep all the attributes from the destroyed one. 
 * </p>
 * <p>
 * This data processor searches waypoints for the observations with the
 * same category where one observation has an actiontaken attribute with
 * an item value of observedonly and another observation has an attribute with 
 * an actiontaken attribute with an item value of destroyed.  When this is found
 * the observedonly observation (and all associated attributes) are delete.
 * <p>
 * 
 * This xml:
 * <pre>
 * {@code
 *  <waypoints time="00:00:00" y="4.0" x="3.0" id="1">
 *    <observations categoryKey="threat.biologicalresourceuse.huntingcollectingterrestrialanimals.peopleconfronted.">
 *      <attributes attributeKey="actiontaken">
 *        <itemKey>observedonly</itemKey>
 *      </attributes>
 *      <attributes attributeKey="numberofpeopleconfronted">
 *        <dValue>3.0</dValue>
 *      </attributes>
 *    </observations>
 *    <observations categoryKey="threat.biologicalresourceuse.huntingcollectingterrestrialanimals.peopleconfronted.">
 *      <attributes attributeKey="actiontaken">
 *        <itemKey>destroyed</itemKey>
 *      </attributes>
 *      <attributes attributeKey="numberofpeopleconfronted">
 *        <dValue>4.0</dValue>
 *      </attributes>
 *    </observations>
 *  </waypoints>
 * }
 * </pre>
 *
 *
 * becomes:
 * <pre>
 * {@code
 *  <waypoints time="00:00:00" y="4.0" x="3.0" id="1">
 *    <observations categoryKey="threat.biologicalresourceuse.huntingcollectingterrestrialanimals.peopleconfronted.">
 *      <attributes attributeKey="actiontaken">
 *        <itemKey>destroyed</itemKey>
 *      </attributes>
 *      <attributes attributeKey="numberofpeopleconfronted">
 *        <dValue>4.0</dValue>
 *      </attributes>
 *    </observations>
 *  </waypoints>
 * }
 * </pre>
 * 
 * @author Emily
 *
 */
public class ObservationDeleter implements IDataProcessor {

	@Override
	public void processFile(File in, File out) throws Exception {
		PatrolType pt = DataUtils.readPatrol(in);
		
		for (PatrolLegType plt : pt.getLegs()){
			for (PatrolLegDayType day : plt.getDays()){
				for (WaypointType wp : day.getWaypoints()){
					HashMap<String, List<WaypointObservationType>> items = new HashMap<String, List<WaypointObservationType>>();
					for (WaypointObservationType wpt : wp.getObservations()){
						List<WaypointObservationType> list = items.get(wpt.getCategoryKey());
						if (list == null){
							list = new ArrayList<WaypointObservationType>();
							items.put(wpt.getCategoryKey(), list);
						}
						list.add(wpt);
					}
					
					for (Entry<String,List<WaypointObservationType>> entry : items.entrySet()){
						if (entry.getValue().size() > 1){
							WaypointObservationType hasActionObserved = null;
							WaypointObservationType hasActionDestroyed = null;
							for (WaypointObservationType wo : entry.getValue()){
								for (WaypointObservationAttributeType att : wo.getAttributes()){
									if (att.getAttributeKey().equals("actiontaken")){
										if (att.getItemKey().equals("observedonly")){
											hasActionObserved = wo;
										}
										if (att.getItemKey().equals("destroyed")){
											hasActionDestroyed = wo;
										}
									}
								}
							}
							if (hasActionDestroyed != null && hasActionObserved != null){
								wp.getObservations().remove(hasActionObserved);
								System.out.println("removing observation: " + pt.getId() + " - WP:" + wp.getId() + " (" + day.getDate().toGregorianCalendar().getTime().toString() + ")");
							}
						}
					}
					
				}
			}
		}
		
		DataUtils.writePatrol(out, pt);

	}
	
	public static void main(String args[]){
		DataUtils.processConfiguration(args, new ObservationDeleter());
	}

}
