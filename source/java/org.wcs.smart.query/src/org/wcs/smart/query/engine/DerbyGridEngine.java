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
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.grids.AddCellMerger;
import org.wcs.smart.query.engine.grids.DistanceValueComputer;
import org.wcs.smart.query.engine.grids.ExistsValueComputer;
import org.wcs.smart.query.engine.grids.Grid;
import org.wcs.smart.query.engine.grids.GridAnalysisEngine;
import org.wcs.smart.query.engine.grids.PatrolCntCellMerger;
import org.wcs.smart.query.engine.grids.PatrolCntValueComputer;
import org.wcs.smart.query.engine.grids.PatrolDayCntValueComputer;
import org.wcs.smart.query.engine.grids.PatrolExistsCellMerger;
import org.wcs.smart.query.engine.grids.Tile;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.summary.AttributeValueItem;
import org.wcs.smart.query.parser.internal.summary.CategoryValueItem;
import org.wcs.smart.query.parser.internal.summary.CategoryValueItem.ValueType;
import org.wcs.smart.query.parser.internal.summary.CombinedValueItem;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.PatrolValueItem;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;

public class DerbyGridEngine extends DerbyQueryEngine2{
	private Collection<GridResultItem> myResults;
	
	protected static final String QUERY_GRID_TEMP_TABLE_PREFIX = "grid_intermediate_"; //$NON-NLS-1$
	protected String gridTempTable = "";	 //$NON-NLS-1$

	private GriddedQuery query;
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
			final GriddedQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {

		this.query = query;
		queryTempTable = QUERY_TEMP_TABLE_PREFIX + System.nanoTime();
		observationTempTable = QUERY_OB_TEMP_TABLE_PREFIX + System.nanoTime();
		gridTempTable = QUERY_GRID_TEMP_TABLE_PREFIX + System.nanoTime();

		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyGridEngine_Progress_RunningQuery, 4);

				try {
					monitor.subTask(Messages.DerbyGridEngine_Progress_CreatingObservationTable);
					IFilter qFilter = query.getFilter();
					if (qFilter == null){
						return;
					}
					if (qFilter != IFilter.EMPTY_FILTER && qFilter.hasAttributeFilter()) {
						createObservationTable(c, query.getFilter(), query.getDateFilter(), query.getConservationAreaFilter());
					}
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}

					boolean needsObservation = false;
					if (query.getQueryDefinition().getValuePart().hasCategory() ||
							query.getQueryDefinition().getValuePart().hasAttribute() ||
							query.getFilter().hasCategoryFilter() || 
							query.getFilter().hasAttributeFilter() ){
						needsObservation = true;
					}
					
