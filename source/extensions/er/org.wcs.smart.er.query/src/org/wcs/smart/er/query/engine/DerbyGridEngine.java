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

/**
 * Query engine for gridded summaries.
 * 
 * 
 * @author jeffloun
 * @since 1.0.0
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.engine.grids.MissionExistsCellMerger;
import org.wcs.smart.er.query.engine.grids.SurveyCntValueComputer;
import org.wcs.smart.er.query.engine.visitors.SurveyHasObservationFilterVisitor;
import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Source;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.filter.summary.MissionValueItem;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AddCellMerger;
import org.wcs.smart.query.common.engine.DistanceValueComputer;
import org.wcs.smart.query.common.engine.ExistsValueComputer;
import org.wcs.smart.query.common.engine.GridAnalysisEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.UuidCellMerger;
import org.wcs.smart.query.common.engine.visitors.HasObservationValueVisitor;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.GridQueryResultMetadata;
import org.wcs.smart.query.common.model.GridResultItem;
import org.wcs.smart.query.common.model.Tile;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.IValueItem.ValueType;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;

public class DerbyGridEngine extends DerbySurveyQueryEngine{
	private GridQueryResult myResults;
	
	private SurveyGriddedQuery query;
	private DateFilter dateFilter;
	
	private String dataTable;
	private String gridTable;
	
	private boolean hasTrackFilter = false;
	
	@Override
	public boolean canExecute(String querytype) {
		return SurveyGriddedQuery.KEY.equals(querytype);
	}
	
	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	@Override
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{

		this.query = (SurveyGriddedQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		
		dataTable = createTempTableName();
		gridTable = createTempTableName();

		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyGridEngine_RunQueryProgress, 100);

				SurveyDesignFilter dsFilter = null;
				if (query.getSurveyDesign() != null){
					dsFilter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}
				
				ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				dateFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				try {
					Grid gridDef = new Grid(query.getGridOrigin().x, query.getGridOrigin().y, query.getGridSize(), query.getCoordinateReferenceSystem());
					IValueItem valueItem = query.getQueryDefinition().getValuePart();
					IValueItem numerator = valueItem;
					IValueItem denominator = null;
					if (valueItem instanceof CombinedValueItem){
						numerator = ((CombinedValueItem)valueItem).getPart1();
						denominator = ((CombinedValueItem)valueItem).getPart2();
					}
					
					
					//get numerator results
					Collection<GridResultItem> numeratorResults = getItems(
							gridDef, numerator, query.getQueryDefinition().getValueFilter(), 
							dsFilter, caFilter, c, session, new SubProgressMonitor(monitor, 30), true);
					
					//apply denominator results
					if (denominator != null){
						boolean isSame = false;
						if (query.getQueryDefinition().getRateFilter() != null && query.getQueryDefinition().getValueFilter() != null){
							isSame = (query.getQueryDefinition().getRateFilter().asString().equals(query.getQueryDefinition().getValueFilter().asString()));	
						}else if (query.getQueryDefinition().getRateFilter() == null && query.getQueryDefinition().getValueFilter() == null){
							isSame = true;
						}
						//computer denominator results
						//only recompute filter if filter is different
						Collection<GridResultItem> denominatorResults = getItems(gridDef, denominator, query.getQueryDefinition().getRateFilter(), dsFilter, caFilter, c, session, new SubProgressMonitor(monitor, 30), !isSame);
						HashMap<String, Double> items = new HashMap<String, Double>();
						for (GridResultItem it : denominatorResults){
							items.put(it.getTileId(), it.getValue());
						}
						
						//compute value
						for (GridResultItem i : numeratorResults){
							Double v = items.get(i.getTileId());
							if (v == null){
								i.setValue(0);
							}else if (v == 0){
								i.setValue(0);
							}else{
								i.setValue(i.getValue() / v);
							}
						}
					}else{
						monitor.worked(30);
					}

					monitor.subTask(Messages.DerbyGridEngine_ProgressTrackLocations);
					//combine with the patrol existance value
					HashMap<String, GridResultItem> items = new HashMap<String, GridResultItem>();
					for (GridResultItem it : numeratorResults){
						items.put(it.getTileId(), it);
					}

					List<GridResultItem> missionLocations = computeMissionExistance(c, gridDef, dsFilter, caFilter);
					for (GridResultItem it : missionLocations){
						if (items.get(it.getTileId()) == null){ 
							GridResultItem newitem = new GridResultItem();
							newitem.setTileX(it.getTileX());
							newitem.setTileY(it.getTileY());
							newitem.setValue(0);
							items.put(it.getTileId(), newitem); 
						}
					}
					myResults = new GridQueryResult(items.values());
					monitor.worked(40);
				}catch (Exception ex){
					ERQueryPlugIn.log(ex.getMessage(), ex);
					throw new SQLException(ex.getMessage(), ex);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryGridTable(c);
					monitor.done();
				}
				c.commit();
			}

		});
		myResults.setResultsMetadata(GridQueryResultMetadata.computeMetadata(myResults.getData()));
		return myResults;

	}

	/*
	 * Computes the grid results
	 * @param needsFilter if the values need to be filtered or if previous filter can be used
	 * 
	 */
	private Collection<GridResultItem> getItems(Grid gridDef, IValueItem value, 
			QueryFilter filter, SurveyDesignFilter sdFilter, 
			ConservationAreaFilter caFilter, Connection c, Session session, 
			IProgressMonitor monitor, boolean needsFilter) throws Exception{
		
		monitor.beginTask(Messages.DerbyGridEngine_CreateObsTableProgress, 100);
		
		if (needsFilter) {
			monitor.subTask(Messages.DerbyGridEngine_ProgressFilters);
			try {
				dropTemporaryGridTable(c);
			} catch (Exception ex) {
				// eatme
			}
			if (filter == null){
				filter = new QueryFilter(EmptyFilter.INSTANCE);
			}
			boolean needsObservation = false;
			
			HasObservationValueVisitor vv = new HasObservationValueVisitor();
			value.accept(vv);
			
			SurveyHasObservationFilterVisitor ov = new SurveyHasObservationFilterVisitor();
			filter.getFilter().accept(ov);
			
			if (vv.hasCategory() || vv.hasAttribute() || ov.hasObservationFilter()){
				needsObservation = true;
			}
			
			//do need join mission tracks?
			final boolean[] needstracks = new boolean[]{false};
			IFilterVisitor missionTracks = new IFilterVisitor() {
				@Override
				public void visit(IFilter filter) {
					if (needstracks[0]) return;
					if ((filter instanceof SamplingUnitFilter
							&& ((SamplingUnitFilter)filter).getSource() == Source.TRACK) ||
						(filter instanceof SamplingUnitAttributeFilter &&
							((SamplingUnitAttributeFilter)filter).getSource() == Source.TRACK)){
						needstracks[0] = true;
					}
					
				}
			};
			if (filter.getFilter() != null){
				filter.getFilter().accept(missionTracks);
				hasTrackFilter = needstracks[0];
			}
			
			IFilterProcessor filterer = super.getFilterProcessor(filter.getFilterType(), dataTable, sdFilter);
			try{
				filterer.processFilter(c, filter.getFilter(), dateFilter, caFilter, 
					needsObservation, false, new SubProgressMonitor(monitor, 90));
			}finally{
				filterer.dropTemporaryTables(c);
			}

			if (monitor.isCanceled()) {
				return null;
			}
		}
		monitor.subTask(Messages.DerbyGridEngine_CalcValueProgresss);
		Collection<GridResultItem> results = getGridResults(c, session, gridDef, value);
		monitor.worked(10);
		
		monitor.done();
		return results;
	}
	/**
	 * Gets results from the temporary query table, grouped by the tile ID
	 * and loads them into internal memory store
	 * 
	 * @param c database connection 
	 * @param session hibernate session
	 * @return list of query results
	 * 
	 * @throws SQLException
	 */
	protected Collection<GridResultItem> getGridResults(Connection c, 
			Session session, Grid gridDef, IValueItem value)
			throws Exception {

		String strAgg =""; //$NON-NLS-1$
		ResultSet rs;

		if(value instanceof MissionValueItem ){
			MissionValueItem vi = (MissionValueItem)value;
			return computeSurveyValue(c, vi, gridDef);
		}else if(value instanceof AttributeValueItem ){
			AttributeValueItem tmp = (AttributeValueItem)value;
				
			String strAggValue = "number_value"; //$NON-NLS-1$
			strAgg = tmp.getAttributeKey();
			if (tmp.getAttributeType() == AttributeType.LIST || tmp.getAttributeType() == AttributeType.TREE){
				strAgg="count";  //$NON-NLS-1$
				strAggValue = "value";  //$NON-NLS-1$
			}
			String key = tmp.getAttributeKey();
			double minX = gridDef.getOriginX();
			double minY = gridDef.getOriginY();
			double size = gridDef.getCellSize();
			
			clearParameters();
			StringBuilder sql = new StringBuilder();

			sql.append("SELECT " + strAgg + "(" + strAggValue + ") as value, tile_id"); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$
			sql.append(" FROM ("); //$NON-NLS-1$
				
			if (tmp.getAttributeType() == AttributeType.NUMERIC){
				sql.append("SELECT number_value  "); //$NON-NLS-1$
			}else if (tmp.getAttributeType() == AttributeType.TREE || tmp.getAttributeType() == AttributeType.LIST){
				sql.append("SELECT distinct "); //$NON-NLS-1$
				if (tmp.getValueType() == ValueType.OBSERVATION){
					sql.append(dataTable);
					sql.append(".ob_uuid as " + strAggValue);  //$NON-NLS-1$
				}else{
					sql.append(dataTable);
					sql.append(".wp_uuid as " + strAggValue);  //$NON-NLS-1$
				}
			}
			String p1 = addParameterValue(gridDef.getCrs().toWKT());
			String p2 = addParameterValue(minX);
			String p3 = addParameterValue(minY);
			String p4 = addParameterValue(size);
			
			sql.append(", smart.computeTileId(");  //$NON-NLS-1$
			sql.append( tablePrefix.get(Waypoint.class));
			sql.append(".x,");  //$NON-NLS-1$
			sql.append( tablePrefix.get(Waypoint.class));
			sql.append(".y," + p1 + "," + p2 + "," + p3 + "," + p4 + ") as tile_id");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			 
			sql.append(" FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append( " as "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservation.class));
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append( dataTable);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append( dataTable);
			sql.append( ".ob_uuid"); //$NON-NLS-1$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(tableNames.get(WaypointObservationAttribute.class));
			sql.append(" as "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservationAttribute.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservation.class) );
			sql.append(".uuid = " ); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservationAttribute.class));
			sql.append(".observation_uuid"); //$NON-NLS-1$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(tableNames.get(Attribute.class));
			sql.append(" as "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Attribute.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservationAttribute.class));
			sql.append(".attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Attribute.class));
			sql.append(".uuid"); //$NON-NLS-1$
			sql.append(" AND keyid = '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(tableNames.get(Waypoint.class));
			sql.append( " as "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Waypoint.class)); 
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(dataTable);
			sql.append(".wp_uuid"); //$NON-NLS-1$
			
			if (tmp.getCategoryKey() != null){
				sql.append(" JOIN "); //$NON-NLS-1$
				sql.append(tableNames.get(Category.class));
				sql.append( " as "); //$NON-NLS-1$
				sql.append(tablePrefix.get(Category.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix.get(WaypointObservation.class));
				sql.append(".category_uuid = "); //$NON-NLS-1$
				sql.append( tablePrefix.get(Category.class));
				sql.append( ".uuid" ); //$NON-NLS-1$
				p1 = addParameterValue(tmp.getCategoryKey());
				p2 = addParameterValue(tmp.getCategoryKey().substring(0, tmp.getCategoryKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" AND Hkey >= " + p1 + " and hkey < " + p2 + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			
			if (tmp.getAttributeType() == AttributeType.LIST){
				sql.append(" JOIN " + tableNames.get(AttributeListItem.class) ); //$NON-NLS-1$
				sql.append(" as "); //$NON-NLS-1$
				sql.append( tablePrefix.get(AttributeListItem.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append( tablePrefix.get(AttributeListItem.class));
				sql.append(".uuid = "); //$NON-NLS-1$
				sql.append( tablePrefix.get(WaypointObservationAttribute.class));
				sql.append(".list_element_uuid and "); //$NON-NLS-1$
				sql.append( tablePrefix.get(AttributeListItem.class));
				p1 = addParameterValue(tmp.getItemKey());
				sql.append(".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
						
			}else if (tmp.getAttributeType() == AttributeType.TREE){
				sql.append(" join "); //$NON-NLS-1$
				sql.append(tableNames.get(AttributeTreeNode.class));
				sql.append(" as "); //$NON-NLS-1$
				sql.append(tablePrefix.get(AttributeTreeNode.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix.get(AttributeTreeNode.class));
				sql.append(".uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix.get(WaypointObservationAttribute.class));
				sql.append(".tree_node_uuid "); //$NON-NLS-1$
				sql.append(" and ("); //$NON-NLS-1$
				p1 = addParameterValue(tmp.getItemKey());
				p2 = addParameterValue(tmp.getItemKey().substring(0, tmp.getItemKey().length() -1 ) + "/"); //$NON-NLS-1$
				sql.append(tablePrefix.get(AttributeTreeNode.class));
				sql.append(".hkey >= " + p1 + " and "); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(tablePrefix.get(AttributeTreeNode.class));
				sql.append(".hkey < " + p2 + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sql.append(") as foo group by tile_id"); //$NON-NLS-1$
			
			QueryPlugIn.logSql(sql.toString());
			rs = parseQueryString(c, sql.toString()).executeQuery();
		}else if(value instanceof CategoryValueItem){
			CategoryValueItem tmp = (CategoryValueItem)value;
			strAgg = "count"; //$NON-NLS-1$
				
			clearParameters();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(strAgg + "(localkey) as value,tile_id"); //$NON-NLS-1$
			sql.append(" FROM ("); //$NON-NLS-1$
			sql.append("SELECT distinct "); //$NON-NLS-1$
			if (tmp.getType() == IValueItem.ValueType.OBSERVATION){
				sql.append(tablePrefix.get(WaypointObservation.class) +".uuid as localkey, "); //$NON-NLS-1$
			}else{
				sql.append(dataTable + ".wp_uuid as localkey, "); //$NON-NLS-1$
			}
			String p1 = addParameterValue(gridDef.getCrs().toWKT());
			String p2 = addParameterValue(gridDef.getOriginX());
			String p3 = addParameterValue(gridDef.getOriginY());
			String p4 = addParameterValue(gridDef.getCellSize());
			sql.append("smart.computeTileId(" + tablePrefix.get(Waypoint.class)+ ".x," + tablePrefix.get(Waypoint.class) + ".y,"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sql.append(p1 + ", " + p2 + ", " + p3 + ", " + p4 + ") as tile_id"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			
			sql.append(" FROM " + tableNames.get(WaypointObservation.class) + " as " + tablePrefix.get(WaypointObservation.class)); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN " + dataTable  //$NON-NLS-1$
					+ " on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ dataTable
					+ ".ob_uuid"); //$NON-NLS-1$
			sql.append(" JOIN " + tableNames.get(Category.class) + " as " + tablePrefix.get(Category.class)  //$NON-NLS-1$ //$NON-NLS-2$
					+ " on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
					+ ".category_uuid = " //$NON-NLS-1$
					+ tablePrefix.get(Category.class)
					+ ".uuid" //$NON-NLS-1$
					+ " AND "); //$NON-NLS-1$
			if (tmp.getCategoryHKey() != null){
				p1 = addParameterValue(tmp.getCategoryHKey());
				p2 = addParameterValue(tmp.getCategoryHKey().substring(0, tmp.getCategoryHKey().length() - 1) + "/"); //$NON-NLS-1$
				sql.append(" Hkey >= " + p1 + " and hkey < " + p2 + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				sql.append(" hkey is not null "); //$NON-NLS-1$
			}
			
			sql.append(" JOIN " + tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class)  //$NON-NLS-1$ //$NON-NLS-2$
					+ " on " + tablePrefix.get(Waypoint.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ dataTable
					+ ".wp_uuid"); //$NON-NLS-1$
			sql.append(") as foo "); //$NON-NLS-1$
			sql.append(" group by "); //$NON-NLS-1$
			sql.append("tile_id"); //$NON-NLS-1$
			
			QueryPlugIn.logSql(sql.toString());
			rs = parseQueryString(c, sql.toString()).executeQuery();
		}else{
			throw new SQLException(MessageFormat.format(Messages.DerbyGridEngine_ValueNotSupported, new Object[]{value.asString()}));	
		}
		
		try {
			List<GridResultItem> items = new ArrayList<GridResultItem>();
			while (rs.next()) {
				GridResultItem it = new GridResultItem();
			
				String tid = rs.getString("TILE_ID"); //$NON-NLS-1$
				String[] tileids = tid.split("_"); //$NON-NLS-1$
				
				it.setTileX(Long.parseLong(tileids[0]));
				it.setTileY(Long.parseLong(tileids[1]));
				it.setValue(rs.getDouble("value")); //$NON-NLS-1$
			
				items.add(it);
				if (items.size() > Grid.MAX_GRID_CELLS){
					throw Grid.GRID_TO_BIG_EXCEPTION;
				}
			}
			return items;
		} finally {
			rs.close();
		}
		
	}

	private List<GridResultItem> computeMissionExistance(Connection c, Grid gridDef, 
			SurveyDesignFilter dsFilter,
			ConservationAreaFilter caFilter) throws Exception{
		GridAnalysisEngine<?> engine = null;
		
		MissionExistsCellMerger cellMerger = new MissionExistsCellMerger();
		ExistsValueComputer valueComputer = new ExistsValueComputer();
		
		engine = new GridAnalysisEngine<Boolean>(gridDef, cellMerger, valueComputer);
		return computeMissionTrackNoFilter(c, engine, dsFilter, caFilter);
	}
	
	

	/**
	 * Determines which grid cells have mission tracks but
	 * does not apply any filter filters except the date, conservation area,
	 * and survey design filter.
	 * 
	 * 
	 * @param c
	 * @param engine
	 * @return
	 * @throws Exception
	 */
	private List<GridResultItem> computeMissionTrackNoFilter(Connection c, 
			GridAnalysisEngine<?> engine, SurveyDesignFilter dsFilter,
			ConservationAreaFilter caFilter) throws Exception{
		
		clearParameters();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + tablePrefix.get(MissionTrack.class) + ".geometry as geom "); //$NON-NLS-1$ //$NON-NLS-2$
		
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNames.get(MissionTrack.class) + " " + tablePrefix.get(MissionTrack.class)); //$NON-NLS-1$
		sql.append(" join " + tableNamePrefix(MissionDay.class) ); //$NON-NLS-1$
		sql.append(" on " + tablePrefix(MissionDay.class) + ".uuid = " + tablePrefix(MissionTrack.class) + ".mission_day_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		sql.append(" join " + tableNamePrefix(Mission.class) ); //$NON-NLS-1$ 
		sql.append(" on " + tablePrefix.get(Mission.class) + ".uuid = " + tablePrefix.get(MissionDay.class) + ".mission_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join " + tableNames.get(Survey.class) + " " + tablePrefix.get(Survey.class) ); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" on " + tablePrefix.get(Survey.class) + ".uuid = " + tablePrefix.get(Mission.class) + ".survey_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join " + tableNames.get(SurveyDesign.class) + " " + tablePrefix.get(SurveyDesign.class) ); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" on " + tablePrefix.get(SurveyDesign.class) + ".uuid = " + tablePrefix.get(Survey.class) + ".survey_design_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		if (dateFilter != null ){
			String dfilter = SurveyFilterSqlGenerator.INSTANCE.toSql(dateFilter, this);
			if (dfilter.length() > 0) {				
				sql.append(" and "); //$NON-NLS-1$
				sql.append(dfilter);
			}
		}
		sql.append( " and "); //$NON-NLS-1$
		sql.append(SurveyFilterSqlGenerator.INSTANCE.toSql(caFilter, this));
		
		if (dsFilter != null){
			sql.append( " and "); //$NON-NLS-1$
			sql.append(SurveyFilterSqlGenerator.INSTANCE.toSql(dsFilter, this));
		}
		
		QueryPlugIn.logSql(sql.toString());
		try(ResultSet rs = parseQueryString(c, sql.toString()).executeQuery()){
			while(rs.next()){
				byte[] bytes = rs.getBytes("geom"); //$NON-NLS-1$
				if (bytes != null){
					WKBReader reader = new WKBReader();
					LineString ls = (LineString) reader.read(bytes);
					try{
						engine.rasterizeLinestring(ls);
					}catch (Exception ex){
						EcologicalRecordsPlugIn.log("Error rasterizing linestring: " + ls.toText(), ex); //$NON-NLS-1$
						throw ex;
					}
				}
			}
		}
		
		
		List<GridResultItem> items = new ArrayList<GridResultItem>();
		for (Iterator<Entry<Tile,Double>> iterator = engine.getData().entrySet().iterator(); iterator.hasNext();) {
			Entry<Tile,Double> object = (Entry<Tile,Double>) iterator.next();
			GridResultItem it = new GridResultItem();
			it.setTileX(object.getKey().getXId()+1);
			it.setTileY(object.getKey().getYId()+1);
			it.setValue(object.getValue());
			items.add(it);
		}
		return items;
		
	}

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	protected void dropTemporaryGridTable(Connection c) throws SQLException {
		dropTable(c, dataTable);
		dropTable(c, gridTable);
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
		
		if (hasTrackFilter){
			sql.append(tablePrefix(MissionTrack.class) + ".uuid, "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as char for bit data),");	 //$NON-NLS-1$
		}
		
		
		if (includeObservations){
			sql.append(tablePrefix(SamplingUnit.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(SamplingUnit.class) + ".id, "); //$NON-NLS-1$
			sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as char for bit data),");	 //$NON-NLS-1$
			sql.append("cast(null as varchar(128)),");	 //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	 //$NON-NLS-1$
			sql.append("cast(null as timestamp),");	 //$NON-NLS-1$
			sql.append("cast(null as char for bit data)");	 //$NON-NLS-1$
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
		sql.append("mission_track_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("sampling_unit_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("sampling_unit_id varchar(128),"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_datetime timestamp,"); //$NON-NLS-1$
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
	protected SurveyQueryResultItem asQueryResultItem(ResultSet rs, Session session)
			throws SQLException {
		return null;
	}

	private Collection<GridResultItem> computeSurveyValue(Connection c,
			MissionValueItem item, 
			Grid gridDef) throws Exception{
		GridAnalysisEngine<?> engine = null;
		String dataField[] = null;
		
		if (item.getValueItem() == MissionValueItem.ValueItem.TRACK_LENGTH){
			AddCellMerger cellMerger = new AddCellMerger();	//adds cell values
			DistanceValueComputer valueComputer = new DistanceValueComputer();
			engine = new GridAnalysisEngine<Double>(gridDef, cellMerger, valueComputer);
			return computeMissionTrack(c, engine, dataField);
		}else if (item.getValueItem() == MissionValueItem.ValueItem.MISSION_COUNT){
			dataField = new String[]{"mission_uuid"}; //$NON-NLS-1$
			UuidCellMerger cellMerger = new UuidCellMerger();
			SurveyCntValueComputer valueComputer = new SurveyCntValueComputer();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
			return computeMissionTrack(c, engine, dataField);
		}else if (item.getValueItem() == MissionValueItem.ValueItem.SURVEY_COUNT){	
			dataField = new String[]{"survey_uuid"}; //$NON-NLS-1$ 
			UuidCellMerger cellMerger = new UuidCellMerger();
			SurveyCntValueComputer valueComputer = new SurveyCntValueComputer();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
			return computeMissionTrack(c, engine, dataField);
		}else if (item.getValueItem() == MissionValueItem.ValueItem.DAY_COUNT){
			dataField = new String[]{"mission_day_uuid"}; //$NON-NLS-1$
			UuidCellMerger cellMerger = new UuidCellMerger();
			SurveyCntValueComputer valueComputer = new SurveyCntValueComputer();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
			return computeMissionTrack(c, engine, dataField);
		}else{
			throw new Exception(MessageFormat.format(Messages.DerbyGridEngine_UnsupportedValue, new Object[]{item.getValueItem().getGuiName(Locale.getDefault())}));
		}
	}
	
	private Collection<GridResultItem> computeMissionTrack(Connection c, 
			GridAnalysisEngine<?> engine, String[] dataField) throws Exception{
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + tablePrefix.get(MissionTrack.class) + ".geometry as geom "); //$NON-NLS-1$ //$NON-NLS-2$
		if (dataField != null){
			for (int i = 0; i < dataField.length; i ++){
				//additional data required for rasterization
				sql.append(", tmp." + dataField[i] + " as dataField_" + i); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionTrack.class));
		sql.append(", ("); //$NON-NLS-1$
		if (hasTrackFilter){
			sql.append("SELECT distinct mission_track_uuid as unqid"); //$NON-NLS-1$
		}else{
			sql.append("SELECT distinct mission_day_uuid as unqid"); //$NON-NLS-1$
		}
		if (dataField != null){
			//additional data required for rasterization
			for (int i = 0; i < dataField.length; i ++){
				sql.append(", " + dataField[i]); //$NON-NLS-1$
			}
		}
		sql.append(" from " ); //$NON-NLS-1$
		sql.append(dataTable);
		sql.append(") tmp "); //$NON-NLS-1$
		sql.append("WHERE " ); //$NON-NLS-1$
		if (hasTrackFilter){
			sql.append(tablePrefix(MissionTrack.class) + ".uuid = "); //$NON-NLS-1$
		}else{
			sql.append(tablePrefix(MissionTrack.class) + ".mission_day_uuid = "); //$NON-NLS-1$
		}
		sql.append("tmp.unqid"); //$NON-NLS-1$
		

		QueryPlugIn.logSql(sql.toString());
		
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())){
			while(rs.next()){
				byte[] bytes = rs.getBytes("geom"); //$NON-NLS-1$
				Object[] data = null;
				if (dataField != null){
					data = new Object[dataField.length];
					for (int i = 0; i < dataField.length; i++){
						data[i] = rs.getObject("dataField_"+i); //$NON-NLS-1$
					}
				}
				if (bytes != null){
					WKBReader reader = new WKBReader();
					LineString ls = (LineString) reader.read(bytes);
					if (data != null){
						if (data.length == 1){
							ls.setUserData(data[0]);		
						}else{
							ls.setUserData(data);
						}
					}
					try{
						engine.rasterizeLinestring(ls);
					}catch (Exception ex){
						ERQueryPlugIn.log("Error rasterizing linestring: " + ls.toText(), ex); //$NON-NLS-1$
						throw ex;
					}
				}
			}
		}
		
		List<GridResultItem> items = new ArrayList<GridResultItem>();	
		for (Iterator<Entry<Tile,Double>> iterator = engine.getData().entrySet().iterator(); iterator.hasNext();) {
			Entry<Tile,Double> object = (Entry<Tile,Double>) iterator.next();
			GridResultItem it = new GridResultItem();
			it.setTileX(object.getKey().getXId()+1);
			it.setTileY(object.getKey().getYId()+1);
			it.setValue(object.getValue());
			items.add(it);
		}
		return items;
		
	}
	
	@Override
	public String getFilterTablesJoinColum(){
		return "wp_uuid"; //$NON-NLS-1$
	}
	
}