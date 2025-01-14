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
package org.wcs.smart.asset.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeploymentDisruption;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.AssetValueItemLabelProvider;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetDropItemFactory;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.model.AssetValueOption;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.asset.query.parser.internal.summary.AssetValueItem;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationArea;
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
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.GeometrySummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryResultKey;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.summary.AreaGroupBy;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.IValueItem.ValueType;
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
public class AssetDeploymentSummaryEngine extends AssetQueryEngine{

	private SummaryQueryResult sumResults = null;
	private HashMap<String, HashMap<SummaryResultKey, Double>> cachedValueToResults = new HashMap<>();
	
	private DateFilter localDateFilter;
	private ConservationAreaFilter caFilter;
	
	private Session session;
	
	private AssetSummaryQuery query;
	private SumQueryDefinition def = null;
	
	private String filterTable;
	
	@Override
	public boolean canExecute(String querytype) {
		return AssetSummaryQuery.DEPLOYMENT_SUMMARY_KEY.equals(querytype);
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
	 * match the required filter.  This table contains all the asset
	 * to waypoint attributes and the observation id.  IT does 
	 * not contain any of the matched attributes.
	 * 
	 * 3) For each asset value to compute the results 
	 * are commputed and added to the results.
	 *  
	 */
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{

		query = (AssetSummaryQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		
		
		try{
			def = query.getQueryDefinition();
		}catch (Exception ex){
			throw new SQLException (ex);
		}

		//determine the type or query results to return
		if (AssetSummaryQuery.canAddGeometry(def)) {
			sumResults = new GeometrySummaryQueryResult();
		}else {
			sumResults = new SummaryQueryResult();
		}
		
		cachedValueToResults = new HashMap<String, HashMap<SummaryResultKey, Double>>();
		
		//create a date filter that caches the dates so the same
		//dates are used for all parts of the query;
		//otherwise different date filters will be computed
		//for different parts of the queries
		localDateFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), 
				new CachingDateFilter(query.getDateFilter().getDateFilterOption()));
		
