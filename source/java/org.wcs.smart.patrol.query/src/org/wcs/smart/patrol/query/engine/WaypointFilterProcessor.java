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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.query.engine.visitors.AreaFilterVisitor;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class WaypointFilterProcessor extends org.wcs.smart.observation.query.engine.WaypointFilterProcessor {

	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public WaypointFilterProcessor(String tableName, AbstractQueryEngine engine, Query query){
		super(tableName, engine, query);	
	}
	
		
	@Override
	protected DerbyFilterToSqlGenerator getSqlGenerator() {
		return PatrolFilterSqlGenerator.INSTANCE;
	}
	
	
	/**
	 * Populates the query temporary table.
	 * 
	 * @param queryFilter the query filter
	 * @param dateFilter the date filter
	 * @param caFilter the conservation area filter
	 * @param onlyObservations if only observation patrol records with observations
	 * are to be returned,  false will return all patrol records
	 * even if they don't have an observation
	 * @param c database connection
	 * @param populateObservation if the processing requires the observation
	 * information attached to the results (otherwise ob_uuid will be populated
	 * with null)
	 * 
	 * @param c the database connection
	 * 
	 * @throws SQLException
	 */
	@Override
	protected void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean populateObservation)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		
		engine.clearParameters();
		
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		HashSet<Class<?>> usedTables = new HashSet<Class<?>>();
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(namePrefix(Patrol.class));
		usedTables.add(Patrol.class);
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(namePrefix(PatrolLeg.class));
		usedTables.add(PatrolLeg.class);
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(prefix(PatrolLeg.class));
		sql.append(".patrol_uuid "); //$NON-NLS-1$
		
		if (caFilter != null) {
			String filter = ((DerbyFilterToSqlGenerator)getSqlGenerator()).asSql(caFilter, engine.tablePrefix(Patrol.class), engine);
			if (filter.length() > 0) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(namePrefix(PatrolLegDay.class));
		usedTables.add(PatrolLegDay.class);
		sql.append(" on ");  //$NON-NLS-1$
		sql.append(prefix(PatrolLeg.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegDay.class));
		sql.append(".patrol_leg_uuid "); //$NON-NLS-1$

		if (dateFilter != null) {
			String filter = getSqlGenerator().toSql(dateFilter, engine);
			if (filter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		
		sql.append(" left join "); //$NON-NLS-1$
		sql.append(name(PatrolLegMember.class));
		usedTables.add(PatrolLegMember.class);
		sql.append(" "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegMember.class) + "_leader "); //$NON-NLS-1$
		sql.append(" on " + prefix(PatrolLeg.class) + ".uuid = "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(prefix(PatrolLegMember.class) + "_leader.patrol_leg_uuid and  "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegMember.class) + "_leader.is_leader "); //$NON-NLS-1$
		sql.append(" left join "); //$NON-NLS-1$
		sql.append(name(PatrolLegMember.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegMember.class) + "_pilot "); //$NON-NLS-1$
		sql.append(" on " + prefix(PatrolLeg.class) + ".uuid = "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(prefix(PatrolLegMember.class) + "_pilot.patrol_leg_uuid and  "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegMember.class) + "_pilot.is_pilot "); //$NON-NLS-1$
		
		if (onlyObservations){
			sql.append(" inner join "); //$NON-NLS-1$
		}else{
			sql.append(" left join "); //$NON-NLS-1$
		}
			
		sql.append(namePrefix(PatrolWaypoint.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(PatrolWaypoint.class) + ".leg_day_uuid = "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegDay.class) + ".uuid "); //$NON-NLS-1$
		if (onlyObservations){
			sql.append(" inner join "); //$NON-NLS-1$
		}else{
			sql.append(" left join "); //$NON-NLS-1$
		}
		sql.append(namePrefix(Waypoint.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(prefix(PatrolWaypoint.class) + ".wp_uuid"); //$NON-NLS-1$


		sql.append(" left join "); //$NON-NLS-1$
		sql.append(waypointTable + " as waypointTable "); //$NON-NLS-1$
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
		sql.append("waypointTable.wp_uuid "); //$NON-NLS-1$
		
		if (populateObservation){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservationGroup.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$
		}
			
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			FilterTable t = cols.getValue();
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(t.tablename);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(t.tablename +"." + t.columnname + " = "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$
		}
			
		AreaFilterVisitor av = new AreaFilterVisitor(sql, engine, usedTables, query.getConservationArea());
		queryFilter.accept(av);

		sql.append(engine.appendFromClause(usedTables));
		
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = getSqlGenerator().toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
			    sql.append(filter);
			}
		}
		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	
	@Override
	protected void createWaypointTable(Connection c,  
			DateFilter dateFilter, ConservationAreaFilter caFilter, IProgressMonitor monitor)
			throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.subTask(Messages.WaypointFilterProcessor_progress1);
		//HashMap<IFilter, String> filter2Column = new HashMap<IFilter, String>();
		
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + waypointTable + " (wp_uuid char(16) for bit data)"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + waypointTable + "_wpuuid_idx on " + waypointTable + " (wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		engine.clearParameters();
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(waypointTable);
		sql.append("(wp_uuid) SELECT "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class));
		sql.append(".uuid "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$

		sql.append(name(Patrol.class));
		sql.append(" as "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class)); 
		sql.append(" join "); //$NON-NLS-1$
		sql.append(name(PatrolLeg.class));
		sql.append( " as " ); //$NON-NLS-1$
		sql.append( prefix(PatrolLeg.class)); 
		sql.append(" ON " + prefix(Patrol.class) + ".uuid = " + prefix(PatrolLeg.class) + ".patrol_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (caFilter != null) {
			String cfilter = ((DerbyFilterToSqlGenerator)getSqlGenerator()).asSql(caFilter, engine.tablePrefix(Patrol.class), engine);
			if (cfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(cfilter);
			}
		}
		sql.append(" join "); //$NON-NLS-1$

		sql.append(name(PatrolLegDay.class));
		sql.append(" as ");//$NON-NLS-1$
		sql.append(prefix(PatrolLegDay.class)); 
		sql.append(" ON " + prefix(PatrolLegDay.class) + ".patrol_leg_uuid = " + prefix(PatrolLeg.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (dateFilter != null) {
			String dfilter = getSqlGenerator().toSql(dateFilter, engine);
			if (dfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(dfilter);
			}
		}
		sql.append(" join "); //$NON-NLS-1$
		
		sql.append(name(PatrolWaypoint.class));
		sql.append(" as ");//$NON-NLS-1$
		sql.append(prefix(PatrolWaypoint.class)); 
		sql.append(" on " + prefix(PatrolLegDay.class) + ".uuid = " + prefix(PatrolWaypoint.class) + ".leg_day_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join "); //$NON-NLS-1$
		
		sql.append(name(Waypoint.class));
		sql.append(" as ");//$NON-NLS-1$
		sql.append(prefix(Waypoint.class)); 
		sql.append(" on " + prefix(PatrolWaypoint.class) + ".wp_uuid = " + prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		

		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
}
