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
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.engine.grids.PatrolCntValueComputer;
import org.wcs.smart.patrol.query.engine.grids.PatrolDayCntValueComputer;
import org.wcs.smart.patrol.query.engine.grids.PatrolExistsCellMerger;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AddCellMerger;
import org.wcs.smart.query.common.engine.DistanceValueComputer;
import org.wcs.smart.query.common.engine.ExistsValueComputer;
import org.wcs.smart.query.common.engine.GridAnalysisEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.UuidCellMerger;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
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
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.IValueItem.ValueType;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;

public class DerbyGridEngine extends DerbyPatrolQueryEngine{
	private GridQueryResult myResults;
	
	private PatrolGriddedQuery query;
	private DateFilter dateFilter;
	
	private String dataTable;
	private String gridTable;
	
	@Override
	public boolean canExecute(String querytype) {
		return PatrolGriddedQuery.KEY.equals(querytype);
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

		this.query = (PatrolGriddedQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		
		dataTable = createTempTableName();
		gridTable = createTempTableName();

		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyGridEngine_Progress_RunningQuery, 4);

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
					
					ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					
					//get numerator results
					Collection<GridResultItem> numeratorResults = getItems(gridDef, numerator, query.getQueryDefinition().getValueFilter(), caFilter, c, session, monitor, true);
					
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
						Collection<GridResultItem> denominatorResults = getItems(gridDef, denominator, query.getQueryDefinition().getRateFilter(), caFilter, c, session, monitor, !isSame);
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
					}

					//combine with the patrol existance value
					HashMap<String, GridResultItem> items = new HashMap<String, GridResultItem>();
					for (GridResultItem it : numeratorResults){
						items.put(it.getTileId(), it);
					}

					List<GridResultItem> patrolLocations = computePatrolExistance(c, gridDef, caFilter);
					for (GridResultItem it : patrolLocations){
						if (items.get(it.getTileId()) == null){ 
							GridResultItem newitem = new GridResultItem();
							newitem.setTileX(it.getTileX());
							newitem.setTileY(it.getTileY());
							newitem.setValue(0);
							items.put(it.getTileId(), newitem); 
						}
					}
					myResults = new GridQueryResult(items.values());
					
