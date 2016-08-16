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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

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
	private ArrayList<GeoJsonFeature> features ;
	
	public String getType() {
		return type;
	}
	public ArrayList<GeoJsonFeature> getFeatures() {
		return features;
	} 
	
	//direct Getters for properties
	public String getDeviceId() {
		return features.get(0).getProperties().getDateTime();
	}
	
	public String getId() {
		return features.get(0).getProperties().getId();
	}
	
	public Date getDateTime() {
		Date date = null;
		String dateString = features.get(0).getProperties().getDateTime();
		if (dateString == null || dateString == "") return null; //$NON-NLS-1$

		try {
			SimpleDateFormat f = new SimpleDateFormat(DATE_FORMAT_STR);
			date = f.parse(dateString);
		} catch (ParseException e) {
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
	
	//direct getters for coordinates
	public Double getLatitude() {
		return features.get(0).getGeometry().getCoordinates().get(1);
	}
	
	public Double getLongitude() {
		return features.get(0).getGeometry().getCoordinates().get(0);
	}

}
