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
package org.wcs.smart.connect.query.engine.observation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;

/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link DerbyPagedObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PsqlObsObservationEngine extends AbstractQueryEngine {

	private final Logger logger = Logger.getLogger(PsqlObsObservationEngine.class.getName());
	
	private String queryDataTable;
	private SimpleQuery query;

	public String getQueryDataTable(){
		return this.queryDataTable;
	}
	
	@Override
	public boolean canExecute(String querytype) {
		return ObsObservationQuery.KEY.equals(querytype);
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
		this.query = (SimpleQuery) lquery;
		this.locale = (Locale) parameters.get(Locale.class.getName());
		final Session session = (Session) parameters.get(Session.class.getName());
		
		if (query.getDateFilter() == null){
			return null;
		}
		queryDataTable = createTempTableName();
		
		return session.doReturningWork(new ReturningWork<ObsObservationQueryResult>() {
			@Override
			public ObsObservationQueryResult execute(Connection c) throws SQLException {
				IFilterProcessor filterer = null;	
				int itemcnt = 0;
				try {
					filterer = PsqlObsObservationEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable);
					
					//create a date filter that caches the dates so the same
					//dates are used for all parts of the query;
					//otherwise different date filters will be computed
					//for different parts of the queries
					DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));
					
					parseConservationAreaFilterInternal(query);
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, caFilter, true, true);
					populateTemporaryTableExtra(c, caFilter, session);
					
					//item cnt
					try(ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM " + getQueryDataTable())){ //$NON-NLS-1$
						rs.next();
						itemcnt = rs.getInt(1);
					}
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
				}
				c.commit();
				
				return new ObsObservationQueryResult(PsqlObsObservationEngine.this, itemcnt);
			}
		});
	}
	
	private void populateTemporaryTableExtra(Connection c,ConservationAreaFilter caFilter, Session session) throws SQLException {
		//NOTE: does 50 worked for monitor in total
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ob_observer","varchar(512)"}, //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}
		//ca details
		populateCaDetails(c, queryDataTable, "p_ca_uuid",query); //$NON-NLS-1$
		
		//add observers
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		logger.finest(sql.toString());

		
		String updateSql = "UPDATE "+queryDataTable+" SET "; //$NON-NLS-1$ //$NON-NLS-2$
		String q1 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		logger.finest(q1);
		PreparedStatement observerSt = c.prepareStatement(q1);
		int cnt = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())){
			while (rs.next()) {
				UUID uuid = (UUID) rs.getObject(1);
				if (uuid == null) continue;
				String name = getEmployeeName(uuid, session);
				
				if (name != null) {
					observerSt.setString(1, name);
					observerSt.setObject(2, uuid);
					observerSt.addBatch();
					cnt++;
					if (cnt >= 100){
						observerSt.executeBatch();
						cnt = 0;
					}
				}
			}
			observerSt.executeBatch();
		}

		populateTemporaryTableCategory(c, session, caFilter, queryDataTable);
		populateAdditionalWpoaTable(c, queryDataTable + "_list", "list_element_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		populateAdditionalWpoaTable(c, queryDataTable + "_tree", "tree_node_uuid"); //$NON-NLS-1$ //$NON-NLS-2$

	}

	private void populateAdditionalWpoaTable(Connection c, String tableName, String obsAttUuidColumn) throws SQLException {
		String sql = "CREATE TABLE " + tableName + " (uuid uuid, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql);

		sql = "INSERT INTO " + tableName + " (uuid) SELECT DISTINCT wpoa." + obsAttUuidColumn //$NON-NLS-1$ //$NON-NLS-2$
				+" FROM "  //$NON-NLS-1$
				+ tableNamePrefix(WaypointObservationAttribute.class) + " inner join " //$NON-NLS-1$
				+ queryDataTable + " r on " //$NON-NLS-1$
				+ tablePrefix(WaypointObservationAttribute.class) + ".OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$
		logger.finest(sql.toString());
		c.createStatement().execute(sql);
		
		updateLabel(c, tableName, "uuid", "value"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	

	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".source, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid "); //$NON-NLS-1$

		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid uuid,"); //$NON-NLS-1$
		sql.append("wp_uuid uuid,"); //$NON-NLS-1$ 
		sql.append("wp_source varchar(16),"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double precision,"); //$NON-NLS-1$
		sql.append("wp_y double precision,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("ob_uuid uuid,"); //$NON-NLS-1$
		sql.append("ob_observer_uuid uuid,"); //$NON-NLS-1$
		sql.append("ob_category_uuid uuid"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		super.buildTemporaryTableIndexes(c, tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("create index "); //$NON-NLS-1$
		sql.append(getIndexName(tableName));
		sql.append("_ob_category_uuid_idx on "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("(ob_category_uuid)"); //$NON-NLS-1$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
	}
	
	@Override
	public void cleanUp(Session session) throws SQLException {
		dropTable(session, queryDataTable);
		dropTable(session, queryDataTable + "_LIST"); //$NON-NLS-1$
		dropTable(session, queryDataTable + "_TREE"); //$NON-NLS-1$
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}

	protected IFilterProcessor getFilterProcessor(FilterType filterType,
			String queryDataTable) {
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new ObsFilterProcessor(queryDataTable, this);
		}else{
			return new ObsWaypointFilterProcessor(queryDataTable, this);
		}
	}

}
