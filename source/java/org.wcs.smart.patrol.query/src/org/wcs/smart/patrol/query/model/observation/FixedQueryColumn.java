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
package org.wcs.smart.patrol.query.model.observation;

import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Class represents one of the fixed table columns that
 * do not change from conservation area to conservation area.
 * 
 * <p>This includes items such as the patrol id, patrol type etc
 * but not items related to the datamodel.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class FixedQueryColumn extends QueryColumn {

	/**
	 * The defined fixed columns.
	 */
	public enum FixedColumns{
		CA_ID(Messages.FixedQueryColumn_CaIdColumnName, ColumnType.STRING,"ca:id"), //$NON-NLS-1$
		CA_NAME(Messages.FixedQueryColumn_CaNameColumnName, ColumnType.STRING,"ca:name"), //$NON-NLS-1$
		PATROL_ID(Messages.FixedQueryColumn_PatrolIdColumnName, ColumnType.STRING, "patrol:id"), //$NON-NLS-1$
		PATROL_TYPE(Messages.FixedQueryColumn_TypeColumnName, ColumnType.STRING, "patrol:type"), //$NON-NLS-1$
		PATROL_START_DATE(Messages.FixedQueryColumn_PatrolStartDateColumnName, ColumnType.DATE, "patrol:startdate"), //$NON-NLS-1$
		PATROL_END_DATE(Messages.FixedQueryColumn_PatrolEndDateColumnName, ColumnType.DATE, "patrol:enddate"), //$NON-NLS-1$
		PATROL_STATION(Messages.FixedQueryColumn_StationColumnName, ColumnType.STRING, "patrol:station"), //$NON-NLS-1$
		PATROL_TEAM(Messages.FixedQueryColumn_TeamColumnName, ColumnType.STRING, "patrol:team"), //$NON-NLS-1$
		PATROL_OBJETIVE(Messages.FixedQueryColumn_ObjectiveColumnName, ColumnType.STRING,"patrol:objective"), //$NON-NLS-1$
//		PATROL_RATING("Objective Rating", ColumnType.INTEGER,"patrol:rating"),
		PATROL_MANDATE(Messages.FixedQueryColumn_MandateColumnName, ColumnType.STRING,"patrol:mandate"), //$NON-NLS-1$
		PATROL_ARMED(Messages.FixedQueryColumn_ArmedColumnName, ColumnType.BOOLEAN,"patrol:armed"), //$NON-NLS-1$
		PATROL_LEG_ID(Messages.FixedQueryColumn_LegIdColumnName, ColumnType.STRING, "patrol:legid"), //$NON-NLS-1$
		PATROL_LEG_LEADER(Messages.FixedQueryColumn_LeaderColumnName, ColumnType.STRING, "patrol:leader"), //$NON-NLS-1$
		PATROL_LEG_PILOT(Messages.FixedQueryColumn_PilotColumnName, ColumnType.STRING, "patrol:pilot"), //$NON-NLS-1$
		PATROL_LEG_START_DATE(Messages.FixedQueryColumn_LegStartDateColumnName, ColumnType.DATE, "patrolleg:startdate"), //$NON-NLS-1$
		PATROL_LEG_END_DATE(Messages.FixedQueryColumn_LegEndDateColumnName, ColumnType.DATE, "patrolleg:enddate"), //$NON-NLS-1$
		TRANSPORT_TYPE(Messages.FixedQueryColumn_TransportColumnName, ColumnType.STRING,"patrol:transporttype"), //$NON-NLS-1$
		WAYPOINT_ID(Messages.FixedQueryColumn_WaypointIdColumnName, ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE(Messages.FixedQueryColumn_WaypointDateColumnName, ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME(Messages.FixedQueryColumn_WaypointTimeColumnName, ColumnType.TIME,"waypoint:time"), //$NON-NLS-1$
		WAYPOINT_X(Messages.FixedQueryColumn_xColumnName, ColumnType.NUMBER,"waypoint:x"), //$NON-NLS-1$
		WAYPOINT_Y(Messages.FixedQueryColumn_yColumnName, ColumnType.NUMBER, "waypoint:y"), //$NON-NLS-1$
		WAYPOINT_DIRECTION(Messages.FixedQueryColumn_DirectionColumnName, ColumnType.NUMBER,"waypoint:direction"), //$NON-NLS-1$
		WAYPOINT_DISTANCE(Messages.FixedQueryColumn_DistanceColumnName, ColumnType.NUMBER,"waypoint:distance"), //$NON-NLS-1$
		WAYPOINT_COMMENT(Messages.FixedQueryColumn_CommentColumnName, ColumnType.STRING,"waypoint:comment"), //$NON-NLS-1$
		WAYPOINT_OBSERVER(Messages.FixedQueryColumn_ObserverColumnName, ColumnType.STRING,"ob:observer");   //$NON-NLS-1$
		
		private String guiName;
		private ColumnType type;
		private String key;
		private FixedColumns(String name, ColumnType type, String key){
			this.guiName = name;
			this.type = type;
			this.key = key;	
		}
		
		public String getKey(){
			return this.key;
		}
	}
	
	
	private FixedColumns column;
	
	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column the column definition
	 */
	public FixedQueryColumn(FixedColumns column) {
		super(column.guiName, column.key, column.type);
		this.column = column;
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#getValue(org.wcs.smart.patrol.query.model.PatrolQueryResultItem)
	 */
	public Object getValue(IResultItem queryResultItem) {
		if (queryResultItem instanceof PatrolQueryResultItem) {
			PatrolQueryResultItem item = (PatrolQueryResultItem) queryResultItem;

			switch (column) {
			case PATROL_ARMED:
				if (item.isArmed()) {
					return Boolean.TRUE;
				} else {
					return Boolean.FALSE;
				}
			case PATROL_END_DATE:
				return item.getPatrolEndDate();
			case PATROL_ID:
				return item.getPatrolId();
			case PATROL_LEG_ID:
				return item.getPatrolLegId();
			case PATROL_MANDATE:
				return item.getMandate();
			case PATROL_OBJETIVE:
				return item.getObjective();
			case PATROL_START_DATE:
				return item.getPatrolStartDate();
			case PATROL_STATION:
				return item.getStation();
			case PATROL_LEG_LEADER:
				return item.getLeader();
			case PATROL_LEG_PILOT:
				return item.getPilot();
			case PATROL_LEG_START_DATE:
				return item.getPatrolLegStartDate();
			case PATROL_LEG_END_DATE:
				return item.getPatrolLegEndDate();
			case PATROL_TEAM:
				return item.getTeam();
			case PATROL_TYPE:
				return item.getPatrolType().getGuiName();
			case WAYPOINT_ID:
				return item.getWaypointId();
			case TRANSPORT_TYPE:
				return item.getTransportType();
			case WAYPOINT_COMMENT:
				return item.getWaypointComment();
			case WAYPOINT_DATE:
				return item.getWpDateTime();
			case WAYPOINT_DIRECTION:
				return item.getWaypointDirection();
			case WAYPOINT_DISTANCE:
				return item.getWaypointDistance();
			case WAYPOINT_TIME:
				return item.getWaypointTime();
			case WAYPOINT_X:
				return item.getWaypointX();
			case WAYPOINT_Y:
				return item.getWaypointY();
			case CA_ID:
				return item.getConservationAreaId();
			case CA_NAME:
				return item.getConservationAreaName();
			case WAYPOINT_OBSERVER:
				return item.getWaypointObserver();
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		FixedQueryColumn newColumn = new FixedQueryColumn(this.column);
		return newColumn;
	}
}
