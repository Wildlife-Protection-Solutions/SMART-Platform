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

/**
 * An individual search result item. Includes the entity,
 * the attribute string matched and the match rating.
 * @author Emily
 *
 */
public class IntelSearchResultItem {
	
	private static final DecimalFormat DFORMAT = new DecimalFormat("0.000"); //$NON-NLS-1$
	
	private UUID entityUuid;
	private Double matchRate;
	private String matchString;

	/**
	 * Creates a new result item with entity
	 * 
	 * @param entity
	 * @param rate
	 */
	public IntelSearchResultItem(UUID entity, double rate) {
		this(entity, rate, null);
	}
	
	/**
	 * Creates a new result item with entity
	 * 
	 * @param entity
	 * @param rate
	 * @param match the string matched, may be null if not applicable
	 */
	public IntelSearchResultItem(UUID entity, double rate, String match) {
		this.entityUuid = entity;
		this.matchRate = rate;
		this.matchString = match;
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
	 * 
	 * @return the string that was matched if applicable.  will return null
	 * if not applicable
	 */
	public String getMatchedString() {
		return this.matchString;
	}

}