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
package org.wcs.smart.query.model.observation;

import org.wcs.smart.query.model.QueryResultItem;

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
		PATROL_ID("Patrol ID", ColumnType.STRING, "patrol:id"),
		PATROL_TYPE("Type", ColumnType.STRING, "patrol:type"),
		PATROL_START_DATE("Patrol Start Date", ColumnType.DATE, "patrol:startdate"),
		PATROL_END_DATE("Patrol End Date", ColumnType.DATE, "patrol:enddate"),
		PATROL_STATION("Station", ColumnType.STRING, "patrol:station"),
		PATROL_TEAM("Team", ColumnType.STRING, "patrol:team"),
		PATROL_OBJETIVE("Objective", ColumnType.STRING,"patrol:objective"),
//		PATROL_RATING("Objective Rating", ColumnType.INTEGER,"patrol:rating"),
		PATROL_MANDATE("Mandate", ColumnType.STRING,"patrol:mandate"),
		PATROL_ARMED("Armed", ColumnType.BOOLEAN,"patrol:armed"),
		PATROL_LEG_ID("Patrol Leg Id", ColumnType.STRING, "patrol:legid"),
		PATROL_LEG_LEADER("Leader", ColumnType.STRING, "patrol:leader"),
		PATROL_LEG_PILOT("Pilot", ColumnType.STRING, "patrol:pilot"),
		PATROL_LEG_START_DATE("Patrol Leg Start Date", ColumnType.DATE, "patrolleg:startdate"),
		PATROL_LEG_END_DATE("Patrol Leg End Date", ColumnType.DATE, "patrolleg:enddate"),
		TRANSPORT_TYPE("Patrol Transport Type", ColumnType.STRING,"patrol:transporttype"),
		WAYPOINT_ID("Waypoint Id", ColumnType.INTEGER,"waypoint:id"),
		WAYPOINT_DATE("Waypoint Date", ColumnType.DATE,"waypoint:date"),
		WAYPOINT_TIME("Waypoint Time", ColumnType.TIME,"waypoint:time"),
		WAYPOINT_X("X", ColumnType.NUMBER,"waypoint:x"),
		WAYPOINT_Y("Y", ColumnType.NUMBER, "waypoint:y"),
		WAYPOINT_DIRECTION("Direction", ColumnType.NUMBER,"waypoint:direction"),
		WAYPOINT_DISTANCE("Distance", ColumnType.NUMBER,"waypoint:distance"),
		WAYPOINT_COMMENT("Comment", ColumnType.STRING,"waypoint:comment");
		
		private String guiName;
		private ColumnType type;
		private String key;
		private FixedColumns(String name, ColumnType type, String key){
			this.guiName = name;
			this.type = type;
			this.key = key;
			
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
	 * @see org.wcs.smart.query.model.observation.QueryColumn#getValue(org.wcs.smart.query.model.QueryResultItem)
	 */
	public Object getValue(QueryResultItem item) {
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
//		case PATROL_RATING:
//			return item.getObjectiveRating();
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
		}
		return "";
	}


	/**
	 * @see org.wcs.smart.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		FixedQueryColumn newColumn = new FixedQueryColumn(this.column);
		return newColumn;
	}
}
