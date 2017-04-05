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

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.observation.query.view.IObservationQueryLabelProvider;
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
		{"waypoint", "wp"} //$NON-NLS-1$ //$NON-NLS-2$
	};
	
	/**
	 * The defined fixed columns.
	 */
	public enum FixedColumns{
		CA_ID(ColumnType.STRING,"ca:id"), //$NON-NLS-1$
		CA_NAME(ColumnType.STRING,"ca:name"), //$NON-NLS-1$
		WAYPOINT_SOURCE(ColumnType.STRING,"wp:source"),  //$NON-NLS-1$
		WAYPOINT_ID(ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE(ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME(ColumnType.TIME,"waypoint:time"), //$NON-NLS-1$
		WAYPOINT_X(ColumnType.NUMBER,"waypoint:x"), //$NON-NLS-1$
		WAYPOINT_Y(ColumnType.NUMBER, "waypoint:y"), //$NON-NLS-1$
		WAYPOINT_DIRECTION(ColumnType.NUMBER,"waypoint:direction"), //$NON-NLS-1$
		WAYPOINT_DISTANCE(ColumnType.NUMBER,"waypoint:distance"), //$NON-NLS-1$
		WAYPOINT_COMMENT(ColumnType.STRING,"waypoint:comment"), //$NON-NLS-1$
		WAYPOINT_OBSERVER(ColumnType.STRING,"ob:observer");  //$NON-NLS-1$
		
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
		super( SmartContext.INSTANCE.getClass(IObservationQueryLabelProvider.class).getLabel(column, l), column.key, column.type);
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
				return item.getWaypointX(getProjection());
			case WAYPOINT_Y:
				return item.getWaypointY(getProjection());
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
		FixedQueryColumn newColumn = new FixedQueryColumn(this.column, this.l);
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
