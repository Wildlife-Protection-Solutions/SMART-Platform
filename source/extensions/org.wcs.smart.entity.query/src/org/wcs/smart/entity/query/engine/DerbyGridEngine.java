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

package org.wcs.smart.entity.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationValueVisitor;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.IValueItem.ValueType;

/**
 * Query engine for gridded summaries.
 * 
 * 
 * @author jeffloun
 * @since 1.0.0
 */

public class DerbyGridEngine extends DerbyEntityQueryEngine{
	private Collection<GridResultItem> myResults;
	
	private EntityGriddedQuery query;
	
	private String dataTable;
	private String gridTable;
	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	public Collection<GridResultItem> executeQuery(
			final EntityGriddedQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {

		this.query = query;
		dataTable = createTempTableName();
		gridTable = createTempTableName();

		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyGridEngine_Progress_RunningQuery, 4);

				try {
					Grid gridDef = new Grid(query.getGridOrigin().x, query.getGridOrigin().y, query.getGridSize(), query.getCoordinateReferenceSystem());
					IValueItem valueItem = query.getQueryDefinition().getValuePart();				
					
					//get numerator results
					Collection<GridResultItem> numeratorResults = getItems(gridDef, valueItem, query.getQueryDefinition().getValueFilter(), c, session, monitor, true);
					
					//combine with the patrol existance value
					HashMap<String, GridResultItem> items = new HashMap<String, GridResultItem>();
					for (GridResultItem it : numeratorResults){
						items.put(it.getTileId(), it);
					}

					myResults = items.values();
					
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
		return myResults;

	}

	/*
	 * Computes the grid results
	 * @param needsFilter if the values need to be filtered or if previous filter can be used
	 * 
	 */
	private Collection<GridResultItem> getItems(Grid gridDef, IValueItem value, 
			QueryFilter filter, Connection c, Session session, 
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
			//create a date filter that caches the dates so the same
			//dates are used for all parts of the query;
			//otherwise different date filters will be computed
			//for different parts of the queries
			DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
			
			try{
				filterer.processFilter(c, filter.getFilter(), dFilter, query.getConservationAreaFilterAsFilter(), 
					needsObservation, false, monitor);
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

		if(value instanceof AttributeValueItem ){
			AttributeValueItem tmp = (AttributeValueItem)value;
				
				String strAggValue = "number_value"; //$NON-NLS-1$
				strAgg = tmp.getAggregation().getName();
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
				sql.append(", smart.computeTileId(");  //$NON-NLS-1$
				sql.append( tablePrefix.get(Waypoint.class));
				sql.append(".x,");  //$NON-NLS-1$
				sql.append( tablePrefix.get(Waypoint.class));
				sql.append(".y,"); //$NON-NLS-1$
				sql.append(addParameterValue(gridDef.getCrs().toWKT()) + ","); //$NON-NLS-1$
				sql.append(addParameterValue(minX) + ","); //$NON-NLS-1$
				sql.append(addParameterValue(minY) + ","); //$NON-NLS-1$
				sql.append(addParameterValue(size));
				sql.append(") as tile_id ");  //$NON-NLS-1$
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
				String p1 = addParameterValue(key);
				sql.append(" AND keyid = " + p1); //$NON-NLS-1$ 
				
				
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
					String p2 = addParameterValue(tmp.getCategoryKey());
					String p3 = addParameterValue(tmp.getCategoryKey().substring(0, tmp.getCategoryKey().length() -1 ) + "/"); //$NON-NLS-1$
					
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(tableNames.get(Category.class));
					sql.append( " as "); //$NON-NLS-1$
					sql.append(tablePrefix.get(Category.class));
					sql.append(" on "); //$NON-NLS-1$
					sql.append(tablePrefix.get(WaypointObservation.class));
					sql.append(".category_uuid = "); //$NON-NLS-1$
					sql.append( tablePrefix.get(Category.class));
					sql.append( ".uuid" ); //$NON-NLS-1$
					sql.append(" AND Hkey >= " + p2 + " and Hkey < " + p3 + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
					String p2 = addParameterValue(tmp.getItemKey()); 
					sql.append(".keyid = " + p2); //$NON-NLS-1$
					
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
					String p2 = addParameterValue(tmp.getItemKey());
					String p3 = addParameterValue(tmp.getItemKey().substring(0, tmp.getItemKey().length() -1 ) + "/"); //$NON-NLS-1$
					sql.append(tablePrefix.get(AttributeTreeNode.class));
					sql.append(".hkey >= " + p2 + " and "); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(tablePrefix.get(AttributeTreeNode.class));
					sql.append(".hkey < " + p3 + " )"); //$NON-NLS-1$ //$NON-NLS-2$
				}

				sql.append(") as foo group by tile_id"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				rs = parseQueryString(c, sql.toString()).executeQuery();
			}else if(value instanceof CategoryValueItem){
				CategoryValueItem tmp = (CategoryValueItem)value;
				strAgg = "count"; //$NON-NLS-1$
				clearParameters();
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
				
				sql.append("smart.computeTileId("); //$NON-NLS-1$
				sql.append(tablePrefix.get(Waypoint.class)+ ".x,"); //$NON-NLS-1$
				sql.append(tablePrefix.get(Waypoint.class) + ".y," + p1 + "," + p2 + "," + p3 + "," + p4 + ") as tile_id"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
					p2 = addParameterValue(tmp.getCategoryHKey().substring(0,  tmp.getCategoryHKey().length()-1) + "/"); //$NON-NLS-1$
					sql.append("hkey >= " + p1 + " and hkey < " + p2 + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					
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
	protected EntityQueryResultItem asQueryResultItem(ResultSet rs,
			Session session) throws SQLException {
		return null;
	}
	
}