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
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.summary.AttributeValueItem;
import org.wcs.smart.query.parser.internal.summary.CategoryValueItem;
import org.wcs.smart.query.parser.internal.summary.CombinedValueItem;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.PatrolValueItem;

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

					monitor.subTask("Creating temporary table");
					createTemporaryTable(c);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Populating results table");
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilter(), false, c);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Calculating Grid Values");
					//double[] mins = getCoordinateMins(c, qFilter);
					double[] mins = {0,0};
					
					double size = query.getGridSize();

					//select the tile_id, and value that we want to show on the grid
					myResults = getGridResults(c, session, mins, size, query.getValueItem());
					
					monitor.worked(1);
					
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
	protected List<GridResultItem> getGridResults(Connection c, Session session, double[] mins, double size, IValueItem value)
			throws SQLException {
		List<GridResultItem> items = new ArrayList<GridResultItem>();
		
		String strAgg ="";
		ResultSet rs;
		//TODO: calculate rations properly, this just uses the numerator as a single value for now.
		if(value instanceof CombinedValueItem){
			CombinedValueItem cmbTmp = (CombinedValueItem)value;
			value = cmbTmp.getPart1();
		}
		
		//TODO: take this out once we can do patrol items
		if(value instanceof PatrolValueItem ){
			throw new SQLException("Cannot process Patrol Values in this software version.");
		}
		
		
		if(value instanceof AttributeValueItem ){
			AttributeValueItem tmp = (AttributeValueItem)value;
			strAgg = tmp.getAggregation().getName(); 
		
			double minX = mins[0];
			double minY = mins[1];
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ");
			sql.append( strAgg + "(number_value) as value,  min(floor(  (X - " + minX + ") /" + size + " ) + 1) as TILE_X , min(floor(  (Y - " + minY + ") / " + size + " ) + 1) as TILE_Y ");
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
			sql.append(" JOIN " + tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class) 
				+ " on " + tablePrefix.get(Waypoint.class)
				+ ".uuid = "
				+ queryTempTable
				+ ".wp_uuid");
			sql.append(" group by (floor(  (X - " + minX + ") /" + size + " ) + 1), (floor(  (Y - " + minY + ") / " + size + " ) + 1)");
		
			QueryPlugIn.logSql(sql.toString());
			rs = c.createStatement().executeQuery(sql.toString());
		}else if(value instanceof CategoryValueItem){
			CategoryValueItem tmp = (CategoryValueItem)value;
			strAgg = "count";
			String hkey = tmp.getCategoryHKey();
			String hkey_max = hkey.substring(0,(hkey.length()-1)) + "/";
					
			double minX = mins[0];
			double minY = mins[1];
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ");
			sql.append( strAgg + "(keyid) as value,  min(floor(  (X - " + minX + ") /" + size + " ) + 1) as TILE_X , min(floor(  (Y - " + minY + ") / " + size + " ) + 1) as TILE_Y ");
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
			sql.append(" group by (floor(  (X - " + minX + ") /" + size + " ) + 1), (floor(  (Y - " + minY + ") / " + size + " ) + 1)");
		
			QueryPlugIn.logSql(sql.toString());
			rs = c.createStatement().executeQuery(sql.toString());
			
		
		}else{
			throw new SQLException("Not an Attribute or Category Selected");	
		}
		try {

			while (rs.next()) {

				GridResultItem it = new GridResultItem();
				it.setValue(rs.getDouble("value"));
				it.setTileX(rs.getInt("TILE_X"));
				it.setTileY(rs.getInt("TILE_Y"));

				items.add(it);
			}
		} finally {
			rs.close();
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
//			String sql = "DROP TABLE " + QUERY_TEMP_SCHEMA + "." + observationTempTable;

			String sql = "DROP TABLE " + gridTempTable;
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}

	}

	
	
}