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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.engine.visitors.SurveyHasObservationFilterVisitor;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.filter.summary.MissionAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.MissionIdGroupBy;
import org.wcs.smart.er.query.filter.summary.MissionValueItem;
import org.wcs.smart.er.query.filter.summary.MissionValueItem.ValueItem;
import org.wcs.smart.er.query.filter.summary.SamplingUnitAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.SamplingUnitGroupBy;
import org.wcs.smart.er.query.filter.summary.SurveyIdGroupBy;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.SurveyValueItemLabelProvider;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.er.query.ui.filter.summary.ISurveyGroupByViewer;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
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
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.ObserverGroupBy;
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
public class DerbySummaryEngine extends DerbySurveyQueryEngine{

	private SummaryQueryResult sumResults = null;
	HashMap<String, HashMap<SummaryResultKey, Double>> cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
	
	private String rateTable;
	private String valueTable;
	
	private HashSet<Class<?>> usedTables;
	
	
	@Override
	public boolean canExecute(String querytype) {
		return SurveySummaryQuery.KEY.equals(querytype);
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

		final SurveySummaryQuery query = (SurveySummaryQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		

		valueTable = createTempTableName();
		rateTable = createTempTableName();
		
		sumResults = new SummaryQueryResult();
		cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {		
				try {
					monitor.beginTask(Messages.DerbySummaryEngine_ProcessingQueryProgress, query.getQueryDefinition().getValuePart().getValueItems().size()*10 + 40);
					
					SurveyDesignFilter surveyFilter = null;
					if (query.getSurveyDesign() != null){
						surveyFilter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
					}

					
					monitor.subTask(Messages.DerbySummaryEngine_LoadingTableProgress);
					getHeaderInfo(query, sumResults, surveyFilter, session);
					monitor.worked(10);
					if (monitor.isCanceled()){
						return;
					}
					
					boolean needsObservationValue = false;
					boolean needsObservationRate = false;
					
					List<IGroupBy> all = new ArrayList<IGroupBy>();
					all.addAll(query.getQueryDefinition().getColumnGroupByPart().getGroupBys());
					all.addAll(query.getQueryDefinition().getRowGroupByPart().getGroupBys());
					GroupByPart allGroupBy = new GroupByPart(all);
					
					HasObservationValueVisitor vv = new HasObservationValueVisitor();
					query.getQueryDefinition().getValuePart().visit(vv);
					needsObservationValue = vv.hasCategory() || vv.hasAttribute();
					
					if(!needsObservationValue){
						HasObservationGroupByVisitor cv = new HasObservationGroupByVisitor();
						query.getQueryDefinition().getColumnGroupByPart().visit(cv);
						needsObservationValue = cv.hasCategory()  || cv.hasAttribute();;
						if (!needsObservationValue){
							query.getQueryDefinition().getRowGroupByPart().visit(cv);
							needsObservationValue = cv.hasCategory() || cv.hasAttribute();;
						}
						
					}
					needsObservationRate = needsObservationValue;
					QueryFilter valueFilter = new QueryFilter(EmptyFilter.INSTANCE);
					if (query.getQueryDefinition().getValueFilter() != null){
						valueFilter = query.getQueryDefinition().getValueFilter();
					}
					QueryFilter rateFilter = new QueryFilter(EmptyFilter.INSTANCE);
					if (query.getQueryDefinition().getRateFilter() != null){
						rateFilter = query.getQueryDefinition().getRateFilter();
					}
					
					if (!needsObservationValue){
						
						SurveyHasObservationFilterVisitor visitor = new SurveyHasObservationFilterVisitor();
						valueFilter.getFilter().accept(visitor);
						if (visitor.hasAttributeFilter() || visitor.hasCategoryFilter() || 
								visitor.hasObservationFilter()){
							needsObservationValue = true;
						}
						
						visitor.clear();
						rateFilter.getFilter().accept(visitor);
						if (visitor.hasAttributeFilter() || visitor.hasCategoryFilter() || visitor.hasSamplingUnitObservationFilter()){
							needsObservationRate = true;
						}
					}
					
					//create a date filter that caches the dates so the same
					//dates are used for all parts of the query;
					//otherwise different date filters will be computed
					//for different parts of the queries
					DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
					
					ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					IFilterProcessor filterer = DerbySummaryEngine.this.getFilterProcessor(valueFilter.getFilterType(), valueTable, surveyFilter);
					try{
						filterer.processFilter(c, valueFilter.getFilter(), dFilter, caFilter, needsObservationValue, false, new SubProgressMonitor(monitor, 10));
					}finally{
						filterer.dropTemporaryTables(c);
					}
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbySummaryEngine_ProgressCategoryKeys);
					addCategoryHkey(valueTable, allGroupBy, query.getQueryDefinition().getValuePart(), c);
					monitor.worked(10);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbySummaryEngine_ProgressRateFilter);
					String vFilter = valueFilter.asString();
					String rFilter = rateFilter.asString();
					
					if (vFilter.equals(rFilter)){
						rateTable = valueTable;
						monitor.worked(10);
					}else{
						rateTable = createTempTableName();
						IFilterProcessor rfilterer = DerbySummaryEngine.this.getFilterProcessor(rateFilter.getFilterType(), rateTable, surveyFilter);
						try{
							rfilterer.processFilter(c, rateFilter.getFilter(), dFilter, caFilter, needsObservationRate, false, new SubProgressMonitor(monitor, 10));
						}finally{
							rfilterer.dropTemporaryTables(c);
						}
						if (monitor.isCanceled()){
							return;
						}
						addCategoryHkey(rateTable, allGroupBy, query.getQueryDefinition().getValuePart(), c);
						if (monitor.isCanceled()){
							return;
						}
					}
					
					monitor.subTask(Messages.DerbySummaryEngine_ProgressValues);
					HashMap<SummaryResultKey, Double> data = computeSummaryValues(c, session, 
							allGroupBy, query.getQueryDefinition().getValuePart(),
							caFilter, new SubProgressMonitor(monitor, query.getQueryDefinition().getValuePart().getValueItems().size() * 10));
					
					if (monitor.isCanceled() || data == null){
						return ;
					}
					sumResults.setData(data);
					
					if (monitor.isCanceled()){
						return;
					}
					monitor.worked(10);
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
					monitor.done();
				}
				c.commit();
			}
		});

		return sumResults ;
	}

	@Override
	public void dropTables(Connection c){}
	
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
			ValuePart values, ConservationAreaFilter caFilter,
			IProgressMonitor monitor) throws SQLException{
		monitor.beginTask(Messages.DerbySummaryEngine_ProgressValues2, values.getValueItems().size());
		
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		for (IValueItem it : values.getValueItems()){
			monitor.subTask("Processing Value: " + it.asString()); //$NON-NLS-1$
			HashMap<SummaryResultKey, Double> data = computeValueItem(c, s, groupBy, it, caFilter, valueTable) ; 
			if (data != null){
				results.putAll( data );	
			}
			
			monitor.worked(1);
			if (monitor.isCanceled()){
				return null;
			}
		}
		monitor.done();
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
		
		usedTables = new HashSet<Class<?>>();
		
		String cacheKey = it.asString() + "_" + groupBy.asString() + "_" + dataTable; //$NON-NLS-1$ //$NON-NLS-2$
		HashMap<SummaryResultKey, Double> results = cachedValueToResults.get(cacheKey); 
		if (results != null){
			return results;
		}
		if (it instanceof AttributeValueItem){
			results =  (getAttributeValue(dataTable, c, s, groupBy, (AttributeValueItem)it, caFilter));
		}else if (it instanceof CategoryValueItem){
			results = (getCategoryValue(dataTable, c, s, groupBy, (CategoryValueItem)it, caFilter));
		}else if (it instanceof CombinedValueItem){
			results = (getCombinedValue(c, s, groupBy, (CombinedValueItem)it, caFilter));
		}else if (it instanceof MissionValueItem){
			results = getSurveySummaryValue(dataTable, c, s, groupBy, (MissionValueItem)it, caFilter);
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
	private HashMap<SummaryResultKey, Double> getSurveySummaryValue(
			String dataTableName,
			Connection c, Session s, 
			GroupByPart groupBy, 
			MissionValueItem valueItem, ConservationAreaFilter caFilter) throws SQLException{
		
		clearParameters();
		
		StringBuilder selectSql = new StringBuilder();
		StringBuilder fromSql = new StringBuilder();
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();
		StringBuilder valueSql = new StringBuilder();
		StringBuilder valueAggSql = new StringBuilder();
		
		fromSql.append(dataTableName + " temp "); //$NON-NLS-1$
		
		createGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, valueItem, caFilter);
		
		boolean hasAreaGroupBy = false;
		for (IGroupBy groupby : groupBy.getGroupBys()){
			if (groupby instanceof AreaGroupBy){
				hasAreaGroupBy = true;
				break;
			}
		}

		if (valueItem.getValueItem() == ValueItem.TRACK_LENGTH || 
			valueItem.getValueItem() == ValueItem.TRACK_LENGTH_TOTAL){
			
			if (!hasAreaGroupBy){
				valueSql.append( "smart.distanceInMeter(" + tablePrefix(MissionTrack.class) + ".geometry) / 1000.0 as distance"); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				StringBuilder append = new StringBuilder();
				valueSql.append("smart.distanceInMeter("); //$NON-NLS-1$
				for(String prefix : areaGroupByPrefix){
					valueSql.append("smart.intersection("); //$NON-NLS-1$
					valueSql.append(prefix);
					valueSql.append(".geom,"); //$NON-NLS-1$
					append.append(")"); //$NON-NLS-1$
				}
				valueSql.append(tablePrefix(MissionTrack.class));
				valueSql.append(".geometry"); //$NON-NLS-1$
				valueSql.append(append);
				valueSql.append(") / 1000.0 as distance "); //$NON-NLS-1$
			}

			valueAggSql.append("sum(distance)"); //$NON-NLS-1$
			
			if (!usedTables.contains(MissionTrack.class)) {
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableNamePrefix(MissionTrack.class));
				fromSql.append( " on temp.mission_day_uuid = "); //$NON-NLS-1$ 
				fromSql.append(tablePrefix(MissionTrack.class));
				fromSql.append(".mission_day_uuid " ); //$NON-NLS-1$
				usedTables.add(MissionTrack.class);
			}
			selectSql.append(tablePrefix(MissionTrack.class) + ".uuid, "); //$NON-NLS-1$
			
		}else if (valueItem.getValueItem() == ValueItem.MISSION_COUNT ||
				valueItem.getValueItem() == ValueItem.MISSION_COUNT_TOTAL){
			valueSql.append("temp.mission_uuid"); //$NON-NLS-1$
			valueAggSql.append("count(mission_uuid)"); //$NON-NLS-1$
			selectSql.append("temp.mission_uuid as uniqueid, "); //$NON-NLS-1$
		}else if (valueItem.getValueItem() == ValueItem.SURVEY_COUNT ||
				valueItem.getValueItem() == ValueItem.SURVEY_COUNT_TOTAL){
			selectSql.append("temp.survey_uuid as uniqueid, "); //$NON-NLS-1$
			valueSql.append("temp.survey_uuid"); //$NON-NLS-1$
			valueAggSql.append("count(survey_uuid)"); //$NON-NLS-1$
		}else if (valueItem.getValueItem() == ValueItem.DAY_COUNT){
			selectSql.append("temp.mission_day_uuid as uniqueid, "); //$NON-NLS-1$
			valueSql.append("temp.mission_day_uuid"); //$NON-NLS-1$
			valueAggSql.append("count(mission_day_uuid)"); //$NON-NLS-1$
		}else if (valueItem.getValueItem() == ValueItem.HOUR_COUNT){
			
			selectSql.append("temp.mission_day_uuid as uniqueid, "); //$NON-NLS-1$
			
			if (!hasAreaGroupBy){
				
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableNamePrefix(MissionDay.class));
				fromSql.append(" on temp.mission_day_uuid = "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionDay.class) + ".uuid "); //$NON-NLS-1$
				
				valueSql.append(tablePrefix(MissionDay.class) + ".start_time as md_start_time, "); //$NON-NLS-1$
				valueSql.append(tablePrefix(MissionDay.class) + ".end_time as md_end_time, "); //$NON-NLS-1$
				valueSql.append(tablePrefix(MissionDay.class) + ".rest_minutes as md_rest"); //$NON-NLS-1$
				
				valueAggSql.append("sum (({fn timestampdiff(SQL_TSI_SECOND, md_start_time, md_end_time) } / (3600.0)) - (md_rest / 60.0))"); //$NON-NLS-1$
				
			}else{
				StringBuilder append = new StringBuilder();
				
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
				valueSql.append(tablePrefix(MissionTrack.class));
				valueSql.append(".geometry) as hours "); //$NON-NLS-1$
				
				
				valueAggSql.append("sum(hours)"); //$NON-NLS-1$
				
			}
			
		}else if (valueItem.getValueItem() == ValueItem.MANHOURS_COUNT){
			selectSql.append("temp.mission_day_uuid as uniqueid, "); //$NON-NLS-1$
			
			fromSql.append(" join "); //$NON-NLS-1$
			fromSql.append(tableNamePrefix(MissionMember.class));
			fromSql.append(" on temp.mission_uuid = "); //$NON-NLS-1$
			fromSql.append(tablePrefix(MissionMember.class) + ".mission_uuid "); //$NON-NLS-1$
			
			if (!hasAreaGroupBy){     
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableNamePrefix(MissionDay.class));
				fromSql.append(" on temp.mission_day_uuid = "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionDay.class) + ".uuid "); //$NON-NLS-1$
			
				valueSql.append(tablePrefix(MissionDay.class) + ".start_time as md_start_time, "); //$NON-NLS-1$
				valueSql.append(tablePrefix(MissionDay.class) + ".end_time as md_end_time, "); //$NON-NLS-1$
				valueSql.append(tablePrefix(MissionDay.class) + ".rest_minutes as md_rest, "); //$NON-NLS-1$
				valueSql.append(tablePrefix(MissionMember.class) + ".employee_uuid as md_member "); //$NON-NLS-1$
				
				valueAggSql.append("sum (({fn timestampdiff(SQL_TSI_SECOND, md_start_time, md_end_time) } / (3600.0)) - (md_rest / 60.0))"); //$NON-NLS-1$
				
			}else{                                                                                                   
				StringBuilder append = new StringBuilder();
				
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
				valueSql.append(tablePrefix(MissionTrack.class));
				valueSql.append(".geometry) as hours, "); //$NON-NLS-1$
				valueSql.append(tablePrefix(MissionMember.class) + ".employee_uuid as md_member " ); //$NON-NLS-1$
				
				valueAggSql.append("sum(hours)"); //$NON-NLS-1$                                                       
			}                                                     
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
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();
		return createValueResults(rs, groupBy, valueItem.asString());
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
			sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (attributeItem.getCategoryKey() != null) {
				p1 = addParameterValue(attributeItem.getCategoryKey());
				String p2 = addParameterValue(attributeItem.getCategoryKey().substring(0,attributeItem.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND ( foo.cat_hkey >= " + p1 + " and foo.cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			sql.append(".keyid = '"); //$NON-NLS-1$
			sql.append(attributeItem.getItemKey());
			sql.append("' and "); //$NON-NLS-1$
		
			sql.append(tablePrefix(Attribute.class));
			String p1 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			 
			if (attributeItem.getCategoryKey() != null){
				p1 = addParameterValue(attributeItem.getCategoryKey());
				String p2 = addParameterValue(attributeItem.getCategoryKey().substring(0,attributeItem.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND ( temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			sql.append(") as foo "); //$NON-NLS-1$
			if (groupBySql.length() > 0){
				sql.append(" GROUP BY " ); //$NON-NLS-1$
				sql.append(groupBySql);
			}
			
			//do something here with sql
			QueryPlugIn.logSql(sql.toString());
			ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();;

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
			sql.append(".hkey >= " + p1 + " and "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey < " + p2 + ") and "); //$NON-NLS-1$ //$NON-NLS-2$
			
			p1 = addParameterValue(attributeItem.getAttributeKey());
			sql.append(tablePrefix(Attribute.class));
			sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			 
			if (attributeItem.getCategoryKey() != null){
				p1 = addParameterValue(attributeItem.getCategoryKey());
				p2 = addParameterValue(attributeItem.getCategoryKey().substring(0,attributeItem.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND ( temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$				
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
				
				String key = gb.getKeyPart() ;
				switch (gb.getType()) {
					case STRING:
						key += ":" + rs.getString(rsindex++); //$NON-NLS-1$
						break;
					case BYTE:
						byte[] info = rs.getBytes(rsindex++);
						if (info != null){
							key += ":" + UuidUtils.uuidToString(UuidUtils.byteToUUID(info)); //$NON-NLS-1$
						}
						break;
					case DATE:
						key += ":" + rs.getDate(rsindex++).toString(); //$NON-NLS-1$
						break;
					case KEY:
						key += ":" + rs.getString(rsindex++); //$NON-NLS-1$
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
			CombinedValueItem item, ConservationAreaFilter caFilter) throws SQLException{
		
		HashMap<SummaryResultKey, Double> values1 = computeValueItem(c, s, groupBy, item.getPart1(), caFilter, valueTable);
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		boolean needsGroupBy = false;
		if (item.getPart2() instanceof MissionValueItem && ((MissionValueItem)item.getPart2()).requiresGroupByFilter()){
			needsGroupBy = true;
		}
		
		if (!needsGroupBy){
			HashMap<SummaryResultKey, Double> values2 = computeValueItem(c, s, new GroupByPart(new ArrayList<IGroupBy>()), item.getPart2(), caFilter, rateTable);
			if (values2.values().size() != 1){
				throw new SQLException(Messages.DerbySummaryEngine_InvalidRateFilterValues);
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
			HashMap<SummaryResultKey, Double> values2 = computeValueItem(c, s, groupBy, item.getPart2(), caFilter, rateTable);
			

			for (Iterator<Entry<SummaryResultKey, Double>> iterator = values1.entrySet().iterator(); iterator.hasNext();) {
				Entry<SummaryResultKey, Double> type = iterator.next();			

				SummaryResultKey key2 = new SummaryResultKey(type.getKey());
				key2.setValueKey(item.getPart2().asString());
				
				Double denominator = values2.get(key2);

				SummaryResultKey key = new SummaryResultKey(type.getKey());
				key.setValueKey(item.asString());
			
				Double value = type.getValue();
				if (denominator == null || denominator == 0){
					value = Double.NaN;
				}else{
					value = value / denominator;
				}
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
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder groupByInnerSql = new StringBuilder();

		
		fromSql.append(dataTable + " temp "); //$NON-NLS-1$
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
			sql.append("cat_hkey >= " + p1 + " and cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
						fromSql.append( SurveyFilterSqlGenerator.INSTANCE.asSql(caFilter, areaPrefix, this));
					}
					fromSql.append(" and "); //$NON-NLS-1$
					String p1 = addParameterValue(agb.getAreaType().name());
					fromSql.append(areaPrefix + ".area_type = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
					
					groupBySql.append(key);
				} else {
					//survey value area group bys; use the mission track 
					AreaGroupBy agb = (AreaGroupBy) gb;
					String key = agb.getAreaType().name() + "_" + itemcnt; //$NON-NLS-1$s
					String areaPrefix = tablePrefix(Area.class)
							+ "_" + itemcnt; //$NON-NLS-1$
					groupByInnerSql
							.append(areaPrefix + ".keyid" + " as " + key); //$NON-NLS-1$ //$NON-NLS-2$
					areaGroupByPrefix.add(areaPrefix);
					
					if (!usedTables.contains(MissionTrack.class)) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(MissionTrack.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(MissionTrack.class));
						fromSql.append(" on temp.mission_day_uuid = "); //$NON-NLS-1$
						fromSql.append( tablePrefix(MissionTrack.class) + ".mission_day_uuid"); //$NON-NLS-1$
						usedTables.add(MissionTrack.class);
					}
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNames.get(Area.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(areaPrefix);
					fromSql.append(" on smart.intersects("); //$NON-NLS-1$
					fromSql.append(tablePrefix(MissionTrack.class) + ".geometry, "); //$NON-NLS-1$
					fromSql.append(areaPrefix + ".geom"); //$NON-NLS-1$
					fromSql.append(")"); //$NON-NLS-1$
					if (caFilter != null ){
						fromSql.append(" and ");//$NON-NLS-1$
						fromSql.append( SurveyFilterSqlGenerator.INSTANCE.asSql(caFilter, areaPrefix, this));
					}
					fromSql.append(" and ");//$NON-NLS-1$
					String p1 = addParameterValue(agb.getAreaType().name());
					fromSql.append(areaPrefix + ".area_type = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
					groupBySql.append(key);
				}
				
			}else if (gb instanceof SurveyIdGroupBy){
				groupByInnerSql.append( " temp.survey_uuid as " + "gp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				groupBySql.append("gp_" + itemcnt); //$NON-NLS-1$
			
			}else if (gb instanceof ObserverGroupBy){
				groupByInnerSql.append( " temp.ob_observer_uuid as " + "gp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				groupBySql.append("gp_" + itemcnt); //$NON-NLS-1$
				
			}else if (gb instanceof MissionIdGroupBy){
				groupByInnerSql.append( " temp.mission_uuid as " + "gp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				groupBySql.append("gp_" + itemcnt); //$NON-NLS-1$
			}else if (gb instanceof MissionAttributeGroupBy){
				
				fromSql.append(" JOIN "); //$NON-NLS-1$
				fromSql.append(tableNames.get(MissionPropertyValue.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionPropertyValue.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionPropertyValue.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".mission_uuid = temp.mission_uuid "); //$NON-NLS-1$
			
				fromSql.append(" JOIN "); //$NON-NLS-1$
				fromSql.append(tableNames.get(MissionAttribute.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".uuid = "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionPropertyValue.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".mission_attribute_uuid "); //$NON-NLS-1$
				fromSql.append(" AND "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".keyid = '"); //$NON-NLS-1$
				fromSql.append(((MissionAttributeGroupBy)gb).getAttributeKey());
				fromSql.append("' "); //$NON-NLS-1$
				
				//always list
				fromSql.append(" JOIN "); //$NON-NLS-1$
				fromSql.append(tableNames.get(MissionAttributeListItem.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionAttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionAttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".uuid ="); //$NON-NLS-1$
				fromSql.append(tablePrefix(MissionPropertyValue.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".list_element_uuid "); //$NON-NLS-1$
				
				String field = tablePrefix(MissionAttributeListItem.class) + "_" + itemcnt + ".keyid"; //$NON-NLS-1$ //$NON-NLS-2$
				groupByInnerSql.append( field + " as " + "mp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				groupBySql.append("mp_" + itemcnt); //$NON-NLS-1$
			
			}else if (gb instanceof SamplingUnitAttributeGroupBy){
				if (value instanceof MissionValueItem){
					if (!usedTables.contains(MissionTrack.class)) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(MissionTrack.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(MissionTrack.class));
						fromSql.append(" on temp.mission_day_uuid = "); //$NON-NLS-1$
						fromSql.append(tablePrefix(MissionTrack.class) + ".mission_day_uuid"); //$NON-NLS-1$ 
						usedTables.add(MissionTrack.class);
					}
					fromSql.append(" JOIN "); //$NON-NLS-1$
					fromSql.append(tableNames.get(SamplingUnitAttributeValue.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".su_uuid = "); //$NON-NLS-1$
					fromSql.append(tablePrefix(MissionTrack.class) + ".sampling_unit_uuid"); //$NON-NLS-1$
				}else{
					fromSql.append(" JOIN "); //$NON-NLS-1$
					fromSql.append(tableNames.get(SamplingUnitAttributeValue.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".su_uuid = temp.sampling_unit_uuid"); //$NON-NLS-1$
				}
			
				fromSql.append(" JOIN "); //$NON-NLS-1$
				fromSql.append(tableNames.get(SamplingUnitAttribute.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(tablePrefix(SamplingUnitAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(tablePrefix(SamplingUnitAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".uuid = "); //$NON-NLS-1$
				fromSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".su_attribute_uuid "); //$NON-NLS-1$
				fromSql.append(" AND "); //$NON-NLS-1$
				fromSql.append(tablePrefix(SamplingUnitAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
				String p1 = addParameterValue(((SamplingUnitAttributeGroupBy)gb).getAttributeKey());
				fromSql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
				
				
				//always list
				fromSql.append(" JOIN "); //$NON-NLS-1$
				fromSql.append(tableNames.get(SamplingUnitAttributeListItem.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(tablePrefix(SamplingUnitAttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(tablePrefix(SamplingUnitAttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".uuid ="); //$NON-NLS-1$
				fromSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "_" + itemcnt); //$NON-NLS-1$
				fromSql.append(".list_element_uuid "); //$NON-NLS-1$
				
				String field = tablePrefix(SamplingUnitAttributeListItem.class) + "_" + itemcnt + ".keyid"; //$NON-NLS-1$ //$NON-NLS-2$
				groupByInnerSql.append( field + " as " + "mp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				groupBySql.append("mp_" + itemcnt); //$NON-NLS-1$	
			}else if (gb instanceof SamplingUnitGroupBy){
				if (value instanceof MissionValueItem){

					if (!usedTables.contains(MissionTrack.class)) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(MissionTrack.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(MissionTrack.class));
						fromSql.append(" on temp.mission_day_uuid = "); //$NON-NLS-1$
						fromSql.append(tablePrefix(MissionTrack.class) + ".mission_day_uuid"); //$NON-NLS-1$ 
						usedTables.add(MissionTrack.class);
					}
					if (((MissionValueItem) value).getValueItem() == ValueItem.TRACK_LENGTH ||
							((MissionValueItem) value).getValueItem() == ValueItem.TRACK_LENGTH_TOTAL){
						//sampling unit link must come from track 
						groupByInnerSql.append( " case when "); //$NON-NLS-1$
						groupByInnerSql.append(tablePrefix(MissionTrack.class));
						groupByInnerSql.append( ".track_type = '" + MissionTrack.TrackType.TRACK + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						groupByInnerSql.append( " then "); //$NON-NLS-1$
//						groupByInnerSql.append(tablePrefix(MissionTrack.class));
//						groupByInnerSql.append( ".uuid "); //$NON-NLS-1$
						groupByInnerSql.append( " null "); //$NON-NLS-1$
						groupByInnerSql.append( " else "); //$NON-NLS-1$
						groupByInnerSql.append(tablePrefix(MissionTrack.class));
						groupByInnerSql.append( ".sampling_unit_uuid end "); //$NON-NLS-1$
						groupByInnerSql.append( " as " + "gp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
						
						groupBySql.append("gp_" + itemcnt); //$NON-NLS-1$
						itemcnt ++;
					}else{
						//sampling unit link can come from track or waypoint; here we use the survey waypoint
						//first then the track.  Ideally we would use both at once, but that doesn't work
						//well with group by statement
						groupByInnerSql.append( "case when temp.sampling_unit_uuid is not null then "); //$NON-NLS-1$
						groupByInnerSql.append(" temp.sampling_unit_uuid "); //$NON-NLS-1$
						groupByInnerSql.append(" else "); //$NON-NLS-1$
							groupByInnerSql.append(" case when temp.wp_mission_track_uuid is not null then "); //$NON-NLS-1$
							groupByInnerSql.append(" temp.wp_mission_track_uuid "); //$NON-NLS-1$
							groupByInnerSql.append(" else "); //$NON-NLS-1$
								groupByInnerSql.append( " case when "); //$NON-NLS-1$
								groupByInnerSql.append(tablePrefix(MissionTrack.class));
								groupByInnerSql.append( ".track_type = '" + MissionTrack.TrackType.TRACK + "'"); //$NON-NLS-1$ //$NON-NLS-2$
								groupByInnerSql.append( " then "); //$NON-NLS-1$
								groupByInnerSql.append(tablePrefix(MissionTrack.class));
								groupByInnerSql.append( ".uuid else "); //$NON-NLS-1$
								groupByInnerSql.append(tablePrefix(MissionTrack.class));
								groupByInnerSql.append( ".sampling_unit_uuid "); //$NON-NLS-1$
								groupByInnerSql.append( "end "); //$NON-NLS-1$
							groupByInnerSql.append(" end "); //$NON-NLS-1$
						groupByInnerSql.append(" end "); //$NON-NLS-1$
						groupByInnerSql.append( " as " + "gp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
					
						groupBySql.append(" gp_" + itemcnt); //$NON-NLS-1$
						itemcnt ++;
					}
				}else{
					groupByInnerSql.append( " case when temp.sampling_unit_uuid is null then temp.wp_mission_track_uuid else temp.sampling_unit_uuid end as " + "gp_"+ itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
					groupBySql.append("gp_" + itemcnt); //$NON-NLS-1$
				}
			}else if (gb instanceof DateGroupBy){
				IDateGroupBy op = ((DateGroupBy)gb).getOption();
			
				String groupByString = "temp.mission_day"; //$NON-NLS-1$
				//data model value item; therefore we use the waypoint date
				if (op.getClass().equals(DayDateGroupBy.class)){
					groupByInnerSql.append(groupByString + " as datePart_" + itemcnt); //$NON-NLS-1$
				}else if (op.getClass().equals(MonthDateGroupBy.class)){
					groupByInnerSql.append("trim(cast(month(" + groupByString + ") as char(2))) || '/' || cast(year(" + groupByString + ") as char(4)) as datePart_" + itemcnt); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}else if (op.getClass().equals(YearDateGroupBy.class)){
					groupByInnerSql.append("YEAR(" + groupByString + ") as datePart_" + itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				}
				groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
				
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
					fromSql.append(" and (temp.cat_hkey >= " + p1 + " and temp.cat_hkey < " + p2 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public static void getHeaderInfo(SurveySummaryQuery query, 
			SummaryQueryResult results, SurveyDesignFilter surveyDesignFilter,
			Session session) throws Exception{
		
		// value headers
		ValuePart vp = query.getQueryDefinition().getValuePart();
		for (IValueItem item : vp.getValueItems()){
			SummaryHeader header = new SummaryHeader(
					SurveyValueItemLabelProvider.INSTANCE.getName(item, session),
					SurveyValueItemLabelProvider.INSTANCE.getFullName(item, session),
					item.asString(), true);
			results.addValueHeader(header);
		}
		
		DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
		
		for (IGroupBy item : query.getQueryDefinition().getRowGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(dFilter.getDateFilterOption());
			}
			List<ListItem> items = null;
			IGroupByViewer<?> viewer = SurveyDropItemFactory.INSTANCE.findViewer(item);
			if (viewer instanceof ISurveyGroupByViewer){
				items = ((ISurveyGroupByViewer)viewer).getItems(session, surveyDesignFilter);
			}else{
				items = viewer.getItems(session);
			}
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
			List<ListItem> items = null;
			IGroupByViewer<?> viewer = SurveyDropItemFactory.INSTANCE.findViewer(item);
			if (viewer instanceof ISurveyGroupByViewer){
				items = ((ISurveyGroupByViewer)viewer).getItems(session, surveyDesignFilter);
			}else{
				items = viewer.getItems(session);
			}
			
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
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".start_datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".end_datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionDay.class) + ".mission_day, "); //$NON-NLS-1$
		
		
		if (includeObservations){
			sql.append(tablePrefix(SamplingUnit.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(SamplingUnit.class) + ".id, "); //$NON-NLS-1$
			
			sql.append(tablePrefix(SurveyWaypoint.class) + ".mission_track_uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as char for bit data),");	//su_uuid //$NON-NLS-1$
			sql.append("cast(null as char),");	//suid //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	//trackuuid //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as timestamp),");	//wp_datetime //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	//wpuuid //$NON-NLS-1$
			sql.append("cast(null as char for bit data)");	//employee_uuid  //$NON-NLS-1$
		}
		return sql.toString();
	}

	@Override
	protected String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("survey_design_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("survey_design_start date,"); //$NON-NLS-1$
		sql.append("survey_design_end date,"); //$NON-NLS-1$
		
		sql.append("survey_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("survey_id varchar(128),"); //$NON-NLS-1$
		sql.append("survey_start date,"); //$NON-NLS-1$
		sql.append("survey_end date,"); //$NON-NLS-1$
		
		sql.append("mission_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("mission_id varchar(128),"); //$NON-NLS-1$
		sql.append("mission_start timestamp,"); //$NON-NLS-1$
		sql.append("mission_end timestamp,"); //$NON-NLS-1$
		
		sql.append("mission_day_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("mission_day date,"); //$NON-NLS-1$
		
		sql.append("sampling_unit_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("sampling_unit_id varchar(128),"); //$NON-NLS-1$
		
		sql.append("wp_mission_track_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_datetime timestamp,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_observer_uuid char(16) for bit data"); //$NON-NLS-1$
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
	protected SurveyQueryResultItem asQueryResultItem(ResultSet rs, Session session)
			throws SQLException {
		return null;
	}
	
	@Override
	public String getFilterTablesJoinColum(){
		return "wp_uuid"; //$NON-NLS-1$
	}

}
