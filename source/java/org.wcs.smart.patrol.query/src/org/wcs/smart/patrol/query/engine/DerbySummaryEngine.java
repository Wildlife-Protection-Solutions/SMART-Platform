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
package org.wcs.smart.patrol.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeTreeNode;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.ext.IExtensionGroupBy;
import org.wcs.smart.patrol.query.ext.PatrolContributionFinder;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.internal.PatrolValueItemLabelProvider;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.model.PatrolEndMonthDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolEndQuarterDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolStartMonthDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolStartQuarterDateGroupBy;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolAttributeGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItemAreaBuffer;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItemCustomDates;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
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
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.EndHourGroupBy;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.QuarterDateGroupBy;
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
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for executing summary
 * queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbySummaryEngine extends AbstractPatrolQueryEngine{

	private SummaryQueryResult sumResults = null;
	HashMap<String, HashMap<SummaryResultKey, Double>> cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
	
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
	private Session session;
	private PatrolSummaryQuery query;
	
	private Set<String> tablesWithNoData = new HashSet<>();
	
	@Override
	public boolean canExecute(String querytype) {
		return PatrolSummaryQuery.KEY.equals(querytype);
	}

	@Override
	public Session getCurrentConnection() {
		return session;
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
		tablesWithNoData.clear();
		query = (PatrolSummaryQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
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
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbySummaryEngine_Progress_RunningQuery, 2);
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				//need to make sure we cleanup all temp tables correctly
				
				//https://app.assembla.com/spaces/smart-cs/tickets/2858-cannot-run-patrol-summary-query-with-patrol-sector-area-filter/details?comment=1671823408#
				progress.subTask(Messages.DerbySummaryEngine_Progress_LoadingHeaders);
				progress.split(1);
				try{
					getHeaderInfo(query, sumResults, session);
				}catch (Exception ex){
					throw new SQLException(ex);
				}
				
				c.setAutoCommit(true);
				try {
					HasObservationValueVisitor vv = new HasObservationValueVisitor();
					ldef.getValuePart().visit(vv);
					needsObservationValue = vv.hasCategory() || vv.hasAttribute();
					
					if(!needsObservationValue){
						HasObservationGroupByVisitor cv = new HasObservationGroupByVisitor() {
							public void visit(IGroupBy item) {
								if (hasCategory && hasAttribute) return;
								super.visit(item);
								if (item instanceof PatrolGroupBy) {
									if ( ((PatrolGroupBy)item).getOption() == PatrolQueryOption.CM){
										hasCategory = true;
									}
								}
							}
							
						};
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
					ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					HashMap<SummaryResultKey, Double> data = computeSummaryValues(c, session, 
							allGroupByParts, ldef.getValuePart(),
							cafilter, progress.split(1));
					
					
					progress.checkCanceled();
					if (data == null) return;
					sumResults.setData(data);
					
				}catch (OperationCanceledException ex) {
					return;
				}catch (Exception ex) {
					ex.printStackTrace();
					throw ex;
				} finally {
					// ensure temporary tables get dropped
					dropTableInternal(c);
					c.setAutoCommit(false);
				}
			}
		});
		return sumResults ;
	}

	@Override
	public void dropTables(Connection c){
	}
	
	private void dropTableInternal(Connection c){
		if (rateTrackTable != null){
			dropTable(c, rateTrackTable);
			rateTrackTable = null;
		}
		if (valueTrackTable != null){
			dropTable(c, valueTrackTable);
			valueTrackTable= null;
		}
		if (rateWaypointTable != null){
			dropTable(c, rateWaypointTable);
			rateWaypointTable = null;
		}
		if (valueWaypointTable != null){
			dropTable(c, valueWaypointTable);
			valueWaypointTable= null;
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
		SubMonitor progress = SubMonitor.convert(monitor, values.getValueItems().size());
		
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		for (IValueItem it : values.getValueItems()){
			progress.subTask(MessageFormat.format(Messages.DerbySummaryEngine_ProgressValueProgressLabel,it.asString()));
			HashMap<SummaryResultKey, Double> data = computeValueItem(c, s, groupBy, it, caFilter, true, progress.split(1)) ; 
			if (data != null){
				results.putAll( data );	
			}
		}
		return results;
		
	}

	private String getFilterTable(boolean isValue, AreaFilter.AreaFilterGeometryType geomType, ConservationAreaFilter caFilter,
			Connection c, IProgressMonitor monitor) throws SQLException{
	
		
		if (isValue){
			if (!hasAreaFilter || geomType == AreaFilterGeometryType.TRACK){
				if (valueTrackTable == null){
					//create filter table
					valueTrackTable = createTempTableName();
					valueTrackTable = createFilterTable(true, geomType, valueTrackTable, caFilter, c, monitor);
				}
				return valueTrackTable;
			}else if (geomType == AreaFilterGeometryType.WAYPOINT){
				if (valueWaypointTable == null){
					//create filter table
					valueWaypointTable = createTempTableName();
					valueWaypointTable = createFilterTable(true, geomType, valueWaypointTable, caFilter, c, monitor);
				}
				return valueWaypointTable;
			}
		}else{
			if (!hasAreaFilter || geomType == AreaFilterGeometryType.TRACK){
				if (rateTrackTable == null){
					//create filter table
					rateTrackTable = createTempTableName();
					rateTrackTable = createFilterTable(false, geomType, rateTrackTable, caFilter, c, monitor);
				}
				return rateTrackTable;
			}else if (geomType == AreaFilterGeometryType.WAYPOINT){
				if (rateWaypointTable == null){
					//create filter table
					rateWaypointTable = createTempTableName();
					rateWaypointTable = createFilterTable(false, geomType, rateWaypointTable, caFilter, c, monitor);
				}
				return rateWaypointTable;
			}
		}
		//should never get here
		return null;
	}
	
	private String createFilterTable(boolean isValue, AreaFilter.AreaFilterGeometryType geomType, 
			String tableName, ConservationAreaFilter caFilter, Connection c, IProgressMonitor monitor) throws SQLException{

		QueryFilter qFilter = null;
		if (isValue){
			qFilter = valueFilter;
		}else{
			qFilter = rateFilter;
			
			//this may be the same as an existing value filter; don't regenerate table 
			if (qFilter.asString().equals(valueFilter.asString())){
				return getFilterTable(true, geomType, caFilter, c, monitor);
			}
		}
		
		AreaFilterCollectorVisitor areaVisitor = new AreaFilterCollectorVisitor();
		qFilter.getFilter().accept(areaVisitor);
		for (AreaFilter af : areaVisitor.getAreaFilters()){
			//update filter type
			af.changeGeometryType(geomType);
		}
		IFilterProcessor rfilterer = DerbySummaryEngine.this.getFilterProcessor(
				qFilter.getFilterType(), tableName, query);
		try{
			rfilterer.processFilter(c, qFilter.getFilter(), localDateFilter, caFilter, needsObservationRate, false, monitor);
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
			boolean isValueItem,
			IProgressMonitor monitor) throws SQLException {
		
		String dataTable = null;
		if (it instanceof PatrolValueItem){
			dataTable = getFilterTable(isValueItem, AreaFilterGeometryType.TRACK, caFilter, c, monitor);
		}else if (it instanceof AttributeValueItem ||
				  it instanceof CategoryValueItem){
			dataTable = getFilterTable(isValueItem, AreaFilterGeometryType.WAYPOINT, caFilter, c, monitor);
		}else if (it instanceof CombinedValueItem){
			//don't do anything here - each value is dealt with separately in the getCombindValue function
		}
			
		monitor.subTask(MessageFormat.format(Messages.DerbySummaryEngine_ProgressValueProgressLabel,it.asString()));
		String cacheKey = it.asString() + "_" + groupBy.asString() + "_" + dataTable; //$NON-NLS-1$ //$NON-NLS-2$
		HashMap<SummaryResultKey, Double> results = cachedValueToResults.get(cacheKey); 
		if (results != null){
			return results;
		}
		if (it instanceof PatrolValueItem){
			String customDateTable = null;
			if (((PatrolValueItem)it).getPatrolValueOption() == PatrolValueOption.NUM_CUSTOM) {
				//make custom data table
				customDateTable = createNumberCustomDataTable(dataTable, c, s, ((PatrolValueItemCustomDates)it));
			}
			results = (getPatrolSummaryValue(dataTable, c, s, groupBy, (PatrolValueItem)it, caFilter, customDateTable));
			//drop table
			if (customDateTable != null) super.dropTable(c, customDateTable);
		}else if (it instanceof AttributeValueItem){
			results =  (getAttributeValue(dataTable, c, s, groupBy, (AttributeValueItem)it, caFilter));
		}else if (it instanceof CategoryValueItem){
			results = (getCategoryValue(dataTable, c, s, groupBy, (CategoryValueItem)it, caFilter));
		}else if (it instanceof CombinedValueItem){
			results = (getCombinedValue(c, s, groupBy, (CombinedValueItem)it, caFilter, monitor));
		}
		if (results != null){
			cachedValueToResults.put(cacheKey, results); 
		}
		
		return results;
	}
	
	/**
	 * Creates a temporary table with a patrol uuid and patrol leg uuid and
	 * a date.  The date represents the user defined range the patrol leg falls
	 * within.  By counting distinct dates we can count the number
	 * of ranges the patrol fall in. 
	 * 
	 * @param filterTable
	 * @param c
	 * @param s
	 * @param value
	 * @return
	 * @throws SQLException
	 */
	private String createNumberCustomDataTable(String filterTable, Connection c, Session s, PatrolValueItemCustomDates value) throws SQLException {
		String temp = createTempTableName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(temp);
		sb.append("(p_uuid char(16) for bit data, pl_uuid char(16) for bit data, pdate date, stime integer, etime integer, is_start boolean default false, is_end boolean default false)"); //$NON-NLS-1$
		QueryPlugIn.logSql(sb.toString());
		c.createStatement().executeUpdate(sb.toString());
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO "); //$NON-NLS-1$
		sb.append( temp );
		sb.append(" (p_uuid, pl_uuid, pdate, stime, etime) "); //$NON-NLS-1$
		sb.append(" SELECT "); //$NON-NLS-1$
		sb.append(tablePrefix(Patrol.class) + ".uuid, ");  //$NON-NLS-1$
		sb.append(tablePrefix(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sb.append(tablePrefix(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$
		sb.append("hour(" + tablePrefix(PatrolLegDay.class) + ".start_time) * 3600 + minute(" + tablePrefix(PatrolLegDay.class) + ".start_time) * 60 + second(" + tablePrefix(PatrolLegDay.class) + ".start_time), " ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		sb.append("hour(" + tablePrefix(PatrolLegDay.class) + ".end_time) * 3600 + minute(" + tablePrefix(PatrolLegDay.class) + ".end_time) * 60 + second(" + tablePrefix(PatrolLegDay.class) + ".end_time) " ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append( filterTable + " a "); //$NON-NLS-1$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append( tableNamePrefix(Patrol.class));
		sb.append(" ON "); //$NON-NLS-1$
		sb.append( tablePrefix(Patrol.class) + ".uuid = a.p_uuid"); //$NON-NLS-1$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append( tableNamePrefix(PatrolLeg.class));
		sb.append(" ON "); //$NON-NLS-1$
		sb.append( tablePrefix(Patrol.class) + ".uuid = " + tablePrefix(PatrolLeg.class) + ".patrol_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append( tableNamePrefix(PatrolLegDay.class));
		sb.append(" ON "); //$NON-NLS-1$
		sb.append( tablePrefix(PatrolLeg.class) + ".uuid = " + tablePrefix(PatrolLegDay.class) + ".patrol_leg_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sb.toString());
		c.createStatement().executeUpdate(sb.toString());
		
		int start = value.getStartTime();
		int end = value.getEndTime();
		
		if (end < start) end += 86400;
		
		sb = new StringBuilder();
		sb.append("update "); //$NON-NLS-1$
		sb.append(temp);
		sb.append(" set is_start = case when stime <= " + end + " and etime >= " + start + " then true else false end, "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" is_end = case when stime <= " + (end - 86400) + " and etime >= " + (start - 86400) + " then true else false end"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sb.toString());
		c.createStatement().executeUpdate(sb.toString());
		
		String temp2 = createTempTableName();

		sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(temp2);
		sb.append("(cp_uuid char(16) for bit data, cpl_uuid char(16) for bit data, custom_pdate date)"); //$NON-NLS-1$
		QueryPlugIn.logSql(sb.toString());
		c.createStatement().executeUpdate(sb.toString());
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO "); //$NON-NLS-1$
		sb.append(temp2);
		sb.append(" select distinct * FROM ( " ); //$NON-NLS-1$
		sb.append(" select p_uuid, pl_uuid, pdate FROM "); //$NON-NLS-1$
		sb.append(temp);
		sb.append(" WHERE is_start "); //$NON-NLS-1$
		sb.append(" UNION " ); //$NON-NLS-1$
		sb.append(" select p_uuid, pl_uuid, date({fn TIMESTAMPADD( SQL_TSI_DAY, -1, pdate)}) FROM "); //$NON-NLS-1$
		sb.append(temp);
		sb.append(" WHERE is_end "); //$NON-NLS-1$
		sb.append(") foo");	 //$NON-NLS-1$
		QueryPlugIn.logSql(sb.toString());
		c.createStatement().executeUpdate(sb.toString());
		
		super.dropTable(c, temp);
		
		return temp2;
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
			PatrolValueItem patrolItem, ConservationAreaFilter caFilter, String customDateTable) throws SQLException{
		
		if (patrolItem.getPatrolValueOption().hasNoDataOption() && !patrolItem.includeNoData()) {
			addHasDataColumn(c, dataTableName);
		}
		
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
		
		StringBuilder innerWhere = new StringBuilder();
		
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
		
		valueSql.append(getFieldName(patrolItem, hasAreaGroupBy, patrolItem.includeNoData(), innerWhere));
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
			fromSql.append(tableName(PatrolLegMember.class));
			fromSql.append(" "); //$NON-NLS-1$
			fromSql.append(tablePrefix(PatrolLegMember.class) + "_value"); //$NON-NLS-1$
			fromSql.append(" on temp.pl_uuid = ");//$NON-NLS-1$
			fromSql.append( tablePrefix(PatrolLegMember.class) + "_value"); //$NON-NLS-1$
			fromSql.append(".patrol_leg_uuid " ); //$NON-NLS-1$ 
		}
		if (option == PatrolValueOption.NUM_FIELDHOURS ||
			  option == PatrolValueOption.NUM_PATROLHOURS ||
			  option == PatrolValueOption.NUM_PATROLHOURS_TOTAL ||
			  option == PatrolValueOption.NUM_FIELDHOURS_TOTAL ||
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
		if (option == PatrolValueOption.NUM_CUSTOM) {
			fromSql.append(" left join "); //$NON-NLS-1$
			fromSql.append(customDateTable + " c2 "); //$NON-NLS-1$
			fromSql.append( " on c2.cpl_uuid = temp.pl_uuid"); //$NON-NLS-1$
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT "); //$NON-NLS-1$
		sql.append(groupBySql);
		if (groupBySql.length() > 0){
			sql.append(","); //$NON-NLS-1$
		}
		sql.append(valueAggSql);
		
		if (option == PatrolValueOption.AREA_BUFFER) {
			sql.append(" FROM "); //$NON-NLS-1$
			
			if (hasAreaGroupBy) {
				sql.append("( SELECT bar.*, "); //$NON-NLS-1$
				StringBuilder sbend = new StringBuilder();
				for (String prefix : areaGroupByPrefix) {
					sql.append("smart.intersection(foo_"+ prefix + ".geom,"); //$NON-NLS-1$ //$NON-NLS-2$
					sbend.append(")"); //$NON-NLS-1$
				}
				sql.append("smart.buffer(t.geometry, " + ((PatrolValueItemAreaBuffer)patrolItem).getBufferValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(sbend);
				sql.append(" as bufferarea "); //$NON-NLS-1$
			}else {
				sql.append("( SELECT bar.*, smart.buffer(t.geometry," + ((PatrolValueItemAreaBuffer)patrolItem).getBufferValue() + ") as bufferarea"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			sql.append(" FROM ( SELECT distinct "); //$NON-NLS-1$
			sql.append(selectSql);
			sql.append(groupByInnerSql);
			if (groupByInnerSql.length() > 0){
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueSql);
			sql.append(" FROM "); //$NON-NLS-1$
			sql.append(fromSql);
			if (innerWhere.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(innerWhere);
			}
			sql.append(") bar"); //$NON-NLS-1$
			sql.append(" join smart.track t on t.uuid = bar.trackuuid "); //$NON-NLS-1$
			if (hasAreaGroupBy) {
				for (int i = 0; i < areaGroupByPrefix.size(); i ++) {
					String p = areaGroupByPrefix.get(i);
					String k = areaGroupByKeys.get(i);
				
					sql.append(" join smart.area_geometries foo_" + p + " on foo_" + p + ".uuid = bar." + k); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			sql.append(") foo"); //$NON-NLS-1$
		}else {
	
			sql.append(" FROM ( SELECT distinct "); //$NON-NLS-1$
			sql.append(selectSql);
			sql.append(groupByInnerSql);
			if (groupByInnerSql.length() > 0){
				sql.append(","); //$NON-NLS-1$
			}
			sql.append(valueSql);
			sql.append(" FROM "); //$NON-NLS-1$
			sql.append(fromSql);
			if (innerWhere.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(innerWhere);
			}
			sql.append(") foo"); //$NON-NLS-1$
		}
		if (groupBySql.length() > 0){
			sql.append(" GROUP BY " ); //$NON-NLS-1$
			sql.append(groupBySql);
		}
		
		//do something here with sql
		QueryPlugIn.logSql(sql.toString());
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
		
		if (attributeItem.getAttributeType() == AttributeType.NUMERIC
			|| attributeItem.getAttributeType().isGeometry()) {
				
			String field = "number_value"; //$NON-NLS-1$
			if (attributeItem.getGeometryProperty() != null) {
				field = attributeItem.getGeometryProperty().getDbField();
			}
			
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
			valueAggSql.append("." + field + ")"); //$NON-NLS-1$ //$NON-NLS-2$

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
			sql.append("." + field + " is not null and "); //$NON-NLS-1$ //$NON-NLS-2$
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
			QueryPlugIn.logSql(sql.toString());
			ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();
			return createValueResults(rs, groupBy, attributeItem.asString());
		} else if (attributeItem.getAttributeType() == AttributeType.MLIST) {
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
			sql.append(tableNamePrefix(WaypointObservationAttributeList.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttributeList.class));
			sql.append(".observation_attribute_uuid "); //$NON-NLS-1$
			
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(AttributeListItem.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeListItem.class));
			sql.append(".attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class));
			sql.append(".uuid "); //$NON-NLS-1$
			sql.append(" and "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeListItem.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttributeList.class));
			sql.append(".list_element_uuid "); //$NON-NLS-1$
			
			sql.append(" WHERE "); //$NON-NLS-1$
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
					case TIME:
						int mins = rs.getInt(rsindex++);
						int hrs = mins / 60;
						mins = mins % 60;
						key += String.format("%d:%02d", hrs, mins); //$NON-NLS-1$
						break;
					case KEY:
						key += rs.getString(rsindex++);
						break;
					case BOOLEAN: 
						key += ((Boolean)rs.getBoolean(rsindex++)).toString();
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
			ConservationAreaFilter caFilter,
			IProgressMonitor monitor) throws SQLException{
		
		HashMap<SummaryResultKey, Double> values1 = computeValueItem(c, s, groupBy, item.getPart1(), caFilter, true, monitor);
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		boolean filterValue2 = false;
		if (item.getPart2() instanceof PatrolValueItem && 
			PatrolQueryOptions.isGroupByFilterValueItem(  ((PatrolValueItem) item.getPart2()).getPatrolValueOption())){
				filterValue2 = true;
		}
		
		HashMap<SummaryResultKey, Double> values2 = null;
		if (!filterValue2){
			values2 = computeValueItem(c, s, new GroupByPart(new ArrayList<IGroupBy>()), item.getPart2(), caFilter, false, monitor);
			if (values2.values().size() != 1){
				throw new SQLException(Messages.DerbySummaryEngine_InvalidRateFilterComputation);
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
			values2 = computeValueItem(c, s, groupBy, item.getPart2(), caFilter, false, monitor);
			
			
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
	private List<String> areaGroupByKeys = new ArrayList<String>();
	
	private void createGroupBySql(GroupByPart groupBy,
			StringBuilder fromSql,
			StringBuilder groupBySql, 
			StringBuilder groupByInnerSql, IValueItem value, ConservationAreaFilter caFilter) throws SQLException{
		areaGroupByPrefix.clear();
		areaGroupByKeys.clear();
		int itemcnt = 1;
		boolean waypointAdd = false;
		boolean trackAdd = false;
		
		Set<Class<?>> usedTables = new HashSet<>();

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
					fromSql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
					fromSql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$					
					fromSql.append(areaPrefix + ".geom"); //$NON-NLS-1$
					fromSql.append(")"); //$NON-NLS-1$
					fromSql.append(" and "); //$NON-NLS-1$
					fromSql.append(areaPrefix + ".ca_uuid = x'" + UuidUtils.uuidToString(query.getConservationArea().getUuid()) + "' "); //$NON-NLS-1$ //$NON-NLS-2$
						
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
					groupByInnerSql.append(areaPrefix + ".keyid" + " as " + key + ","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					groupByInnerSql.append(areaPrefix + ".uuid" + " as " + key + "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					areaGroupByPrefix.add(areaPrefix);
					areaGroupByKeys.add(key + "uuid"); //$NON-NLS-1$
					
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
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(areaPrefix + ".ca_uuid = x'" + UuidUtils.uuidToString(query.getConservationArea().getUuid()) + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					fromSql.append(" and ");//$NON-NLS-1$
					String p1 = addParameterValue(agb.getAreaType().name());
					fromSql.append(areaPrefix + ".area_type = " + p1);//$NON-NLS-1$
					if (!(value instanceof PatrolValueItemAreaBuffer)) {
						fromSql.append(" and "); //$NON-NLS-1$
						fromSql.append("smart.trackIntersects("); //$NON-NLS-1$
						fromSql.append(tablePrefix(Track.class) + ".geometry, "); //$NON-NLS-1$
						fromSql.append(areaPrefix + ".geom"); //$NON-NLS-1$
						fromSql.append(")"); //$NON-NLS-1$
					}
					
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
					if (!usedTables.contains(PatrolLegMember.class)) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(PatrolLegMember.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(PatrolLegMember.class));
						fromSql.append(" on temp.pl_uuid = " + tablePrefix(PatrolLegMember.class) + ".patrol_leg_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
						usedTables.add(PatrolLegMember.class);
					}
				}else if (option == PatrolQueryOption.CM){
					if (!usedTables.contains(Waypoint.class)) {
						fromSql.append(" left join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(Waypoint.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(Waypoint.class));
						fromSql.append(" on temp.wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
						usedTables.add(Waypoint.class);
					}
				}else if (option == PatrolQueryOption.AGENCY || option == PatrolQueryOption.AGENCY_KEY || 
						option == PatrolQueryOption.RANK) {
					
					if (!usedTables.contains(PatrolLegMember.class)) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNames.get(PatrolLegMember.class));
						fromSql.append(" "); //$NON-NLS-1$
						fromSql.append(tablePrefix(PatrolLegMember.class));
						fromSql.append(" on temp.pl_uuid = " + tablePrefix(PatrolLegMember.class) + ".patrol_leg_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
						usedTables.add(PatrolLegMember.class);
					}
					if (!usedTables.contains(Employee.class)) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNamePrefix(Employee.class));
						fromSql.append(" on "); //$NON-NLS-1$
						fromSql.append(tablePrefix(Employee.class) + ".uuid "); //$NON-NLS-1$
						fromSql.append(" = "); //$NON-NLS-1$
						fromSql.append(tablePrefix(PatrolLegMember.class) + ".employee_uuid "); //$NON-NLS-1$
						usedTables.add(Employee.class);
					}
					if ((option == PatrolQueryOption.AGENCY || option == PatrolQueryOption.AGENCY_KEY) &&
							!usedTables.contains(Agency.class)) {
						fromSql.append(" join "); //$NON-NLS-1$
						fromSql.append(tableNamePrefix(Agency.class));
						fromSql.append(" on "); //$NON-NLS-1$
						fromSql.append(tablePrefix(Agency.class) + ".uuid "); //$NON-NLS-1$
						fromSql.append(" = "); //$NON-NLS-1$
						fromSql.append(tablePrefix(Employee.class) + ".agency_uuid "); //$NON-NLS-1$
						usedTables.add(Agency.class);
					}
					
					if (option == PatrolQueryOption.RANK) {
						if (!usedTables.contains(Rank.class)) {
							fromSql.append(" join "); //$NON-NLS-1$
							fromSql.append(tableNamePrefix(Rank.class));
							fromSql.append(" on "); //$NON-NLS-1$
							fromSql.append(tablePrefix(Rank.class) + ".uuid "); //$NON-NLS-1$
							fromSql.append(" = "); //$NON-NLS-1$
							fromSql.append(tablePrefix(Employee.class) + ".rank_uuid "); //$NON-NLS-1$
							usedTables.add(Rank.class);
						}
					}
				}else if (option.getType() == PatrolQueryOptionType.KEY){
					PatrolQueryOption op = option;
					fromSql.append(" join "); //$NON-NLS-1$
					fromSql.append(tableNamePrefix(op.getSourceClass()));
					fromSql.append(" on temp."); //$NON-NLS-1$
					fromSql.append(getUuidFieldName(op));
					fromSql.append(" = "); //$NON-NLS-1$
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
					groupByInnerSql.append("trim(cast(month(pld_patrol_day) as char(2))) || '/' || cast(year(pld_patrol_day) as char(4)) as datePart_" + itemcnt); //$NON-NLS-1$

				}else if (op.getClass().equals(PatrolStartMonthDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("trim(cast(month(p_start_date) as char(2))) || '/' || cast(year(p_start_date) as char(4)) as datePart_" + itemcnt); //$NON-NLS-1$
					
				}else if (op.getClass().equals(PatrolStartQuarterDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("cast(year(p_start_date) as char(4)) || '_' || cast(((month(p_start_date) - 1)/3) + 1 as char(1)) as datePart_" + itemcnt); //$NON-NLS-1$
					
				}else if (op.getClass().equals(PatrolEndQuarterDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("cast(year(p_end_date) as char(4)) || '_' || cast(((month(p_end_date) - 1)/3) + 1 as char(1)) as datePart_" + itemcnt); //$NON-NLS-1$
					
					
				}else if (op.getClass().equals(PatrolEndMonthDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("trim(cast(month(p_end_date) as char(2))) || '/' || cast(year(p_end_date) as char(4)) as datePart_" + itemcnt); //$NON-NLS-1$
				
				}else if (op.getClass().equals(YearDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("YEAR(pld_patrol_day) as datePart_" + itemcnt); //$NON-NLS-1$
					
				}else if (op.getClass().equals(QuarterDateGroupBy.class)){
					groupBySql.append("datePart_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("cast(year(pld_patrol_day) as char(4)) || '_' || cast(((month(pld_patrol_day) - 1)/3) + 1 as char(1)) as datePart_" + itemcnt); //$NON-NLS-1$
					
				}else if (op.getClass().equals(StartHourGroupBy.class)){
					groupBySql.append("startminute_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("pld_uuid,((hour(pld_start_time) * 60 + minute(pld_start_time)) / 30)* 30 as startminute_" + itemcnt); //$NON-NLS-1$
				}else if (op.getClass().equals(EndHourGroupBy.class)){
					groupBySql.append("endminute_" + itemcnt); //$NON-NLS-1$
					groupByInnerSql.append("pld_uuid,((hour(pld_end_time) * 60 + minute(pld_end_time)) / 30)* 30 as endminute_" + itemcnt); //$NON-NLS-1$
				}
			}else if (gb instanceof PatrolAttributeGroupBy){
				
				PatrolAttributeGroupBy option = ((PatrolAttributeGroupBy) gb);
				
				String valueprefix = tablePrefix(PatrolAttributeValue.class) + "_" + itemcnt; //$NON-NLS-1$
				String listprefix = tablePrefix(PatrolAttributeListItem.class) + "_" + itemcnt; //$NON-NLS-1$
				String treeprefix = tablePrefix(PatrolAttributeTreeNode.class) + "_" + itemcnt; //$NON-NLS-1$
				String attributeprefix = tablePrefix(PatrolAttribute.class) + "_" + itemcnt; //$NON-NLS-1$
				
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableName(PatrolAttributeValue.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(valueprefix);
				fromSql.append(" on temp.p_uuid = " + valueprefix + ".patrol_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				
				fromSql.append(" join "); //$NON-NLS-1$
				fromSql.append(tableName(PatrolAttribute.class));
				fromSql.append(" "); //$NON-NLS-1$
				fromSql.append(attributeprefix);
				fromSql.append(" on "); //$NON-NLS-1$
				fromSql.append(valueprefix + ".patrol_attribute_uuid = "); //$NON-NLS-1$
				fromSql.append(attributeprefix + ".uuid  AND "); //$NON-NLS-1$
				String p1 = addParameterValue(option.getAttributeKey());
				fromSql.append(attributeprefix + ".keyid = " + p1); //$NON-NLS-1$
				
				fromSql.append(" join "); //$NON-NLS-1$
				
				if (option.getAttributeType() == Attribute.AttributeType.LIST) {
					fromSql.append(tableName(PatrolAttributeListItem.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(listprefix);
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(listprefix);
					fromSql.append(".uuid = " ); //$NON-NLS-1$
					fromSql.append(valueprefix);
					fromSql.append(".list_item_uuid " ); //$NON-NLS-1$
					
					groupByInnerSql.append(listprefix + ".keyid" + " as gp_" + itemcnt); //$NON-NLS-1$ //$NON-NLS-2$
				}else if (option.getAttributeType() == Attribute.AttributeType.TREE) {
					fromSql.append(tableName(PatrolAttributeTreeNode.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(treeprefix);
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(treeprefix);
					fromSql.append(".uuid = " ); //$NON-NLS-1$
					fromSql.append(valueprefix);
					fromSql.append(".tree_node_uuid " ); //$NON-NLS-1$
					
					groupByInnerSql.append("smart.trimHkeyToLevel("); //$NON-NLS-1$
					groupByInnerSql.append(option.getLevel() + ", "); //$NON-NLS-1$
					groupByInnerSql.append(treeprefix + ".hkey"); //$NON-NLS-1$
					groupByInnerSql.append(") as gp_" + itemcnt); //$NON-NLS-1$
				}
				groupBySql.append("gp_" + itemcnt); //$NON-NLS-1$
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
				}else if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.MLIST){
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
				
				}else if (((AttributeGroupBy)gb).getAttributeType() == AttributeType.MLIST){
					
					fromSql.append(tableNames.get(WaypointObservationAttributeList.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(WaypointObservationAttributeList.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(WaypointObservationAttribute.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".uuid = "); //$NON-NLS-1$
					fromSql.append(tablePrefix(WaypointObservationAttributeList.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".observation_attribute_uuid"); //$NON-NLS-1$
					
					fromSql.append(" JOIN "); //$NON-NLS-1$
					
					fromSql.append(tableNames.get(AttributeListItem.class));
					fromSql.append(" "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(" on "); //$NON-NLS-1$
					fromSql.append(tablePrefix(AttributeListItem.class) + "_" + itemcnt); //$NON-NLS-1$
					fromSql.append(".uuid ="); //$NON-NLS-1$
					fromSql.append(tablePrefix(WaypointObservationAttributeList.class) + "_" + itemcnt); //$NON-NLS-1$
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
				PatrolContributionFinder.addGroupBySql((IExtensionGroupBy)gb, fromSql, 
						groupBySql, groupByInnerSql, 
						value, caFilter, itemcnt, this);
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
		case NUM_CUSTOM:
			return " count(custom_pdate)"; //$NON-NLS-1$
		case NUM_NIGHTS:
			return " count(pld_patrol_day) - count(distinct p_uuid) "; //$NON-NLS-1$
		case DISTANCE:
		case DISTANCE_TOTAL:
			return "sum(distance)"; //$NON-NLS-1$
		case AREA_BUFFER:
			return "smart.unionarea(bufferarea)"; //$NON-NLS-1$
		case NUM_PATROLHOURS:
		case NUM_PATROLHOURS_TOTAL:
			if (!hasAreaGroupBy) {
				return "sum(({fn timestampdiff(SQL_TSI_SECOND, pld_start_time, pld_end_time)} / ( 3600.0 )))"; //$NON-NLS-1$
			}else{
				return "sum(hours)"; //$NON-NLS-1$
			}
		case NUM_FIELDHOURS:
		case NUM_FIELDHOURS_TOTAL:
			if (!hasAreaGroupBy){
				return "sum(({fn timestampdiff(SQL_TSI_SECOND, pld_start_time, pld_end_time)} / ( 3600.0 )) - (case when pld_rest_minutes is null then 0 else pld_rest_minutes end / 60.0))"; //$NON-NLS-1$
			}else{
				return "sum(hours)"; //$NON-NLS-1$
			}
		case NUM_MEMBERS:
			return "count(pl_member)"; //$NON-NLS-1$
		case MAN_HOURS:
		case MAN_HOURS_TOTAL:
			if (!hasAreaGroupBy){
				return "sum(({fn timestampdiff(SQL_TSI_SECOND, pld_start_time, pld_end_time)} / ( 3600.0 )) - (case when pld_rest_minutes is null then 0 else pld_rest_minutes end  / 60.0))"; //$NON-NLS-1$
			}else{
				return "sum(hours)"; //$NON-NLS-1$
			}
		case MAN_DAYS:
		case MAN_DAYS_TOTAL:
			return "count(pld_patrol_day) "; //$NON-NLS-1$
		case PATROLHOURS_TRACK:
			throw new UnsupportedOperationException();
		
		}
		assert false;
		return ""; //$NON-NLS-1$
	}
	
	private String getFieldName(PatrolValueItem item, boolean hasAreaGroupBy, boolean includeNoData, StringBuilder sbWhere){
		PatrolValueOption option = item.getPatrolValueOption();
		switch(option){
		case NUM_PATROLS:
		case NUM_PATROLS_TOTAL:
			return "p_uuid"; //$NON-NLS-1$
		case NUM_DAYS:
		case NUM_DAYS_TOTAL:
			if (!includeNoData) {
				if (sbWhere.length() > 0) sbWhere.append(" AND "); //$NON-NLS-1$
				sbWhere.append(" has_data is not null "); //$NON-NLS-1$
			}
			return "pld_patrol_day"; //$NON-NLS-1$
		case NUM_CUSTOM:
			return " custom_pdate"; //$NON-NLS-1$
		case NUM_NIGHTS:
			if (!includeNoData) {
				if (sbWhere.length() > 0) sbWhere.append(" AND "); //$NON-NLS-1$
				sbWhere.append(" has_data is not null "); //$NON-NLS-1$
			}
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
		case AREA_BUFFER:
			StringBuilder sql = new StringBuilder();
			sql.append(tablePrefix(Track.class) + ".uuid as trackuuid"); //$NON-NLS-1$
			return sql.toString();
		case NUM_PATROLHOURS:
		case NUM_FIELDHOURS:
		case NUM_PATROLHOURS_TOTAL:
		case NUM_FIELDHOURS_TOTAL:
			if (!hasAreaGroupBy){
				if (!includeNoData) {
					if (sbWhere.length() > 0) sbWhere.append(" AND "); //$NON-NLS-1$
					sbWhere.append(" has_data is not null "); //$NON-NLS-1$
				}
				return tablePrefix(PatrolLegDay.class) + ".start_time as pld_start_time," + //$NON-NLS-1$
					tablePrefix(PatrolLegDay.class) + ".end_time as pld_end_time," + //$NON-NLS-1$
					tablePrefix(PatrolLegDay.class) + ".rest_minutes as pld_rest_minutes "; //$NON-NLS-1$
			}else{
				//we don't need to check include no data here because 
				//this computation is based on the track which means their is data
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
			return tablePrefix(PatrolLegMember.class) + "_value.employee_uuid as pl_member"; //$NON-NLS-1$
		case MAN_HOURS:
		case MAN_HOURS_TOTAL:
			if (!hasAreaGroupBy){
				if (!includeNoData) {
					if (sbWhere.length() > 0) sbWhere.append(" AND "); //$NON-NLS-1$
					sbWhere.append(" has_data is not null "); //$NON-NLS-1$
				}
				return tablePrefix(PatrolLegDay.class) + ".start_time as pld_start_time, " + //$NON-NLS-1$
						tablePrefix(PatrolLegDay.class) + ".end_time as pld_end_time, " + //$NON-NLS-1$
						tablePrefix(PatrolLegDay.class) + ".rest_minutes as pld_rest_minutes," + //$NON-NLS-1$
						tablePrefix(PatrolLegMember.class) + "_value.employee_uuid as pl_member "; //$NON-NLS-1$
			}else{
				//we don't need to check include no data here because 
				//this computation is based on the track which means their is data
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
				valueSql.append("_value.employee_uuid as pl_member"); //$NON-NLS-1$
				return valueSql.toString();
			}
		case MAN_DAYS:
		case MAN_DAYS_TOTAL:
			if (!includeNoData) {
				if (sbWhere.length() > 0) sbWhere.append(" AND "); //$NON-NLS-1$
				sbWhere.append(" has_data is not null ");	 //$NON-NLS-1$
			}	
			return "pld_patrol_day, " + tablePrefix(PatrolLegMember.class) + "_value.employee_uuid as pl_member"; //$NON-NLS-1$ //$NON-NLS-2$
		case PATROLHOURS_TRACK:
			throw new UnsupportedOperationException();
		}
		//should not get here
		return null;
	}
	
	private String getUuidFieldName(PatrolQueryOption gb){
		switch(gb){
		case TEAM_KEY:
			return "p_team_uuid"; //$NON-NLS-1$
		case MANDATE_KEY:
			return "pl_mandate_uuid"; //$NON-NLS-1$
		case PATROL_TRANSPORT_TYPE_KEY:
			return "pl_transport_uuid"; //$NON-NLS-1$
		case PATROL_TYPE:
			return "p_type_uuid"; //$NON-NLS-1$
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
			return "pl_mandate_uuid"; //$NON-NLS-1$
		case PATROL_TYPE:
			return tablePrefix.get(PatrolType.class) + ".keyid"; //$NON-NLS-1$
		case PATROL_TRANSPORT_TYPE:
			return "pl_transport_uuid"; //$NON-NLS-1$
		case ARMED:
			return "p_is_armed"; //$NON-NLS-1$
		case PILOT:
			return "plm_pilot"; //$NON-NLS-1$
		case LEADER:
			return "plm_leader"; //$NON-NLS-1$
		case EMPLOYEE:
			return "employee_uuid"; //$NON-NLS-1$
		case CONSERVATION_AREA:
			return "temp.ca_uuid"; //$NON-NLS-1$
		case CM:
			return tablePrefix.get(Waypoint.class) + ".source_cm_uuid"; //$NON-NLS-1$
		case TEAM_KEY:
			return tablePrefix.get(Team.class) + ".keyid"; //$NON-NLS-1$
		case MANDATE_KEY:
			return tablePrefix.get(PatrolMandate.class) + ".keyid"; //$NON-NLS-1$
		case PATROL_TRANSPORT_TYPE_KEY:
			return tablePrefix.get(PatrolTransportType.class) + ".keyid"; //$NON-NLS-1$
		case RANK:
			return tablePrefix.get(Rank.class) + ".uuid"; //$NON-NLS-1$
		case AGENCY:
			return tablePrefix.get(Agency.class) + ".uuid"; //$NON-NLS-1$
		case AGENCY_KEY:
			return tablePrefix.get(Agency.class) + ".keyid"; //$NON-NLS-1$
			
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
	public static void getHeaderInfo(PatrolSummaryQuery query, SummaryQueryResult results, Session session) throws Exception{
		
		// value headers
		ValuePart vp = query.getQueryDefinition().getValuePart();
		for (IValueItem item : vp.getValueItems()){
			SummaryHeader header = new SummaryHeader(
					PatrolValueItemLabelProvider.INSTANCE.getName(item, session),
					PatrolValueItemLabelProvider.INSTANCE.getFullName(item, session),
					item.asString(), true);
			header.setUiFormatter(item.getFormatter(session,ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas())));
			results.addValueHeader(header);
		}
		
		DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
		
		for (IGroupBy item : query.getQueryDefinition().getRowGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(dFilter.getDateFilterOption());
			}
			List<ListItem> items = PatrolDropItemFactory.INSTANCE.findViewer(item).getItems(session);
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
			List<ListItem> items = PatrolDropItemFactory.INSTANCE.findViewer(item).getItems(session);
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
		sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".station_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".team_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".patrol_type_uuid, "); //$NON-NLS-1$
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
			sql.append("cast(null as char for bit data),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	//wpob_uuid //$NON-NLS-1$
		}
		sql.append(tablePrefix(PatrolLegMember.class) + "_leader.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegMember.class) + "_pilot.employee_uuid "); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_id varchar(256),"); //$NON-NLS-1$
		sql.append("p_station_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_team_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_objective varchar(8192),"); //$NON-NLS-1$
		sql.append("pl_mandate_uuid  char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_type_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_is_armed boolean,"); //$NON-NLS-1$
		sql.append("p_start_date date,"); //$NON-NLS-1$
		sql.append("p_end_date date,"); //$NON-NLS-1$
		sql.append("pl_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pl_id varchar(50),"); //$NON-NLS-1$
		sql.append("pl_transport_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pl_start_date date,"); //$NON-NLS-1$
		sql.append("pl_end_date date,"); //$NON-NLS-1$
		sql.append("pld_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pld_patrol_day date,"); //$NON-NLS-1$
		sql.append("pld_start_time time,"); //$NON-NLS-1$
		sql.append("pld_end_time time,"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_leader char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_pilot char(16) for bit data"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	
	
	private void addHasDataColumn(Connection c, String tableName) throws SQLException{
		if (tablesWithNoData.contains(tableName)) return;
		tablesWithNoData.add(tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("ALTER TABLE " + tableName + " ADD COLUMN has_data boolean "); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().executeUpdate(sql.toString());
		
		String temp = createTempTableName();
		sql = new StringBuilder();
		sql.append("CREATE TABLE " + temp + " (patrol_leg_uuid char(16) for bit data, patrol_day date, has_data boolean)"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().executeUpdate(sql.toString());
		
		sql = new StringBuilder();
		sql.append("INSERT INTO " + temp); //$NON-NLS-1$
		sql.append(" SELECT a.patrol_leg_uuid, a.patrol_day, max(case when b.leg_day_uuid is null and c.uuid is null then false else true end ) as has_data "); //$NON-NLS-1$
		sql.append(" FROM smart.patrol_leg_day a "); //$NON-NLS-1$
		sql.append(" JOIN  "); //$NON-NLS-1$
		sql.append( tableName );
		sql.append( " d on a.uuid = d.pld_uuid " ); //$NON-NLS-1$
		sql.append(" LEFT JOIN smart.patrol_waypoint b on a.uuid = b.leg_day_uuid "); //$NON-NLS-1$
		sql.append(" LEFT JOIN smart.track c on a.uuid = c.patrol_leg_day_uuid "); //$NON-NLS-1$
		sql.append(" group by a.patrol_leg_uuid, a.patrol_day"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().executeUpdate(sql.toString());
		
		sql = new StringBuilder();
		sql.append("UPDATE " + tableName + " set has_data = "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("( SELECT foo.has_data FROM "); //$NON-NLS-1$
		sql.append(temp);
		sql.append(" foo " ); //$NON-NLS-1$
		sql.append(" WHERE foo.has_data and foo.patrol_leg_uuid = " ); //$NON-NLS-1$
		sql.append(tableName);
		sql.append(".pl_uuid AND "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append(".pld_patrol_day = foo.patrol_day ) "); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().executeUpdate(sql.toString());
		
	}

	@Override
	public void createTemporaryTableIndexes(Connection c, String tableName) throws SQLException {
		super.createObsIndex(c, tableName);
		super.createWpIndex(c, tableName);
	}

}
