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

import java.util.UUID;

import org.wcs.smart.i2.model.IntelRecord;

/**
 * Record search result item
 * 
 * @author Emily
 *
 */
public class IntelRecordSearchResultItem {
	
	private String title;
	private UUID record;
	private UUID recordSource;
	private IntelRecord.Status status;
	
	private String localMatch;
	int[][] matchRanges;
	/**
	 * Creates a new result item with entity
	 * @param entity
	 * @param matchedString
	 * @param rate
	 */
	public IntelRecordSearchResultItem(UUID record, UUID recordSource, String title, IntelRecord.Status status, String localMatch, int[][] matchRanges) {
		this.title = title;
		this.record = record;
		this.recordSource = recordSource;
		this.status = status;
		this.localMatch = localMatch;
		this.matchRanges = matchRanges;
	}
	
	public int[][] getMatchRanges(){
		return this.matchRanges;
	}
	public String getLocalMatch(){
		return this.localMatch;
	}
	
	/**
	 * 
	 * @return status associated with record
	 */
	public IntelRecord.Status getStatus(){
		return this.status;
	}
	
	/**
	 * 
	 * @return the uuid of the matched record
	 */
	public UUID getRecordUuid(){
		return this.record;
	}
	
	/**
	 * 
	 * @return the uuid of the source of the matched record 
	 */
	public UUID getRecordSourceUuid(){
		return this.recordSource;
	}
	
	/**
	 * The record title
	 * @return
	 */
	public String getTitle(){
		return this.title;
	}
}