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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.filter.IFilter;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class DerbyPatrolQueryEngine extends AbstractQueryEngine implements IPatrolQueryEngine{
	
	protected HashMap<IFilter, String> filterTables = new HashMap<IFilter, String>();
	
	static {
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(PatrolLeg.class, "pl"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegDay.class, "pld"); //$NON-NLS-1$
		tablePrefix.put(PatrolWaypoint.class, "pwp"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegMember.class, "plm"); //$NON-NLS-1$
		tablePrefix.put(Track.class, "t"); //$NON-NLS-1$
		tablePrefix.put(Team.class, "smart.team"); //$NON-NLS-1$
		tablePrefix.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tablePrefix.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(PatrolLeg.class, "smart.patrol_leg"); //$NON-NLS-1$
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day"); //$NON-NLS-1$
		tableNames.put(PatrolWaypoint.class, "smart.patrol_waypoint"); //$NON-NLS-1$
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members"); //$NON-NLS-1$
		tableNames.put(Track.class, "smart.track"); //$NON-NLS-1$
		tableNames.put(Team.class, "smart.team"); //$NON-NLS-1$
		tableNames.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tableNames.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
	}

	/**
	 * Create the select statement to populate the temporary table
	 * containing observation data for the query engine.
	 * 
	 * @param includeObservations if observation information should be included
	 * in the output table (ob_uuid).
	 * 
	 * @return
	 */
	protected abstract String getTemporaryTableSelectClause(boolean includeObservations);
	
	/**
	 * Converts the a row in the temporary table select clause to
	 * a result item
	 * @param rs result set item to convert to the queryresultitem
	 * @param session current database connection
	 * @return
	 * @throws SQLException
	 */
	protected abstract PatrolQueryResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException;
	
	/**
	 * Create the temporary table for hold observation data
	 * for querying
	 * 
	 * @param tableName temporary table name
	 * @return 
	 */
	protected abstract String getTemporaryTableCreateClause(String tableName);
	
	/**
	 * A string to append to the from clause of the select
	 * statement to create the temporary table.
	 * <p>Depending on the select clause additional tables may
	 * be required.  See {@link DerbyPatrolQueryEngine#getTemporaryTableCreateClause(String)}. </p> 
	 * @param tables List of tables already included in the from clause
	 * @return
	 */
	protected String appendFromClause(HashSet<Class<?>> tables){
		return ""; //$NON-NLS-1$
	}
	
	
	/**
	 * By default creates an index on the ob_uuid field.  This method can be overwritten to 
	 * create additional indexes.
	 * 
	 * @param c database connection
	 * @param tableName temporary table to create indexes on
	 * @throws SQLException
	 */
	protected void buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_ob_uuid_idx on " +  tableName + "(ob_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	protected IFilterProcessor getFilterProcessor(IFilter.FilterType filterType, String queryDataTable){
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this);
		}else{
			return new WaypointFilterProcessor(queryDataTable, this);
		}
	}
	
	/**
	 * Drop all tables created to support query result set.
	 * @param c
	 * @throws SQLException
	 */
	public abstract void dropTables(Connection c) throws SQLException;
}
