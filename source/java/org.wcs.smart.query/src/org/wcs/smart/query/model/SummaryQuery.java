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
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.DerbySummaryEngine;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.DateGroupBy;
import org.wcs.smart.query.parser.internal.summary.GroupByPart;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.ValuePart;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.summary_query")
public class SummaryQuery extends Query {

	private String strQuery;	
	@Transient
	private SumQueryDefinition query;	//cached copy of the parsed query

	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
	
	/* transient fields for tracking ui items */
	@Transient
	private List<DropItem> rowGroupByDropItems;
	@Transient
	private List<DropItem> colGroupByDropItems;
	@Transient
	private List<DropItem> valueDropItems;
	@Transient
	private List<DropItem> filterDropItems;
	@Transient
	private SummaryQueryResult lastResults;
	
	/**
	 * Creates a new summary query with the default
	 * conservation area filter and no date filter
	 */
	public SummaryQuery(){
		super();
		setName("<No Name Summary>");
		caFilter = new ConservationAreaFilter();
		if (SmartDB.getCurrentConservationArea() != null){
			caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		}
		
		dateFilter = null;
	}
	
	@Transient
	public SumQueryDefinition getQueryDefinition(){
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
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	private  SumQueryDefinition parseQuery() throws Exception {
		
		if (getQuery() == null || getQuery().length() == 0){
			return null;
		}
		InputStream is = new ByteArrayInputStream(getQuery().getBytes());
		Parser parser = new Parser(is);
		SumQueryDefinition myQuery = parser.SumQuery();
		is.close();
		return myQuery;
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
		return QueryType.SUMMARY;
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
	 * 
	 * @return Results from last query run
	 */
	@Transient
	public SummaryQueryResult getLastResults(){
		return this.lastResults;
	}
	
	/**
	 * Executes the query and returns the results
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	@Transient
	public SummaryQueryResult getQueryResults(IProgressMonitor monitor) throws Exception{
		lastResults = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			DerbySummaryEngine engine = new DerbySummaryEngine();
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
	public SummaryQuery clone(){
		SummaryQuery q = new SummaryQuery();
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
	 * @return row group by drop items
	 */
	@Transient
	public List<DropItem>getRowGroupByDropItems(){
		return this.rowGroupByDropItems;
	}
	
	/**
	 * @param items row gorup by drop items
	 */
	@Transient
	public void setRowGroupByDropItems(List<DropItem> items){
		this.rowGroupByDropItems = items;
	}
	
	/**
	 * @return column group by drop items
	 */
	@Transient
	public List<DropItem>getColumnGroupByDropItems(){
		return this.colGroupByDropItems;
	}
	
	/**
	 * @param items column group by drop items
	 */
	@Transient
	public void setColumnGroupByDropItems(List<DropItem> items){
		this.colGroupByDropItems = items;
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
	 * @see org.wcs.smart.query.model.Query#generateDropItems(org.hibernate.Session)
	 */
	@Override
	@Transient
	public void generateDropItems(Session session) throws Exception{
		clearDropItemList(rowGroupByDropItems);
		clearDropItemList(colGroupByDropItems);
		clearDropItemList(valueDropItems);
		clearDropItemList(filterDropItems);
		if (rowGroupByDropItems == null){
			rowGroupByDropItems = new ArrayList<DropItem>();
		}
		if (colGroupByDropItems == null){
			colGroupByDropItems = new ArrayList<DropItem>();
		}
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
		ValuePart part = getQueryDefinition().getValuePart();
		List<DropItem> valueItems = part.getDropItems(session);
		valueDropItems.addAll(valueItems);
		
		//---- generate drop items for group by items ----
		GroupByPart groupByPart = getQueryDefinition().getRowGroupByPart();
		List<DropItem> groupbyItems = groupByPart.getDropItems(session);
		rowGroupByDropItems.addAll(groupbyItems);
		
		//---- generate drop items for group by items ----
		groupByPart = getQueryDefinition().getColumnGroupByPart();
		groupbyItems = groupByPart.getDropItems(session);
		colGroupByDropItems.addAll(groupbyItems);
	
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
		
		for (IValueItem valueIt : def.getValuePart().getValueItems()){
			if (valueIt instanceof PatrolValueItem){
				PatrolValueItem pIt = (PatrolValueItem) valueIt;
				if (pIt.getOption() == PatrolValueOption.NUM_NIGHTS){
					//cannot group by patrol leader, patrol memeber, time period, or transport
					for (IGroupBy groupBy : groupBys){
						if (groupBy instanceof PatrolGroupBy){
							Class<?> source = ((PatrolGroupBy)groupBy).getOption().getPatrolAttributeClass();
							if (source.equals(PatrolLeg.class) || source.equals(PatrolLegMember.class)){
								return "The value " + pIt.getOption().getGuiName() + " cannot be grouped by " + ((PatrolGroupBy)groupBy).getOption().getGuiName();
							}
						}else if (groupBy instanceof DateGroupBy){
							return "The value " + pIt.getOption().getGuiName() + " cannot be grouped by date";
						}
					}
				}else if (pIt.getOption() == PatrolValueOption.MAN_DAYS ||
						pIt.getOption() == PatrolValueOption.MAN_HOURS || 
						pIt.getOption() == PatrolValueOption.NUM_MEMBERS ){
					
					for (IGroupBy groupBy : groupBys){
						if (groupBy instanceof PatrolGroupBy){
							if (((PatrolGroupBy)groupBy).getOption() == PatrolQueryOption.EMPLOYEE){
								return "The value " + pIt.getOption().getGuiName() + " cannot be grouped by " + ((PatrolGroupBy)groupBy).getOption().getGuiName();
							}
						}
					}
				}
			}
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
	 * @see org.wcs.smart.query.model.Query#copyFrom(org.wcs.smart.query.model.Query)
	 */
	public void copyFrom(Query copy){
		assert copy instanceof SummaryQuery;
		
		SummaryQuery q = (SummaryQuery)copy;
		setQuery(q.getQuery());
	}
}
