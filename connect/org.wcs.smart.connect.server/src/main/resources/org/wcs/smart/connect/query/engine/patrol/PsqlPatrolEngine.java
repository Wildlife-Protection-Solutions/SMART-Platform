/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.patrol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;

public class PsqlPatrolEngine extends AbstractQueryEngine{

	private Logger logger = Logger.getLogger(PsqlPatrolEngine.class.getName());
	
	private String filterTable;
	private String queryDataTable;
	
	@Override
	public boolean canExecute(String querytype) {
		return PatrolQuery.KEY.equals(querytype);
	}
	
	public String getDataQuery(){
		StringBuilder fields = new StringBuilder();
		fields.append("ca_id,ca_name, r_p_ca_uuid, r_p_uuid,");
		fields.append("r_p_id,r_p_start_date,r_p_end_date,r_p_station_uuid,");
		fields.append("r_p_team_uuid,r_p_objective,r_p_mandate_uuid,r_p_type,");
		fields.append("r_p_is_armed,r_pl_transport_uuid,r_pl_id,r_pl_start_date,");
		fields.append("r_pl_end_date,r_plm_leader,r_plm_pilot,r_pl_uuid,");
		fields.append("p_station,p_team,p_mandate,p_transporttype,p_leader,p_pilot");
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(fields.toString());
		sb.append(",st_astext(st_collect(st_geomfromwkb(r_track))) as track ");
		sb.append("FROM ");
		sb.append(queryDataTable);
		sb.append(" GROUP BY ");
		sb.append(fields.toString());
		 
		return sb.toString();
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

		final SimpleQuery query = (SimpleQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		locale = (Locale)parameters.get(Locale.class.getName());
		
		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		filterTable = createTempTableName();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				
				IFilterProcessor filterer = null;
				try{
					filterer = getFilterProcessor(query.getFilter().getFilterType(), filterTable);
				}catch (Exception ex){
					throw new SQLException (ex);
				}
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				try {
					ConservationAreaFilter cafilter = AbstractQueryEngine.parseConservationAreaFilter(lquery);
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, cafilter, false, false);
					getResults(c, session);
					
					c.commit();
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					c.rollback();
					if (ex instanceof SQLException) throw (SQLException)ex;
					throw new SQLException(ex);
				} finally {
					// ensure temporary tables get dropped
					try{
						filterer.dropTemporaryTables(c);
						dropTable(c, filterTable);
						c.commit();
					}catch (Exception ex){
						c.rollback();
						logger.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
			}
		});
		return new PatrolQueryResult(this);
	}

	/**
	 * Reads the results from the temporary query table
	 * and loads them into internal memory store
	 * 
	 * @param c database connection 
	 * @param session hibernate session
	 * @return list of query results
	 * 
	 * @throws SQLException
	 */
	protected void getResults(Connection c, Session session)
			throws SQLException {
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("CREATE TABLE " + queryDataTable + " AS ");
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(buildSelectClause());
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(buildFromClause());
		sql.append(" ORDER BY p_id, pl_uuid, pld_uuid "); //$NON-NLS-1$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
		populateTemporaryTableExtra(c, session);
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session) throws SQLException {
		//NOTE: does 50 worked for monitor in total
		String[][] columnsToAdd = new String[][]{
				{"p_station","varchar(1024)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{"p_team","varchar(1024)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{"p_mandate","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"p_transporttype","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"p_leader","varchar(164)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"p_pilot","varchar(164)"}, //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().executeUpdate(sql);
		}
		
		updateLabel(c, queryDataTable, "r_p_station_uuid", "p_station");  //$NON-NLS-1$//$NON-NLS-2$
		updateLabel(c, queryDataTable, "r_p_team_uuid", "p_team");  //$NON-NLS-1$//$NON-NLS-2$
		updateLabel(c, queryDataTable, "r_p_mandate_uuid", "p_mandate");  //$NON-NLS-1$//$NON-NLS-2$
		updateLabel(c, queryDataTable, "r_pl_transport_uuid", "p_transporttype");  //$NON-NLS-1$//$NON-NLS-2$
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT r_plm_leader FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" UNION SELECT DISTINCT r_plm_pilot FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		logger.finest(sql.toString());
		
		
		String updateSql = "UPDATE "+queryDataTable+" SET "; //$NON-NLS-1$ //$NON-NLS-2$
		
		String q1 = updateSql + "p_leader = ? where r_plm_leader = ?"; //$NON-NLS-1$
		String q2 = updateSql + "p_pilot = ? where r_plm_pilot = ?"; //$NON-NLS-1$
		logger.finest(q1);
		logger.finest(q2);
		PreparedStatement leaderSt = c.prepareStatement(q1);
		PreparedStatement pilotSt = c.prepareStatement(q2);
		int cnt = 0;
		try (ResultSet rs = c.createStatement().executeQuery(sql.toString())){
			while (rs.next()) {
				UUID uuid = (UUID) rs.getObject(1);
				if (uuid == null) continue;
				String name = getEmployeeName(uuid, session);
				
				if (name != null) {
					leaderSt.setString(1, name);
					leaderSt.setObject(2, uuid);
					leaderSt.addBatch();

					pilotSt.setString(1, name);
					pilotSt.setObject(2, uuid);
					pilotSt.addBatch();
					
					cnt++;
					if (cnt >= 100){
						pilotSt.executeBatch();
						leaderSt.executeBatch();
						cnt = 0;
					}
				}
			}
			pilotSt.executeBatch();
			leaderSt.executeBatch();
		}	
	}

	
	/**
	 * Build select clause 
	 * 
	 * @return select clause
	 */
	private String buildSelectClause() {
		String[] ca = {"id", "name"}; //$NON-NLS-1$ //$NON-NLS-2$
		
		String[] results = {"p_ca_uuid", "p_uuid", "p_id", "p_start_date", "p_end_date", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				"p_station_uuid", "p_team_uuid",  //$NON-NLS-1$ //$NON-NLS-2$
				"p_objective", "p_mandate_uuid", "p_type", "p_is_armed", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"pl_transport_uuid", "pl_id", //$NON-NLS-1$ //$NON-NLS-2$
				"pl_start_date", //$NON-NLS-1$
				"pl_end_date", //$NON-NLS-1$
				//"pld_patrol_day", 
				"plm_leader",  //$NON-NLS-1$
				"plm_pilot", "track", "pl_uuid", "pld_uuid" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ca.length; i++) {
			if (i != 0) {
				sb.append(","); //$NON-NLS-1$
			}
			sb.append(tablePrefix.get(ConservationArea.class) + "." + ca[i] + " as ca_" + ca[i]); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		for (int i = 0; i < results.length; i++) {
			sb.append(","); //$NON-NLS-1$
			sb.append("r." + results[i] + " as r_" + results[i]); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}


	/**
	 * Builds the from clause
	 */
	private String buildFromClause() {
		StringBuilder sql = new StringBuilder();
		sql.append(filterTable);
		sql.append(" r"); //$NON-NLS-1$

		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(tableNames.get(ConservationArea.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(ConservationArea.class));
		sql.append(" on " + tablePrefix.get(ConservationArea.class) //$NON-NLS-1$
				+ ".uuid = r.p_ca_uuid "); //$NON-NLS-1$

		return sql.toString();
	}
	
	/**
	 * Creates the temporary table that holds the query results.
	 * 
	 * @param c database connection
	 * @throws SQLException
	 */
	@Override
	public String getTemporaryTableCreateClause(String tableName) {

		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid uuid,"); //$NON-NLS-1$
		sql.append("p_uuid uuid,"); //$NON-NLS-1$
		sql.append("p_id varchar(32),"); //$NON-NLS-1$
		sql.append("p_station_uuid uuid,"); //$NON-NLS-1$
		sql.append("p_team_uuid uuid,"); //$NON-NLS-1$
		sql.append("p_objective varchar(8192),"); //$NON-NLS-1$
		sql.append("p_mandate_uuid  uuid,"); //$NON-NLS-1$
		sql.append("p_type varchar(6),"); //$NON-NLS-1$
		sql.append("p_is_armed boolean,"); //$NON-NLS-1$
		sql.append("p_start_date date,"); //$NON-NLS-1$
		sql.append("p_end_date date,"); //$NON-NLS-1$
		sql.append("pl_uuid uuid,"); //$NON-NLS-1$
		sql.append("pl_id varchar(50),"); //$NON-NLS-1$
		sql.append("pl_transport_uuid uuid,"); //$NON-NLS-1$
		sql.append("pl_start_date date,"); //$NON-NLS-1$
		sql.append("pl_end_date date,"); //$NON-NLS-1$
		sql.append("pld_uuid uuid,"); //$NON-NLS-1$
		sql.append("pld_patrol_day date,"); //$NON-NLS-1$
		sql.append("wp_uuid uuid,"); //$NON-NLS-1$
		sql.append("ob_uuid uuid,"); //$NON-NLS-1$
		sql.append("plm_leader uuid,"); //$NON-NLS-1$
		sql.append("plm_pilot uuid,"); //$NON-NLS-1$
		sql.append("track bytea)"); //$NON-NLS-1$
		
		return sql.toString();
	}
	
	
	/**
	 * Creates the temporary table that holds the query results.
	 * 
	 * @param <code>true</code> if observation (waypoint uuid, waypoint observation uuid) need to
	 * be populated; otherwise
	 * these fields will be left blank.
	 */
	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
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
			sql.append("cast(null as uuid),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as uuid),");	//wpob_uuid //$NON-NLS-1$
		}
		sql.append(tablePrefix(PatrolLegMember.class) + "_leader.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegMember.class) + "_pilot.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Track.class) + ".geometry"); //$NON-NLS-1$
		return sql.toString();
	}
	
	public String appendFromClause(HashSet<Class<?>> tables){
		if (!tables.contains(Track.class)){
			StringBuilder sb = new StringBuilder();
			sb.append(" left join "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Track.class));
			sb.append(" on "); //$NON-NLS-1$
			sb.append(tablePrefix(Track.class));
			sb.append(".patrol_leg_day_uuid = "); //$NON-NLS-1$
			sb.append(tablePrefix(PatrolLegDay.class));
			sb.append(".uuid"); //$NON-NLS-1$
			tables.add(Track.class);
			return sb.toString();
		}
		return ""; //$NON-NLS-1$
	}
	
	public void  buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
		super.buildTemporaryTableIndexes(c, tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_wp_uuid_idx on " +  tableName + "(wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		
		c.createStatement().execute(sql.toString());
	}

	@Override
	public void cleanUp(Session session) {
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				dropTable(c, queryDataTable);
				c.commit();
			}});
		
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}

	protected IFilterProcessor getFilterProcessor(FilterType filterType,
			String queryDataTable) {
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new PatrolFilterProcessor(queryDataTable, this);
		}else{
			return new PatrolWaypointFilterProcessor(queryDataTable, this);
		}
	}
}
