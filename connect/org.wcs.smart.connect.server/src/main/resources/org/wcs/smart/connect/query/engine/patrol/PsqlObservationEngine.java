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
package org.wcs.smart.connect.query.engine.patrol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.util.SharedUtils;


/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link PostgresqlPagedObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PsqlObservationEngine extends AbstractQueryEngine {
	
	private final Logger logger = Logger.getLogger(FilterProcessor.class.getName());
	
	private String queryDataTable;
	
	private SimpleQuery query;
	
	private Locale l;
	
	public PsqlObservationEngine(Locale l){
		this.l = l;
	}

	public SimpleQuery getQuery(){
		return this.query;
	}
	public String getQueryDataTable(){
		return this.queryDataTable;
	}
	
	@Override
	public IQueryResult executeQuery(Query lquery, HashMap<String, Object> params) throws SQLException {
		this.query = (SimpleQuery) lquery;
		queryDataTable = createTempTableName();
		
		Session session = (Session) params.get(Session.class.getName());

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				IFilterProcessor filterer = null;
				try {
					filterer = PsqlObservationEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable);
					
					ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), 
							Collections.singleton(query.getConservationArea()));
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, true, true);
					
					populateTemporaryTableExtra(c, session);
					
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
					dropTemporaryTables(c, false);
				}
				c.commit();
			}

		});
		return null;
	}

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	private void dropTemporaryTables(Connection c, boolean fullDrop) throws SQLException {
		if (!fullDrop)
			return;
		//original table
		dropTable(c, queryDataTable);
		dropTable(c, queryDataTable + "_LIST"); //$NON-NLS-1$
		dropTable(c, queryDataTable + "_TREE"); //$NON-NLS-1$
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		String sql = "SELECT DISTINCT p_ca_uuid, "+uuidColumn+" FROM "+queryDataTable;  //$NON-NLS-1$//$NON-NLS-2$
		logger.info(sql);
		
		try(ResultSet rs = c.createStatement().executeQuery(sql)) {
			PreparedStatement statement = c.prepareStatement("UPDATE "+ queryDataTable +" SET "+nameColumn+" = ? where "+uuidColumn+" = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int count = 0;
			while (rs.next()) {
				UUID ca_uuid = (UUID)rs.getObject(1);
				UUID uuid = (UUID)rs.getObject(2);
				if (uuid == null || ca_uuid == null)
					continue;
				
				String name = getName(uuid, ca_uuid, session);
//				List<?> options = session.createCriteria(Label.class)
//						.add(Restrictions.eq("id.element.uuid",element)).list(); //$NON-NLS-1$
//				String name = UuidUtils.uuidToString(uuid);
				statement.setString(1, name);
				statement.setObject(2, uuid);
				statement.addBatch();
				count ++;
				if (count > 100){
					statement.executeBatch();
					count = 0;
				}				
			}
			statement.executeBatch();
			
		}
	}

	
	private void populateTemporaryTableCategory(Connection c, Session session) throws SQLException {
		
		// add data model category columns
		int categoryCount = QueryManager.INSTANCE.getCategoryDepth(session, query.getConservationArea().getUuid());
		if (categoryCount < 0){
			return;			//nothing to update
		}
		
		for (int i = 0; i <= categoryCount; i++) {
			String sql = "ALTER TABLE "+queryDataTable+" ADD category_"+i+" varchar(1024)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}
		
		Map<Integer, PreparedStatement> num2Statement = new HashMap<Integer, PreparedStatement>();
		String sql = "SELECT DISTINCT OB_CATEGORY_UUID FROM "+queryDataTable;  //$NON-NLS-1$
		logger.finest(sql);
		
		try(ResultSet rs = c.createStatement().executeQuery(sql)) {
			while (rs.next()) {
				UUID uuid = (UUID) rs.getObject(1);
				if (uuid == null)
					continue;
				String[] names = getCategoryLabels(uuid, l, session);
				int count = names.length;
				int depth = Math.min(categoryCount + 1, count);	//the full category name may be longer than the number of columns in cross-ca analysis 
				PreparedStatement statement = num2Statement.get(count); //try to reuse already created prepare statement
				if (statement == null) {
					//that means that we didn't create update statement for this number of columns to update -> create one
					StringBuilder colunms = new StringBuilder();
					for (int j = 0; j < depth; j++) {
						if (j > 0){
							colunms.append(", "); //$NON-NLS-1$
						}
						colunms.append("category_").append(j).append("=?"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sql = "UPDATE "+queryDataTable+" SET "+colunms.toString()+" where OB_CATEGORY_UUID = ?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					logger.finest(sql);
					statement = c.prepareStatement(sql);
					
					num2Statement.put(count, statement);
				}
				
				for (int i = 0; i <  depth; i++) {
					statement.setString(i+1, names[i]);
				}
				statement.setObject( depth+1, uuid);
				statement.executeUpdate();
			}
		}
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
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ob_observer", "varchar(512)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().executeUpdate(sql);
		}
		
		
		populateTemporaryTableNameObjExtra("p_station_uuid", "p_station", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		
		populateTemporaryTableNameObjExtra("p_team_uuid", "p_team", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		populateTemporaryTableNameObjExtra("p_mandate_uuid", "p_mandate", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		populateTemporaryTableNameObjExtra("pl_transport_uuid", "p_transporttype", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT plm_leader FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" UNION SELECT DISTINCT plm_pilot FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" UNION SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		logger.finest(sql.toString());
		
		
		String updateSql = "UPDATE "+queryDataTable+" SET "; //$NON-NLS-1$ //$NON-NLS-2$
		
		String q1 = updateSql + "p_leader = ? where plm_leader = ?"; //$NON-NLS-1$
		String q2 = updateSql + "p_pilot = ? where plm_pilot = ?"; //$NON-NLS-1$
		String q3 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		logger.finest(q1);
		logger.finest(q2);
		logger.finest(q3);
		PreparedStatement leaderSt = c.prepareStatement(q1);
		PreparedStatement pilotSt = c.prepareStatement(q2);
		PreparedStatement observerSt = c.prepareStatement(q3);
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
					
					observerSt.setString(1, name);
					observerSt.setObject(2, uuid);
					observerSt.addBatch();
					
					cnt++;
					if (cnt >= 100){
						pilotSt.executeBatch();
						leaderSt.executeBatch();
						observerSt.executeBatch();
						cnt = 0;
					}
				}
			}
			pilotSt.executeBatch();
			leaderSt.executeBatch();
			observerSt.executeBatch();
		}
		
		//ca information
//		if (SmartDB.isMultipleAnalysis()){
		//TODO:
		if (false){
			//ca id and names are only used for cross-ca analysis
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(PsqlObservationEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(PsqlObservationEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
				
		//populating categories
		populateTemporaryTableCategory(c, session);

		WpoaLinkedData listData = new WpoaLinkedData("_list", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return Label.getDescription(uuid, session);
				//return QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, listData);

		WpoaLinkedData treeData = new WpoaLinkedData("_tree", "tree_node_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return Label.getDescription(uuid, session);
//				return QueryDataModelManager.getInstance().getAttributeTreeNodeLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, treeData);
		
	}

	
	
	private void populateAdditionalWpoaTable(Connection c, Session session, WpoaLinkedData linkedData) throws SQLException {
		String sql = "CREATE TABLE " + queryDataTable + linkedData.getPostfix() + " (uuid uuid, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql);

		String sql2 = "SELECT DISTINCT wpoa."+linkedData.getUuidColumn()+", r.P_CA_UUID FROM smart.wp_observation_attributes wpoa inner join "+queryDataTable+" r on wpoa.OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		
		sql = "INSERT INTO "+queryDataTable+linkedData.getPostfix()+" VALUES (?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		PreparedStatement statement = c.prepareStatement(sql);
		int count = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql2)){
			while (rs.next()) {
				UUID uuid = (UUID) rs.getObject(1);
				if (uuid != null) {
					UUID cauuid = (UUID)rs.getObject(2);
					String value = linkedData.getLabel(session, cauuid, uuid);
					statement.setObject(1, uuid);
					statement.setString(2, value);
					statement.addBatch();
					count++;
					if (count >= 100){
						statement.executeBatch();
						count = 0;
					}
				}
			}
			statement.executeBatch();
		}
	}

	
	/**
	 * Wrapper class for populating linked data (additional columns)
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private abstract class WpoaLinkedData {
		private String postfix;
		private String uuidColumn;

		public WpoaLinkedData(String postfix, String uuidColumn) {
			super();
			this.postfix = postfix;
			this.uuidColumn = uuidColumn;
		}

		public String getPostfix() {
			return postfix;
		}

		public String getUuidColumn() {
			return uuidColumn;
		}
		
		public abstract String getLabel(Session session, UUID cauuid, UUID keyuuid);
	}

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
		sql.append(tablePrefix(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$

		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid, "); //$NON-NLS-1$

		sql.append(tablePrefix(PatrolLegMember.class) + "_leader.employee_uuid as leader_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegMember.class) + "_pilot.employee_uuid as pilot_uuid "); //$NON-NLS-1$
		return sql.toString();
	}

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
		sql.append("p_armed boolean,"); //$NON-NLS-1$
		sql.append("p_startdate date,"); //$NON-NLS-1$
		sql.append("p_enddate date,"); //$NON-NLS-1$
		sql.append("pl_uuid uuid,"); //$NON-NLS-1$
		sql.append("p_legid varchar(50),"); //$NON-NLS-1$
		sql.append("pl_transport_uuid uuid,"); //$NON-NLS-1$
		sql.append("pld_uuid uuid,"); //$NON-NLS-1$
		sql.append("wp_date date,"); //$NON-NLS-1$ //sql.append("pld_patrol_day date,");
		sql.append("wp_uuid uuid,"); //$NON-NLS-1$

		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double precision,"); //$NON-NLS-1$
		sql.append("wp_y double precision,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("ob_observer_uuid uuid,"); //$NON-NLS-1$
		sql.append("ob_uuid uuid,"); //$NON-NLS-1$
		sql.append("ob_category_uuid uuid,"); //$NON-NLS-1$
		
		sql.append("plm_leader uuid,"); //$NON-NLS-1$
		sql.append("plm_pilot uuid"); //$NON-NLS-1$

		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		Object v = getValue(rs, column.getKey(), c);
		if (v == null) return "";
		if (v instanceof String){
			return (String)v;
		}
		if(v instanceof Time){
			return DateFormat.getTimeInstance(DateFormat.DEFAULT, l).format((Time)v);
		}else if (v instanceof Date){
			return DateFormat.getDateInstance(DateFormat.DEFAULT, l).format((Date)v);
		}else if (v instanceof Double){
			Double d = (Double)v;
			if (column.getType() == ColumnType.BOOLEAN){
				if (d < 0.5) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE,l);
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
			}
			return Double.toString((Double)v);
		}
		return v.toString();
	}
	
	public Object getValue(ResultSet rs, String columnKey, Connection c) throws SQLException{
		
		if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_ID.getKey())){
			return rs.getString("p_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TYPE.getKey())){
			return org.wcs.smart.patrol.model.PatrolType.Type.valueOf(rs.getString("p_type")).getGuiName(l);
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_START_DATE.getKey())){
			return rs.getDate("p_startdate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_END_DATE.getKey())){
			return rs.getDate("p_enddate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_STATION.getKey())){
			return rs.getString("p_station");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TEAM.getKey())){
			return rs.getString("p_team");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_OBJETIVE.getKey())){
			return rs.getString("p_objective");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_MANDATE.getKey())){
			return rs.getString("p_mandate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_ARMED.getKey())){
			return rs.getBoolean("p_armed");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_ID.getKey())){
			return rs.getString("p_legid");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_LEADER.getKey())){
			return rs.getString("p_leader");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT.getKey())){
			return rs.getString("p_pilot");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE.getKey())){
			return rs.getString("p_transporttype");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
			return rs.getString("wp_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
			return rs.getDate("wp_date");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
			return rs.getTime("wp_time");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
			return rs.getDouble("wp_x");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
			return rs.getDouble("wp_y");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION.getKey())){
			return rs.getDouble("wp_direction");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
			return rs.getDouble("wp_distance");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
			return rs.getString("wp_comment");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return rs.getString("ob_observer");
		}else if (columnKey.startsWith("category:")){
			String level = columnKey.split(":")[1];
			return rs.getString("category_"+level);
		}else if (columnKey.startsWith("attribute:")){
			UUID obuuid = (UUID) rs.getObject("ob_uuid");
			if (obuuid == null) return null;
			if (!obuuid.equals(obUuid)){
				attributeToValue = new HashMap<String, Object>();
				obUuid = obuuid;
				attachObservations(obuuid, c);
			}
			String key = columnKey.split(":")[1];
			return attributeToValue.get(key);
		}
			
			
		return null;
	}
	
	
	private HashMap<String, Object> attributeToValue;
	private UUID obUuid;
	
	private void attachObservations(UUID obUuid, Connection c) throws SQLException {
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.p_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(queryDataTable);
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(queryDataTable).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(queryDataTable).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid = ? "); //$NON-NLS-1$
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		ps.setObject(1, obUuid);
		ResultSet rs = ps.executeQuery();
		while(rs.next()){
			String key = rs.getString(2);
			
			//double
			if (rs.getObject(3) != null){
				attributeToValue.put(key,  rs.getDouble(3));
				continue;
			}
			//string
			String v = rs.getString(4);
			if (v != null){
				attributeToValue.put(key, v);
				continue;
			}
			//list
			v = rs.getString(5);
			if (v != null){
				attributeToValue.put(key, v);
				continue;
			}
			//tree
			v = rs.getString(6);
			if (v != null){
				attributeToValue.put(key,  v);
				continue;
			}
		}

	}
	
	
	
	
	
//	@Override
//	protected PatrolQueryResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
//		PatrolQueryResultItem it = new PatrolQueryResultItem();
//		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
//		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
//		it.setPatrolUuid( (UUID)rs.getObject("p_uuid")); //$NON-NLS-1$
//		it.setPatrolId(rs.getString("p_id")); //$NON-NLS-1$
//		it.setPatrolStartDate(rs.getDate("p_startdate")); //$NON-NLS-1$
//		it.setPatrolEndDate(rs.getDate("p_enddate")); //$NON-NLS-1$
//		it.setStation(rs.getString("p_station"));				 //$NON-NLS-1$
//		it.setTeam(rs.getString("p_team"));	 //$NON-NLS-1$
//		it.setObjective(rs.getString("p_objective")); //$NON-NLS-1$
//		it.setMandate(rs.getString("p_mandate")); //$NON-NLS-1$
//		it.setPatrolType( org.wcs.smart.patrol.model.PatrolType.Type.valueOf(rs.getString("p_type"))); //$NON-NLS-1$
//		it.setArmed(rs.getBoolean("p_armed")); //$NON-NLS-1$
//		it.setTransportType(rs.getString("p_transporttype")); //$NON-NLS-1$
//		it.setPatrolLegId(rs.getString("p_legid")); //$NON-NLS-1$
//		it.setWpDateTime(rs.getDate("wp_date")); //$NON-NLS-1$
//		
//		it.setLeader(rs.getString("p_leader")); //$NON-NLS-1$
//		it.setPilot(rs.getString("p_pilot")); //$NON-NLS-1$
//		it.setWaypointUuid( (UUID)rs.getObject("wp_uuid")); //$NON-NLS-1$
//		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
//		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
//		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
//		it.setWaypointTime(rs.getTime("wp_time")); //$NON-NLS-1$
//		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
//		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
//		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
//		it.setWaypointObserver(rs.getString("ob_observer")); //$NON-NLS-1$
//		it.setObservationUuid((UUID)rs.getObject("ob_uuid")); //$NON-NLS-1$
//		
//		//build categories
//		List<String> categories = new ArrayList<String>();
//		for (int i = 0; i < categoryCount; i ++){
//			String category = rs.getString("category_"+i); //$NON-NLS-1$
//			if (category == null){
//				break;
//			}
//			categories.add(category);
//		}
//		
//		it.setCategory(categories.toArray(new String[categories.size()]));
//		return it;
//	}

	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		super.buildTemporaryTableIndexes(c, tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("create index "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("_ob_category_uuid_idx on "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("(ob_category_uuid)"); //$NON-NLS-1$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
	}

	@Override
	public boolean canExecute(String queryType) {
		return queryType.equals(PatrolObservationQuery.KEY);
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}

}
