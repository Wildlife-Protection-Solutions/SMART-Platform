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
package org.wcs.smart.event.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.event.filter.ParsedFilter;

/**
 * Event filters, specified by the user
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.e_event_filter")
public class EFilter extends UuidItem{
	
	private static final long serialVersionUID = 1L;
	
	public static final int MAX_ID_LENGTH = 128;
	
	private String id;
	private String filterString;
	private ConservationArea ca;
	
	private ParsedFilter cachedFilter;
	
	/**
	 * 
	 * @return the conservation area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * the filter identifier
	 * @return
	 */
	@Column(name="id")
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Filter string
	 * @return
	 */
	@Column(name="filter_string")
	public String getFilterString() {
		return this.filterString;
	}
	
	public void setFilterString(String filterString) {
		this.filterString = filterString;
		this.cachedFilter = null;
	}

	@Transient
	public synchronized ParsedFilter getParsedFilter() throws Exception{
		if (cachedFilter != null) return cachedFilter; 
		cachedFilter = ParsedFilter.parse(getFilterString());
		return cachedFilter;
	}
	
}
