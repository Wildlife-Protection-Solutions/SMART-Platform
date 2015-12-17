/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;

/**
 * Implementation for patrol query label provider.
 * 
 * @author Emily
 *
 */
public class PatrolQueryLabelProvider implements IQueryPatrolLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof FixedQueryColumn.FixedColumns){
			switch((FixedQueryColumn.FixedColumns)item){
			case CA_ID: return "Conservation Area ID";
			case CA_NAME: return "Conservation Area Name";
			case PATROL_ID: return "Patrol ID";
			case PATROL_TYPE: return "Type";
			case PATROL_START_DATE: return "Patrol Start Date";
			case PATROL_END_DATE: return "Patrol End Date";
			case PATROL_STATION: return "Station";
			case PATROL_TEAM: return "Team";
			case PATROL_OBJETIVE: return "Objective";
			case PATROL_MANDATE: return "Mandate";
			case PATROL_ARMED: return "Armed";
			case PATROL_LEG_ID: return "Patrol Leg ID";
			case PATROL_LEG_LEADER: return "Leader";
			case PATROL_LEG_PILOT: return "Pilot";
			case PATROL_LEG_START_DATE: return "Patrol Leg Start Date";
			case PATROL_LEG_END_DATE: return "Patrol Leg End Date";
			case TRANSPORT_TYPE: return "Patrol Transport Type";
			case WAYPOINT_ID: return "Waypoint ID";
			case WAYPOINT_DATE: return "Waypoint Date";
			case WAYPOINT_TIME: return "Waypoint Time";
			case WAYPOINT_X: return "X";
			case WAYPOINT_Y: return "Y";
			case WAYPOINT_DIRECTION: return "Direction";
			case WAYPOINT_DISTANCE: return "Distance";
			case WAYPOINT_COMMENT: return "Comment";
			case WAYPOINT_OBSERVER: return "Observer";
			}
		}
		if (item instanceof PatrolValueOption){
			switch((PatrolValueOption)item){
				case NUM_PATROLS: return "Number of Patrols"; 
				case NUM_DAYS: return "Number of Days";    
				case NUM_NIGHTS: return "Number of Nights";
				case DISTANCE: return "Distance (km)";      
				case NUM_FIELDHOURS: return "Number of Active Patrol Hours";
				case NUM_PATROLHOURS: return "Number of Patrol Hours";  
				case NUM_MEMBERS: return "Number of Employees";
				case MAN_HOURS: return "Person - Field Hours";    
				case MAN_DAYS: return "Person - Days";
				
				case NUM_PATROLS_TOTAL: return "Total Number of Patrols";
				case NUM_DAYS_TOTAL: return "Total Number of Days";   
				case DISTANCE_TOTAL: return "Total Distance (km)";  
				case NUM_FIELDHOURS_TOTAL: return "Total Number of Active Patrol Hours";
				case NUM_PATROLHOURS_TOTAL: return "Total Number of Patrol Hours";
				case MAN_HOURS_TOTAL: return "Total Person - Field Hours";
				case MAN_DAYS_TOTAL: return "Total Person - Days";
			}
		}
		if (item instanceof PatrolQueryOption){
			switch((PatrolQueryOption)item){
				case ID: return "Patrol ID";
				case ARMED: return "Armed";
				case STATION: return "Station";
				case TEAM: return "Team";
				case TEAM_KEY: return "Team";
				case EMPLOYEE: return "Employee";
				case LEADER: return "Leader";
				case PILOT: return "Pilot";
				case MANDATE: return "Mandate";
				case MANDATE_KEY: return "Mandate";
				case PATROL_TYPE: return "Patrol Type";
				case PATROL_TRANSPORT_TYPE: return "Transport Type";
				case PATROL_TRANSPORT_TYPE_KEY: return "Transport Type";
				case CONSERVATION_AREA: return "Conservation Area";
				case AGENCY: return "Agency";
				case RANK: return "Rank";
			}
		}
		if (item instanceof PatrolEndDateField){
			return "Patrol End";
		}
		if (item instanceof PatrolStartDateField){
			return "Patrol Start";
		}
		return null;
	}

}
