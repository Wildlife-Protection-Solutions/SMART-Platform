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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.ISummaryEngine;
import org.wcs.smart.connect.query.engine.ListItem;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.connect.query.engine.SummaryItemLabelProvider;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.engine.visitor.HasObservationFilterVisitor;
import org.wcs.smart.entity.query.engine.visitor.HasObservationGroupByVisitor;
import org.wcs.smart.entity.query.model.EntitySummaryQuery;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeGroupBy;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.query.model.filter.WaypointSourceGroupBy;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.visitors.HasObservationValueVisitor;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryResultKey;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
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
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.model.summary.ValuePart;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for executing summary
 * queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PsqlEntitySummaryEngine extends AbstractQueryEngine implements ISummaryEngine{

	private final Logger logger = Logger.getLogger(PsqlEntitySummaryEngine.class.getName());
	
	private SummaryQueryResult sumResults = null;
	private HashMap<String, HashMap<SummaryResultKey, Double>> cachedValueToResults;
	
	private String valueWaypointTable;
	
	
	@Override
	public boolean canExecute(String querytype) {
		return EntitySummaryQuery.KEY.equals(querytype);
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
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{
		
		final EntitySummaryQuery query = (EntitySummaryQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		locale = (Locale)parameters.get(Locale.class.getName());

		SumQueryDefinition def = null;
		try{
			def = query.getQueryDefinition();
		}catch (Exception ex){
			throw new SQLException (ex);
		}

		sumResults = new SummaryQueryResult();
		cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
		
		//create a date filter that caches the dates so the same
		// dates are used for all parts of the query;
		// otherwise different date filters will be computed
		// for different parts of the queries
		final DateFilter localDateFilter = new DateFilter(query.getDateFilter()
				.getDateFieldOption(), new CachingDateFilter(query
				.getDateFilter().getDateFilterOption()));

		List<IGroupBy> all = new ArrayList<IGroupBy>();
		all.addAll(def.getColumnGroupByPart().getGroupBys());
		all.addAll(def.getRowGroupByPart().getGroupBys());
		final GroupByPart allGroupByParts = new GroupByPart(all);

		final SumQueryDefinition ldef = def;
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				
				try {
					ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
					
					try{
						getHeaderInfo(query, sumResults, locale, session);
					}catch (Exception ex){
						throw new SQLException(ex);
					}
					boolean needsObservationValue = false;
					
					HasObservationValueVisitor vv = new HasObservationValueVisitor();
					ldef.getValuePart().visit(vv);
					needsObservationValue = vv.hasCategory() || vv.hasAttribute();
					
					if(!needsObservationValue){
						HasObservationGroupByVisitor cv = new HasObservationGroupByVisitor();
						ldef.getColumnGroupByPart().visit(cv);
						needsObservationValue = cv.hasCategory() || cv.hasAttribute();
						if (!needsObservationValue){
							ldef.getRowGroupByPart().visit(cv);
							needsObservationValue = cv.hasCategory() || cv.hasAttribute();
						}
					}
					
					QueryFilter valueFilter = new QueryFilter(EmptyFilter.INSTANCE);
					if (ldef.getValueFilter() != null){
						valueFilter = ldef.getValueFilter();
					}
					
					if (!needsObservationValue){
						HasObservationFilterVisitor visitor = new HasObservationFilterVisitor();
						visitor.visit(valueFilter.getFilter());
						if (visitor.hasAttributeFilter() || visitor.hasCategoryFilter()){
							needsObservationValue = true;
						}
					}
					valueWaypointTable = createTempTableName();
					IFilterProcessor filterer = getFilterProcessor(valueFilter.getFilterType(), valueWaypointTable);
					try{
						filterer.processFilter(c, valueFilter.getFilter(), localDateFilter, caFilter, needsObservationValue, false);
					}finally{
						filterer.dropTemporaryTables(c);
					}
					
					addCategoryHkey(valueWaypointTable, allGroupByParts, query.getQueryDefinition().getValuePart(), c);
					
					HashMap<SummaryResultKey, Double> data = computeSummaryValues(c, session, 
							allGroupByParts, ldef.getValuePart(),
							caFilter);
					
					if (data == null){
						return ;
					}
					sumResults.setData(data);
					
					
				}catch(Exception ex){
					throw new SQLException(ex);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
				}
				c.commit();
			}
		});

		return sumResults ;
	}

	private void dropTemporaryTables(Connection c) throws SQLException{
		if (valueWaypointTable != null){
			dropTable(c, valueWaypointTable);
		}
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
			logger.finest(sql.toString());
			
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
			
			logger.finest(sql.toString());
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
			ValuePart values, ConservationAreaFilter caFilter) throws SQLException{
	
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		for (IValueItem it : values.getValueItems()){
			HashMap<SummaryResultKey, Double> data =computeValueItem(c, s, groupBy, it, caFilter, valueWaypointTable) ; 
			if (data != null){
				results.putAll( data );	
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
			ConservationAreaFilter caFilter,
			String dataTable) throws SQLException {
		
		clearParameters();
		String cacheKey = it.asString() + "_" + groupBy.asString() + "_" + dataTable; //$NON-NLS-1$ //$NON-NLS-2$
		HashMap<SummaryResultKey, Double> results = cachedValueToResults.get(cacheKey); 
		if (results != null){
			return results;
		}
		if (it instanceof AttributeValueItem){
			results =  (getAttributeValue(dataTable, c, s, groupBy, (AttributeValueItem)it, caFilter));
		}else if (it instanceof CategoryValueItem){
			results = (getCategoryValue(dataTable, c, s, groupBy, (CategoryValueItem)it, caFilter));
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
			AttributeValueItem attributeItem, ConservationAreaFilter caFilter) throws SQLException{
		
		if (attributeItem.getAttributeType() == AttributeType.NUMERIC) {
			StringBuilder fromSql = new StringBuilder();

			fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
			StringBuilder groupBySql = new StringBuilder();
			StringBuilder groupByInnerSql = new StringBuilder();

			createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql,
					attributeItem, caFilter);

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
			String p1 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (attributeItem.getCategoryKey() != null) {
				p1 = addParameterValue(attributeItem.getCategoryKey() + "%"); //$NON-NLS-1$
				sql.append(" AND ( foo.cat_hkey like " + p1 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ 
			}
			if (groupBySql.length() > 0) {
				sql.append(" GROUP BY "); //$NON-NLS-1$
				sql.append(groupBySql);
			}

			// do something here with sql
			logger.finest(sql.toString());
			ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();
			return createValueResults(rs, groupBy, attributeItem.asString());
		} else if (attributeItem.getAttributeType() == AttributeType.LIST) {
			StringBuilder fromSql = new StringBuilder();
			
			fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
			StringBuilder groupBySql = new StringBuilder();
			StringBuilder groupByInnerSql = new StringBuilder();

			createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, attributeItem, caFilter);
			
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
			String p1 = addParameterValue(attributeItem.getItemKey());
			sql.append(".keyid = " + p1); //$NON-NLS-1$
			sql.append(" and "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			p1 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			 
			if (attributeItem.getCategoryKey() != null){	
				p1 = addParameterValue(attributeItem.getCategoryKey() + "%"); //$NON-NLS-1$
				sql.append("AND ( temp.cat_hkey like " + p1 + ") "); //$NON-NLS-1$ //$NON-NLS-2$ 

			}
			sql.append(") as foo "); //$NON-NLS-1$
			if (groupBySql.length() > 0){
				sql.append(" GROUP BY " ); //$NON-NLS-1$
				sql.append(groupBySql);
			}
			
			//do something here with sql
			logger.finest(sql.toString());
			ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();

			return createValueResults(rs, groupBy, attributeItem.asString());
		} else if (attributeItem.getAttributeType() == AttributeType.TREE) {
			StringBuilder fromSql = new StringBuilder();
			
			fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
			StringBuilder groupBySql = new StringBuilder();
			StringBuilder groupByInnerSql = new StringBuilder();

			createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, attributeItem, caFilter);
			
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
			
			String p1 = addParameterValue(attributeItem.getItemKey()+ "%"); //$NON-NLS-1$
			
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey like " + p1); //$NON-NLS-1$
			sql.append(") and "); //$NON-NLS-1$
		
			sql.append(tablePrefix(Attribute.class));
			p1 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			if (attributeItem.getCategoryKey() != null){
				p1 = addParameterValue(attributeItem.getCategoryKey()+ "%"); //$NON-NLS-1$
				sql.append("AND ( temp.cat_hkey like " + p1 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ 
			}
			sql.append(") as foo "); //$NON-NLS-1$
			if (groupBySql.length() > 0){
				sql.append(" GROUP BY " ); //$NON-NLS-1$
				sql.append(groupBySql);
			}
			
			//do something here with sql
			logger.finest(sql.toString());
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
						key += UuidUtils.uuidToString((UUID)rs.getObject(rsindex++));
						break;
					case DATE:
						key += rs.getDate(rsindex++).toString();
						break;
					case KEY:
						key += rs.getString(rsindex++);
						break;
					case TIME:
						int mins = rs.getInt(rsindex++);
						int hrs = mins / 60;
						mins = mins % 60;
						key += String.format("%d:%02d", hrs, mins); //$NON-NLS-1$
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
			CategoryValueItem categoryItem, ConservationAreaFilter caFilter) throws SQLException{
		
		StringBuilder fromSql = new StringBuilder();
		
		fromSql.append(dataTable + " temp "); //$NON-NLS-1$
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();

		createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, categoryItem, caFilter);
		
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
			String p1 = addParameterValue(categoryItem.getCategoryHKey()+ "%"); //$NON-NLS-1$
			sql.append(" ( cat_hkey like " + p1 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ 
		}
		sql.append(") foo"); //$NON-NLS-1$
		
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " ); //$NON-NLS-1$
			sql.append(groupBySql);
		}
		
		//do something here with sql
		logger.finest(sql.toString());
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
			StringBuilder groupByInnerSql, IValueItem value, ConservationAreaFilter caFilter) throws SQLException{
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
					if (caFilter != null){
						fromSql.append(" and "); //$NON-NLS-1$
						fromSql.append( PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, areaPrefix, this));
					}
					fromSql.append(" and "); //$NON-NLS-1$
					String p1 = addParameterValue(agb.getAreaType().name());
					fromSql.append(areaPrefix + ".area_type = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
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
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime as wp_date_time_" + itemcnt); //$NON-NLS-1$
					groupBySql.append("wp_date_time_" + itemcnt); //$NON-NLS-1$
				}else if (op.getClass().equals(MonthDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					
					groupByInnerSql.append("trim(cast(date_part('month', "); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as char(2))) || '/' || cast(date_part('year',"); //$NON-NLS-1$
					groupByInnerSql.append(tablePrefix(Waypoint.class));
					groupByInnerSql.append(".datetime) as char(4)) as datePart_"); //$NON-NLS-1$
					groupByInnerSql.append( itemcnt);

				}else if (op.getClass().equals(YearDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("date_part('year',"); //$NON-NLS-1$
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
					String p1 = addParameterValue(catkey + "%"); //$NON-NLS-1$
					fromSql.append(" and (temp.cat_hkey like " + p1 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ 
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
				String p1 = addParameterValue(((AttributeGroupBy)gb).getAttributeKey());
				fromSql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			}else if (gb instanceof EntityAttributeGroupBy){
				EntityAttributeGroupBy egb = (EntityAttributeGroupBy)gb;
				
				groupBySql.append("attribute_" + itemcnt); //$NON-NLS-1$
				if (egb.getAttributeType() == AttributeType.LIST){
					groupByInnerSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append(".keyid as  attribute_" + itemcnt); //$NON-NLS-1$
				}else if (egb.getAttributeType() == AttributeType.TREE){
					groupByInnerSql.append("smart.trimHkeyToLevel("); //$NON-NLS-1$
					groupByInnerSql.append(((EntityAttributeGroupBy)gb).getTreeLevel().intValue() + ","); //$NON-NLS-1$
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
			
				fromSql.append(" join (SELECT "); //$NON-NLS-1$
				fromSql.append(tablePrefix(Entity.class) + ".attribute_list_item_uuid as attribute_list_item_uuid, "); //$NON-NLS-1$
				fromSql.append(tablePrefix(EntityAttributeValue.class) + ".list_element_uuid as list_element_uuid, ");//$NON-NLS-1$
				fromSql.append(tablePrefix(EntityAttributeValue.class) + ".tree_node_uuid as tree_node_uuid "); //$NON-NLS-1$
				fromSql.append(" FROM "); //$NON-NLS-1$
				fromSql.append(tableNamePrefix(Entity.class));
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableNamePrefix(EntityAttributeValue.class));
				fromSql.append(" ON "); //$NON-NLS-1$
				fromSql.append(tablePrefix(Entity.class) + ".uuid="); //$NON-NLS-1$
				fromSql.append(tablePrefix(EntityAttributeValue.class) + ".entity_uuid"); //$NON-NLS-1$
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableNamePrefix(EntityAttribute.class));
				fromSql.append(" ON "); //$NON-NLS-1$
				fromSql.append(tablePrefix(EntityAttribute.class) + ".uuid="); //$NON-NLS-1$
				fromSql.append(tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid"); //$NON-NLS-1$
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableNamePrefix(EntityType.class));
				fromSql.append(" ON "); //$NON-NLS-1$
				fromSql.append(tablePrefix(EntityType.class) + ".uuid="); //$NON-NLS-1$
				fromSql.append(tablePrefix(Entity.class) + ".entity_type_uuid"); //$NON-NLS-1$
				fromSql.append(" WHERE "); //$NON-NLS-1$
				fromSql.append(tablePrefix(EntityType.class) + ".keyid = '"+ egb.getEntityKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				fromSql.append(" AND "); //$NON-NLS-1$
				fromSql.append(tablePrefix(EntityAttribute.class) + ".keyid = '"+ egb.getEntityAttributeKey() + "') foo_" + itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				fromSql.append(" ON foo_" + itemcnt + ".attribute_list_item_uuid = ");  //$NON-NLS-1$//$NON-NLS-2$
				fromSql.append(tablePrefix(WaypointObservationAttribute.class) + "_" + itemcnt + ".list_element_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				
				fromSql.append(" JOIN "); //$NON-NLS-1$
				if (((EntityAttributeGroupBy)gb).getAttributeType() == AttributeType.LIST){
					fromSql.append(tableNames.get(AttributeListItem.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".uuid ="); //$NON-NLS-1$
					fromSql.append("foo_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".list_element_uuid "); //$NON-NLS-1$
				}else if (((EntityAttributeGroupBy)gb).getAttributeType() == AttributeType.TREE){
					fromSql.append(tableNames.get(AttributeTreeNode.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeTreeNode.class)+ "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeTreeNode.class)+ "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".uuid ="); //$NON-NLS-1$
					fromSql.append("foo_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".tree_node_uuid "); //$NON-NLS-1$
				}
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
				//throw new exception; 
				throw new RuntimeException(MessageFormat.format(Messages.getString("PsqlEntitySummaryEngine.InvalidGroupByOp", locale), new Object[]{gb.getClass().getName()})); //$NON-NLS-1$
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
	
	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid, "); //$NON-NLS-1$
		
		if (includeObservations){
			sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as UUID),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as UUID)");	//wpob_uuid //$NON-NLS-1$
		}
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid UUID,"); //$NON-NLS-1$
		sql.append("wp_uuid UUID,"); //$NON-NLS-1$
		sql.append("ob_uuid UUID"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		super.buildTemporaryTableIndexes(c, tableName);
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_wp_uuid_idx on " +  tableName + "(wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	
	/**
	 * Computes the header information for a given
	 * query.
	 * 
	 * @param query the summary query
	 * @param results the summary query results to update
	 * @param session hibernate session
	 */
	public void getHeaderInfo(SummaryQuery query, SummaryQueryResult results,Locale l, Session session) throws Exception{
		ConservationAreaFilter cafilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SummaryItemLabelProvider summary = new SummaryItemLabelProvider(l, session, cafilter); 

		// value headers
		ValuePart vp = query.getQueryDefinition().getValuePart();
		for (IValueItem item : vp.getValueItems()){
			SummaryHeader header = new SummaryHeader(
					summary.getName(item),
					summary.getFullName(item),
					item.asString(), true);
			results.addValueHeader(header);
		}
		
		DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
		
		for (IGroupBy item : query.getQueryDefinition().getRowGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(dFilter.getDateFilterOption());
			}
			List<ListItem> items = summary.getNames(item);
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
			List<ListItem> items = summary.getNames(item);
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
	public void cleanUp(Session session) {
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}

	protected IFilterProcessor getFilterProcessor(FilterType filterType,
			String queryDataTable) {
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new PsqlEntityFilterProcessor(queryDataTable, this);
		}else{
			return new PsqlEntityWaypointFilterProcessor(queryDataTable, this);
		}
	}
}
