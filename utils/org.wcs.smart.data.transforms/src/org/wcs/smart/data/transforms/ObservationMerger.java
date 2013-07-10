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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;


/**
 * This function merges observations at a single waypoint.  It does not merge
 * observations across waypoints.
 * <p>
 * Observations are merged if they have the same categories and either different
 * attributes or the same attribute with the same attribute values.  
 * If the same attribute has
 * different values across and waypoint the observations are not merged.
 * 
 * </p>
 * 
 * This xml:
 * <pre>
 * {@code
 * <waypoints time="10:00:03" y="15.430397" x="99.397924" id="8">
 * 	<observations categoryKey="mammals.track.">
 *		<attributes attributeKey="hkkspecieslist">
 *			<itemKey>redmuntjac</itemKey>
 *		</attributes>
 *		<attributes attributeKey="age">
 *			<itemKey>old</itemKey>
 *		</attributes>
 *	</observations>
 *	<observations categoryKey="mammals.track.">
 *		<attributes attributeKey="hkkspecieslist">
 *			<itemKey>redmuntjac</itemKey>
 *		</attributes>
 *		<attributes attributeKey="numberoftrack">
 *			<dValue>3.0</dValue>
 *		</attributes>
 *	</observations>
 *</waypoints>
 * }
 * </pre>
 * 
 * Becomes:
 * <pre>
 * {@code
 * <waypoints time="10:00:03" y="15.430397" x="99.397924" id="8">
 * 	<observations categoryKey="mammals.track.">
 *		<attributes attributeKey="hkkspecieslist">
 *			<itemKey>redmuntjac</itemKey>
 *		</attributes>
 *		<attributes attributeKey="age">
 *			<itemKey>old</itemKey>
 *		</attributes>
 *		<attributes attributeKey="numberoftrack">
 *			<dValue>3.0</dValue>
 *		</attributes>
 *	</observations>
 *</waypoints>
 * }
 * </pre>
 * @author Emily
 *
 */
public class ObservationMerger implements IDataProcessor{

	private Object NODATA = new Object();
	
	private void mergeObservation(PatrolType patrol){
		
		for (PatrolLegType leg : patrol.getLegs()){
			for (PatrolLegDayType day : leg.getDays()){
				for (WaypointType wp : day.getWaypoints()){

					//create a maping of category key to observations
					HashMap<String, List<WaypointObservationType>> maps = new HashMap<String, List<WaypointObservationType>>();
					for (WaypointObservationType wot : wp.getObservations()){
						List<WaypointObservationType> obs = maps.get(wot.getCategoryKey());
						if (obs == null){
							obs = new ArrayList<WaypointObservationType>();
							maps.put(wot.getCategoryKey(), obs);
						}
						obs.add(wot);
					}
					
					//for each category key; check to ensure attributes are either different or have the same attribute value
					ArrayList<WaypointObservationType> newObservations = new ArrayList<WaypointObservationType>();
					for(Entry<String,List<WaypointObservationType>> item : maps.entrySet()){
						List<WaypointObservationType> elements = item.getValue();
						HashMap<String, Object> attributeValues = new HashMap<String,Object>();
						boolean canMerge = true;
						for (WaypointObservationType element : elements){
							for (WaypointObservationAttributeType attribute : element.getAttributes()){
								if (attributeValues.containsKey(attribute.getAttributeKey())){
									//validate that the value is the same
									Object v = attributeValues.get(attribute.getAttributeKey());
									if (attribute.getDValue() != null){
										if (!attribute.getDValue().equals(v)){
											canMerge = false;
										}
									}else if (attribute.getSValue() != null){
										if (!attribute.getSValue().equals(v)){
											canMerge = false;
										}
									}else if (attribute.getItemKey() != null){
										if (!attribute.getItemKey().equals(v)){
											canMerge = false;
										}
									}else if (v != NODATA){
										canMerge = false;
									}

									if (!canMerge){
										break;
									}
								}else{
									if (attribute.getDValue() != null){
										attributeValues.put(attribute.getAttributeKey(), attribute.getDValue());
									}else if (attribute.getSValue() != null && !attribute.getSValue().isEmpty()){
										attributeValues.put(attribute.getAttributeKey(), attribute.getSValue());
									}else if (attribute.getItemKey() != null && !attribute.getItemKey().isEmpty()){
										attributeValues.put(attribute.getAttributeKey(), attribute.getItemKey());
									}else{
										attributeValues.put(attribute.getAttributeKey(), NODATA);
									}
								}
							}
							if (!canMerge){
								break;
							}
						}
						if (canMerge && elements.size() > 1){
							WaypointObservationType main = elements.get(0);
							HashSet<String> attKeys = new HashSet<String>();
							for (WaypointObservationAttributeType a : main.getAttributes()){
								attKeys.add(a.getAttributeKey());
							}
							for (int i = 1; i < elements.size(); i ++){
								WaypointObservationType merge = elements.get(i);
								for (WaypointObservationAttributeType b : merge.getAttributes()){
									if (!attKeys.contains(b.getAttributeKey())){
										main.getAttributes().add(b);
										attKeys.add(b.getAttributeKey());
									}
								}
							}
							newObservations.add(main);
							System.out.println("Observations Merged: " + patrol.getId());
						}else{
							if (elements.size() > 1){
								System.out.println("Multiple observations found for category that could not be merged because different attribute values exists.  Category " + item.getKey() + " waypoint id " + wp.getId() + ", " + day.getDate().toString() + ", PID: " + patrol.getId());
							}
							newObservations.addAll(elements);
						}
					}
					wp.getObservations().clear();
					wp.getObservations().addAll(newObservations);
				}
			}
		}
		
	}
	
	public boolean nullEquals(Object x, Object y){
		if (x == null && y == null) return true;
		if (x != null && y != null) return x.equals(y);
		return false;
	}
	@Override
	public void processFile(File in, File out) throws Exception{
		PatrolType pt = DataUtils.readPatrol(in);
		mergeObservation(pt);
		DataUtils.writePatrol(out, pt);
	}
	
	
	public static void main(String args[]){
		DataUtils.processConfiguration(args, new ObservationMerger());
	}
}
