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
import java.util.ArrayList;
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
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.grids.AddCellMerger;
import org.wcs.smart.query.engine.grids.DistanceValueComputer;
import org.wcs.smart.query.engine.grids.Grid;
import org.wcs.smart.query.engine.grids.GridAnalysisEngine;
import org.wcs.smart.query.engine.grids.PatrolCntCellMerger;
import org.wcs.smart.query.engine.grids.PatrolCntValueComputer;
import org.wcs.smart.query.engine.grids.PatrolDayCntValueComputer;
import org.wcs.smart.query.engine.grids.Tile;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.summary.AttributeValueItem;
import org.wcs.smart.query.parser.internal.summary.CategoryValueItem;
import org.wcs.smart.query.parser.internal.summary.CombinedValueItem;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.PatrolValueItem;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;

public class DerbyGridEngine extends DerbyQueryEngine2{
	private List<GridResultItem> myResults;
	
	protected static final String QUERY_GRID_TEMP_TABLE_PREFIX = "grid_intermediate_";
	protected String gridTempTable = "";	

	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	public List<GridResultItem> executeQuery(
			final GriddedQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {

		queryTempTable = QUERY_TEMP_TABLE_PREFIX + System.nanoTime();
		observationTempTable = QUERY_OB_TEMP_TABLE_PREFIX + System.nanoTime();
		gridTempTable = QUERY_GRID_TEMP_TABLE_PREFIX + System.nanoTime();

		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask("Running Query.", 4);

				try {
					monitor.subTask("Creating observation table");
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
							query.getQueryDefinition().getValuePart().hasAttribute()){
						needsObservation = true;
					}
					
					monitor.subTask("Creating temporary table");
					createTemporaryTable(c);

					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Populating results table");
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilter(), false, c, needsObservation);
					monitor.worked(1);

					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Calculating Grid Values");
					
					Grid gridDef = new Grid(query.getGridOrigin().x, query.getGridOrigin().y, query.getGridSize(), query.getCoordinateReferenceSystem());
					
					//select the tile_id, and value that we want to show on the grid
					myResults = getGridResults(c, session, gridDef, query.getQueryDefinition().getValuePart());
					
					monitor.worked(1);
				}catch (Exception ex){
					throw new SQLException("Failed to process grid query: " + ex.getMessage(), ex);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
					monitor.done();
				}
			}

		});
		return myResults;

	}


	protected double[] getCoordinateMins(Connection c, IFilter filter) throws SQLException {
		
		HashSet<AttributeInfo> keys = new HashSet<AttributeInfo>();
		filter.getAttributeFilters(keys);
		
		StringBuilder sql = new StringBuilder();
//		sql.append("SELECT min(\"X\") as minx, min(\"Y\") as miny FROM " + queryTempTable);
		sql.append("SELECT min(w_x) as minx, min(w_y) as miny  FROM (");
		sql.append(" SELECT ");
		sql.append(SelectClause(true));
		sql.append(" FROM ");
		sql.append(FromClause(true));
		sql.append(" WHERE ");
		sql.append(WhereClause(true));
		sql.append(" UNION ");
		sql.append(" SELECT ");
		sql.append(SelectClause(false));
		sql.append(" FROM ");
		sql.append(FromClause(false));
		sql.append(" WHERE ");
		sql.append(WhereClause(false));
		sql.append(" ) as foo");

		
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		double minx=0;
		double miny=0;
		if(rs.next()){
			minx = rs.getDouble("minx");
			miny = rs.getDouble("miny");
		}
		
		double[] both = new double[2];
		both[0] = minx;
		both[1] = miny;

		return both;
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
	protected List<GridResultItem> getGridResults(Connection c, 
			Session session, Grid gridDef, IValueItem value)
			throws Exception {
		List<GridResultItem> items = new ArrayList<GridResultItem>();
		
		String strAgg ="";
		ResultSet rs;

		if (value instanceof CombinedValueItem){
			List<GridResultItem> value1 = getGridResults(c, session, gridDef, ((CombinedValueItem)value).getPart1());
			List<GridResultItem> value2 = getGridResults(c, session, gridDef, ((CombinedValueItem)value).getPart2());
			
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
		
		if(value instanceof PatrolValueItem ){
			return computePatrolValue(c, (PatrolValueItem)value, gridDef);
		}
		
		if(value instanceof AttributeValueItem ){
			AttributeValueItem tmp = (AttributeValueItem)value;
			strAgg = tmp.getAggregation().getName(); 
			String key = tmp.getAttributeKey();
			
			double minX = gridDef.getOriginX();
			double minY = gridDef.getOriginY();
			double size = gridDef.getCellSize();
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT " + strAgg + "(number_value) as value, tile_id");
			sql.append(" FROM (");
			sql.append("SELECT ");
			sql.append(" number_value,  ");
			sql.append("smart.computeTileId(" + tablePrefix.get(Waypoint.class)+ ".x," + tablePrefix.get(Waypoint.class) + ".y,'" + gridDef.getCrs().toWKT().replaceAll("'", "''") + "'," + minX + "," + minY + "," + size + ") as tile_id");
			sql.append(" FROM " + tableNames.get(WaypointObservation.class) + " as " + tablePrefix.get(WaypointObservation.class));
			sql.append(" JOIN " + queryTempTable 
				+ " on " + tablePrefix.get(WaypointObservation.class)
				+ ".uuid = "
				+ queryTempTable
				+ ".ob_uuid");
			sql.append(" JOIN " + tableNames.get(WaypointObservationAttribute.class) + " as " + tablePrefix.get(WaypointObservationAttribute.class) 
				+ " on " + tablePrefix.get(WaypointObservation.class)
				+ ".uuid = "
				+ tablePrefix.get(WaypointObservationAttribute.class)
				+ ".observation_uuid");
			sql.append(" JOIN " + tableNames.get(Attribute.class) + " as " + tablePrefix.get(Attribute.class) 
					+ " on " + tablePrefix.get(WaypointObservationAttribute.class)
					+ ".attribute_uuid = "
					+ tablePrefix.get(Attribute.class)
					+ ".uuid"
					+ " AND keyid = '" + key + "'");
			sql.append(" JOIN " + tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class) 
				+ " on " + tablePrefix.get(Waypoint.class)
				+ ".uuid = "
				+ queryTempTable
				+ ".wp_uuid");
			sql.append(") as foo group by tile_id");
		
			QueryPlugIn.logSql(sql.toString());
			rs = c.createStatement().executeQuery(sql.toString());
		}else if(value instanceof CategoryValueItem){
			CategoryValueItem tmp = (CategoryValueItem)value;
			strAgg = "count";
			String hkey = tmp.getCategoryHKey();
			String hkey_max = hkey.substring(0,(hkey.length()-1)) + "/";
					
			double minX = gridDef.getOriginX();
			double minY = gridDef.getOriginY();
			double size = gridDef.getCellSize();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ");
			sql.append(strAgg + "(keyid) as value,tile_id");
			sql.append(" FROM (");
			sql.append("SELECT keyid, ");
			sql.append("smart.computeTileId(" + tablePrefix.get(Waypoint.class)+ ".x," + tablePrefix.get(Waypoint.class) + ".y,'" + gridDef.getCrs().toWKT().replaceAll("'", "''") + "'," + minX + "," + minY + "," + size + ") as tile_id");
			sql.append(" FROM " + tableNames.get(WaypointObservation.class) + " as " + tablePrefix.get(WaypointObservation.class));
			sql.append(" JOIN " + queryTempTable 
				+ " on " + tablePrefix.get(WaypointObservation.class)
				+ ".uuid = "
				+ queryTempTable
				+ ".ob_uuid");
			sql.append(" JOIN " + tableNames.get(Category.class) + " as " + tablePrefix.get(Category.class) 
				+ " on " + tablePrefix.get(WaypointObservation.class)
				+ ".category_uuid = "
				+ tablePrefix.get(Category.class)
				+ ".uuid"
				+ " AND Hkey >= '" + hkey + "' AND Hkey < '" + hkey_max + "'");
			
//			WHERE ( c.hkey >= 'threat.residentialcommercialdevelopment.' and c.hkey < 'threat.residentialcommercialdevelopment/') 
			
			sql.append(" JOIN " + tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class) 
				+ " on " + tablePrefix.get(Waypoint.class)
				+ ".uuid = "
				+ queryTempTable
				+ ".wp_uuid");
			//sql.append(" group by (floor(  (X - " + minX + ") /" + size + " ) + 1), (floor(  (Y - " + minY + ") / " + size + " ) + 1)");
			//sql.append(" group by tile_id");
			sql.append(") as foo ");
			sql.append(" group by ");
			sql.append("tile_id");
		
			QueryPlugIn.logSql(sql.toString());
			rs = c.createStatement().executeQuery(sql.toString());
			
		
		}else{
			throw new SQLException("Not an Attribute or Category Selected");	
		}
		try {

			while (rs.next()) {

				GridResultItem it = new GridResultItem();
				it.setValue(rs.getDouble("value"));
				String[] tileids = rs.getString("TILE_ID").split("_");
				
				it.setTileX(Integer.parseInt(tileids[0]));
				it.setTileY(Integer.parseInt(tileids[1]));

				items.add(it);
			}
		} finally {
			rs.close();
		}
		return items;
	}

	private List<GridResultItem> computePatrolValue(Connection c,
			PatrolValueItem item, 
			Grid gridDef) throws Exception{

		GridAnalysisEngine<?> engine = null;
		String dataField[] = null;
		
		
		if (item.getOption() == PatrolValueOption.DISTANCE){
			AddCellMerger<Double> cellMerger = new AddCellMerger<Double>();	//adds cell values
			DistanceValueComputer<Double> valueComputer = new DistanceValueComputer<Double>();
			engine = new GridAnalysisEngine<Double>(gridDef, cellMerger, valueComputer);
		}else if (item.getOption() == PatrolValueOption.NUM_DAYS){	
			dataField = new String[]{"p_uuid", "pld_patrol_day"};
			PatrolCntCellMerger<HashSet<String>> cellMerger = new PatrolCntCellMerger<HashSet<String>>();
			PatrolDayCntValueComputer<HashSet<String>> valueComputer = new PatrolDayCntValueComputer<HashSet<String>>();
			engine = new GridAnalysisEngine<HashSet<String>>(gridDef, cellMerger, valueComputer);
		}else if (item.getOption() == PatrolValueOption.NUM_PATROLS){
			dataField = new String[]{"p_uuid"};
			PatrolCntCellMerger<HashSet<Object>> cellMerger = new PatrolCntCellMerger<HashSet<Object>>();
			PatrolCntValueComputer<HashSet<Object>> valueComputer = new PatrolCntValueComputer<HashSet<Object>>();
			engine = new GridAnalysisEngine<HashSet<Object>>(gridDef, cellMerger, valueComputer);
			
		}else{
			throw new UnsupportedOperationException("Patrol value option " + item.getOption().getGuiName() + " not supported for grid analysis.");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + tablePrefix.get(Track.class) + ".geometry as geom ");
		if (dataField != null){
			for (int i = 0; i < dataField.length; i ++){
				//additional data required for rasterization
				sql.append(", tmp." + dataField[i] + " as dataField_" + i);
			}
		}
		sql.append(" FROM ");
		sql.append(tableNames.get(Track.class) + " " + tablePrefix.get(Track.class));
		sql.append(", (");
		sql.append("SELECT distinct pld_uuid ");
		if (dataField != null){
			//additional data required for rasterization
			for (int i = 0; i < dataField.length; i ++){
				sql.append(", " + dataField[i]);
			}
		}
		sql.append(" from " );
		sql.append(queryTempTable);
		sql.append(") tmp ");
		sql.append("WHERE " );
		sql.append(tablePrefix.get(Track.class) + ".patrol_leg_day_uuid = ");
		sql.append("tmp.pld_uuid");

		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		WKBReader reader = new WKBReader();
		
		while(rs.next()){
			byte[] bytes = rs.getBytes("geom");
			Object[] data = null;
			if (dataField != null){
				data = new Object[dataField.length];
				for (int i = 0; i < dataField.length; i++){
					data[i] = rs.getObject("dataField_"+i);
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
					QueryPlugIn.log("Error rasterizing linestring: " + ls.toText(), ex);
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
			String sql = "DROP TABLE " + gridTempTable;
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}

	}

	
	
}