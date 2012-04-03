package org.wcs.smart.query.ui.querytable.column;

import java.text.DateFormat;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.query.model.QueryResultItem;

public class FixedTableColumn implements QueryTableColumn {

	public enum FIXED_COLUMN{
		PATROL_ID("Patrol ID"),
		PATROL_TYPE("Type"),
		PATROL_START_DATE("Patrol Start Date"),
		PATROL_END_DATE("Patrol End Date"),
		PATROL_STATION("Station"),
		PATROL_TEAM("Team"),
		PATROL_OBJETIVE("Objective"),
		PATROL_RATING("Objective Rating"),
		PATROL_MANDATE("Mandate"),
		PATROL_ARMED("Armed"),
		PATROL_LEG_ID("Patrol Leg Id"),
		TRANSPORT_TYPE("Patrol Transport Type"),
		WAYPOINT_ID("Waypoint Id"),
		WAYPOINT_DATE("Waypoint Date"),
		WAYPOINT_TIME("Waypoint Time"),
		WAYPOINT_X("X"),
		WAYPOINT_Y("Y"),
		WAYPOINT_DIRECTION("Direction"),
		WAYPOINT_DISTANCE("Distance"),
		WAYPOINT_COMMENT("Comment");
		
		String name;
		private FIXED_COLUMN(String name){
			this.name = name;
		}
	}
	
	
	private String name;
	private FIXED_COLUMN column;
	 private ColumnLabelProvider provider;
	
	public FixedTableColumn(FIXED_COLUMN column){
		this.name = column.name;
		this.column = column;
	}
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public ColumnLabelProvider getLabelProvider() {
		if (provider == null){
			provider = getLabelProvider(column);
		}
		return provider;
	}

	
	private static ColumnLabelProvider getLabelProvider(final FIXED_COLUMN col){
		
		ColumnLabelProvider provider = new ColumnLabelProvider(){
			/* 
			 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
			 */
			public String getText(Object element) {
				if (element instanceof QueryResultItem){
					return getValue(col, (QueryResultItem)element );
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		};
		
		return provider;
	}
	
	private static String getValue(FIXED_COLUMN col, QueryResultItem item){
		switch(col){
		case PATROL_ARMED:
			if (item.isArmed()){
				return "Yes";
			}else{
				return "No";
			}
		case PATROL_END_DATE:
			return DateFormat.getDateInstance().format(item.getPatrolEndDate());
		case PATROL_ID:
			return item.getPatrolId();
		case PATROL_LEG_ID:
			return item.getPatrolLegId();
		case PATROL_MANDATE:
			return item.getMandate();
		case PATROL_OBJETIVE:
			return item.getObjective();
		case PATROL_RATING:
			return String.valueOf(item.getObjectiveRating());
		case PATROL_START_DATE:
			return DateFormat.getDateInstance().format(item.getPatrolStartDate());
		case PATROL_STATION:
			return item.getStation();
		case PATROL_TEAM:
			return item.getTeam();
		case PATROL_TYPE:
			return item.getPatrolType().getGuiName();
		case WAYPOINT_ID:
			return String.valueOf(item.getWaypointId());
		case TRANSPORT_TYPE:
			return item.getTransportType();
		case WAYPOINT_COMMENT:
			return item.getWaypointComment();
		case WAYPOINT_DATE:
			return DateFormat.getDateInstance().format(item.getWpDateTime());
		case WAYPOINT_DIRECTION:
			return String.valueOf(item.getWaypointDirection());
		case WAYPOINT_DISTANCE:
			return String.valueOf(item.getWaypointDistance());
		case WAYPOINT_TIME:
			return DateFormat.getTimeInstance().format(item.getWaypointTime());
		case WAYPOINT_X:
			return String.valueOf(item.getWaypointX());
		case WAYPOINT_Y:
			return String.valueOf(item.getWaypointY());
		}
		return "";
		
	}
}
