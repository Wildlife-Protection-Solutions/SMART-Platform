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

import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryPlugIn;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolEndMonthDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolEndQuarterDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.model.PatrolStartMonthDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolStartQuarterDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.TrackGeometryQueryColumn;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItemAreaBuffer;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;
import org.wcs.smart.patrol.ui.LabelConstants;
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
			case PATROL_ID: return LabelConstants.ID;
			case PATROL_TYPE: return LabelConstants.TRACK_TYPE;
			case PATROL_START_DATE: return Messages.PatrolQueryLabelProvider_StartDateColumnName;
			case PATROL_END_DATE: return Messages.PatrolQueryLabelProvider_EndDateColumnName;
			case PATROL_STATION: return LabelConstants.STATION_NAME;
			case PATROL_TEAM: return LabelConstants.TEAM_NAME;
			case PATROL_OBJETIVE: return LabelConstants.OBJECTIVE;
			case PATROL_MANDATE: return LabelConstants.MANDATE_NAME;
			case PATROL_ARMED: return LabelConstants.ARMED;
			case PATROL_START_TIME: return Messages.PatrolQueryLabelProvider_StartTimeColumnName;
			case PATROL_END_TIME: return Messages.PatrolQueryLabelProvider_EndTimeColumnName;
			case PATROL_LEG_ID: return Messages.PatrolQueryLabelProvider_LegIdColumnName;
			case PATROL_LEG_LEADER: return LabelConstants.LEADER;
			case PATROL_LEG_MEMBERS: return LabelConstants.MEMBERS;
			case PATROL_LEG_PILOT: return LabelConstants.PILOT;
			case PATROL_LEG_START_DATE: return Messages.PatrolQueryLabelProvider_LegStartDateColumnName;
			case PATROL_LEG_END_DATE: return Messages.PatrolQueryLabelProvider_LegEndDateColumnName;
			case TRANSPORT_TYPE: return LabelConstants.TRANSPORT_MODE;
			case TRANSPORT_GROUP: return LabelConstants.ENVIRONMENT_NAME;
			case WAYPOINT_ID: return Messages.FixedQueryColumn_WaypointIdColumnName;
			case OBS_GROUP_ID: return Messages.PatrolQueryLabelProvider_ObsGroupColumnName;
			case WAYPOINT_DATE: return Messages.FixedQueryColumn_WaypointDateColumnName;
			case WAYPOINT_TIME: return Messages.FixedQueryColumn_WaypointTimeColumnName;
			case WAYPOINT_X: return Messages.FixedQueryColumn_xColumnName;
			case WAYPOINT_Y: return Messages.FixedQueryColumn_yColumnName;
			case WAYPOINT_RAWX: return Messages.PatrolQueryLabelProvider_RawXColumnName;
			case WAYPOINT_RAWY: return Messages.PatrolQueryLabelProvider_RawYColumnName;
			case WAYPOINT_DIRECTION: return Messages.FixedQueryColumn_DirectionColumnName;
			case WAYPOINT_DISTANCE: return Messages.FixedQueryColumn_DistanceColumnName;
			case WAYPOINT_COMMENT: return Messages.FixedQueryColumn_CommentColumnName;
			case WAYPOINT_OBSERVER: return Messages.FixedQueryColumn_ObserverColumnName;
			case WAYPOINT_LASTMODIFIED: return Messages.PatrolQueryLabelProvider_LastModified_ColumnName;
			case WAYPOINT_LASTMODIFIEDBY: return Messages.PatrolQueryLabelProvider_LastModifiedBy_ColumnName;
			case OBSERVATION_UUID: return Messages.PatrolQueryLabelProvider_ObservationUUIDColumnName;
			case PATROL_UUID: return Messages.PatrolQueryLabelProvider_PatrolUUIDColumnName2;
			case WAYPOINT_UUID: return Messages.PatrolQueryLabelProvider_WaypointUUIDColumnName;
			default:
				break;
			}
		}
		if (item instanceof PatrolValueOption){
			switch((PatrolValueOption)item){
				case NUM_PATROLS: return Messages.PatrolQueryOptions_ValueOpNumPatrols2; 
				case NUM_DAYS: return Messages.PatrolQueryOptions_ValueOpNumberDays;    
				case NUM_NIGHTS: return Messages.PatrolQueryOptions_ValueOpNumberNights;
				case NUM_CUSTOM: return Messages.PatrolQueryLabelProvider_NumTimeRanges;
				case DISTANCE: return Messages.PatrolQueryOptions_ValueOpDistance;
				case AREA_BUFFER: return Messages.PatrolQueryLabelProvider_TrackAreaValueItem;
				case NUM_PATROLHOURS: return Messages.PatrolQueryOptions_ValueOpNumberPatrolHours2;
				case PATROLHOURS_TRACK: return Messages.PatrolQueryOptions_ValueOpNumberPatrolHours2;
				case NUM_FIELDHOURS: return Messages.PatrolQueryOptions_ValueOpNumberActivePatrolHours2;  
				case NUM_MEMBERS: return Messages.PatrolQueryOptions_ValueOpNumEmployees;
				case MAN_HOURS: return Messages.PatrolQueryOptions_ValueOpPersonHrs;    
				case MAN_DAYS: return Messages.PatrolQueryOptions_ValueOpPersonDays;    
				case NUM_PATROLS_TOTAL: return Messages.PatrolQueryOptions_TotalNumPatrols;
				case NUM_DAYS_TOTAL: return Messages.PatrolQueryOptions_TotalNumDays;   
				case DISTANCE_TOTAL: return Messages.PatrolQueryOptions_TotalDistance;  
				case NUM_PATROLHOURS_TOTAL: return Messages.PatrolQueryOptions_TotalNumHours12;
				case NUM_FIELDHOURS_TOTAL: return Messages.PatrolQueryLabelProvider_TotalActivePatrolHours2;
				case MAN_HOURS_TOTAL: return Messages.PatrolQueryOptions_TotalPersonHours1;
				case MAN_DAYS_TOTAL: return Messages.PatrolQueryOptions_TotalPersonDays;
			}
		}
		if (item instanceof PatrolQueryOption){
			switch((PatrolQueryOption)item){
				case ID: return LabelConstants.ID;
				case ARMED: return LabelConstants.ARMED;
				case STATION: return LabelConstants.STATION_NAME;
				case TEAM: return LabelConstants.TEAM_NAME;
				case TEAM_KEY: return LabelConstants.TEAM_NAME;
				case EMPLOYEE: return LabelConstants.MEMBER;
				case LEADER: return LabelConstants.LEADER;
				case PILOT: return LabelConstants.PILOT;
				case MANDATE: return LabelConstants.MANDATE_NAME;
				case MANDATE_KEY: return LabelConstants.MANDATE_NAME;
				case PATROL_TYPE: return LabelConstants.TRACK_TYPE;
				case CM: return Messages.PatrolQueryLabelProvider_ConfigurableModel;
				case PATROL_TRANSPORT_TYPE: return LabelConstants.TRANSPORT_MODE;
				case PATROL_TRANSPORT_TYPE_KEY: return LabelConstants.TRANSPORT_MODE;
				case PATROL_TRANSPORT_GROUP_KEY: return LabelConstants.ENVIRONMENT_NAME;
				case PATROL_TRANSPORT_PATROL_GROUP_KEY: return MessageFormat.format(Messages.PatrolQueryLabelProvider_EnvironmentPatrolLevelOptionName, LabelConstants.ENVIRONMENT_NAME);
				case CONSERVATION_AREA: return Messages.PatrolQueryOptions_CaGroupByOptionName;
				case AGENCY: return Messages.PatrolQueryLabelProvider_AgencyLabel;
				case AGENCY_KEY: return Messages.PatrolQueryLabelProvider_AgencyLabel;
				case RANK: return Messages.PatrolQueryLabelProvider_RankLabel;
			}
		}
		if (item instanceof PatrolEndDateField){
			return Messages.PatrolEndDateField_PatrolEndDatefilter2;
		}
		if (item instanceof PatrolStartDateField){
			return Messages.PatrolStartDateField_PatrolStartDate2;
		}
		if (item instanceof PatrolStartQuarterDateGroupBy) {
			return Messages.PatrolQueryLabelProvider_PatrolStartQuarter2;
		}
		if (item instanceof PatrolEndQuarterDateGroupBy) {
			return Messages.PatrolQueryLabelProvider_PatrolEndQuarter2;
		}
		if (item instanceof PatrolStartMonthDateGroupBy){
			return Messages.PatrolQueryLabelProvider_PatrolStartMonthOp2;
		}
		if (item instanceof PatrolEndMonthDateGroupBy){
			return Messages.PatrolQueryLabelProvider_PatrolEndMonthOp2;
		}
		if (item == PatrolValueItemAreaBuffer.ERROR_MSG_KEY) {
			return Messages.PatrolQueryLabelProvider_InvalidBufferValue;
		}
		if (item == TrackGeometryQueryColumn.KEY) { 
			return Messages.PatrolQueryLabelProvider_TrackColumnName2;
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
			case NUM_PATROLHOURS:
			case NUM_FIELDHOURS:
			case NUM_FIELDHOURS_TOTAL:
			case NUM_PATROLHOURS_TOTAL:
			case PATROLHOURS_TRACK:
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_HOURS_ICON);
			case NUM_MEMBERS:
				return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_NUM_EMPLOYEES_ICON);
			case MAN_HOURS:
			case MAN_HOURS_TOTAL:
				return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_PERSON_HOURS_ICON);
			case MAN_DAYS:
			case MAN_DAYS_TOTAL:
				return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_PERSON_DAYS_ICON);
			case NUM_CUSTOM:
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_TIMERANGE_ICON);
		default:
			break;
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
			case CONSERVATION_AREA:
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
			case CM:
				return DataentryPlugIn.getDefault().getImageRegistry().get(DataentryPlugIn.CONFIG_MODEL_ICON);
			case PATROL_TRANSPORT_TYPE:
			case PATROL_TRANSPORT_TYPE_KEY:
			default:
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TRANSPORTTYPE_ICON);
		}
		
	}
}
