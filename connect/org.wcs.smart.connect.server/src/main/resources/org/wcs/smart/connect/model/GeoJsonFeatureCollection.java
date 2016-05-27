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

import org.wcs.smart.util.UuidUtils;

/*
 * A geoJSON feature collection. I assume there will be some differences from GeoJsonAlert as we figure out with Cybertracker what they are going to send in this object.
 *
 * @Author Jeff
 */



public class GeoJsonFeatureCollection extends GeoJsonAlert{

	//TODO  update this when we have a CA ID in teh JSON to get properly
	public UUID getConservationAreaUUID() {
		return UuidUtils.stringToUuid("1f925d6f-808f-4cbc-8103-c6f357e1ca3e");
	}

	public String getname() {
		String str = getConservationAreaUUID().toString() + "::" + getPatrolID().toString() + "::";
		int i = getFirstObservationId();
		String text = String.valueOf(i);
		return str + text;
	}


	private Object getPatrolID() {
		GeoJsonFeature first = this.getFirstFeature();
		return first.getProperties().getSighting().getPatrolId();
	}

	private GeoJsonFeature getFirstFeature() {
		return this.getFeatures().get(0);
	}
	
	private int getFirstObservationId() {
		GeoJsonFeature first = this.getFirstFeature();
		return first.getProperties().getSighting().getObservationId();
	}
}
