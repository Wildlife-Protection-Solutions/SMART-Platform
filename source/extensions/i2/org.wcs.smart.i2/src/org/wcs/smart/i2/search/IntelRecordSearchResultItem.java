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

import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

/**
 * Record search result item
 * 
 * @author Emily
 *
 */
public class IntelRecordSearchResultItem {
	
	private String title;
	private UUID record;
	private IntelProfile profile;
	private IntelRecordSource recordSource;
	private IntelRecord.Status status;

	/**
	 * Creates a new result item with entity
	 * @param entity
	 * @param matchedString
	 * @param rate
	 */
	public IntelRecordSearchResultItem(UUID record, IntelProfile profile, IntelRecordSource recordSource, 
			String title, IntelRecord.Status status) {
		this.title = title;
		this.record = record;
		this.profile = profile;
		this.recordSource = recordSource;
		this.status = status;
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
	 * @return the uuid of the matched record
	 */
	public IntelProfile getProfile(){
		return this.profile;
	}
	/**
	 * 
	 * @return the uuid of the source of the matched record 
	 */
	public IntelRecordSource getRecordSource(){
		return this.recordSource;
	}
	
	/**
	 * The record title
	 * @return
	 */
	public String getTitle(){
		return this.title;
	}
	
	public RecordEditorInput asRecordInput() {
		return new RecordEditorInput(getTitle(), 
				getRecordUuid(), null, 
				getRecordSource() == null ? null : getRecordSource().getUuid(), 
				getProfile().getUuid(), 
				getStatus());
	}
}