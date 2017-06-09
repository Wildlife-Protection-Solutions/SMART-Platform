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

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;
import org.wcs.smart.query.common.engine.IResultItem;
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

	private static final String[][] FIXED_COLUMN_KEY_TO_ROW  = {
		 //NOTE: order is important as we don't want to change "patrolleg" to "pleg"
		{"patrolleg", "pl"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"patrol", "p"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"waypoint", "wp"} //$NON-NLS-1$ //$NON-NLS-2$
	};
	
	/**
	 * The defined fixed columns.
	 */
	public enum FixedColumns{
		CA_ID(ColumnType.STRING,"ca:id"), //$NON-NLS-1$
		CA_NAME(ColumnType.STRING,"ca:name"), //$NON-NLS-1$
		PATROL_ID(ColumnType.STRING, "patrol:id"), //$NON-NLS-1$
		PATROL_TYPE(ColumnType.STRING, "patrol:type"), //$NON-NLS-1$
		PATROL_START_DATE(ColumnType.DATE, "patrol:startdate"), //$NON-NLS-1$
		PATROL_END_DATE(ColumnType.DATE, "patrol:enddate"), //$NON-NLS-1$
		PATROL_STATION(ColumnType.STRING, "patrol:station"), //$NON-NLS-1$
		PATROL_TEAM(ColumnType.STRING, "patrol:team"), //$NON-NLS-1$
		PATROL_OBJETIVE(ColumnType.STRING,"patrol:objective"), //$NON-NLS-1$
//		PATROL_RATING("Objective Rating", ColumnType.INTEGER,"patrol:rating"),
		PATROL_MANDATE(ColumnType.STRING,"patrol:mandate"), //$NON-NLS-1$
		PATROL_ARMED(ColumnType.BOOLEAN,"patrol:armed"), //$NON-NLS-1$
		PATROL_LEG_ID(ColumnType.STRING, "patrol:legid"), //$NON-NLS-1$
		PATROL_LEG_LEADER(ColumnType.STRING, "patrol:leader"), //$NON-NLS-1$
		PATROL_LEG_PILOT(ColumnType.STRING, "patrol:pilot"), //$NON-NLS-1$
		PATROL_LEG_START_DATE(ColumnType.DATE, "patrolleg:startdate"), //$NON-NLS-1$
		PATROL_LEG_END_DATE(ColumnType.DATE, "patrolleg:enddate"), //$NON-NLS-1$
		TRANSPORT_TYPE( ColumnType.STRING,"patrol:transporttype"), //$NON-NLS-1$
		WAYPOINT_ID( ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE(ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME(ColumnType.TIME,"waypoint:time"), //$NON-NLS-1$
		WAYPOINT_X(ColumnType.NUMBER,"waypoint:x"), //$NON-NLS-1$
		WAYPOINT_Y(ColumnType.NUMBER, "waypoint:y"), //$NON-NLS-1$
		WAYPOINT_DIRECTION(ColumnType.NUMBER,"waypoint:direction"), //$NON-NLS-1$
		WAYPOINT_DISTANCE( ColumnType.NUMBER,"waypoint:distance"), //$NON-NLS-1$
		WAYPOINT_COMMENT(ColumnType.STRING,"waypoint:comment"), //$NON-NLS-1$
		WAYPOINT_OBSERVER( ColumnType.STRING,"ob:observer");   //$NON-NLS-1$
		
		private ColumnType type;
		private String key;
		private FixedColumns(ColumnType type, String key){
			this.type = type;
			this.key = key;	
		}
		
		public String getKey(){
			return this.key;
		}
	}
	
	
	private FixedColumns column;
	private Locale l;
	
	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column the column definition
	 */
	public FixedQueryColumn(FixedColumns column, Locale l) {
		super(SmartContext.INSTANCE.getClass(IQueryPatrolLabelProvider.class).getLabel(column,l), column.key, column.type);
		this.column = column;
		this.l = l;
	}

	@Override
	public String getTooltip(){
		if (column == FixedColumns.WAYPOINT_X || column == FixedColumns.WAYPOINT_Y){
			return getProjectionTooltip();
		}
		return null;
	}

	public FixedColumns getColumn(){
		return this.column;
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
				return  SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(item.getPatrolType(), l);
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
				return item.getWaypointX(getProjection());
			case WAYPOINT_Y:
				return item.getWaypointY(getProjection());
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
		FixedQueryColumn newColumn = new FixedQueryColumn(this.column, l);
		newColumn.setEdit(canEdit());
		return newColumn;
	}
	
	public static String getDbColumnName(String key) {
		key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
		for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
			key = key.replace(data[0], data[1]);
		}
		return key;
	}
	
}
