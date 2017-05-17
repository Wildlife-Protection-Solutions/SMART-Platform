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
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.query.engine.visitors.AreaFilterVisitor;
import org.wcs.smart.query.common.engine.NamedPreparedStatement;
import org.wcs.smart.query.common.engine.visitors.AttributeFilterCollectorVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;


/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class PatrolFilterProcessor implements IFilterProcessor {
	
	private final Logger logger = Logger.getLogger(PatrolFilterProcessor.class.getName());
	
	private String tableName;
	private String observationTable;
	
	private AbstractQueryEngine engine;
	
	private HasObservationFilterVisitor observationFilterVisitor = new HasObservationFilterVisitor();
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public PatrolFilterProcessor(String tableName, AbstractQueryEngine engine){
		this.tableName = tableName;
		this.engine = engine;
		this.observationTable = engine.createTempTableName();
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 * @throws SQLException 
	 */
	@Override
	public void dropTemporaryTables(Connection c) throws SQLException{
		engine.dropTable(c, observationTable);
	}

	/**
	 * 
	 * @param c database connection
	 * @param queryFilter query filter
	 * @param dateFilter date filter
	 * @param caFilter conservation area filter
	 * @param populateObservation if observation fields (wp_uuid, wp_ob_uuid) are to be populated
	 * @param includeEmptyObservations if waypoints with no observations should be included
	 * @param monitor
	 * @throws SQLException
	 */
	@Override
	public void processFilter(Connection c, IFilter queryFilter, 
			DateFilter dateFilter, Query query,
			ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations) throws SQLException{

		IFilter qFilter = queryFilter;
		if (qFilter == null){
			qFilter = EmptyFilter.INSTANCE;
		}
		qFilter.accept(observationFilterVisitor);		

		if (observationFilterVisitor.hasAttributeFilter()){
			createObservationTable(c, qFilter, dateFilter, caFilter);
		}

		createTemporaryTable(c);
		populateTemporaryTable(qFilter, dateFilter, query, caFilter, 
				includeEmptyObservations, c, populateObservation);
	}
	
	
	/*
	 * creates the query observation flattened table
	 */
	private void createTemporaryTable(Connection c) throws SQLException {

		String createTableStatement = engine.getTemporaryTableCreateClause(tableName);
		logger.finest(createTableStatement);
		c.createStatement().execute(createTableStatement);
		
		engine.buildTemporaryTableIndexes(c, tableName);
	}
	
	
	/*
	 * return the table name for the associate object 
	 */
	private String name(Class<?> clazz){
		return engine.tableName(clazz);
	}
	
	/*
	 * return the sql prefix for the given class
	 */
	private String prefix(Class<?> clazz){
		return engine.tablePrefix(clazz);
	}
	
	/*
	 * combine the table name with the table prefix for
	 * the given class
	 * Patrol.cass = "smart.patrol p"
	 */
	private String namePrefix(Class<?> clazz){
		return engine.tableNamePrefix(clazz);
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
	private void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			Query query,
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
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
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
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
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
			sql.append(namePrefix(WaypointObservation.class));
			usedTables.add(WaypointObservation.class);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class));
			sql.append(".wp_uuid "); //$NON-NLS-1$
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
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
			    sql.append(filter);
			}
		}
		logger.finest(sql.toString());
		try(NamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	
	private void createObservationTable(Connection c, IFilter filter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter)
			throws SQLException {
		
		AttributeFilterCollectorVisitor collector = new AttributeFilterCollectorVisitor();
		filter.accept(collector);
		Collection<AttributeInfo> keys = collector.getAttributeInfo();
		
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + observationTable + " (observation_uuid uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		for (AttributeInfo key : keys) {
			sql.append(", " + key.getKey() + " " //$NON-NLS-1$ //$NON-NLS-2$
					+ engine.getDataType(key.getType()));
		}
		sql.append(")"); //$NON-NLS-1$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + engine.getIndexName(observationTable) + "_obuuid_idx on " + observationTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
		String attributeTempTable = engine.createTempTableName();
			
		for (AttributeInfo key : keys){
			
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(attributeTempTable); 
			sql.append("(observation_uuid uuid, value "); //$NON-NLS-1$
			sql.append( engine.getDataType(key.getType()) );
			sql.append( ")"); //$NON-NLS-1$
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());
			try {
				engine.clearParameters();
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".observation_uuid, "); //$NON-NLS-1$

				if (key.getType() == AttributeType.LIST) {
					sql.append("l.keyid "); //$NON-NLS-1$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append("t.hkey "); //$NON-NLS-1$
				} else {
					sql.append(prefix(WaypointObservationAttribute.class)
							+ "." + key.getColumn()); //$NON-NLS-1$						
				}
				sql.append(" as "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" "); //$NON-NLS-1$

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
					String cfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
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

				if (dateFilter != null) {
					String dfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
					if (dfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(dfilter);
					}
				}

				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservation.class)
						+ " as " + prefix(WaypointObservation.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(Waypoint.class) + ".uuid = " + prefix(WaypointObservation.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservationAttribute.class)
						+ " as " + prefix(WaypointObservationAttribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(WaypointObservation.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(Attribute.class)
						+ " as " + prefix(Attribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(Attribute.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (key.getType() == AttributeType.LIST) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeListItem.class));
					sql.append(" l on l.uuid = " + prefix(WaypointObservationAttribute.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeTreeNode.class));
					sql.append(" t on t.uuid = " + prefix(WaypointObservationAttribute.class) + ".tree_node_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sql.append("WHERE "); //$NON-NLS-1$
				String p = engine.addParameterValue(key.getKey());
				sql.append(" " + prefix(Attribute.class) + ".keyid = " + p ); //$NON-NLS-1$ //$NON-NLS-2$
				
				logger.finest(sql.toString());
				
				try(NamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
					ps.executeUpdate();
				}
				
				
				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(engine.getIndexName(attributeTempTable));
				sql.append("__observation_uuid_idx on "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("(observation_uuid)"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" set "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE a.observation_uuid = "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(".observation_uuid)"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append("(observation_uuid, "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  observation_uuid, value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT observation_uuid FROM "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" b WHERE b.observation_uuid = a.observation_uuid))"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + attributeTempTable); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
		
		
	}
}
