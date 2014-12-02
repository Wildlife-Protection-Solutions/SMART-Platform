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

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.StyledQuery;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

/**
 * 
 * A simple query contains exactly one
 * queryFilter, one Conservation Area files,
 * one DateFiler and one set of drop items.
 * 
 *  <p>All query results  
 * 
 * 
 * @author jeff
 * @author egouge 
 * 
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class SimpleQuery extends StyledQuery {

	/* db fields */
	protected String strQueryFilter;
	protected String visibleColumns = null;
	
	/* transient fieldss */
	protected QueryFilter queryFilter;	//cached copy of the parsed query
	private DateFilter dateFilter;
	
	
	/**
	 * Creates a new waypoint query with the default
	 * conservation area filter and no date filter
	 */
	public SimpleQuery(){
		super();
		setName(Messages.SimpleQuery_DefaultQueryName);
		dateFilter = null;
		strQueryFilter = ""; //$NON-NLS-1$
	}
	
	/**
	 * Creates a new waypoint query the given filter and
	 * the default conservation area filter and no date filter.
	 * 
	 * @param pFilter query filter
	 */
	public SimpleQuery(QueryFilter pFilter){
		this();
		
		this.queryFilter = pFilter;
		this.strQueryFilter = pFilter.asString();
	}
	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setQueryFilter(String filter){
		this.strQueryFilter = filter;
		this.queryFilter = null;
	}
	
	/**
	 * @return the query filter as string
	 */
	@Column(name = "query_filter")
	public String getQueryFilter(){
		return this.strQueryFilter;
	}
	
	/**
	 * May call the database, so if performance important
	 * need to call inside job
	 * @return list of output columns available to the query.
	 */
	@Transient
	public abstract List<QueryColumn> getQueryColumns();

	
	/**
	 * Updates the visible columns based 
	 * on the isVisible field of the associated
	 * WaypointQueryColumn columns.
	 */
	@Transient
	public abstract void updateVisibleColumns();
	
	
	/**
	 * 
	 * @return the query filter in the filter format.  Will
	 * attempt to parse the query if it has not been parsed
	 */
	@Transient
	public QueryFilter getFilter(){
		if (queryFilter == null){
			try{
				queryFilter = parseQueryFilter();
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.SimpleQuery_ParseError, ex);
			}
		}
		return queryFilter;	
	}
	
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
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected abstract  QueryFilter parseQueryFilter() throws Exception;

	
	/**
	 * Returns a list of columns that are visible in the output table.
	 * @return a list of visible column
	 */
	@Column(name = "column_filter")
	public String getVisibleColumns(){
		return this.visibleColumns;
	}
	
	/**
	 * Sets the columns that are visible in the output table.
	 * 
	 * @param columns
	 */
	public void setVisibleColumns(String columns){
		this.visibleColumns = columns;
	}
	
	/**
	 * <p>Compares the query filter and output columns.</p>
	 * @see org.wcs.smart.query.model.Query#isDefinitionEqual(org.wcs.smart.query.model.Query)
	 */
	public boolean isDefinitionEqual(Query other){
		if (other == null || !(other instanceof SimpleQuery)){
			return false;
		}
		
		SimpleQuery query = (SimpleQuery)other;
		return ((query.getVisibleColumns() == null && this.getVisibleColumns() == null) || 
				(query.getVisibleColumns() != null && query.getVisibleColumns().equals(this.getVisibleColumns()))) &&
				query.getQueryFilter().equalsIgnoreCase(this.getQueryFilter());
	}
	
	/**
	 * @see org.wcs.smart.query.model.Query#copyQuery(org.wcs.smart.query.model.Query)
	 */
	public void copyQuery(Query copy){
		assert copy instanceof SimpleQuery;
		
		SimpleQuery q = (SimpleQuery)copy;
		setQueryFilter(q.getQueryFilter());
		setVisibleColumns(q.getVisibleColumns());
		setConservationAreaFilter(q.getConservationAreaFilter());
	}
	
}