					monitor.subTask(Messages.DerbyGridEngine_Progress_CreatingTempTable);
					createTemporaryTable(c);

					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbyGridEngine_Progress_PopulatingResults);
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilter(), false, c, needsObservation);
					monitor.worked(1);

					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbyGridEngine_Progress_CalculatingGridValue);
					
					Grid gridDef = new Grid(query.getGridOrigin().x, query.getGridOrigin().y, query.getGridSize(), query.getCoordinateReferenceSystem());
					
					//select the tile_id, and value that we want to show on the grid
					myResults = getGridResults(c, session, gridDef, query.getQueryDefinition().getValuePart());
					
					monitor.worked(1);
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
					monitor.done();
				}
			}

		});
		return myResults;

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

		if (value instanceof CombinedValueItem){
			Collection<GridResultItem> value1 = getGridResults(c, session, gridDef, ((CombinedValueItem)value).getPart1());
			Collection<GridResultItem> value2 = getGridResults(c, session, gridDef, ((CombinedValueItem)value).getPart2());
			
			//merge the results based on tile ids
			HashMap<Tile, Double> values2 = new HashMap<Tile, Double>();
			for (Iterator<GridResultItem> iterator = value2.iterator(); iterator.hasNext();) {
				GridResultItem gridResultItem = (GridResultItem) iterator.next();
				values2.put(new Tile(gridResultItem.getTileX(), gridResultItem.getTileY()), gridResultItem.getValue());				
			}
			
			
			for (Iterator<GridResultItem> iterator = value1.iterator(); iterator.hasNext();) {
				GridResultItem gridResultItem = (GridResultItem) iterator.next();
				
				Double denominator= values2.get(new Tile(gridResultItem.getTileX(), gridResultItem.getTileY()));
				if (denominator == null){
					//no data
					iterator.remove();
				}else if (denominator == 0){
					//error - cannot divide by 0
					gridResultItem.setValue(Double.NaN);
				}else{
					gridResultItem.setValue(gridResultItem.getValue() / denominator);
				}
			}
			return value1;
		}
		HashMap<String, GridResultItem> items;
		if(value instanceof PatrolValueItem ){
			items = computePatrolValue(c, (PatrolValueItem)value, gridDef);
		}else{
		
		if(value instanceof AttributeValueItem ){
			AttributeValueItem tmp = (AttributeValueItem)value;
			strAgg = tmp.getAggregation().getName(); 
			String key = tmp.getAttributeKey();
			
			double minX = gridDef.getOriginX();
			double minY = gridDef.getOriginY();
			double size = gridDef.getCellSize();
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT " + strAgg + "(number_value) as value, tile_id"); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" FROM ("); //$NON-NLS-1$
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(" number_value,  "); //$NON-NLS-1$
			sql.append("smart.computeTileId(" + tablePrefix.get(Waypoint.class)+ ".x," + tablePrefix.get(Waypoint.class) + ".y,'" + gridDef.getCrs().toWKT().replaceAll("'", "''") + "'," + minX + "," + minY + "," + size + ") as tile_id"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
			sql.append(" FROM " + tableNames.get(WaypointObservation.class) + " as " + tablePrefix.get(WaypointObservation.class)); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN " + queryTempTable  //$NON-NLS-1$
				+ " on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
				+ ".uuid = " //$NON-NLS-1$
				+ queryTempTable
				+ ".ob_uuid"); //$NON-NLS-1$
			sql.append(" JOIN " + tableNames.get(WaypointObservationAttribute.class) + " as " + tablePrefix.get(WaypointObservationAttribute.class)  //$NON-NLS-1$ //$NON-NLS-2$
				+ " on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
				+ ".uuid = " //$NON-NLS-1$
				+ tablePrefix.get(WaypointObservationAttribute.class)
				+ ".observation_uuid"); //$NON-NLS-1$
			sql.append(" JOIN " + tableNames.get(Attribute.class) + " as " + tablePrefix.get(Attribute.class)  //$NON-NLS-1$ //$NON-NLS-2$
					+ " on " + tablePrefix.get(WaypointObservationAttribute.class) //$NON-NLS-1$
					+ ".attribute_uuid = " //$NON-NLS-1$
					+ tablePrefix.get(Attribute.class)
					+ ".uuid" //$NON-NLS-1$
					+ " AND keyid = '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN " + tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class)  //$NON-NLS-1$ //$NON-NLS-2$
				+ " on " + tablePrefix.get(Waypoint.class) //$NON-NLS-1$
				+ ".uuid = " //$NON-NLS-1$
				+ queryTempTable
				+ ".wp_uuid"); //$NON-NLS-1$
			sql.append(") as foo group by tile_id"); //$NON-NLS-1$
		
			QueryPlugIn.logSql(sql.toString());
			rs = c.createStatement().executeQuery(sql.toString());
		}else if(value instanceof CategoryValueItem){
			CategoryValueItem tmp = (CategoryValueItem)value;
			strAgg = "count"; //$NON-NLS-1$
			String hkey = tmp.getCategoryHKey();
			String hkey_max = hkey.substring(0,(hkey.length()-1)) + "/"; //$NON-NLS-1$
					
			double minX = gridDef.getOriginX();
			double minY = gridDef.getOriginY();
			double size = gridDef.getCellSize();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(strAgg + "(localkey) as value,tile_id"); //$NON-NLS-1$
			sql.append(" FROM ("); //$NON-NLS-1$
			sql.append("SELECT distinct "); //$NON-NLS-1$
			if (tmp.getType() == ValueType.OBSERVATION){
				sql.append(tablePrefix.get(WaypointObservation.class) +".uuid as localkey, "); //$NON-NLS-1$
			}else{
				sql.append(queryTempTable + ".wp_uuid as localkey, "); //$NON-NLS-1$
			}
			sql.append("smart.computeTileId(" + tablePrefix.get(Waypoint.class)+ ".x," + tablePrefix.get(Waypoint.class) + ".y,'" + gridDef.getCrs().toWKT().replaceAll("'", "''") + "'," + minX + "," + minY + "," + size + ") as tile_id"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
			sql.append(" FROM " + tableNames.get(WaypointObservation.class) + " as " + tablePrefix.get(WaypointObservation.class)); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN " + queryTempTable  //$NON-NLS-1$
				+ " on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
				+ ".uuid = " //$NON-NLS-1$
				+ queryTempTable
				+ ".ob_uuid"); //$NON-NLS-1$
			sql.append(" JOIN " + tableNames.get(Category.class) + " as " + tablePrefix.get(Category.class)  //$NON-NLS-1$ //$NON-NLS-2$
				+ " on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
				+ ".category_uuid = " //$NON-NLS-1$
				+ tablePrefix.get(Category.class)
				+ ".uuid" //$NON-NLS-1$
				+ " AND Hkey >= '" + hkey + "' AND Hkey < '" + hkey_max + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
			sql.append(" JOIN " + tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class)  //$NON-NLS-1$ //$NON-NLS-2$
				+ " on " + tablePrefix.get(Waypoint.class) //$NON-NLS-1$
				+ ".uuid = " //$NON-NLS-1$
				+ queryTempTable
				+ ".wp_uuid"); //$NON-NLS-1$
			sql.append(") as foo "); //$NON-NLS-1$
			sql.append(" group by "); //$NON-NLS-1$
			sql.append("tile_id"); //$NON-NLS-1$
		
			QueryPlugIn.logSql(sql.toString());
			rs = c.createStatement().executeQuery(sql.toString());
			
		
		}else{
			throw new SQLException(Messages.DerbyGridEngine_Error_GridValueNotSupported);	
		}
		
		try {
			items = new HashMap<String, GridResultItem>();
			while (rs.next()) {
				GridResultItem it = new GridResultItem();
				
				String tid = rs.getString("TILE_ID"); //$NON-NLS-1$
				String[] tileids = tid.split("_"); //$NON-NLS-1$
				
				it.setTileX(Long.parseLong(tileids[0]));
				it.setTileY(Long.parseLong(tileids[1]));
				it.setValue(rs.getDouble("value")); //$NON-NLS-1$
				
				items.put(tid, it);
				if (items.size() > Grid.MAX_GRID_CELLS){
					throw Grid.GRID_TO_BIG_EXCEPTION;
				}
			}
		} finally {
			rs.close();
		}
		}
		
		
		//combine the two if patrol and no count then we want the
		//value 0 to display otherwise we keep the count value
		List<GridResultItem> patrolLocations = computePatrolExistance(c, gridDef);
		for (GridResultItem it : patrolLocations){
			if (items.get(it.getTileId()) == null){ 
				GridResultItem newitem = new GridResultItem();
				newitem.setTileX(it.getTileX());
				newitem.setTileY(it.getTileY());
				newitem.setValue(0);
				items.put(it.getTileId(), newitem); 
			}
		}
		
		return items.values();
	}

	private List<GridResultItem> computePatrolExistance(Connection c, Grid gridDef) throws Exception{
		GridAnalysisEngine<?> engine = null;
		
		PatrolExistsCellMerger cellMerger = new PatrolExistsCellMerger();
		ExistsValueComputer valueComputer = new ExistsValueComputer();
		
		engine = new GridAnalysisEngine<Boolean>(gridDef, cellMerger, valueComputer);
		return computePatrolTrackNoFilter(c, engine);
	}
	
	private HashMap<String,GridResultItem> computePatrolValue(Connection c,
			PatrolValueItem item, 
			Grid gridDef) throws Exception{
		GridAnalysisEngine<?> engine = null;
		String dataField[] = null;
		
		if (item.getOption() == PatrolValueOption.DISTANCE){
			AddCellMerger cellMerger = new AddCellMerger();	//adds cell values
			DistanceValueComputer valueComputer = new DistanceValueComputer();
			engine = new GridAnalysisEngine<Double>(gridDef, cellMerger, valueComputer);
		}else if (item.getOption() == PatrolValueOption.NUM_DAYS){	
			dataField = new String[]{"p_uuid", "pld_patrol_day"}; //$NON-NLS-1$ //$NON-NLS-2$
			PatrolCntCellMerger cellMerger = new PatrolCntCellMerger();
			PatrolDayCntValueComputer valueComputer = new PatrolDayCntValueComputer();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
		}else if (item.getOption() == PatrolValueOption.NUM_PATROLS){
			dataField = new String[]{"p_uuid"}; //$NON-NLS-1$
			PatrolCntCellMerger cellMerger = new PatrolCntCellMerger();
			PatrolCntValueComputer valueComputer = new PatrolCntValueComputer();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
			
		}else{
			throw new UnsupportedOperationException(
					MessageFormat.format(Messages.DerbyGridEngine_Error_PatrolValueNotSupported, new Object[]{item.getOption().getGuiName()}));
		}
		return computePatrolTrack(c, engine, dataField);
	}
	
	private HashMap<String,GridResultItem> computePatrolTrack(Connection c, 
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
		sql.append(queryTempTable);
		sql.append(") tmp "); //$NON-NLS-1$
		sql.append("WHERE " ); //$NON-NLS-1$
		sql.append(tablePrefix.get(Track.class) + ".patrol_leg_day_uuid = "); //$NON-NLS-1$
		sql.append("tmp.pld_uuid"); //$NON-NLS-1$

		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		WKBReader reader = new WKBReader();
		
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
					QueryPlugIn.log("Error rasterizing linestring: " + ls.toText(), ex); //$NON-NLS-1$
					throw ex;
				}
			}
		}
		rs.close();
		
		HashMap<String, GridResultItem> items = new HashMap<String, GridResultItem>();
		for (Iterator<Entry<Tile,Double>> iterator = engine.getData().entrySet().iterator(); iterator.hasNext();) {
			Entry<Tile,Double> object = (Entry<Tile,Double>) iterator.next();
			GridResultItem it = new GridResultItem();
			it.setTileX(object.getKey().getXId()+1);
			it.setTileY(object.getKey().getYId()+1);
			it.setValue(object.getValue());
			items.put(it.getTileId(), it);
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
			GridAnalysisEngine<?> engine) throws Exception{
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
		DateFilter dateFilter = query.getDateFilter();
		if (dateFilter != null ){
			String dfilter = dateFilter.asSql(tablePrefix);
			if (dfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(dfilter);
			}
		}
		sql.append( " and "); //$NON-NLS-1$
		sql.append(query.getConservationAreaFilter().asSql(tablePrefix));
		
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		WKBReader reader = new WKBReader();
		
		while(rs.next()){
			byte[] bytes = rs.getBytes("geom"); //$NON-NLS-1$
			if (bytes != null){
				LineString ls = (LineString) reader.read(bytes);
				try{
					engine.rasterizeLinestring(ls);
				}catch (Exception ex){
					QueryPlugIn.log("Error rasterizing linestring: " + ls.toText(), ex); //$NON-NLS-1$
					throw ex;
				}
			}
		}
		rs.close();
		
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
		try {
			String sql = "DROP TABLE " + gridTempTable; //$NON-NLS-1$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}

	}

	
	
}