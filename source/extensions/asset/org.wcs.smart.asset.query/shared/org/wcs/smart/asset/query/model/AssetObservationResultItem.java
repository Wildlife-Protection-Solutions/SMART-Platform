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
package org.wcs.smart.asset.query.model;

import org.wcs.smart.query.common.engine.test.ObservationQueryResultItem;

/**
 * A class to hold the results of a waypoint 
 * query.  Each class contains the results for
 * a single observation.  The observation contains
 * a single category and all attributes.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AssetObservationResultItem extends ObservationQueryResultItem implements IAssetResultItem {

	private String station;
	private String assets;
	private String locations;

	private int incidentLength;
	
	public void setAssets(String assets) {
		this.assets = assets;
	}
	public String getAssets() {
		return this.assets;
	}
	public void setLocations(String locations) {
		this.locations = locations;
	}
	public String getLocations() {
		return this.locations;
	}
	public void setStation(String station) {
		this.station = station;
	}
	public String getStation() {
		return this.station;
	}
	
	public void setIncidentLength(int incidentLength) {
		this.incidentLength = incidentLength;
	}
	
	/**
	 * The incident length in seconds
	 * @return
	 */
	public int getIncidentLength() {
		return this.incidentLength;
	}


}
