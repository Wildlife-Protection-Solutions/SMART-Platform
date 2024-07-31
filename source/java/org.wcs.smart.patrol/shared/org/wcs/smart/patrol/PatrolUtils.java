/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeTreeNode;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.TrackUtil;

/**
 * A set of utility classes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolUtils {

	/**
	 * Converts a set of coordinate to a track.  Coordinates are first sorted
	 * by date/time which should be stored in the  z coordinate IN GMT. Use
	 * SharedUtils.toLongTime to convert from local value to gmt;
	 * 
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static Track convertToTrack(List<Coordinate> coordinates){
		if (coordinates.size() < 2) {
			return null;
		}
		LineString track = TrackUtil.convertToLineString(coordinates);
		Track t = new Track();
		t.setLineStrings(Arrays.asList(track));
		return t;
	}

	/**
	 * Takes a Patrol adds in single-day PatrolLegs(with PatrolLegDays) for any days
	 *  between the start and end dates that do not have a PatrolLeg on them. 
	 *  
	 * @param patrol
	 */
	public static void createLegDaysForMissingDays(Patrol patrol) {
		List<PatrolLeg> legList = patrol.getLegs();
		if(legList == null) {
			legList = new ArrayList<PatrolLeg>();
			patrol.setLegs(legList);
		}
		PatrolUtils.createLegDaysForMissingDays(patrol, patrol.getStartDate(), patrol.getEndDate(), legList);
	}

	/**
	 * Takes a Patrol and a list of PatrolLegs and extends PatrolLegs (with empty PatrolLegDays) for any days
	 * between the start and end dates that do not have a PatrolLeg on them. The date range and list of legs is passed in
	 * because the PatrolLegsComposite works on a cloned list of legs.
	 * 
	 * @param patrol
	 * @param legs
	 */
	public static void createLegDaysForMissingDays(Patrol patrol, LocalDate startDate, LocalDate endDate, List<PatrolLeg> legs){
		// sort patrols by start date
		Collections.sort(legs, new PatrolLegStartDateComparator());
		
		// loop over each leg from the start to the end
		for(PatrolLeg leg : legs) {
			// while there is no leg on the day after this leg ends, and it is still within the patrol date range,
			LocalDate working = leg.getEndDate().plusDays(1);
			while(!working.isAfter(endDate) && !PatrolUtils.hasLegOnDate(legs, working)){
				// extend this leg by one day
				PatrolLegDay pld = new PatrolLegDay();
				pld.setPatrolLeg(leg);
				pld.setDate(working);
				pld.setStartTime(LocalTime.MIN);
				pld.setEndTime(SharedUtils.END_OF_DAY);
				leg.getPatrolLegDays().add(pld);
				leg.setEndDate(working);
				
				working = ChronoUnit.DAYS.addTo(working, 1);
			}	
			
		}
	}

	/**
	 * Loops through a list of PatrolLegs to determine if there is at least one leg on the given date.
	 * 
	 * @param legList the list of PatrolLegs to search
	 * @param cal the date to search for
	 * @return true if at least one PatrolLeg in the list is on the given date, false otherwise
	 */
	public static boolean hasLegOnDate(List<PatrolLeg> legList, LocalDate date) {
		for (PatrolLeg leg : legList){
			if(date.isEqual(leg.getStartDate()) || date.isEqual(leg.getEndDate()) 
					||  (date.isAfter(leg.getStartDate()) && date.isBefore(leg.getEndDate())) ){
				return true;
			}
		}
		return false;
	}

	/**
	 * Loops through a list of PatrolLegs and returns the earliest startDate and latest EndDate.
	 * 
	 * @param legList the list of PatrolLegs to loop through
	 * @return an array of two Dates, the start and end, in that order
	 */
	public static LocalDate[] calculateDateRange(List<PatrolLeg> legList) {
		LocalDate[] dates = new LocalDate[2];
		for (PatrolLeg leg : legList) {
			if(dates[0] == null || leg.getStartDate().isBefore(dates[0])) {
				dates[0] = leg.getStartDate();
			}
			if(dates[1] == null || leg.getEndDate().isAfter(dates[1])) {
				dates[1] = leg.getEndDate();
			}
		}
		return dates;
	}
	
	/**
	 * Find the attribute tree node with the given hkey associated with the given patrol attribute
	 * Will return null if not found.
	 * 
	 * @param attribute
	 * @param hkey
	 * @param session
	 */
	public static PatrolAttributeTreeNode findAttributeTreeNode(PatrolAttribute attribute, String hkey, Session session) {
		return QueryFactory.buildQuery(session, PatrolAttributeTreeNode.class, 
				new Object[] {"attribute", attribute}, //$NON-NLS-1$
				new Object[] {"hkey", hkey}).uniqueResult(); //$NON-NLS-1$		
			
	}

	
	/**
	 * For ccaa analysis; merges patrol attributes from various ca's 
	 * into a single patrol attribute object.
	 * If the patrolAttributes are of different types (Tree/list etc) this will 
	 * return null. List items and tree nodes are also merged.
	 * Icons are not merged at this time.
	 * @param toMerge
	 * @return
	 */
	public static PatrolAttribute mergeAttributes(List<PatrolAttribute> toMerge) {
		PatrolAttribute pa = toMerge.get(0);
		
		//validate all are the same type otherwise return null
		for(PatrolAttribute p : toMerge) {
			if (p.getType() != pa.getType()) return null;
		}
		
		//make a clone and sort out list items
		PatrolAttribute temp = new PatrolAttribute();
		temp.setKeyId(pa.getKeyId());
		temp.setIsActive(true);
		temp.setName(pa.getName());
		temp.setType(pa.getType());
		
		if (pa.getType() == Attribute.AttributeType.LIST) {
			temp.setAttributeList(mergeListAttributes(toMerge, temp));			
		}
		if (pa.getType() == Attribute.AttributeType.TREE) {
			temp.setAttributeTree(mergeTreeAttributes(toMerge, temp));			
		}
		return temp;
	}
	
	private static List<PatrolAttributeListItem> mergeListAttributes(List<PatrolAttribute> toMerge, PatrolAttribute rattribute) {
		Map<String, PatrolAttributeListItem> listItems = new HashMap<>();
		
		for (PatrolAttribute attribute : toMerge) {
			if (attribute.getAttributeList() == null) continue;
			
			for (PatrolAttributeListItem li : attribute.getAttributeList()) {
				if (listItems.get(li.getKeyId()) != null) continue;
				
				PatrolAttributeListItem clone = new PatrolAttributeListItem();
				clone.setIsActive(true);
				clone.setKeyId(li.getKeyId());
				clone.setName(li.getName());
				clone.setListOrder(li.getListOrder());
				clone.setAttribute(rattribute);
				listItems.put(li.getKeyId(), clone);
				
			}
		}
		
		List<PatrolAttributeListItem> sortedItems = new ArrayList<>();
		sortedItems.addAll(listItems.values());
		Collections.sort(sortedItems);
		return sortedItems;
	}
	
	
	private static List<PatrolAttributeTreeNode> mergeTreeAttributes(List<PatrolAttribute> toMerge, PatrolAttribute rattribute) {
		Map<String, PatrolAttributeTreeNode> treeNodes = new HashMap<>();
		
		for (PatrolAttribute attribute : toMerge) {
			if (attribute.getAttributeTree() == null) continue;
			
			List<PatrolAttributeTreeNode> toProcess = new ArrayList<>();
			toProcess.addAll(attribute.getAttributeTree());
			
			while(!toProcess.isEmpty()) {
				PatrolAttributeTreeNode tnode = toProcess.remove(0);
				toProcess.addAll(tnode.getChildren());
				
				if (treeNodes.get(tnode.getHkey()) != null) continue;
				
				PatrolAttributeTreeNode clone = new PatrolAttributeTreeNode();
				clone.setIsActive(true);
				clone.setChildren(new ArrayList<>());
				clone.setKeyId(tnode.getKeyId());
				clone.setHkey(tnode.getHkey());
				clone.setName(tnode.getName());
				clone.setNodeOrder(tnode.getNodeOrder());
				clone.setAttribute(rattribute);
				if (tnode.getParent() != null) {
					clone.setParent(treeNodes.get(tnode.getParent().getHkey()));
					clone.getParent().getChildren().add(clone);
				}
				
				treeNodes.put(tnode.getHkey(), clone);
				
			}
		}
		
		List<PatrolAttributeTreeNode> sortedItems = treeNodes.values().stream().filter(f->f.getParent() == null).collect(Collectors.toList());
		Collections.sort(sortedItems);
		return sortedItems;
	}
}
