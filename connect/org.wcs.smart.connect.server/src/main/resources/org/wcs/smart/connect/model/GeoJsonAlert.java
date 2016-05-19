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
import java.util.TimeZone;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ibm.icu.text.DateFormat;

/*
 * An Alert entity
 *
 * @Author Jeff
 */


@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonAlert{
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
			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			//Get the date in UTC, we need to remove the "Z" at the end of cybertracker dates
			date = f.parse(dateString.replaceAll("Z$", ""));			
			
			
//			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//			dateFormatter.setTimeZone(TimeZone.getTimeZone("Etc/Universal")); //keep in UTC time, default behavior is to convert to local/server time, we don't want that.		
//			date = (dateFormatter.parse(dateString.replaceAll("Z$", "-0000"))); //$NON-NLS-1$ //get rid of the Z on the end of the date from CT, it is in UTC time.
			//date = dateFormatter.parse(dateString); //$NON-NLS-1$
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
