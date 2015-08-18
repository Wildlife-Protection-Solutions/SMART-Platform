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
package org.wcs.smart.patrol.query.ui;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;
import org.wcs.smart.query.QueryPlugIn;

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
			case CA_ID: return Messages.FixedQueryColumn_CaIdColumnName;
			case CA_NAME: return Messages.FixedQueryColumn_CaNameColumnName;
			case PATROL_ID: return Messages.FixedQueryColumn_PatrolIdColumnName;
			case PATROL_TYPE: return Messages.FixedQueryColumn_TypeColumnName;
			case PATROL_START_DATE: return Messages.FixedQueryColumn_PatrolStartDateColumnName;
			case PATROL_END_DATE: return Messages.FixedQueryColumn_PatrolEndDateColumnName;
			case PATROL_STATION: return Messages.FixedQueryColumn_StationColumnName;
			case PATROL_TEAM: return Messages.FixedQueryColumn_TeamColumnName;
			case PATROL_OBJETIVE: return Messages.FixedQueryColumn_ObjectiveColumnName;
			case PATROL_MANDATE: return Messages.FixedQueryColumn_MandateColumnName;
			case PATROL_ARMED: return Messages.FixedQueryColumn_ArmedColumnName;
			case PATROL_LEG_ID: return Messages.FixedQueryColumn_LegIdColumnName;
			case PATROL_LEG_LEADER: return Messages.FixedQueryColumn_LeaderColumnName;
			case PATROL_LEG_PILOT: return Messages.FixedQueryColumn_PilotColumnName;
			case PATROL_LEG_START_DATE: return Messages.FixedQueryColumn_LegStartDateColumnName;
			case PATROL_LEG_END_DATE: return Messages.FixedQueryColumn_LegEndDateColumnName;
			case TRANSPORT_TYPE: return Messages.FixedQueryColumn_TransportColumnName;
			case WAYPOINT_ID: return Messages.FixedQueryColumn_WaypointIdColumnName;
			case WAYPOINT_DATE: return Messages.FixedQueryColumn_WaypointDateColumnName;
			case WAYPOINT_TIME: return Messages.FixedQueryColumn_WaypointTimeColumnName;
			case WAYPOINT_X: return Messages.FixedQueryColumn_xColumnName;
			case WAYPOINT_Y: return Messages.FixedQueryColumn_yColumnName;
			case WAYPOINT_DIRECTION: return Messages.FixedQueryColumn_DirectionColumnName;
			case WAYPOINT_DISTANCE: return Messages.FixedQueryColumn_DistanceColumnName;
			case WAYPOINT_COMMENT: return Messages.FixedQueryColumn_CommentColumnName;
			case WAYPOINT_OBSERVER: return Messages.FixedQueryColumn_ObserverColumnName;
			}
		}
		if (item instanceof PatrolValueOption){
			switch((PatrolValueOption)item){
				case NUM_PATROLS: return Messages.PatrolQueryOptions_ValueOpNumPatrols; 
				case NUM_DAYS: return Messages.PatrolQueryOptions_ValueOpNumberDays;    
				case NUM_NIGHTS: return Messages.PatrolQueryOptions_ValueOpNumberNights;
				case DISTANCE: return Messages.PatrolQueryOptions_ValueOpDistance;      
				case NUM_HOURS: return Messages.PatrolQueryOptions_ValueOpNumberHours;  
				case NUM_MEMBERS: return Messages.PatrolQueryOptions_ValueOpNumEmployees;
				case MAN_HOURS: return Messages.PatrolQueryOptions_ValueOpPersonHrs;    
				case MAN_DAYS: return Messages.PatrolQueryOptions_ValueOpPersonDays;    
				case NUM_PATROLS_TOTAL: return Messages.PatrolQueryOptions_TotalNumPatrols;
				case NUM_DAYS_TOTAL: return Messages.PatrolQueryOptions_TotalNumDays;   
				case DISTANCE_TOTAL: return Messages.PatrolQueryOptions_TotalDistance;  
				case NUM_HOURS_TOTAL: return Messages.PatrolQueryOptions_TotalNumHours; 
				case MAN_HOURS_TOTAL: return Messages.PatrolQueryOptions_TotalPersonHours;
				case MAN_DAYS_TOTAL: return Messages.PatrolQueryOptions_TotalPersonDays;
			}
		}
		if (item instanceof PatrolQueryOption){
			switch((PatrolQueryOption)item){
				case ID: return Messages.PatrolQueryOptions_QueryOpId;
				case ARMED: return Messages.PatrolQueryOptions_QueryOpArmed;
				case STATION: return Messages.PatrolQueryOptions_QueryOpStation;
				case TEAM: return Messages.PatrolQueryOptions_QueryOpTeam;
				case TEAM_KEY: return Messages.PatrolQueryOptions_QueryOpTeam;
				case EMPLOYEE: return Messages.PatrolQueryOptions_QueryOpMember;
				case LEADER: return Messages.PatrolQueryOptions_QueryOpLeader;
				case PILOT: return Messages.PatrolQueryOptions_QueryOpPilot;
				case MANDATE: return Messages.PatrolQueryOptions_QueryOpMandate;
				case MANDATE_KEY: return Messages.PatrolQueryOptions_QueryOpMandate;
				case PATROL_TYPE: return Messages.PatrolQueryOptions_QueryOpType;
				case PATROL_TRANSPORT_TYPE: return Messages.PatrolQueryOptions_QueryOpTransportType;
				case PATROL_TRANSPORT_TYPE_KEY: return Messages.PatrolQueryOptions_QueryOpTransportType;
				case CONSERVATION_AREA: return Messages.PatrolQueryOptions_CaGroupByOptionName;
			}
		}
		if (item instanceof PatrolEndDateField){
			return Messages.PatrolEndDateField_PatrolEndDatefilter;
		}
		if (item instanceof PatrolStartDateField){
			return Messages.PatrolStartDateField_PatrolStartDate;
		}
		return null;
	}

	public static Image getImage(PatrolValueOption item){
		switch(item){
			case NUM_PATROLS:
			case NUM_PATROLS_TOTAL:
				return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_NUM_PATROLS_ICON);
			case NUM_DAYS:
			case NUM_DAYS_TOTAL:
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_DAYS_ICON);
			case NUM_NIGHTS:
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_NIGHTS_ICON);
			case DISTANCE:
			case DISTANCE_TOTAL:
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_DISTANCE_ICON);
			case NUM_HOURS:
			case NUM_HOURS_TOTAL:
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_HOURS_ICON);
			case NUM_MEMBERS:
				return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_NUM_EMPLOYEES_ICON);
			case MAN_HOURS:
			case MAN_HOURS_TOTAL:
				return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_PERSON_HOURS_ICON);
			case MAN_DAYS:
			case MAN_DAYS_TOTAL:
				return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_PERSON_DAYS_ICON);
		}
		return null;
	}
	
	/**
	 * @return the image that represents a particular patrol filter option
	 */
	public static Image getImage(PatrolQueryOption option){
		switch(option){
			case ARMED:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ARMED_ICON);
			case TEAM:
			case TEAM_KEY:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON);
			case STATION:
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STATION_ICON);
			case MANDATE:
			case MANDATE_KEY:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON);
			case LEADER:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_LEADER_ICON);
			case PILOT:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_PILOT_ICON);
			case EMPLOYEE:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MEMBER_ICON);
			case PATROL_TYPE:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
			case PATROL_TRANSPORT_TYPE:
			case PATROL_TRANSPORT_TYPE_KEY:
			default:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
		}
		
	}
}
