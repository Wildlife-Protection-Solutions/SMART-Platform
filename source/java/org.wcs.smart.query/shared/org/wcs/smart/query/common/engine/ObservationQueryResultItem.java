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
package org.wcs.smart.query.common.engine;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;

/**
 * A class to hold the results of a observation 
 * query.  Each class contains the results for
 * a single observation.  The observation contains
 * a single category and all attributes.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationQueryResultItem extends WaypointQueryResultItem implements IObservationQueryResultItem{
	
	private String[] observationCategory;
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	private UUID groupUuid;
	private UUID observationUuid;

	
	/**
	 * @param observationUuid the observation uuid
	 */
	public void setObservationUuid(UUID observationUuid){
		this.observationUuid = observationUuid;
	}
	
	/**
	 * @return the observation uuid
	 */
	public UUID getObservationUuid(){
		return this.observationUuid;
	}

	/**
	 * @param observationUuid the observation group uuid
	 */
	public void setObservationGroupUuid(UUID groupUuid){
		this.groupUuid = groupUuid;
	}
	
	/**
	 * @return the observation group uuid
	 */
	public UUID getObservationGroupUuid(){
		return this.groupUuid;
	}
	
	/**
	 * Each item is associated with a single category.  This
	 * returns an array of the names of the category and
	 * all the parent categories:
	 *   - {parent1, parent2, category}
	 * 
	 * @return an array of the category names of the category & parent categories
	 */
	public String[] getCategories(){
		return this.observationCategory;
	}
	
	/**
	 * @param cat sets the category
	 */
	public void setCategory(String[] categoryLabels){
		this.observationCategory = categoryLabels;
	}
	
	public void setAttributes(HashMap<String, Object> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * Finds the attribute value of the associated attribute
	 * key.
	 * 
	 * @param attributeKey the attribute key
	 * @return the value associated with the attribute given key
	 */
	public Object getAttributeValue(String attributeKey){
		return attributes.get(attributeKey);
	}
	
	/**
	 * Adds an attribute to the observation results 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addAttribute(String key, Object value){
		attributes.put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(Waypoint.class) && getWaypointUuid() != null){
			Waypoint wp = new Waypoint();
			wp.setUuid(getWaypointUuid());
			return (T)wp;
		}
		if (adapter.equals(WaypointObservation.class) && getObservationUuid() != null){
			WaypointObservation wo = new WaypointObservation();
			wo.setUuid(getObservationUuid());
			return (T)wo;
		}
		return null;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (getClass() != other.getClass()) return false;
		return Objects.equals(getWaypointUuid(), ((ObservationQueryResultItem)other).getWaypointUuid()) &&
				Objects.equals(getObservationUuid(), ((ObservationQueryResultItem)other).getObservationUuid()); 
	}
	
	@Override
	public int hashCode() {
		if (getObservationUuid() != null) return getObservationUuid().hashCode();
		return super.hashCode();
	}
}
