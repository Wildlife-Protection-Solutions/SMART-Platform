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

import org.wcs.smart.connect.i18n.Messages;
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
			case CA_ID: return Messages.getString("PatrolQueryLabelProvider.CaId", l); //$NON-NLS-1$
			case CA_NAME: return Messages.getString("PatrolQueryLabelProvider.CaName", l); //$NON-NLS-1$
			case PATROL_ID: return Messages.getString("PatrolQueryLabelProvider.PId", l); //$NON-NLS-1$
			case PATROL_TYPE: return Messages.getString("PatrolQueryLabelProvider.PatrolType", l); //$NON-NLS-1$
			case PATROL_START_DATE: return Messages.getString("PatrolQueryLabelProvider.PStartDate", l); //$NON-NLS-1$
			case PATROL_END_DATE: return Messages.getString("PatrolQueryLabelProvider.PEndDate", l); //$NON-NLS-1$
			case PATROL_STATION: return Messages.getString("PatrolQueryLabelProvider.StationName", l); //$NON-NLS-1$
			case PATROL_TEAM: return Messages.getString("PatrolQueryLabelProvider.TeamName", l); //$NON-NLS-1$
			case PATROL_OBJETIVE: return Messages.getString("PatrolQueryLabelProvider.Objective", l); //$NON-NLS-1$
			case PATROL_MANDATE: return Messages.getString("PatrolQueryLabelProvider.Mandate", l); //$NON-NLS-1$
			case PATROL_ARMED: return Messages.getString("PatrolQueryLabelProvider.Armed", l); //$NON-NLS-1$
			case PATROL_LEG_ID: return Messages.getString("PatrolQueryLabelProvider.LegId", l); //$NON-NLS-1$
			case PATROL_LEG_LEADER: return Messages.getString("PatrolQueryLabelProvider.Leader", l); //$NON-NLS-1$
			case PATROL_LEG_PILOT: return Messages.getString("PatrolQueryLabelProvider.Pilot", l); //$NON-NLS-1$
			case PATROL_LEG_START_DATE: return Messages.getString("PatrolQueryLabelProvider.LegStartdate", l); //$NON-NLS-1$
			case PATROL_LEG_END_DATE: return Messages.getString("PatrolQueryLabelProvider.LegEnddate", l); //$NON-NLS-1$
			case TRANSPORT_TYPE: return Messages.getString("PatrolQueryLabelProvider.Transporttype", l); //$NON-NLS-1$
			case WAYPOINT_ID: return Messages.getString("PatrolQueryLabelProvider.Wid", l); //$NON-NLS-1$
			case WAYPOINT_DATE: return Messages.getString("PatrolQueryLabelProvider.WPDate", l); //$NON-NLS-1$
			case WAYPOINT_TIME: return Messages.getString("PatrolQueryLabelProvider.WPTime", l); //$NON-NLS-1$
			case WAYPOINT_X: return Messages.getString("PatrolQueryLabelProvider.WPX", l); //$NON-NLS-1$
			case WAYPOINT_Y: return Messages.getString("PatrolQueryLabelProvider.WPY", l); //$NON-NLS-1$
			case WAYPOINT_DIRECTION: return Messages.getString("PatrolQueryLabelProvider.WPDirection", l); //$NON-NLS-1$
			case WAYPOINT_DISTANCE: return Messages.getString("PatrolQueryLabelProvider.WPDistance", l); //$NON-NLS-1$
			case WAYPOINT_COMMENT: return Messages.getString("PatrolQueryLabelProvider.WPComment", l); //$NON-NLS-1$
			case WAYPOINT_OBSERVER: return Messages.getString("PatrolQueryLabelProvider.Observer", l); //$NON-NLS-1$
			}
		}
		if (item instanceof PatrolValueOption){
			switch((PatrolValueOption)item){
				case NUM_PATROLS: return Messages.getString("PatrolQueryLabelProvider.NumPatrolsOp", l);  //$NON-NLS-1$
				case NUM_DAYS: return Messages.getString("PatrolQueryLabelProvider.NumDaysOp", l);     //$NON-NLS-1$
				case NUM_NIGHTS: return Messages.getString("PatrolQueryLabelProvider.NumNightsOp", l); //$NON-NLS-1$
				case DISTANCE: return Messages.getString("PatrolQueryLabelProvider.DistanceOp", l);       //$NON-NLS-1$
				case NUM_FIELDHOURS: return Messages.getString("PatrolQueryLabelProvider.NumberActiveHoursOp", l); //$NON-NLS-1$
				case NUM_PATROLHOURS: return Messages.getString("PatrolQueryLabelProvider.NumberHoursOp", l);   //$NON-NLS-1$
				case NUM_MEMBERS: return Messages.getString("PatrolQueryLabelProvider.NumEmployeesOp", l); //$NON-NLS-1$
				case MAN_HOURS: return Messages.getString("PatrolQueryLabelProvider.PersonFieldHoursOp", l);     //$NON-NLS-1$
				case MAN_DAYS: return Messages.getString("PatrolQueryLabelProvider.PersonDaysOp", l); //$NON-NLS-1$
				
				case NUM_PATROLS_TOTAL: return Messages.getString("PatrolQueryLabelProvider.TotalPatrolsOp", l); //$NON-NLS-1$
				case NUM_DAYS_TOTAL: return Messages.getString("PatrolQueryLabelProvider.TotalDaysOp", l);    //$NON-NLS-1$
				case DISTANCE_TOTAL: return Messages.getString("PatrolQueryLabelProvider.TotalDistanceOp", l);   //$NON-NLS-1$
				case NUM_FIELDHOURS_TOTAL: return Messages.getString("PatrolQueryLabelProvider.TotalActiveHoursOp", l); //$NON-NLS-1$
				case NUM_PATROLHOURS_TOTAL: return Messages.getString("PatrolQueryLabelProvider.TotalPatroHrsOp", l); //$NON-NLS-1$
				case MAN_HOURS_TOTAL: return Messages.getString("PatrolQueryLabelProvider.TotalPersonFieldHrs", l); //$NON-NLS-1$
				case MAN_DAYS_TOTAL: return Messages.getString("PatrolQueryLabelProvider.TotalPersonDaysOp", l); //$NON-NLS-1$
			}
		}
		if (item instanceof PatrolQueryOption){
			switch((PatrolQueryOption)item){
				case ID: return Messages.getString("PatrolQueryLabelProvider.PatrolIdOp", l); //$NON-NLS-1$
				case ARMED: return Messages.getString("PatrolQueryLabelProvider.ArmedOp", l); //$NON-NLS-1$
				case STATION: return Messages.getString("PatrolQueryLabelProvider.StationOp", l); //$NON-NLS-1$
				case TEAM: return Messages.getString("PatrolQueryLabelProvider.TeamOp", l); //$NON-NLS-1$
				case TEAM_KEY: return Messages.getString("PatrolQueryLabelProvider.TeamOp", l); //$NON-NLS-1$
				case EMPLOYEE: return Messages.getString("PatrolQueryLabelProvider.EmployeeOp", l); //$NON-NLS-1$
				case LEADER: return Messages.getString("PatrolQueryLabelProvider.LeaderOp", l); //$NON-NLS-1$
				case PILOT: return Messages.getString("PatrolQueryLabelProvider.PilotOp", l); //$NON-NLS-1$
				case MANDATE: return Messages.getString("PatrolQueryLabelProvider.MandateOp", l); //$NON-NLS-1$
				case MANDATE_KEY: return Messages.getString("PatrolQueryLabelProvider.MandateOp", l); //$NON-NLS-1$
				case PATROL_TYPE: return Messages.getString("PatrolQueryLabelProvider.PatrolTypeOp", l); //$NON-NLS-1$
				case PATROL_TRANSPORT_TYPE: return Messages.getString("PatrolQueryLabelProvider.TransportTypeOp", l); //$NON-NLS-1$
				case PATROL_TRANSPORT_TYPE_KEY: return Messages.getString("PatrolQueryLabelProvider.TransportTypeOp", l); //$NON-NLS-1$
				case CONSERVATION_AREA: return Messages.getString("PatrolQueryLabelProvider.CaOp", l); //$NON-NLS-1$
				case AGENCY: return Messages.getString("PatrolQueryLabelProvider.AgencyOp", l); //$NON-NLS-1$
				case RANK: return Messages.getString("PatrolQueryLabelProvider.RankOp", l); //$NON-NLS-1$
			}
		}
		if (item instanceof PatrolEndDateField){
			return Messages.getString("PatrolQueryLabelProvider.PatrolEndDateQueryFilterfield", l); //$NON-NLS-1$
		}
		if (item instanceof PatrolStartDateField){
			return Messages.getString("PatrolQueryLabelProvider.PatrolStartDateQueryFilterfield", l); //$NON-NLS-1$
		}
		return null;
	}

}
