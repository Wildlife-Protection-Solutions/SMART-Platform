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
package org.wcs.smart.query.ui.querytable;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.ColumnLabelProvider;
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
public class FixedTableColumn implements QueryTableColumn {

	/**
	 * The defined fixed columns.
	 */
	public enum FIXED_COLUMN{
		PATROL_ID("Patrol ID", ColumnType.STRING, "patrol:id"),
		PATROL_TYPE("Type", ColumnType.STRING, "patrol:type"),
		PATROL_START_DATE("Patrol Start Date", ColumnType.DATE, "patrol:startdate"),
		PATROL_END_DATE("Patrol End Date", ColumnType.DATE, "patrol:enddate"),
		PATROL_STATION("Station", ColumnType.STRING, "patrol:station"),
		PATROL_TEAM("Team", ColumnType.STRING, "patrol:team"),
		PATROL_OBJETIVE("Objective", ColumnType.STRING,"patrol:objective"),
		PATROL_RATING("Objective Rating", ColumnType.INTEGER,"patrol:rating"),
		PATROL_MANDATE("Mandate", ColumnType.STRING,"patrol:mandate"),
		PATROL_ARMED("Armed", ColumnType.BOOLEAN,"patrol:armed"),
		PATROL_LEG_ID("Patrol Leg Id", ColumnType.STRING, "patrol:legid"),
		PATROL_LEG_LEADER("Leader", ColumnType.STRING, "patrol:leader"),
		PATROL_LEG_PILOT("Pilot", ColumnType.STRING, "patrol:pilot"),
		TRANSPORT_TYPE("Patrol Transport Type", ColumnType.STRING,"patrol:transporttype"),
		WAYPOINT_ID("Waypoint Id", ColumnType.INTEGER,"waypoint:id"),
		WAYPOINT_DATE("Waypoint Date", ColumnType.DATE,"waypoint:date"),
		WAYPOINT_TIME("Waypoint Time", ColumnType.DATE,"waypoint:time"),
		WAYPOINT_X("X", ColumnType.NUMBER,"waypoint:x"),
		WAYPOINT_Y("Y", ColumnType.NUMBER, "waypoint:y"),
		WAYPOINT_DIRECTION("Direction", ColumnType.NUMBER,"waypoint:direction"),
		WAYPOINT_DISTANCE("Distance", ColumnType.NUMBER,"waypoint:distance"),
		WAYPOINT_COMMENT("Comment", ColumnType.STRING,"waypoint:comment");
		
		private String guiName;
		private ColumnType type;
		private String key;
		private FIXED_COLUMN(String name, ColumnType type, String key){
			this.guiName = name;
			this.type = type;
			this.key = key;
		}
	}
	
	
	private FIXED_COLUMN column;
	private ColumnLabelProvider provider;

	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column the column definition
	 */
	public FixedTableColumn(FIXED_COLUMN column) {
		this.column = column;
	}

	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getName()
	 */
	@Override
	public String getName() {
		return this.column.guiName;
	}

	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getType()
	 */
	@Override
	public ColumnType getType() {
		return this.column.type;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getKey()
	 */
	@Override
	public String getKey() {
		return this.column.key;
	}

	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getLabelProvider()
	 */
	@Override
	public ColumnLabelProvider getLabelProvider() {
		if (provider == null) {
			provider = getLabelProvider(this);
		}
		return provider;
	}

	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getValue(org.wcs.smart.query.model.QueryResultItem)
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
		case PATROL_RATING:
			return item.getObjectiveRating();
		case PATROL_START_DATE:
			return item.getPatrolStartDate();
		case PATROL_STATION:
			return item.getStation();
		case PATROL_LEG_LEADER:
			return item.getLeader();
		case PATROL_LEG_PILOT:
			return item.getPilot();
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

	private static ColumnLabelProvider getLabelProvider(final FixedTableColumn col) {

		ColumnLabelProvider provider = new ColumnLabelProvider() {
			/*
			 * @see
			 * org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object
			 * )
			 */
			public String getText(Object element) {
				if (element instanceof QueryResultItem) {
					return asString(col.getValue((QueryResultItem) element), col.column);
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		};

		return provider;
	}

	private static String asString(Object value, FIXED_COLUMN col) {
		if (col.type == ColumnType.BOOLEAN){
			if ((Boolean)value){
				return "Yes";
			}else{
				return "No";
			}
		}else if (col.type == ColumnType.DATE){
			if (col == FIXED_COLUMN.WAYPOINT_TIME){
				return DateFormat.getTimeInstance().format((Date)value);
			}else{
				return DateFormat.getDateInstance().format((Date)value);
			}
		}else if (col.type == ColumnType.STRING){
			return (String)value;
		}else if (col.type == ColumnType.INTEGER){
			return String.valueOf((Integer)value);
		}else if (col.type == ColumnType.NUMBER){
			return String.valueOf((Double)value);
		}
		return "";

	}
}
