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
package org.wcs.smart.observation.query.model.columns;

import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
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
		WAYPOINT_SOURCE(Messages.FixedQueryColumn_WaypointSourceColumnName, ColumnType.STRING,"wp:source"),  //$NON-NLS-1$
		WAYPOINT_ID(Messages.FixedQueryColumn_WaypointIdColumnName, ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE(Messages.FixedQueryColumn_WaypointDateColumnName, ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME(Messages.FixedQueryColumn_WaypointTimeColumnName, ColumnType.TIME,"waypoint:time"), //$NON-NLS-1$
		WAYPOINT_X(Messages.FixedQueryColumn_xColumnName, ColumnType.NUMBER,"waypoint:x"), //$NON-NLS-1$
		WAYPOINT_Y(Messages.FixedQueryColumn_yColumnName, ColumnType.NUMBER, "waypoint:y"), //$NON-NLS-1$
		WAYPOINT_DIRECTION(Messages.FixedQueryColumn_DirectionColumnName, ColumnType.NUMBER,"waypoint:direction"), //$NON-NLS-1$
		WAYPOINT_DISTANCE(Messages.FixedQueryColumn_DistanceColumnName, ColumnType.NUMBER,"waypoint:distance"), //$NON-NLS-1$
		WAYPOINT_COMMENT(Messages.FixedQueryColumn_CommentColumnName, ColumnType.STRING,"waypoint:comment"), //$NON-NLS-1$
		WAYPOINT_OBSERVER(Messages.FixedQueryColumn_ObserverColumnName, ColumnType.STRING,"ob:observer");  //$NON-NLS-1$
		
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
		if (queryResultItem instanceof ObservationQueryResultItem) {
			ObservationQueryResultItem item = (ObservationQueryResultItem) queryResultItem;

			switch (column) {
			
			case WAYPOINT_ID:
				return item.getWaypointId();
			case WAYPOINT_SOURCE:
				return item.getSourceId();
			case WAYPOINT_COMMENT:
				return item.getWaypointComment();
			case WAYPOINT_OBSERVER:
				return item.getWaypointObserver();
			case WAYPOINT_DATE:
				return item.getWpDateTime();
			case WAYPOINT_DIRECTION:
				return item.getWaypointDirection();
			case WAYPOINT_DISTANCE:
				return item.getWaypointDistance();
			case WAYPOINT_TIME:
				return item.getWpDateTime();
			case WAYPOINT_X:
				return item.getWaypointX();
			case WAYPOINT_Y:
				return item.getWaypointY();
			case CA_ID:
				return item.getConservationAreaId();
			case CA_NAME:
				return item.getConservationAreaName();
			}
		}
		return null;
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
