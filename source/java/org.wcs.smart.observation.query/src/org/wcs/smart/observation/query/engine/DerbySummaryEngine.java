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
package org.wcs.smart.observation.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.internal.ObservationValueItemLabelProvider;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.filter.WaypointSourceGroupBy;
import org.wcs.smart.observation.query.ui.definition.ObservationDropItemFactory;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationGroupByVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationValueVisitor;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryResultKey;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.YearDateGroupBy;
import org.wcs.smart.query.model.summary.AreaGroupBy;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.ConservationAreaGroupBy;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.model.summary.ValuePart;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for executing summary
 * queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbySummaryEngine extends AbstractDerbyObservationQueryEngine {

	private SummaryQueryResult sumResults = null;
	HashMap<String, HashMap<SummaryResultKey, Double>> cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
	
	private String rateTable;
	private String valueTable;
	
	@Override
	public boolean canExecute(String querytype) {
		return ObservationSummaryQuery.KEY.equals(querytype);
	}

	/**
	 * Executes the given summary query.
	 * 
	 * @param query
	 *            the query to execute
	 * @param session
	 *            open hibernate session
	 * @param monitor
	 *            progress monitor
	 * 
	 * @return the results of the query
	 * @throws SQLException
	 */
	/*
	 * The query execute process is as follows:
	 * 
	 * 1) If the query includes attributes then create a "cross join" table
	 * of all observations and the required attributes. This table (observationTempTable)
	 * looks as follows:
	 * observation_uuid | attribute1 | attribute 2 | attribute 3 etc.
	 * 
	 * 2) A temporary table (queryTempTable) is created for holding all observations which
	 * match the required filter.  This table contains all the patrol
	 * to waypoint attributes and the observation id.  IT does 
	 * not contain any of the matched attributes.
	 * 
	 * 3) For each patrol value to compute the results 
	 * are commputed and added to the results.
	 *  
	 */
	@Override
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{

		final ObservationSummaryQuery query = (ObservationSummaryQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
	
		valueTable = createTempTableName();
		rateTable = createTempTableName();
		

		sumResults = new SummaryQueryResult();
		cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
		
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SumQueryDefinition def = null;
				try{
					def = query.getQueryDefinition();
				}catch (Exception ex){
					throw new SQLException (ex.getMessage(), ex);
				}
				monitor.beginTask(Messages.DerbySummaryEngine_Progress_RunningQuery, def.getValuePart().getValueItems().size() + 5);

				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				final DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				c.setAutoCommit(true);
				
				try {
					monitor.subTask(Messages.DerbySummaryEngine_Progress_LoadingHeaders);
					getHeaderInfo(query, sumResults, session);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					boolean needsObservationValue = false;
					boolean needsObservationRate = false;
					
					List<IGroupBy> all = new ArrayList<IGroupBy>();
					all.addAll(def.getColumnGroupByPart().getGroupBys());
					all.addAll(def.getRowGroupByPart().getGroupBys());
					GroupByPart allGroupBy = new GroupByPart(all);
					
					HasObservationValueVisitor vv = new HasObservationValueVisitor();
					def.getValuePart().visit(vv);
					needsObservationValue = vv.hasCategory() || vv.hasAttribute();
					
					if(!needsObservationValue){
						HasObservationGroupByVisitor cv = new HasObservationGroupByVisitor();
						def.getColumnGroupByPart().visit(cv);
						needsObservationValue = cv.hasCategory() || cv.hasAttribute();
						if (!needsObservationValue){
							def.getRowGroupByPart().visit(cv);
							needsObservationValue = cv.hasCategory() || cv.hasAttribute();
						}
						
					}
					needsObservationRate = needsObservationValue;
					QueryFilter valueFilter = new QueryFilter(EmptyFilter.INSTANCE);
					if (def.getValueFilter() != null){
						valueFilter = def.getValueFilter();
					}
					QueryFilter rateFilter = new QueryFilter(EmptyFilter.INSTANCE);
					if (def.getRateFilter() != null){
						rateFilter = def.getRateFilter();
					}
					
					if (!needsObservationValue){
						
						HasObservationFilterVisitor visitor = new HasObservationFilterVisitor();
						visitor.visit(valueFilter.getFilter());
						if (visitor.hasAttributeFilter() || visitor.hasCategoryFilter()){
							needsObservationValue = true;
						}
						
						visitor.clear();
						visitor.visit(rateFilter.getFilter());
						if (visitor.hasAttributeFilter() || visitor.hasCategoryFilter()){
							needsObservationRate = true;
						}
					}
					
					IFilterProcessor filterer = DerbySummaryEngine.this.getFilterProcessor(valueFilter.getFilterType(), valueTable, query);
					ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					try{
						filterer.processFilter(c, valueFilter.getFilter(), dFilter, cafilter, needsObservationValue, false, monitor);
					}finally{
						filterer.dropTemporaryTables(c);
					}
					
					if (monitor.isCanceled()){
						return;
					}
					monitor.subTask(Messages.DerbySummaryEngine_Progress_ProcessingValue);
					addCategoryHkey(valueTable, allGroupBy, def.getValuePart(), c);
					
					String vFilter = valueFilter.asString();
					String rFilter = rateFilter.asString();
					
					if (vFilter.equals(rFilter)){
						rateTable = valueTable;
					}else{
						rateTable = createTempTableName();
						
						
						IFilterProcessor rfilterer = DerbySummaryEngine.this.getFilterProcessor(rateFilter.getFilterType(), rateTable, query);
						try{
							rfilterer.processFilter(c, rateFilter.getFilter(), dFilter, cafilter, needsObservationRate, false, monitor);
						}finally{
							rfilterer.dropTemporaryTables(c);
						}
						if (monitor.isCanceled()){
							return;
						}
						monitor.subTask(Messages.DerbySummaryEngine_Progress_ProcessingValue);
						addCategoryHkey(rateTable, allGroupBy, def.getValuePart(), c);
					}
					
					HashMap<SummaryResultKey, Double> data = computeSummaryValues(c, session, 
							allGroupBy, def.getValuePart(), query, monitor);
					
					if (monitor.isCanceled() || data == null){
						return ;
					}
					sumResults.setData(data);
					
					
					monitor.worked(1);
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
					monitor.done();
					c.setAutoCommit(false);
				}
			}
		});

		return sumResults ;
	}

	private void dropTemporaryTables(Connection c){
		dropTable(c, rateTable);
		dropTable(c, valueTable);
	}
	
	private void addCategoryHkey(String tableName, GroupByPart groupByPart, ValuePart values, Connection c) throws SQLException{
		boolean add = false;
		for (IGroupBy groupBy : groupByPart.getGroupBys()){
			if (groupBy instanceof CategoryGroupBy ||
					(groupBy instanceof AttributeGroupBy && ((AttributeGroupBy) groupBy).getCategoryHkey() != null) ){
				add = true;
				break;
			}
		}
		if (!add){
			HasObservationValueVisitor visitor = new HasObservationValueVisitor();
			values.visit(visitor);
			add = visitor.hasCategory();
		}
		
		if (add){
			StringBuilder sql = new StringBuilder();
			sql.append("ALTER TABLE "); //$NON-NLS-1$
			sql.append(tableName);
			sql.append(" ADD column cat_hkey varchar(32672)"); //$NON-NLS-1$
			QueryPlugIn.logSql(sql.toString());
			
			c.createStatement().execute(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(tableName);
			sql.append(" SET cat_hkey = "); //$NON-NLS-1$
			sql.append("(SELECT "); //$NON-NLS-1$
			sql.append(tablePrefix(Category.class));
			sql.append(".hkey FROM "); //$NON-NLS-1$
			sql.append(tableNamePrefix(Category.class));
			sql.append(", "); //$NON-NLS-1$
			sql.append(tableNamePrefix(WaypointObservation.class));
			sql.append(" WHERE "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(tableName);
			sql.append(".ob_uuid AND "); //$NON-NLS-1$
			sql.append(tablePrefix(Category.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class));
			sql.append(".category_uuid )"); //$NON-NLS-1$
			
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
		}
	}
	
	/**
	 * Compute the each value defined in the summary.
	 * 
	 * @param c database connection
	 * @param s hibernate session
	 * @param groupBy summary query gorup by part
	 * @param values summary query values 
	 * @param monitor progress monitor
	 * @return map of results
	 * @throws SQLException
	 */
	private HashMap<SummaryResultKey, Double> computeSummaryValues(Connection c,
			Session s, 
			GroupByPart groupBy, 
			ValuePart values, Query query,
			IProgressMonitor monitor) throws SQLException{
	
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		for (IValueItem it : values.getValueItems()){
			monitor.subTask("Processing Value: " + it.asString()); //$NON-NLS-1$
			HashMap<SummaryResultKey, Double> data =computeValueItem(c, s, groupBy, it, query, valueTable) ; 
			if (data != null){
				results.putAll( data );	
			}
			
			monitor.worked(1);
			if (monitor.isCanceled()){
				return null;
			}
		}
		return results;
		
	}


	/**
	 * Computes the data for a given value item
	 * @param c
	 * @param s
	 * @param groupBy
	 * @param it
	 * @return
	 * @throws SQLException
	 */
	private HashMap<SummaryResultKey, Double> computeValueItem(
			Connection c, 
			Session s,
			GroupByPart groupBy, 
			IValueItem it, 
			Query query,
			String dataTable) throws SQLException {
		
		String cacheKey = it.asString() + "_" + groupBy.asString() + "_" + dataTable; //$NON-NLS-1$ //$NON-NLS-2$
		HashMap<SummaryResultKey, Double> results = cachedValueToResults.get(cacheKey); 
		if (results != null){
			return results;
		}
		if (it instanceof AttributeValueItem){
			results =  (getAttributeValue(dataTable, c, s, groupBy, (AttributeValueItem)it, query));
		}else if (it instanceof CategoryValueItem){
			results = (getCategoryValue(dataTable, c, s, groupBy, (CategoryValueItem)it, query));
		}
		if (results != null){
			cachedValueToResults.put(cacheKey, results); 
		}
		
		return results;
	}
	
	/**
	 * Computes a patrol summary value 
	 * @param c database connection
	 * @param s hibernate session 
	 * @param groupBy query group by options
	 * @param patrolItem patrol value to computer  
	 * @return query results
	 * @throws SQLException
	 */
	private HashMap<SummaryResultKey, Double> getAttributeValue(
			String dataTableName,
			Connection c, Session s, 
			GroupByPart groupBy, 
			AttributeValueItem attributeItem, Query query) throws SQLException{
		
		clearParameters();
		if (attributeItem.getAttributeType() == AttributeType.NUMERIC) {
			StringBuilder fromSql = new StringBuilder();

			fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
			StringBuilder groupBySql = new StringBuilder();
			StringBuilder groupByInnerSql = new StringBuilder();

			createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql,
					attributeItem, query);

			String valueSql = "temp.ob_uuid"; //$NON-NLS-1$
			if (attributeItem.getCategoryKey() != null) {
				valueSql = valueSql + ",temp.cat_hkey"; //$NON-NLS-1$
			}
			StringBuilder valueAggSql = new StringBuilder();
			valueAggSql.append(attributeItem.getAggregationKey());
			valueAggSql.append("("); //$NON-NLS-1$
			valueAggSql.append(tablePrefix
					.get(WaypointObservationAttribute.class));
			valueAggSql.append(".number_value)"); //$NON-NLS-1$

			StringBuilder sql = new StringBuilder();
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(groupBySql);
			if (groupBySql.length() > 0) {
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueAggSql);
			sql.append(" FROM ( SELECT distinct "); //$NON-NLS-1$
			sql.append(groupByInnerSql);
			if (groupByInnerSql.length() > 0) {
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueSql);
			sql.append(" FROM "); //$NON-NLS-1$
			sql.append(fromSql);

			sql.append(") foo"); //$NON-NLS-1$

			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(WaypointObservationAttribute.class));
			sql.append(" on foo.ob_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".observation_uuid "); //$NON-NLS-1$
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(Attribute.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			sql.append(".uuid "); //$NON-NLS-1$

			sql.append(" WHERE "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".number_value is not null and "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			String p = addParameterValue(attributeItem.getAttributeKey());
			sql.append(".keyid = " + p); //$NON-NLS-1$
			if (attributeItem.getCategoryKey() != null) {
				String p1 = addParameterValue(attributeItem.getCategoryKey());
				String p2 = addParameterValue(attributeItem.getCategoryKey().substring(0, attributeItem.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND ( foo.cat_hkey >= " + p1 + " and  foo.cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if (groupBySql.length() > 0) {
				sql.append(" GROUP BY "); //$NON-NLS-1$
				sql.append(groupBySql);
			}

			// do something here with sql
			QueryPlugIn.logSql(sql.toString());
			ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();
			return createValueResults(rs, groupBy, attributeItem.asString());
			
		} else if (attributeItem.getAttributeType() == AttributeType.LIST) {
			StringBuilder fromSql = new StringBuilder();
			
			fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
			StringBuilder groupBySql = new StringBuilder();
			StringBuilder groupByInnerSql = new StringBuilder();

			createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, attributeItem, query);
			
			String valueSql = ""; //$NON-NLS-1$
			
			StringBuilder valueAggSql = new StringBuilder();
			
			if (attributeItem.getValueType() == IValueItem.ValueType.OBSERVATION){
				valueSql = "temp.ob_uuid"; //$NON-NLS-1$
				valueAggSql.append("count(ob_uuid)"); //$NON-NLS-1$
			}else{
				valueSql = "temp.wp_uuid"; //$NON-NLS-1$
				valueAggSql.append("count(wp_uuid)"); //$NON-NLS-1$
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(groupBySql);
			if (groupBySql.length() > 0){
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueAggSql);
			sql.append(" FROM ( SELECT distinct "); //$NON-NLS-1$
			sql.append(groupByInnerSql);
			if (groupByInnerSql.length() > 0){
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueSql);
			sql.append(" FROM "); //$NON-NLS-1$
			sql.append(fromSql);
			
			
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(WaypointObservationAttribute.class));
			sql.append(" on temp.ob_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".observation_uuid "); //$NON-NLS-1$
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(Attribute.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			sql.append(".uuid "); //$NON-NLS-1$
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(AttributeListItem.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeListItem.class));
			sql.append(".attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			sql.append(".uuid "); //$NON-NLS-1$

			sql.append(" WHERE "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".list_element_uuid =  "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeListItem.class));
			sql.append(".uuid and "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeListItem.class));
			sql.append(".keyid = '"); //$NON-NLS-1$
			sql.append(attributeItem.getItemKey());
			sql.append("' and "); //$NON-NLS-1$
		
			sql.append(tablePrefix(Attribute.class));
			sql.append(".keyid = '"); //$NON-NLS-1$
			sql.append(attributeItem.getAttributeKey() + "'"); //$NON-NLS-1$
			if (attributeItem.getCategoryKey() != null){	
				String p1 = addParameterValue(attributeItem.getCategoryKey());
				String p2 = addParameterValue(attributeItem.getCategoryKey().substring(0, attributeItem.getCategoryKey().length()-1) + "/"); //$NON-NLS-1$
				sql.append("AND ( temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			sql.append(") as foo "); //$NON-NLS-1$
			if (groupBySql.length() > 0){
				sql.append(" GROUP BY " ); //$NON-NLS-1$
				sql.append(groupBySql);
			}
			
			//do something here with sql
			QueryPlugIn.logSql(sql.toString());
			ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();

			return createValueResults(rs, groupBy, attributeItem.asString());
		} else if (attributeItem.getAttributeType() == AttributeType.TREE) {
			StringBuilder fromSql = new StringBuilder();
			
			fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
			StringBuilder groupBySql = new StringBuilder();
			StringBuilder groupByInnerSql = new StringBuilder();

			createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, attributeItem, query);
			
			String valueSql = ""; //$NON-NLS-1$
			
			StringBuilder valueAggSql = new StringBuilder();
			
			if (attributeItem.getValueType() == IValueItem.ValueType.OBSERVATION){
				valueSql = "temp.ob_uuid"; //$NON-NLS-1$
				valueAggSql.append("count(ob_uuid)"); //$NON-NLS-1$
			}else{
				valueSql = "temp.wp_uuid"; //$NON-NLS-1$
				valueAggSql.append("count(wp_uuid)"); //$NON-NLS-1$
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(groupBySql);
			if (groupBySql.length() > 0){
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueAggSql);
			sql.append(" FROM ( SELECT distinct "); //$NON-NLS-1$
			sql.append(groupByInnerSql);
			if (groupByInnerSql.length() > 0){
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueSql);
			sql.append(" FROM "); //$NON-NLS-1$
			sql.append(fromSql);
			
			
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(WaypointObservationAttribute.class));
			sql.append(" on temp.ob_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".observation_uuid "); //$NON-NLS-1$
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(Attribute.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			sql.append(".uuid "); //$NON-NLS-1$
			sql.append("join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(AttributeTreeNode.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			sql.append(".uuid "); //$NON-NLS-1$

			sql.append(" WHERE "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".tree_node_uuid =  "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".uuid and ("); //$NON-NLS-1$
			
			String p1 = addParameterValue(attributeItem.getItemKey());
			String p2 = addParameterValue(attributeItem.getItemKey().substring(0, attributeItem.getItemKey().length() -1 ) + "/"); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey >=  " + p1); //$NON-NLS-1$
			sql.append(" and "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey < " + p2 + " ) and "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(tablePrefix(Attribute.class));
			p1 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			
			
			if (attributeItem.getCategoryKey() != null){
				p1 = addParameterValue(attributeItem.getCategoryKey());
				p2 = addParameterValue(attributeItem.getCategoryKey().substring(0, attributeItem.getCategoryKey().length()-1) + "/"); //$NON-NLS-1$
				sql.append("AND ( temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			sql.append(") as foo "); //$NON-NLS-1$
			if (groupBySql.length() > 0){
				sql.append(" GROUP BY " ); //$NON-NLS-1$
				sql.append(groupBySql);
			}
			
			//do something here with sql
			QueryPlugIn.logSql(sql.toString());
			ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();
			return createValueResults(rs, groupBy, attributeItem.asString());
		}
		return null;
	}
	
	/**
	 * Reads the results from a database value query
	 * and creates a set of summary results.
	 * 
	 * @param rs database value query result
	 * @param groupBy group by part
	 * @param valueKey value key 
	 * @return map of summary result key
	 * @throws SQLException
	 */
	private HashMap<SummaryResultKey, Double> createValueResults(ResultSet rs, GroupByPart groupBy, String valueKey) throws SQLException{
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		while(rs.next()){
			String groupby[] = new String[groupBy.getGroupBys().size()];
			
			int rsindex = 1;
			for (int i = 0; i < groupBy.getGroupBys().size(); i ++){
				IGroupBy gb = groupBy.getGroupBys().get(i);
				
				String key = gb.getKeyPart() + ":"; //$NON-NLS-1$
				switch (gb.getType()) {
					case STRING:
						key += rs.getString(rsindex++);
						break;
					case BYTE:
						key += UuidUtils.uuidToString(UuidUtils.byteToUUID(rs.getBytes(rsindex++)));
						break;
					case DATE:
						key += rs.getDate(rsindex++).toString();
						break;
					case KEY:
						key += rs.getString(rsindex++);
						break;
				}
				groupby[i] = key;
			}
			
			SummaryResultKey key = new SummaryResultKey(valueKey, groupby);
			results.put(key, rs.getDouble(rsindex++));
		}
		rs.close();
		return results;
	}
	
	
	
	/**
	 * Computes a category summaries 
	 * @param c database connection
	 * @param s hibernate session 
	 * @param groupBy query group by options
	 * @param patrolItem patrol value to computer  
	 * @return query results
	 * @throws SQLException
	 */
	private HashMap<SummaryResultKey, Double> getCategoryValue(
			String dataTable,
			Connection c, Session s, 
			GroupByPart groupBy, 
			CategoryValueItem categoryItem, Query query) throws SQLException{
		
		clearParameters();
		StringBuilder fromSql = new StringBuilder();
		
		fromSql.append(dataTable + " temp "); //$NON-NLS-1$
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();

		createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, categoryItem, query);
		
		String valueSql = ""; //$NON-NLS-1$
		StringBuilder valueAggSql = new StringBuilder();
		if (categoryItem.getType() == IValueItem.ValueType.OBSERVATION){
			valueSql = "temp.ob_uuid"; //$NON-NLS-1$
			valueAggSql.append("count(ob_uuid)"); //$NON-NLS-1$
		}else{
			valueSql = "temp.wp_uuid"; //$NON-NLS-1$
			valueAggSql.append("count(wp_uuid)"); //$NON-NLS-1$
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT "); //$NON-NLS-1$
		sql.append(groupBySql);
		if (groupBySql.length() > 0){
			sql.append(","); //$NON-NLS-1$
		}
		sql.append(valueAggSql);
		sql.append(" FROM ( SELECT distinct "); //$NON-NLS-1$
		//sql.append("temp.cat_hkey, "); //$NON-NLS-1$
		sql.append(groupByInnerSql);
		if (groupByInnerSql.length() > 0){
			sql.append(","); //$NON-NLS-1$
		}
		sql.append(valueSql);
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(fromSql);
		
		sql.append(" WHERE "); //$NON-NLS-1$
		String hkey = categoryItem.getCategoryHKey();
		if (hkey == null){
			sql.append(" cat_hkey is not null "); //$NON-NLS-1$
		}else{
			String p1 = addParameterValue(categoryItem.getCategoryHKey());
			String p2 = addParameterValue(categoryItem.getCategoryHKey().substring(0, categoryItem.getCategoryHKey().length()-1) + "/"); //$NON-NLS-1$
			sql.append(" ("); //$NON-NLS-1$
			sql.append("cat_hkey >=  " + p1 + " and cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		sql.append(") foo"); //$NON-NLS-1$
		
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " ); //$NON-NLS-1$
			sql.append(groupBySql);
		}
		
		//do something here with sql
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();
		return createValueResults(rs, groupBy, categoryItem.asString());
	}
	
	/**
	 * Updates group by string builder string with
	 * given group by part. 
	 *  
	 * @param groupBy
	 * @param fromSql
	 * @param groupBySql
	 * @param groupByInnerSql
	 */
	private List<String> areaGroupByPrefix = new ArrayList<String>();
	
	private void createGroupBySql(GroupByPart groupBy,
			StringBuilder fromSql,
			StringBuilder groupBySql, 
			StringBuilder groupByInnerSql, IValueItem value, Query query) throws SQLException{
		areaGroupByPrefix.clear();
		
		int itemcnt = 1;
		boolean waypointAdd = false;
		
		for (IGroupBy gb : groupBy.getGroupBys()){
			if (gb instanceof AreaGroupBy){
				if (value instanceof CategoryValueItem
						|| value instanceof AttributeValueItem) {
					//category and attribute value area group bys use the waypoint location
					AreaGroupBy agb = (AreaGroupBy) gb;
					String key = agb.getAreaType().name() + "_" + itemcnt; //$NON-NLS-1$s
					String areaPrefix = tablePrefix(Area.class)
							+ "_" + itemcnt; //$NON-NLS-1$
					groupByInnerSql
							.append(areaPrefix + ".keyid" + " as " + key); //$NON-NLS-1$ //$NON-NLS-2$

					if (!waypointAdd) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(Waypoint.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(Waypoint.class));
						fromSql.append(" on temp.wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
						waypointAdd = true;
					}
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNames.get(Area.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(areaPrefix);
					fromSql.append(" on smart.pointinpolygon("); //$NON-NLS-1$
					fromSql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
					fromSql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
					fromSql.append(areaPrefix + ".geom"); //$NON-NLS-1$
					fromSql.append(")"); //$NON-NLS-1$
					fromSql.append(" and ");//$NON-NLS-1$
					fromSql.append(areaPrefix + ".ca_uuid = x'" + UuidUtils.uuidToString(query.getConservationArea().getUuid()) + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					fromSql.append(" and "); //$NON-NLS-1$
					fromSql.append(areaPrefix + ".area_type = '" + agb.getAreaType().name() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					
					groupBySql.append(key);
				} 
			}else if (gb instanceof ConservationAreaGroupBy){
				groupByInnerSql.append(tablePrefix(Waypoint.class));
				groupByInnerSql.append(".ca_uuid as cauuid_" + itemcnt); //$NON-NLS-1$
				groupBySql.append("cauuid_" + itemcnt); //$NON-NLS-1$
				if (!waypointAdd) {
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNames.get(Waypoint.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(Waypoint.class));
					fromSql.append(" on temp.wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
					waypointAdd = true;
				}
			}else if (gb instanceof DateGroupBy){
				IDateGroupBy op = ((DateGroupBy)gb).getOption();
				if (op.getClass().equals(DayDateGroupBy.class)){
					groupByInnerSql.append("date( trim(cast(year("); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as char(4))) || '-' || trim(cast(month("); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as char(2))) || '-' || trim(cast(day("); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as char(2))) )"); //$NON-NLS-1$
					groupByInnerSql.append(" as wp_date_time_" + itemcnt); //$NON-NLS-1$
					groupBySql.append("wp_date_time_" + itemcnt); //$NON-NLS-1$
				}else if (op.getClass().equals(MonthDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					
					groupByInnerSql.append("trim(cast(month("); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as char(2))) || '/' || cast(year("); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as char(4)) as datePart_"); //$NON-NLS-1$
					groupByInnerSql.append( itemcnt); 

				}else if (op.getClass().equals(YearDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("YEAR("); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as datePart_" + itemcnt); //$NON-NLS-1$
				}
				
				if (!waypointAdd) {
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNamePrefix(Waypoint.class));
					fromSql.append(" on temp.wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
					waypointAdd = true;
				}
			}else if (gb instanceof CategoryGroupBy){
				CategoryGroupBy op = ((CategoryGroupBy)gb);

				String categoryKey = "category_" + itemcnt; //$NON-NLS-1$
				groupByInnerSql.append("smart.trimHkeyToLevel("); //$NON-NLS-1$
				groupByInnerSql.append(op.getTreeLevel() + ", "); //$NON-NLS-1$
				groupByInnerSql.append("cat_hkey) as " + categoryKey); //$NON-NLS-1$
				
				groupBySql.append(categoryKey);
			}else if (gb instanceof AttributeGroupBy){
			
				groupBySql.append("attribute_" + itemcnt); //$NON-NLS-1$
				if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.LIST){
					groupByInnerSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append(".keyid as  attribute_" + itemcnt); //$NON-NLS-1$
				}else if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.TREE){
					groupByInnerSql.append("smart.trimHkeyToLevel("); //$NON-NLS-1$
					groupByInnerSql.append(((AttributeGroupBy)gb).getTreeLevel().intValue() + ","); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(AttributeTreeNode.class)+ "_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append(".hkey) as  attribute_" + itemcnt + " "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				fromSql.append(" JOIN "); //$NON-NLS-1$
				fromSql.append(tableNames.get(WaypointObservationAttribute.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(tablePrefix(WaypointObservationAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(tablePrefix(WaypointObservationAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".observation_uuid = temp.ob_uuid "); //$NON-NLS-1$
			
				String catkey = ((AttributeGroupBy)gb).getCategoryHkey();
				if (catkey != null){
					String p1 = addParameterValue(catkey);
					String p2 = addParameterValue(catkey.substring(0, catkey.length() - 1) + "/"); //$NON-NLS-1$
					fromSql.append(" and (temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				
				fromSql.append(" JOIN "); //$NON-NLS-1$
				if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.LIST){
					fromSql.append(tableNames.get(AttributeListItem.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".uuid ="); //$NON-NLS-1$
					fromSql.append(tablePrefix(WaypointObservationAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".list_element_uuid "); //$NON-NLS-1$
				}else if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.TREE){
					fromSql.append(tableNames.get(AttributeTreeNode.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeTreeNode.class)+ "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeTreeNode.class)+ "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".uuid ="); //$NON-NLS-1$
					fromSql.append(tablePrefix(WaypointObservationAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".tree_node_uuid "); //$NON-NLS-1$
				}
				fromSql.append(" JOIN "); //$NON-NLS-1$
				fromSql.append(tableNames.get(Attribute.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(tablePrefix(Attribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(tablePrefix(WaypointObservationAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".attribute_uuid = "); //$NON-NLS-1$
				fromSql.append(tablePrefix(Attribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".uuid AND "); //$NON-NLS-1$
				fromSql.append(tablePrefix(Attribute.class) + "_" + itemcnt); //$NON-NLS-1$
				
				String p2 = addParameterValue(((AttributeGroupBy)gb).getAttributeKey());
				fromSql.append(".keyid =  " + p2); //$NON-NLS-1$
				
				
			}else if (gb instanceof WaypointSourceGroupBy){
				String categoryKey = "wpsrc_" + itemcnt; //$NON-NLS-1$
				groupByInnerSql.append(tablePrefix(Waypoint.class));
				groupByInnerSql.append(".source"); //$NON-NLS-1$
				groupByInnerSql.append(" as " + categoryKey); //$NON-NLS-1$
				
				groupBySql.append(categoryKey);
				
				if (!waypointAdd) {
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNamePrefix(Waypoint.class));
					fromSql.append(" on temp.wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
					waypointAdd = true;
				}
			}else{
				//throw new exception; should only be patrol group bys here for now
			}
			itemcnt++;
			
			groupBySql.append(","); //$NON-NLS-1$
			groupByInnerSql.append(","); //$NON-NLS-1$
		}
		
		if (groupBySql.length() > 0){
			groupBySql.deleteCharAt(groupBySql.length() - 1);
			groupByInnerSql.deleteCharAt(groupByInnerSql.length() - 1);
		}
	}
	
	
	/**
	 * Computes the header information for a given
	 * query.
	 * 
	 * @param query the summary query
	 * @param results the summary query results to update
	 * @param session hibernate session
	 * @throws Exception 
	 */
	public static void getHeaderInfo(ObservationSummaryQuery query, SummaryQueryResult results, Session session) throws Exception{
		
		// value headers
		ValuePart vp = query.getQueryDefinition().getValuePart();
		for (IValueItem item : vp.getValueItems()){
			SummaryHeader header = new SummaryHeader(ObservationValueItemLabelProvider.INSTANCE.getName(item, session),
					ObservationValueItemLabelProvider.INSTANCE.getFullName(item, session),
					item.asString(), true);
			
			results.addValueHeader(header);
		}
		
		DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
		
		for (IGroupBy item : query.getQueryDefinition().getRowGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(dFilter.getDateFilterOption());
			}
			IGroupByViewer<?> viewer = ObservationDropItemFactory.INSTANCE.findViewer(item);
			List<ListItem> items = viewer.getItems(session);
			SummaryHeader[] rowHeader = new SummaryHeader[items.size()];
			for (int i = 0; i < items.size(); i ++){
				ListItem it = items.get(i);
				if (it.getUuid() != null){
					rowHeader[i] = new SummaryHeader( it.getName(), it.getName(), item.getKeyPart(), UuidUtils.uuidToString( it.getUuid() ), false);
				}else{
					rowHeader[i] = new SummaryHeader( it.getName(), it.getName(), item.getKeyPart(), it.getKey(), false);
				}	
				
			}
			results.addRowHeader(rowHeader);
		}
		
		for (IGroupBy item : query.getQueryDefinition().getColumnGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(dFilter.getDateFilterOption());
			}
			IGroupByViewer<?> viewer = ObservationDropItemFactory.INSTANCE.findViewer(item);
			List<ListItem> items = viewer.getItems(session);
			SummaryHeader[] colHeader = new SummaryHeader[items.size()];
			for (int i = 0; i < items.size(); i ++){
				ListItem it = items.get(i);
				if (it.getUuid() != null){
					colHeader[i] = new SummaryHeader( it.getName(), it.getName(), item.getKeyPart(), UuidUtils.uuidToString( it.getUuid() ), false);
				}else{
					colHeader[i] = new SummaryHeader( it.getName(), it.getName(), item.getKeyPart(), it.getKey(), false);
				}
				
			}
			results.addColumnHeader(colHeader);
		}
		
	}
	
	
	@Override
	protected String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid, "); //$NON-NLS-1$
		
		if (includeObservations){
			sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as char for bit data),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as char for bit data)");	//wpob_uuid //$NON-NLS-1$
		}
		return sql.toString();
	}

	@Override
	protected String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	protected void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		super.buildTemporaryTableIndexes(c, tableName);
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_wp_uuid_idx on " +  tableName + "(wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	@Override
	protected ObservationQueryResultItem asQueryResultItem(ResultSet rs, Session session)
			throws SQLException {
		return null;
	}
	
	@Override
	public void dropTables(Connection c) throws SQLException{
		
	}
}
