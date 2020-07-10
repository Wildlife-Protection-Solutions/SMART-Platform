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
package org.wcs.smart.patrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.TrackUtil;

/**
 * A set of utility classes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolUtils {

	public static final String EDIT_LINK_TEXT = DialogConstants.EDIT_LINK_TEXT;
	/**
	 * Returns the image to use for a given patrol type or
	 * null if no image found
	 * 
	 * @param type patrol type
	 * 
	 * @return image associated with patrol type
	 */
	public static Image getImage(PatrolType.Type type){
		if (type == PatrolType.Type.GROUND){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.GROUND_PATROL_ICON);
		}else if (type == PatrolType.Type.MARINE){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MARINE_PATROL_ICON);
		}else if (type == PatrolType.Type.AIR){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.AIR_PATROL_ICON);
		}else if (type == PatrolType.Type.MIXED){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MIXED_PATROL_ICON);
		}
		return null;
	}
	
	/**
	 * Returns the image descriptor to use for a given patrol type or
	 * null if no image found
	 * 
	 * @param type patrol type
	 * 
	 * @return image descriptor associated with patrol type
	 */
	public static ImageDescriptor getImageDescriptor(PatrolType.Type type){
		if (type == PatrolType.Type.GROUND){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.GROUND_PATROL_ICON);
		}else if (type == PatrolType.Type.MARINE){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.MARINE_PATROL_ICON);
		}else if (type == PatrolType.Type.AIR){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.AIR_PATROL_ICON);
		}else if (type == PatrolType.Type.MIXED){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.MIXED_PATROL_ICON);
		}
		return null;
	}
	
	/**
	 * Converts a set of coordinate to a track.  Coordinates are first sorted
	 * by date/time which should be stored in the z coordinate
	 * as the the date/time in the current timezone.  This function
	 * convert the z to GMT time.
	 * 
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static Track convertToTrack(List<Coordinate> coordinates){
		if (coordinates.size() < 2) {
			return null;
		}
		LineString track = TrackUtil.convertToLineString(coordinates, Track.ZTIMEZONE);
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
		createLegDaysForMissingDays(patrol, patrol.getStartDate(), patrol.getEndDate(), legList);
	}

	/**
	 * Takes a Patrol and a list of PatrolLegs and extends PatrolLegs (with emptry PatrolLegDays) for any days
	 * between the start and end dates that do not have a PatrolLeg on them. The date range and list of legs is passed in
	 * because the PatrolLegsComposite works on a cloned list of legs.
	 * 
	 * @param patrol
	 * @param legs
	 */
	public static void createLegDaysForMissingDays(Patrol patrol, Date startDate, Date endDate, List<PatrolLeg> legs){
		// convert the patrol end date into an easier to deal with Calendar
		Calendar calEnd= SharedUtils.convertDate( SharedUtils.getDatePart(endDate, false) );

		// sort patrols by start date
		Collections.sort(legs, new PatrolLegStartDateComparator());
		
		// loop over each leg from the start to the end
		for(PatrolLeg leg : legs) {
			// while there is no leg on the day after this leg ends, and it is still within the patrol date range,
			Calendar cal = SharedUtils.convertDate( SharedUtils.getDatePart(leg.getEndDate(), false));
			cal.add(Calendar.DAY_OF_MONTH, 1);
			while(!cal.after(calEnd) && !hasLegOnDate(legs, cal)){
				// extend this leg by one day
				PatrolLegDay pld = new PatrolLegDay();
				pld.setPatrolLeg(leg);
				pld.setDate(cal.getTime());
				pld.setStartTime(SmartUtils.convertDateToTime(cal.getTime()));
				pld.setEndTime(SmartUtils.createPatrolTime(23, 59, 59));
				leg.getPatrolLegDays().add(pld);
				leg.setEndDate(cal.getTime());
				cal.add(Calendar.DAY_OF_MONTH, 1);
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
	public static boolean hasLegOnDate(List<PatrolLeg> legList, Calendar cal) {
		for (PatrolLeg leg : legList){
			Calendar legStart = SharedUtils.convertDate( SharedUtils.getDatePart(leg.getStartDate(), false));
			Calendar legEnd = SharedUtils.convertDate( SharedUtils.getDatePart(leg.getEndDate(), false));
			if(cal.equals(legStart) || cal.equals(legEnd) ||  (cal.after(legStart) && cal.before(legEnd)) ){
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
	public static Date[] calculateDateRange(List<PatrolLeg> legList) {
		Date[] dates = new Date[2];
		for (PatrolLeg leg : legList) {
			if(dates[0] == null || leg.getStartDate().before(dates[0])) {
				dates[0] = leg.getStartDate();
			}
			if(dates[1] == null || leg.getEndDate().after(dates[1])) {
				dates[1] = leg.getEndDate();
			}
		}
		return dates;
	}
	
}
