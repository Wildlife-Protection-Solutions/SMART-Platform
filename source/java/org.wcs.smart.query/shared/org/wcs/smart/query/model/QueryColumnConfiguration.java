/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.query.model;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @since 8.1.0
 */
@Entity
@Table(name="query_column_config", schema="smart")
public class QueryColumnConfiguration extends NamedItem{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ConservationArea ca;
	
	private String columnConfig;
	
	private String queryTypeKey;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	@Column(name = "column_config")
	public String getColumnConfiguration() {
		return this.columnConfig;
	}
	
	public void setColumnConfiguration(String columnConfig) {
		this.columnConfig = columnConfig;
	}
	
	@Column(name = "query_type_key")
	public String getQueryTypeKey() {
		return this.queryTypeKey;
	}
	
	public void setQueryTypeKey(String queryTypeKey) {
		this.queryTypeKey = queryTypeKey;
	}
}
