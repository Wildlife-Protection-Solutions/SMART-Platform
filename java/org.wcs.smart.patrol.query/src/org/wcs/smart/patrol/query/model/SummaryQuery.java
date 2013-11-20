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
package org.wcs.smart.patrol.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
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
import org.wcs.smart.patrol.query.engine.DerbySummaryEngine;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.types.PatrolSummaryQueryType;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.summary_query")
public class SummaryQuery extends Query {

	/* db fields */
	private String strQuery;	
	
	/* transient fields */
	private SumQueryDefinition query;	//cached copy of the parsed query
	private DateFilter dateFilter;
	
	/**
	 * Creates a new summary query with the default
	 * conservation area filter and no date filter
	 */
	protected SummaryQuery(){
		super();
		
	}
	
	@Transient
	public SumQueryDefinition getQueryDefinition(){
		if (query == null){
			try{
				query = parseQuery();
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.SummaryQuery_ParseError, ex);
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
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public IQueryType getType() {
		return QueryTypeManager.getInstance().findQueryType(PatrolSummaryQueryType.KEY);
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
	
	
	@Transient
	public SummaryQueryResult executeQueryInternal(IProgressMonitor monitor) throws Exception{
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			DerbySummaryEngine engine = new DerbySummaryEngine();
			SummaryQueryResult lastResults = engine.executeQuery(this, session, monitor);
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
		SummaryQuery q = PatrolQueryFactory.createSummaryQuery();
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
						if (groupBy instanceof CategoryGroupBy ){
							return MessageFormat.format(
									Messages.SummaryQuery_CannotGroupByCategory, new Object[]{pIt.getOption().getGuiName()});
									
						}else if (groupBy instanceof AttributeGroupBy){
							return MessageFormat.format(
									Messages.SummaryQuery_CannotGroupByAttribute, new Object[]{pIt.getOption().getGuiName()});
						}
					}
				}else if (pIt.getOption() == PatrolValueOption.MAN_DAYS ||
						pIt.getOption() == PatrolValueOption.MAN_HOURS || 
						pIt.getOption() == PatrolValueOption.NUM_MEMBERS ){
					
					for (IGroupBy groupBy : groupBys){
						if (groupBy instanceof PatrolGroupBy){
							if (((PatrolGroupBy)groupBy).getOption() == PatrolQueryOption.EMPLOYEE){
								return MessageFormat.format(
										Messages.SummaryQuery_GroupByError3 , new Object[]{pIt.getOption().getGuiName(), ((PatrolGroupBy)groupBy).getOption().getGuiName()});
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
	 * @see org.wcs.smart.query.model.Query#copyQuery(org.wcs.smart.query.model.Query)
	 */
	public void copyQuery(Query copy){
		assert copy instanceof SummaryQuery;
		SummaryQuery q = (SummaryQuery)copy;
		setQuery(q.getQuery());
		setConservationAreaFilter(getConservationAreaFilter());
	}
}
