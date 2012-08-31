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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.DerbyGridEngine;
import org.wcs.smart.query.model.observation.QueryColumn;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A class to represent a summary query.
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@Table(name="smart.gridded_query")
public class GriddedQuery extends Query {

	private GridQueryDefinition query;
	
	private String strQuery;	
	@Transient

	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
	public IFilter queryFilter;	//cached copy of the parsed query
	
	private List<QueryColumn> queryColumns = null;
	
	/* transient fields for tracking ui items */
	@Transient
	private List<DropItem> valueDropItems;
	@Transient
	private List<DropItem> filterDropItems;
	@Transient
	private List<GridResultItem> lastResults;

	@Transient
	private GridQueryResultMetadata resultMetadata;
	
	
	private double gridSize;
	private IValueItem valueItem;
	
	private List<DropItem> items;
	
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
	public  GridQueryDefinition parseQuery() throws Exception {
		
		if (strQuery == null || strQuery.length() == 0){
			return null;
		}
		InputStream is = new ByteArrayInputStream(strQuery.getBytes());
		Parser parser = new Parser(is);
	
		GridQueryDefinition myQuery = parser.GridQuery();
		queryFilter = myQuery.getQueryFilter();
		gridSize = myQuery.getGridSize();
		valueItem = myQuery.getValuePart();
		is.close();
		return myQuery;
	}
	
	/**
	 * @return the conservation area filter
	 */
	
	@Column(name="ca_filter")
	public String getConservationAreaFilterAsFilter(){
		return this.caFilter.asString();
	}
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilterAsFilter(String filter){
		this.caFilter = ConservationAreaFilter.parseFilter(filter);
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
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setQuery(String queryStr, GridQueryDefinition queryDef){
		this.strQuery = queryStr;
		this.query = queryDef;
		if(query != null){
			queryFilter = query.getQueryFilter();
		}
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
	public List<GridResultItem> getLastResults(){
		return this.lastResults;
	}
	
	/**
	 * Executes the query and returns the results
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	@Transient
	public List<GridResultItem> getQueryResults(IProgressMonitor monitor) throws Exception{
		lastResults = Collections.emptyList();
		resultMetadata = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			
			DerbyGridEngine engine = new DerbyGridEngine();
			lastResults = engine.executeQuery(this, session, monitor);
			resultMetadata = GridQueryResultMetadata.computeMetadata(lastResults);
			return lastResults;
		}finally{
			if (session.isOpen()){
				session.getTransaction().commit();
				session.close();
			}
		}
	}
	
	/**
	 * 
	 * @return the metadata about the last set of results computed
	 * or null if not yet computed
	 */
	@Transient
	public GridQueryResultMetadata getResultMetadata(){
		return this.resultMetadata;
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
		q.setConservationAreaFilter(caFilter);
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

		
		//---- generate drop items for filter items ----		
		IFilter query = getQueryDefinition().getQueryFilter();
		if (query != null){
			DropItem[] filterItems = query.getDropItems(session);
			for (int i = 0; i < filterItems.length; i ++){
				filterDropItems.add(filterItems[i]);
			}
		}
		
		//---- generate drop items for value items ----
		List<DropItem> valueItems = new ArrayList<DropItem>();
		IValueItem part = getQueryDefinition().getValuePart();

		valueItems.add(part.asDropItem(session));
		valueDropItems.addAll(valueItems);
		
			
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
	public String validate(){
		//TODO: validation on the query string and/or definition
		//return the error if there is on, null if it's ok.
		if (gridSize <= 0){
			return "Invalid Grid Size";
		}
		return null;
	}


	
	@Transient
	public List<QueryColumn> getQueryColumns() {
		if (this.queryColumns == null){
			initQueryColumns();
		}
		return this.queryColumns;
	}



	/**
	 * Loads the query columns
	 */
	private void initQueryColumns(){
		QueryColumn[] cols = QueryColumn.getGridColumns();
		
		queryColumns = new ArrayList<QueryColumn>();
		for (int i = 0; i < cols.length; i ++){
			queryColumns.add(cols[i]);
		}
	}
	
	
	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	//@Override
	//public void setQueryFilter(String filter){
		//I think this function is not used anymore
		
		//		String[] temp = filter.split("[|]");
//		if(temp.length < 1){
//			this.valueFilter = "";
//		}else{
//			this.valueFilter = temp[0];
//		}
//		if(temp.length < 2){
//			this.strQueryFilter = "";
//		}else{
//			this.strQueryFilter = temp[1];
//		}
//		this.queryFilter = null;
		
 
	//}
	

	/**
	 * Sets the grid size.  
	 * 
	 * @param size, the size of the grid squares
	 * @return
	 */
	public void setGridSize(int size){
		this.gridSize = size;
	}
	
	/**
	 * Gets the grid size.
	 * 
	 * @return the grid size as an int
	 */
	@Transient
	public double getGridSize(){
		return this.gridSize;
	}
	

	@Transient
	public GridQueryDefinition getQueryDefinition(){
		if (query == null){
			try{
				query = parseQuery();
			} catch (Exception ex) {
				QueryPlugIn.displayLog("Could not parse query.", ex);
			}
		}
		return query;
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
				GridQueryDefinition def = parseQuery();
				queryFilter = def.getQueryFilter();
			} catch (Exception ex) {
				QueryPlugIn.displayLog("Could not parse query.", ex);
			}
		}
		if(queryFilter == null){
			return IFilter.EMPTY_FILTER;
		}else{
			return queryFilter;
		}
	}

	@Transient
	public IValueItem getValueItem() {
		return valueItem; 
		
	}


	@Override
	public boolean isDefinitionEqual(Query other) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void copyFrom(Query copy) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilter(ConservationAreaFilter filter){
		this.caFilter = filter;
	}
	
	/**
	 * @return the conservation area filter
	 */
	@Transient
	public ConservationAreaFilter getConservationAreaFilter(){
		return this.caFilter;
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
}
