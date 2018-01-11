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

import java.util.HashMap;

import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;

/**
 * Summarized data
 * 
 * @author Emily
 *
 */
public class StationData {

	private AssetStation station;
	private AssetStationLocation location;
	
	private HashMap<IOverviewTableColumn, Object> values = new HashMap<>();
	
	public StationData(AssetStation station) {
		this.station = station;
	}
	
	public StationData(AssetStationLocation location) {
		this.location = location;
	}
	
	public Object getData(IOverviewTableColumn column) {
		return column.getValue(this);
	}
	
	public Object getColumnValue(IOverviewTableColumn column) {
		return values.get(column);
	}
	
	public String getIdField() {
		if (station != null) return station.getId();
		if (location != null) return location.getId();
		return "";
	}
	
	public AssetStation getStation() {
		return this.station;
	}
	
	public AssetStationLocation getStationLocation() {
		return this.location;
	}
	public void setData(IOverviewTableColumn column, Object value) {
		values.put(column, value);
	}
	
}
