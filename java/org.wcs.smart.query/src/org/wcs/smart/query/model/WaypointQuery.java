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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.DerbyQueryEngine2;
import org.wcs.smart.query.parser.internal.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.DateFilter;
import org.wcs.smart.query.parser.internal.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A class to represent a waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.waypoint_query")
public class WaypointQuery extends Query{
	
	private String strQueryFilter;
	private IFilter queryFilter;	//cached copy of the parsed query
	
	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
	
	private Set<String> visibleTableColumns = null;
	
	private List<QueryResultItem> lastResults  = null;
	
	
	/**
	 * Creates a new waypoint query with the default
	 * conservation area filter and no date filter
	 */
	public WaypointQuery(){
		super();
		
		caFilter = new ConservationAreaFilter();
		if (SmartDB.getCurrentConservationArea() != null){
			caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		}
		
		dateFilter = null;
		strQueryFilter = "";
	}
	
	/**
	 * Creates a new waypoint query the given filter and
	 * the default conservation area filter and no date filter.
	 * 
	 * @param pFilter query filter
	 */
	public WaypointQuery(IFilter pFilter){
		this();
		
		this.queryFilter = pFilter;
		this.strQueryFilter = pFilter.asString();
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
	public void setDateFilter(DateFilter dateFilter){
		this.dateFilter = dateFilter;
	}
	
	
	/**
	 * Returns a list of columns that are visible in the output table.
	 * @return a list of visible column
	 */
	@Column(name = "column_filter")
	public String getVisibleColumns(){
		if (this.visibleTableColumns == null){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		
		for (String column : this.visibleTableColumns){
			sb.append(column);
			sb.append(",");
		}
		if (sb.length() > 0){
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
	/**
	 * Sets the columns that are visible in the output table.
	 * @param columns
	 */
	public void setVisibleColumns(String columns){
		if (columns == null){
			return;
		}
		visibleTableColumns = new HashSet<String>();
		String bits[] = columns.split(",");
		for (int i = 0; i < bits.length; i++){
			visibleTableColumns.add(bits[i]);
		}
	}
	
	/**
	 * Sets the columns that are visible in the output table.
	 * @param columns
	 */
	
	public void setVisibleColumns(Set<String> columns){
		this.visibleTableColumns = columns;
	}
	@Transient
	public Set<String> getVisibleColumnsAsArray(){
		return this.visibleTableColumns;
	}
	
	/**
	 * @return the conservation area filter
	 */
	@Transient
	public ConservationAreaFilter getConservationAreaFilterAsFilter(){
		return this.caFilter;
	}
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilter(ConservationAreaFilter filter){
		this.caFilter = filter;
	}
	@Column(name="ca_filter")
	public String getConservationAreaFilter(){
		return this.caFilter.asString();
	}
	public void setConservationAreaFilter(String caFilterString){
		this.caFilter = ConservationAreaFilter.parseFilter(caFilterString);
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
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	private  IFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return IFilter.EMPTY_FILTER;
		}
		InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes());
		Parser parser = new Parser(is);
		IFilter myQuery = parser.Expression();
		is.close();
		return myQuery;
	}
	
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
	@Transient
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
	@Transient
	public List<QueryResultItem> getLastResults(){
		return lastResults;
	}
	
	/** public for testing purposes only */
	@Transient
	public List<QueryResultItem> getQueryResults(Session session, IProgressMonitor progressMonitor) throws Exception{
		DerbyQueryEngine2 engine = new DerbyQueryEngine2();
		return engine.executeQuery(this, session, progressMonitor);
	}
	
	
	@Transient
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
	
	@Transient
	private List<DropItem> items;
	@Transient
	public List<DropItem> getDropItems(){
		return items;
	}
	@Transient
	public void setDropItems(List<DropItem> items){
		this.items = items;
	}
	
	@Transient
	public WaypointQuery clone(){
		WaypointQuery q = new WaypointQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(this.getVisibleColumnsAsArray());
		return q;
	}
	
}
