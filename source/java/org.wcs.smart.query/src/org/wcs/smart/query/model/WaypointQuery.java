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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.DerbyQueryEngine2;
import org.wcs.smart.query.parser.internal.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.DateFilter;
import org.wcs.smart.query.parser.internal.Filter;
import org.wcs.smart.query.parser.internal.parser.Parser;

/**
 * A class to represent a waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class WaypointQuery extends Query{
	
	private String strQueryFilter;
	private Filter queryFilter;
	
	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
	
	private List<String> visibleTableColumns = null;
	private List<QueryResultItem> lastResults  = null;
	
	
	/**
	 * Creates a new waypoint query with the default
	 * conservation area filter and no date filter
	 */
	public WaypointQuery(){
		
		caFilter = new ConservationAreaFilter();
		caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		
		dateFilter = null;
		
		super.setName("<No Name Query>");
	}
	
	/**
	 * Creates a new waypoint query the given filter and
	 * the default conservation area filter and no date filter.
	 * 
	 * @param pFilter query filter
	 */
	public WaypointQuery(Filter pFilter){
		this();
		this.queryFilter = pFilter;
		
	}
	
	/**
	 * Sets the date filter 
	 * @param dateFilter
	 */
	public void setDateFilter(DateFilter dateFilter){
		this.dateFilter = dateFilter;
	}
	/**
	 * @return the date filter; or null if date filter not set
	 */
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}
	

	/**
	 * Returns a list of columns that are visible in the output table.
	 * @return a list of visible column
	 */
	public List<String> getVisibleColumns(){
		return this.visibleTableColumns;
	}
	
	/**
	 * Sets the columns that are visible in the output table.
	 * @param columns
	 */
	public void setVisibleColumns(List<String> columns){
		this.visibleTableColumns = columns;
	}
	
	
	/**
	 * @return the conservation area filter
	 */
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
	public String getQueryFilter(){
		if (this.strQueryFilter == null){
			return "";
		}
		return this.strQueryFilter;
	}
	
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	public Filter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return Filter.EMPTY_FILTER;
		}
		InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes());
		Parser parser = new Parser(is);
		Filter myQuery = parser.Expression();
		is.close();
		return myQuery;
	}
	
	/**
	 * 
	 * @return the query filter in the filter format.  Will
	 * attempt to parse the query if it has not been parsed
	 */
	public Filter getFilter(){
		if (queryFilter == null){
			try{
				queryFilter = parseQueryFilter();
			} catch (Exception ex) {
				QueryPlugIn.displayLog("Could not parse query.", ex);
			}
		}
		return queryFilter;	
	}
	
	
	/**
	 * Runs the query and returns the results.
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<QueryResultItem> getQueryResults(IProgressMonitor progressMonitor) throws Exception{
		
		lastResults = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			lastResults = getQueryResults(session, progressMonitor);
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
		return lastResults;
	}
	
	/**
	 * Returns the results from last time the query was run.  Does not re-run the query.
	 * @return the last run results
	 */
	public List<QueryResultItem> getLastResults(){
		return lastResults;
	}
	
	/** public for testing purposes only */
	public List<QueryResultItem> getQueryResults(Session session, IProgressMonitor progressMonitor) throws Exception{
		DerbyQueryEngine2 engine = new DerbyQueryEngine2();
		return engine.executeQuery(this, session, progressMonitor);
	}
	
	
}