					monitor.worked(1);
				}catch (Exception ex){
					throw new SQLException(ex);
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
			QueryFilter filter, ConservationAreaFilter caFilter, Connection c, Session session, 
			IProgressMonitor monitor, boolean needsFilter) throws Exception{
		monitor.subTask(Messages.DerbyGridEngine_Progress_CreatingObservationTable);
		
		if (needsFilter) {
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
			vv.visit(value);
			HasObservationFilterVisitor ov = new HasObservationFilterVisitor();
			ov.visit(filter.getFilter());
			
			if (vv.hasCategory() || vv.hasAttribute() ||
				ov.hasAttributeFilter() || ov.hasCategoryFilter()){
				needsObservation = true;
			}
			
			IFilterProcessor filterer = super.getFilterProcessor(filter.getFilterType(), dataTable);
			try{
				filterer.processFilter(c, filter.getFilter(), dateFilter, caFilter, needsObservation, false, monitor);
			}finally{
				filterer.dropTemporaryTables(c);
			}

			if (monitor.isCanceled()) {
				return null;
			}
		}
		monitor.subTask(Messages.DerbyGridEngine_Progress_CalculatingGridValue);
		return getGridResults(c, session, gridDef, value);
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
		clearParameters();
		
		if(value instanceof PatrolValueItem ){
			return computePatrolValue(c, (PatrolValueItem)value, gridDef);
		}else{
			if(value instanceof AttributeValueItem ){
				AttributeValueItem tmp = (AttributeValueItem)value;
				
				String strAggValue = "number_value"; //$NON-NLS-1$
				strAgg = tmp.getAggregationKey();
				if (tmp.getAttributeType() == AttributeType.LIST || tmp.getAttributeType() == AttributeType.TREE){
					strAgg="count";  //$NON-NLS-1$
					strAggValue = "value";  //$NON-NLS-1$
					
				}
				String key = tmp.getAttributeKey();
				double minX = gridDef.getOriginX();
				double minY = gridDef.getOriginY();
				double size = gridDef.getCellSize();
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
				sql.append(".y," + p1 + "," + p2 + "," + p3 + ", " + p4 + " ) as tile_id");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$				
				 
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
					sql.append(" AND Hkey >= " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(" AND Hkey < " + p2 + " "); //$NON-NLS-1$ //$NON-NLS-2$
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
					sql.append(".keyid = " + p1); //$NON-NLS-1$
					
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
					sql.append(tablePrefix.get(AttributeTreeNode.class));
					p1 = addParameterValue(tmp.getItemKey());
					p2 = addParameterValue(tmp.getItemKey().substring(0, tmp.getItemKey().length() -1 ) + "/"); //$NON-NLS-1$
					sql.append(".hkey >= " + p1); //$NON-NLS-1$
					sql.append(" and "); //$NON-NLS-1$
					sql.append(tablePrefix.get(AttributeTreeNode.class));
					sql.append(".hkey < " + p2 + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					
				}

				sql.append(") as foo group by tile_id"); //$NON-NLS-1$
				rs = parseQueryString(c, sql.toString()).executeQuery();
				
			}else if(value instanceof CategoryValueItem){
				CategoryValueItem tmp = (CategoryValueItem)value;
				strAgg = "count"; //$NON-NLS-1$
				
				double minX = gridDef.getOriginX();
				double minY = gridDef.getOriginY();
				double size = gridDef.getCellSize();
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
				String p2 = addParameterValue(minX);
				String p3 = addParameterValue(minY);
				String p4 = addParameterValue(size);
				sql.append("smart.computeTileId(" + tablePrefix.get(Waypoint.class)+ ".x," + tablePrefix.get(Waypoint.class) + ".y,"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(p1 + "," + p2 + ","+ p3 + "," + p4 + ") as tile_id "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

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
					p2 = addParameterValue(tmp.getCategoryHKey().substring(0,  tmp.getCategoryHKey().length()-1) +"/"); //$NON-NLS-1$
					
					sql.append("hkey >= " + p1); //$NON-NLS-1$
					sql.append(" AND hkey < " + p2); //$NON-NLS-1$
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
				throw new SQLException(Messages.DerbyGridEngine_Error_GridValueNotSupported);	
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
	}

	private List<GridResultItem> computePatrolExistance(Connection c, 
			Grid gridDef, ConservationAreaFilter caFilter) throws Exception{
		GridAnalysisEngine<?> engine = null;
		
		PatrolExistsCellMerger cellMerger = new PatrolExistsCellMerger();
		ExistsValueComputer valueComputer = new ExistsValueComputer();
		
		engine = new GridAnalysisEngine<Boolean>(gridDef, cellMerger, valueComputer);
		return computePatrolTrackNoFilter(c, engine, caFilter);
	}
	
	private Collection<GridResultItem> computePatrolValue(Connection c,
			PatrolValueItem item, 
			Grid gridDef) throws Exception{
		GridAnalysisEngine<?> engine = null;
		String dataField[] = null;
		PatrolValueOption option = PatrolQueryOptions.findPatrolValueItem(item.getPatrolValueOptionKey());
		if (option == PatrolValueOption.DISTANCE){
			AddCellMerger cellMerger = new AddCellMerger();	//adds cell values
			DistanceValueComputer valueComputer = new DistanceValueComputer();
			engine = new GridAnalysisEngine<Double>(gridDef, cellMerger, valueComputer);
		}else if (option == PatrolValueOption.NUM_DAYS){	
			dataField = new String[]{"p_uuid", "pld_patrol_day"}; //$NON-NLS-1$ //$NON-NLS-2$
			UuidCellMerger cellMerger = new UuidCellMerger();
			PatrolDayCntValueComputer valueComputer = new PatrolDayCntValueComputer();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
		}else if (option == PatrolValueOption.NUM_PATROLS){
			dataField = new String[]{"p_uuid"}; //$NON-NLS-1$
			UuidCellMerger cellMerger = new UuidCellMerger();
			PatrolCntValueComputer valueComputer = new PatrolCntValueComputer();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
			
		}else{
			throw new UnsupportedOperationException(
					MessageFormat.format(Messages.DerbyGridEngine_Error_PatrolValueNotSupported, new Object[]{option.getGuiName(Locale.getDefault())}));
		}
		return computePatrolTrack(c, engine, dataField);
	}
	
	private Collection<GridResultItem> computePatrolTrack(Connection c, 
			GridAnalysisEngine<?> engine, 
			String[] dataField) throws Exception{
		
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + tablePrefix.get(Track.class) + ".geometry as geom "); //$NON-NLS-1$ //$NON-NLS-2$
		if (dataField != null){
			for (int i = 0; i < dataField.length; i ++){
				//additional data required for rasterization
				sql.append(", tmp." + dataField[i] + " as dataField_" + i); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNames.get(Track.class) + " " + tablePrefix.get(Track.class)); //$NON-NLS-1$
		sql.append(", ("); //$NON-NLS-1$
		sql.append("SELECT distinct pld_uuid "); //$NON-NLS-1$
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
		sql.append(tablePrefix.get(Track.class) + ".patrol_leg_day_uuid = "); //$NON-NLS-1$
		sql.append("tmp.pld_uuid"); //$NON-NLS-1$

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
						PatrolQueryPlugIn.log("Error rasterizing linestring: " + ls.toText(), ex); //$NON-NLS-1$
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
	 * Determines which grid cells have patrol tracks but
	 * does not apply any filter filters except the date
	 * and conservation area filter.
	 * 
	 * @param c
	 * @param engine
	 * @return
	 * @throws Exception
	 */
	private List<GridResultItem> computePatrolTrackNoFilter(Connection c, 
			GridAnalysisEngine<?> engine, ConservationAreaFilter caFilter) throws Exception{
		clearParameters();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + tablePrefix.get(Track.class) + ".geometry as geom "); //$NON-NLS-1$ //$NON-NLS-2$
		
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNames.get(Track.class) + " " + tablePrefix.get(Track.class)); //$NON-NLS-1$
		sql.append(" join " + tableNames.get(PatrolLegDay.class) + " " + tablePrefix.get(PatrolLegDay.class) ); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = " + tablePrefix.get(Track.class) + ".patrol_leg_day_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join " + tableNames.get(PatrolLeg.class) + " " + tablePrefix.get(PatrolLeg.class) ); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = " + tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join " + tableNames.get(Patrol.class) + " " + tablePrefix.get(Patrol.class) ); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" on " + tablePrefix.get(Patrol.class) + ".uuid = " + tablePrefix.get(PatrolLeg.class) + ".patrol_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		if (dateFilter != null ){
			String dfilter = PatrolFilterSqlGenerator.INSTANCE.toSql(dateFilter, this);
			if (dfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(dfilter);
			}
		}
		sql.append( " and "); //$NON-NLS-1$
		sql.append(PatrolFilterSqlGenerator.INSTANCE.toSql(caFilter, this));
		
		QueryPlugIn.logSql(sql.toString());		
		try(ResultSet rs = parseQueryString(c, sql.toString()).executeQuery();){
			while(rs.next()){
				byte[] bytes = rs.getBytes("geom"); //$NON-NLS-1$
				if (bytes != null){
					WKBReader reader = new WKBReader();
					LineString ls = (LineString) reader.read(bytes);
					try{
						engine.rasterizeLinestring(ls);
					}catch (Exception ex){
						PatrolQueryPlugIn.log("Error rasterizing linestring: " + ls.toText(), ex); //$NON-NLS-1$
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
	protected String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_id varchar(32),"); //$NON-NLS-1$
		sql.append("p_station_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_team_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_objective varchar(8192),"); //$NON-NLS-1$
		sql.append("p_mandate_uuid  char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_type varchar(6),"); //$NON-NLS-1$
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
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_leader char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_pilot char(16) for bit data"); //$NON-NLS-1$
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
	protected PatrolQueryResultItem asQueryResultItem(ResultSet rs, Session session)
			throws SQLException {
		return null;
	}

	
	
}