/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonProperties{
	
	private String deviceId; 
	private String id;
	private String dateTime;
	private String latitude;
	private String longitude;
	private String altitude;
	private String accuracy;
	private UUID caUuid;
	private Integer level;
	private String description;
	private UUID typeUuid;
	private GeoJsonSighting sighting;
	
	public String getDeviceId() {
		return deviceId;
	}
	public String getId() {
		return id;
	}
	public String getDateTime() {
		return dateTime;
	}
	public String getLatitude() {
		return latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public String getAltitude() {
		return altitude;
	}
	public String getAccuracy() {
		return accuracy;
	}
	public UUID getCaUuid() {
		return caUuid;
	}
	public Integer getLevel() {
		return level;
	}
	public String getDescription() {
		return description;
	}
	public UUID getTypeUuid() {
		return typeUuid;
	}
	public Object getSighting() {
		return sighting;
	}

}