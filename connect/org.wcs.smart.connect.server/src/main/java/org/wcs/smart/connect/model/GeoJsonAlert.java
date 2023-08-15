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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.connect.model.Alert.AlertStatusEnum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * An Alert entity
 *
 * @Author Jeff
 */


@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonAlert{
	
	private static final String DATE_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; //$NON-NLS-1$
	
	private String type;
	private List<GeoJsonFeature> features ;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public List<GeoJsonFeature> getFeatures() {
		return features;
	} 
	
	
	public void setFeature(List<GeoJsonFeature> features) {
		this.features = features;
	}
	
	//direct Getters for properties
	public String getDeviceId() {
		return features.get(0).getProperties().getDateTime();
	}
	
	public String getId() {
		return features.get(0).getProperties().getId();
	}
	
	/**
	 * Alerts are stored in the time provided (local time); the time
	 * zone information is dropped
	 * 
	 * @return
	 */
	public LocalDateTime getDateTime() {
		LocalDateTime date = null;
		String dateString = features.get(0).getProperties().getDateTime();
		if (dateString == null || dateString.isBlank()) return null;

		try {
			DateTimeFormatter f = DateTimeFormatter.ofPattern(DATE_FORMAT_STR);
			date = LocalDateTime.parse(dateString,f);
		} catch (DateTimeParseException e) {
			e.printStackTrace();
		}
		return date;
		
	}
	
	public String getAltitude() {
		return features.get(0).getProperties().getAltitude();
	}
	
	public String getAccuracy() {
		return features.get(0).getProperties().getAccuracy();
	}
	
	public UUID getCaUuid() {
		return features.get(0).getProperties().getCaUuid();
	}
	
	public Integer getLevel() {
		return features.get(0).getProperties().getLevel();
	}
	
	public String getDescription() {
		return features.get(0).getProperties().getDescription();
	}
	
	public UUID getTypeUuid() {
		return features.get(0).getProperties().getTypeUuid();
	}
	
	public Object getSighting() {
		return features.get(0).getProperties().getSighting();
	}
	
	public String getFieldIdentifier() {
		return features.get(0).getProperties().getFieldIdentifier();
	}
	
	//direct getters for coordinates
	public Double getLatitude() {
		return features.get(0).getGeometry().getCoordinates().get(1);
	}
	
	public Double getLongitude() {
		return features.get(0).getGeometry().getCoordinates().get(0);
	}

	
	/**
	 * Converts the geojson alert to a connect alert.
	 * Set the lat,long,typeuuid,level,datetime,description,status,track
	 * fields.  Does not set the creator
	 * 
	 * @return
	 */
	public Alert toAlert() {
		Alert a = new Alert();

		a.setCreatorUuid(null);
    	a.setX(getLongitude());
    	a.setY(getLatitude());
    	a.setTypeUuid(getTypeUuid());
    	a.setLevel(getLevel());
    	a.setFieldIdentifier(getFieldIdentifier());
    	//default to now if no date given 
		if(getDateTime() == null){
			a.setDate(LocalDateTime.now());
		}else{
			a.setDate(getDateTime());
		}
		
		//default to Active for new alerts 
		a.setStatus(AlertStatusEnum.ACTIVE);
		a.setDescription(getDescription());

		String track = "[ [" + getLongitude() + " , " + getLatitude() + "]" + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		a.setTrack(track);
		
    	return a; 
	}
}
