package org.wcs.smart.entity.ui.typelist.editor.sightings;

import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.filter.date.IDateFilter;



public class SightingTableColumns {
	
	
	public static IDateFilter[] SIGHTING_DATE_FILTERS = IDateFilter.DATE_FILTERS;
	
	
	public static enum FixedColumns{
		ENTITY_ID("Entity Id", ColumnType.STRING, "entity:id"),
		CA_ID("Conservation Area Id", ColumnType.STRING,"ca:id"), //$NON-NLS-1$
		CA_NAME("Conservation Area Name", ColumnType.STRING,"ca:name"), //$NON-NLS-1$
		WAYPOINT_SOURCE("Waypoint Source", ColumnType.STRING,"wp:source"),  //$NON-NLS-1$
		WAYPOINT_ID("Waypoint Id", ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE("Waypoint Date", ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME("Waypoint Time", ColumnType.TIME,"waypoint:time"), //$NON-NLS-1$
		WAYPOINT_X("Waypoint X", ColumnType.NUMBER,"waypoint:x"), //$NON-NLS-1$
		WAYPOINT_Y("Waypoint Y", ColumnType.NUMBER, "waypoint:y"), //$NON-NLS-1$
		WAYPOINT_DIRECTION("Direction", ColumnType.NUMBER,"waypoint:direction"), //$NON-NLS-1$
		WAYPOINT_DISTANCE("Distance", ColumnType.NUMBER,"waypoint:distance"), //$NON-NLS-1$
		WAYPOINT_COMMENT("Comment", ColumnType.STRING,"waypoint:comment"); //$NON-NLS-1$
		
		private String guiName;
		private ColumnType type;
		private String key;
		
		private FixedColumns(String name, ColumnType type, String key){
			this.guiName = name;
			this.type = type;
			this.key = key;
		}
		
		public String getGuiName(){
			return this.guiName;
		}
		
		public ColumnType getType(){
			return this.type;
		}
		public String getKey(){
			return this.key;
		}
	}
	
}
