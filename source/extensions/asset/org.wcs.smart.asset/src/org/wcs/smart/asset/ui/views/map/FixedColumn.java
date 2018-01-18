/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.map;

import java.util.Locale;

import org.json.simple.JSONObject;
import org.wcs.smart.util.UuidUtils;

/**
 * Fixed asset overview map columns.  These canno be modified by the user.
 * 
 * @author Emily
 *
 */
public class FixedColumn implements IOverviewTableColumn{

	public enum Column{
		ID("Id", IOverviewTableColumn.ColumnType.STRING, true),
		UUID("UUID", IOverviewTableColumn.ColumnType.STRING, false),
		STATUS("Current Status", IOverviewTableColumn.ColumnType.STRING, true),
		STATUS_KEY("Current Status Key", IOverviewTableColumn.ColumnType.STRING, false),
		ACTIVE_DAYS("Total Active Days", IOverviewTableColumn.ColumnType.INTEGER, true),
		ASSET_DAYS("Total Asset Days", IOverviewTableColumn.ColumnType.INTEGER, true),
		INCIDENTS("Total Incidents", IOverviewTableColumn.ColumnType.INTEGER, true);
		
		private String guiName;
		private IOverviewTableColumn.ColumnType type;
		public boolean defaultVisibility;
		
		Column(String name, IOverviewTableColumn.ColumnType type, boolean visibility){
			this.guiName = name;
			this.type = type;
			this.defaultVisibility = visibility;
		}
	}
	
	private Column column;
	
	public FixedColumn(Column column) {
		this.column = column;
	}
	
	public Column getColumn() {
		return column;
	}
	@Override
	public IOverviewTableColumn.ColumnType getType(){
		return column.type;
	}
	
	@Override
	public String getKey() {
		return column.name().toLowerCase();
	}
	
	@Override
	public String getName() {
		return column.guiName;
	}

	@Override
	public Object getValue(StationData data) {
		if (this.column == Column.STATUS) {
			if (data.getStation() != null) return data.getStation().getCachedStatus().getGuiName(Locale.getDefault());
			if (data.getStationLocation() != null) return data.getStationLocation().getCachedStatus().getGuiName(Locale.getDefault());
			return "unknown";
		}else if (this.column == Column.STATUS_KEY) {
			if (data.getStation() != null) return data.getStation().getCachedStatus().name();
			if (data.getStationLocation() != null) return data.getStationLocation().getCachedStatus().name();
			return "UNKNOWN";
		}else if (this.column == Column.ID) {
			return data.getIdField();
		}else if (this.column == Column.UUID) {
			if (data.getStation() != null) return UuidUtils.uuidToString(data.getStation().getUuid());
			if (data.getStationLocation() != null) return UuidUtils.uuidToString(data.getStationLocation().getUuid());
		}
		return data.getColumnValue(this);
	}
	

	/**
	 * Serializes the column to a json object
	 */
	@Override
	public JSONObject serialize() {
		JSONObject json = new JSONObject();
		json.put("type", "fixed");
		json.put("key", getKey());
		return json;
	}
	
	/**
	 * Converts a json object into a FixedColumn object
	 * @param json
	 * @return the FixedColumn or null if could not parse the object
	 */
	public static FixedColumn deserialize(JSONObject json) {
		if (json.containsKey("type") && json.containsKey("key")) {
			if (json.get("type").equals("fixed")) {
				String key = (String)json.get("key");
				for (Column c : Column.values()) {
					if (c.name().equalsIgnoreCase(key)) {
						return new FixedColumn(c);
					}
				}
			}
		}
		return null;
	}

}
