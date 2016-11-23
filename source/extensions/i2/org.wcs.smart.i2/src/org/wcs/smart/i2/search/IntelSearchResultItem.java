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
package org.wcs.smart.i2.search;

import java.text.DecimalFormat;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.wcs.smart.i2.model.IntelEntity;

/**
 * An individual search result item. Includes the entity,
 * the attribute string matched and the match rating.
 * @author Emily
 *
 */
public class IntelSearchResultItem implements IAdaptable {
	
	private static final DecimalFormat DFORMAT = new DecimalFormat("0.000");
	
	private String matchedString;
	private UUID entityUuid;
	private IntelEntity entity;
	private Double matchRate;

	/**
	 * Creates a new result item
	 * @param entity
	 * @param matchedString
	 * @param rate
	 */
	public IntelSearchResultItem(UUID entity, String matchedString, double rate) {
		setResult(entity, matchedString, rate);
	}
	
	/**
	 * Updates the result item values 
	 * @param entity
	 * @param matchedString
	 * @param rate
	 */
	public void setResult(UUID entity, String matchedString, double rate){
		this.matchedString = matchedString;
		this.entityUuid = entity;
		this.matchRate = rate;
		this.entity = null;
	}
	
	/**
	 * 
	 * @return the uuid of the entity matched; this will always return a value
	 */
	public UUID getEntityUuid(){
		return this.entityUuid;
	}
	
	/**
	 * the match rating
	 * @return
	 */
	public Double getRating(){
		return this.matchRate;
	}
	
	/**
	 * The rating formatted for display.
	 * 
	 * @return
	 */
	public String getFormattedRating(){
		return DFORMAT.format(getRating());
	}
	/**
	 * The string matched
	 * @return
	 */
	public String getMatchString(){
		return this.matchedString;
	}
	
	/**
	 * 
	 * @return the entity object matched; 
	 * this will only return a value if setEntity(IntelEntity) has been called
	 */
	public IntelEntity getEntity(){
		return this.entity;
	}
	
	/**
	 * Sets the intelligence entity associated with the search result item
	 *  
	 * @param entity
	 */
	public void setEntity(IntelEntity entity){
		this.entity = entity;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IntelEntity.class && entity != null) return entity;
		return null;
	}
}