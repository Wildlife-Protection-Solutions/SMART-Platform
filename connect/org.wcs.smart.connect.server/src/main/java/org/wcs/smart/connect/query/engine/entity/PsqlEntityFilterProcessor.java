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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.logging.Logger;

import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.ObservationFilterUtils;
import org.wcs.smart.connect.query.engine.ObservationFilterUtils.IDateFilterProcessor;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.entity.query.engine.visitor.AreaFilterVisitor;
import org.wcs.smart.entity.query.engine.visitor.HasObservationFilterVisitor;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
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
public class PsqlEntityFilterProcessor implements IFilterProcessor {
	private final Logger logger = Logger.getLogger(PsqlEntityFilterProcessor.class.getName());
	
	private String tableName;
	private String observationTable;
	
	private AbstractQueryEngine engine;
	private EntityAttributeFilterVisitor entityVisitor;
	
	private HasObservationFilterVisitor observationFilterVisitor = new HasObservationFilterVisitor();
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public PsqlEntityFilterProcessor(String tableName, AbstractQueryEngine engine){
		this.tableName = tableName;
		this.engine = engine;
		this.observationTable = engine.createTempTableName();
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 */
	@Override
	public void dropTemporaryTables(Connection c) throws SQLException{
		engine.dropTable(c, observationTable);
		if (entityVisitor != null){
			entityVisitor.dropTemporaryTables(c, engine);
		}
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
			IDateFilterProcessor dateProcessor = (engine, sb)->{
				String dfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
				if ( !dfilter.isEmpty() ) {
					sb.append(" AND "); //$NON-NLS-1$
					sb.append(dfilter);
				}
			};
			ObservationFilterUtils.createObservationTable(observationTable, c, queryFilter, 
					engine, dateProcessor, caFilter, 
					WaypointSourceEngine.INSTANCE.getSupportedSources(),
					new EntityAttributeFilterCollectorVisitor(c, caFilter, engine));
		}
		createTemporaryTable(c);
		populateTemporaryTable(qFilter, dateFilter, query, caFilter, includeEmptyObservations, c, populateObservation);

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

		engine.clearParameters();
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		HashSet<Class<?>> usedTables = new HashSet<Class<?>>();
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM " ); //$NON-NLS-1$
		usedTables.add(Waypoint.class);
		
		//this is done to improve derby performance; otherwise performance is very slow
		sql.append ("(SELECT * FROM "); //$NON-NLS-1$
		sql.append(namePrefix(Waypoint.class));
		boolean innerwhere = true;
		if (caFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
			if (filter.length() > 0) {
				if (innerwhere){
					sql.append(" WHERE "); //$NON-NLS-1$
					innerwhere = false;
				}
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		if (dateFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (filter.length() > 0) {
				if (innerwhere){
					sql.append(" WHERE "); //$NON-NLS-1$
					innerwhere = false;
				}else{
					sql.append(" and "); //$NON-NLS-1$
				}
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(filter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}
		sql.append(") " + prefix(Waypoint.class)); //$NON-NLS-1$
		sql.append(" "); //$NON-NLS-1$
		
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
		
		AreaFilterVisitor areaVisitor = new AreaFilterVisitor(sql, engine, query.getConservationArea());
		queryFilter.accept(areaVisitor);

		entityVisitor = new EntityAttributeFilterVisitor(sql, engine, caFilter, c);
		queryFilter.accept(entityVisitor);
		
		sql.append(engine.appendFromClause(usedTables));
		
		boolean where = true;
		if (caFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
			if (filter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
					where = false;
				}
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		if (dateFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (filter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
					where = false;
				}else{
					sql.append(" and "); //$NON-NLS-1$
				}
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(filter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}
				
		
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
					where = false;
				}else{
					sql.append(" AND "); //$NON-NLS-1$	
				}
				sql.append(" ( "); //$NON-NLS-1$
			    sql.append(filter);
			    sql.append(" ) "); //$NON-NLS-1$
			}
		}
		logger.finest(sql.toString());
		try(NamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
}
