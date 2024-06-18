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
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.patrol.model.Patrol;
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

}
