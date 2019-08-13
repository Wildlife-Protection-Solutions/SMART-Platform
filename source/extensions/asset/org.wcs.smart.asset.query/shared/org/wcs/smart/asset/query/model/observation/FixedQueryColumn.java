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
package org.wcs.smart.asset.query.model.observation;

import java.sql.Date;
import java.sql.Time;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Class represents one of the fixed table columns that
 * do not change from conservation area to conservation area.
 * 
 * <p>This includes items such as the asset, station etc
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
		CA_ID(ColumnType.STRING,"ca:id"), //$NON-NLS-1$
		CA_NAME(ColumnType.STRING,"ca:name"), //$NON-NLS-1$
		
		STATION(ColumnType.STRING, "asset:station"), //$NON-NLS-1$
		LOCATION(ColumnType.STRING, "asset:location"), //$NON-NLS-1$
		ASSET(ColumnType.STRING, "asset:asset"), //$NON-NLS-1$

		WAYPOINT_ID( ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE(ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME(ColumnType.TIME,"waypoint:time"), //$NON-NLS-1$
		INCIDENT_LENGTH(ColumnType.INTEGER,"waypoint:length"), //$NON-NLS-1$
		WAYPOINT_X(ColumnType.NUMBER,"waypoint:x"), //$NON-NLS-1$
		WAYPOINT_Y(ColumnType.NUMBER, "waypoint:y"), //$NON-NLS-1$
//		WAYPOINT_DIRECTION(ColumnType.NUMBER,"waypoint:direction"), //$NON-NLS-1$
//		WAYPOINT_DISTANCE( ColumnType.NUMBER,"waypoint:distance"), //$NON-NLS-1$

		WAYPOINT_COMMENT(ColumnType.STRING,"waypoint:comment"), //$NON-NLS-1$
		
		WAYPOINT_LASTMODIFIED( ColumnType.DATETIME,"waypoint:modified"),   //$NON-NLS-1$
		WAYPOINT_LASTMODIFIEDBY( ColumnType.STRING,"waypoint:modifiedby");   //$NON-NLS-1$
		
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
		super(SmartContext.INSTANCE.getClass(IQueryAssetLabelProvider.class).getLabel(column,l), column.key, column.type);
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
	 * @see org.wcs.smart.asset.query.model.observation.QueryColumn#getValue(org.wcs.smart.asset.query.model.AssetQueryResultItem)
	 */
	public Object getValue(IResultItem queryResultItem) {
		if (queryResultItem instanceof AssetQueryResultItem) {
			AssetQueryResultItem item = (AssetQueryResultItem) queryResultItem;

			switch (column) {
			case ASSET:
				return item.getAssets();
			case STATION:
				return item.getStation();
			case LOCATION:
				return item.getLocations();
			case WAYPOINT_ID:
				return item.getWaypointId();
			case WAYPOINT_COMMENT:
				return item.getWaypointComment();
			case WAYPOINT_DATE:
				return new Date(item.getWaypointDate().getTime());
//			case WAYPOINT_DIRECTION:
//				return item.getWaypointDirection();
//			case WAYPOINT_DISTANCE:
//				return item.getWaypointDistance();
			case WAYPOINT_TIME:
				return new Time(item.getWaypointDate().getTime());
			case WAYPOINT_X:
				return item.getWaypointX(getProjection());
			case WAYPOINT_Y:
				return item.getWaypointY(getProjection());
			case CA_ID:
				return item.getConservationAreaId();
			case CA_NAME:
				return item.getConservationAreaName();
			case INCIDENT_LENGTH:
				return item.getIncidentLength();
			case WAYPOINT_LASTMODIFIED:
				return item.getLastModifiedDate();
			case WAYPOINT_LASTMODIFIEDBY:
				return item.getLastModifiedBy();
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.asset.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		FixedQueryColumn newColumn = new FixedQueryColumn(this.column, l);
		newColumn.setEdit(canEdit());
		return newColumn;
	}
	
	
	public static String getDbColumnName(String key) {
		if (key.equals("waypoint:length")) return "incident_length"; //$NON-NLS-1$ //$NON-NLS-2$
		
		if (key.equals(FixedColumns.WAYPOINT_LASTMODIFIED.getKey() )){
			key = "waypoint:lastmodified"; //$NON-NLS-1$
		}else if (key.equals(FixedColumns.WAYPOINT_LASTMODIFIEDBY.getKey() )){
			key = "waypoint:lastmodifiedbyname"; //$NON-NLS-1$
		}
		
		key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
			key = key.replace(data[0], data[1]);
		}
		return key;
	}
	
	private static String[][] FIXED_COLUMN_KEY_TO_ROW  = {
			{"waypoint_time", "waypoint_date"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"waypoint", "wp"} //$NON-NLS-1$ //$NON-NLS-2$
		};
}
