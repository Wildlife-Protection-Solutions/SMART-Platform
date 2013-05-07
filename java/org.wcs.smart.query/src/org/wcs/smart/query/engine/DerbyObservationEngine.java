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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.filter.IFilter;

/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link DerbyPagedObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyObservationEngine extends DerbyQueryEngine2 {

	public DerbyPagedObservationResult executeDerbyQuery(final SimpleQuery query, final Session session, final IProgressMonitor monitor) throws SQLException {
		
		queryTempTable = QUERY_TEMP_TABLE_PREFIX + System.nanoTime();
		observationTempTable = QUERY_OB_TEMP_TABLE_PREFIX + System.nanoTime();
		
		final DerbyPagedObservationResult result = new DerbyPagedObservationResult(queryTempTable);
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyQueryEngine2_Progress_RunningQuery, 70);

				try {			
					monitor.subTask(Messages.DerbyQueryEngine2_Progress_CreatingObservationTable);
					IFilter qFilter = query.getFilter();
					if (qFilter == null){
						return;
					}
					if (qFilter != IFilter.EMPTY_FILTER && qFilter.hasAttributeFilter()) {
						createObservationTable(c, query.getFilter(), query.getDateFilter(), 
								query.getConservationAreaFilterAsFilter(), monitor);
					}
					monitor.worked(2);
					if (monitor.isCanceled()){
						return;
					}

					monitor.subTask(Messages.DerbyQueryEngine2_Progress_CreatingTempTable);
					createResultTemporaryTable(c);
					monitor.worked(2);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbyQueryEngine2_Progress_PopulatingResults);
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilterAsFilter(), true, c, true); //MUST be both true
					monitor.worked(15);
					if (monitor.isCanceled()){
						return;
					}
					
					populateTemporaryTableExtra(c, session, monitor);
					
					monitor.subTask(Messages.DerbyObservationEngine_Progress_FetchSize);
					//setting result size
					ResultSet rs = c.createStatement().executeQuery("select count(*) from "+queryTempTable); //$NON-NLS-1$
					try {
						if (rs.next()) { 
							result.setItemCount(rs.getInt(1));
						}
					} finally {
						rs.close();
					}

					//setting waypoint count
					rs = c.createStatement().executeQuery("select count(*) from (SELECT DISTINCT WP_UUID from "+queryTempTable+") wp"); //$NON-NLS-1$ //$NON-NLS-2$
					try {
						if (rs.next()) { 
							result.setWpCount(rs.getInt(1));
						}
					} finally {
						rs.close();
					}
					
				} finally {
					dropTemporaryTables(c, monitor.isCanceled());
					monitor.done();
				}
			}

		});
		return result;
	}

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	private void dropTemporaryTables(Connection c, boolean fullDrop) throws SQLException {
		try {
			String sql = "DROP TABLE " + observationTempTable; //$NON-NLS-1$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}
		
		if (!fullDrop)
			return;

		//original table
		try {
			String sql = "DROP TABLE " + queryTempTable; //$NON-NLS-1$
			c.createStatement().execute(sql);
			QueryPlugIn.logSql(sql);
		} catch (Exception ex) {
			// eatme
		}
		//list elements value table
		try {
			String sql = "DROP TABLE " + queryTempTable + "_LIST"; //$NON-NLS-1$ //$NON-NLS-2$
			c.createStatement().execute(sql);
			QueryPlugIn.logSql(sql);
		} catch (Exception ex) {
			// eatme
		}
		//tree elements value table
		try {
			String sql = "DROP TABLE " + queryTempTable + "_TREE"; //$NON-NLS-1$ //$NON-NLS-2$
			c.createStatement().execute(sql);
			QueryPlugIn.logSql(sql);
		} catch (Exception ex) {
			// eatme
		}

	}

	/**
	 * Creates the temporary table that holds the query results.
	 * 
	 * @param c database connection
	 * @throws SQLException
	 */
	private void createResultTemporaryTable(Connection c) throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + queryTempTable + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_id varchar(23),"); //$NON-NLS-1$
		sql.append("p_station_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_team_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_objective varchar(8192),"); //$NON-NLS-1$
		sql.append("p_mandate_uuid  char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_type varchar(6),"); //$NON-NLS-1$
		sql.append("p_armed boolean,"); //$NON-NLS-1$
		sql.append("p_startdate date,"); //$NON-NLS-1$
		sql.append("p_enddate date,"); //$NON-NLS-1$
		sql.append("pl_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_legid varchar(50),"); //$NON-NLS-1$
		sql.append("pl_transport_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pld_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_date date,"); //$NON-NLS-1$ //sql.append("pld_patrol_day date,");
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$

		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time time,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$

		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_category_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("plm_leader char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_pilot char(16) for bit data"); //$NON-NLS-1$

		sql.append(")"); //$NON-NLS-1$

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		//create index on observation uuid as this is used in other query joings
		sql = new StringBuilder();
		sql.append("create index "); //$NON-NLS-1$
		sql.append(queryTempTable);
		sql.append("_obuuid_idx on "); //$NON-NLS-1$
		sql.append(queryTempTable);
		sql.append("(ob_uuid)"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		sql = new StringBuilder();
		sql.append("create index "); //$NON-NLS-1$
		sql.append(queryTempTable);
		sql.append("_ob_category_uuid_idx on "); //$NON-NLS-1$
		sql.append(queryTempTable);
		sql.append("(ob_uuid)"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		ResultSet rs = c.createStatement().executeQuery("SELECT DISTINCT p_ca_uuid, "+uuidColumn+" FROM "+queryTempTable);  //$NON-NLS-1$//$NON-NLS-2$
		try {
			while (rs.next()) {
				byte[] ca_uuid = rs.getBytes(1);
				byte[] uuid = rs.getBytes(2);
				if (uuid == null || ca_uuid == null)
					continue;
				String name = getName(uuid, ca_uuid, session);
				PreparedStatement statement = c.prepareStatement("UPDATE "+queryTempTable+" SET "+nameColumn+" = ? where "+uuidColumn+" = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				statement.setString(1, name);
				statement.setBytes(2, uuid);
				statement.executeUpdate();
			}
		} finally {
			rs.close();
		}
	}

	private void populateTemporaryTableCategory(Connection c, Session session) throws SQLException {
		DataModel dataModel = QueryDataModelManager.getInstance().getDataModel();
		// add data model category columns
		int numCategory = -1;
		for (Category cat : dataModel.getActiveCategories()) {
			numCategory = Math.max(numCategory, getDepth(cat));
		}
		
		for (int i = 0; i <= numCategory; i++) {
			c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD category_"+i+" varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (numCategory < 0){
			//nothing to update
			return;
		}
		Map<Integer, PreparedStatement> num2Statement = new HashMap<Integer, PreparedStatement>();
		ResultSet rs = c.createStatement().executeQuery("SELECT DISTINCT OB_CATEGORY_UUID FROM "+queryTempTable);  //$NON-NLS-1$
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid == null)
					continue;
				String[] names = getCategoryLabels(uuid, session);
				int count = names.length;
				int depth = Math.min(numCategory+1, count);	//the full category name may be longer than the number of columns in cross-ca analysis 
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
					statement = c.prepareStatement("UPDATE "+queryTempTable+" SET "+colunms.toString()+" where OB_CATEGORY_UUID = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					num2Statement.put(count, statement);
				}
				
				for (int i = 0; i <  depth; i++) {
					statement.setString(i+1, names[i]);
				}
				statement.setBytes( depth+1, uuid);
				statement.executeUpdate();
			}
		} finally {
			rs.close();
		}
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session, IProgressMonitor monitor) throws SQLException {
		//NOTE: does 50 worked for monitor in total
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD p_station varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD p_team varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD p_mandate varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD p_transporttype varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
		
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD p_leader varchar(164)"); //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD p_pilot varchar(164)"); //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD ca_id varchar(8)"); //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute("ALTER TABLE "+queryTempTable+" ADD ca_name varchar(256)"); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.subTask(Messages.DerbyObservationEngine_Progress_StationData);
		populateTemporaryTableNameObjExtra("p_station_uuid", "p_station", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		monitor.worked(7);
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.subTask(Messages.DerbyObservationEngine_Progress_TeamData);
		populateTemporaryTableNameObjExtra("p_team_uuid", "p_team", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		monitor.worked(7);
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.DerbyObservationEngine_Progress_MandateData);
		populateTemporaryTableNameObjExtra("p_mandate_uuid", "p_mandate", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		monitor.worked(2);
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.DerbyObservationEngine_Progress_TransportData);
		populateTemporaryTableNameObjExtra("pl_transport_uuid", "p_transporttype", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		monitor.worked(2);
		if (monitor.isCanceled()){
			return;
		}

		
		monitor.subTask(Messages.DerbyObservationEngine_Progress_LeaderPilotData);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT plm_leader FROM "); //$NON-NLS-1$
		sql.append(queryTempTable);
		sql.append(" UNION SELECT DISTINCT plm_pilot FROM "); //$NON-NLS-1$
		sql.append(queryTempTable);

		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		String updateSql = "UPDATE "+queryTempTable+" SET "; //$NON-NLS-1$ //$NON-NLS-2$
		PreparedStatement leaderSt = c.prepareStatement(updateSql+"p_leader = ? where plm_leader = ?"); //$NON-NLS-1$
		PreparedStatement pilotSt = c.prepareStatement(updateSql+"p_pilot = ? where plm_pilot = ?"); //$NON-NLS-1$
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				String name = getEmployeeName(uuid, session);
				if (name != null) {
					leaderSt.setString(1, name);
					leaderSt.setBytes(2, uuid);
					leaderSt.executeUpdate();
					
					pilotSt.setString(1, name);
					pilotSt.setBytes(2, uuid);
					pilotSt.executeUpdate();
				}
			}
		} finally {
			rs.close();
		}
		monitor.worked(12);
		if (monitor.isCanceled()){
			return;
		}
		
		//ca information
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			monitor.subTask(Messages.DerbyObservationEngine_Progress_CaInfo);
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryTempTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryTempTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			c.createStatement().executeUpdate(sql.toString());
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryTempTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryTempTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			c.createStatement().executeUpdate(sql.toString());
		}
		
		//populating categories
		monitor.subTask(Messages.DerbyObservationEngine_Progress_CategoryData);
		populateTemporaryTableCategory(c, session);
		monitor.worked(13);
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.DerbyObservationEngine_Progress_ListAttributesData);
		WpoaLinkedData listData = new WpoaLinkedData("_list", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, byte[] cauuid, byte[] uuid) {
				return QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, listData);
		monitor.worked(3);
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.subTask(Messages.DerbyObservationEngine_Progress_TreeAttributesData);
		WpoaLinkedData treeData = new WpoaLinkedData("_tree", "tree_node_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, byte[] cauuid, byte[] uuid) {
				return QueryDataModelManager.getInstance().getAttributeTreeNodeLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, treeData);
		monitor.worked(3);
		if (monitor.isCanceled()){
			return;
		}
	}

	private void populateAdditionalWpoaTable(Connection c, Session session, WpoaLinkedData linkedData) throws SQLException {
		String sql = "CREATE TABLE " + queryTempTable + linkedData.getPostfix() + " (uuid char(16) for bit data, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(sql);

		sql = "SELECT DISTINCT wpoa."+linkedData.getUuidColumn()+", r.P_CA_UUID FROM smart.wp_observation_attributes wpoa inner join "+queryTempTable+" r on wpoa.OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ResultSet rs = c.createStatement().executeQuery(sql);
		PreparedStatement statement = c.prepareStatement("INSERT INTO "+queryTempTable+linkedData.getPostfix()+" VALUES (?, ?)"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid != null) {
					byte[] cauuid = rs.getBytes(2);
					String value = linkedData.getLabel(session, cauuid, uuid);
					statement.setBytes(1, uuid);
					statement.setString(2, value);
					statement.executeUpdate();
				}
			}
		} finally {
			rs.close();
		}
	}

	/**
	 * Compute the maximum category depth.
	 * 
	 * @param cat category
	 * @return maximum depth
	 */
	private int getDepth(Category cat) {
		int maxDepth = -1;
		for (Category child : cat.getActiveChildren()) {
			maxDepth = Math.max(maxDepth, getDepth(child));
		}
		return maxDepth + 1;
	}

	@Override
	protected String buildTemporaryTableSelectClause(boolean needsObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".station_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".team_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".patrol_type, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".is_armed, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".transport_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$

		sql.append(tablePrefix.get(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Waypoint.class) + ".time, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(WaypointObservation.class) + ".category_uuid, "); //$NON-NLS-1$

		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.employee_uuid as leader_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.employee_uuid as pilot_uuid "); //$NON-NLS-1$
		return sql.toString();
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
		
		public abstract String getLabel(Session session, byte[] cauuid, byte[] keyuuid);
	}
}
