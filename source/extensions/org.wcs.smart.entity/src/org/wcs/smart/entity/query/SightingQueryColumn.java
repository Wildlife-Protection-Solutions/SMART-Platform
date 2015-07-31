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
package org.wcs.smart.entity.query;

import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.date.IDateFilter;


/**
 * A column available for the sightings table. 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SightingQueryColumn extends QueryColumn{

	public static IDateFilter[] SIGHTING_DATE_FILTERS = new IDateFilter[IDateFilter.DATE_FILTERS.length + 1];
	static{
		SIGHTING_DATE_FILTERS[0] = LastSightingDateFilter.INSTANCE;
		for (int i = 0; i < IDateFilter.DATE_FILTERS.length; i++){
			SIGHTING_DATE_FILTERS[i+1] = IDateFilter.DATE_FILTERS[i];
		}
	}
	
	/**
	 * Fixed columns in the entity type
	 * @author Emily
	 *
	 */
	public static enum FixedColumns{
		ENTITY_ID(Entity.ID_FIELD_NAME, ColumnType.STRING, "entity:id", "entity_id"), //$NON-NLS-1$ //$NON-NLS-2$
		ENTITY_STATUS(Entity.STATUS_FIELD_NAME, ColumnType.STRING, "entity:status", "entity_status"), //$NON-NLS-1$ //$NON-NLS-2$
		CA_ID(Messages.SightingQueryColumn_CaIdColumnName, ColumnType.STRING,"ca:id", "ca_id"), //$NON-NLS-1$ //$NON-NLS-2$
		CA_NAME(Messages.SightingQueryColumn_CaNameColumnName, ColumnType.STRING,"ca:name", "ca_name"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_SOURCE(Messages.SightingQueryColumn_WpSourceColumnName, ColumnType.STRING,"wp:source", "wp_source"),  //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_ID(Messages.SightingQueryColumn_WaypointIdColumnName, ColumnType.INTEGER,"waypoint:id", "wp_id"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_DATE(Messages.SightingQueryColumn_WaypointDateColumnName, ColumnType.DATE,"waypoint:date", "wp_time"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_TIME(Messages.SightingQueryColumn_WaypointTimeColumnName, ColumnType.TIME,"waypoint:time", "wp_time"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_X(Messages.SightingQueryColumn_xColumnName, ColumnType.NUMBER,"waypoint:x", "wp_x"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_Y(Messages.SightingQueryColumn_yColumnName, ColumnType.NUMBER, "waypoint:y", "wp_y"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_DIRECTION(Messages.SightingQueryColumn_DirectionColumnName, ColumnType.NUMBER,"waypoint:direction", "wp_direction"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_DISTANCE(Messages.SightingQueryColumn_DistanceColumnName, ColumnType.NUMBER,"waypoint:distance", "wp_distance"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_COMMENT(Messages.SightingQueryColumn_CommentColumnName, ColumnType.STRING,"waypoint:comment", "wp_comment"), //$NON-NLS-1$ //$NON-NLS-2$
		WAYPOINT_OBSERVER(Messages.SightingQueryColumn_ObserverColumnName, ColumnType.STRING,"ob:observer", "ob_observer");  //$NON-NLS-1$ //$NON-NLS-2$
		
		private String guiName;
		private ColumnType type;
		private String key;
		public String dbColName;
		
		private FixedColumns(String name, ColumnType type, String key, String colName){
			this.guiName = name;
			this.type = type;
			this.key = key;
			this.dbColName = colName;
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
	
	
	private String dbCol;
	
	/**
	 * Creates a new column
	 * @param name name
	 * @param key key
	 * @param type type
	 */
	public SightingQueryColumn(String name, String key, ColumnType type, String dbCol) {
		super(name, key, type);
		this.dbCol = dbCol;
	}
	
	public String getDbColumn(){
		return this.dbCol;
	}
	

	@Override
	public Object getValue(IResultItem item) {

		SightingResultItem ri = (SightingResultItem) item;
		if (getKey().equals(FixedColumns.ENTITY_ID.getKey())){
			return ri.getEntityId();
		}else if (getKey().equals(FixedColumns.ENTITY_STATUS.getKey())){
			return ri.getEntityStatus().getGuiName();
		}else if (getKey().equals(FixedColumns.CA_ID.getKey())){
			return ri.getConservationAreaId();
		}else if (getKey().equals(FixedColumns.CA_NAME.getKey())){
			return ri.getConservationAreaName();
		}else if (getKey().equals(FixedColumns.WAYPOINT_SOURCE.getKey())){
			return ri.getSourceId();
		}else if (getKey().equals(FixedColumns.WAYPOINT_ID.getKey())){
			return ri.getWaypointId();
		}else if (getKey().equals(FixedColumns.WAYPOINT_DATE.getKey())){
			return ri.getWaypointDateTime();
		}else if (getKey().equals(FixedColumns.WAYPOINT_TIME.getKey())){
			return ri.getWaypointDateTime();
		}else if (getKey().equals(FixedColumns.WAYPOINT_X.getKey())){
			return ri.getWaypointX();
		}else if (getKey().equals(FixedColumns.WAYPOINT_Y.getKey())){
			return ri.getWaypointY();
		}else if (getKey().equals(FixedColumns.WAYPOINT_DIRECTION.getKey())){
			return ri.getWaypointDirection();
		}else if (getKey().equals(FixedColumns.WAYPOINT_DISTANCE.getKey())){
			return ri.getWaypointDistance();
		}else if (getKey().equals(FixedColumns.WAYPOINT_COMMENT.getKey())){
			return ri.getWaypointComment();
		}else if (getKey().equals(FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return ri.getWaypointObserver();
		}else if (getKey().startsWith("cat:")){ //$NON-NLS-1$
			Integer index = Integer.parseInt(getKey().substring(getKey().lastIndexOf(':')+1));
			if (index < ri.getCategories().length){
				return ri.getCategories()[index];
			}
			return ""; //$NON-NLS-1$
		}
		
		Object x = ri.getEntityAttribute(getKey());
		if (x != null){
			return x;
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		throw new IllegalStateException("Cannot clone sighting columns."); //$NON-NLS-1$
	}
	
}
