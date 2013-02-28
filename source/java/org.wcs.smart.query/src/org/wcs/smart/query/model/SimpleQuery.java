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
package org.wcs.smart.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.observation.QueryColumn;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

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
public abstract class SimpleQuery extends Query {
	@Transient
	private List<DropItem> items;
	
	public String strQueryFilter;
	public IFilter queryFilter;	//cached copy of the parsed query
	
	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
		
	private Collection<QueryResultItem> lastResults  = null;
	protected String visibleTableColumnKeys = null;
	
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
	public SimpleQuery(IFilter pFilter){
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
	public IFilter getFilter(){
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
	 * @return the conservation area filter
	 */
	@Transient
	public ConservationAreaFilter getConservationAreaFilter(){
		return this.caFilter;
	}
	
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilter(ConservationAreaFilter filter){
		this.caFilter = filter;
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
	 * Runs the query and returns the results.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Transient
	public Collection<QueryResultItem> getQueryResults(IProgressMonitor progressMonitor) throws Exception{
		
		lastResults = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			lastResults = getQueryResults(session, progressMonitor);
		}finally{
			if (session.isOpen()){
				session.getTransaction().commit();
				session.close();
			}
		}
		return lastResults;
	}
	
	/**
	 * Returns the results from last time the query was run.  Does not re-run the query.
	 * @return the last run results
	 */
	@Transient
	public Collection<QueryResultItem> getLastResults(){
		return lastResults;
	}
	
	
	/* public for testing purposes only */
	/**
	 * Runs the query and returns the results
	 * @param session
	 * @param progressMonitor
	 * @return
	 * @throws Exception
	 */
	@Transient
	public abstract Collection<QueryResultItem> getQueryResults(Session session, IProgressMonitor progressMonitor) throws Exception;

	
	/**
	 * Generates all drop items for the query.
	 * @param session hibernate session
	 * @throws Exception
	 */
	@Transient
	@Override
	public void generateDropItems(Session session) throws Exception{
		//parses the query into a collection of drop items
		IFilter query = parseQueryFilter();
		if (items != null){
			for (DropItem it : items){
				it.dispose();
			}
			items.clear();
		}else{
			items = new ArrayList<DropItem>();
		}
		DropItem[] filterItems = query.getDropItems(session);
		for (int i = 0; i < filterItems.length; i ++){
			items.add(filterItems[i]);
		}
	}
	
	
	
	/**
	 * @return the drop items generated for the query
	 */
	@Transient
	public List<DropItem> getDropItems(){
		return items;
	}
	/**
	 * @param items the drop items associated with the query
	 */
	@Transient
	public void setDropItems(List<DropItem> items){
		this.items = items;
	}
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	private  IFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return IFilter.EMPTY_FILTER;
		}
		if(queryFilter != null){
			return queryFilter;
		}
		InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes());
		Parser parser = new Parser(is);
		IFilter myQuery = parser.QueryFilter();
		is.close();
		queryFilter = myQuery;
		return myQuery;
	}
	

	
	/**
	 * Returns a list of columns that are visible in the output table.
	 * @return a list of visible column
	 */
	@Column(name = "column_filter")
	public String getVisibleColumns(){
		return this.visibleTableColumnKeys;
	}
	
	/**
	 * Sets the columns that are visible in the output table.
	 * 
	 * @param columns
	 */
	public void setVisibleColumns(String columns){
		this.visibleTableColumnKeys = columns;
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
		return (query.getVisibleColumns() == null && 
				this.getVisibleColumns() == null) || 
				(query.getVisibleColumns() != null && query.getVisibleColumns().equals(this.getVisibleColumns()) &&
				query.getQueryFilter().equalsIgnoreCase(this.getQueryFilter()));
	}
	
	/**
	 * @see org.wcs.smart.query.model.Query#copyFrom(org.wcs.smart.query.model.Query)
	 */
	public void copyFrom(Query copy){
		assert copy instanceof SimpleQuery;
		
		SimpleQuery q = (SimpleQuery)copy;
		setQueryFilter(q.getQueryFilter());
		setVisibleColumns(q.getVisibleColumns());
		setConservationAreaFilter(q.getConservationAreaFilter());
	}
	
}
