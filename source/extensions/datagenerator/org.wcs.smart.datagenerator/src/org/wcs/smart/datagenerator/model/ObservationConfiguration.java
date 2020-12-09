/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.datagenerator.model;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * And observation configuration for generating patrol data.
 * 
 * @author Emily
 *
 */
public class ObservationConfiguration {
	/**
	 * Identifies if an attribute should be mapped to 
	 * a fixed value, generated randomly or left
	 * empty.
	 * @author Emily
	 *
	 */
	public enum Type{FIXED, RANDOM, EMPTY};
	
	private WaypointObservation observation;
	private Map<Attribute, ObservationConfiguration.Type> attributeMappingTypes;
	
	private int weight;
	
	public ObservationConfiguration(WaypointObservation observation, Map<Attribute, ObservationConfiguration.Type> types) {
		this.observation = observation;
		this.attributeMappingTypes = types;
		this.weight = 10;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public int getWeight() {
		return this.weight;
	}
	
	public WaypointObservation getObservation() {
		return this.observation;
	}
	
	public Set<Attribute> getAttributes(){
		return attributeMappingTypes.keySet();
	}
	
	public ObservationConfiguration.Type getType(Attribute attribute){
		if (attributeMappingTypes.containsKey(attribute)) return attributeMappingTypes.get(attribute);
		return Type.EMPTY;
	}
	
	public String asText() {
		StringBuilder sb = new StringBuilder();
		sb.append(observation.getCategory().getName());
		sb.append(" | "); //$NON-NLS-1$
		for (Attribute a : attributeMappingTypes.keySet()) {
			sb.append(a.getName() );
			sb.append(" ["); //$NON-NLS-1$
			sb.append(attributeMappingTypes.get(a).name());
			sb.append("]"); //$NON-NLS-1$
			if (attributeMappingTypes.get(a) == Type.FIXED) {
				for (WaypointObservationAttribute woa : observation.getAttributes()) {
					if (woa.getAttribute() == a) {
						sb.append(" - "); //$NON-NLS-1$
						sb.append(woa.getAttributeValueAsString(Locale.getDefault()));
					}
				}
			}
			sb.append(" | "); //$NON-NLS-1$
		}
		System.out.println(sb.toString());
		return sb.toString();
	}
}
