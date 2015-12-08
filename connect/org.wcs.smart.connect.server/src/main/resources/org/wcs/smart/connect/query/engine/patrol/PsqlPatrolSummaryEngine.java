/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.patrol;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.ListItem;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.connect.query.engine.SummaryItemLabelProvider;
import org.wcs.smart.intelligence.query.IntelligencePatrolGroupBy;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.ext.IExtensionGroupBy;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.visitors.AreaFilterCollectorVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationGroupByVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationValueVisitor;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryResultKey;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.AreaFilter.AreaFilterGeometryType;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.EndHourGroupBy;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.StartHourGroupBy;
import org.wcs.smart.query.model.filter.date.YearDateGroupBy;
import org.wcs.smart.query.model.summary.AreaGroupBy;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.model.summary.ValuePart;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol summary query engine.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PsqlPatrolSummaryEngine extends AbstractQueryEngine{
	private final Logger logger = Logger.getLogger(PsqlPatrolSummaryEngine.class.getName());
	
	private SummaryQueryResult sumResults = null;
	private HashMap<String, HashMap<SummaryResultKey, Double>> cachedValueToResults;
	
	private String rateTrackTable;
	private String rateWaypointTable;
	private String valueTrackTable;
	private String valueWaypointTable;
	
	private boolean needsObservationValue = false;
	private boolean needsObservationRate = false;

	private DateFilter localDateFilter;
	private QueryFilter valueFilter;
	private QueryFilter rateFilter;
	private GroupByPart allGroupByParts;
	private ValuePart valuePart;
	
	private boolean hasAreaFilter = false;
	
	@Override
	public boolean canExecute(String querytype) {
		return PatrolSummaryQuery.KEY.equals(querytype);
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

		final PatrolSummaryQuery query = (PatrolSummaryQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		locale = (Locale)parameters.get(Locale.class.getName());
		
		SumQueryDefinition def = null;
		try{
			def = query.getQueryDefinition();
		}catch (Exception ex){
			throw new SQLException (ex);
		}

		//parse query bits that are needed for processing
		sumResults = new SummaryQueryResult();
		cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
		
		//create a date filter that caches the dates so the same
		//dates are used for all parts of the query;
		//otherwise different date filters will be computed
		//for different parts of the queries
		localDateFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));
		
		List<IGroupBy> all = new ArrayList<IGroupBy>();
		all.addAll(def.getColumnGroupByPart().getGroupBys());
		all.addAll(def.getRowGroupByPart().getGroupBys());
		allGroupByParts = new GroupByPart(all);
		
		valuePart = def.getValuePart();
			
		final SumQueryDefinition ldef = def;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				
				try {
					try{
						getHeaderInfo(query, sumResults, locale, session);
					}catch (Exception ex){
						throw new SQLException(ex);
					}
					HasObservationValueVisitor vv = new HasObservationValueVisitor();
					ldef.getValuePart().visit(vv);
					needsObservationValue = vv.hasCategory() || vv.hasAttribute();
					
					if(!needsObservationValue){
						HasObservationGroupByVisitor cv = new HasObservationGroupByVisitor();
						ldef.getColumnGroupByPart().visit(cv);
						needsObservationValue = cv.hasCategory()  || cv.hasAttribute();;
						if (!needsObservationValue){
							ldef.getRowGroupByPart().visit(cv);
							needsObservationValue = cv.hasCategory() || cv.hasAttribute();;
						}
					}
					needsObservationRate = needsObservationValue;
					valueFilter = new QueryFilter(EmptyFilter.INSTANCE);
					if (ldef.getValueFilter() != null){
						valueFilter = ldef.getValueFilter();
					}
					rateFilter = new QueryFilter(EmptyFilter.INSTANCE);
					if (ldef.getRateFilter() != null){
						rateFilter = ldef.getRateFilter();
					}
					
					//determine if has area filter
					AreaFilterCollectorVisitor hasAreaFilterVisitor = new AreaFilterCollectorVisitor();
					valueFilter.getFilter().accept(hasAreaFilterVisitor);
					rateFilter.getFilter().accept(hasAreaFilterVisitor);
					hasAreaFilter = hasAreaFilterVisitor.hasAreaFilter();
					
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
					ConservationAreaFilter cafilter = AbstractQueryEngine.parseConservationAreaFilter(query);
					
					HashMap<SummaryResultKey, Double> data = computeSummaryValues(c, session, 
							allGroupByParts, ldef.getValuePart(),
							cafilter);
					
					sumResults.setData(data);
					
				}catch (Exception ex){
					c.rollback();
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					if (ex instanceof SQLException) throw (SQLException)ex;
					throw new SQLException(ex);
				} finally {
					try{
						dropTemporaryTables(c);
						c.commit();
					}catch (Exception ex){
						c.rollback();
						logger.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
				c.commit();
			}
		});

		return sumResults ;
	}

	private void dropTemporaryTables(Connection c) throws SQLException{
		if (rateTrackTable != null){
			dropTable(c, rateTrackTable);
		}
		if (valueTrackTable != null){
			dropTable(c, valueTrackTable);
		}
		if (rateWaypointTable != null){
			dropTable(c, rateWaypointTable);
		}
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
			HashMap<SummaryResultKey, Double> data = computeValueItem(c, s, groupBy, it, caFilter, true) ; 
			if (data != null){
				results.putAll( data );	
			}
		}
		return results;
		
	}

	private String getFilterTable(boolean isValue, AreaFilter.AreaFilterGeometryType geomType, ConservationAreaFilter caFilter,
			Connection c) throws SQLException{
		if (isValue){
			if (!hasAreaFilter || geomType == AreaFilterGeometryType.TRACK){
				if (valueTrackTable == null){
					//create filter table
					valueTrackTable = createTempTableName();
					valueTrackTable = createFilterTable(true, geomType, valueTrackTable, caFilter, c);
				}
				return valueTrackTable;
			}else if (geomType == AreaFilterGeometryType.WAYPOINT){
				if (valueWaypointTable == null){
					//create filter table
					valueWaypointTable = createTempTableName();
					valueWaypointTable = createFilterTable(true, geomType, valueWaypointTable, caFilter, c);
				}
				return valueWaypointTable;
			}
		}else{
			if (!hasAreaFilter || geomType == AreaFilterGeometryType.TRACK){
				if (rateTrackTable == null){
					//create filter table
					rateTrackTable = createTempTableName();
					rateTrackTable = createFilterTable(false, geomType, rateTrackTable, caFilter, c);
				}
				return rateTrackTable;
			}else if (geomType == AreaFilterGeometryType.WAYPOINT){
				if (rateWaypointTable == null){
					//create filter table
					rateWaypointTable = createTempTableName();
					rateWaypointTable = createFilterTable(false, geomType, rateWaypointTable, caFilter, c);
				}
				return rateWaypointTable;
			}
		}
		//should never get here
		return null;
	}
	
	private String createFilterTable(boolean isValue, AreaFilter.AreaFilterGeometryType geomType, 
			String tableName, ConservationAreaFilter caFilter, Connection c) throws SQLException{

		QueryFilter qFilter = null;
		if (isValue){
			qFilter = valueFilter;
		}else{
			qFilter = rateFilter;
			
			//this may be the same as an existing value filter; don't regenerate table 
			if (qFilter.asString().equals(valueFilter.asString())){
				return getFilterTable(true, geomType, caFilter, c);
			}
		}
		
		AreaFilterCollectorVisitor areaVisitor = new AreaFilterCollectorVisitor();
		qFilter.getFilter().accept(areaVisitor);
		for (AreaFilter af : areaVisitor.getAreaFilters()){
			//update filter type
			af.changeGeometryType(geomType);
		}
		IFilterProcessor rfilterer = PsqlPatrolSummaryEngine.this.getFilterProcessor(
				qFilter.getFilterType(), tableName);
		try{
			rfilterer.processFilter(c, qFilter.getFilter(), localDateFilter, caFilter, needsObservationRate, false);
		}finally{
			rfilterer.dropTemporaryTables(c);
		}
		addCategoryHkey(tableName, allGroupByParts, valuePart, c);
		
		return tableName;
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
			boolean isValueItem) throws SQLException {
		
		String dataTable = null;
		if (it instanceof PatrolValueItem){
			dataTable = getFilterTable(isValueItem, AreaFilterGeometryType.TRACK, caFilter, c);
		}else if (it instanceof AttributeValueItem ||
				  it instanceof CategoryValueItem){
			dataTable = getFilterTable(isValueItem, AreaFilterGeometryType.WAYPOINT, caFilter, c);
		}else if (it instanceof CombinedValueItem){
			//don't do anything here - each value is dealt with separatly in the getCombindValue function
		}
			
		String cacheKey = it.asString() + "_" + groupBy.asString() + "_" + dataTable; //$NON-NLS-1$ //$NON-NLS-2$
		HashMap<SummaryResultKey, Double> results = cachedValueToResults.get(cacheKey); 
		if (results != null){
			return results;
		}
		if (it instanceof PatrolValueItem){
			results = (getPatrolSummaryValue(dataTable, c, s, groupBy, (PatrolValueItem)it, caFilter));
		}else if (it instanceof AttributeValueItem){
			results =  (getAttributeValue(dataTable, c, s, groupBy, (AttributeValueItem)it, caFilter));
		}else if (it instanceof CategoryValueItem){
			results = (getCategoryValue(dataTable, c, s, groupBy, (CategoryValueItem)it, caFilter));
		}else if (it instanceof CombinedValueItem){
			results = (getCombinedValue(c, s, groupBy, (CombinedValueItem)it, caFilter));
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
	private HashMap<SummaryResultKey, Double> getPatrolSummaryValue(
			String dataTableName,
			Connection c, Session s, 
			GroupByPart groupBy, 
			PatrolValueItem patrolItem, ConservationAreaFilter caFilter) throws SQLException{
		
		clearParameters();
		
		StringBuilder selectSql = new StringBuilder();
		StringBuilder fromSql = new StringBuilder();
		
		fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
		
		PatrolValueOption option = patrolItem.getPatrolValueOption();
		String tmp = getNameByClass(option.getOptionClass()) ;
		if (tmp != null){
			selectSql.append(tmp + " as uniqueid"); //$NON-NLS-1$
			selectSql.append(","); //$NON-NLS-1$
		}
		
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();
		
		StringBuilder valueSql = new StringBuilder();
		StringBuilder valueAggSql = new StringBuilder();

		
		createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, patrolItem, caFilter);
		
		boolean hasAreaGroupBy = false;
		for (IGroupBy groupby : groupBy.getGroupBys()){
			if (groupby instanceof AreaGroupBy){
				hasAreaGroupBy = true;
				break;
			}
		}
		
		valueSql.append(getFieldName(option, hasAreaGroupBy));
		valueAggSql.append(getAggFieldName(option, hasAreaGroupBy));

		if (option.getOptionClass().equals(Track.class) && !hasAreaGroupBy){
			fromSql.append(" join "); //$NON-NLS-1$
			fromSql.append(tableNamePrefix(Track.class));
			fromSql.append( " on temp.pld_uuid = "); //$NON-NLS-1$ 
			fromSql.append(tablePrefix(Track.class));
			fromSql.append(".patrol_leg_day_uuid " ); //$NON-NLS-1$
		}
		if (option == PatrolValueOption.NUM_MEMBERS ||
			option == PatrolValueOption.MAN_HOURS  ||
			option == PatrolValueOption.MAN_HOURS_TOTAL  || 
			option == PatrolValueOption.MAN_DAYS  ||
			option == PatrolValueOption.MAN_DAYS_TOTAL){
			fromSql.append(" left join "); //$NON-NLS-1$
			fromSql.append(tableNamePrefix(PatrolLegMember.class));
			fromSql.append(" on temp.pl_uuid = ");//$NON-NLS-1$
			fromSql.append( tablePrefix(PatrolLegMember.class));
			fromSql.append(".patrol_leg_uuid " ); //$NON-NLS-1$ 
		}
		if (option == PatrolValueOption.NUM_FIELDHOURS ||
			  option == PatrolValueOption.NUM_PATROLHOURS ||
			  option == PatrolValueOption.NUM_FIELDHOURS_TOTAL ||
			  option == PatrolValueOption.NUM_PATROLHOURS_TOTAL ||
			  option == PatrolValueOption.MAN_HOURS ||
			  option == PatrolValueOption.MAN_HOURS_TOTAL  ||
			  option == PatrolValueOption.MAN_DAYS  ||
			  option == PatrolValueOption.MAN_DAYS_TOTAL){
			fromSql.append(" left join "); //$NON-NLS-1$
			fromSql.append(tableNamePrefix(PatrolLegDay.class));
			fromSql.append( " on temp.pld_uuid = "); //$NON-NLS-1$
			fromSql.append(tablePrefix(PatrolLegDay.class));
			fromSql.append(".uuid "); //$NON-NLS-1$ 
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT "); //$NON-NLS-1$
		sql.append(groupBySql);
		if (groupBySql.length() > 0){
			sql.append(","); //$NON-NLS-1$
		}
		sql.append(valueAggSql);
		sql.append(" FROM ( SELECT distinct "); //$NON-NLS-1$
		sql.append(selectSql);
		sql.append(groupByInnerSql);
		if (groupByInnerSql.length() > 0){
			sql.append(","); //$NON-NLS-1$
		}
		sql.append(valueSql);
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(fromSql);
		sql.append(") foo"); //$NON-NLS-1$
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " ); //$NON-NLS-1$
			sql.append(groupBySql);
		}
		
		//do something here with sql
		logger.finest(sql.toString());
		ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();
		return createValueResults(rs, groupBy, patrolItem.asString());
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
		
		clearParameters();
		
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
			sql.append(".keyid = " + p1); //$NON-NLS-1$
			
			if (attributeItem.getCategoryKey() != null) {
				String p2 = addParameterValue(attributeItem.getCategoryKey());
				String p3 = addParameterValue(attributeItem.getCategoryKey().substring(0,attributeItem.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND ( foo.cat_hkey >= " + p2 + " and foo.cat_hkey < " + p3 + ") "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			String p2 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(".keyid = " + p1); //$NON-NLS-1$
			sql.append(" and "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			sql.append(".keyid = " + p2); //$NON-NLS-1$
			
			
			if (attributeItem.getCategoryKey() != null){
				p1 = addParameterValue(attributeItem.getCategoryKey());
				p2 = addParameterValue(attributeItem.getCategoryKey().substring(0,attributeItem.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND ( temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + ") "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			
			String p1 = addParameterValue(attributeItem.getItemKey());
			String p2 = addParameterValue(attributeItem.getItemKey().substring(0, attributeItem.getItemKey().length() -1 ) + "/"); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey >= " + p1); //$NON-NLS-1$
			sql.append(" and "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey < " + p2); //$NON-NLS-1$
			sql.append(") and "); //$NON-NLS-1$
		
			p1 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(tablePrefix(Attribute.class));
			sql.append(".keyid = " + p1); //$NON-NLS-1$
			
			if (attributeItem.getCategoryKey() != null){
				p1 = addParameterValue(attributeItem.getCategoryKey());
				p2 = addParameterValue(attributeItem.getCategoryKey().substring(0,attributeItem.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND ( temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + ") "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			sql.append(") as foo "); //$NON-NLS-1$
			if (groupBySql.length() > 0){
				sql.append(" GROUP BY " ); //$NON-NLS-1$
				sql.append(groupBySql);
			}
			
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
	 * Computes a value that is consists of performing divide by
	 * on two values.  It computes both values, then combines the results
	 * into a new value set.
	 * 
	 * @param c database connection
	 * @param s hibernate session 
	 * @param groupBy query group by options
	 * @param patrolItem patrol value to computer  
	 * @return query results
	 * @throws SQLException
	 */
	private HashMap<SummaryResultKey, Double> getCombinedValue(
			Connection c, Session s, 
			GroupByPart groupBy, 
			CombinedValueItem item, 
			ConservationAreaFilter caFilter) throws SQLException{
		
		HashMap<SummaryResultKey, Double> values1 = computeValueItem(c, s, groupBy, item.getPart1(), caFilter, true);
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		boolean filterValue2 = false;
		if (item.getPart2() instanceof PatrolValueItem && 
			PatrolQueryOptions.isGroupByFilterValueItem(  ((PatrolValueItem) item.getPart2()).getPatrolValueOption())){
				filterValue2 = true;
		}
		
		HashMap<SummaryResultKey, Double> values2 = null;
		if (!filterValue2){
			values2 = computeValueItem(c, s, new GroupByPart(new ArrayList<IGroupBy>()), item.getPart2(), caFilter, false);
			if (values2.values().size() != 1){
				throw new SQLException("Invalid filter computation");
			}
			
			Double denominator = values2.values().iterator().next();
			for (Iterator<Entry<SummaryResultKey, Double>> iterator = values1.entrySet().iterator(); iterator.hasNext();) {
				Entry<SummaryResultKey, Double> type = iterator.next();			
				SummaryResultKey key = new SummaryResultKey(type.getKey());
				key.setValueKey(item.asString());
				
				Double value = type.getValue();
				if (denominator == 0){
					value = Double.NaN;
				}else{
					value = value / denominator;
				}
				results.put(key, value);
			}
		}else{
			values2 = computeValueItem(c, s, groupBy, item.getPart2(), caFilter, false);
			
			
			for (Iterator<Entry<SummaryResultKey, Double>> iterator = values1.entrySet().iterator(); iterator.hasNext();) {
				Entry<SummaryResultKey, Double> type = iterator.next();
				
				SummaryResultKey key2 = new SummaryResultKey(type.getKey());
				key2.setValueKey(item.getPart2().asString());
				Double denominator = values2.get(key2);
				
				Double value = type.getValue();
				if (denominator == null || denominator == 0){
					value = Double.NaN;
				}else{
					value = value / denominator;
				}
				
				SummaryResultKey key = new SummaryResultKey(type.getKey());
				key.setValueKey(item.asString());
				results.put(key, value);
			}
		}

		
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
		
		clearParameters();
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
			String p1 = addParameterValue(categoryItem.getCategoryHKey());
			String p2 = addParameterValue(categoryItem.getCategoryHKey().substring(0, categoryItem.getCategoryHKey().length()-1) + "/"); //$NON-NLS-1$
			
			sql.append(" ("); //$NON-NLS-1$
			sql.append("cat_hkey >= " + p1); //$NON-NLS-1$
			sql.append(" and cat_hkey < " + p2); //$NON-NLS-1$
			sql.append(") "); //$NON-NLS-1$
		}
		sql.append(") foo"); //$NON-NLS-1$
		
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " ); //$NON-NLS-1$
			sql.append(groupBySql);
		}
		
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
		boolean trackAdd = false;
		
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
					String p1 = addParameterValue(agb.getAreaType().name());
					fromSql.append(" and "); //$NON-NLS-1$
					fromSql.append(areaPrefix + ".area_type = " + p1); //$NON-NLS-1$ 
					
					
					groupBySql.append(key);
				} else {
					//patrol value area group bys; use the track 
					AreaGroupBy agb = (AreaGroupBy) gb;
					String key = agb.getAreaType().name() + "_" + itemcnt; //$NON-NLS-1$s
					String areaPrefix = tablePrefix(Area.class)
							+ "_" + itemcnt; //$NON-NLS-1$
					groupByInnerSql
							.append(areaPrefix + ".keyid" + " as " + key); //$NON-NLS-1$ //$NON-NLS-2$
					areaGroupByPrefix.add(areaPrefix);
					
					if (!trackAdd) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(Track.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(Track.class));
						fromSql.append(" on temp.pld_uuid = " + tablePrefix(Track.class) + ".patrol_leg_day_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
						trackAdd = true;
					}
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNames.get(Area.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(areaPrefix);
					fromSql.append(" on smart.intersects("); //$NON-NLS-1$
					fromSql.append(tablePrefix(Track.class) + ".geometry, "); //$NON-NLS-1$
					fromSql.append(areaPrefix + ".geom"); //$NON-NLS-1$
					fromSql.append(")"); //$NON-NLS-1$
					if (caFilter != null ){
						fromSql.append(" and ");//$NON-NLS-1$
						fromSql.append( PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, areaPrefix, this));
					}
					fromSql.append(" and ");//$NON-NLS-1$
					String p1 = addParameterValue(agb.getAreaType().name());
					fromSql.append(areaPrefix + ".area_type = " + p1);//$NON-NLS-1$
					groupBySql.append(key);
				}
				
			}else if (gb instanceof PatrolGroupBy){
				PatrolQueryOption option = ((PatrolGroupBy) gb).getOption();
				String prefix = getTablePrefix(option);
				String name = getFieldName(option);
				if (prefix != null){
					groupByInnerSql.append(prefix + "."); //$NON-NLS-1$
				}
				groupByInnerSql.append(name + " as " + "gp_" + itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				groupBySql.append("gp_" + itemcnt); //$NON-NLS-1$
				
				if (option == PatrolQueryOption.EMPLOYEE){
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNames.get(PatrolLegMember.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(PatrolLegMember.class));
					fromSql.append(" on temp.pl_uuid = " + tablePrefix(PatrolLegMember.class) + ".patrol_leg_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				}else if (option.getType() == PatrolQueryOptionType.KEY){
					PatrolQueryOption op = option;
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNames.get(op.getSourceClass()));
					fromSql.append(" on temp."); //$NON-NLS-1$
					fromSql.append(getUuidFieldName(op));
					fromSql.append(" = "  ); //$NON-NLS-1$
					fromSql.append(tablePrefix(op.getSourceClass()));
					fromSql.append(".uuid"); //$NON-NLS-1$
				}
			}else if (gb instanceof DateGroupBy){
				IDateGroupBy op = ((DateGroupBy)gb).getOption();
				if (op.getClass().equals(DayDateGroupBy.class)){
					groupByInnerSql.append("pld_patrol_day as pld_patrol_day_" + itemcnt); //$NON-NLS-1$
					groupBySql.append("pld_patrol_day_" + itemcnt); //$NON-NLS-1$
				}else if (op.getClass().equals(MonthDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					
					groupByInnerSql.append("trim(cast(date_part('month', pld_patrol_day) as char(2))) "); //$NON-NLS-1$
					groupByInnerSql.append(" || '/' || cast(date_part('year',pld_patrol_day) as char(4)) "); //$NON-NLS-1$
					groupByInnerSql.append("as datePart_"); //$NON-NLS-1$
					groupByInnerSql.append( itemcnt); 
					
				}else if (op.getClass().equals(YearDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					
					groupByInnerSql.append("date_part('year',pld_patrol_day) as datePart_" + itemcnt); //$NON-NLS-1$
					
				}else if (op.getClass().equals(StartHourGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("pld_uuid,floor((date_part('hour', pld_start_time) * 60 + date_part('minute', pld_start_time)) / 30.0)* 30 as datePart_" + itemcnt); //$NON-NLS-1$
				}else if (op.getClass().equals(EndHourGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("pld_uuid,floor((date_part('hour', pld_end_time) * 60 + date_part('minute', pld_end_time)) / 30.0)* 30 as datePart_" + itemcnt); //$NON-NLS-1$
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
					
					fromSql.append(" and (temp.cat_hkey >= " + p1); //$NON-NLS-1$
					fromSql.append(" and "); //$NON-NLS-1$
					fromSql.append("temp.cat_hkey < " + p2); //$NON-NLS-1$
					fromSql.append(") "); //$NON-NLS-1$
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
				fromSql.append(".keyid = " + p1); //$NON-NLS-1$
				
			}else if (gb instanceof IExtensionGroupBy){
				if (gb instanceof IntelligencePatrolGroupBy){
					String intelPrefix = "intel_" + itemcnt; //$NON-NLS-1$
					groupBySql.append("i_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append(" CASE WHEN " + intelPrefix + ".patrol_uuid IS NULL THEN 'nm' else 'm' END as i_" + itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
					fromSql.append(" LEFT JOIN "); //$NON-NLS-1$
					fromSql.append(" smart.patrol_intelligence " + intelPrefix); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append("temp.p_uuid = " + intelPrefix + ".patrol_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
					
				}
//				PatrolContributionFinder.addGroupBySql((IExtensionGroupBy)gb, fromSql, 
//						groupBySql, groupByInnerSql, 
//						value, caFilter, itemcnt, this);
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
	 * Returns the column unique id based 
	 * on the given class
	 * @param clazz
	 * @return
	 */
	private String getNameByClass(Class<?> clazz){
		if (clazz.equals(Patrol.class)){
			return "p_uuid"; //$NON-NLS-1$
		}else if (clazz.equals(PatrolLeg.class)){
			return "pl_uuid"; //$NON-NLS-1$
		}else if (clazz.equals(PatrolLegDay.class) || clazz.equals(Track.class)){
			return "pld_uuid"; //$NON-NLS-1$
		}
		return null;
	}
	
	
	/**
	 * Converts a patrol value item to the column in the
	 * temporary filter results table with associated
	 * aggregation function.
	 * 
	 * @param item
	 * @return
	 */
	private String getAggFieldName(PatrolValueOption option, boolean hasAreaGroupBy){		
		switch(option){
		case NUM_PATROLS:
		case NUM_PATROLS_TOTAL:
			return "count(p_uuid)"; //$NON-NLS-1$
		case NUM_DAYS:
		case NUM_DAYS_TOTAL:
			return " count(pld_patrol_day)";  //$NON-NLS-1$
		case NUM_NIGHTS:
			return " count(pld_patrol_day) - count(distinct p_uuid) "; //$NON-NLS-1$
		case DISTANCE:
		case DISTANCE_TOTAL:
			return "sum(distance)"; //$NON-NLS-1$
		case NUM_PATROLHOURS:
		case NUM_PATROLHOURS_TOTAL:
			if (!hasAreaGroupBy) {
				return "sum((EXTRACT(EPOCH FROM pld_end_time - pld_start_time ) / ( 3600.0 )))"; //$NON-NLS-1$
			}else{
				return "sum(hours)"; //$NON-NLS-1$
			}
		case NUM_FIELDHOURS:
		case NUM_FIELDHOURS_TOTAL:
			if (!hasAreaGroupBy){			
				return "sum((EXTRACT(EPOCH FROM  pld_end_time - pld_start_time) / ( 3600.0 )) - (case when pld_rest_minutes is null then 0 else pld_rest_minutes end / 60.0))"; //$NON-NLS-1$
			}else{
				return "sum(hours)"; //$NON-NLS-1$
			}
		case NUM_MEMBERS:
			return "count(pl_member)"; //$NON-NLS-1$
		case MAN_HOURS:
		case MAN_HOURS_TOTAL:
			if (!hasAreaGroupBy){
				return "sum((EXTRACT(EPOCH FROM  pld_end_time - pld_start_time) / ( 3600.0 )) - (case when pld_rest_minutes is null then 0 else pld_rest_minutes end  / 60.0))"; //$NON-NLS-1$
			}else{
				return "sum(hours)"; //$NON-NLS-1$
			}
		case MAN_DAYS:
		case MAN_DAYS_TOTAL:
			return "count(pld_patrol_day) "; //$NON-NLS-1$
		}
		assert false;
		return ""; //$NON-NLS-1$
	}
	
	private String getFieldName(PatrolValueOption option, boolean hasAreaGroupBy){
		switch(option){
		case NUM_PATROLS:
		case NUM_PATROLS_TOTAL:
			return "p_uuid"; //$NON-NLS-1$
		case NUM_DAYS:
		case NUM_DAYS_TOTAL:
			return "pld_patrol_day"; //$NON-NLS-1$
		case NUM_NIGHTS:
			return "p_uuid, pld_patrol_day"; //$NON-NLS-1$
		case DISTANCE:
		case DISTANCE_TOTAL:
			if (!hasAreaGroupBy){
				return tablePrefix(Track.class) + ".distance as distance"; //$NON-NLS-1$
			}else{
				StringBuilder valueSql = new StringBuilder();
				StringBuilder append = new StringBuilder();
				valueSql.append("smart.distanceInMeter("); //$NON-NLS-1$
				for(String prefix : areaGroupByPrefix){
					valueSql.append("smart.intersection("); //$NON-NLS-1$
					valueSql.append(prefix);
					valueSql.append(".geom,"); //$NON-NLS-1$
					append.append(")"); //$NON-NLS-1$
				}
				valueSql.append(tablePrefix(Track.class));
				valueSql.append(".geometry"); //$NON-NLS-1$
				valueSql.append(append);
				valueSql.append(") / 1000.0 as distance "); //$NON-NLS-1$
				return valueSql.toString();
			}
		case NUM_PATROLHOURS:
		case NUM_FIELDHOURS:
		case NUM_PATROLHOURS_TOTAL:
		case NUM_FIELDHOURS_TOTAL:
			if (!hasAreaGroupBy){
				return tablePrefix(PatrolLegDay.class) + ".start_time as pld_start_time," + //$NON-NLS-1$
						tablePrefix(PatrolLegDay.class) + ".end_time as pld_end_time," + //$NON-NLS-1$
						tablePrefix(PatrolLegDay.class) + ".rest_minutes as pld_rest_minutes "; //$NON-NLS-1$
			}else{
				StringBuilder append = new StringBuilder();
				StringBuilder valueSql = new StringBuilder();
				valueSql.append("smart.computeHours("); //$NON-NLS-1$
				for (int i = 0; i < areaGroupByPrefix.size() - 1; i ++){
					valueSql.append("smart.intersection("); //$NON-NLS-1$
					valueSql.append(areaGroupByPrefix.get(i));
					valueSql.append(".geom,"); //$NON-NLS-1$
					append.append(")"); //$NON-NLS-1$
				}
				valueSql.append(areaGroupByPrefix.get(areaGroupByPrefix.size() - 1)+ ".geom"); //$NON-NLS-1$
				valueSql.append(append);
					
				valueSql.append(","); //$NON-NLS-1$
				valueSql.append(tablePrefix(Track.class));
				valueSql.append(".geometry) as hours "); //$NON-NLS-1$
				return valueSql.toString();
			}
		case NUM_MEMBERS:
			return tablePrefix(PatrolLegMember.class) + ".employee_uuid as pl_member"; //$NON-NLS-1$
		case MAN_HOURS:
		case MAN_HOURS_TOTAL:
			if (!hasAreaGroupBy){
				return tablePrefix(PatrolLegDay.class) + ".start_time as pld_start_time, " + //$NON-NLS-1$
						tablePrefix(PatrolLegDay.class) + ".end_time as pld_end_time, " + //$NON-NLS-1$
						tablePrefix(PatrolLegDay.class) + ".rest_minutes as pld_rest_minutes," + //$NON-NLS-1$
						tablePrefix(PatrolLegMember.class) + ".employee_uuid as pl_member "; //$NON-NLS-1$
			}else{
				StringBuilder valueSql = new StringBuilder();
				StringBuilder append = new StringBuilder();
				valueSql.append("smart.computeHours("); //$NON-NLS-1$
				for (int i = 0; i < areaGroupByPrefix.size() - 1; i ++){
					valueSql.append("smart.intersection("); //$NON-NLS-1$
					valueSql.append(areaGroupByPrefix.get(i));
					valueSql.append(".geom,");//$NON-NLS-1$
					append.append(")"); //$NON-NLS-1$
				}
				valueSql.append(areaGroupByPrefix.get(areaGroupByPrefix.size() - 1)+ ".geom"); //$NON-NLS-1$
				valueSql.append(append);
					
				valueSql.append(","); //$NON-NLS-1$
				valueSql.append(tablePrefix(Track.class));
				valueSql.append(".geometry)"); //$NON-NLS-1$
				valueSql.append(" as hours, "); //$NON-NLS-1$
				valueSql.append(tablePrefix(PatrolLegMember.class));
				valueSql.append(".employee_uuid as pl_member"); //$NON-NLS-1$
				return valueSql.toString();
			}
		case MAN_DAYS:
		case MAN_DAYS_TOTAL:
			return "pld_patrol_day, " + //$NON-NLS-1$
			tablePrefix(PatrolLegMember.class) + ".employee_uuid as pl_member"; //$NON-NLS-1$
		}
		//should not get here
		return null;
	}
	
	private String getUuidFieldName(PatrolQueryOption gb){
		switch(gb){
		case TEAM_KEY:
			return "p_team_uuid"; //$NON-NLS-1$
		case MANDATE_KEY:
			return "p_mandate_uuid"; //$NON-NLS-1$
		case PATROL_TRANSPORT_TYPE_KEY:
			return "pl_transport_uuid"; //$NON-NLS-1$
		default:
			return null;
		}
	}
	
	/**
	 * Returns the patrol group by field from 
	 * the temproary results table that contains
	 * the given patrol group by item.
	 * 
	 * @param gb
	 * @return
	 */
	private String getFieldName(PatrolQueryOption option){
		switch(option){
		case ID:
			return "p_id"; //$NON-NLS-1$
		case STATION:
			return "p_station_uuid"; //$NON-NLS-1$
		case TEAM:
			return "p_team_uuid"; //$NON-NLS-1$
		case MANDATE:
			return "p_mandate_uuid"; //$NON-NLS-1$
		case PATROL_TYPE:
			return "p_type"; //$NON-NLS-1$
		case PATROL_TRANSPORT_TYPE:
			return "pl_transport_uuid"; //$NON-NLS-1$
		case LEADER:
			return "plm_leader"; //$NON-NLS-1$
		case EMPLOYEE:
			return "employee_uuid"; //$NON-NLS-1$
		case CONSERVATION_AREA:
			return "p_ca_uuid"; //$NON-NLS-1$
		case TEAM_KEY:
			return tablePrefix.get(Team.class) + ".keyid"; //$NON-NLS-1$
		case MANDATE_KEY:
			return tablePrefix.get(PatrolMandate.class) + ".keyid"; //$NON-NLS-1$
		case PATROL_TRANSPORT_TYPE_KEY:
			return tablePrefix.get(PatrolTransportType.class) + ".keyid"; //$NON-NLS-1$
		default:
			assert false;
			return ""; //$NON-NLS-1$
		}
	}
	
	/**
	 * Table group item prefix from the query.
	 * 
	 * @param gb
	 * @return
	 */
	private String getTablePrefix(PatrolQueryOption option){
		if (option == PatrolQueryOption.EMPLOYEE){
			return tablePrefix(PatrolLegMember.class);
		}
		return null;
	}
	
	
	/**
	 * Computes the header information for a given
	 * query.
	 * 
	 * @param query the summary query
	 * @param results the summary query results to update
	 * @param session hibernate session
	 */
	public static void getHeaderInfo(PatrolSummaryQuery query, 
			SummaryQueryResult results,
			Locale l,
			Session session) throws Exception{
		
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
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".station_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".team_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".patrol_type, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".is_armed, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".transport_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".start_time, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".end_time, "); //$NON-NLS-1$
		
		if (includeObservations){
			sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as uuid),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as uuid),");	//wpob_uuid //$NON-NLS-1$
		}
		sql.append(tablePrefix(PatrolLegMember.class) + "_leader.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegMember.class) + "_pilot.employee_uuid "); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid UUID,"); //$NON-NLS-1$
		sql.append("p_uuid UUID,"); //$NON-NLS-1$
		sql.append("p_id varchar(32),"); //$NON-NLS-1$
		sql.append("p_station_uuid UUID,"); //$NON-NLS-1$
		sql.append("p_team_uuid UUID,"); //$NON-NLS-1$
		sql.append("p_objective varchar(8192),"); //$NON-NLS-1$
		sql.append("p_mandate_uuid  UUID,"); //$NON-NLS-1$
		sql.append("p_type varchar(6),"); //$NON-NLS-1$
		sql.append("p_is_armed boolean,"); //$NON-NLS-1$
		sql.append("p_start_date date,"); //$NON-NLS-1$
		sql.append("p_end_date date,"); //$NON-NLS-1$
		sql.append("pl_uuid UUID,"); //$NON-NLS-1$
		sql.append("pl_id varchar(50),"); //$NON-NLS-1$
		sql.append("pl_transport_uuid UUID,"); //$NON-NLS-1$
		sql.append("pl_start_date date,"); //$NON-NLS-1$
		sql.append("pl_end_date date,"); //$NON-NLS-1$
		sql.append("pld_uuid UUID,"); //$NON-NLS-1$
		sql.append("pld_patrol_day date,"); //$NON-NLS-1$
		sql.append("pld_start_time time,"); //$NON-NLS-1$
		sql.append("pld_end_time time,"); //$NON-NLS-1$
		sql.append("wp_uuid UUID,"); //$NON-NLS-1$
		sql.append("ob_uuid UUID,"); //$NON-NLS-1$
		sql.append("plm_leader UUID,"); //$NON-NLS-1$
		sql.append("plm_pilot UUID"); //$NON-NLS-1$
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


	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}

	@Override
	public void cleanUp(Session session) {
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				dropTemporaryTables(c);
				
			}});
	}

	protected IFilterProcessor getFilterProcessor(FilterType filterType,
			String queryDataTable) {
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new PatrolFilterProcessor(queryDataTable, this);
		}else{
			return new PatrolWaypointFilterProcessor(queryDataTable, this);
		}
	}
}
