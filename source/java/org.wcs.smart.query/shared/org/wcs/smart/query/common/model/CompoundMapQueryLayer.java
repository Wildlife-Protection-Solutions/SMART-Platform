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
package org.wcs.smart.query.common.model;

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;

/**
 * Compound query layer 
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.compound_query_layer")
public class CompoundMapQueryLayer extends UuidItem{

	//The owning compound map query
	private CompoundMapQuery mapQuery;
	//query uuid
	private UUID queryUuid;
	//query type
	private String queryType;
	//date filter string representation
	private String strDateFilter;
	//optional style
	private String queryStyle;
	//layer order - required and managed by system
	private int order;

	//parsed date filter
	@Transient
	private DateFilter dateFilter;
	
	
	@ManyToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name="compound_query_uuid", referencedColumnName="uuid", nullable=false)
	public CompoundMapQuery getMapQuery() {
		return mapQuery;
	}
	public void setMapQuery(CompoundMapQuery mapQuery) {
		this.mapQuery = mapQuery;
	}
	
	@Column(name="query_uuid")
	public UUID getQueryUuid() {
		return queryUuid;
	}
	public void setQueryUuid(UUID queryUuid) {
		this.queryUuid = queryUuid;
	}
	
	@Column(name="query_type")
	public String getQueryType() {
		return queryType;
	}
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}
	
	@Column(name="style")
	public String getQueryStyle() {
		return queryStyle;
	}
	public void setQueryStyle(String queryStyle) {
		this.queryStyle = queryStyle;
	}
	
	@Column(name="layer_order")
	public int getOrder(){
		return this.order;
	}
	
	public void setOrder(int order){
		this.order = order;
	}
	
	@Column(name="date_filter")
	public String getDateFilter(){
		return this.strDateFilter;
	}
	
	public void setDateFilter(String dateFilter){
		this.strDateFilter = dateFilter;
		this.dateFilter = null;
	}
	
	@Transient
	public DateFilter getDateFilterAsFilter(){
		if (this.dateFilter == null){
			IQueryType type = QueryTypeManager.INSTANCE.findQueryType(queryType);
			try{
				dateFilter = DateFilter.fromString(strDateFilter, type);
			}catch (Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
				dateFilter = new DateFilter(type.getDateFilterOptions()[0], AllDatesFilter.INSTANCE);
			}
		}
		return this.dateFilter;
	}
	
	public void setDateFilter(DateFilter dateFilter){
		this.dateFilter = dateFilter;
		this.strDateFilter = dateFilter.toString();
	}
}
