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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.engine.DerbyGridEngine;
import org.wcs.smart.query.parser.internal.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.filter.DateFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.gridded_query")
public class GriddedQuery extends Query {

	private String strQuery;	
	@Transient

	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
	
	/* transient fields for tracking ui items */
	@Transient
	private List<DropItem> valueDropItems;
	@Transient
	private List<DropItem> filterDropItems;
	@Transient
	private GriddedQueryResult lastResults;
	
	private int gridSize;
	
	
	/**
	 * Creates a new gridded query with the default
	 * conservation area filter and no date filter
	 */
	public GriddedQuery(){
		super();
		setName("<No Name Gridded Summary>");
		caFilter = new ConservationAreaFilter();
		if (SmartDB.getCurrentConservationArea() != null){
			caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		}
		
		dateFilter = null;
	}
	
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	public  void parseQuery() throws Exception {
		
		if (getQuery() == null || getQuery().length() == 0){
			return;
		}
		InputStream is = new ByteArrayInputStream(getQuery().getBytes());
		Parser parser = new Parser(is);
//TODO: parse the string of the query into variables/lists 
		
//		SumQueryDefinition myQuery = parser.SumQuery();
//		is.close();
		return;
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
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public QueryType getType() {
		return QueryType.GRIDDED;
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
	}
	
	
	/**
	 * @return the query filter as string
	 */
	@Column(name = "query_def")
	public String getQuery(){
		return this.strQuery;
	}

	/**
	 * 
	 * @return Results from last query run
	 */
	@Transient
	public GriddedQueryResult getLastResults(){
		return this.lastResults;
	}
	
	/**
	 * Executes the query and returns the results
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	@Transient
	public GriddedQueryResult getQueryResults(IProgressMonitor monitor) throws Exception{
		lastResults = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			DerbyGridEngine engine = new DerbyGridEngine();
			lastResults = engine.executeQuery(this, session, monitor);
			return lastResults;
		}finally{
			if (session.isOpen()){
				session.getTransaction().commit();
				session.close();
			}
		}
	}
	
	/**
	 * Creates a copy of the summary query
	 * with a null uuid, and null id;
	 * 
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public GriddedQuery clone(){
		GriddedQuery q = new GriddedQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQuery(getQuery());
		return q;
	}
	
	

	/**
	 * @return value drop items
	 */
	@Transient
	public List<DropItem>getValueDropItems(){
		return this.valueDropItems;
	}
	
	/**
	 * @param items value drop items
	 */
	@Transient
	public void setValueDropItems(List<DropItem> items){
		this.valueDropItems = items;
	}
	
	/**
	 * @return filter drop items
	 */
	@Transient
	public List<DropItem>getFilterDropItems(){
		return this.filterDropItems;
	}
	
	/**
	 * @param items filter drop items
	 */
	@Transient
	public void setFilterDropItems(List<DropItem> items){
		this.filterDropItems = items;
	}
	
	/**
	 * Generates ui drop items for the given query
	 * @param session
	 * @throws Exception
	 */
	@Transient
	public void generateDropItems(Session session) throws Exception{

		clearDropItemList(valueDropItems);
		clearDropItemList(filterDropItems);
		if (valueDropItems == null){
			valueDropItems = new ArrayList<DropItem>();
		}
		if (filterDropItems == null){
			filterDropItems = new ArrayList<DropItem>();
		}

		
		/* not sure we need this, keeping it commented for now. 
		//---- generate drop items for filter items ----		
		IFilter query = getQueryDefinition().getQueryFilter();
		if (query != null){
			DropItem[] filterItems = query.getDropItems(session);
			for (int i = 0; i < filterItems.length; i ++){
				filterDropItems.add(filterItems[i]);
			}
		}
		
		//---- generate drop items for value items ----
		ValuePart part = getQueryDefinition().getValuePart();
		List<DropItem> valueItems = part.getDropItems(session);
		valueDropItems.addAll(valueItems);
		*/
			
	}
	
	@Transient
	private void clearDropItemList(List<DropItem> list){
		if (list != null){
			for (DropItem it : list){
				it.dispose();
			}
			list.clear();
		}
	}
	
	/**
	 * Validates the query.  Assumes the query
	 * definition is formed from a valid string.
	 * <p>
	 * This validates the items in the query.

	 */
	public static String validate(){
//TODO: validation on the query string and/or definition
		//return the error if there is on, null if it's ok.
		
		return null;
	}
}