		//ca filter
		caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), 
				SmartDB.getConservationAreaConfiguration().getConservationAreas());
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbySummaryEngine_Progress_RunningQuery, 2);
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				//need to make sure we cleanup all temp tables correctly
				c.setAutoCommit(true);
				

				try {
					//computer header details
					progress.subTask(Messages.DerbySummaryEngine_Progress_LoadingHeaders);
					progress.split(1);
					try{
						getHeaderInfo(query, sumResults, session);
					}catch (Exception ex){
						throw new SQLException(ex);
					}
					
					//create filter table
					filterTable = createTempTableName();
					AssetDeploymentFilterProcessor processor = new AssetDeploymentFilterProcessor(filterTable, AssetDeploymentSummaryEngine.this);
					try{
						processor.processFilter(c, def.getValueFilter() == null ? EmptyFilter.INSTANCE : def.getValueFilter().getFilter(), localDateFilter, caFilter, false, false, monitor);
					}finally{
						processor.dropTemporaryTables(c);
					}
							
					addAreaGroupBy(c);
					
					HashMap<SummaryResultKey, Double> data = computeSummaryValues(c, session, progress.split(1));
					
					
					progress.checkCanceled();
					if (data == null) return;
					
					sumResults.setData(data);
					
					if (sumResults instanceof GeometrySummaryQueryResult) {
						addCoordinatesForMap(def, (GeometrySummaryQueryResult)sumResults, session, caFilter);
					}
					
				}catch (OperationCanceledException ex) {
					return;
				} finally {
					// ensure temporary tables get dropped
					dropTableInternal(c);
					c.setAutoCommit(false);
				}
			}
		});
		return sumResults ;
	}

	private void addCoordinatesForMap(SumQueryDefinition ldef, GeometrySummaryQueryResult results, Session session, ConservationAreaFilter caFilter) {
		if (!AssetSummaryQuery.canAddGeometry(ldef)) return;
		
		AssetGroupBy gb = (AssetGroupBy)ldef.getRowGroupByPart().getGroupBys().get(0);
		if (gb.getOption() == AssetFilterOption.STATION) {
			try {
				List<AssetStation> stations = session.createQuery("FROM AssetStation WHERE conservationArea.uuid IN (:cas)", AssetStation.class) //$NON-NLS-1$
						.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
						.list();
				for (AssetStation s : stations) {
					results.addCoordinateDetails(s.getUuid(), new Coordinate(s.getX(), s.getY()));
				}
			}catch (Exception ex) {
				AssetQueryPlugIn.log(ex.getMessage(), ex);
			}
			
		}else if (gb.getOption() == AssetFilterOption.STATIONLOCATION) {
			try {
				List<AssetStationLocation> locations = session.createQuery("SELECT l FROM AssetStationLocation l join l.station s WHERE s.conservationArea.uuid IN (:cas)", AssetStationLocation.class) //$NON-NLS-1$
						.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
						.list();
				for (AssetStationLocation s : locations) {
					results.addCoordinateDetails(s.getUuid(), new Coordinate(s.getX(), s.getY()));
				}
			}catch (Exception ex) {
				AssetQueryPlugIn.log(ex.getMessage(), ex);
			}
		}		
	}
	
	private void addAreaGroupBy(Connection c) throws SQLException {
		//if there is an area group by add an area key to the column to the filter table
		Set<AreaType> areas = new HashSet<>();
		for (IGroupBy gb : def.getRowGroupByPart().getGroupBys()) {
			if (gb instanceof AreaGroupBy) areas.add(((AreaGroupBy)gb).getAreaType());
		}
		for (IGroupBy gb : def.getColumnGroupByPart().getGroupBys()) {
			if (gb instanceof AreaGroupBy) areas.add(((AreaGroupBy)gb).getAreaType());
		}
		
		ConservationArea ca = query.getConservationArea();
		
		for (AreaType at : areas) {
		
			clearParameters();
			
			String fieldName = "area_" + at.name(); //$NON-NLS-1$
			
			StringBuilder sb = new StringBuilder();
			
			sb.append("ALTER TABLE " ); //$NON-NLS-1$
			sb.append(filterTable);
			sb.append(" ADD column " + fieldName); //$NON-NLS-1$
			sb.append(" varchar(128)"); //$NON-NLS-1$
			QueryPlugIn.logSql(sb.toString());
			c.createStatement().execute(sb.toString());
			
			sb = new StringBuilder();
			String p1 = addParameterValue(at.name());
			String p2 = addParameterValue(ca.getUuid());
			
			sb.append("SELECT "); //$NON-NLS-1$
			sb.append(tablePrefix(AssetStationLocation.class) + ".uuid, "); //$NON-NLS-1$
			sb.append(tablePrefix(Area.class) + ".keyid"); //$NON-NLS-1$
			sb.append(" FROM "); //$NON-NLS-1$
			sb.append(tableNamePrefix(AssetStationLocation.class));
			sb.append(", "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Area.class));
			sb.append(" WHERE "); //$NON-NLS-1$
			sb.append(tablePrefix(Area.class) + ".area_type = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" AND "); //$NON-NLS-1$
			sb.append("smart.pointInPolygon( " + tablePrefix(AssetStationLocation.class) + ".x, "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append( tablePrefix(AssetStationLocation.class) + ".y, null, null, " + tablePrefix(Area.class) + ".geom)"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" AND " + tablePrefix(Area.class) + ".ca_uuid = " + p2 + " " ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			StringBuilder update = new StringBuilder();
			update.append("UPDATE "); //$NON-NLS-1$
			update.append(filterTable);
			update.append(" SET "); //$NON-NLS-1$
			update.append(fieldName);
			update.append(" = ? WHERE station_location_uuid = ?"); //$NON-NLS-1$
			
			PreparedStatement ps = c.prepareStatement(update.toString());
			
			QueryPlugIn.logSql(sb.toString());
			try(PreparedStatement selectps = parseQueryString(c, sb.toString())){
				try(ResultSet rs = selectps.executeQuery()){
					while(rs.next()) {
						byte[] locationUuid = rs.getBytes(1);
						String keyid = rs.getString(2);
				
						ps.setString(1, keyid);
						ps.setBytes(2, locationUuid);
						
						ps.executeUpdate();
					}
				}
			}
		}
	}
	
	@Override
	public void dropTables(Connection c){
	}
	
	private void dropTableInternal(Connection c){
		if (filterTable != null){
			dropTable(c, filterTable);
			filterTable= null;
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
			Session s, IProgressMonitor monitor) throws SQLException{
		
		SubMonitor progress = SubMonitor.convert(monitor, def.getValuePart().getValueItems().size());
		
		HashMap<SummaryResultKey, Double> results = new HashMap<SummaryResultKey, Double>();
		for (IValueItem it : def.getValuePart().getValueItems()){
			progress.subTask(MessageFormat.format(Messages.DerbySummaryEngine_ProgressValueProgressLabel,it.asString()));
			HashMap<SummaryResultKey, Double> data = computeValueItem(c, s, it, progress.split(1)) ; 
			if (data != null) results.putAll( data );	
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
			Connection c, Session s,
			IValueItem it, 
			IProgressMonitor monitor) throws SQLException {
		
		List<IGroupBy> all = new ArrayList<IGroupBy>();
		all.addAll(def.getColumnGroupByPart().getGroupBys());
		all.addAll(def.getRowGroupByPart().getGroupBys());
		GroupByPart groupBy = new GroupByPart(all);
		
		monitor.subTask(MessageFormat.format(Messages.DerbySummaryEngine_ProgressValueProgressLabel,it.asString()));
		String cacheKey = it.asString() + "_" + groupBy.asString() + "_" + filterTable; //$NON-NLS-1$ //$NON-NLS-2$
		HashMap<SummaryResultKey, Double> results = cachedValueToResults.get(cacheKey); 
		if (results != null){
			return results;
		}
		if (it instanceof AttributeValueItem){
			if (((AttributeValueItem)it).getAttributeType() == AttributeType.NUMERIC) {
				results = getNumberAttributeValue(filterTable, c, s, groupBy, (AttributeValueItem)it, caFilter);
			}else {
				results = getAttributeValue(filterTable, c, s, groupBy, (AttributeValueItem)it, caFilter);
			}
		}else if (it instanceof CategoryValueItem){
			results = getCategoryValue(filterTable, c, s, groupBy, (CategoryValueItem)it, caFilter);
		}else if (it instanceof AssetValueItem) {
			results = getAssetValue(filterTable, c, s, groupBy, (AssetValueItem)it, caFilter);
		}
		if (results != null){
			cachedValueToResults.put(cacheKey, results); 
		}
		
		return results;
	}
	
	/*
	 * 
	 */
	private HashMap<SummaryResultKey, Double> getNumberAttributeValue(
			String dataTable,
			Connection c, Session s, 
			GroupByPart groupBy, 
			AttributeValueItem attributeItem, ConservationAreaFilter caFilter) throws SQLException{
		
		clearParameters();
		
		StringBuilder sb = new StringBuilder();
		StringBuilder groupBySql = new StringBuilder();
		StringBuilder outer = new StringBuilder();
		
		outer.append("SELECT "); //$NON-NLS-1$
		
		sb.append(" FROM (SELECT distinct "); //$NON-NLS-1$
		
		boolean joinAsset = false;
		boolean joinStation = false;
		int cnt = 0;
		for(IGroupBy gb : groupBy.getGroupBys()) {
			cnt++;
			if (gb instanceof AssetGroupBy) {
				AssetGroupBy agb = (AssetGroupBy) gb;
				
				switch(agb.getOption()) {
				case ASSET:
					sb.append("filter.asset_uuid as asset_uuid"); //$NON-NLS-1$
					groupBySql.append("foo.asset_uuid"); //$NON-NLS-1$
					outer.append("foo.asset_uuid"); //$NON-NLS-1$
					break;
				case ASSETTYPE:
					joinAsset = true;
					sb.append(tablePrefix(Asset.class) + ".asset_type_uuid as asset_type_uuid"); //$NON-NLS-1$
					groupBySql.append("foo.asset_type_uuid"); //$NON-NLS-1$
					outer.append("foo.asset_type_uuid"); //$NON-NLS-1$
					break;
				case CONSERVATION_AREA:
					joinAsset = true;
					sb.append(tablePrefix(Asset.class) + ".ca_uuid as ca_uuid"); //$NON-NLS-1$
					groupBySql.append("foo.ca_uuid"); //$NON-NLS-1$
					break;
				case STATION:
					joinStation = true;
					sb.append(tablePrefix(AssetStation.class) + ".uuid as station_uuid"); //$NON-NLS-1$
					groupBySql.append("foo.station_uuid"); //$NON-NLS-1$
					outer.append("foo.station_uuid"); //$NON-NLS-1$

					break;
				case STATIONLOCATION:
					sb.append("filter.station_location_uuid as station_location_uuid"); //$NON-NLS-1$
					groupBySql.append("foo.station_location_uuid"); //$NON-NLS-1$
					outer.append("foo.station_location_uuid"); //$NON-NLS-1$
					break;
					
				default:
					throw new SQLException(MessageFormat.format("Asset group by {0} not supported.", agb.getOption().toString() )); //$NON-NLS-1$
				}
			}else if (gb instanceof AreaGroupBy) {
				String fieldname = "area_"+ ((AreaGroupBy)gb).getAreaType().name() ; //$NON-NLS-1$
				String fieldname2 = fieldname + "_" + cnt; //$NON-NLS-1$
				sb.append("filter." + fieldname + " as " + fieldname2 ); //$NON-NLS-1$ //$NON-NLS-2$
				groupBySql.append("foo." + fieldname2); //$NON-NLS-1$
				outer.append("foo." + fieldname2 ); //$NON-NLS-1$
			}else {
				throw new SQLException(MessageFormat.format("Cannot group by {0} for Asset Deployment Summary query", gb.asString() )); //$NON-NLS-1$
			}
			
			sb.append(","); //$NON-NLS-1$
			groupBySql.append(","); //$NON-NLS-1$
			outer.append(","); //$NON-NLS-1$
			
		}

		
		if (attributeItem.getAttributeType() == AttributeType.NUMERIC) {
			//we only want to count this once
			outer.append(attributeItem.getAggregationKey() + "(number_value)"); //$NON-NLS-1$
			sb.append(tablePrefix(WaypointObservationAttribute.class) + ".number_value as number_value, "); //$NON-NLS-1$
			sb.append(tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid as ob_uuid"); //$NON-NLS-1$
			
		}else {
			throw new IllegalStateException();
		}

		
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append( dataTable );
		sb.append(" filter "); //$NON-NLS-1$
		
		if (joinStation) {
			sb.append( " JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(AssetStationLocation.class) + " on filter.station_location_uuid = " + tablePrefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append( " JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(AssetStation.class) + " on " + tablePrefix(AssetStation.class) + ".uuid = " + tablePrefix(AssetStationLocation.class) + ".station_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (joinAsset) {
			sb.append( " JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Asset.class) + " on filter.asset_uuid = " + tablePrefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(AssetWaypoint.class) );
		sb.append(" ON " + tablePrefix(AssetWaypoint.class) + ".asset_deployment_uuid = filter.deployment_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Waypoint.class) );
		sb.append(" ON " + tablePrefix(Waypoint.class) + ".uuid = " + tablePrefix(AssetWaypoint.class) + ".wp_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservationGroup.class) );
		sb.append(" ON " + tablePrefix(WaypointObservationGroup.class) + ".wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservation.class) );
		sb.append(" ON " + tablePrefix(WaypointObservation.class) + ".wp_group_uuid = " + tablePrefix(WaypointObservationGroup.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservationAttribute.class));
		sb.append(" ON " + tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid = " + tablePrefix(WaypointObservation.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Attribute.class));
		sb.append(" ON " + tablePrefix(Attribute.class) + ".uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		StringBuilder where = new StringBuilder();
		
		where.append(" WHERE "); //$NON-NLS-1$
		String p0 = addParameterValue(attributeItem.getAttributeKey());
		where.append(tablePrefix(Attribute.class) + ".keyid = " + p0 ); //$NON-NLS-1$
		
		if (attributeItem.getCategoryKey() != null) {
			sb.append(" JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Category.class) );
			sb.append(" ON " + tablePrefix(Category.class) + ".uuid = " + tablePrefix(WaypointObservation.class) + ".category_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			String p1 = addParameterValue(attributeItem.getCategoryKey()); 
			String p2 = addParameterValue(attributeItem.getCategoryKey().substring(0,  attributeItem.getCategoryKey().length() -1) + "/"); //$NON-NLS-1$
			where.append(" AND ");	 //$NON-NLS-1$
			where.append("( " + tablePrefix(Category.class) + ".hkey >= "+ p1 + " and " + tablePrefix(Category.class) + ".hkey < " + p2 + ") "); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$	
		}
		
		sb.append(where);
		
		outer.append(sb);
		outer.append(") foo"); //$NON-NLS-1$
		
		if (groupBySql.length() != 0) {
			outer.append(" GROUP BY "); //$NON-NLS-1$
			outer.append(groupBySql.substring(0, groupBySql.length() - 1));
		}
		
		QueryPlugIn.logSql(outer.toString());
		ResultSet rs = parseQueryString(c, outer.toString()).executeQuery();
		return createValueResults(rs, groupBy, attributeItem.asString());
	}
	/*
	 * 
	 */
	private HashMap<SummaryResultKey, Double> getAttributeValue(
			String dataTable,
			Connection c, Session s, 
			GroupByPart groupBy, 
			AttributeValueItem attributeItem, ConservationAreaFilter caFilter) throws SQLException{
		
		clearParameters();
		
		StringBuilder sb = new StringBuilder();
		StringBuilder gbsql = new StringBuilder();
		StringBuilder joinSql = new StringBuilder();
		
		sb.append("SELECT "); //$NON-NLS-1$
		
		
		createGroupBySql(groupBy, sb, gbsql, joinSql);
		
		if (attributeItem.getAttributeType() == AttributeType.TREE ||
				attributeItem.getAttributeType() == AttributeType.LIST ||
				attributeItem.getAttributeType() == AttributeType.MLIST ) {
			
			if (attributeItem.getValueType() == ValueType.WAYPOINT) {
				sb.append(" COUNT(distinct "); //$NON-NLS-1$
				sb.append(tablePrefix(Waypoint.class) + ".uuid )"); //$NON-NLS-1$
			}else if (attributeItem.getValueType() == ValueType.OBSERVATION) {
				sb.append(" COUNT(distinct "); //$NON-NLS-1$
				sb.append(tablePrefix(WaypointObservation.class) + ".uuid )"); //$NON-NLS-1$
			}else {
				throw new IllegalStateException("category value item not supported: " + attributeItem.asString()); //$NON-NLS-1$
			}
			
		}else {
			throw new IllegalStateException(MessageFormat.format("Cannot compute value for attribute type {0}", attributeItem.getAttributeType())); //$NON-NLS-1$
		}

		
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append( dataTable );
		sb.append(" filter "); //$NON-NLS-1$
		sb.append(joinSql);
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(AssetWaypoint.class) );
		sb.append(" ON " + tablePrefix(AssetWaypoint.class) + ".asset_deployment_uuid = filter.deployment_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Waypoint.class) );
		sb.append(" ON " + tablePrefix(Waypoint.class) + ".uuid = " + tablePrefix(AssetWaypoint.class) + ".wp_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservationGroup.class) );
		sb.append(" ON " + tablePrefix(WaypointObservationGroup.class) + ".wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservation.class) );
		sb.append(" ON " + tablePrefix(WaypointObservation.class) + ".wp_group_uuid = " + tablePrefix(WaypointObservationGroup.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservationAttribute.class));
		sb.append(" ON " + tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid = " + tablePrefix(WaypointObservation.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Attribute.class));
		sb.append(" ON " + tablePrefix(Attribute.class) + ".uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		StringBuilder where = new StringBuilder();
		
		where.append(" WHERE "); //$NON-NLS-1$
		String p0 = addParameterValue(attributeItem.getAttributeKey());
		where.append(tablePrefix(Attribute.class) + ".keyid = " + p0 ); //$NON-NLS-1$
		
		if (attributeItem.getAttributeType() == AttributeType.LIST) {
			sb.append(" JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(AttributeListItem.class));
			sb.append(" ON " + tablePrefix(AttributeListItem.class) + ".uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
			String p1 = addParameterValue(attributeItem.getItemKey());
			where.append(" AND "); //$NON-NLS-1$
			where.append(tablePrefix(AttributeListItem.class) + ".keyid = " + p1); //$NON-NLS-1$
			
		}
		
		if (attributeItem.getAttributeType() == AttributeType.MLIST){
			sb.append(" JOIN "); //$NON-NLS-1$
			sb.append(tableNames.get(WaypointObservationAttributeList.class));
			sb.append(" "); //$NON-NLS-1$
			sb.append(tablePrefix(WaypointObservationAttributeList.class)); 
			sb.append(" on "); //$NON-NLS-1$
			sb.append(tablePrefix(WaypointObservationAttribute.class) ); 
			sb.append(".uuid = "); //$NON-NLS-1$
			sb.append(tablePrefix(WaypointObservationAttributeList.class) ); 
			sb.append(".observation_attribute_uuid"); //$NON-NLS-1$
			
			sb.append(" JOIN "); //$NON-NLS-1$
			
			sb.append(tableNames.get(AttributeListItem.class));
			sb.append(" "); //$NON-NLS-1$
			sb.append(tablePrefix(AttributeListItem.class) ); 
			sb.append(" on "); //$NON-NLS-1$
			sb.append(tablePrefix(AttributeListItem.class) ); 
			sb.append(".uuid ="); //$NON-NLS-1$
			sb.append(tablePrefix(WaypointObservationAttributeList.class) ); 
			sb.append(".list_element_uuid "); //$NON-NLS-1$
			
			String p1 = addParameterValue(attributeItem.getItemKey());
			where.append(" AND "); //$NON-NLS-1$
			where.append(tablePrefix(AttributeListItem.class) + ".keyid = " + p1); //$NON-NLS-1$
		}
		if (attributeItem.getAttributeType() == AttributeType.TREE) {
			sb.append(" JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(AttributeTreeNode.class));
			sb.append(" ON " + tablePrefix(AttributeTreeNode.class) + ".uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".tree_node_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			String p1 = addParameterValue(attributeItem.getItemKey()); 
			String p2 = addParameterValue(attributeItem.getItemKey().substring(0,  attributeItem.getItemKey().length() -1) + "/"); //$NON-NLS-1$
			where.append(" AND ");	 //$NON-NLS-1$
			where.append("( " + tablePrefix(AttributeTreeNode.class) + ".hkey >= "+ p1 + " and " + tablePrefix(AttributeTreeNode.class) + ".hkey < " + p2 + ") "); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		
		if (attributeItem.getCategoryKey() != null) {
			sb.append(" JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Category.class) );
			sb.append(" ON " + tablePrefix(Category.class) + ".uuid = " + tablePrefix(WaypointObservation.class) + ".category_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			String p1 = addParameterValue(attributeItem.getCategoryKey()); 
			String p2 = addParameterValue(attributeItem.getCategoryKey().substring(0,  attributeItem.getCategoryKey().length() -1) + "/"); //$NON-NLS-1$
			where.append(" AND ");	 //$NON-NLS-1$
			where.append("( " + tablePrefix(Category.class) + ".hkey >= "+ p1 + " and " + tablePrefix(Category.class) + ".hkey < " + p2 + ") "); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$	
		}
		
		sb.append(where);
		
		if (gbsql.length() != 0) {
			sb.append(" GROUP BY "); //$NON-NLS-1$
			sb.append(gbsql.substring(0, gbsql.length() - 1));
		}
		
		QueryPlugIn.logSql(sb.toString());
		ResultSet rs = parseQueryString(c, sb.toString()).executeQuery();
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
	 * Computes a category summaries 
	 *
	 */
	private HashMap<SummaryResultKey, Double> getCategoryValue(
			String dataTable,
			Connection c, Session s, 
			GroupByPart groupBy, 
			CategoryValueItem categoryItem, ConservationAreaFilter caFilter) throws SQLException{
		
		clearParameters();
		
		StringBuilder sb = new StringBuilder();
		StringBuilder gbsql = new StringBuilder();
		StringBuilder joinSql = new StringBuilder();
		
		sb.append("SELECT "); //$NON-NLS-1$
		
		createGroupBySql(groupBy, sb, gbsql, joinSql);
		
		//make sure to use the filter.start_Date and filter.end_date fields
		//here as these fields have been trimmed to the filters
		if (categoryItem.getType() == ValueType.WAYPOINT) {
			sb.append(" COUNT(distinct "); //$NON-NLS-1$
			sb.append(tablePrefix(Waypoint.class) + ".uuid )"); //$NON-NLS-1$
		}else if (categoryItem.getType() == ValueType.OBSERVATION) {
			sb.append(" COUNT(distinct "); //$NON-NLS-1$
			sb.append(tablePrefix(WaypointObservation.class) + ".uuid )"); //$NON-NLS-1$
		}else {
			throw new IllegalStateException("category value item not supported: " + categoryItem.asString()); //$NON-NLS-1$
		}
		
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append( dataTable );
		sb.append(" filter "); //$NON-NLS-1$
		sb.append(joinSql);
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(AssetWaypoint.class) );
		sb.append(" ON " + tablePrefix(AssetWaypoint.class) + ".asset_deployment_uuid = filter.deployment_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Waypoint.class) );
		sb.append(" ON " + tablePrefix(Waypoint.class) + ".uuid = " + tablePrefix(AssetWaypoint.class) + ".wp_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservationGroup.class) );
		sb.append(" ON " + tablePrefix(WaypointObservationGroup.class) + ".wp_uuid = " + tablePrefix(Waypoint.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(WaypointObservation.class) );
		sb.append(" ON " + tablePrefix(WaypointObservation.class) + ".wp_group_uuid = " + tablePrefix(WaypointObservationGroup.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Category.class) );
		sb.append(" ON " + tablePrefix(Category.class) + ".uuid = " + tablePrefix(WaypointObservation.class) + ".category_uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		if (categoryItem.getCategoryHKey() != null) {
			sb.append(" WHERE ");	 //$NON-NLS-1$
			String p1 = addParameterValue(categoryItem.getCategoryHKey()); 
			String p2 = addParameterValue(categoryItem.getCategoryHKey().substring(0,  categoryItem.getCategoryHKey().length() -1) + "/"); //$NON-NLS-1$ 
			sb.append("( " + tablePrefix(Category.class) + ".hkey >= "+ p1 + " and " + tablePrefix(Category.class) + ".hkey < " + p2 + ") "); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$	
		}
		if (gbsql.length() != 0) {
			sb.append(" GROUP BY "); //$NON-NLS-1$
			sb.append(gbsql.substring(0, gbsql.length() - 1));
		}
		
		QueryPlugIn.logSql(sb.toString());
		ResultSet rs = parseQueryString(c, sb.toString()).executeQuery();
		return createValueResults(rs, groupBy, categoryItem.asString());
		
	}
	
	
	/**
	 * Computes a asset summaries
	 *
	 */
	private HashMap<SummaryResultKey, Double> getAssetValue(
			String dataTable,
			Connection c, Session s, 
			GroupByPart groupBy, 
			AssetValueItem valueItem, ConservationAreaFilter caFilter) throws SQLException{
		
		clearParameters();
		
		StringBuilder sb = new StringBuilder();
		StringBuilder gbsql = new StringBuilder();
		StringBuilder joinSql = new StringBuilder();
		
		sb.append("SELECT "); //$NON-NLS-1$
		
		createGroupBySql(groupBy, sb, gbsql, joinSql);
		
		
		StringBuilder from = null;
		//make sure to use the filter.start_Date and filter.end_date fields
		//here as these fields have been trimmed to the filters
		if (valueItem.getAssetValueOption() == AssetValueOption.ASSET_HOURS) {
			sb.append(" SUM({fn TIMESTAMPDIFF( SQL_TSI_FRAC_SECOND,  filter.start_date, case when filter.end_date is null then CURRENT_TIMESTAMP else filter.end_date END)} / 1000000000.0)"); //$NON-NLS-1$
			
		}else if (valueItem.getAssetValueOption() == AssetValueOption.ASSET_ACTIVEHOURS) {
		
			sb.append(" SUM({fn TIMESTAMPDIFF( SQL_TSI_FRAC_SECOND,  filter.start_date, case when filter.end_date is null then CURRENT_TIMESTAMP else filter.end_date END)} / 1000000000.0"); //$NON-NLS-1$
			sb.append(" - case when disruptions.sumtotal is null then 0 else disruptions.sumtotal end "); //$NON-NLS-1$
			sb.append(" )"); //$NON-NLS-1$
			
			//need to truncate disruption dates to date filters
			LocalDateTime filterStart = null;
			LocalDateTime filterEnd = null;
			LocalDate[] bits = this.localDateFilter.getDateFilterOption().getDates(); 

			if (bits != null){
				if (bits.length == 1){
					filterStart = bits[0].atTime(LocalTime.MIDNIGHT) ;
				}else if (bits.length == 2){
					filterStart = bits[0].atTime(LocalTime.MIDNIGHT);
					if (this.localDateFilter.getDateFilterOption().isEndDateInclusive()) {
						filterEnd = bits[1].atTime(LocalTime.MAX);
					}else {
						filterEnd = bits[1].atTime(LocalTime.MIDNIGHT).minusDays(5);
					}
				}else {
					throw new IllegalStateException("Invalid date filter"); //$NON-NLS-1$
				}
			}
				
			String sField = tablePrefix(AssetDeploymentDisruption.class) + ".start_date"; //$NON-NLS-1$
			String eField = tablePrefix(AssetDeploymentDisruption.class) + ".end_date"; //$NON-NLS-1$
			
			
			from = new StringBuilder();
			from.append("SELECT  SUM({fn TIMESTAMPDIFF( SQL_TSI_FRAC_SECOND,  "); //$NON-NLS-1$
			
			if (filterStart != null) {
				String p1 = addParameterValue(java.sql.Timestamp.valueOf(filterStart));
				from.append(" CASE WHEN " + sField + " > " + p1 + " then " + sField + " else " + p1  + " end, "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}else {
				from.append(sField + ","); //$NON-NLS-1$
			}
			if (filterEnd != null) {
				String p1 = addParameterValue(java.sql.Timestamp.valueOf( filterEnd));				
				from.append(" CASE WHEN " + eField + " is not null AND " + eField + " < " + p1 + " THEN " + eField ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				from.append( " WHEN " + eField + " is not null THEN " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
				from.append(" WHEN " + eField + " is null AND CURRENT_TIMESTAMP < " + p1 + " THEN CURRENT_TIMESTAMP "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				from.append(" ELSE " + p1 ); //$NON-NLS-1$
				from.append(" END "); //$NON-NLS-1$
			}else {
				from.append(eField);
			}
			
			from.append(")} / 1000000000.0) as sumtotal, "); //$NON-NLS-1$
			from.append( tablePrefix(AssetDeploymentDisruption.class) + ".asset_deployment_uuid"); //$NON-NLS-1$
			from.append(" FROM "); //$NON-NLS-1$
			from.append(tableNamePrefix(AssetDeploymentDisruption.class));
			from.append(" JOIN "); //$NON-NLS-1$
			from.append( dataTable + " dd ON dd.deployment_uuid = " + tablePrefix(AssetDeploymentDisruption.class) + ".asset_deployment_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (bits != null) {
				//add date filter
				StringBuilder df = new StringBuilder();
				String startField = tablePrefix(AssetDeploymentDisruption.class) + ".start_date"; //$NON-NLS-1$
				String endField = tablePrefix(AssetDeploymentDisruption.class) + ".end_date"; //$NON-NLS-1$
				if (bits.length == 1){
					String p1 = addParameterValue(java.sql.Date.valueOf(bits[0]));
					String p2 = addParameterValue(java.sql.Date.valueOf(bits[1]));
					
					df.append("( "); //$NON-NLS-1$
					df.append(" ( cast(" + startField + " as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + startField + " as date) <= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(")"); //$NON-NLS-1$
				}else if (bits.length == 2 && localDateFilter.getDateFilterOption().isEndDateInclusive()){
					String p1 = addParameterValue(java.sql.Date.valueOf(bits[0]));
					String p2 = addParameterValue(java.sql.Date.valueOf(bits[1]));
					
					df.append("( "); //$NON-NLS-1$
					df.append(" ( cast(" + startField + " as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + startField + " as date) <= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + endField + " as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) <= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + startField  + " as date) <= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) >= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(")"); //$NON-NLS-1$
					
				}else if (bits.length == 2){
					String p1 = addParameterValue(java.sql.Date.valueOf(bits[0]));
					String p2 = addParameterValue(java.sql.Date.valueOf(bits[1]));
					
					df.append("( "); //$NON-NLS-1$
					df.append(" ( cast(" + startField + " as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + startField + " as date) < " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + endField + "  as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) < " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + startField  +" as date) <= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) > " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(")"); //$NON-NLS-1$
				}else {
					throw new IllegalStateException("Invalid date filter"); //$NON-NLS-1$
				}
				
				from.append(" WHERE "); //$NON-NLS-1$
				from.append(df);
			
			}
			from.append(" GROUP BY "); //$NON-NLS-1$
			from.append( tablePrefix(AssetDeploymentDisruption.class) + ".asset_deployment_uuid"); //$NON-NLS-1$
			
		}else {
			throw new IllegalStateException("asset value item not supported: " + valueItem.getAssetValueOption().getGuiName(Locale.getDefault())); //$NON-NLS-1$
		}
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append( dataTable );
		sb.append(" filter "); //$NON-NLS-1$
		if (from != null) {
			sb.append(" LEFT JOIN ("); //$NON-NLS-1$
			sb.append(from);
			sb.append(") disruptions ON filter.deployment_uuid = disruptions.asset_deployment_uuid "); //$NON-NLS-1$
		}
		
		sb.append(joinSql);
		
		if (gbsql.length() != 0) {
			sb.append(" GROUP BY "); //$NON-NLS-1$
			sb.append(gbsql.substring(0, gbsql.length() - 1));
		}
		
		
		QueryPlugIn.logSql(sb.toString());
		ResultSet rs = parseQueryString(c, sb.toString()).executeQuery();
		return createValueResults(rs, groupBy, valueItem.asString());
	}
	
	
	private void createGroupBySql(GroupByPart groupBy,
			StringBuilder fromSql,
			StringBuilder groupBySql, 
			StringBuilder joinSql) throws SQLException{
		
		boolean joinAsset = false;
		boolean joinStation = false;
		for(IGroupBy gb : groupBy.getGroupBys()) {
			if (gb instanceof AssetGroupBy) {
				AssetGroupBy agb = (AssetGroupBy) gb;
				
				switch(agb.getOption()) {
				case ASSET:
					fromSql.append("filter.asset_uuid"); //$NON-NLS-1$
					groupBySql.append("filter.asset_uuid"); //$NON-NLS-1$
					break;
				case ASSETTYPE:
					joinAsset = true;
					fromSql.append(tablePrefix(Asset.class) + ".asset_type_uuid"); //$NON-NLS-1$
					groupBySql.append(tablePrefix(Asset.class) + ".asset_type_uuid"); //$NON-NLS-1$
					break;
				case CONSERVATION_AREA:
					joinAsset = true;
					fromSql.append(tablePrefix(Asset.class) + ".ca_uuid"); //$NON-NLS-1$
					groupBySql.append(tablePrefix(Asset.class) + ".ca_uuid"); //$NON-NLS-1$
					break;
				case STATION:
					joinStation = true;
					fromSql.append(tablePrefix(AssetStation.class) + ".uuid"); //$NON-NLS-1$
					groupBySql.append(tablePrefix(AssetStation.class) + ".uuid"); //$NON-NLS-1$
					
					break;
				case STATIONLOCATION:
					fromSql.append("filter.station_location_uuid"); //$NON-NLS-1$
					groupBySql.append("filter.station_location_uuid"); //$NON-NLS-1$
					break;
				default:
					throw new SQLException(MessageFormat.format("Asset group by {0} not supported.", agb.getOption().toString() )); //$NON-NLS-1$
				}
			}else if (gb instanceof AreaGroupBy) {
				fromSql.append("filter.area_" + ((AreaGroupBy)gb).getAreaType().name()); //$NON-NLS-1$
				groupBySql.append("filter.area_" + ((AreaGroupBy)gb).getAreaType().name()); //$NON-NLS-1$
			}else {
				throw new SQLException(MessageFormat.format("Cannot group by {0} for Asset Deployment Summary query", gb.asString() )); //$NON-NLS-1$
			}
			fromSql.append(","); //$NON-NLS-1$
			groupBySql.append(","); //$NON-NLS-1$
		}

		if (joinStation) {
			joinSql.append( " JOIN "); //$NON-NLS-1$
			joinSql.append(tableNamePrefix(AssetStationLocation.class) + " on filter.station_location_uuid = " + tablePrefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			joinSql.append( " JOIN "); //$NON-NLS-1$
			joinSql.append(tableNamePrefix(AssetStation.class) + " on " + tablePrefix(AssetStation.class) + ".uuid = " + tablePrefix(AssetStationLocation.class) + ".station_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (joinAsset) {
			joinSql.append( " JOIN "); //$NON-NLS-1$
			joinSql.append(tableNamePrefix(Asset.class) + " on filter.asset_uuid = " + tablePrefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		return;
	}
		
	/**
	 * Computes the header information for a given
	 * query.
	 * 
	 * @param query the summary query
	 * @param results the summary query results to update
	 * @param session hibernate session
	 */
	public static void getHeaderInfo(AssetSummaryQuery query, SummaryQueryResult results, Session session) throws Exception{
		
		// value headers
		ValuePart vp = query.getQueryDefinition().getValuePart();
		for (IValueItem item : vp.getValueItems()){

			SummaryHeader header = new SummaryHeader(
					AssetValueItemLabelProvider.INSTANCE.getName(item, session),
					AssetValueItemLabelProvider.INSTANCE.getFullName(item, session),
					item.asString(), true) ;
			header.setUiFormatter(item.getFormatter(session,ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas())));
			if (item instanceof AssetValueItem && ((AssetValueItem)item).getAssetFormatOption() != null) {
				header.setFormatter( ((AssetValueItem)item).getAssetFormatOption().getFormatter(Locale.getDefault()) );
			}
			results.addValueHeader(header);
		}
		
		DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
		
		for (IGroupBy item : query.getQueryDefinition().getRowGroupByPart().getGroupBys()){
			if (item instanceof DateGroupBy){
				((DateGroupBy) item).setDateFilter(dFilter.getDateFilterOption());
			}
			List<ListItem> items = AssetDropItemFactory.INSTANCE.findViewer(item).getItems(session);
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
			List<ListItem> items = AssetDropItemFactory.INSTANCE.findViewer(item).getItems(session);
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
		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime,"); //$NON-NLS-1$
		if (includeObservations) {
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid "); //$NON-NLS-1$
		}else {
			sql.append("cast(null as char(16) for bit data)"); //$NON-NLS-1$
		}
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("deployment_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("start_date timestamp, "); //$NON-NLS-1$
		sql.append("end_date timestamp, "); //$NON-NLS-1$
		sql.append("asset_uuid char(16) for bit data, "); //$NON-NLS-1$
		sql.append("station_location_uuid char(16) for bit data "); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public void createTemporaryTableIndexes(Connection c, String tableName) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_deployment_uuid_idx on " +  tableName + "(deployment_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

}
