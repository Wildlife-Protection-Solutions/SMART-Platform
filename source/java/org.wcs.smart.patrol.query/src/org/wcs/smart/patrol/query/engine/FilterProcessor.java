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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.query.engine.visitors.AreaFilterVisitor;
import org.wcs.smart.query.QueryPlugIn;
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
public class FilterProcessor extends org.wcs.smart.observation.query.engine.FilterProcessor{

	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public FilterProcessor(String tableName, AbstractPatrolQueryEngine engine, Query query){
		super(tableName, engine, query);
	}

	@Override
	protected DerbyFilterToSqlGenerator getSqlGenerator() {
		return PatrolFilterSqlGenerator.INSTANCE;
	}

	@Override
	protected void processDatFilter(DateFilter dateFilter, StringBuilder fromSql) throws SQLException {
		if (dateFilter == null) return;
		
		fromSql.append(" JOIN "); //$NON-NLS-1$
		fromSql.append(namePrefix(PatrolWaypoint.class));
		fromSql.append(" on "); //$NON-NLS-1$
		fromSql.append(prefix(PatrolWaypoint.class));
		fromSql.append(".wp_uuid = "); //$NON-NLS-1$
		fromSql.append(prefix(Waypoint.class));
		fromSql.append(".uuid "); //$NON-NLS-1$
		
		fromSql.append(" JOIN "); //$NON-NLS-1$
		fromSql.append(namePrefix(PatrolLegDay.class));
		fromSql.append(" on "); //$NON-NLS-1$
		fromSql.append(prefix(PatrolWaypoint.class));
		fromSql.append(".leg_day_uuid = "); //$NON-NLS-1$
		fromSql.append(prefix(PatrolLegDay.class));
		fromSql.append(".uuid "); //$NON-NLS-1$
		
		fromSql.append(" JOIN "); //$NON-NLS-1$
		fromSql.append(namePrefix(PatrolLeg.class));
		fromSql.append(" on "); //$NON-NLS-1$
		fromSql.append(prefix(PatrolLeg.class));
		fromSql.append(".uuid = "); //$NON-NLS-1$
		fromSql.append(prefix(PatrolLegDay.class));
		fromSql.append(".patrol_leg_uuid "); //$NON-NLS-1$
		
		fromSql.append(" JOIN "); //$NON-NLS-1$
		fromSql.append(namePrefix(Patrol.class));
		fromSql.append(" on "); //$NON-NLS-1$
		fromSql.append(prefix(Patrol.class));
		fromSql.append(".uuid = "); //$NON-NLS-1$
		fromSql.append(prefix(PatrolLeg.class));
		fromSql.append(".patrol_uuid "); //$NON-NLS-1$
		
		String filter = getSqlGenerator().toSql(dateFilter, engine);
		if (filter.length() > 0) {
			fromSql.append(" and "); //$NON-NLS-1$
			fromSql.append(filter);
		}
		
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
	 * @throws Exception
	 */
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
			String filter = ((PatrolFilterSqlGenerator)getSqlGenerator()).asSql(caFilter, engine.tablePrefix(Patrol.class), engine);
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
		
		if (dateFilter != null ) {
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
		usedTables.add(Waypoint.class);
		usedTables.add(PatrolWaypoint.class);
		sql.append(namePrefix(PatrolWaypoint.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegDay.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(prefix(PatrolWaypoint.class));
		sql.append(".leg_day_uuid "); //$NON-NLS-1$
		
		if (onlyObservations){
			sql.append(" inner join "); //$NON-NLS-1$
		}else{
			sql.append(" left join "); //$NON-NLS-1$
		}
		sql.append(namePrefix(Waypoint.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(PatrolWaypoint.class));
		sql.append(".wp_uuid = "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class));
		sql.append(".uuid "); //$NON-NLS-1$
		
		
		if (populateObservation || 
				observationFilterVisitor.hasAttributeFilter() || 
				observationFilterVisitor.hasCategoryFilter()){
		
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservationGroup.class));
			usedTables.add(WaypointObservationGroup.class);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class));
			sql.append(".wp_uuid "); //$NON-NLS-1$
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			usedTables.add(WaypointObservation.class);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class));
			sql.append(".wp_group_uuid "); //$NON-NLS-1$
		}	
		
		if (observationFilterVisitor.hasAttributeFilter() || 
				observationFilterVisitor.hasCategoryFilter()){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(name(Category.class));
			usedTables.add(Category.class);
			sql.append(" "); //$NON-NLS-1$
			sql.append(prefix(Category.class));
			
			sql.append(" on " + prefix(Category.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ prefix(WaypointObservation.class)
					+ ".category_uuid "); //$NON-NLS-1$
				if (observationFilterVisitor.hasAttributeFilter()){
					sql.append(" left join "); //$NON-NLS-1$
					sql.append(observationTable + " qa on qa.observation_uuid = "); //$NON-NLS-1$
					sql.append(prefix(WaypointObservation.class) + ".uuid"); //$NON-NLS-1$
				}
		}

		
		// area filters
		AreaFilterVisitor areaVisitor = new AreaFilterVisitor(sql, engine, usedTables, query.getConservationArea());
		queryFilter.accept(areaVisitor);
		
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
	protected Collection<IWaypointSource> getWaypointSources(){
		return Collections.singletonList( WaypointSourceEngine.INSTANCE.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID) );
	}
	
}
