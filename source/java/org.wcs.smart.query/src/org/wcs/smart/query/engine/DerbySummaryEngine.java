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
package org.wcs.smart.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.model.SummaryHeader;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.model.SummaryQueryResult;
import org.wcs.smart.query.model.SummaryResultKey;
import org.wcs.smart.query.parser.PatrolQueryOptions.DateGroupByOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.summary.AttributeGroupBy;
import org.wcs.smart.query.parser.internal.summary.AttributeValueItem;
import org.wcs.smart.query.parser.internal.summary.CategoryGroupBy;
import org.wcs.smart.query.parser.internal.summary.CategoryValueItem;
import org.wcs.smart.query.parser.internal.summary.CombinedValueItem;
import org.wcs.smart.query.parser.internal.summary.DateGroupBy;
import org.wcs.smart.query.parser.internal.summary.GroupByPart;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.parser.internal.summary.ValuePart;
import org.wcs.smart.util.SmartUtils;

/**
 * Query engine for executing summary
 * queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbySummaryEngine extends DerbyQueryEngine2{

	private SummaryQueryResult sumResults = null;
	HashMap<String, HashMap<SummaryResultKey, Double>> cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
	
	
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
	public SummaryQueryResult executeQuery(final SummaryQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {

		queryTempTable = QUERY_TEMP_TABLE_PREFIX + System.nanoTime();
		observationTempTable = QUERY_OB_TEMP_TABLE_PREFIX + System.nanoTime();

		sumResults = new SummaryQueryResult();
		cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask("Running Query.", query.getQueryDefinition().getValuePart().getValueItems().size() + 5);

				try {
					monitor.subTask("Loading header names");
					getHeaderInfo(query, sumResults, session);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Creating observation table");
					IFilter qFilter = query.getQueryDefinition().getQueryFilter();
					if (qFilter == null){
						qFilter = IFilter.EMPTY_FILTER;
					}
					if (qFilter != IFilter.EMPTY_FILTER && qFilter.hasAttributeFilter()) {
						createObservationTable(c, query.getQueryDefinition().getQueryFilter(), query.getDateFilter(), query.getConservationAreaFilterAsFilter());
					}
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}

					monitor.subTask("Creating temporary table");
					createTemporaryTable(c);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Populating results table");
					List<IGroupBy> all = new ArrayList<IGroupBy>();
					all.addAll(query.getQueryDefinition().getColumnGroupByPart().getGroupBys());
					all.addAll(query.getQueryDefinition().getRowGroupByPart().getGroupBys());
					GroupByPart allGroupBy = new GroupByPart(all);
					
					//TODO: review this needsObservation
					//i think you only need the observations if 
					//computing an observation value; if only computing patrol
					//values you shouldn't need observations
					boolean needsObservation = false;
					for (IValueItem it : query.getQueryDefinition().getValuePart().getValueItems()){
						if (it.hasAttribute() || it.hasCategory()){
							needsObservation = true;
							break;
						}
					}
					if(!needsObservation){
						for (IGroupBy gp : allGroupBy.getGroupBys()){
							if (gp.hasCategory() || gp.hasAttribute()){
								needsObservation = true;
								break;
							}
						}
					}
					
					populateTemporaryTable(qFilter, query.getDateFilter(), query.getConservationAreaFilterAsFilter(), false,c, needsObservation);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Processing Values");
					
					
					addCategoryHkey(allGroupBy, query.getQueryDefinition().getValuePart(), c);
					
					sumResults.setData(
							computeSummaryValues(c, session, 
									allGroupBy, 
									query.getQueryDefinition().getValuePart(),
									monitor));
					
					monitor.worked(1);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
					monitor.done();
				}
			}
		});

		return sumResults ;
	}

	
	private void addCategoryHkey(GroupByPart groupByPart, ValuePart values, Connection c) throws SQLException{
		boolean add = false;
		for (IGroupBy groupBy : groupByPart.getGroupBys()){
			if (groupBy.hasCategory() ){
				add = true;
				break;
			}
		}
		if (!add){
			for (IValueItem item : values.getValueItems()){
				if (item.hasCategory()){
					add = true;
					break;
				}
			}
		}
		
		if (add){
			StringBuilder sql = new StringBuilder();
			sql.append("ALTER TABLE ");
			sql.append(queryTempTable);
			sql.append(" ADD column cat_hkey varchar(32672)");
			QueryPlugIn.log(sql.toString(), null);
			
			c.createStatement().execute(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE ");
			sql.append(queryTempTable);
			sql.append(" SET cat_hkey = ");
			sql.append("(SELECT ");
			sql.append(tablePrefix.get(Category.class));
			sql.append(".hkey FROM ");
			sql.append(tableNames.get(Category.class));
			sql.append(" ");
			sql.append(tablePrefix.get(Category.class));
			sql.append(", ");
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append(" ");
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(" WHERE ");
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(".uuid = ");
			sql.append(queryTempTable);
			sql.append(".ob_uuid AND ");
			sql.append(tablePrefix.get(Category.class));
			sql.append(".uuid = ");
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(".category_uuid )");
			
			QueryPlugIn.log(sql.toString(), null);
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
			ValuePart values,
			IProgressMonitor monitor) throws SQLException{
	
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		for (IValueItem it : values.getValueItems()){
			monitor.subTask("Processing Value: " + it.asString());
			HashMap<SummaryResultKey, Double> data =computeValueItem(c, s, groupBy, it) ; 
			if (data != null){
				results.putAll( data );	
			}
			
			monitor.worked(1);
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
	private HashMap<SummaryResultKey, Double> computeValueItem(Connection c, 
			Session s,
			GroupByPart groupBy, 
			IValueItem it) throws SQLException {
		
		HashMap<SummaryResultKey, Double> results = cachedValueToResults.get(it.asString());
		if (results != null){
			return results;
		}
		if (it instanceof PatrolValueItem){
			results = (getPatrolSummaryValue(c, s, groupBy, (PatrolValueItem)it));
		}else if (it instanceof AttributeValueItem){
			results =  (getAttributeValue(c, s, groupBy, (AttributeValueItem)it));
		}else if (it instanceof CategoryValueItem){
			results = (getCategoryValue(c, s, groupBy, (CategoryValueItem)it));
		}else if (it instanceof CombinedValueItem){
			results = (getCombinedValue(c, s, groupBy, (CombinedValueItem)it));
		}
		if (results != null){
			cachedValueToResults.put(it.asString(), results);
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
			Connection c, Session s, 
			GroupByPart groupBy, 
			PatrolValueItem patrolItem) throws SQLException{
		
		StringBuilder selectSql = new StringBuilder();
		StringBuilder fromSql = new StringBuilder();
		
		fromSql.append(queryTempTable + " a ");
		
		String tmp = getNameByClass(patrolItem.getOption().getOptionClass()) ;
		if (tmp != null){
			selectSql.append(tmp + " as uniqueid");
			selectSql.append(",");
		}
		
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();
		
		StringBuilder valueSql = new StringBuilder();
		StringBuilder valueAggSql = new StringBuilder();

		
		createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql);
		
		
		valueSql.append(getFieldName(patrolItem));
		valueAggSql.append(getAggFieldName(patrolItem));
		
		if (patrolItem.getOption().getOptionClass().equals(Track.class)){
			fromSql.append(" left join ");
			fromSql.append(tableNames.get(Track.class));
			fromSql.append(" ");
			fromSql.append(tablePrefix.get(Track.class) );
			fromSql.append( " on a.pld_uuid = " + tablePrefix.get(Track.class) + ".patrol_leg_day_uuid " );
		}
		if (patrolItem.getOption() == PatrolValueOption.NUM_MEMBERS ||
			patrolItem.getOption() == PatrolValueOption.MAN_HOURS  || 
			patrolItem.getOption() == PatrolValueOption.MAN_DAYS  ){
			fromSql.append(" left join ");
			fromSql.append(tableNames.get(PatrolLegMember.class));
			fromSql.append(" ");
			fromSql.append(tablePrefix.get(PatrolLegMember.class) );
			fromSql.append(" on a.pl_uuid = " + tablePrefix.get(PatrolLegMember.class) + ".patrol_leg_uuid " );
		}
		if (patrolItem.getOption() == PatrolValueOption.NUM_HOURS ||
			  patrolItem.getOption() == PatrolValueOption.MAN_HOURS ||
			  patrolItem.getOption() == PatrolValueOption.MAN_DAYS  ){
			fromSql.append(" left join ");
			fromSql.append(tableNames.get(PatrolLegDay.class));
			fromSql.append(" ");
			fromSql.append(tablePrefix.get(PatrolLegDay.class));
			fromSql.append( " on a.pld_uuid = " + tablePrefix.get(PatrolLegDay.class)+ ".uuid ");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append(groupBySql);
		if (groupBySql.length() > 0){
			sql.append(",");
		}
		sql.append(valueAggSql);
		sql.append(" FROM ( SELECT distinct ");
		sql.append(selectSql);
		sql.append(groupByInnerSql);
		if (groupByInnerSql.length() > 0){
			sql.append(",");
		}
		sql.append(valueSql);
		sql.append(" FROM ");
		sql.append(fromSql);
		sql.append(") foo");
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " );
			sql.append(groupBySql);
		}
		
		//do something here with sql
		QueryPlugIn.logSql(sql.toString());
		
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
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
			Connection c, Session s, 
			GroupByPart groupBy, 
			AttributeValueItem attributeItem) throws SQLException{
		
		StringBuilder fromSql = new StringBuilder();
		
		fromSql.append(queryTempTable + " tmp ");
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();

		createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql);
		
		String valueSql = "tmp.ob_uuid";
		if (attributeItem.getCategoryKey() != null){
			valueSql = valueSql + ",tmp.cat_hkey";
		}
		StringBuilder valueAggSql = new StringBuilder();
		valueAggSql.append(attributeItem.getAggregation().getName());
		valueAggSql.append("(");
		valueAggSql.append(tablePrefix.get(WaypointObservationAttribute.class));
		valueAggSql.append(".number_value)");
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append(groupBySql);
		if (groupBySql.length() > 0){
			sql.append(",");
		}
		sql.append(valueAggSql);
		sql.append(" FROM ( SELECT distinct ");
		sql.append(groupByInnerSql);
		if (groupByInnerSql.length() > 0){
			sql.append(",");
		}
		sql.append(valueSql);
		sql.append(" FROM ");
		sql.append(fromSql);
		
		
		sql.append(") foo");
		
		sql.append(" join ");
		sql.append(tableNames.get(WaypointObservationAttribute.class));
		sql.append(" ");
		sql.append(tablePrefix.get(WaypointObservationAttribute.class));
		sql.append(" on foo.ob_uuid = ");
		sql.append(tablePrefix.get(WaypointObservationAttribute.class));
		sql.append(".observation_uuid ");
		sql.append(" join ");
		sql.append(tableNames.get(Attribute.class));
		sql.append(" ");
		sql.append(tablePrefix.get(Attribute.class));
		sql.append(" on ");
		sql.append(tablePrefix.get(WaypointObservationAttribute.class));
		sql.append(".attribute_uuid = ");
		sql.append(tablePrefix.get(Attribute.class));
		sql.append(".uuid ");

		sql.append(" WHERE ");
		sql.append(tablePrefix.get(WaypointObservationAttribute.class));
		sql.append(".number_value is not null and ");
		sql.append(tablePrefix.get(Attribute.class));
		sql.append(".keyid = '");
		sql.append(attributeItem.getAttributeKey() + "'");
		if (attributeItem.getCategoryKey() != null){
			sql.append("AND ( foo.cat_hkey >= '");
			sql.append(attributeItem.getCategoryKey());
			sql.append("' and ");
			sql.append("foo.cat_hkey < '");
			sql.append(attributeItem.getCategoryKey().substring(0, attributeItem.getCategoryKey().length()-1));
			sql.append("/') ");
		}
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " );
			sql.append(groupBySql);
		}
		
		//do something here with sql
		QueryPlugIn.logSql(sql.toString());
		
		ResultSet rs = c.createStatement().executeQuery(sql.toString());

		return createValueResults(rs, groupBy, attributeItem.asString());
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
				
				String key = gb.getKeyPart() + ":";
				switch (gb.getType()) {
					case STRING:
//						if (gb instanceof PatrolGroupBy ){
//							key += "\"" + rs.getString(rsindex++) + "\"";
//						}else{
							key += rs.getString(rsindex++);
//						}
						break;
					case BYTE:
						key += SmartUtils.encodeHex(rs.getBytes(rsindex++));
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
			CombinedValueItem item) throws SQLException{
		
		HashMap<SummaryResultKey, Double> values1 = computeValueItem(c, s, groupBy, item.getPart1());
		HashMap<SummaryResultKey, Double> values = new HashMap<SummaryResultKey, Double>();
		
		HashMap<SummaryResultKey, Double> values2 = computeValueItem(c, s, groupBy, item.getPart2());
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		
		for (Iterator<Entry<SummaryResultKey, Double>> iterator = values2.entrySet().iterator(); iterator.hasNext();) {
			Entry<SummaryResultKey, Double> type = iterator.next();

			SummaryResultKey newKey = new SummaryResultKey(type.getKey());
			newKey.setValueKey(item.asString());
			
			values.put(newKey, type.getValue());
		}
		
		for (Iterator<Entry<SummaryResultKey, Double>> iterator = values1.entrySet().iterator(); iterator.hasNext();) {
			Entry<SummaryResultKey, Double> type = iterator.next();
			
			SummaryResultKey key = new SummaryResultKey(type.getKey());
			key.setValueKey(item.asString());
			
			Double value = type.getValue();
			Double v2 = values.get(key);
			if (v2 == null ){
				value = null;
			}else if (v2 == 0){
				value = Double.NaN;
			}else if (value != null){
				value = (value / v2);
			}
			results.put(key, value);
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
			Connection c, Session s, 
			GroupByPart groupBy, 
			CategoryValueItem categoryItem) throws SQLException{
		
		StringBuilder fromSql = new StringBuilder();
		
		fromSql.append(queryTempTable + " a ");
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();

		createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql);
		
		String valueSql = "a.ob_uuid";
		StringBuilder valueAggSql = new StringBuilder();
		valueAggSql.append("count(ob_uuid)");
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append(groupBySql);
		if (groupBySql.length() > 0){
			sql.append(",");
		}
		sql.append(valueAggSql);
		sql.append(" FROM ( SELECT distinct ");
		sql.append("a.cat_hkey, ");
		sql.append(groupByInnerSql);
		if (groupByInnerSql.length() > 0){
			sql.append(",");
		}
		sql.append(valueSql);
		sql.append(" FROM ");
		sql.append(fromSql);
		
		
		sql.append(") foo");
//		
//		sql.append(" join smart.wp_observation ");
//		sql.append(tablePrefix.get(WaypointObservation.class));
//		sql.append(" on foo.ob_uuid = ");
//		sql.append(tablePrefix.get(WaypointObservation.class));
//		sql.append(".uuid");
//		sql.append(" join smart.dm_category ");
//		sql.append(tablePrefix.get(Category.class));
//		sql.append(" on ");
//		sql.append(tablePrefix.get(WaypointObservation.class));
//		sql.append(".category_uuid = ");
//		sql.append(tablePrefix.get(Category.class));
//		sql.append(".uuid ");
		
		sql.append(" WHERE ");
		sql.append(" (");
//		sql.append(tablePrefix.get(Category.class));
//		sql.append(".hkey >= '");
		sql.append("cat_hkey >= '");
		sql.append(categoryItem.getCategoryHKey());
		sql.append("' and ");
//		sql.append(tablePrefix.get(Category.class));
//		sql.append(".hkey < '");
		sql.append("cat_hkey < '");
		sql.append(categoryItem.getCategoryHKey().substring(0, categoryItem.getCategoryHKey().length()-1));
		sql.append("/') ");
		
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " );
			sql.append(groupBySql);
		}
		
		//do something here with sql
		QueryPlugIn.logSql(sql.toString());
		
		ResultSet rs = c.createStatement().executeQuery(sql.toString());

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
	private void createGroupBySql(GroupByPart groupBy,
			StringBuilder fromSql,
			StringBuilder groupBySql, 
			StringBuilder groupByInnerSql) {
		
		int itemcnt = 1;
		for (IGroupBy gb : groupBy.getGroupBys()){
			if (gb instanceof PatrolGroupBy){
				String prefix = getTablePrefix((PatrolGroupBy) gb);
				String name = getFieldName((PatrolGroupBy) gb);
				if (prefix != null){
					groupByInnerSql.append(prefix + ".");
				}
				groupByInnerSql.append(name + " as " + name + "_" + itemcnt);
				groupBySql.append(name + "_" + itemcnt);
				
				if (((PatrolGroupBy)gb).option == PatrolQueryOption.EMPLOYEE){
					fromSql.append(" left join ");
					fromSql.append(tableNames.get(PatrolLegMember.class));
					fromSql.append(" ");
					fromSql.append(tablePrefix.get(PatrolLegMember.class));
					fromSql.append(" on a.pl_uuid = " + tablePrefix.get(PatrolLegMember.class) + ".patrol_leg_uuid ");
				}
			}else if (gb instanceof DateGroupBy){
				DateGroupByOption op = ((DateGroupBy)gb).getOption();
				if (op == DateGroupByOption.DAY){
					groupByInnerSql.append("pld_patrol_day as pld_patrol_day_" + itemcnt);
					groupBySql.append("pld_patrol_day_" + itemcnt);
				}else if (op == DateGroupByOption.MONTH){
					groupBySql.append("datePart_" + itemcnt);
					groupByInnerSql.append("trim(cast(month(pld_patrol_day) as char(2))) || '/' || cast(year(pld_patrol_day) as char(4)) as datePart_" + itemcnt);
				}else if (op == DateGroupByOption.YEAR){
					groupBySql.append("datePart_" + itemcnt);
					groupByInnerSql.append("YEAR(pld_patrol_day) as datePart_" + itemcnt);
				}
			}else if (gb instanceof CategoryGroupBy){
				CategoryGroupBy op = ((CategoryGroupBy)gb);
//				if (categoryKey == null){
//					groupByInnerSql.append("a.cat_hkey as category_" + itemcnt + "_a, ");
//				}
				String categoryKey = "category_" + itemcnt;
				groupByInnerSql.append("smart.trimHkeyToLevel(");
				groupByInnerSql.append(op.getTreeLevel() + ", ");
				groupByInnerSql.append("cat_hkey) as " + categoryKey);
				
				groupBySql.append(categoryKey);
			}else if (gb instanceof AttributeGroupBy){
			
				groupBySql.append("attribute_" + itemcnt);
				if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.LIST){
					groupByInnerSql.append(tablePrefix.get(AttributeListItem.class));
					groupByInnerSql.append(".keyid as  attribute_" + itemcnt);
				}else if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.TREE){
					groupByInnerSql.append("smart.trimHkeyToLevel(");
					groupByInnerSql.append(((AttributeGroupBy)gb).getTreeLevel().intValue() + ",");
					groupByInnerSql.append(tablePrefix.get(AttributeTreeNode.class));
					groupByInnerSql.append(".hkey) as  attribute_" + itemcnt + " ");
				}
				
				fromSql.append(" JOIN ");
				fromSql.append(tableNames.get(WaypointObservationAttribute.class));
				fromSql.append(" ");
				fromSql.append(tablePrefix.get(WaypointObservationAttribute.class));
				fromSql.append(" on ");
				fromSql.append(tablePrefix.get(WaypointObservationAttribute.class));
				fromSql.append(".observation_uuid = a.ob_uuid ");
				
				String catkey = ((AttributeGroupBy)gb).getCategoryHkey();
				if (catkey != null){
					fromSql.append(" and (");
					fromSql.append("a.cat_hkey >= '");
					fromSql.append(catkey);
					fromSql.append("' and ");
					fromSql.append("a.cat_hkey < '");
					fromSql.append(catkey.substring(0, catkey.length() - 1));
					fromSql.append("/') ");
				}
				
				fromSql.append(" JOIN ");
				if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.LIST){
					fromSql.append(tableNames.get(AttributeListItem.class));
					fromSql.append(" ");
					fromSql.append(tablePrefix.get(AttributeListItem.class));
					fromSql.append(" on ");
					fromSql.append(tablePrefix.get(AttributeListItem.class));
					fromSql.append(".uuid =");
					fromSql.append(tablePrefix.get(WaypointObservationAttribute.class));
					fromSql.append(".list_element_uuid ");
				}else if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.TREE){
					fromSql.append(tableNames.get(AttributeTreeNode.class));
					fromSql.append(" ");
					fromSql.append(tablePrefix.get(AttributeTreeNode.class));
					fromSql.append(" on ");
					fromSql.append(tablePrefix.get(AttributeTreeNode.class));
					fromSql.append(".uuid =");
					fromSql.append(tablePrefix.get(WaypointObservationAttribute.class));
					fromSql.append(".tree_node_uuid ");
				}
				fromSql.append(" JOIN ");
				fromSql.append(tableNames.get(Attribute.class));
				fromSql.append(" ");
				fromSql.append(tablePrefix.get(Attribute.class));
				fromSql.append(" on ");
				fromSql.append(tablePrefix.get(WaypointObservationAttribute.class));
				fromSql.append(".attribute_uuid = ");
				fromSql.append(tablePrefix.get(Attribute.class));
				fromSql.append(".uuid AND ");
				fromSql.append(tablePrefix.get(Attribute.class));
				fromSql.append(".keyid = '");
				fromSql.append(((AttributeGroupBy)gb).getAttributeKey());
				fromSql.append("' ");
								
			}else{
				//throw new exception; should only be patrol group bys here for now
			}
			itemcnt++;
			
			groupBySql.append(",");
			groupByInnerSql.append(",");
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
			return "p_uuid";
		}else if (clazz.equals(PatrolLeg.class)){
			return "pl_uuid";
		}else if (clazz.equals(PatrolLegDay.class) || clazz.equals(Track.class)){
			return "pld_uuid";
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
	private String getAggFieldName(PatrolValueItem item){
		switch(item.getOption()){
		case NUM_PATROLS:
			return "count(p_uuid)";
		case NUM_DAYS:
			return " count(pld_patrol_day) ";
		case NUM_NIGHTS:
			return " sum( {fn timestampdiff(SQL_TSI_DAY, p_start_date,p_end_date)} ) ";
		case DISTANCE:
			return "sum(distance)";
		case NUM_HOURS:
			return "sum({fn timestampdiff(SQL_TSI_SECOND, pld_start_time, pld_end_time)} / ( 60.0 * 60.0))";
		case NUM_MEMBERS:
			return "count(pl_member)";
		case MAN_HOURS:
			return "sum({fn timestampdiff(SQL_TSI_SECOND, pld_start_time, pld_end_time)} / ( 60.0 * 60.0))";
		case MAN_DAYS:
			return "count(pld_patrol_day) ";
		}
		assert false;
		return "";
	}
	
	private String getFieldName(PatrolValueItem item){
		switch(item.getOption()){
		case NUM_PATROLS:
			return "p_uuid";
		case NUM_DAYS:
			return "pld_patrol_day";
		case NUM_NIGHTS:
			return "p_start_date,p_end_date";
		case DISTANCE:
			return tablePrefix.get(Track.class) + ".distance as distance";
		case NUM_HOURS:
			return tablePrefix.get(PatrolLegDay.class) + ".start_time as pld_start_time," +
			tablePrefix.get(PatrolLegDay.class) + ".end_time as pld_end_time";
		case NUM_MEMBERS:
			return tablePrefix.get(PatrolLegMember.class) + ".employee_uuid as pl_member";
		case MAN_HOURS:
			return tablePrefix.get(PatrolLegDay.class) + ".start_time as pld_start_time, " +
			tablePrefix.get(PatrolLegDay.class) + ".end_time as pld_end_time, " +
			tablePrefix.get(PatrolLegMember.class) + ".employee_uuid as pl_member";
		case MAN_DAYS:
			return "pld_patrol_day, " +
			tablePrefix.get(PatrolLegMember.class) + ".employee_uuid as pl_member";
		}
		//TODO: should not get here
		return null;
	}
	
	
	/**
	 * Returns the patrol group by field from 
	 * the temproary results table that contains
	 * the given patrol group by item.
	 * 
	 * @param gb
	 * @return
	 */
	private String getFieldName(PatrolGroupBy gb){
		switch(gb.getOption()){
		case ID:
			return "p_id";
		case STATION:
			return "p_station_uuid";
		case TEAM:
			return "p_team_uuid";
		case MANDATE:
			return "p_mandate_uuid";
		case PATROL_TYPE:
			return "p_type";
		case PATROL_TRANSPORT_TYPE:
			return "pl_transport_uuid";
		case LEADER:
			return "plm_leader";
		case EMPLOYEE:
			return "employee_uuid";
		}
		assert false;
		return "";
	}
	
	/**
	 * Table group item prefix from the query.
	 * 
	 * @param gb
	 * @return
	 */
	private String getTablePrefix(PatrolGroupBy gb){
		if (gb.getOption() == PatrolQueryOption.EMPLOYEE){
			return tablePrefix.get(PatrolLegMember.class);
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
	public static void getHeaderInfo(SummaryQuery query, SummaryQueryResult results, Session session){
		
		// value headers
		ValuePart vp = query.getQueryDefinition().getValuePart();
		for (IValueItem item : vp.getValueItems()){
			SummaryHeader header = new SummaryHeader(item.getName(session), item.asString(), true);
			results.addValueHeader(header);
		}
		
		for (IGroupBy item : query.getQueryDefinition().getRowGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(query.getDateFilter());
			}
			List<ListItem> items = item.getItems(session);
			SummaryHeader[] rowHeader = new SummaryHeader[items.size()];
			for (int i = 0; i < items.size(); i ++){
				ListItem it = items.get(i);
				if (it.getUuid() != null){
					rowHeader[i] = new SummaryHeader( it.getName(), item.getKeyPart(), SmartUtils.encodeHex( it.getUuid() ), false);
				}else{
					rowHeader[i] = new SummaryHeader( it.getName(), item.getKeyPart(), it.getKey(), false);
				}	
				
			}
			results.addRowHeader(rowHeader);
		}
		
		for (IGroupBy item : query.getQueryDefinition().getColumnGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(query.getDateFilter());
			}
			List<ListItem> items = item.getItems(session);
			SummaryHeader[] colHeader = new SummaryHeader[items.size()];
			for (int i = 0; i < items.size(); i ++){
				ListItem it = items.get(i);
				if (it.getUuid() != null){
					colHeader[i] = new SummaryHeader( it.getName(), item.getKeyPart(), SmartUtils.encodeHex( it.getUuid() ), false);
				}else{
					colHeader[i] = new SummaryHeader( it.getName(), item.getKeyPart(), it.getKey(), false);
				}
				
			}
			results.addColumnHeader(colHeader);
		}
		
	}
}
