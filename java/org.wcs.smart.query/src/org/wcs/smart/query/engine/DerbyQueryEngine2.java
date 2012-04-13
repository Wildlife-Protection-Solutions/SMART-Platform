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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.parser.internal.AttributeInfo;
import org.wcs.smart.query.parser.internal.Filter;

/**
 * Query engine for executing queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DerbyQueryEngine2 implements QueryEngine {

	/**
	 * Maps database tables to a prefix to use in the query.
	 */
	private static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static {
		tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(Patrol.class, "p");
		tablePrefix.put(PatrolLeg.class, "pl");
		tablePrefix.put(PatrolLegDay.class, "pld");
		tablePrefix.put(Waypoint.class, "wp");
		tablePrefix.put(WaypointObservation.class, "wpo");
		tablePrefix.put(WaypointObservationAttribute.class, "wpoa");
		tablePrefix.put(Attribute.class, "a");
		tablePrefix.put(Category.class, "c");
		tablePrefix.put(AttributeTreeNode.class, "at");
		tablePrefix.put(AttributeListItem.class, "al");
	}

	private List<QueryResultItem> myResults = null;

	private String tempSchema = "smart";
	private String queryTempTable = "query_results_";
	private String observationTempTable = "query_attributes_";

	/**
	 * Executes the given query.
	 * 
	 * @param query
	 *            the query to execute
	 * @param session
	 *            open hibernate session
	 * @param monitor
	 *            progress monitor
	 * 
	 * @return the results of the query
	 * @throws SQLException
	 */
	/*
	 * The query execute process is as follows:
	 * 
	 * 1) If the query includes attributes then create a "cross join" table
	 * of all observations and the required attributes. This table (observationTempTable)
	 * looks as follows:
	 * observation_uuid | attribute1 | attribute 2 | attribute 3 etc.
	 * 
	 * 2) A temporary table (queryTempTable) is created for holding all observations which
	 * match the required filter.  This table contains all the patrol
	 * to waypoint attributes and the observation id.  IT does 
	 * not contain any of the matched attributes.
	 * 
	 * 3) Join together the temporary results table with the observations
	 * to get all attributes associated with a matching observations.
	 */
	@Override
	public List<QueryResultItem> executeQuery(final WaypointQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {

		queryTempTable = queryTempTable + System.nanoTime();
		observationTempTable = observationTempTable + System.nanoTime();

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask("Running Query.", 4);

				try {
					monitor.subTask("Creating observation table");
					if (query.getFilter() != Filter.EMPTY_FILTER && query.getFilter().hasAttributeFilter()) {
						createObservationTable(c, query);
					}
					monitor.worked(1);

					monitor.subTask("Creating temporary table");
					createTemporaryTable(c);
					monitor.worked(1);

					monitor.subTask("Populating results table");
					populateTemporaryTable(query, c);
					monitor.worked(1);

					monitor.subTask("Loading results into editor");
					myResults = getResults(c, session);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
				}
				monitor.worked(1);
				monitor.done();

			}
		});

		return myResults;

	}


	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	private void dropTemporaryTables(Connection c) throws SQLException {
		try {
			String sql = "DROP TABLE " + tempSchema + "." + observationTempTable;
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}

		try {
			String sql = "DROP TABLE " + tempSchema + "." + queryTempTable;
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}
	}

	/**
	 * Create the temporary table that contains all attribute
	 * in the query.
	 * 
	 * @param c the database connection
	 * @param query the query 
	 * @throws SQLException
	 */
	private void createObservationTable(Connection c, WaypointQuery query)
			throws SQLException {
		HashSet<AttributeInfo> keys = new HashSet<AttributeInfo>();
		query.getFilter().getAttributeFilters(keys);

		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tempSchema + "." + observationTempTable + " (observation_uuid char(16) for bit data");
		for (AttributeInfo key : keys) {
			sql.append(", " + key.getKey() + " "
					+ this.getDataType(key.getType()));
		}
		sql.append(")");
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		sql = new StringBuilder();
		sql.append("INSERT INTO " + tempSchema + "." + observationTempTable + " SELECT observation_uuid ");
		for (AttributeInfo key : keys) {
			sql.append(", max(" + key.getKey() + ") as " + key.getKey() + " ");
		}
		sql.append("FROM (");
		sql.append("SELECT a.observation_uuid ");
		boolean list = false;
		boolean tree = false;
		for (AttributeInfo key : keys) {
			if (key.getType() == AttributeType.LIST) {
				list = true;
				sql.append(", case when c.keyid = '" + key.getKey() + "'");
				sql.append(" then l.keyid else null end as " + key.getKey() + " ");
			} else if (key.getType() == AttributeType.TREE) {
				tree = true;
				sql.append(", case when c.key = '" + key.getKey() + "'");
				sql.append(" then t.hkey else null end as ");
				sql.append(key.getKey() + " ");
				
			} else {
				sql.append(", case when c.keyid = '" + key.getKey()
						+ "' then a." + key.getColumn() + " else null end as "
						+ key.getKey() + " ");
			}
		}
		sql.append("FROM smart.wp_observation_attributes as a join smart.dm_attribute c on a.attribute_uuid = c.uuid");
		if (list) {
			sql.append(" LEFT JOIN smart.dm_attribute_list l on l.uuid = a.list_element_uuid ");
		}
		if (tree){
			sql.append(" LEFT JOIN smart.dm_attribute_tree t on t.uuid = a.tree_node_uuid ");
		}
		sql.append(") foo GROUP BY observation_uuid ");

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

	}

	/**
	 * Creates the temporary table that holds the query results.
	 * 
	 * @param c database connection
	 * @throws SQLException
	 */
	private void createTemporaryTable(Connection c) throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tempSchema + "." + queryTempTable + "(");
		sql.append("p_uuid char(16) for bit data,");
		sql.append("p_id varchar(23),");
		sql.append("p_station_uuid char(16) for bit data,");
		sql.append("p_team_uuid char(16) for bit data,");
		sql.append("p_objective_rating smallint,");
		sql.append("p_objective varchar(8192),");
		sql.append("p_mandate_uuid  char(16) for bit data,");
		sql.append("p_type varchar(6),");
		sql.append("p_is_armed boolean,");
		sql.append("p_start_date date,");
		sql.append("p_end_date date,");
		sql.append("pl_id varchar(50),");
		sql.append("pl_transport_uuid char(16) for bit data,");
		sql.append("pld_patrol_day date,");
		sql.append("wp_uuid char(16) for bit data,");
		sql.append("ob_uuid char(16) for bit data)");

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		//-- add indexes 
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryTempTable + "_wp_uuid_idx on " + tempSchema + "." + queryTempTable + "(wp_uuid)");
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryTempTable + "_ob_uuid_idx on " + tempSchema + "." + queryTempTable + "(ob_uuid)");
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	/**
	 * Populates the query temporary table.
	 * 
	 * @param q the query 
	 * @param c the database connection
	 * 
	 * @throws SQLException
	 */
	private void populateTemporaryTable(WaypointQuery q, Connection c)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + tempSchema + "." + queryTempTable );
		// ---- SELECT CLAUSE -----
		sql.append(" SELECT ");
		sql.append(tablePrefix.get(Patrol.class) + ".uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".id, ");
		sql.append(tablePrefix.get(Patrol.class) + ".station_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".team_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".objective_rating, ");
		sql.append(tablePrefix.get(Patrol.class) + ".objective, ");
		sql.append(tablePrefix.get(Patrol.class) + ".mandate_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".patrol_type, ");
		sql.append(tablePrefix.get(Patrol.class) + ".is_armed, ");
		sql.append(tablePrefix.get(Patrol.class) + ".start_date, ");
		sql.append(tablePrefix.get(Patrol.class) + ".end_date, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".id, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".transport_uuid, ");
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".patrol_day, ");
		sql.append(tablePrefix.get(Waypoint.class) + ".uuid, ");
		sql.append(tablePrefix.get(WaypointObservation.class) + ".uuid ");

		// ---- FROM CLAUSE -----
		sql.append(" FROM ");
		sql.append(" smart.patrol " + tablePrefix.get(Patrol.class));
		sql.append(" inner join ");
		sql.append(" smart.patrol_leg " + tablePrefix.get(PatrolLeg.class));
		sql.append(" on " + tablePrefix.get(Patrol.class) + ".uuid = "
				+ tablePrefix.get(PatrolLeg.class) + ".patrol_uuid ");
		sql.append(" inner join ");
		sql.append(" smart.patrol_leg_day "
				+ tablePrefix.get(PatrolLegDay.class));
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = "
				+ tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid ");
		sql.append(" inner join ");
		sql.append(" smart.waypoint " + tablePrefix.get(Waypoint.class));
		sql.append(" on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = "
				+ tablePrefix.get(Waypoint.class) + ".leg_day_uuid ");
		sql.append(" left join ");
		sql.append(" smart.wp_observation "
				+ tablePrefix.get(WaypointObservation.class));
		sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = "
				+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");

		if (q.getFilter() != Filter.EMPTY_FILTER) {
			if (q.getFilter().hasAttributeFilter()
					|| q.getFilter().hasCategoryFilter()) {
				sql.append(" left join ");
				sql.append(" smart.dm_category "
						+ tablePrefix.get(Category.class));
				sql.append(" on " + tablePrefix.get(Category.class)
						+ ".uuid = "
						+ tablePrefix.get(WaypointObservation.class)
						+ ".category_uuid ");

				if (q.getFilter().hasAttributeFilter()) {
					sql.append(" left join ");
					sql.append(tempSchema + "." + observationTempTable + " qa on qa.observation_uuid = ");
					sql.append(tablePrefix.get(WaypointObservation.class)
							+ ".uuid");

				}
			}
		}
		
		// ---- WHERE CLAUSE -----
		sql.append(" WHERE ");
		boolean and = false;
		if (q.getDateFilter() != null) {
			String filter = q.getDateFilter().asSql(tablePrefix);
			if (filter.length() > 0) {
				sql.append(filter);
				and = true;
			}
		}
		if (q.getConservationAreaFilter() != null) {
			String filter = q.getConservationAreaFilter().asSql(tablePrefix);
			if (filter.length() > 0) {
				if (and) {
					sql.append(" AND ");
				}
				sql.append("(" + filter + ")");
				and = true;
			}
		}
		if (q.getFilter() != Filter.EMPTY_FILTER) {
			String filter = q.getFilter().asSql(tablePrefix);
			if (filter.length() > 0) {
				if (and) {
					sql.append(" AND ");
				}

				sql.append(" ( " + filter + " ) ");
				and = true;
			}

		}

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
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
	private List<QueryResultItem> getResults(Connection c, Session session)
			throws SQLException {
		List<QueryResultItem> items = new ArrayList<QueryResultItem>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM (");
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

		QueryResultItem last = null;
		try {
			while (rs.next()) {
				byte[] wpouuid = rs.getBytes(22);
				if (wpouuid != null && last != null
						&& last.getObservationUuid() != null
						&& Arrays.equals(wpouuid, last.getObservationUuid())) {
					//same observation new attribute
					Attribute att = getAttribute(rs.getBytes(24), session);
					if (att != null){
						Object value = getAttributeValue(att, rs, session);
						last.addAttribute(att.getKeyId(), value);
					}
					continue;
				}

				QueryResultItem it = new QueryResultItem();
				it.setPatrolUuid(rs.getBytes(1));
				it.setPatrolId(rs.getString(2));
				it.setPatrolStartDate(rs.getDate(3));
				it.setPatrolEndDate(rs.getDate(4));
				it.setStation(getStationName(rs.getBytes(5), session));				
				it.setTeam(getTeamName(rs.getBytes(6), session));				
				it.setObjectiveRating(rs.getInt(7));
				it.setObjective(rs.getString(8));
				it.setObjective(getMandateName(rs.getBytes(9), session));
				it.setPatrolType(PatrolType.Type.valueOf(rs.getString(10)));
				it.setArmed(rs.getBoolean(11));
				it.setTransportType(getTransportType(rs.getBytes(12), session));
				it.setPatrolLegId(rs.getString(13));
				it.setWpDateTime(rs.getDate(14));
				it.setWaypointId(rs.getInt(15));
				it.setWaypointX(rs.getDouble(16));
				it.setWaypointY(rs.getDouble(17));
				it.setWaypointTime(rs.getTime(18));
				it.setWaypointDirection(rs.getFloat(19));
				it.setWaypointDistance(rs.getFloat(20));
				it.setWaypointComment(rs.getString(21));
				it.setObservationUuid(wpouuid);
				it.setCategory(getCategory(rs.getBytes(23), session));
				Attribute att = getAttribute(rs.getBytes(24), session);
				if (att != null){
					Object value = getAttributeValue(att, rs, session);
					it.addAttribute(att.getKeyId(), value);
				}

				items.add(it);
				last = it;
			}
		} finally {
			rs.close();
		}
		return items;
	}
	
	/**
	 * Gets the attribute value from the result set for the given attribute.
	 * 
	 * @param att
	 * @param rs
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	private Object getAttributeValue(Attribute att, ResultSet rs,
			Session session) throws SQLException {
		Object value = null;
		switch (att.getType()) {
		case NUMERIC:
			value = rs.getDouble(25);
			break;
		case BOOLEAN:
			value = (rs.getDouble(25) >= 0.5);
			break;
		case TEXT:
			value = rs.getString(26);
			break;
		case TREE:
			byte[] nodeuuid = rs.getBytes(28);
			if (nodeuuid != null) {
				AttributeTreeNode i = (AttributeTreeNode) session.load(
						AttributeTreeNode.class, nodeuuid);
				value = i.getName();
			}
			break;
		case LIST:
			byte[] listuuid = rs.getBytes(27);
			if (listuuid != null) {
				AttributeListItem i = (AttributeListItem) session.load(
						AttributeListItem.class, listuuid);
				value = i.getName();
			}
			break;
		}
		return value;
	}
	
	
	/**
	 * Loads the attribute object from the session
	 * 
	 * @param uuid
	 * @param session
	 * @return
	 */
	private Attribute getAttribute(byte[] uuid, Session session){
		if (uuid != null){
			Attribute att = (Attribute) session.load(Attribute.class, uuid);
			return att;
		}
		return null;
	}
	/**
	 * Loads the category object from the session
	 * 
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	private Category getCategory(byte[] uuid, Session session){
		if (uuid != null){
			Category x = (Category) session.load(Category.class, uuid);
			return x;
		}
		return null;
	}
	
	/**
	 * Loads the station object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	private String getStationName(byte[] suuid, Session session){
		if (suuid != null){
			Station x = (Station) session.load(Station.class, suuid);
			if (x != null) {
				return x.getName();
			}
		}
		return null;
	}
	
	/**
	 * Loads the team object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	private String getTeamName(byte[] tuuid, Session session){
		if (tuuid != null){
			Team x = (Team) session.load(Team.class, tuuid);
			if (x != null) {
				return x.getName();
			}
		}
		return null;
	}
	
	/**
	 * Loads the team mandate from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	private String getMandateName(byte[] muuid, Session session){
		if (muuid != null){
			PatrolMandate x = (PatrolMandate) session.load(PatrolMandate.class, muuid);
			if (x != null) {
				return x.getName();
			}
		}
		return null;
	}
	
	/**
	 * Loads the transport type from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	private String getTransportType(byte[] uuid, Session session){
		if (uuid != null){
			PatrolTransportType x = (PatrolTransportType) session.load(
					PatrolTransportType.class, uuid);
			if (x != null){
				return x.getName();
			}
		}
		return null;
	}
	
	
	/**
	 * Build select clause 
	 * 
	 * @param includeObservations if observations should be included
	 * @return select clause
	 */
	private String SelectClause(boolean includeObservations) {
		String[] results = { "p_uuid", "p_id", "p_start_date", "p_end_date",
				"p_station_uuid", "p_team_uuid", "p_objective_rating",
				"p_objective", "p_mandate_uuid", "p_type", "p_is_armed",
				"pl_transport_uuid", "pl_id", "pld_patrol_day" };
		String[] waypoints = { "id", "x", "y", "time", "direction", "distance",
				"wp_comment" };
		String[] observations = { "uuid", "category_uuid" };
		String[] attributes = { "attribute_uuid", "number_value",
				"string_value", "list_element_uuid", "tree_node_uuid" };

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < results.length; i++) {
			if (i != 0) {
				sb.append(",");
			}
			sb.append("r." + results[i] + " as r_" + results[i]);
		}

		for (int i = 0; i < waypoints.length; i++) {
			sb.append(",");
			sb.append(tablePrefix.get(Waypoint.class) + "." + waypoints[i]
					+ " as w_" + waypoints[i]);
		}

		if (includeObservations) {
			for (int i = 0; i < observations.length; i++) {
				sb.append(",");
				sb.append(tablePrefix.get(WaypointObservation.class) + "."
						+ observations[i] + " as o_" + observations[i]);
			}
			for (int i = 0; i < attributes.length; i++) {
				sb.append(",");
				sb.append(tablePrefix.get(WaypointObservationAttribute.class)
						+ "." + attributes[i] + " as a_" + attributes[i]);
			}
		} else {
			for (int i = 0; i < observations.length; i++) {
				sb.append(",");
				sb.append(" cast(null as char(16) for bit data)");
			}
			for (int i = 0; i < attributes.length; i++) {
				sb.append(",");
				if (i == 1) {
					sb.append(" cast(null as double)");
				} else if (i == 2) {
					sb.append(" cast(null as varchar(1024))");
				} else {
					sb.append(" cast(null as char(16) for bit data)");
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Builds the where clause 
	 * @param includeObservations
	 * @return
	 */
	private String WhereClause(boolean includeObservations) {
		if (includeObservations) {
			return "r.ob_uuid is not null";
		} else {
			return "r.ob_uuid is null";
		}
	}

	/**
	 * Builds the from clause
	 * @param includeObservations
	 * @return
	 */
	private String FromClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();

		sql.append(tempSchema + "." + queryTempTable);
		sql.append(" r");

		if (includeObservations) {
			sql.append(" inner join ");
			sql.append("smart.wp_observation "
					+ tablePrefix.get(WaypointObservation.class));
			sql.append(" on " + tablePrefix.get(WaypointObservation.class)
					+ ".uuid = r.ob_uuid ");

			sql.append(" inner join ");
			sql.append("smart.waypoint " + tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = "
					+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");
		} else {
			sql.append(" inner join ");
			sql.append("smart.waypoint " + tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class)
					+ ".uuid = r.wp_uuid ");
		}

		if (includeObservations) {
			sql.append(" left join ");
			sql.append(" smart.wp_observation_attributes "
					+ tablePrefix.get(WaypointObservationAttribute.class));
			sql.append(" on " + tablePrefix.get(WaypointObservation.class)
					+ ".uuid = "
					+ tablePrefix.get(WaypointObservationAttribute.class)
					+ ".observation_uuid");
		}
		return sql.toString();
	}

	/**
	 * Returns the database data type for a given 
	 * attribute type.
	 * @param type the attribute type
	 * @return the database datatype for the observation
	 * temporary table
	 */
	private String getDataType(AttributeType type) {

		switch (type) {
		case LIST:
			return "varchar(128)"; //keyid
		case TREE:
			return "varchar(32672)"; ///hkey
		case NUMERIC:
			return "double";
		case BOOLEAN:
			return "double";
		case TEXT:
			return "varchar(1024)";
		}
		return "";

	}
}
