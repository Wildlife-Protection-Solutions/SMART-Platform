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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class SummaryQuery extends Query {

	/* db fields */
	protected String strQuery;	
	
	/* transient fields */
	protected SumQueryDefinition query;	//cached copy of the parsed query
	protected DateFilter dateFilter;
	
	/**
	 * Creates a new summary query with the default
	 * conservation area filter and no date filter
	 */
	protected SummaryQuery(){
		super();
		
	}
	
	@Transient
	public SumQueryDefinition getQueryDefinition() throws Exception{
		if (query == null){
			query = parseQuery();
		}
		return query;	
	}
	
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected abstract SumQueryDefinition parseQuery() throws Exception;
	
	/**
	 * @return the date filter; or null if date filter not set
	 */
	@Transient
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}
	/**
	 * Sets the date filter 
	 * @param dateFilter
	 */
	@Override
	public void setDateFilter(DateFilter dateFilter){
		this.dateFilter = dateFilter;
	}
	
	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setQuery(String query){
		this.strQuery = query;
		this.query = null;
	}
	
	/**
	 * Sets the query string and associated 
	 * parsed query definition
	 * @param query
	 * @param definition
	 */
	public void setQuery(String query, SumQueryDefinition definition){
		setQuery(query);
		this.query = definition;
	}
	
	/**
	 * @return the query filter as string
	 */
	@Column(name = "query_def")
	public String getQuery(){
		return this.strQuery;
	}
	
	/**
	 * Validates the query parts.  Assumes the query
	 * definition is formed from a valid string.
	 * <p>
	 * This validates the items in the query.
	 * </p>
	 * 
	 * @param def the summary query definition
	 * @return error string or null if query validates okay
	 */
	public static String validateQueryParts(SumQueryDefinition def){
		List<IGroupBy> groupBys = new ArrayList<IGroupBy>();
		if (def.getRowGroupByPart() != null){
			groupBys.addAll(def.getRowGroupByPart().getGroupBys());
		}
		if (def.getColumnGroupByPart().getGroupBys() != null){
			groupBys.addAll(def.getColumnGroupByPart().getGroupBys());
		}
		return null;
	}
	
	/**
	 * @see org.wcs.smart.query.model.Query#isDefinitionEqual(org.wcs.smart.query.model.Query)
	 */
	public boolean isDefinitionEqual(Query other){
		if (other == null || !(other instanceof SummaryQuery)){
			return false;
		}
		SummaryQuery query = (SummaryQuery)other;
		return (query.getQuery().equals(this.getQuery()));
	}
	
	/**
	 * @see org.wcs.smart.query.model.Query#copyQuery(org.wcs.smart.query.model.Query)
	 */
	public void copyQuery(Query copy){
		assert copy instanceof SummaryQuery;
		SummaryQuery q = (SummaryQuery)copy;
		setQuery(q.getQuery());
		setConservationAreaFilter(q.getConservationAreaFilter());
	}
}
